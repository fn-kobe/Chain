package com.scu.suhong.transaction.exchangeMode;

import account.AccountManager;
import com.scu.suhong.block.Block;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.graph.JGraphTWrapper;
import com.scu.suhong.transaction.ConditionalAssociationTransaction;
import com.scu.suhong.transaction.Transaction;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.io.ExportException;
import util.FileLogger;

import java.io.File;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class EMTransactionService {
    static Logger logger = FileLogger.getLogger();

    static EMTransactionService instance;
    EMTransactionServiceMap emTransactionServiceMap;
    AccountManager accountManager = AccountManager.getInstance();
    int dumpCount = 0;

    EMTransactionService() {
        emTransactionServiceMap = new EMTransactionServiceMap();
    }

    public synchronized static EMTransactionService getInstance() {
        if (null == instance) {
            instance = new EMTransactionService();
        }
        return instance;
    }

    public void reset() {
        resetService();
    }

    public void resetService() {
        emTransactionServiceMap = new EMTransactionServiceMap();
        dumpCount = 0;
    }

    public void shutDown() {
        processUnFinishedTransaction();
        resetService();
    }

    public void processUnFinishedTransaction() {
        for (List<EMTransaction> oneContractList : emTransactionServiceMap.contractToEMTransactionList.values()) {
            for (EMTransaction t : oneContractList) {
                if (t.isShouldReturnFrozenBalance()) {
                    accountManager.addValue(t.getFrom(), t.getMaxFrozenValue());
                    t.setShouldReturnFrozenBalance(false);
                }
            }
        }
    }

    public boolean processTx(EMTransaction emTransaction) {
        if (!emTransaction.isValid()) {
            System.out.println("[EMTransactionService][Warning] EMTransaction is not valid");
            return false;
        }

        if (EMServerTransactionService.getInstance().tryProcess(emTransaction)) {
            System.out.println("[EMTransactionService] Try to process EMTransaction in server mode");
            return true;
        }

        List<EMTransaction> transactionList = emTransactionServiceMap.getTransactionList(emTransaction.getContractNumber());
        if (null != transactionList) {
            if (!isPrepaidEnoughForRatioCase(transactionList, emTransaction)) {
                System.out.println("[EMTransactionService][Warning] Balance is not enough " + emTransaction.getFrom());
                return false;
            }
        }

        if (!checkToAndIncomingCondition(emTransaction)) return false;

        System.out.println("[EMTransactionService] Try to process EMTransaction in peer to peer mode");
        processPeerToPeerTx(emTransaction);
        return true;
    }

    public boolean checkToAndIncomingCondition(EMTransaction emTransaction) {
        // Get the incoming transaction of emTransaction
        EMTransaction emTransactionWithdrawal = emTransactionServiceMap.getWithdrawalTransaction(emTransaction.getContractNumber(), emTransaction.getFrom());
        if (null != emTransactionWithdrawal && !isPaymentWithdrawalConditionMatch(emTransaction, emTransactionWithdrawal)) {
            System.out.println("[EMTransactionService][Warning] Transaction doesn't match its incoming condition");
            return false;
        }
        // One transaction can be the payment and the withdrawal in the exchange
        // payment->A->withdrawal
        EMTransaction emTransactionPayment = emTransactionServiceMap.getPaymentTransaction(emTransaction.getContractNumber(), emTransaction.getFrom());
        if (null != emTransactionPayment && !isPaymentWithdrawalConditionMatch(emTransactionPayment, emTransaction)) {
            System.out.println("[EMTransactionService][Warning] Transaction doesn't match its out-going condition");
            return false;
        }

        return true;
    }

    // paymentTransaction 'to' field is 'incoming' field of withdrawal transaction
    public boolean isPaymentWithdrawalConditionMatch(EMTransaction paymentTransaction, EMTransaction withdrawalTransaction) {
        List<ValueCondition> incomingValueConditionList = withdrawalTransaction.getIncomingValueConditionList().valueConditions;
        List<ValueCondition> toValueConditionList = paymentTransaction.getToValueConditionList().valueConditions;
        if (!paymentTransaction.getOutgoingAssetType().equals(withdrawalTransaction.getIncomingAssetType()) ||
                incomingValueConditionList.size() != toValueConditionList.size()) {
            System.out.println("[EMTransactionService] Transaction asset type or condition size doesn't match");
            return false;
        }

        for (int i = 0; i < incomingValueConditionList.size(); ++i) {
            ValueCondition incomingValueCondition = incomingValueConditionList.get(i);
            ValueCondition toValueCondition = toValueConditionList.get(i);
            if (!isPaymentRatioToIncoming(withdrawalTransaction) && incomingValueCondition.getValue() != toValueCondition.getValue()) {
                System.out.println("[EMTransactionService][Warning] Condition value of payment is not the same at " + i);
                System.out.printf("[EMTransactionService][Debug] Condition of withdrawal %s is not the same with %s \n",
                        incomingValueCondition.getCondition(), toValueCondition.getCondition());
                return false;
            }
            if (!incomingValueCondition.getCondition().isMatch(toValueCondition.getCondition())) {
                System.out.println("[EMTransactionService][Warning] Condition of withdrawal is not the same at " + i);
                System.out.printf("[EMTransactionService][Debug] Condition of withdrawal %s is not the same with %s \n",
                        incomingValueCondition.getCondition(), toValueCondition.getCondition());
                return false;
            }
        }
        return true;
    }

    boolean isPaymentRatioToIncoming(EMTransaction transaction) {
        if (transaction.hasToRatioCondition()) {
            return transaction.isPaymentRatioToIncoming();
        }
        return false;
    }

    public boolean isPrepaidEnoughForRatioCase(List<EMTransaction> transactionList, EMTransaction emTransaction) {
        for (EMTransaction t : transactionList) {
            if (!isPrepaidEnoughForRatioCase(t, emTransaction)) {
                return false;
            }
        }
        return true;
    }

    public boolean isPrepaidEnoughForRatioCase(EMTransaction emTransaction1, EMTransaction emTransaction2) {
        return isPrepaidEnoughForRatioCaseInOneWay(emTransaction1, emTransaction2)
                && isPrepaidEnoughForRatioCaseInOneWay(emTransaction2, emTransaction1);
    }

    public boolean isPrepaidEnoughForRatioCaseInOneWay(EMTransaction emTransaction1, EMTransaction emTransaction2) {
        if (emTransaction2.getIncomingAddress().equals(emTransaction1.getTo()) && emTransaction2.hasIncomingRatioCondition()) {
            if (emTransaction2.getPrePaidValue() < emTransaction2.getMaxPaymentRatio() * emTransaction1.getMaxToValue()) {
                return false;
            }
        }
        return true;
    }

    public boolean processPeerToPeerTx(EMTransaction emTransaction) {
        if (!tryFreezeAccount(emTransaction)) {
            logger.error(String.format("[EMTransactionService][Warning] %s doesn't have enough money", emTransaction.getFrom()));
            return false;
        }
        emTransactionServiceMap.tryAdd(emTransaction);

        showAllBalance(formatPaidMessage(emTransaction));

        JGraphTWrapper jGraphTWrapper = emTransactionServiceMap.tryGetJGraphTWrapper(emTransaction.getContractNumber());
        jGraphTWrapper.addEdge(emTransaction.getFrom(), emTransaction.getTo(), emTransactionServiceMap.tryGetTransactionWeight(emTransaction));
        //assure by plan
//        if (jGraphTWrapper.hasDuplicatedEdges()){
//            System.out.println("[EMTransactionService][Error] Duplicated case is not allowed in peer to peer case currently. It is supported in server mode");
//            System.out.println("[EMTransactionService][Error] Try re-plan the exchange cases.");
//            jGraphTWrapper.removeEdge(emTransaction.getFrom(), emTransaction.getTo(), emTransactionServiceMap.tryGetTransactionWeight(emTransaction));
//            return false;
//        }

        // TO DO, add the timeout mechanism
        return doGraphCalculation(emTransaction.getContractNumber());
    }

    @NotNull
    public String formatWithdrawalMessage(EMTransaction clientTransaction) {
        return "Tx" + clientTransaction.getFrom() + "_w";
    }

    @NotNull
    public String formatFailedMessage(EMTransaction clientTransaction) {
        return "Tx" + clientTransaction.getFrom() + "_f";
    }

    @NotNull
    public String formatPaidMessage(EMTransaction clientTransaction) {
        return "Tx" + clientTransaction.getFrom() + "_p";
    }

    // for paper test data collection
    public static void showAllBalance(String flag) {
        final String userA = "A";
        final String userB = "B";
        final String userC = "C";
        AccountManager accountManager = AccountManager.getInstance();
        Set<String> allAssetTypes = accountManager.getAllAssetTypes();
        for (String type: allAssetTypes) {
            System.out.printf("[EMTransactionService][verification] The balance of A ,B and C for type %s is %s\t%f\t%f\t%f\n",
                    type , flag, accountManager.getBalance(userA, type), accountManager.getBalance(userB, type)
                    , accountManager.getBalance(userC, type));

        }
    }

    // return finished or not
    // true is done, false is not
    public boolean doGraphCalculation(Integer contractNumber) {
        return doGraphCalculation("", contractNumber);
    }

    // return finished or not
    // true is done, false is not
    public boolean doGraphCalculation(String msg, Integer contractNumber) {
        tryDumpDiagram(msg, getJGraphTWrapper(contractNumber));
        return computeAllMatched(contractNumber);
    }

    JGraphTWrapper getJGraphTWrapper(EMTransaction emTransaction) {
        return emTransactionServiceMap.getJGraphTWrapper(emTransaction.getContractNumber());
    }

    JGraphTWrapper getJGraphTWrapper(Integer contractNumber) {
        return emTransactionServiceMap.getJGraphTWrapper(contractNumber);
    }

    // return finished or not
    // true is done, false is not
    private boolean computeAllMatched(Integer contractNumber) {
        if (emTransactionServiceMap.isContractProcessDone(contractNumber)) {
            System.out.printf("[EMTransactionService] Transaction %d has been done\n", contractNumber);
            return true;
        }
        JGraphTWrapper jGraphTWrapper = getJGraphTWrapper(contractNumber);
        if (!jGraphTWrapper.isStronglyConnected()) return false;

        System.out.println("[EMTransactionService][Info] Transaction forms the connected directed diagram");
        Set<DefaultEdge> edges = jGraphTWrapper.getAllEdges();
        for (DefaultEdge edge : edges) {
            EMTransaction emTransaction = emTransactionServiceMap.getEmTransaction(jGraphTWrapper.getEdgeWeight(edge));
            if (!emTransaction.isConditionMatched()) return false;
        }
        System.out.println("[EMTransactionService][Info] Conditions check is Ok");

        // It is a directed diagram and exists loop matched condition
        // then handle the balance and remove the circle
        processBalance(contractNumber);

        emTransactionServiceMap.markContractAsDone(contractNumber);
        return true;
    }

    private void processBalance(EMTransaction emTransaction) {
        // payment and withdrawal are minimum if more than one condition matched
//        double minIncomingMatchedValue = emTransaction.getMinIncomingMatchedCondition().getValue();
//        double minToMatchedValue = 0;
//        if (emTransaction.isRatioTransaction())
//            minToMatchedValue = minIncomingMatchedValue * emTransaction.getMaxPaymentRatio();
//        else minToMatchedValue = emTransaction.getMinToMatchedCondition().getValue();
//        int balance = (int) (emTransaction.getMaxFrozenValue() - minToMatchedValue + minIncomingMatchedValue);
//        accountManager.changeValue(emTransaction.getFrom(), balance);
        processBalance(emTransaction.getContractNumber());
    }

    void processBalance(int contractNumber) {
        //1. find the first non ratio or any valueCondition
        List<EMTransaction> txList = emTransactionServiceMap.getTransactionList(contractNumber);
        EMTransaction firstNotRatioPaymentTx = null;
        for (EMTransaction t : txList) {
            if (!t.hasToRatioCondition()) {
                firstNotRatioPaymentTx = t;
                break;
            }
        }
        firstNotRatioPaymentTx.calculatedPaid = firstNotRatioPaymentTx.getMinToMatchedCondition().getValue();
        //2. From it to calculate the result
        EMTransaction old = firstNotRatioPaymentTx;
        EMTransaction next = emTransactionServiceMap.getWithdrawalTransaction(contractNumber, old.getFrom());
        while (next != firstNotRatioPaymentTx) {
            // Incoming
            next.calculatedIncoming = old.calculatedPaid;

            //Paid has no valueRang, it must be a specific vlue
            if (next.hasToRatioCondition()) {
                next.calculatedPaid = next.getMinToMatchedCondition().getValue() * next.calculatedIncoming;
            } else {
                next.calculatedPaid = next.getMinToMatchedCondition().getValue();
            }

            old = next;
            next = emTransactionServiceMap.getWithdrawalTransaction(contractNumber, old.getFrom());
        }
        //next is the first now as they will form a circle, and only incoming value is not calculated
        next.calculatedIncoming = old.calculatedPaid;

        for (EMTransaction t : txList) {
            // return the left balance to the sender
            accountManager.changeValue(t.getFrom(), t.getOutgoingAssetType(), t.getMaxFrozenValue() - t.calculatedPaid);
            // add the incoming to the sender
            accountManager.changeValue(t.getFrom(), t.getIncomingAssetType(),t.calculatedIncoming);
            showAllBalance(formatWithdrawalMessage(t));
            t.setShouldReturnFrozenBalance(false);
        }
    }

    void dumpVertexes(Set<String> edges) {
        if (edges.isEmpty()) return;

        String r = "Vertexes: \n";
        for (String e : edges) {
            r += e + ", ";
        }
        System.out.println(r);
    }

    public void removeTx(EMTransaction tx, JGraphTWrapper jGraphTWrapper) {
        System.out.println("[EMTransactionService][WARN] In EMTransacrion start epoc and interaction id is not implemented. The value cannot be used");
        if (jGraphTWrapper.removeEdge(0, tx.getFrom(), tx.getTo(), emTransactionServiceMap.tryGetTransactionWeight(tx), Long.valueOf(0))) {
            unFreezeAccount(tx.getFrom(), tx.getMaxFrozenValue());
        }
    }

    public void tryDumpDiagram(JGraphTWrapper jGraphTWrapper) {
        tryDumpDiagram("", jGraphTWrapper);
    }

    public void tryDumpDiagram(String msg, JGraphTWrapper jGraphTWrapper) {
        logger.info("[EMTransactionService] Try to dump the diagram");
        if (createDumpFolder()) jGraphTWrapper.export(getFileName(), msg);
    }

    String getDumpFolderName() {
        return "EMDiagramDump";
    }

    boolean createDumpFolder() {
        File file = new File(getDumpFolderName());
        if (!file.exists()) return file.mkdir();
        return true;
    }

    String getFileName() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(Date.from(Instant.now()));
        return getDumpFolderName() + File.separator + String.format("D-%1$tY-%1$tm-%1$td-%1$tk-%1$tM-%1$tS-", cal) + (++dumpCount) + ".gv";
    }

    public void addNewBlock(Block block) {
        List<AbstractTransaction> transactionList = block.getTransactions();
        for (AbstractTransaction transaction : transactionList) {
            // If want to support the normal transaction, please comments here
            if (transaction instanceof EMTransaction) {
                processTx((EMTransaction) transaction);
            }
        }
    }

    public void removeBlock(Block block) {
        List<AbstractTransaction> transactionList = block.getTransactions();
        for (AbstractTransaction transaction : transactionList) {
            // If want to support the normal transaction, please comments here
            if (transaction instanceof EMTransaction) {
                EMTransaction emTransaction = (EMTransaction) transaction;
                removeTx(emTransaction, getJGraphTWrapper(emTransaction));
            }
        }
    }

    private void addValueToAccount(Set<String> d, Double value) {
        // as incoming weight is equal to out-going weight for the whole diagram
        // we just get incoming of the first one
        for (String v : d) {
            accountManager.addValue(v, value);
        }
    }

    private boolean tryFreezeAccount(EMTransaction t) {
        if (!accountManager.canTransferValue(t.getFrom(), t.getOutgoingAssetType(), t.getMaxFrozenValue())) {
            return false;
        }
        accountManager.subValue(t.getFrom(), t.getOutgoingAssetType(), t.getMaxFrozenValue());
        t.setShouldReturnFrozenBalance(true);
        return true;
    }

    public void unFreezeAccount(JGraphTWrapper jGraphTWrapper) {
        tryDumpDiagram("Unfrozen", jGraphTWrapper);
        Set<DefaultEdge> edgeList = jGraphTWrapper.getAllEdges();
        for (DefaultEdge e : edgeList) {
            accountManager.addValue(jGraphTWrapper.getEdgeSource(e), Double.valueOf(jGraphTWrapper.getEdgeWeight(e)));
        }
        jGraphTWrapper.resetGraph();
    }

    private boolean unFreezeAccount(String address, Double value) {
        accountManager.addValue(address, value);
        return true;
    }

    private int getW(ConditionalAssociationTransaction ctx) {
        int w = ctx.getCondition().getValue();
        //if (ctx.isNormalTransaction) w = -w;
        return w;
    }

    private int getW(ConditionalAssociationTransaction ctx, String to) {
        int w = ctx.getCondition().getValue(to);
        //if (ctx.isNormalTransaction) w = -w;
        return w;
    }

    public void exportGraph(JGraphTWrapper jGraphTWrapper) {
        try {
            jGraphTWrapper.export();
        } catch (ExportException e) {
            e.printStackTrace();
        }
    }
}

package com.scu.suhong.transaction;

import account.AccountManager;
import com.scu.suhong.block.Block;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.graph.JGraphTWrapper;
import org.apache.log4j.Logger;
import util.FileLogger;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ConditionalDivisionTransactionHelper {
    static Logger logger = FileLogger.getLogger();
    static ConditionalDivisionTransactionHelper instance;
    JGraphTWrapper jGraphTWrapper = new JGraphTWrapper();
    JGraphTWrapper jGraphTWrapperStep = new JGraphTWrapper();
    List<ConditionalDivisionTransaction> unmatchedDivisionTransactionList = new ArrayList<>();
    int dumpCount = 0;

    ConditionalDivisionTransactionHelper() {
    }

    public static synchronized ConditionalDivisionTransactionHelper getInstance() {
        if (null == instance) instance = new ConditionalDivisionTransactionHelper();
        return instance;
    }

    public void addNewBlock(Block block) {
        List<AbstractTransaction> transactionList = block.getTransactions();
        for (AbstractTransaction transaction : transactionList) {
            // If want to support the normal transaction, please comments here
            if (transaction instanceof ConditionalDivisionTransaction) {
                processCTx((ConditionalDivisionTransaction) transaction);
            }
        }
    }

    List<ConditionalDivisionTransaction> getUnmatchedDivisionTransactionList() {
        return unmatchedDivisionTransactionList;
    }

    public boolean isAllTransactionDone() {
        return 0 == getUnmatchedDivisionTransactionList().size();
    }

    public void processCTx(ConditionalDivisionTransactionInterface ctx) {
        if (ctx instanceof ConditionalDivisionTransaction) {
            processConditionalDivisionTransaction((ConditionalDivisionTransaction) ctx);
        } else if (ctx instanceof DivisionCondition) {
            processDivisionCondition((DivisionCondition) ctx);
        }
    }

    private boolean tryFreezeAccount(String from, Double value) {
        if (!AccountManager.getInstance().canTransferValue(from, value)) {
            return false;
        }
        AccountManager.getInstance().subValue(from, value);
        return true;
    }

    private void processDivisionCondition(DivisionCondition condition) {
        List<ConditionalDivisionTransaction> toBeRemovedCondition = new ArrayList<>();
        for (ConditionalDivisionTransaction transaction : unmatchedDivisionTransactionList) {
            if (transaction.processCondition(condition)) {
                if (transaction.isAllDivisionProcessed()) {
                    toBeRemovedCondition.add(transaction);
                }
            }
        }
        for (ConditionalDivisionTransaction transaction : toBeRemovedCondition) {
            unmatchedDivisionTransactionList.remove(transaction);
        }
    }

    private void processConditionalDivisionTransaction(ConditionalDivisionTransaction ctx) {
        List<DivisionConditionPair> conditionParis = ctx.getDivisionConditionPairList();
        for (DivisionConditionPair pair : conditionParis) {
            for (String c : pair.getConditionList())
                jGraphTWrapper.addEdge(String.valueOf(pair.percent), c + "_from_" + ctx.getFrom(), pair.percent);
        }
        tryDumpDiagram();

        if (!tryFreezeAccount(ctx.getFrom(), Double.valueOf(ctx.getValue()))) {
            logger.error(String.format("[ConditionalDivisionTransactionHelper] %s doesn't have enough balance: %d"
                    , ctx.getFrom(), ctx.getValue()));
            return;
        }
        unmatchedDivisionTransactionList.add(ctx);
    }

    public void tryDumpDiagram() {
        tryDumpDiagram("");
    }

    public void tryDumpDiagram(String msg) {
        logger.info("[ConditionalAssociationTransactionHelper] Try to dump the diagram");
        if (createDumpFolder()) jGraphTWrapper.export(getFileName(), msg);
    }

    String getDumpFolderName() {
        return "DiagramDumpDivision";
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
}

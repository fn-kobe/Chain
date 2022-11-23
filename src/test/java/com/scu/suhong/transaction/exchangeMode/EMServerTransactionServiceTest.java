package com.scu.suhong.transaction.exchangeMode;

import account.AccountManager;
import com.scu.suhong.block.Block;
import com.scu.suhong.block.BlockBody;
import com.scu.suhong.block.BlockHeader;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.Transaction;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

public class EMServerTransactionServiceTest {
    final static String server = "server";
    final String testGameServerIp = "127.0.0.1";
    final String userA = "A";
    final String userB = "B";
    final String userC = "C";
    final String userD = "D";
    final String userE = "E";
    final String userF = "F";
    final String userG = "G";
    String userArray[] = {userA, userB, userC, userD, userE, userF, userG};

    double clientIncomingValue1[] = {20, 20, 20, 20, 20, 20, 20};
    double clientIncomingValue2[] = {5, 5, 5, 5, 5, 5, 5};
    double clientPaymentValue[] = {10, 10, 10, 10, 10, 10, 10};
    final Double serverIncomingValue = Double.valueOf(10);
    final Double serverPayment1 = Double.valueOf(20);
    final Double serverPayment2 = Double.valueOf(5);

    double clientIncomingValueRatio1[] = {2, 2, 2, 2, 2, 2, 2};
    double clientIncomingValueRatio2[] = {1/2, 1/2, 1/2, 1/2, 1/2, 1/2, 1/2};
    double clientPaymentValueRatio[] = {10, 20, 16, 28, 20, 30, 10};
    final Double serverIncomingValueRatio = Double.valueOf(0);
    final Double serverPaymentRatio1 = Double.valueOf(2);
    final Double serverPaymentRatio2 = Double.valueOf(1/2);
    final Double minRatioServerAllowedValue = Double.valueOf(10);
    boolean useMinPaymentOptimization = false;

    final static AccountManager accountManager = AccountManager.getInstance();

    @Test
    public void runTimes() {
        System.out.println("[EMServerTransactionServiceTest] Start to test EMServerTransactionService in loop mode");
        initUserBalance();

        int prepaidAsset = 100;
        boolean isPaymentRatioToIncoming = true;
        boolean isRatioTransaction = false;
        final int contractNumber  = 1357;

        EMServerTransactionService emServerTransactionService = EMServerTransactionService.getInstance();
        Thread emServerTransactionThread = new Thread(emServerTransactionService, "process server transaction");
        EMServerTransaction serverTransaction = createEmServerTransaction(prepaidAsset, isRatioTransaction, isPaymentRatioToIncoming,
                serverPayment1, serverPayment2, serverIncomingValue);
        emServerTransactionService.addServerTransaction(serverTransaction);

        int times = 3;
        for (int i = 0; i < userArray.length; ++i) {
            // We select B as the times transaction
            if (2 == i) {
                emServerTransactionService.tryProcess(createClientToServerEmTransaction(contractNumber, userArray[i], isRatioTransaction,
                        clientIncomingValue1[i], clientIncomingValue2[i], clientPaymentValue[i], times));
            } else {
                emServerTransactionService.tryProcess(
                        createClientToServerEmTransaction(contractNumber, userArray[i], isRatioTransaction,
                                clientIncomingValue1[i], clientIncomingValue2[i], clientPaymentValue[i]));
            }
            showAllBalance();
        }

        emServerTransactionThread.start();
        try {
            emServerTransactionThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
        serverTransaction.shutDownService();
        showAllBalance();

        System.out.println("[EMServerTransactionServiceTest] End to test EMServerTransactionService");
    }

// real value exchange comparing to ratio exchange
    @Test
    public void runServerRealValue() {
        boolean isRatioTransaction = false;
        runCase(isRatioTransaction, clientIncomingValue1, clientIncomingValue2, clientPaymentValue,
                serverPayment1, serverPayment2, serverIncomingValue);
    }

    // Can be used to do server op or without it
    @Test
    public void runServerRatioCase() {
        boolean isRatioTransaction = true;
        // Open this if want to use client in-coming value to increase the server capacity
        useMinPaymentOptimization = false;
        runCase(isRatioTransaction, clientIncomingValueRatio1, clientIncomingValueRatio2,
                clientPaymentValueRatio, serverPaymentRatio1, serverPaymentRatio2, serverIncomingValueRatio);
    }

    public void runCase(boolean isRatioTransaction, double[] clientIncomingValue1, double[] clientIncomingValue2,
                        double[] clientPaymentValue, double serverPaymentValue1, double serverPaymentValue2,
                        double serverIncomingValue) {
        System.out.println("[EMServerTransactionServiceTest] Start to test EMServerTransactionService in "
                + (isRatioTransaction? "ratio" : "real value") + " mode");
        initUserBalance();

        final int contractNumber  = 1357;
        int prepaidAsset = 100;
        boolean isPaymentRatioToIncoming = true;

        EMServerTransactionService emServerTransactionService = EMServerTransactionService.getInstance();
        Thread emServerTransactionThread = new Thread(emServerTransactionService, "process server transaction");
        EMServerTransaction serverTransaction = createEmServerTransaction(prepaidAsset, isRatioTransaction, isPaymentRatioToIncoming,
                serverPaymentValue1, serverPaymentValue2, serverIncomingValue);
        emServerTransactionService.addServerTransaction(serverTransaction);

        showAllBalance();
        for (int i = 0; i < userArray.length; ++i) {
            emServerTransactionService.tryProcess(
                    createClientToServerEmTransaction(contractNumber, userArray[i], isRatioTransaction,
                            clientIncomingValue1[i], clientIncomingValue2[i], clientPaymentValue[i]));
        }
        showAllBalance();

        emServerTransactionThread.start();
        try {
            emServerTransactionThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
        serverTransaction.shutDownService();

        showAllBalance();

        System.out.println("[EMServerTransactionServiceTest] End to test EMServerTransactionService");
    }

    @Test
    public void runLoop() {
        System.out.println("[EMServerTransactionServiceTest] Start to test EMServerTransactionService in loop mode");
        initUserBalance();

        Block b = createBlock();
        List<AbstractTransaction> numberedTransactionList = getNumberedTransactionList(b);

        int prepaidAsset = 100;
        boolean isPaymentRatioToIncoming = true;
        boolean isRatioTransaction = false;

        EMServerTransactionService emServerTransactionService = EMServerTransactionService.getInstance();
        Thread emServerTransactionThread = new Thread(emServerTransactionService, "process server transaction");
        EMServerTransaction serverTransaction = createEmServerTransaction(prepaidAsset, isRatioTransaction, isPaymentRatioToIncoming,
                serverPayment1, serverPayment2, serverIncomingValue);
        emServerTransactionService.addServerTransaction(serverTransaction);

        for (AbstractTransaction t: numberedTransactionList) {
            if (!(t instanceof EMTransaction)) continue;
            emServerTransactionService.tryProcess((EMTransaction) t);
        }
        showAllBalance();

        emServerTransactionThread.start();
        try {
            emServerTransactionThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
        serverTransaction.shutDownService();
        showAllBalance();

        System.out.println("[EMServerTransactionServiceTest] End to test EMServerTransactionService");
    }

    public List<AbstractTransaction> getNumberedTransactionList(Block b) {
        List<AbstractTransaction> transactions = b.getTransactions();
        // The loop number starts from 1. 0 is as the invalid number.
        int i = 1;
        for (AbstractTransaction t : transactions){
            if (t instanceof EMTransaction){
                ((EMTransaction) t).setLoopNumber(i);
            }
            ++i;
        }
        return transactions;
    }

    @NotNull
    public Block createBlock() {
        BlockHeader h = new BlockHeader();
        BlockBody b = new BlockBody();
        final int contractNumber  = 135783;
        boolean isRatioTransaction = false;

        for (int i = 0; i < userArray.length; ++i) {
            showAllBalance();
            b.addTransaction(
                    createClientToServerEmTransaction(contractNumber, userArray[i], isRatioTransaction,
                            clientIncomingValue1[i], clientIncomingValue2[i], clientPaymentValue[i]));
        }
        Block block = new Block(h, b);
        return block;
    }

    public void initUserBalance() {
        accountManager.addValue(server, 130);
        accountManager.addValue(userA, 8);
        accountManager.addValue(userB, 50);
        accountManager.addValue(userC, 50);
        accountManager.addValue(userD, 50);
        accountManager.addValue(userE, 50);
        accountManager.addValue(userF, 50);
        accountManager.addValue(userG, 50);
        showAllBalance();
    }

    void showAllBalance(){
        System.out.printf("[EMServerTransactionServiceTest][verification] The balance is %f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\n",
                accountManager.getBalance(server), accountManager.getBalance(userA),
                accountManager.getBalance(userB), accountManager.getBalance(userC),
                accountManager.getBalance(userD), accountManager.getBalance(userE),
                accountManager.getBalance(userF), accountManager.getBalance(userG));
    }

    @NotNull
    public EMServerTransaction createEmServerTransaction(int prepaidAsset, boolean isRatioTransaction, boolean isPaymentRatioToIncoming,
                                                         Double paymentValue1, Double paymentValue2, Double incomingValue) {
        EMServerTransaction serverTransaction = new EMServerTransaction(server);
        if (isPaymentRatioToIncoming){
            serverTransaction.setMinAllowedValue(minRatioServerAllowedValue);
            serverTransaction.setPaymentRatioToIncoming();
        }
        serverTransaction.setUseMinPaymentOptimization(useMinPaymentOptimization);

        serverTransaction.prepareService(prepaidAsset);
        serverTransaction.addToValueConditionPair(
                createValueCondition(ConditionResult.Undefined, paymentValue1,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp
                        , "win", isRatioTransaction));
        serverTransaction.addToValueConditionPair(
                createValueCondition(ConditionResult.Undefined, paymentValue2,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp
                        , "lost", isRatioTransaction));
        serverTransaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.True, incomingValue));
        return serverTransaction;
    }

    @NotNull
    public EMTransaction createClientToServerEmTransaction(int contractNumber, String address, boolean isRatioTransaction,
                                                           Double incomingValue1, Double incomingValue2, Double paymentValue) {
        return createClientToServerEmTransaction(
                contractNumber, address, isRatioTransaction, incomingValue1, incomingValue2, paymentValue, 1);
    }

    @NotNull
    public EMTransaction createClientToServerEmTransaction(int contractNumber, String address, boolean isRatioTransaction,
                                                           Double incomingValue1, Double incomingValue2, Double paymentValue,
                                                           int times) {
        String fistCondition = "win";
        String secondCondition = "lost";

        EMTransaction transactionClient2Server = createClientTransaction(address, server, server, contractNumber);
        transactionClient2Server.addIncomingValueConditionPair(
                createValueCondition(ConditionResult.Undefined, incomingValue1,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp
                        , fistCondition, isRatioTransaction));
        transactionClient2Server.addIncomingValueConditionPair(
                createValueCondition(ConditionResult.Undefined, incomingValue2,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp
                        , secondCondition, isRatioTransaction));
        transactionClient2Server.addToValueConditionPair(createValueCondition(ConditionResult.True, paymentValue));
        transactionClient2Server.setTimes(times);
        return transactionClient2Server;
    }

    private ValueCondition createValueCondition(ConditionResult result, Double value) {
        return createValueCondition(result, value, "", "");
    }

    @NotNull
    private EMTransaction createClientTransaction(String from, String incoming, String payment, int contractNumber) {
        EMTransaction transaction = new EMTransaction();
        transaction.setContractNumber(contractNumber);
        transaction.setFrom(from);
        transaction.setIncomingAddress(incoming);
        transaction.setPaymentAddress(payment);
        return transaction;
    }

    private ValueCondition createRatioValueCondition(ConditionResult initState, double ratio,
                                                     String conditionCommand, String expectResultString) {
        return createValueCondition(initState, ratio, conditionCommand, expectResultString, true);
    }

    private ValueCondition createValueCondition(ConditionResult initState, Double value,
                                                String conditionCommand, String expectResultString) {
        return createValueCondition(initState, value, conditionCommand, expectResultString, false);
    }

    private ValueCondition createValueCondition(ConditionResult initState, Double value,
                                                String conditionCommand, String expectResultString, boolean isRatioPayment) {
        Condition condition = createCondition(initState);
        condition.setConditionContract(conditionCommand);
        condition.setExpectResultString(expectResultString);

        ValueCondition valueCondition = null;
        if (!isRatioPayment){
            valueCondition = new ValueCondition(value, condition);
        } else {
            valueCondition = new RatioValueCondition(value, condition);
        }
        return valueCondition;
    }

    private Condition createCondition(ConditionResult result) {
        Condition condition = new Condition();
        condition.setResult(result);
        return condition;
    }
}
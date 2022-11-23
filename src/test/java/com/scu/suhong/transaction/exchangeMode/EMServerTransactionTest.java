package com.scu.suhong.transaction.exchangeMode;

import account.AccountManager;
import org.junit.Test;

public class EMServerTransactionTest {

    final static String testServerAddress = "00ABC123";
    final static String testClient1Address = "99ABC001";
    final static AccountManager accountManager = AccountManager.getInstance();

    @Test
    public void tryAddClientTransaction() {
        int prePaidValue = 50;
        accountManager.addValue(testServerAddress, 60);
        EMServerTransaction serverTransaction = new EMServerTransaction(testServerAddress);
        assert serverTransaction.prepareService(prePaidValue);

        assert serverTransaction.tryProcessClientTransaction(createClientTransaction(10, 10));
        assert serverTransaction.tryProcessClientTransaction(createClientTransaction(10, 10));
        assert !serverTransaction.tryProcessClientTransaction(createClientTransaction(20, 40));
        assert serverTransaction.tryProcessClientTransaction(createClientTransaction(10, 25));
        assert !serverTransaction.tryProcessClientTransaction(createClientTransaction(10, 10));

        Double balance = accountManager.getBalance(testServerAddress);
        serverTransaction.shutDownService();
        assert balance + 5 == accountManager.getBalance(testServerAddress);// return the left 5 asset
        assert !serverTransaction.tryProcessClientTransaction(createClientTransaction(10, 1));
        assert 0 == serverTransaction.getUnprocessedClientTransactionNumber();
    }

    @Test
    public void processUnprocessedClientTransactions() {
        int prePaidValue = 50;
        accountManager.addValue(testServerAddress, 60);
        EMServerTransaction serverTransaction = new EMServerTransaction(testServerAddress);
        assert serverTransaction.prepareService(prePaidValue);

        assert serverTransaction.tryProcessClientTransaction(createClientTransaction(10, 10));
        assert serverTransaction.tryProcessClientTransaction(createClientTransaction(10, 2));
        assert serverTransaction.tryProcessClientTransaction(createClientTransaction(
                10, 2, ConditionResult.False, ConditionResult.True));

        assert !serverTransaction.tryProcessClientTransaction(createClientTransaction(20, 40));
        assert serverTransaction.tryProcessClientTransaction(createClientTransaction(
                10, 10, ConditionResult.True, ConditionResult.False));

        assert serverTransaction.tryProcessClientTransaction(createClientTransaction(10, 25));

        serverTransaction.shutDownService();
        assert 2 == serverTransaction.getUnprocessedClientTransactionNumber();
    }

    @Test
    public void testJson(){
        int prePaidValue = 50;
        accountManager.addValue(testServerAddress, 60);
        EMServerTransaction serverTransaction = new EMServerTransaction(testServerAddress);
        assert serverTransaction.prepareService(prePaidValue);
        assert serverTransaction.tryProcessClientTransaction(createClientTransaction(10, 10));
        assert serverTransaction.tryProcessClientTransaction(createClientTransaction(10, 2));

        String jsonString = serverTransaction.getJson().toString();

        EMServerTransaction serverTransactionNew = EMServerTransaction.fromJson(jsonString);
        assert null != serverTransactionNew;
        assert serverTransactionNew.getJson().toString().equals(jsonString);
    }

    @Test
    public void exchangeAsset() {
        int prePaidValue = 50;
        accountManager.addValue(testServerAddress, 60);
        accountManager.addValue(testClient1Address, 60);
        EMServerTransaction serverTransaction = new EMServerTransaction(testServerAddress);
        assert serverTransaction.prepareService(prePaidValue);

        Double serverOldBalance = accountManager.getBalance(testServerAddress);
        Double clientOldBalance = accountManager.getBalance(testClient1Address);

        //exchange
        assert serverTransaction.tryProcessClientTransaction(createClientTransaction(10, 5));
        assert serverOldBalance == accountManager.getBalance(testServerAddress) - 10;
        assert clientOldBalance == accountManager.getBalance(testClient1Address) + 5;
    }

    EMTransaction createClientTransaction(int toValue, int withdrawalValue) {
        return createClientTransaction(testClient1Address, toValue, withdrawalValue);
    }
    EMTransaction createClientTransaction(String senderAddress, int toValue, int withdrawalValue) {
        return createClientTransaction(senderAddress, toValue, withdrawalValue, ConditionResult.True, ConditionResult.True);
    }
    EMTransaction createClientTransaction(int toValue, int withdrawalValue,
                                          ConditionResult toState, ConditionResult withdrawalState){
        return createClientTransaction(testClient1Address, toValue, withdrawalValue, toState, withdrawalState);
    }

    EMTransaction createClientTransaction(String senderAddress, int toValue, int withdrawalValue,
                                          ConditionResult toState, ConditionResult withdrawalState){
        EMTransaction clientTransaction = new EMTransaction();
        clientTransaction.setFrom(senderAddress);
        ValueCondition toValueCondition = new ValueCondition(toValue, createCondition(toState));
        clientTransaction.addToValueConditionPair(toValueCondition);

        ValueCondition fromValueCondition = new ValueCondition(withdrawalValue, createCondition(withdrawalState));
        clientTransaction.addIncomingValueConditionPair(fromValueCondition);
        clientTransaction.setHash(clientTransaction.getHash());

        return clientTransaction;
    }

    private Condition createCondition(ConditionResult result) {
        Condition condition = new Condition();
        condition.setResult(result);
        return condition;
    }
}
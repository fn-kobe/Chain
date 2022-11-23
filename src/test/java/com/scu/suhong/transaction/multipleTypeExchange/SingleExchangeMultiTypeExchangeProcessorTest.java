package com.scu.suhong.transaction.multipleTypeExchange;

import account.AccountManager;
import com.scu.suhong.transaction.CrosschainMultiTypeExchangeTransaction;
import consensus.pow.MiningConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import util.ThreadHelper;

public class SingleExchangeMultiTypeExchangeProcessorTest {
    int exchangeId= 10;
    String chainID = MiningConfiguration.getBlockchainStringId();

    String testUser = "user";
    String testTo = "to";
    String testAssetType = "assetType";
    int testValue = 10;
    int initialValue = 100;

    @Test
    public void process() {
    }

    @Test
    public void processVariable() {
        AccountManager.getInstance().addValue(testUser, testAssetType, initialValue);
        assert initialValue == AccountManager.getInstance().getBalance(testUser, testAssetType);

        SingleExchangeMultiTypeExchangeProcessor processor = new SingleExchangeMultiTypeExchangeProcessor(exchangeId);
        MultiTypeExchangeTransaction transaction = createMultiTypeExchangeTransactionWithoutRequiredData("1", "2", "variable");

        transaction.addRequiredData(constructRequiredData("1", exchangeId));
        processor.process(transaction);

        assert 1 == processor.requiredList.size();
        assert 1 == processor.appearedTransactionList.size();

        processor.process(transaction);
        assert 1 == processor.requiredList.size();
        assert 1 == processor.appearedTransactionList.size();

        transaction.addRequiredData(constructRequiredData("2", exchangeId));
        processor.process(transaction);
        assert 2 == processor.requiredList.size();
        assert 1 == processor.appearedTransactionList.size();
    }

    @Test
    public void processVariableMatch() {
        String add_1 = "1";
        String add_2 = "2";
        String add_3 = "3";
        AccountManager.getInstance().reset();
        AccountManager.getInstance().addValue(join(testUser, add_1), join(testAssetType,add_1), initialValue);
        AccountManager.getInstance().addValue(join(testUser, add_2), join(testAssetType,add_2), initialValue);
        AccountManager.getInstance().addValue(join(testUser, add_3), join(testAssetType,add_3), initialValue);
        assert initialValue == AccountManager.getInstance().getBalance(join(testUser, add_1), join(testAssetType,add_1));
        assert initialValue == AccountManager.getInstance().getBalance(join(testUser, add_2), join(testAssetType,add_2));
        assert initialValue == AccountManager.getInstance().getBalance(join(testUser, add_3), join(testAssetType,add_3));

        // 1->2 at 1 re at 3; 2-3>3 at 2 re: at1; 3->1 at3 : re:at2
        // 1->2 at 1 re at 3
        SingleExchangeMultiTypeExchangeProcessor processor = new SingleExchangeMultiTypeExchangeProcessor(exchangeId);
        MultiTypeExchangeTransaction transaction = createMultiTypeExchangeTransactionWithoutRequiredData(add_1, add_2, "variable");
       // int exchangeId, String from, String to, String assetType, int value
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_1), join(testAssetType, add_3), testValue));
        processor.process(transaction);
        assert 1 == processor.requiredList.size();
        assert 1 == processor.appearedTransactionList.size();

        // 2-3>3 at 2 re: at1
        transaction = createMultiTypeExchangeTransactionWithoutRequiredData(add_2, add_3, "variable");
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_2), join(testAssetType, add_1), testValue));
        processor.process(transaction);

        // 3->1 at3 : re:at2
        transaction = createMultiTypeExchangeTransactionWithoutRequiredData(add_3, add_1, "variable");
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_3), join(testAssetType, add_2), testValue));
        processor.process(transaction);

        assert processor.complete();
    }

    @Test
    public void processCrosschainVariableMatch() {
        String add_1 = "1";
        String add_2 = "2";
        String chain1ID = MiningConfiguration.getBlockchainStringId();
        String chain2ID = "222";
        AccountManager.getInstance().reset();
        AccountManager.getInstance().addValue(join(testUser, add_1), join(testAssetType,add_1), initialValue);
        assert initialValue == AccountManager.getInstance().getBalance(join(testUser, add_1), join(testAssetType,add_1));

        // 1->2 at 1 re at 2; 2->1 at 2; 2->1 at 2 re at 1; 1->2 at 1;
        // 1->2 at 1 re at 2; 2->1 at 2
        SingleExchangeMultiTypeExchangeProcessor processor = new SingleExchangeMultiTypeExchangeProcessor(exchangeId);
        CrosschainMultiTypeExchangeTransaction transaction = createCrosschainMultiTypeExchangeTransactionWithoutRequiredData(add_1, add_2, "variable", chain1ID);
       // int exchangeId, String from, String to, String assetType, int value
        transaction.addRequiredData(new RequiredData(exchangeId, chain2ID, "", join(testUser, add_1), join(testAssetType, add_2), testValue));
        processor.process(transaction);
        assert 1 == processor.requiredList.size();
        assert 1 == processor.appearedTransactionList.size();

        // 2-3>3 at 2 re: at1
        transaction = createCrosschainMultiTypeExchangeTransactionWithoutRequiredData(add_2, add_1, "variable", chain2ID);
        transaction.addRequiredData(new RequiredData(exchangeId, chain1ID, "", join(testUser, add_2), join(testAssetType, add_1), testValue));
        processor.process(transaction);

        assert processor.complete();
        assert initialValue - testValue == AccountManager.getInstance().getBalance(join(testUser, add_1), join(testAssetType,add_1));

        //User2 is not in this chain - not the value of MiningConfiguration.getBlockchainStringId();, then no balance
        //assert initialValue - testValue == AccountManager.getInstance().getBalance(join(testUser, add_2), join(testAssetType,add_2));

        //user2 in this chain
        assert testValue == AccountManager.getInstance().getBalance(join(testUser, add_2), join(testAssetType,add_1));
    }

    @Test
    public void processFixed() {
        String add_1 = "1";
        String add_2 = "2";
        String add_3 = "3";
        AccountManager.getInstance().reset();
        AccountManager.getInstance().addValue(join(testUser, add_1), join(testAssetType,add_1), initialValue);
        AccountManager.getInstance().addValue(join(testUser, add_2), join(testAssetType,add_2), initialValue);
        AccountManager.getInstance().addValue(join(testUser, add_3), join(testAssetType,add_3), initialValue);
        assert initialValue == AccountManager.getInstance().getBalance(join(testUser, add_1), join(testAssetType,add_1));
        assert initialValue == AccountManager.getInstance().getBalance(join(testUser, add_2), join(testAssetType,add_2));
        assert initialValue == AccountManager.getInstance().getBalance(join(testUser, add_3), join(testAssetType,add_3));

        // 1->2 at 1 re at 3; 2-3>3 at 2 re: at1; 3->1 at3 : re:at2
        // 1->2 at 1 re at 3
        SingleExchangeMultiTypeExchangeProcessor processor = new SingleExchangeMultiTypeExchangeProcessor(exchangeId);
        MultiTypeExchangeTransaction transaction = createMultiTypeExchangeTransactionWithoutRequiredData(add_1, add_2, "fixed");
        // int exchangeId, String from, String to, String assetType, int value
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_1), join(testAssetType, add_3), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_2), join(testAssetType, add_1), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_3), join(testAssetType, add_2), testValue));
        assert processor.process(transaction);
        assert 3 == processor.requiredList.size();
        assert 1 == processor.appearedTransactionList.size();

        // 2-3>3 at 2 re: at1
        transaction = createMultiTypeExchangeTransactionWithoutRequiredData(add_2, add_3, "fixed");
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_1), join(testAssetType, add_3), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_2), join(testAssetType, add_1), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_3), join(testAssetType, add_2), testValue));
        assert processor.process(transaction);

        // 3->1 at3 : re:at2
        transaction = createMultiTypeExchangeTransactionWithoutRequiredData(add_3, add_1, "fixed");
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_1), join(testAssetType, add_3), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_2), join(testAssetType, add_1), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_3), join(testAssetType, add_2), testValue));
        assert processor.process(transaction);

        assert processor.complete();
    }

    @Test
    public void processFixedWithConflictRequirement() {
        String add_1 = "1";
        String add_2 = "2";
        String add_3 = "3";
        AccountManager.getInstance().reset();
        AccountManager.getInstance().addValue(join(testUser, add_1), join(testAssetType,add_1), initialValue);
        AccountManager.getInstance().addValue(join(testUser, add_2), join(testAssetType,add_2), initialValue);
        AccountManager.getInstance().addValue(join(testUser, add_3), join(testAssetType,add_3), initialValue);
        assert initialValue == AccountManager.getInstance().getBalance(join(testUser, add_1), join(testAssetType,add_1));
        assert initialValue == AccountManager.getInstance().getBalance(join(testUser, add_2), join(testAssetType,add_2));
        assert initialValue == AccountManager.getInstance().getBalance(join(testUser, add_3), join(testAssetType,add_3));

        // 1->2 at 1 re at 3; 2-3>3 at 2 re: at1; 3->1 at3 : re:at2
        // 1->2 at 1 re at 3
        SingleExchangeMultiTypeExchangeProcessor processor = new SingleExchangeMultiTypeExchangeProcessor(exchangeId);
        MultiTypeExchangeTransaction transaction = createMultiTypeExchangeTransactionWithoutRequiredData(add_1, add_2, "fixed");
        // int exchangeId, String from, String to, String assetType, int value
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_1), join(testAssetType, add_3), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_2), join(testAssetType, add_1), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_3), join(testAssetType, add_2), testValue));
        assert processor.process(transaction);
        assert 3 == processor.requiredList.size();
        assert 1 == processor.appearedTransactionList.size();

        // 2-3>3 at 2 re: at1
        transaction = createMultiTypeExchangeTransactionWithoutRequiredData(add_2, add_3, "fixed");
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_1), join(testAssetType, add_3), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_3), join(testAssetType, add_1), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_3), join(testAssetType, add_2), testValue));
        assert !processor.process(transaction);

        // 3->1 at3 : re:at2
        transaction = createMultiTypeExchangeTransactionWithoutRequiredData(add_3, add_1, "fixed");
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_1), join(testAssetType, add_3), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_2), join(testAssetType, add_1), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_3), join(testAssetType, add_2), testValue));
        assert processor.process(transaction);

        assert !processor.complete();
    }

    @Test
    public void testTimeout(){
        String add_1 = "1";
        String add_2 = "2";
        String add_3 = "3";
        AccountManager.getInstance().addValue(join(testUser, add_1), join(testAssetType,add_1), initialValue);
        AccountManager.getInstance().addValue(join(testUser, add_2), join(testAssetType,add_2), initialValue);
        AccountManager.getInstance().addValue(join(testUser, add_3), join(testAssetType,add_3), initialValue);
        assert initialValue == AccountManager.getInstance().getBalance(join(testUser, add_1), join(testAssetType,add_1));
        assert initialValue == AccountManager.getInstance().getBalance(join(testUser, add_2), join(testAssetType,add_2));
        assert initialValue == AccountManager.getInstance().getBalance(join(testUser, add_3), join(testAssetType,add_3));

        // 1->2 at 1 re at 3; 2-3>3 at 2 re: at1; 3->1 at3 : re:at2
        // 1->2 at 1 re at 3
        SingleExchangeMultiTypeExchangeProcessor processor = new SingleExchangeMultiTypeExchangeProcessor(exchangeId);
        processor.setTransactionMaxWaitTime(2*1000);// 20 seconds for test
        MultiTypeExchangeTransaction transaction = createMultiTypeExchangeTransactionWithoutRequiredData(add_1, add_2, "fixed");
        // int exchangeId, String from, String to, String assetType, int value
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_1), join(testAssetType, add_3), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_2), join(testAssetType, add_1), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_3), join(testAssetType, add_2), testValue));
        assert processor.process(transaction);
        assert 3 == processor.requiredList.size();
        assert 1 == processor.appearedTransactionList.size();

        // 2-3>3 at 2 re: at1
        transaction = createMultiTypeExchangeTransactionWithoutRequiredData(add_2, add_3, "fixed");
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_1), join(testAssetType, add_3), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_2), join(testAssetType, add_1), testValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, "", join(testUser, add_3), join(testAssetType, add_2), testValue));
        assert processor.process(transaction);

        ThreadHelper.safeSleep(4*1000);

        // Timeout also complete
        assert processor.complete();
    }

    @NotNull
    private MultiTypeExchangeTransaction createMultiTypeExchangeTransactionWithoutRequiredData(String sender, String receiver, String type) {
        MultiTypeExchangeTransaction transaction = new MultiTypeExchangeTransaction(exchangeId);
        transaction.setId();
        transaction.setFrom(join(testUser, sender));
        transaction.setAssetType(join(testAssetType, sender));
        // Sender want to be receiver
        transaction.setToAndValue(join(testUser, receiver), testValue);
        transaction.setData(join("transation" , sender));
        transaction.setRequiredTxListType(type);
        return transaction;
    }

    @NotNull
    private CrosschainMultiTypeExchangeTransaction createCrosschainMultiTypeExchangeTransactionWithoutRequiredData(String sender, String receiver, String type, String chainID) {
        CrosschainMultiTypeExchangeTransaction transaction = new CrosschainMultiTypeExchangeTransaction(exchangeId, chainID);
        transaction.setId();
        transaction.setFrom(join(testUser, sender));
        transaction.setAssetType(join(testAssetType, sender));
        // Sender want to be receiver
        transaction.setToAndValue(join(testUser, receiver), testValue);
        transaction.setData(join("transation" , sender));
        transaction.setRequiredTxListType(type);
        return transaction;
    }

    String join(String original, String postfix){
        return original + postfix;
    }

    @NotNull
    RequiredData constructRequiredData(String postFix, int exchangeId){
        String requiredFrom = testUser + "_" + postFix;
        String requiredTo = testTo + "_" + postFix;
        String requiredAssetType = testAssetType + "_" + postFix;
        int requiredValue = testValue;

        return new RequiredData(exchangeId, chainID, requiredFrom, requiredTo, requiredAssetType, requiredValue);
    }
}
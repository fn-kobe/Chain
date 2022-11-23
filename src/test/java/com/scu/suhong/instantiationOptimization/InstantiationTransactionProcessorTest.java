package com.scu.suhong.instantiationOptimization;

import com.scu.suhong.block.Block;
import com.scu.suhong.block.BlockException;
import com.scu.suhong.transaction.Transaction;
import org.junit.Test;
import util.FileHelper;
import util.ThreadHelper;

public class InstantiationTransactionProcessorTest {

    @Test
    public void testDelayCreation() throws BlockException {
        InstantiationTransactionProcessor p = InstantiationTransactionProcessor.getInstance();
        String varietyName = "testVar1";
        String smartContract = "testDummySmartContract";
        String data = p.instantiationTxKeyword + p.fieldSeparator + p.newSmartContract + p.fieldSeparator + smartContract + p.fieldSeparator + varietyName;
        Transaction tx = createTransaction("from", "to", 10, data);
        Block block = createBlock(tx, null);

        prepareJarAndConfig(smartContract);
        p.tryAddNewBlock(block);
    }

    @Test
    public void testDealyedMethod() throws BlockException {
        InstantiationTransactionProcessor p = InstantiationTransactionProcessor.getInstance();
        String varietyName = "notCallingOtherVar1";
        String smartContract = "NotCallingOther";
        String data = p.instantiationTxKeyword + p.fieldSeparator + p.newSmartContract + p.fieldSeparator + smartContract + p.fieldSeparator + varietyName;
        Transaction tx = createTransaction("from", "to", 10, data);
        Block block = createBlock(tx, null);

        prepareJarAndConfig(smartContract);
        p.tryAddNewBlock(block);

        data = p.instantiationTxKeyword + p.fieldSeparator + p.method + p.fieldSeparator + varietyName + p.fieldSeparator + "setLoopTimes" + p.fieldSeparator + "3";
        tx = createTransaction("from", "to", 10, data);
        block = createBlock(tx, null);

        p.tryAddNewBlock(block);
        ThreadHelper.safeSleepSecond(5);
    }

    @Test
    public void testNewAndCallMethod() throws BlockException {
        InstantiationTransactionProcessor p = InstantiationTransactionProcessor.getInstance();
        String varietyName = "notCallingOtherVar1";
        String smartContract = "NotCallingOther";
        String data = p.instantiationTxKeyword + p.fieldSeparator + p.newAndInvocation + p.fieldSeparator + smartContract + p.fieldSeparator + varietyName
                + p.fieldSeparator + "setLoopTimes" + p.fieldSeparator + "5";
        System.out.println("[Test][Debug] test data is " + data);
        Transaction tx = createTransaction("from", "to", 10, data);
        Block block = createBlock(tx, null);

        prepareJarAndConfig(smartContract);
        p.tryAddNewBlock(block);
        ThreadHelper.safeSleepSecond(5);
    }

    @Test
    public void testDisposable() throws BlockException {
        InstantiationTransactionProcessor p = InstantiationTransactionProcessor.getInstance();
        String varietyName = "notCallingOtherVar1";
        String smartContract = "NotCallingOther";
        String data = p.instantiationTxKeyword + p.fieldSeparator + p.disposableInvocation + p.fieldSeparator + smartContract + p.fieldSeparator + varietyName
                + p.fieldSeparator + "checkLatestBlockTransactions" + p.fieldSeparator + "" + p.fieldSeparator + "setLoopTimes" + p.fieldSeparator + "5";
        System.out.println("[Test][Debug] test data is " + data);
        Transaction tx = createTransaction("from", "to", 10, data);
        Block block = createBlock(tx, null);

        prepareJarAndConfig(smartContract);
        p.tryAddNewBlock(block);
        ThreadHelper.safeSleepSecond(10);
    }

    @Test
    public void testDisposableEmptyAtLast() throws BlockException {
        InstantiationTransactionProcessor p = InstantiationTransactionProcessor.getInstance();
        String varietyName = "notCallingOtherVar1";
        String smartContract = "NotCallingOther";
        String data = p.instantiationTxKeyword + p.fieldSeparator + p.disposableInvocation + p.fieldSeparator + smartContract + p.fieldSeparator + varietyName
                + p.fieldSeparator + "setLoopTimes" + p.fieldSeparator + "10" + p.fieldSeparator + "checkLatestBlockTransactions" + p.fieldSeparator + "";
        System.out.println("[Test][Debug] test data is " + data);
        Transaction tx = createTransaction("from", "to", 10, data);
        Block block = createBlock(tx, null);

        prepareJarAndConfig(smartContract);
        p.tryAddNewBlock(block);
        ThreadHelper.safeSleepSecond(30);
    }

    Block createBlock(Transaction t, Block previousBlock) throws BlockException {
        String previousHash = null == previousBlock ? "" : previousBlock.getBlockHash();
        int nouce = null == previousBlock ? 0 : previousBlock.getBlockNounce() + 1;
        int index = null == previousBlock ? 0 : previousBlock.getBlockIndex() + 1;
        Block block = Block.constructBlock(previousHash, nouce, index);
        block.addTransaction(t);
        return block;
    }

    Transaction createTransaction(String from, String to, int value, String data) {
        Transaction transaction = new Transaction();
        transaction.setFrom(from);
        transaction.setToAndValue(to, value);
        transaction.setData(data);
        transaction.setId();
        transaction.setHash();
        return transaction;
    }

    void prepareJarAndConfig(String smartContractName){
        FileHelper.deleteFile("smartContract\\blockchain.jar");
        FileHelper.copyFileByForce("classes\\artifacts\\blockchain_jar\\blockchain.jar", "smartContract\\blockchain.jar");
        String scName = smartContractName;

        // configuration file
        String configFileName = SmartContractInstanceConfig.getConfigurationFullPath(scName);
        //prepare configuration file
        FileHelper.deleteFile(configFileName);

        String content =
                "[instantiation]\n" +
                        "isDelayedInstance = yes\n" +
                        "instanceMustMethods = setLoopTimes\n" +
                        "[command]\n" +
                        "Launcher=java -cp\n" +
                        "runCommand = blockchain.jar\n" +
                        "ClosePost = com.scu.suhong.instantiationOptimization.embed." + smartContractName;
        FileHelper.createFile(configFileName, content);
    }
}
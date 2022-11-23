package com.scu.suhong.block;

import account.AccountManager;
import com.scu.suhong.transaction.CrosschainTransaction;
import com.scu.suhong.transaction.Transaction;
import consensus.pow.MiningConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import util.ArrayHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockListBalanceProcessorTest {
    @Test
    public void processComparisonBalanceForCommonTransaction() throws BlockException {
        BlockListBalanceProcessor balanceProcessor = new BlockListBalanceProcessor();

        BlockListComparison comparison = new BlockListComparison();
        List<Block> oldBlockList = new ArrayList<>();
        List<Block> newBlockList = new ArrayList<>();
        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert comparison.getNewTxList().isEmpty();

        // Init balance
        String from = "from";
        String to = "to";
        AccountManager accountManager = AccountManager.getInstance();
        int fromInitValue = 159;
        int toInitValue = 87;
        accountManager.addValue(from, fromInitValue);
        accountManager.addValue(to, toInitValue);
        assert fromInitValue == accountManager.getBalance(from);
        assert toInitValue == accountManager.getBalance(to);

        // Create first block for old and new blocklist
        List<Transaction> tempTransactionList = new ArrayList<>();
        int value11 = 11;
        Transaction transaction11 = createTransaction(from, to, value11, "data11");
        tempTransactionList.add(transaction11);
        int value12 = 12;
        Transaction transaction12 = createTransaction(from, to, value12, "data12");
        tempTransactionList.add(transaction12);
        Block block = createBlock(tempTransactionList, null);
        oldBlockList.add(block);
        newBlockList.add(block);

        // Compare and recalculate
        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert comparison.getNewTxList().isEmpty();
        balanceProcessor.processComparisonBalance(comparison);
        // no change, and here is the base of the calcuation: t1 t2
        assert fromInitValue == accountManager.getBalance(from);
        assert toInitValue == accountManager.getBalance(to);

        // simulate the blockchain update,
        Collections.copy(oldBlockList, newBlockList);
        int value21 = 21;
        Transaction transaction21 = createTransaction(from, to, value21, "data21");
        Block tempBlock = createBlock(transaction21, block);
        newBlockList.add(tempBlock);
        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert 1 == comparison.getNewTxList().size();
        balanceProcessor.processComparisonBalance(comparison);
        assert fromInitValue - value21 == accountManager.getBalance(from);
        assert toInitValue + value21 == accountManager.getBalance(to);

        // simulate the blockchain update,
        oldBlockList = ArrayHelper.copy(newBlockList); //  old: (t1 t2) (t3)
        newBlockList = new ArrayList<>();
        newBlockList.add(tempBlock = createBlock(transaction11, null));
        newBlockList.add(Block.constructEmptyBlock());
        newBlockList.add(Block.constructEmptyBlock());
        comparison.compare(oldBlockList, newBlockList);// new: (t1) () ()
        assert 2 == comparison.getDisappearedTxList().size();
        assert comparison.getNewTxList().isEmpty();
        balanceProcessor.processComparisonBalance(comparison);
        assert fromInitValue + value12 == accountManager.getBalance(from);
        assert toInitValue - value12 == accountManager.getBalance(to);

        // simulate the blockchain update,
        oldBlockList = ArrayHelper.copy(newBlockList); //  old: (t1) () ()
        // init value: old t1 12, new t1 t2(t1) () ()
        newBlockList.add(createBlock(transaction21, tempBlock));
        comparison.compare(oldBlockList, newBlockList);// old: (t1) () () new : (t1) () (t3)
        assert 0 == comparison.getDisappearedTxList().size();
        assert 1 == comparison.getNewTxList().size();
        balanceProcessor.processComparisonBalance(comparison);
        assert fromInitValue + value12 - value21 == accountManager.getBalance(from);
        assert toInitValue - value12 + value21 == accountManager.getBalance(to);
    }

    @Test
    public void processComparisonBalanceForCrosschainTransaction() throws BlockException {
        BlockListBalanceProcessor balanceProcessor = new BlockListBalanceProcessor();
        MiningConfiguration.setDifficulty(0);
        BlockChain blockchain = BlockChain.getInstance();
        List<Block> blockList = blockchain.getBlockList();

        int blockchainId = 123;
        int interactionId = 10;
        String from = "1_from";
        String to = "2_to";
        String fromLocal = "1_from";
        String toLocal = "2_to";
        int value = 10;
        CrosschainTransaction crosschainTxInBlockchain1 = createCrosschainTransaction(blockchainId, interactionId, from, to, value);
        // From network, one crosschainTx only know one requiredcrosschainTx. It cannot have the whole map
        CrosschainTransaction crosschainTxInBlockchain1_json = createCrosschainTransaction(blockchainId, interactionId, from, to, value);

        blockchainId = 234;
        interactionId = 10;
        from = "222?1_from";
        to = "222?2_to";
        value = 10;
        CrosschainTransaction crosschainTxInBlockchain2 = createCrosschainTransaction(blockchainId, interactionId, from, to, value);
        CrosschainTransaction crosschainTxInBlockchain2_json = createCrosschainTransaction(blockchainId, interactionId, from, to, value);

        blockchainId = 345;
        interactionId = 10;
        from = "333?1234_from";
        to = "333?1234_to";
        value = 10;
        CrosschainTransaction crosschainTxInBlockchain3 = createCrosschainTransaction(blockchainId, interactionId, from, to, value);
        CrosschainTransaction crosschainTxInBlockchain3_json = createCrosschainTransaction(blockchainId, interactionId, from, to, value);

        crosschainTxInBlockchain1.addRequiredCrosschainTransaction(crosschainTxInBlockchain2_json);
        crosschainTxInBlockchain2.addRequiredCrosschainTransaction(crosschainTxInBlockchain3_json);
        crosschainTxInBlockchain3.addRequiredCrosschainTransaction(crosschainTxInBlockchain1_json);

        BlockListComparison comparison = new BlockListComparison();
        List<Block> oldBlockList = new ArrayList<>();
        List<Block> newBlockList = new ArrayList<>();
        blockchain.testSetBlockChain(oldBlockList);
        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert comparison.getNewTxList().isEmpty();

        // Init balance
        AccountManager accountManager = AccountManager.getInstance();
        int fromInitValue = 159;
        int toInitValue = 87;
        accountManager.addValue(fromLocal, fromInitValue);
        accountManager.addValue(toLocal, toInitValue);
        assert fromInitValue == accountManager.getBalance(fromLocal);
        assert toInitValue == accountManager.getBalance(toLocal);

        // Create first block for old and new blocklist
        List<Transaction> tempTransactionList = new ArrayList<>();
        tempTransactionList.add(crosschainTxInBlockchain2);
        Block block = createBlock(tempTransactionList, null);
        oldBlockList.add(block);
        newBlockList.add(block);

        blockchain.testSetBlockChain(oldBlockList);
        // Compare and recalculate
        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert comparison.getNewTxList().isEmpty();
        balanceProcessor.processComparisonBalance(comparison);
        // no change, and here is the base of the calcuation: t2
        assert fromInitValue == accountManager.getBalance(fromLocal);
        assert toInitValue == accountManager.getBalance(toLocal);

        newBlockList.add(block = createBlock(crosschainTxInBlockchain1, block));// new t2 : t1
        blockchain.testSetBlockChain(oldBlockList);
        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert 1 == comparison.getNewTxList().size();
        balanceProcessor.processComparisonBalance(comparison);
        // no change, and here is the base of the calcuation: t1 t2
        assert fromInitValue - value == accountManager.getBalance(fromLocal);
        assert toInitValue == accountManager.getBalance(toLocal);


        // simulate the blockchain update,
        oldBlockList = ArrayHelper.copy(newBlockList);// old t2 : t1
        Block tempBlock = createBlock(crosschainTxInBlockchain3, block);
        newBlockList.add(tempBlock); // new : t2 : t1 : t3
        blockchain.testSetBlockChain(oldBlockList);
        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert 1 == comparison.getNewTxList().size();
        balanceProcessor.processComparisonBalance(comparison);
        assert fromInitValue - value == accountManager.getBalance(fromLocal);
        assert toInitValue == accountManager.getBalance(toLocal);

        // simulate the blockchain update,
        oldBlockList = ArrayHelper.copy(newBlockList); //  old: (t1 t2) (t3)
        newBlockList = new ArrayList<>();
        newBlockList.add(tempBlock = createBlock(crosschainTxInBlockchain2, null));
        newBlockList.add(Block.constructEmptyBlock());
        newBlockList.add(Block.constructEmptyBlock());
        blockchain.testSetBlockChain(oldBlockList);
        comparison.compare(oldBlockList, newBlockList);// new: (t2) () ()
        assert 2 == comparison.getDisappearedTxList().size();
        assert comparison.getNewTxList().isEmpty();
        balanceProcessor.processComparisonBalance(comparison);
        assert fromInitValue == accountManager.getBalance(fromLocal);
        assert toInitValue == accountManager.getBalance(toLocal);

        // simulate the blockchain update,
        newBlockList = ArrayHelper.copy(oldBlockList); //  old: (t1 t2) (t3)
        comparison.compare(oldBlockList, newBlockList);// old: (t1 t2) (t3) new : (t1 t2) (t3)
        assert 0 == comparison.getDisappearedTxList().size();
        assert 0 == comparison.getNewTxList().size();
        balanceProcessor.processComparisonBalance(comparison);
        assert fromInitValue == accountManager.getBalance(fromLocal);
        assert toInitValue == accountManager.getBalance(toLocal);

        // simulate the blockchain update,
        newBlockList = ArrayHelper.copy(oldBlockList); //  new: (t1 t2) (t3)
        oldBlockList = new ArrayList<>();
        comparison.compare(oldBlockList, newBlockList);// old:   new : (t1 t2) (t3)
        assert 0 == comparison.getDisappearedTxList().size();
        assert 3 == comparison.getNewTxList().size();
        balanceProcessor.processComparisonBalance(comparison);
        assert fromInitValue - value == accountManager.getBalance(fromLocal);
        assert toInitValue + value == accountManager.getBalance(toLocal);
    }

    Block createBlock(List<Transaction> transactionList, Block previousBlock) throws BlockException {
        String previousHash = null == previousBlock ? "" : previousBlock.getBlockHash();
        int nouce = null == previousBlock ? 0 : previousBlock.getBlockNounce() + 1;
        int index = null == previousBlock ? 0 : previousBlock.getBlockIndex() + 1;
        Block block = Block.constructBlock(previousHash, nouce, index);
        for (Transaction t : transactionList) {
            block.addTransaction(t);
        }
        return block;
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
        transaction.setHash();
        return transaction;
    }

    @NotNull
    private CrosschainTransaction createCrosschainTransaction(int blockchainId, int interactionId,
                                                              String from, String to, int value) {
        CrosschainTransaction requiredcrosschainTx = new CrosschainTransaction(blockchainId, interactionId);
        requiredcrosschainTx.setFrom(from);
        requiredcrosschainTx.setToAndValue(to, value);
        return requiredcrosschainTx;
    }
}
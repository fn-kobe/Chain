package com.scu.suhong.block;

import com.scu.suhong.dynamic_definition.TransactionHelper;
import com.scu.suhong.transaction.CrosschainTransaction;
import com.scu.suhong.transaction.Transaction;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BlockListComparisonTest {
    @Test
    public void compare() throws BlockException {
        BlockListComparison comparison = new BlockListComparison();
        List<Block> oldBlockList = new ArrayList<>();
        List<Block> newBlockList = new ArrayList<>();
        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert comparison.getNewTxList().isEmpty();

        List<Transaction> tempTransactionList = new ArrayList<>();
        Transaction transaction11 = createExternalCrosschainTransaction("from11", "to11", 11, "data11");
        tempTransactionList.add(transaction11);
        Transaction transaction12 = createExternalCrosschainTransaction("from12", "to12", 12, "data12");
        tempTransactionList.add(transaction12);

        Block block = createBlock(tempTransactionList, null);
        oldBlockList.add(block);
        newBlockList.add(block);

        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert comparison.getNewTxList().isEmpty();

        Transaction transaction21 = createExternalCrosschainTransaction("from21", "to21", 21, "data21");
        Block tempBlock = createBlock(transaction21, block);
        newBlockList.add(tempBlock);
        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert 1 == comparison.getNewTxList().size();

        newBlockList = new ArrayList<>();
        newBlockList.add(tempBlock = createBlock(transaction11, null));
        comparison.compare(oldBlockList, newBlockList);// old t11 t12 ;new t11
        assert 1 == comparison.getDisappearedTxList().size();
        assert comparison.getNewTxList().isEmpty();

        newBlockList.add(createBlock(transaction21, tempBlock));
        comparison.compare(oldBlockList, newBlockList); ;// old t11 t12 ;new t11 t21
        assert 1 == comparison.getDisappearedTxList().size();
        assert 1 == comparison.getNewTxList().size();
    }

    @Test
    public void removeBothNewAndDisappearedTx() throws BlockException {
        BlockListComparison comparison = new BlockListComparison();
        List<Block> oldBlockList = new ArrayList<>();
        List<Block> newBlockList = new ArrayList<>();
        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert comparison.getNewTxList().isEmpty();

        List<Transaction> tempTransactionList = new ArrayList<>();
        Transaction transaction11 = createExternalCrosschainTransaction("from11", "to11", 11, "data11");
        tempTransactionList.add(transaction11);
        Transaction transaction12 = createExternalCrosschainTransaction("from12", "to12", 12, "data12");
        tempTransactionList.add(transaction12);

        Block block = createBlock(tempTransactionList, null);
        oldBlockList.add(block);
        newBlockList.add(block = createBlock(transaction11, null));
        newBlockList.add(createBlock(transaction12, block));

        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert comparison.getNewTxList().isEmpty();
    }

    @Test
    public void compareWithDisappearTx() throws BlockException {
        BlockListComparison comparison = new BlockListComparison();
        List<Block> oldBlockList = new ArrayList<>();
        List<Block> newBlockList = new ArrayList<>();
        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert comparison.getNewTxList().isEmpty();

        List<Transaction> tempTransactionList = new ArrayList<>();
        Transaction transaction11 = createExternalCrosschainTransaction("from11", "to11", 11, "data11");
        tempTransactionList.add(transaction11);
        Transaction transaction12 = createExternalCrosschainTransaction("from12", "to12", 12, "data12");
        tempTransactionList.add(transaction12);

        Block block = createBlock(tempTransactionList, null);
        oldBlockList.add(block);
        newBlockList.add(block);

        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert comparison.getNewTxList().isEmpty();

        Transaction transaction21_disappear = createExternalCrosschainTransaction("from21", "to21", 21, "data21");
        TransactionHelper.markAsDisappeared(transaction21_disappear);
        Block tempBlock = createBlock(transaction21_disappear, block);
        newBlockList.add(tempBlock);
        comparison.compare(oldBlockList, newBlockList);
        assert comparison.getDisappearedTxList().isEmpty();
        assert 0 == comparison.getNewTxList().size();

        newBlockList = new ArrayList<>();
        newBlockList.add(tempBlock = createBlock(transaction11, null));
        comparison.compare(oldBlockList, newBlockList);
        assert 1 == comparison.getDisappearedTxList().size();
        assert comparison.getNewTxList().isEmpty();

        Transaction transaction21 = createExternalCrosschainTransaction("from21", "to21", 21, "data21");
        newBlockList.add(createBlock(transaction21, tempBlock));
        comparison.compare(oldBlockList, newBlockList);
        assert 1 == comparison.getDisappearedTxList().size();
        assert 1 == comparison.getNewTxList().size();

        oldBlockList = new ArrayList<>();
        newBlockList = new ArrayList<>();
        oldBlockList.add(createBlock(transaction21_disappear, null));// old transaction21_disappear new :
        newBlockList.add(tempBlock = createBlock(transaction21_disappear, null));
        comparison.compare(oldBlockList, newBlockList);
        assert 0 == comparison.getDisappearedTxList().size();
        assert 0 == comparison.getNewTxList().size();

        newBlockList.add(tempBlock = createBlock(transaction21, tempBlock));
        comparison.compare(oldBlockList, newBlockList);
        assert 0 == comparison.getDisappearedTxList().size();
        assert 1 == comparison.getNewTxList().size();

        Transaction transaction21_disappear_2 = createExternalCrosschainTransaction("from21", "to21", 21, "data21");
        TransactionHelper.markAsRebranchDisappeared(transaction21_disappear_2);
        newBlockList.add(createBlock(transaction21_disappear_2, tempBlock));
        comparison.compare(oldBlockList, newBlockList);
        comparison.markTransactionAsDisappearedWhenNeighborRebranch(newBlockList);
        comparison.markTransactionAsDisappearedWhenNeighborRebranch(oldBlockList);
        comparison.compare(oldBlockList, newBlockList);
        assert 0 == comparison.getDisappearedTxList().size();
        assert 0 == comparison.getNewTxList().size();
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

    CrosschainTransaction createExternalCrosschainTransaction(String from, String to, int value, String data) {
        int blockchainId = 9090;
        int interactionId = 10;
        return createCrosschainTransaction(blockchainId, interactionId, from, to, value, data);
    }

    CrosschainTransaction createCrosschainTransaction(int blockchainId, int interactionId,
                                          String from, String to, int value, String data) {
        CrosschainTransaction crosschainTransaction = new CrosschainTransaction(blockchainId, interactionId);
        crosschainTransaction.setFrom(from);
        crosschainTransaction.setToAndValue(to, value);
        crosschainTransaction.setData(data);
        crosschainTransaction.setHash();
        crosschainTransaction.setId();
        return crosschainTransaction;
    }

}
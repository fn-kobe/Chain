package com.scu.suhong.instantiationOptimization;

import com.scu.suhong.block.Block;
import com.scu.suhong.block.BlockChain;
import com.scu.suhong.block.BlockException;
import com.scu.suhong.transaction.Transaction;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class RequestProcessingUtilityTest {

    @Test
    public void waitStateToBeConfirmed() throws BlockException {
        DummyRequestProvider requestProvider = new DummyRequestProvider();
        RequestProcessingUtility requestProcessingUtility = new RequestProcessingUtility(requestProvider);
        List<String> stateList = new ArrayList<>();
        stateList.add("s1");
        stateList.add("s2");
        BlockChain blockChain = BlockChain.getInstance();
        List<Block> testBlockList = new ArrayList<>();

        testBlockList.add(createBlock(null, null));
        blockChain.testSetBlockChain(testBlockList);
        assert !requestProcessingUtility.doesBlockContainsAllStates(stateList);

        testBlockList.add(createBlock(createTransaction("from", "to", 10, "This s1 state"), null));
        blockChain.testSetBlockChain(testBlockList);
        assert !requestProcessingUtility.doesBlockContainsAllStates(stateList);

        testBlockList.add(createBlock(createTransaction("from", "to", 10, "This s2 state"), null));
        blockChain.testSetBlockChain(testBlockList);
        assert requestProcessingUtility.doesBlockContainsAllStates(stateList);
    }

    class DummyRequestProvider implements RequestProviderInterface {
        @Override
        public String getIdentify() {
            return "Dummy";
        }
    }


    Block createBlock(Transaction t, Block previousBlock) throws BlockException {
        String previousHash = null == previousBlock ? "" : previousBlock.getBlockHash();
        int nouce = null == previousBlock ? 0 : previousBlock.getBlockNounce() + 1;
        int index = null == previousBlock ? 0 : previousBlock.getBlockIndex() + 1;
        Block block = Block.constructBlock(previousHash, nouce, index);
        if (null != t) {
            block.addTransaction(t);
        }
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
}
package com.scu.suhong.block;

import com.scu.suhong.transaction.Transaction;
import consensus.pow.PoW;
import consensus.pow.PoWException;
import junit.framework.TestCase;

import java.util.Date;
import java.util.List;

public class BlockDBHandlerTest extends TestCase {
    BlockDBHandler blockDBHandler = BlockDBHandler.getInstance();

    public void testSaveWholeBlockchain() throws BlockException, PoWException {
        BlockChain.reset();// update blockchain
        BlockChain blockChain = BlockChain.getInstance();
        List<Block> originalBlockList = blockChain.getBlockList();

        int originalChainSize = originalBlockList.size();
        Block originalTopBlock = originalBlockList.get(originalChainSize-1);
        Block newBlock = constructNewBlock(originalTopBlock);
        blockChain.addBlock(newBlock);

        blockDBHandler.save(blockChain.blockList);
        List<Block> newBlockList = blockDBHandler.loadChainList();

        int newChainSize = newBlockList.size();
        Block newTopBlock = newBlockList.get(newChainSize-1);
        System.out.println("The blockchain size is " + newChainSize + " : old size: " + originalChainSize);
        assert(newBlock.getBlockHash().equals(newTopBlock.getBlockHash()));
        assert((originalChainSize + 1) == newChainSize);

        blockDBHandler.getTopBlockHashDB();
    }

    //After testSaveWholeBlockchain, as the DB is not empty then
    public void testSaveTopBlockBlockchain() throws BlockException, PoWException {
        BlockChain blockChain = BlockChain.getInstance();
        List<Block> originalBlockList = blockChain.getBlockList();

        int originalChainSize = originalBlockList.size();
        Block originalTopBlock = originalBlockList.get(originalChainSize-1);
        Block newBlock = constructNewBlock(originalTopBlock);
        blockChain.addBlock(newBlock);
        blockDBHandler.saveTopBlock(newBlock);

        List<Block> newBlockList = blockDBHandler.loadChainList();

        int newChainSize = newBlockList.size();
        Block newTopBlock = newBlockList.get(newChainSize-1);
        System.out.println("The blockchain size is " + newChainSize + " : old size: " + originalChainSize);
        assert(newBlock.getBlockHash().equals(newTopBlock.getBlockHash()));
        assert((originalChainSize + 1) == newChainSize);

        blockDBHandler.getTopBlockHashDB();
    }

    public void testSaveTopBlock() throws BlockException {
        Block block = new Block(new BlockHeader(), new BlockBody());
        Transaction transaction = new Transaction();
        transaction.setData("transaction Data in test save top block");
        transaction.setHash();
        block.getBody().addTransaction(transaction);
        block.setPreviousHash(BlockChain.getInstance().getTopBlockHash());
        block.setBlockNounce(PoW.safeFindBlockNounce(block.getPreviousHash(), block.getTransactionHash()));
        blockDBHandler.saveTopBlock(block);

        List<Block> blockList = blockDBHandler.loadChainList();
        Block loadBlock = blockList.get(blockList.size()-1);
        assert(loadBlock.getBlockHash().equals(block.getBlockHash()));
    }

    public void testSaveBlock() throws BlockException {
        Block block = new Block(new BlockHeader(), new BlockBody());
        Transaction transaction = new Transaction();
        transaction.setData("123");
        transaction.setHash();
        block.getBody().addTransaction(transaction);
        block.setBlockNounce(PoW.safeFindBlockNounce(block.getPreviousHash(), block.getTransactionHash()));
        blockDBHandler.save(block);

        Block loadBlock = blockDBHandler.loadBlock(block.getBlockHash());
        assert(loadBlock.getBlockHash().equals(block.getBlockHash()));
    }

    private Block constructGenesesBlock() {
        Block genesesBlock = constructEmptyBlock();
        genesesBlock.setBlockNounce(1951040714);
        return genesesBlock;
    }

    private Block constructNewBlock(Block previousBlock) throws BlockException, PoWException {
        Block newBlock = constructEmptyBlock();

        // add the header
        newBlock.setPreviousHash(previousBlock.getBlockHash());
        // add transaction
        Transaction transaction = new Transaction();
        transaction.setData("The new Block" + new Date().toString());
        transaction.setHash();
        newBlock.getBody().addTransaction(transaction);
        // find the nounce
        int blockNounce = PoW.findBlockNounce(newBlock.getPreviousHash(), newBlock.getTransactionHash());
        if (0 != blockNounce) {
            newBlock.setBlockNounce(blockNounce);
            System.out.printf("Nounce %d found with hash: %s\n", blockNounce, newBlock.getBlockHash());
        } else {
            System.out.println("No nounce found, please decrease the difficulty");
        }
        return newBlock;
    }

    @org.jetbrains.annotations.NotNull
    private Block constructEmptyBlock() {
        BlockHeader h = new BlockHeader(); // no previous block hash
        BlockBody b = new BlockBody(); // no transaction
        return new Block(h, b);
    }
}
package com.scu.suhong.block;

import com.scu.suhong.network.P2PConfiguration;
import com.scu.suhong.transaction.Transaction;
import consensus.pow.MiningConfiguration;
import consensus.pow.PoW;
import consensus.pow.PoWException;
import junit.framework.TestCase;

public class BlockCheckerTest extends TestCase {

    public void testGenesesBlockCheck() throws BlockException { // test geneses block
        Block genesesBlock = constructGenesesBlock();
        assert(MiningConfiguration.isHashMatched(genesesBlock.getBlockHash()));
    }

    public void testSecondBlockCheck() throws BlockException, PoWException { // test 2nd block
        BlockChain blockChain = new BlockChain();
        Block genesesBlock = constructGenesesBlock();
        assert(blockChain.addBlock(genesesBlock));

        Block secondBlock = constructSecondBlock(genesesBlock);
        MiningConfiguration.testSetRequiredZeroCount(10);
        P2PConfiguration.getInstance().testSetRequiredZeroCount("", 3);
        assert(!BlockChecker.isMatchBlockchain(blockChain, secondBlock, false));
        assert(BlockChecker.isMatchBlockchain(blockChain, secondBlock, true));
    }

    public void testBlockCheckExternal() throws BlockException, PoWException { // test 2nd block
        MiningConfiguration.getRequiredZeroCount();
        BlockChain blockChain = new BlockChain();
        Block genesesBlock = constructGenesesBlock();
        assert(blockChain.addBlock(genesesBlock));

        Block secondBlock = constructSecondBlock(genesesBlock);
        assert(BlockChecker.isMatchBlockchain(blockChain, secondBlock, false));
    }

    private Block constructGenesesBlock() {
        Block genesesBlock = constructEmptyBlock();
        genesesBlock.setBlockNounce(1951040714);
        return genesesBlock;
    }

    private Block constructSecondBlock(Block genesesBlock) throws BlockException, PoWException {
        Block secondBlock = constructEmptyBlock();

        // add the header
        secondBlock.setPreviousHash(genesesBlock.getBlockHash());
        // add transaction
        Transaction transaction = new Transaction();
        transaction.setFrom("from");
        transaction.setData("The Second Block");
        transaction.setHash();
        secondBlock.getBody().addTransaction(transaction);
        // find the nounce
        int blockNounce = PoW.findBlockNounce(secondBlock.getPreviousHash(), secondBlock.getTransactionHash());
        if (0 != blockNounce){
            secondBlock.setBlockNounce(blockNounce);
            System.out.printf("Nounce %d found with hash: %s\n", blockNounce,secondBlock.getBlockHash() );
        } else {
            System.out.println("No nounce found, please decrease the difficulty");
        }

        return secondBlock;
    }

    @org.jetbrains.annotations.NotNull
    private Block constructEmptyBlock() {
        BlockHeader h = new BlockHeader(); // no previous block hash
        BlockBody b = new BlockBody(); // no transaction
        Transaction t = new Transaction();
        t.setData("Geneses transaction");
        t.setHash();
        return new Block(h, b);
    }
}
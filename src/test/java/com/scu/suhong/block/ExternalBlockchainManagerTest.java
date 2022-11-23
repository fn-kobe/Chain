package com.scu.suhong.block;

import Service.BlockchainService;
import Service.BlockchainServiceThread;
import account.AccountManager;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.miner.Miner;
import com.scu.suhong.network.P2PConfiguration;
import com.scu.suhong.transaction.CrosschainInterface;
import com.scu.suhong.transaction.CrosschainTransaction;
import consensus.pow.MiningConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import util.ThreadHelper;

import java.util.ArrayList;
import java.util.List;

public class ExternalBlockchainManagerTest {
    @Test
    public void testExternalBlockCheck() throws BlockException { // test 3rd block
        String exChainId = "999";
        MiningConfiguration.setDifficulty(0);
        P2PConfiguration.getInstance().testSetRequiredZeroCount(exChainId, 0);

        BlockchainService blockchainService = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchainService);
        Thread mainThread = new Thread(blockchainServiceThread, "Block chain test thread");
        mainThread.start();
        ThreadHelper.safeSleep(2 * 1000);
        while (null == blockchainService.getMiner()) {
            ThreadHelper.safeSleep(500);
        }
        Miner miner = blockchainService.getMiner();
        miner.setRunInterval(1);

        BlockChain externalBlockchain = ExternalBlockchainManager.getWorker(exChainId);
        BlockChain blockchain = BlockChain.getInstance();
        ArrayList<Block> blockList = new ArrayList<>();
        blockList.add(constructGenesisBlock());
        externalBlockchain.init(blockList);
        blockchain.init(blockList);

        String from1 = "from1";
        String to1 = "to1";
        int from1InitValue = 60;
        AccountManager accountManager = AccountManager.getInstance();
        accountManager.addValue(from1, from1InitValue);
        CrosschainTransaction t1 = createCrosschainTransaction(MiningConfiguration.getBlockchainId(), 10, "from1", "to1", 10);

        assert !isFoundInBlockchain(externalBlockchain, t1);
        assert !isFoundInBlockchain(blockchain, t1);
        assert from1InitValue == accountManager.getBalance(from1);
        assert 0 == accountManager.getBalance(to1);

        Block block = packageTransactionToBlock(t1, externalBlockchain.getLatestBlock());
        assert (externalBlockchain.addBlock(block));
        ThreadHelper.safeSleep(2 * 1000);
        assert isFoundInBlockchain(externalBlockchain, t1);
        assert !isFoundInBlockchain(blockchain, t1);

        miner.addTransaction(t1);
        ThreadHelper.safeSleep(2 * 1000);
        assert isFoundInBlockchain(externalBlockchain, t1);
        assert isFoundInBlockchain(blockchain, t1);

        CrosschainTransaction t2 = createCrosschainTransaction(555, 10, "from3", "to3", 10);
        assert !isFoundInBlockchain(externalBlockchain, t2);
        assert !isFoundInBlockchain(blockchain, t2);

        List<CrosschainTransaction> crosschainTransactionList = new ArrayList<>();
        crosschainTransactionList.add(t1);
        crosschainTransactionList.add(t2);
        block = packageTransactionToBlock(crosschainTransactionList, block);
        assert (externalBlockchain.addBlock(block));
        ThreadHelper.safeSleep(2 * 1000);

        assert isFoundInBlockchain(externalBlockchain, t2);
        assert isFoundInBlockchain(blockchain, t2);
        assert from1InitValue - 10 == accountManager.getBalance(from1);
        assert 0 == accountManager.getBalance(to1);

        CrosschainTransaction t3 = createCrosschainTransaction(777, 10, "from3", "to3", 10);
        t1.addRequiredCrosschainTransaction(t2);
        t2.addRequiredCrosschainTransaction(t3);
        t3.addRequiredCrosschainTransaction(t1);
        assert !isFoundInBlockchain(externalBlockchain, t3);
        assert !isFoundInBlockchain(blockchain, t3);

        crosschainTransactionList = new ArrayList<>();
        crosschainTransactionList.add(t1);
        crosschainTransactionList.add(t3);
        block = packageTransactionToBlock(crosschainTransactionList, block);
        assert (externalBlockchain.addBlock(block));
        ThreadHelper.safeSleep(2 * 1000);

        System.out.println(externalBlockchain.dump());
        System.out.println(blockchain.dump());
        assert isFoundInBlockchain(externalBlockchain, t3);
        assert isFoundInBlockchain(blockchain, t3);
        assert from1InitValue - 10 == accountManager.getBalance(from1);
        assert 10 == accountManager.getBalance(to1);
    }

    public boolean isFoundInBlockchain(BlockChain blockChain, CrosschainTransaction t1) {
        boolean found = false;
        for (Block b : blockChain.getBlockList()) {
            for (AbstractTransaction t : b.getTransactions()) {
                if (t instanceof CrosschainInterface) {
                    if (CrosschainTransaction.isTransactionSameInExternalCase((CrosschainTransaction) t, t1))
                        found = true;
                }
            }

        }
        return found;
    }

    private Block constructGenesisBlock() {
        Block genesisBlock = Block.constructEmptyBlock();
        genesisBlock.setBlockNounce(1951040714);
        genesisBlock.setBlockIndexAndMiningTime(0);
        return genesisBlock;
    }

    @NotNull
    private Block packageTransactionToBlock(List<CrosschainTransaction> crosschainTransactionList, Block previousBlock) throws BlockException {
        MiningConfiguration.setDifficulty(0);// no diffcult to make our block add successfully
        Block block = Block.constructEmptyBlock();
        for (CrosschainTransaction crosschainTransaction : crosschainTransactionList) {
            block.addTransaction(crosschainTransaction);
        }
        block.setBlockIndexAndMiningTime(previousBlock.getBlockIndex() + 1);
        block.setBlockNounce(0000000);
        if (null != previousBlock) block.setPreviousHash(previousBlock.getBlockHash());
        System.out.println("[Test] Block hash: " + block.getBlockHash());
        return block;
    }

    @NotNull
    private Block packageTransactionToBlock(CrosschainTransaction crosschainTransaction, Block previousBlock) throws BlockException {
        MiningConfiguration.setDifficulty(0);// no diffcult to make our block add successfully
        Block block = Block.constructEmptyBlock();
        block.addTransaction(crosschainTransaction);
        block.setBlockIndexAndMiningTime(previousBlock.getBlockIndex() + 1);
        block.setBlockNounce(0000000);
        if (null != previousBlock) block.setPreviousHash(previousBlock.getBlockHash());
        System.out.println("[Test] Block hash: " + block.getBlockHash());
        return block;
    }

    @NotNull
    private CrosschainTransaction createCrosschainTransaction(int blockchainId, int interactionId,
                                                              String from, String to, int value) {
        CrosschainTransaction crosschainTransaction = new CrosschainTransaction(blockchainId, interactionId);
        crosschainTransaction.setFrom(from);
        crosschainTransaction.setToAndValue(to, value);
        crosschainTransaction.setId();
        return crosschainTransaction;
    }
}
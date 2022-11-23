package com.scu.suhong.transaction;

import Service.BlockchainService;
import Service.BlockchainServiceThread;
import account.AccountManager;
import com.scu.suhong.block.Block;
import com.scu.suhong.block.BlockChain;
import com.scu.suhong.block.BlockException;
import com.scu.suhong.block.ExternalBlockchainManager;
import com.scu.suhong.miner.Miner;
import com.scu.suhong.network.P2PConfiguration;
import consensus.pow.MiningConfiguration;
import consensus.pow.PoWException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import util.ThreadHelper;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class CrosschainTransactionThreadTest {
    @Test
    public void processExternalTransaction() throws BlockException, PoWException {
        CrosschainTransactionThread crosschainTransactionThread = CrosschainTransactionThread.getInstance();

        final String currentChainSenderAddress = "1_from";// we only give short name for inner blockchain transaction
        final String currentChainReceiverAddress = "2_to";
        final int currentChainSenderInitValue = 20;
        final int currentChainSenderUsedValue = 10;
        int blockchainId = 123;
        int interactionId = 10;
        String from = currentChainSenderAddress;//"0x123_1_from";
        String to = currentChainReceiverAddress;
        int value = currentChainSenderUsedValue;
        CrosschainTransaction exTxInBlockchain1 = createExternalTransaction(blockchainId, interactionId, from, to, value);
        AccountManager.getInstance().addValue(from, currentChainSenderInitValue);
        assert currentChainSenderInitValue == AccountManager.getInstance().getBalance(currentChainSenderAddress);

        blockchainId = 234;
        interactionId = 10;
        from = "234?1_from";
        to = "234?2_to";
        value = 10;
        CrosschainTransaction exTxInBlockchain2 = createExternalTransaction(blockchainId, interactionId, from, to, value);

        blockchainId = 345;
        interactionId = 10;
        from = "345?1_from";
        to = "345?_2to";
        value = 10;
        CrosschainTransaction exTxInBlockchain3 = createExternalTransaction(blockchainId, interactionId, from, to, value);

        exTxInBlockchain1.addRequiredCrosschainTransaction(exTxInBlockchain2);
        exTxInBlockchain2.addRequiredCrosschainTransaction(exTxInBlockchain3);
        exTxInBlockchain3.addRequiredCrosschainTransaction(exTxInBlockchain1);
        assert 0 == crosschainTransactionThread.testGetExternalTransactionListSize();

        Miner miner = new Miner();
        String exChainId = "999";
        BlockChain exBlockChain = ExternalBlockchainManager.getWorker(exChainId);
        BlockChain inBlockChain = BlockChain.getInstance();
        exBlockChain.testRest();
        inBlockChain.testRest();
        exBlockChain.setMiner(miner);
        inBlockChain.setMiner(miner);

        System.out.println("\n[Test] begin to test sealTransactionToBlockChain");
        Block tempBlock = exBlockChain.getLatestBlock();// reset will create a genesis block
        assert currentChainSenderInitValue == AccountManager.getInstance().getBalance(currentChainSenderAddress);
        assert 0 == AccountManager.getInstance().getBalance(currentChainReceiverAddress);
        sealTransactionToBlockChain(inBlockChain, exTxInBlockchain1, inBlockChain.getLatestBlock());
        assert currentChainSenderInitValue - currentChainSenderUsedValue == AccountManager.getInstance().getBalance(currentChainSenderAddress);

        tempBlock = sealTransactionToBlockChain(exBlockChain, exTxInBlockchain2, exBlockChain.getLatestBlock());
        // Only external transaction needs mining to current blockchain list
        miner.testProcessNewTransaction();
        assert currentChainSenderInitValue - currentChainSenderUsedValue == AccountManager.getInstance().getBalance(currentChainSenderAddress);
        assert 0 == AccountManager.getInstance().getBalance(currentChainReceiverAddress);

        System.out.println("[Test] ExternalTransactionListSize: " + crosschainTransactionThread.testGetExternalTransactionListSize());
        assert 2 == crosschainTransactionThread.testGetExternalTransactionListSize();

        tempBlock = sealTransactionToBlockChain(exBlockChain, exTxInBlockchain3, exBlockChain.getLatestBlock());
        miner.testProcessNewTransaction();
        System.out.println("[Test] ExternalTransactionListSize: " + crosschainTransactionThread.testGetExternalTransactionListSize());
        assert currentChainSenderInitValue - currentChainSenderUsedValue == AccountManager.getInstance().getBalance(currentChainSenderAddress);
        assert AccountManager.getInstance().getBalance(currentChainReceiverAddress) - (currentChainSenderUsedValue) < 0.001;
        assert 0 == crosschainTransactionThread.testGetExternalTransactionListSize();
    }

    @Test
    public void processExternalTransactionWithExternalBlockchainInformation() throws BlockException, PoWException, UnknownHostException {
        CrosschainTransactionThread crosschainTransactionThread = CrosschainTransactionThread.getInstance();

        final String currentChainSenderAddress = "1_from";// we only give short name for inner blockchain transaction
        final String currentChainReceiverAddress = "2_to";
        final int currentChainSenderInitValue = 20;
        final int currentChainSenderUsedValue = 10;
        int blockchainId = 123;
        int interactionId = 10;
        String from = currentChainSenderAddress;//"0x123_1_from";
        String to = currentChainReceiverAddress;
        int value = currentChainSenderUsedValue;
        CrosschainTransaction exTxInBlockchain1 = createExternalTransaction(blockchainId, interactionId, from, to, value);
        AccountManager.getInstance().addValue(from, currentChainSenderInitValue);
        assert currentChainSenderInitValue == AccountManager.getInstance().getBalance(currentChainSenderAddress);

        blockchainId = 234;
        interactionId = 10;
        from = "234?1_from";
        to = "234?2_to";
        value = 10;
        CrosschainTransaction exTxInBlockchain2 = createExternalTransaction(blockchainId, interactionId, from, to, value);

        blockchainId = 345;
        interactionId = 10;
        from = "345?1_from";
        to = "345?_2to";
        value = 10;
        CrosschainTransaction exTxInBlockchain3 = createExternalTransaction(blockchainId, interactionId, from, to, value);

        exTxInBlockchain1.addRequiredCrosschainTransaction(exTxInBlockchain2);
        exTxInBlockchain2.addRequiredCrosschainTransaction(exTxInBlockchain3);
        exTxInBlockchain3.addRequiredCrosschainTransaction(exTxInBlockchain1);
        // Not started. so the list should be 0
        assert 0 == crosschainTransactionThread.testGetExternalTransactionListSize();

        BlockchainService blockchainService = BlockchainService.getInstance();
        BlockchainServiceThread serviceThread = new BlockchainServiceThread(blockchainService);
        Thread mainThread = new Thread(serviceThread, "Block chain main thread");
        mainThread.start();
        // Wait 2 seconds to make the para,eter init fully
        ThreadHelper.safeSleep(2*1000);

        String exChainId = "999";
        BlockChain exBlockChain = ExternalBlockchainManager.getWorker(exChainId);
        BlockChain inBlockChain = BlockChain.getInstance();
        exBlockChain.testRestBlockList();
        inBlockChain.testRestBlockList();
        Miner miner = BlockchainService.getInstance().getMiner();
        miner.setRunInterval(1);
        //set the miner period to be short for unit test

        System.out.println("\n[Test] begin to test sealTransactionToBlockChain");
        assert currentChainSenderInitValue == AccountManager.getInstance().getBalance(currentChainSenderAddress);
        assert 0 == AccountManager.getInstance().getBalance(currentChainReceiverAddress);

        System.out.println("\n[Test] begin to process the first transaction");
        sealTransactionToBlockChain(inBlockChain, exTxInBlockchain1, inBlockChain.getLatestBlock());
        assert currentChainSenderInitValue - currentChainSenderUsedValue == AccountManager.getInstance().getBalance(currentChainSenderAddress);

        System.out.println("\n[Test] begin to process the second transaction");
        byte[] ipAddr = new byte[]{(byte)192, (byte)168, 30, 1};
        InetAddress inetAddress = InetAddress.getByAddress(ipAddr);
        int port = 7501;
        P2PConfiguration.getInstance().testAddProducerPeerAddress(exChainId, inetAddress.getHostAddress(), port);
        Block externalBlockchainBlock = packageTransactionToBlock(exTxInBlockchain2, exBlockChain.getLatestBlock());
        miner.onNetworkMsg(externalBlockchainBlock.getJson().toString().getBytes(),inetAddress, port);
        // sleep for the miner to process the block list
        ThreadHelper.safeSleep(2 * 1000);
        assert currentChainSenderInitValue - currentChainSenderUsedValue == AccountManager.getInstance().getBalance(currentChainSenderAddress);
        assert 0 == AccountManager.getInstance().getBalance(currentChainReceiverAddress);

        System.out.println("\n[Test] begin to process the third transaction");
        externalBlockchainBlock = packageTransactionToBlock(exTxInBlockchain3, exBlockChain.getLatestBlock());
        miner.onNetworkMsg(externalBlockchainBlock.getJson().toString().getBytes(),inetAddress, port);
        // sleep for the miner to process the block list
        ThreadHelper.safeSleep(2 * 1000);
        assert currentChainSenderInitValue - currentChainSenderUsedValue == AccountManager.getInstance().getBalance(currentChainSenderAddress);
        assert currentChainSenderUsedValue == AccountManager.getInstance().getBalance(currentChainReceiverAddress);

//        System.out.println(inBlockChain.dump());
//        System.out.println(exBlockChain.dump());
    }

    @Test
    public void testInteractionMatch() throws BlockException {
        int blockchainId = 123;
        int interactionId = 10;
        String from = MiningConfiguration.getBlockchainId() + AccountManager.getAddressConnectSymbol() + "1_from";
        String to = MiningConfiguration.getBlockchainId() + AccountManager.getAddressConnectSymbol() + "2_to";
        String from1 = from;
        String to1 = to;
        int value = 10;
        int from1InitValue = 20;
        CrosschainTransaction exTxInBlockchain1 = createExternalTransaction(blockchainId, interactionId, from, to, value);
        // From network, one exTx only know one requiredExTx. It cannot have the whole map
        CrosschainTransaction exTxInBlockchain1_json = createExternalTransaction(blockchainId, interactionId, from, to, value);

        AccountManager accountManager = AccountManager.getInstance();
        accountManager.addValue(from1, from1InitValue);

        blockchainId = 234;
        interactionId = 10;
        from = "222" + AccountManager.getAddressConnectSymbol() + "1_from";
        to = "222" + AccountManager.getAddressConnectSymbol() + "2_to";
        value = 10;
        CrosschainTransaction exTxInBlockchain2 = createExternalTransaction(blockchainId, interactionId, from, to, value);
        CrosschainTransaction exTxInBlockchain2_json = createExternalTransaction(blockchainId, interactionId, from, to, value);

        blockchainId = 345;
        interactionId = 10;
        from = "333" + AccountManager.getAddressConnectSymbol() + "1_from";
        to = "333" + AccountManager.getAddressConnectSymbol() + "2_to";
        value = 10;
        CrosschainTransaction exTxInBlockchain3 = createExternalTransaction(blockchainId, interactionId, from, to, value);
        CrosschainTransaction exTxInBlockchain3_json = createExternalTransaction(blockchainId, interactionId, from, to, value);

        exTxInBlockchain1.addRequiredCrosschainTransaction(exTxInBlockchain2_json);
        exTxInBlockchain2.addRequiredCrosschainTransaction(exTxInBlockchain3_json);
        exTxInBlockchain3.addRequiredCrosschainTransaction(exTxInBlockchain1_json);

        MiningConfiguration.setDifficulty(0);
        CrosschainTransactionThread crosschainTransactionThread = new CrosschainTransactionThread();
        BlockChain blockChain = BlockChain.getInstance();

        System.out.println("\n[Test] begin to process the first transaction");
        Block tempBlock = packageTransactionToBlock(exTxInBlockchain1, BlockChain.getInstance().getLatestBlock());
        blockChain.getBlockList().add(tempBlock);
        crosschainTransactionThread.processExternalTransaction(exTxInBlockchain1);

        System.out.println("\n[Test] begin to process the second transaction");
        tempBlock = packageTransactionToBlock(exTxInBlockchain2, BlockChain.getInstance().getLatestBlock());
        blockChain.getBlockList().add(tempBlock);
        crosschainTransactionThread.processExternalTransaction(exTxInBlockchain2);

        System.out.println("\n[Test] begin to process the third transaction");
        tempBlock = packageTransactionToBlock(exTxInBlockchain3, BlockChain.getInstance().getLatestBlock());
        blockChain.getBlockList().add(tempBlock);
        crosschainTransactionThread.processExternalTransaction(exTxInBlockchain3);

        assert from1InitValue - value == accountManager.getBalance(from1);
        assert value == accountManager.getBalance(to1);
    }

    private Block sealTransactionToBlockChain(BlockChain blockChain,
                                              CrosschainTransaction crosschainTransaction, Block previousBlock) throws BlockException {
        MiningConfiguration.setDifficulty(0);// no diffcult to make our block add successfully
        Block block = packageTransactionToBlock(crosschainTransaction, previousBlock);
        blockChain.addBlock(block);
        return block;
    }

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
    private CrosschainTransaction createExternalTransaction(int blockchainId, int interactionId,
                                                            String from, String to, int value) {
        CrosschainTransaction crosschainTransaction = new CrosschainTransaction(blockchainId, interactionId);
        crosschainTransaction.setFrom(from);
        crosschainTransaction.setToAndValue(to, value);
        crosschainTransaction.setId();
        return crosschainTransaction;
    }
}
package com.scu.suhong.block;

import Service.BlockchainService;
import Service.BlockchainServiceThread;
import account.AccountManager;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.Condition;
import com.scu.suhong.transaction.ConditionalAssociationTransaction;
import com.scu.suhong.transaction.CrosschainTransaction;
import com.scu.suhong.transaction.Transaction;
import consensus.pow.MiningConfiguration;
import consensus.pow.PoW;
import consensus.pow.PoWException;
import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;
import util.ThreadHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BlockChainTest {

    @Test
    public void testThirdBlockCheck() throws BlockException, PoWException { // test 3rd block
        BlockChain blockChain = new BlockChain();
        Block genesesBlock = constructGenesisBlock();
        assert (blockChain.addBlock(genesesBlock));

        Block secondBlock = constructSecondBlock(genesesBlock);
        assert (blockChain.addBlock(secondBlock));

        Block thirdBlock = constructThirdBlock(secondBlock);
        assert (blockChain.addBlock(thirdBlock));
    }

    @Test
    public void testConstructGenesesBlock() {
        assert BlockChain.constructGenesisBlock().isBlockListValid();
    }

    private Block constructGenesisBlock() {
        Block genesisBlock = Block.constructEmptyBlock();
        genesisBlock.setBlockNounce(1951040714);
        genesisBlock.setBlockIndexAndMiningTime(0);
        return genesisBlock;
    }

    private Block constructSecondBlock(Block genesesBlock) throws BlockException, PoWException {
        Block secondBlock = constructEmptyBlock();

        // add the header
        secondBlock.setPreviousHash(genesesBlock.getBlockHash());
        // add transaction
        Transaction transaction = new Transaction();
        transaction.setFrom("0x1234");
        transaction.setData("The Second Block");
        transaction.setHash();
        secondBlock.getBody().addTransaction(transaction);
        // find the nounce
        int blockNounce = PoW.findBlockNounce(secondBlock.getPreviousHash(), secondBlock.getTransactionHash());
        if (0 != blockNounce) {
            secondBlock.setBlockNounce(blockNounce);
            System.out.printf("Nounce %d found with hash: %s\n", blockNounce, secondBlock.getBlockHash());
        } else {
            System.out.println("No nounce found, please decrease the difficulty");
        }

        return secondBlock;
    }

    private Block constructThirdBlock(Block secondBlock) throws BlockException, PoWException {
        Block thirdBlock = constructEmptyBlock();
        // add the header
        thirdBlock.setPreviousHash(secondBlock.getBlockHash());
        // add transaction
        Transaction transaction = new Transaction();
        transaction.setFrom("0x12345");//any from peerAddress is OK
        transaction.setData("The third Block transaction 1");
        transaction.setHash();
        thirdBlock.addTransaction(transaction);
        transaction = new Transaction();
        transaction.setFrom("0x12346");//any from peerAddress is OK
        transaction.setData("The third Block transaction 2");
        transaction.setHash();
        thirdBlock.addTransaction(transaction);
        // find the nounce
        int blockNounce = PoW.findBlockNounce(thirdBlock.getPreviousHash(), thirdBlock.getTransactionHash());
        if (0 != blockNounce) {
            thirdBlock.setBlockNounce(blockNounce);
            System.out.printf("Nounce %d found with hash: %s\n", blockNounce, thirdBlock.getBlockHash());
        } else {
            System.out.println("No nounce found, please decrease the difficulty");
        }

        return thirdBlock;
    }

    @org.jetbrains.annotations.NotNull
    private Block constructEmptyBlock() {
        BlockHeader h = new BlockHeader(); // no previous block hash
        BlockBody b = new BlockBody(); // no transaction
        return new Block(h, b);
    }

    @Test
    public void testGetBlockChain() {
        BlockChain.blockChainInstance = null;//make the instance empty
        BlockChain blockchain = BlockChain.getInstance();
        assert (1 == blockchain.blockList.size());// genesis block
        assert (null != BlockChain.blockChainInstance);
    }

    @Test
    public void testForceSaveNewBlockWithEmptyArray() throws BlockException {
        BlockChain.blockChainInstance = null;//make the instance empty
        BlockChain blockchain = BlockChain.getInstance();
        ArrayList<Block> blocks = new ArrayList<>();
        assert blockchain.updateBlockListWithLongerChain(blocks);
    }

    @Test
    public void testPeerBlockchainIsEmpty(@Mocked final BlockDBHandler blockDBHandler) throws BlockException {
        new Expectations() {{
            blockDBHandler.loadChainList();
            result = constructTestBlockChainWith3Block();
        }};
        BlockChain.blockChainInstance = null;//make the instance empty
        BlockChain blockchain = BlockChain.getInstance();
        blockchain.getLatestBlock();
        ArrayList<Block> blocks = new ArrayList<>();
        assert blockchain.updateBlockListWithLongerChain(blocks);
    }

    @Test
    public void testGetLatestBlock(@Mocked final BlockDBHandler blockDBHandler) throws BlockException {
        new Expectations() {{
            blockDBHandler.loadChainList();
            result = constructTestBlockChainWith3Block();
        }};
        BlockChain.blockChainInstance = null;//make the instance empty
        BlockChain blockchain = BlockChain.getInstance();
        List<Block> queriedBlockList = blockchain.getLatestBlock(3, -1);
        assert 3 == queriedBlockList.size();
    }

    @Test
    public void testPeerblockchinaiIsLonger(@Mocked final BlockDBHandler blockDBHandler) throws BlockException {
        new Expectations() {{
            blockDBHandler.loadChainList();
            result = constructTestBlockChainWith3Block();
        }};
        BlockChain.blockChainInstance = null;//make the instance empty
        BlockChain blockchain = BlockChain.getInstance();
        assert blockchain.getBlockList().size() == 3;

        System.out.println("\n[Test] begin to test peer blockchain is one block more than ours");
        assert blockchain.updateBlockListWithLongerChain(constructTestBlockChainWith4Block());
        assert blockchain.getBlockList().size() == 4;

        System.out.println("\n[Test] begin to test peer blockchain is two blocks more than ours");
        assert blockchain.updateBlockListWithLongerChain(constructTestBlockChainWith6Block());
        assert blockchain.getBlockList().size() == 6;
    }

    @Test
    public void testPeerblockchinaiIsShorter(@Mocked final BlockDBHandler blockDBHandler) throws BlockException {
        new Expectations() {{
            blockDBHandler.loadChainList();
            result = constructTestBlockChainWith4Block();
        }};
        BlockChain.blockChainInstance = null;//make the instance empty
        BlockChain blockchain = BlockChain.getInstance();
        assert blockchain.getBlockList().size() == 4;
        assert blockchain.updateBlockListWithLongerChain(constructTestBlockChainWith3Block());
        assert blockchain.getBlockList().size() == 4;
    }

    @Test
    public void testIsBlocksContainSameTransaction() {
        BlockChain blockChain = BlockChain.getInstance();
        Block block1 = Block.constructEmptyBlock();
        assert !blockChain.isBlocksContainSameTransaction(block1, block1);

        Transaction transaction1 = new Transaction();
        transaction1.setData("");
        block1.addTransaction(transaction1);
        assert blockChain.isBlocksContainSameTransaction(block1, block1);

        Transaction transaction2 = new Transaction();
        transaction2.setData("");
        transaction2.setId();
        Block block2 = Block.constructEmptyBlock();
        block2.addTransaction(transaction2);
        assert !blockChain.isBlocksContainSameTransaction(block1, block2);

        block2.addTransaction(transaction1);
        assert blockChain.isBlocksContainSameTransaction(block1, block2);
    }

    @Test
    public void testIsBlockValid(@Mocked final BlockDBHandler blockDBHandler) {
        BlockChain blockChain = BlockChain.getInstance();
        Block block1 = Block.constructEmptyBlock();
        Transaction transaction1 = new Transaction();
        transaction1.setData("");
        block1.addTransaction(transaction1);
        assert blockChain.hasDuplicatedTransaction(block1);

        blockChain.getBlockList().add(block1);
        assert !blockChain.hasDuplicatedTransaction(block1);

        blockChain.getBlockList().add(Block.constructBlock("0009E4E2F511368F265966807D24CD19", -1941014815, 5));
        assert !blockChain.hasDuplicatedTransaction(block1);

        Transaction transaction2 = new Transaction();
        transaction2.setData("");
        transaction2.setId();
        Block block2 = Block.constructEmptyBlock();
        block2.addTransaction(transaction2);
        assert blockChain.hasDuplicatedTransaction(block2);

        block2.addTransaction(transaction1);
        assert !blockChain.hasDuplicatedTransaction(block2);
    }

    @Test
    public void testInitBalance() throws BlockException, PoWException {
        AccountManager accountManager = AccountManager.getInstance();
        String minerUser = "miner";
        String fromUser = "from";
        String toUser = "to";
        int targetSenderInitValue = 10;
        int targetTransactionInitValue = 4;

        accountManager.reset();
        assert 0 == accountManager.getBalance(fromUser);
        assert 0 == accountManager.getBalance(toUser);

        BlockChain blockChain = new BlockChain();
        blockChain.init(constructTestInitBalanceWith4Block(fromUser, toUser, minerUser, targetSenderInitValue, targetTransactionInitValue));

        assert targetSenderInitValue - targetTransactionInitValue == accountManager.getBalance(fromUser);
        assert targetTransactionInitValue == accountManager.getBalance(toUser);
    }

    @Test
    public void reAddMissingCrosschainTransactionWhenRebranch() throws BlockException, PoWException {
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);

        AccountManager accountManager = AccountManager.getInstance();
        MiningConfiguration.testSetRequiredZeroCount(0);
        String minerUser = "miner";
        String fromUser = "from";
        String toUser = "to";
        int targetSenderInitValue = 100;
        int targetTransactionInitValue = 4;

        accountManager.reset();
        assert 0 == accountManager.getBalance(fromUser);
        assert 0 == accountManager.getBalance(toUser);

        accountManager.addValue(minerUser, targetSenderInitValue * 2);

        BlockChain blockChain = BlockChain.getInstance();
        List<Block> blockList = constructTestInitBalanceWith4Block(fromUser, toUser, minerUser, targetSenderInitValue, targetTransactionInitValue);
        System.out.println("\n[Test]  init blockchain");
        blockChain.init(blockList);
        Transaction crtx1 = new CrosschainTransaction(111, 1);
        crtx1.setFrom(fromUser);
        crtx1.setToAndValue(toUser, targetTransactionInitValue);
        crtx1.setData("The Fourth Block");
        crtx1.setId();
        crtx1.setHash();

        assert blockChain.addBlock(createBlock(crtx1, blockChain.getLatestBlock()));

        Block block = Block.constructEmptyBlock();
        block.setBlockIndexAndMiningTime(blockChain.getLatestBlock().getBlockIndex() + 1);
        blockList.add(block);

        block = Block.constructEmptyBlock();
        block.setBlockIndexAndMiningTime(blockChain.getLatestBlock().getBlockIndex() + 2);
        blockList.add(block);

        assert blockChain.updateBlockListWithLongerChain(blockList);

        assert 1 == BlockchainService.getInstance().getMiner().getTransactionListCount();
    }

    @Test
    public void removeTransactionExistInBlockchain() throws BlockException {
        AccountManager accountManager = AccountManager.getInstance();
        MiningConfiguration.setDifficulty(0);
        String minerUser = "miner";
        String fromUser = "from";
        String toUser = "to";
        int value = 10;
        accountManager.addValue(fromUser, 1000);

        Transaction t1 = createTransaction(fromUser, toUser, value, "data1");
        Transaction t2 = createTransaction(fromUser, toUser, value, "data2");
        Transaction t3 = createTransaction(fromUser, toUser, value, "data3");
        Transaction t4 = createTransaction(fromUser, toUser, value, "data4");
        Transaction t5 = createTransaction(fromUser, toUser, value, "data5");
        Transaction t6 = createTransaction(fromUser, toUser, value, "data6");

        System.out.printf("[Test] Generated ids are: %d %d %d %d %d %d\n", t1.getId(), t2.getId(), t3.getId(), t4.getId(), t5.getId(), t6.getId());

        Block block1 = createBlock(t1, null);
        Block block2 = createBlock(null, block1);
        Block block3 = createBlock(t3, block2);
        List<Block> blockArrayList = new ArrayList<>();
        blockArrayList.add(block1);
        blockArrayList.add(block2);
        blockArrayList.add(block3);

        BlockChain blockChain = BlockChain.getInstance();
        System.out.println("\n[Test] ******* begin to init the blockchain");
        blockChain.init(blockArrayList);

        List<AbstractTransaction> transactionArrayList = new ArrayList<>();
        transactionArrayList.add(t2);
        transactionArrayList.add(t3);
        transactionArrayList.add(t4);
        transactionArrayList.add(t1);
        BlockChain.getInstance().removeTransactionExistInBlockchain(transactionArrayList, 10);
        assert 2 == transactionArrayList.size();

        transactionArrayList = new ArrayList<>();
        transactionArrayList.add(t2);
        transactionArrayList.add(t6);
        transactionArrayList.add(t4);
        transactionArrayList.add(t5);
        BlockChain.getInstance().removeTransactionExistInBlockchain(transactionArrayList, 10);
        assert 4 == transactionArrayList.size();

        transactionArrayList = new ArrayList<>();
        transactionArrayList.add(t3);
        transactionArrayList.add(t1);
        BlockChain.getInstance().removeTransactionExistInBlockchain(transactionArrayList, 10);
        assert transactionArrayList.isEmpty();
    }

    @Test
    public void testHandleInternalStatus() throws BlockException, PoWException {
        AccountManager accountManager = AccountManager.getInstance();
        String fromUser = "from";
        accountManager.reset();
        accountManager.addValue(fromUser, 50);
        assert accountManager.getBalance(fromUser) == 50;
        BlockChain blockChain = new BlockChain();
        blockChain.init(constructTestInternalStatusWith3Block(fromUser));
        assert blockChain.getBlockList().size() == 3;
        assert accountManager.getBalance(fromUser) == 48;

        assert blockChain.updateBlockListWithLongerChain(constructTestBlockChainWith6Block());
        assert blockChain.getBlockList().size() == 6;
        assert accountManager.getBalance(fromUser) == accountManager.getMiningReward() - 3;
    }

    Block createBlock(Transaction t, Block previousBlock) throws BlockException {
        String previousHash = null == previousBlock ? "" : previousBlock.getBlockHash();
        int nouce = null == previousBlock ? 0 : previousBlock.getBlockNounce() + 1;
        int index = null == previousBlock ? 0 : previousBlock.getBlockIndex() + 1;
        Block block = Block.constructBlock(previousHash, nouce, index, t);
        block.getBlockHash();
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

    private List<Block> constructTestBlockChainWith3Block() {
        MiningConfiguration.setDifficulty(0);
        List<Block> blockList = new CopyOnWriteArrayList<Block>();

        blockList.add(Block.constructBlock("", 867957279, 0));
        blockList.add(Block.constructBlock("00006C2CA920AE27B5A32F72A5C1B5B4", 314214769, 1));
        blockList.add(Block.constructBlock("000933AB43C1A0B4CC96A660B74627E0", -44162486, 2));
        return blockList;
    }

    private List<Block> constructTestInitBalanceWith4Block(String targetSenderAddress, String targetReceiverAddress,
                                                           String minerAddress, int targetSenderInitValue,
                                                           int targetTransactionInitValue) throws BlockException, PoWException {
        List<Block> blockList = new CopyOnWriteArrayList<Block>();

        Block block;
        blockList.add(block = Block.constructBlock("", 867957279, 0));
        blockList.add(block = Block.constructBlock("00006C2CA920AE27B5A32F72A5C1B5B4", 314214769, 1));
        block.setMiner(minerAddress);

        block = constructEmptyBlock();
        block.setBlockIndexAndMiningTime(2);
        // add the header
        block.setPreviousHash("000933AB43C1A0B4CC96A660B74627E0");
        // add transaction
        Transaction transaction = new Transaction();
        transaction.setFrom(minerAddress);
        transaction.setToAndValue(targetSenderAddress, targetSenderInitValue);
        transaction.setData("The Third Block");
        transaction.setId();
        transaction.setHash();
        block.getBody().addTransaction(transaction);
        // find the nounce
        int blockNounce = PoW.findBlockNounce(block.getPreviousHash(), block.getTransactionHash());
        if (0 != blockNounce) {
            block.setBlockNounce(blockNounce);
            System.out.printf("Nounce %d found with hash: %s\n", blockNounce, block.getBlockHash());
        } else {
            System.out.println("No nounce found, please decrease the difficulty");
        }

        blockList.add(block);

        String previousHash = block.getBlockHash();
        block = constructEmptyBlock();
        block.setBlockIndexAndMiningTime(3);
        // add the header
        block.setPreviousHash(previousHash);
        // add transaction
        transaction = new Transaction();
        transaction.setFrom(targetSenderAddress);
        transaction.setToAndValue(targetReceiverAddress, targetTransactionInitValue);
        transaction.setData("The forth Block");
        transaction.setId();
        transaction.setHash();
        block.getBody().addTransaction(transaction);
        // find the nounce
        blockNounce = PoW.findBlockNounce(block.getPreviousHash(), block.getTransactionHash());
        if (0 != blockNounce) {
            block.setBlockNounce(blockNounce);
            System.out.printf("Nounce %d found with hash: %s\n", blockNounce, block.getBlockHash());
        } else {
            System.out.println("No nounce found, please decrease the difficulty");
        }
        blockList.add(block);

        return blockList;
    }

    private List<Block> constructTestInternalStatusWith3Block(String address) throws BlockException, PoWException {
        List<Block> blockList = new CopyOnWriteArrayList<Block>();

        blockList.add(Block.constructBlock("", 867957279, 0));
        blockList.add(Block.constructBlock("00006C2CA920AE27B5A32F72A5C1B5B4", 314214769, 1));

        Block thirdBlock = constructEmptyBlock();
        // add the header
        thirdBlock.setPreviousHash("000933AB43C1A0B4CC96A660B74627E0");
        // add transaction
        Transaction transaction = new Transaction();
        transaction.setFrom(address);
        transaction.setToAndValue("to", 2);
        transaction.setData("The Third Block");
        transaction.setHash();
        thirdBlock.getBody().addTransaction(transaction);
        // find the nounce
        int blockNounce = PoW.findBlockNounce(thirdBlock.getPreviousHash(), thirdBlock.getTransactionHash());
        if (0 != blockNounce) {
            thirdBlock.setBlockNounce(blockNounce);
            System.out.printf("Nounce %d found with hash: %s\n", blockNounce, thirdBlock.getBlockHash());
        } else {
            System.out.println("No nounce found, please decrease the difficulty");
        }

        blockList.add(thirdBlock);
        return blockList;
    }

    private List<Block> constructTestBlockChainWith4Block() {
        List<Block> blockList = constructTestBlockChainWith3Block();
        blockList.add(Block.constructBlock("000BF41F6BC6F65C798BD0A7EBF72686", 1767426602, 3));
        return blockList;
    }

    private List<Block> constructTestBlockChainWith6Block() {
        List<Block> blockList = constructTestBlockChainWith4Block();

        int interactionId = 1;
        Transaction t = new ConditionalAssociationTransaction(interactionId, new Condition("from", "to", 3));
        Block block = Block.constructBlock("0005FD401244D9F9105B96964B8F0CEF", 911077208, 4);
        block.addTransaction(t);
        block.setMiner("from");
        blockList.add(block);

        blockList.add(Block.constructBlock("0009E4E2F511368F265966807D24CD19", -1941014815, 5));
        return blockList;
    }

}
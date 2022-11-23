package com.scu.suhong.block;

import Service.BlockchainService;
import Service.BlockchainServiceThread;
import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.dynamic_definition.DynamicalAsset;
import com.scu.suhong.miner.Miner;
import com.scu.suhong.miner.QueryTopBlocksInformation;
import com.scu.suhong.network.NetworkException;
import com.scu.suhong.network.P2P;
import com.scu.suhong.network.P2PConfiguration;
import consensus.pow.MiningConfiguration;
import mockit.*;
import org.junit.Test;
import util.StringHelper;
import util.ThreadHelper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BlockchainSyncManagerTest {

    final String queryTopBlockMsg = "QueryTopBlockMsg";
    final String queryTopBlockMsgSeparator = ":";
    final String queryNumber = "3";
    final String peerAddress = "127.0.0.1";
    final int peerPort = StringHelper.getDefaultPort();// as we do not set in mining configuration file. We have to use the default
    final String startBlockIndex = "1234";

    @Test
    public void testSend() throws IOException {
        BlockchainSyncManager syncManager = new BlockchainSyncManager(new BlockChain());// we don't need the blockchain change, so we let here null
        ArrayList<Block> blocks = new ArrayList<>();
        BlockHeader h1 = new BlockHeader("00006C2CA920AE27B5A32F72A5C1B5B4", 314214769, 1);
        Block block = new Block(h1, new BlockBody());
        blocks.add(block);
        BlockHeader h2 = new BlockHeader("000933AB43C1A0B4CC96A660B74627E0", -44162486, 2);
        block = new Block(h2, new BlockBody());
        blocks.add(block);
        syncManager.send(blocks, "127.0.0.1", 7601);
    }

    @Test
    public void testProcessBlockMsgWithOneIndexAdvance(@Mocked final BlockChain blockChain) throws BlockException {
        MiningConfiguration.setDifficulty(0);
        List<Block> blocks = constructTestBlockChainWith3Block();
        new Expectations() {{
            blockChain.getLatestBlock();
            result = blocks.get(blocks.size() - 1);
        }};
        List<Block> anotherBlocks = constructTestBlockChainWith4Block();
        Block anotherLatestBlock = anotherBlocks.get(anotherBlocks.size() - 1);

        BlockchainSyncManager syncManager = new BlockchainSyncManager(BlockChain.getInstance());
        assert syncManager.updateCurrentBlockchain(anotherLatestBlock, null, 7601);

        new Verifications() {{
            blockChain.saveNewBlock(anotherLatestBlock);
            times = 1;
        }};
    }

    @Test
    public void testProcessBlockMsgWithOneIndexAdvanceForDynamicalAsset(@Mocked final BlockChain blockChain) throws BlockException {
        MiningConfiguration.setDifficulty(0);
        List<Block> blocks = constructTestBlockChainWith3Block();
        new Expectations() {{
            blockChain.getLatestBlock();
            result = blocks.get(blocks.size() - 1);
        }};
        List<Block> anotherBlocks = constructTestBlockChainWith4BlockWithDynamicalAsset();
        Block anotherLatestBlock = anotherBlocks.get(anotherBlocks.size() - 1);

        BlockchainSyncManager syncManager = new BlockchainSyncManager(BlockChain.getInstance());
        assert syncManager.updateCurrentBlockchain(anotherLatestBlock, null, 7601);
    }

    @Test
    public void testProcessBlockMsgWithThreeIndexAdvance() throws IOException {
        List<Block> shortBlockList = constructTestBlockChainWith3Block();
        List<Block> longerBlocks = constructTestBlockChainWith6Block();
        BlockchainService blockchainService = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchainService);
        Thread mainThread = new Thread(blockchainServiceThread, "Block test thread");
        mainThread.start();
        ThreadHelper.safeSleep(1*1000);
        while (null == blockchainService.getMiner()){
            ThreadHelper.safeSleep(500);
        }
        Miner miner = blockchainService.getMiner();


        String exChainId = "999";
        BlockChain externalBlockchain = ExternalBlockchainManager.getWorker(exChainId);
        BlockChain blockchain = BlockChain.getInstance();
        externalBlockchain.init(shortBlockList);
        blockchain.init(shortBlockList);

        byte[] ipAddr = new byte[]{(byte)192, (byte)168, 30, 1};
        InetAddress address = InetAddress.getByAddress(ipAddr);
        miner.onNetworkMsg(longerBlocks.get(longerBlocks.size() - 1).getJson().toString().getBytes(), address, peerPort);
        miner.onNetworkMsg(Block.getBlockListJson(longerBlocks).getBytes(), address, peerPort);

        ThreadHelper.safeSleep(2 * 1000);
        assert 6 == BlockChain.getInstance().getBlockList().size();
    }

    @Test
    public void testProcessBlockMsgWithThreeIndexAdvanceBatchlyUpdate() throws IOException, BlockException {
        MiningConfiguration.testSetRequiredZeroCount(0);
        List<Block> shortBlockList = constructTestBlockChainWith3Block();
        List<Block> longerBlocks = constructTestBlockChainWith6Block();
        BlockchainService blockchainService = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchainService);
        Thread mainThread = new Thread(blockchainServiceThread, "Block test thread");
        mainThread.start();
        ThreadHelper.safeSleep(1*1000);
        while (null == blockchainService.getMiner()){
            ThreadHelper.safeSleep(500);
        }
        Miner miner = blockchainService.getMiner();


        String exChainId = "999";
        BlockChain externalBlockchain = ExternalBlockchainManager.getWorker(exChainId);
        BlockChain blockchain = BlockChain.getInstance();
        externalBlockchain.init(shortBlockList);
        blockchain.init(shortBlockList);

        byte[] ipAddr = new byte[]{(byte)192, (byte)168, 30, 1};
        InetAddress address = InetAddress.getByAddress(ipAddr);
        miner.onNetworkMsg(longerBlocks.get(longerBlocks.size() - 1).getJson().toString().getBytes(), address, peerPort);

        longerBlocks.remove(longerBlocks.get(longerBlocks.size() -1));// the latest has been sent
        miner.onNetworkMsg(Block.getBlockListJson(longerBlocks).getBytes(), address, peerPort);

        ThreadHelper.safeSleep(2 * 1000);
        for (Block b : longerBlocks){
            assert b.getBlockHash().equals(blockchain.getBlock(b.getBlockIndex()).getBlockHash());
        }
    }

    @Test
    // Notice this test will test with a really environment 192.168.1.163
    public void testProcessBlockMsgWithThreeIndexAdvanceBatchlyUpdateWithrealPeer() throws IOException {
        P2PConfiguration.getInstance().testAddInternalPeerAddress("192.168.1.163", 7601);
        List<Block> shortBlockList = constructTestBlockChainWith3Block();
        BlockchainService blockchainService = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchainService);
        Thread mainThread = new Thread(blockchainServiceThread, "Block test thread");
        mainThread.start();
        ThreadHelper.safeSleep(1*1000);
        while (null == blockchainService.getMiner()){
            ThreadHelper.safeSleep(500);
        }

        String exChainId = "999";
        BlockChain externalBlockchain = ExternalBlockchainManager.getWorker(exChainId);
        BlockChain blockchain = BlockChain.getInstance();
        externalBlockchain.init(shortBlockList);
        blockchain.init(shortBlockList);

        String queryTopBlockInformationString = formatQueryString(10, -1);
        String msgHash = null;
        try {
            msgHash = StringHelper.byteArrayToHexString(MD5Hash.getValue(queryTopBlockInformationString.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        BlockchainSyncManager syncManager = new BlockchainSyncManager(BlockChain.getInstance());
        InetAddress address = null;
       syncManager.doProcessBlockQueryMsg(queryTopBlockInformationString.getBytes(), msgHash);

       // why so long 100 seconds?
       for (int i = 0; i < 30; ++i) {
           ThreadHelper.safeSleep(1000);
           System.out.println("[Test] **** ******* " + BlockChain.getInstance().getBlockList().size());
       }
        assert 3 < BlockChain.getInstance().getBlockList().size();
    }

    @Test
    public void testProcessBlockMsgAdvanceWithJson() throws UnknownHostException {
        BlockchainService blockchainService = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchainService);
        Thread mainThread = new Thread(blockchainServiceThread, "Block test thread");
        mainThread.start();
        ThreadHelper.safeSleep(1*1000);
        while (null == blockchainService.getMiner()){
            ThreadHelper.safeSleep(500);
        }

        String exChainId = "999";
        MiningConfiguration.testSetRequiredZeroCount(0);
        P2PConfiguration.getInstance().testSetRequiredZeroCount(exChainId, 0);
        InetAddress address = InetAddress.getLocalHost();
        doProcessBlockMsgAdvanceWithJson(address, BlockChain.getInstance());
        assert 4 == BlockChain.getInstance().blockList.size();
        BlockChain.reset();
        BlockChain externalBlockchain = ExternalBlockchainManager.getWorker(exChainId);
        assert 1 == externalBlockchain.blockList.size(); // The genesis block

        System.out.println("\n[Test] begin to do external sync");
        P2PConfiguration.getInstance().isProducerPeer(address.getHostAddress(), 7601);
        doProcessBlockMsgAdvanceWithJson(address, externalBlockchain);
        assert 4 == BlockChain.getInstance().blockList.size();
        assert 4 == externalBlockchain.blockList.size();
        BlockchainFileDumper fileDumper = new BlockchainFileDumper();
        fileDumper.dumpAll();
    }

    public void doProcessBlockMsgAdvanceWithJson(InetAddress address, BlockChain blockChain) {
        MiningConfiguration.testSetRequiredZeroCount(0);
        List<Block> blocks = constructTestBlockChainWith3Block();
        List<Block> blockList = new ArrayList<>();
        blockList.add(blocks.get(blocks.size() - 3));
        blockList.add(blocks.get(blocks.size() - 2));
        blockChain.testSetBlockChain(blockList);
        assert blockChain.getBlockList().size() == 2;

        String blockListJson = "{\"blocks\" :[{\"b\":{\"ts\":[]},\"h\":{\"p\":\"\",\"index\":0,\"n\":867957279},\"hash\":\"00006C2CA920AE27B5A32F72A5C1B5B4\"},{\"b\":{\"ts\":[]},\"h\":{\"p\":\"00006C2CA920AE27B5A32F72A5C1B5B4\",\"index\":1,\"n\":483852094},\"hash\":\"00088CB79562EEF06C3F7B1031D67B20\"},{\"b\":{\"ts\":[]},\"h\":{\"p\":\"00088CB79562EEF06C3F7B1031D67B20\",\"index\":2,\"n\":1783996189},\"hash\":\"000D5869872DCD2822A98158897E1CE5\"},{\"b\":{\"ts\":[]},\"h\":{\"p\":\"000D5869872DCD2822A98158897E1CE5\",\"index\":3,\"n\":358755144},\"hash\":\"0006BA7ACC64F49AB7831E63C8096699\"}]}\n";
        BlockchainSyncManager syncManager = new BlockchainSyncManager(blockChain);
        assert syncManager.tryProcessBlockMsg(blockListJson.getBytes(), "testJsonHash", address, peerPort);
        List<Block> newBlockList = blockChain.getBlockList();
        assert newBlockList.size() == 4;
    }

    @Test
    public void testProcessBlockMsgWithStart(@Mocked final BlockChain blockChain, @Mocked final List<Block> blockList) {
//        new Expectations() {{
//            blockChain.updateBlockListWithLongerChain(blockList);
//            result = true;
//        }};
        List<Block> blocks = constructTestBlockChainWith3Block();
        int start = 2;
        BlockchainSyncManager syncManager = new BlockchainSyncManager(BlockChain.getInstance());
        List<Block> newList = syncManager.updateCurrentBlockchain(blocks, start);
        for (int i = start; i < blocks.size(); ++i) {
            assert blocks.get(i).isEqual(newList.get(i - start));
        }

        start = 1;
        newList = syncManager.updateCurrentBlockchain(blocks, start);
        for (int i = start; i < blocks.size(); ++i) {
            assert blocks.get(i).isEqual(newList.get(i - start));
        }
    }

    @Test
    public void testTryProcessTopBlock() {
        BlockChain blockChain = BlockChain.getInstance();
        blockChain.blockList = constructTestBlockChainWith3Block();

        Block block = Block.constructBlock("000BF41F6BC6F65C798BD0A7EBF72686", 1767426602, 3);
        BlockchainSyncManager syncManager = new BlockchainSyncManager(BlockChain.getInstance());
        assert syncManager.tryProcessLatestBlock("".getBytes(), block, null, peerPort);
        block = Block.constructBlock("00006C2CA920AE27B5A32F72A5C1B5B4", 314214769, 1);
        System.out.println("[BlockchainSyncManagerTest] Try process the second block");
        assert !syncManager.tryProcessLatestBlock("".getBytes(), block, null, peerPort);
    }

    @Test
    public void testIsBlockListValid(@Mocked final BlockDBHandler blockDBHandler) {
        MiningConfiguration.testSetRequiredZeroCount(0);
        new Expectations() {{
            blockDBHandler.loadChainList();
            result = constructTestBlockChainWith4Block();
        }};

        BlockchainSyncManager syncManager = new BlockchainSyncManager(BlockChain.getInstance());
        List<Block> sixBlockList = constructTestBlockChainWith6Block();
        List<Block> newList = sixBlockList;
        assert syncManager.shouldWeUpdate(newList);
        newList = sixBlockList.subList(3, sixBlockList.size());
        assert syncManager.shouldWeUpdate(newList);
        newList = sixBlockList.subList(3, sixBlockList.size());
        assert syncManager.shouldWeUpdate(newList);

        newList = sixBlockList.subList(3, 4);
        assert !syncManager.shouldWeUpdate(newList);
        newList = sixBlockList.subList(5, sixBlockList.size());
        assert syncManager.shouldWeUpdate(newList);
        newList = constructTestBlockChainWith3Block();
        assert !syncManager.shouldWeUpdate(newList);
    }

    @Test
    public void testTryProcessBlocks() throws UnknownHostException {
        BlockChain blockChain = BlockChain.getInstance();
        blockChain.blockList = constructTestBlockChainWith3Block();
        List<Block> newList = constructTestBlockChainWith6Block();
        String msgHash = null;
        try {
            msgHash = StringHelper.byteArrayToHexString(MD5Hash.getValue(Block.getBlockListJson(newList).getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        BlockchainSyncManager syncManager = new BlockchainSyncManager(BlockChain.getInstance());
        InetAddress address = InetAddress.getLocalHost();
        assert syncManager.tryProcessBlockMsg(Block.getBlockListJson(newList).getBytes(), msgHash, address, peerPort);
    }

    @Test
    public void testTryQueryBlocksAtSpecificStartMsgWithStartIndexOutOfRange() {
        int startBlockIndex = 1234;
        final int numberOfBlock = 3;

        BlockChain blockChain = BlockChain.getInstance();
        blockChain.blockList = constructTestBlockChainWith6Block();

        String queryTopBlockInformationString = formatQueryString(numberOfBlock, startBlockIndex);
        String msgHash = null;
        try {
            msgHash = StringHelper.byteArrayToHexString(MD5Hash.getValue(queryTopBlockInformationString.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        BlockchainSyncManager syncManager = new BlockchainSyncManager(BlockChain.getInstance());
        InetAddress address = null;
        assert 0 == syncManager.doProcessBlockQueryMsg(queryTopBlockInformationString.getBytes(), msgHash);
    }

    @Test
    public void testTryQueryBlocksAtSpecificStartMsgWithNumberTwoLarge(@Mocked final P2P p2p) throws IOException {
        int startBlockIndex = 3;
        final int numberOfBlock = 1234;
        final int realSendNumberOfBlock = 4;
        new Expectations() {{
            //p2p.;
            p2p.send(anyString, peerAddress, StringHelper.getDefaultPort());
            result = any;
        }};

        BlockChain blockChain = BlockChain.getInstance();
        blockChain.blockList = constructTestBlockChainWith6Block();

        String queryTopBlockInformationString = formatQueryString(numberOfBlock, startBlockIndex);
        String msgHash = null;
        try {
            msgHash = StringHelper.byteArrayToHexString(MD5Hash.getValue(queryTopBlockInformationString.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        BlockchainSyncManager syncManager = new BlockchainSyncManager(BlockChain.getInstance());
        InetAddress address = null;
        assert realSendNumberOfBlock == syncManager.doProcessBlockQueryMsg(queryTopBlockInformationString.getBytes(), msgHash);
        new Verifications() {{
            p2p.send(anyString, anyString, anyInt);
            times = 1;
        }};
    }

    @Test
    public void testTryQueryBlocksAtSpecificStartMsg(@Mocked final P2P p2p) throws IOException {
        int startBlockIndex = 4;
        int numberOfBlock = 3;
        System.out.println("[Test] begin to test query number is " + numberOfBlock);
        queryBlocksAtSpecificStartMsg(p2p, startBlockIndex, numberOfBlock);

        numberOfBlock = 1;
        System.out.println("[Test] begin to test query number is " + numberOfBlock);
        queryBlocksAtSpecificStartMsg(p2p, startBlockIndex, numberOfBlock);

        numberOfBlock = 0;
        System.out.println("[Test] begin to test query number is " + numberOfBlock);
        queryBlocksAtSpecificStartMsgWithZeroNumber(startBlockIndex, numberOfBlock);
    }

    private Block constructGenesisBlock() {
        Block genesisBlock = Block.constructEmptyBlock();
        genesisBlock.setBlockNounce(1951040714);
        genesisBlock.setBlockIndexAndMiningTime(0);
        return genesisBlock;
    }

    private void queryBlocksAtSpecificStartMsg(@Mocked P2P p2p, int startBlockIndex, int numberOfBlock) throws IOException {
        new Expectations() {{
            //p2p.;
            p2p.send(anyString, peerAddress, peerPort);
            result = any;
        }};

        BlockChain blockChain = BlockChain.getInstance();
        blockChain.blockList = constructTestBlockChainWith6Block();

        String queryTopBlockInformationString = formatQueryString(numberOfBlock, startBlockIndex);
        String msgHash = null;
        try {
            msgHash = StringHelper.byteArrayToHexString(MD5Hash.getValue(queryTopBlockInformationString.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        BlockchainSyncManager syncManager = new BlockchainSyncManager(BlockChain.getInstance());
        InetAddress address = null;
        assert numberOfBlock == syncManager.doProcessBlockQueryMsg(queryTopBlockInformationString.getBytes(), msgHash);
        new Verifications() {{
            p2p.send(anyString, anyString, anyInt);
            times = 1;
        }};
    }

    private void queryBlocksAtSpecificStartMsgWithZeroNumber(int startBlockIndex, int numberOfBlock) throws IOException {
        BlockChain blockChain = BlockChain.getInstance();
        blockChain.blockList = constructTestBlockChainWith6Block();

        String queryTopBlockInformationString = formatQueryString(numberOfBlock, startBlockIndex);
        String msgHash = null;
        try {
            msgHash = StringHelper.byteArrayToHexString(MD5Hash.getValue(queryTopBlockInformationString.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        BlockchainSyncManager syncManager = new BlockchainSyncManager(BlockChain.getInstance());
        InetAddress address = null;
        assert numberOfBlock == syncManager.doProcessBlockQueryMsg(queryTopBlockInformationString.getBytes(), msgHash);
    }

    @Test
    public void testTryQueryTopBlocksMsg(@Mocked final P2P p2p) throws IOException {
        new Expectations() {{
            //p2p.;
            p2p.send(anyString, peerAddress, StringHelper.getDefaultPort());
            result = any;
        }};

        final int startBlockIndex = -1;
        final int numberOfBlock = 3;
        BlockChain blockChain = BlockChain.getInstance();
        blockChain.blockList = constructTestBlockChainWith6Block();
        String queryTopBlockInformationString = formatQueryString(numberOfBlock, startBlockIndex);
        String msgHash = null;
        try {
            msgHash = StringHelper.byteArrayToHexString(MD5Hash.getValue(queryTopBlockInformationString.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        BlockchainSyncManager syncManager = new BlockchainSyncManager(BlockChain.getInstance());
        InetAddress address = null;
        assert 3 == syncManager.doProcessBlockQueryMsg(queryTopBlockInformationString.getBytes(), msgHash);
        new Verifications() {{
            p2p.send(anyString, anyString, anyInt);
            times = 1;
        }};
    }

    @Test
    public void testGetQueryTopBlocksInformation() throws NetworkException {
        BlockchainSyncManager syncManager = new BlockchainSyncManager(BlockChain.getInstance());
        String queryTopBlockInformationString = formatQueryString(queryNumber, startBlockIndex);
        QueryTopBlocksInformation queryTopBlocksInformation = syncManager.getQueryTopBlocksInformation(queryTopBlockInformationString);
        assert Integer.parseInt(queryNumber) == queryTopBlocksInformation.getNumberOfQueryBlock();
        assert peerAddress.equals(queryTopBlocksInformation.getPeerAddress());
        assert startBlockIndex.equals(String.valueOf(queryTopBlocksInformation.getStartBlockIndex()));
        assert queryNumber.equals(String.valueOf(queryTopBlocksInformation.getNumberOfQueryBlock()));
    }

    private List<String> loadPeerAddress() {
        List r = new ArrayList<String>();
        r.add("127.0.0.1");
        return r;
    }

    private String formatQueryString(int queryNumber, int startBlockIndex) {
        return queryTopBlockMsg + queryTopBlockMsgSeparator + peerAddress + queryTopBlockMsgSeparator + queryNumber + queryTopBlockMsgSeparator + startBlockIndex;
    }

    private String formatQueryString(String queryNumber, String startBlockIndex) {
        return queryTopBlockMsg + queryTopBlockMsgSeparator + peerAddress + queryTopBlockMsgSeparator + queryNumber + queryTopBlockMsgSeparator + startBlockIndex;
    }

    private List<Block> constructTestBlockChainWith3Block() {
        List<Block> blockList = new CopyOnWriteArrayList<Block>();
        blockList.add(Block.constructBlock("", 867957279, 0));
        blockList.add(Block.constructBlock("00006C2CA920AE27B5A32F72A5C1B5B4", 314214769, 1));
        blockList.add(Block.constructBlock("000933AB43C1A0B4CC96A660B74627E0", -44162486, 2));
        return blockList;
    }

    private List<Block> constructTestBlockChainWith1Block() {
        List<Block> blockList = constructTestBlockChainWith3Block();
        blockList.add(Block.constructBlock("000933AB43C1A0B4CC96A660B74627E0", -44162486, 2));
        return blockList;
    }

    private List<Block> constructTestBlockChainWith4Block() {
        List<Block> blockList = constructTestBlockChainWith3Block();
        blockList.add(Block.constructBlock("000BF41F6BC6F65C798BD0A7EBF72686", 1767426602, 3));
        return blockList;
    }


    private List<Block> constructTestBlockChainWith4BlockWithDynamicalAsset() {
        List<Block> blockList = constructTestBlockChainWith3Block();
        Block block = Block.constructBlock("000BF41F6BC6F65C798BD0A7EBF72686", 1767426602, 3);
        block.addTransaction(createDynamicalAsset());
        blockList.add(block);
        return blockList;
    }

    private DynamicalAsset createDynamicalAsset(){
        DynamicalAsset asset = new DynamicalAsset();
        asset.setOwner("testOwner");
        asset.setGas(2000);
        asset.addKeyValue("key1", "value1");
        asset.setId();
        asset.setHash();
        String code = constructNewAsset("NewAsset");
        asset.setCode(code);
        return asset;
    }

    private String constructNewAsset(String className)
    {
        String code = "";
        code += "  public class " + className + " extends DynamicalAsset{\n";
        code += "    @Override\n";
        code += "    public boolean check() {\n";
        code += "        return false;\n";
        code += "    }\n";
        code += "  }\n";
        return code;
    }

    private List<Block> constructTestBlockChainWith6Block() {
        List<Block> blockList = constructTestBlockChainWith4Block();
        blockList.add(Block.constructBlock("0005FD401244D9F9105B96964B8F0CEF", 911077208, 4));
        blockList.add(Block.constructBlock("0009E4E2F511368F265966807D24CD19", -1941014815, 5));
        return blockList;
    }
}
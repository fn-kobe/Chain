package com.scu.suhong.miner;

import Service.BlockchainService;
import Service.BlockchainServiceThread;
import account.AccountManager;
import com.scu.suhong.block.*;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.graph.JGraphTWrapper;
import com.scu.suhong.network.P2PConfiguration;
import com.scu.suhong.transaction.Condition;
import com.scu.suhong.transaction.ConditionalAssociationTransaction;
import com.scu.suhong.transaction.Transaction;
import consensus.pow.MiningConfiguration;
import consensus.pow.PoWException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import util.NoDuplicatedRandomHelper;
import util.ThreadHelper;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MinerTest {

    @Test
    public void testRun() throws Exception {
        Miner miner = new Miner();

        Transaction transaction = new Transaction();
        transaction.setData("User1 TX1");
        transaction.setAssetType("Big_data");
        transaction.setId();
        transaction.setHash();
        InetAddress address = InetAddress.getLocalHost();
        int port = 7601;
        miner.onNetworkMsg(transaction.getJson().toString().getBytes(), address, port);

        transaction.setData("User2 TX1");
        transaction.setId(123);
        transaction.setHash();
        miner.onNetworkMsg(transaction.getJson().toString().getBytes(), address, port);

        transaction.setData("User1 TX2");
        transaction.setId(125);
        transaction.setHash();
        miner.onNetworkMsg(transaction.getJson().toString().getBytes(), address, port);

        Thread minerThread = new Thread(miner, "test miner thread");
        minerThread.start();
        Thread.sleep(3000);// sleep 1 second
        miner.setForceStop();// stop the miner now
        minerThread.join();
        BlockChain blockChain = BlockChain.getInstance();
        System.out.printf(blockChain.dump());
    }

    @Test
    public void testFormatQueryTopBlockMsg() throws UnknownHostException {
        String hostName = MiningConfiguration.getHostIP();
        int port = MiningConfiguration.getSelfListenPort();
        String queryTopBlockMsg = BlockchainSyncManager.formatQueryTopBlockMsg();
        System.out.println("[Test] Querymessage is " + queryTopBlockMsg);
        assert (queryTopBlockMsg.contains(hostName));
        assert (queryTopBlockMsg.contains(String.valueOf(port)));
    }

    @Test
    public void testStringToByte() throws UnknownHostException {
        String invalidHostName = "127.0.0.1";
        byte[] b = invalidHostName.getBytes();
        int l = b.length;
        assert (b != null);
    }

    @NotNull
    private Miner constructMiner(int transactionNumber) {
        Miner miner = new Miner("NoService");

        Transaction t = new Transaction();
        for (int i = 0; i < transactionNumber; ++i) {
            t.setData("Test fetch transaction " + i);
            t.setId();
            miner.onTransactionMsg(t);
        }

        return miner;
    }

    @Test
    public void testOnNetworkMsg() throws UnknownHostException {
        MiningConfiguration.setDifficulty(0);
        String testBlockListMsg = "{\"blocks\" :[{\"b\":{\"ts\":[]},\"h\":{\"p\":\"00006C2CA920AE27B5A32F72A5C1B5B4\",\"index\":1,\"n\":314214769}},{\"b\":{\"ts\":[]},\"h\":{\"p\":\"000933AB43C1A0B4CC96A660B74627E0\",\"index\":2,\"n\":-44162486}}]}";
        Miner miner = new Miner();
        InetAddress address = InetAddress.getLocalHost();
        int port = 7601;
        miner.onNetworkMsg(testBlockListMsg.getBytes(), address, port);

        String testConditionTxMsg = "{\"normal\":{\"pr\":\"\",\"t\":\"\",\"d\":\"if_to_0xabcd_value_2\",\"pu\":\"\",\"h\":\"D41D8CD98F00B204E9800998ECF8427ED41D8CD98F00B204E9800998ECF8427E806A4BC867BFFE5FB63E6B4A139BFC1F72310608B875E02B243B6E3C5F458540103D62DEB2014D093BAC5E89BBA589F5C81E728D9D4C2F636F067F89CC14862C72310608B875E02B243B6E3C5F45854072310608B875E02B243B6E3C5F458540C81E728D9D4C2F636F067F89CC14862C\",\"from\":\"0xabcd\",\"to\":\"0xabca\",\"value\":\"2\"},\"condition\":{\"from\":\"0xabcd\",\"to\":\"0xabcd\",\"value\":\"2\"}}";
        miner.onNetworkMsg(testConditionTxMsg.getBytes(), address, port);

        String testTransaction = "{\"pr\":\"\",\"blockIndex\":\"-1\",\"t\":\"\",\"d\":\"init\",\"pu\":\"\",\"h\":\"E3A93050238CC32853E0B6748244416C\",\"from\":\"333?45678_09\",\"id\":915886903,\"to\":\"333?78900_12\",\"value\":\"40\"}";
        miner.onNetworkMsg(testTransaction.getBytes(), address, port);
        AbstractTransaction t = miner.testGetLatestTransaction();
        assert null != t;
        assert -1 == t.getBlockIndex();

        testTransaction = "{\"pr\":\"\",\"blockIndex\":\"239\",\"t\":\"\",\"d\":\"init\",\"pu\":\"\",\"h\":\"E3A93050238CC32853E0B6748244416C\",\"from\":\"333?45678_09\",\"id\":915886904,\"to\":\"333?78900_12\",\"value\":\"40\"}";
        miner.onNetworkMsg(testTransaction.getBytes(), address, port);
        t = miner.testGetLatestTransaction();
        assert null != t;
        assert 239 == t.getBlockIndex();
        miner.onNetworkMsg(testTransaction.getBytes(), address, port);
    }

    @Test
    public void testProcessPoSRight() {
        Miner miner = new Miner();
        // Miningconfigure nodeId should be 02
        String account  = "";
        for (int i = 0; i < 100; ++i) {
            System.out.printf("[TEST] test %d time\n", i+1);
            int nodeIndex  = miner.getSelectPosAccountNodeIndex(miner.getPosAccount());
            assert nodeIndex >= 0;
            assert nodeIndex < P2PConfiguration.getInstance().getPeerAddressListWithoutSelf().size();
        }
    }

    @Test
    public void testDynamicalAssetTransaction() throws UnknownHostException {
        MiningConfiguration.setDifficulty(0);
        BlockchainService blockchain = BlockchainService.getInstance();
        blockchain.testStartMiner();

        InetAddress address = InetAddress.getLocalHost();
        int port = 7601;
        Miner miner = BlockchainService.getInstance().getMiner();
        String testTransaction = "{\"owner\":\"owner\",\"code\":\"public class AssetT11 extends DynamicalAsset{\\n @Override\\n public boolean check(){\\n return true;\\n }\\n public AssetT11 getIntstance(){\\n return (AssetT11)@GlobalInstance;\\n }\\n}\\n \",\"blockIndex\":\"5\",\"data\":\"data\",\"DynamicalAsset\":\"identifier\",\"gas\":10000,\"h\":\"516AB9B12C221D6E63034FD882C1C3F5\",\"className\":\"AssetT11\",\"id\":909798928,\"kv\":{\"k1\":\"v1\",\"k2\":\"v2\",\"k3\":\"v3\"}}";
        miner.onNetworkMsg(testTransaction.getBytes(), address, port);
        AbstractTransaction t = miner.testGetLatestTransaction();
        assert null != t;
    }


    @Test
    public void testDynamicalAssetBlock() throws UnknownHostException {
        MiningConfiguration.setDifficulty(0);
        BlockchainService blockchain = BlockchainService.getInstance();
        blockchain.testStartMiner();

        Miner miner = BlockchainService.getInstance().getMiner();
        miner.setForceStop();
        ThreadHelper.safeSleep(2000);
        BlockChain.getInstance().testRest();

        InetAddress address = InetAddress.getLocalHost();
        int port = 7601;
        String testTransaction = "{\"b\":{\"ts\":[{\"owner\":\"owner\",\"code\":\"public class AssetT11 extends DynamicalAsset{\\n @Override\\n public boolean check(){\\n return true;\\n }\\n public AssetT11 getIntstance(){\\n return (AssetT11)@GlobalInstance;\\n }\\n}\\n \",\"blockIndex\":\"5\",\"data\":\"data\",\"DynamicalAsset\":\"identifier\",\"gas\":10000,\"h\":\"516AB9B12C221D6E63034FD882C1C3F5\",\"className\":\"AssetT11\",\"id\":909798928,\"kv\":{\"k1\":\"v1\",\"k2\":\"v2\",\"k3\":\"v3\"}}]},\"h\":{\"p\":\"0000000C91B7D0459D6B0E0EA163B149\",\"timeStamp\":\"2019.11.14_04.44.20\",\"index\":1,\"n\":508397141,\"miner\":\"111?45678_01\"},\"hash\":\"000000C20946D59DF80F92D3DB86E17A\"}";
        miner.onNetworkMsg(testTransaction.getBytes(), address, port);
        assert 2 == BlockChain.getInstance().getBlockList().size();
    }

    @Test
    public void testOnNetworkMsgWithExternal() throws UnknownHostException {
        String exChainId = "999";
        BlockChain externalInstance = ExternalBlockchainManager.getWorker(exChainId);
        externalInstance.testRest();

        BlockchainService blockchain = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchain);
        Thread mainThread = new Thread(blockchainServiceThread, "Block chain main thread");
        mainThread.start();
        ThreadHelper.safeSleep(2 * 1000);

        String testBlockListMsg = "{\"blocks\" :[{\"b\":{\"ts\":[]},\"h\":{\"p\":\"\",\"index\":0,\"n\":220455692,\"miner\":\"999?miner\"}},{\"b\":{\"ts\":[]},\"h\":{\"p\":\"0000000C91B7D0459D6B0E0EA163B149\",\"index\":1,\"n\":2027294060,\"miner\":\"999?miner\"}}]}";
        int testExternalPort = 9207;// refer to externalChainProducer in test folder
        String testExternalIpAddress = "127.0.0.1";
        P2PConfiguration.getInstance().testAddProducerPeerAddress(exChainId, testExternalIpAddress, testExternalPort);// also test external address
        Miner miner = blockchain.getMiner();
        InetAddress address = InetAddress.getByName(testExternalIpAddress);
        System.out.println("\n[Test] Block test");
        miner.onNetworkMsg(testBlockListMsg.getBytes(), address, testExternalPort);
        System.out.println("[Test] external blockchain size: " + externalInstance.getBlockList().size());
        assert 2 == externalInstance.getBlockList().size();

        testBlockListMsg = "{\"blocks\" :[{\"b\":{\"ts\":[]},\"h\":{\"p\":\"0000000C6A895B981DB5CB80213EB0F3\",\"index\":3,\"n\":1899752335,\"miner\":\"999?miner\"}}]}";
        miner.onNetworkMsg(testBlockListMsg.getBytes(), address, testExternalPort);
        assert 2 == externalInstance.getBlockList().size();

        testBlockListMsg = "{\"blocks\" :[{\"b\":{\"ts\":[]},\"h\":{\"p\":\"0000000C91B7D0459D6B0E0EA163B149\",\"index\":1,\"n\":2027294060,\"miner\":\"999?miner\"}}, {\"b\":{\"ts\":[]},\"h\":{\"p\":\"000000FB350907DF04478842C315D824\",\"index\":2,\"n\":151895186, \"miner\":\"999?miner\"}}, {\"b\":{\"ts\":[]},\"h\":{\"p\":\"0000000C6A895B981DB5CB80213EB0F3\",\"index\":3,\"n\":1899752335,\"miner\":\"999?miner\"}}]}";
        miner.onNetworkMsg(testBlockListMsg.getBytes(), address, testExternalPort);
        System.out.println("[Test] external blockchain size: " + externalInstance.getBlockList().size());
        BlockchainFileDumper fileDumper = new BlockchainFileDumper();
        fileDumper.dumpAll();
        assert 4 == externalInstance.getBlockList().size();

        System.out.println("\n[Test] Transaction test");
        String testConditionTxMsg = "{\"normal\":{\"pr\":\"\",\"t\":\"\",\"d\":\"if_to_0xabcd_value_2\",\"pu\":\"\",\"h\":\"D41D8CD98F00B204E9800998ECF8427ED41D8CD98F00B204E9800998ECF8427E806A4BC867BFFE5FB63E6B4A139BFC1F72310608B875E02B243B6E3C5F458540103D62DEB2014D093BAC5E89BBA589F5C81E728D9D4C2F636F067F89CC14862C72310608B875E02B243B6E3C5F45854072310608B875E02B243B6E3C5F458540C81E728D9D4C2F636F067F89CC14862C\",\"from\":\"0xabcd\",\"to\":\"0xabca\",\"value\":\"2\"},\"condition\":{\"from\":\"0xabcd\",\"to\":\"0xabcd\",\"value\":\"2\"}}";
        miner.onNetworkMsg(testConditionTxMsg.getBytes(), address, testExternalPort);

        ThreadHelper.safeSleep(3 * 1000);
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
        List<Block> blockList = new CopyOnWriteArrayList<Block>();
        blockList.add(BlockChain.constructGenesisBlock());
        blockList.add(Block.constructBlock("00006C2CA920AE27B5A32F72A5C1B5B4", 314214769, 1));
        blockList.add(Block.constructBlock("000933AB43C1A0B4CC96A660B74627E0", -44162486, 2));
        blockList.add(Block.constructBlock("000BF41F6BC6F65C798BD0A7EBF72686", 1767426602, 3));
        return blockList;
    }

    private List<Block> constructTestBlockChainWith6Block() {
        List<Block> blockList = constructTestBlockChainWith4Block();
        blockList.add(Block.constructBlock("0005FD401244D9F9105B96964B8F0CEF", 911077208, 4));
        blockList.add(Block.constructBlock("0009E4E2F511368F265966807D24CD19", -1941014815, 5));
        return blockList;
    }

    @Test
    public void testAddTransaction() {
        Miner miner = new Miner();
        Transaction t = new Transaction();
        t.setData("");
        assert miner.addTransaction(t);
        assert !miner.addTransaction(t);
    }

    @Test
    public void testProcessNewTransactionWithCTxBetween2Peers() throws BlockException, PoWException, InterruptedException {
        // send if_to_0xabcd_value_2 0xabcd 0xabca 2
        String userA = "0xabcdef1";
        String userB = "0xabcdef2";
        AccountManager accountManager = AccountManager.getInstance();
        accountManager.addValue(userA, 100);
        accountManager.addValue(userB, 50);
        Double userAInitValue = accountManager.getBalance(userA);
        Double userBInitValue = accountManager.getBalance(userB);
        System.out.println("userAInitValue2: " + userAInitValue);
        System.out.println("userBInitValue2: " + userBInitValue);

        int value = 2;
        String data = "if_to_" + userA + "_value_" + value;

        int interactionId = 1;
        Transaction transaction = new ConditionalAssociationTransaction(interactionId, new Condition(userA, userB, value));
        transaction.setData(data);
        transaction.setFrom(userA);
        transaction.setToAndValue(userB, value);
        transaction.setId();
        MiningConfiguration.setDifficulty(0);
        Miner miner = new Miner();
        miner.addTransaction(transaction);

        miner.setForceStop();// only run once
        miner.setRunInterval(0);
        miner.mine();

        System.out.println("accountManager.getBalance(userA): " + accountManager.getBalance(userA));
        System.out.println("userAInitValue: " + userAInitValue);
        System.out.println("accountManager.getBalance(userB): " + accountManager.getBalance(userB));
        System.out.println("userBInitValue: " + userBInitValue);
        assert accountManager.getBalance(userA).equals(userAInitValue - value);
        assert accountManager.getBalance(userB).equals(userBInitValue);

        data = "if_to_" + userB + "_value_" + value;

        transaction = new ConditionalAssociationTransaction(interactionId, new Condition(userB, userA, value));
        transaction.setData(data);
        transaction.setFrom(userB);
        transaction.setToAndValue(userA, value);
        transaction.setId(1234);
        miner.addTransaction(transaction);
        miner.setForceStop();// only run once
        miner.setRunInterval(0);
        miner.mine();

        System.out.println("accountManager.getBalance(userA): " + accountManager.getBalance(userA));
        System.out.println("userAInitValue: " + userAInitValue);
        System.out.println("accountManager.getBalance(userB): " + accountManager.getBalance(userB));
        System.out.println("userBInitValue: " + userBInitValue);
        assert accountManager.getBalance(userA).equals(userAInitValue);
        assert accountManager.getBalance(userB).equals(userBInitValue);
    }

    @Test
    public void testProcessNewTransactionWithCTxBetween3Peers() throws BlockException, PoWException, InterruptedException {
        // send if_to_0xabcd_value_2 0xabcd 0xabca 2
        String userA = "0xabcdef31";
        String userB = "0xabcdef32";
        String userC = "0xabcdef33";
        AccountManager accountManager = AccountManager.getInstance();
        accountManager.addValue(userA, 100);
        accountManager.addValue(userB, 50);
        accountManager.addValue(userC, 50);

        Miner miner = new Miner();
        Double userAInitValue = accountManager.getBalance(userA);
        Double userBInitValue = accountManager.getBalance(userB);
        Double userCInitValue = accountManager.getBalance(userC);

        // A -> B with to A as condition
        int value = 2;
        String data = "if_to_" + userA + "_value_" + value;
        int interactionId = 0;
        Transaction transaction = new ConditionalAssociationTransaction(interactionId, new Condition(userA, userB, value));
        transaction.setData(data);
        transaction.setFrom(userA);
        transaction.setToAndValue(userB, value);
        transaction.setId(123401);
        miner.addTransaction(transaction);

        miner.setForceStop();// only run once
        miner.setRunInterval(0);
        miner.mine();
        assert accountManager.getBalance(userA) == userAInitValue - value;
        assert accountManager.getBalance(userB) == userBInitValue;
        assert accountManager.getBalance(userC) == userCInitValue;

        // B->C with to B as condition
        data = "if_to_" + userB + "_value_" + value;
        transaction = new ConditionalAssociationTransaction(interactionId, new Condition(userB, userC, value));
        transaction.setData(data);
        transaction.setFrom(userB);
        transaction.setToAndValue(userC, value);
        miner.addTransaction(transaction);
        miner.setForceStop();// only run once
        miner.setRunInterval(0);
        miner.mine();
        assert accountManager.getBalance(userA) == userAInitValue - value;
        assert accountManager.getBalance(userB) == userBInitValue - value;
        assert accountManager.getBalance(userC) == userCInitValue;

        // C->A with to C as condition
        data = "if_to_" + userC + "_value_" + value;
        transaction = new ConditionalAssociationTransaction(interactionId, new Condition(userC, userA, value));
        transaction.setData(data);
        transaction.setFrom(userC);
        transaction.setToAndValue(userA, value);
        transaction.setId(123405);
        miner.addTransaction(transaction);
        miner.setForceStop();// only run once
        miner.setRunInterval(0);
        miner.mine();
        assert accountManager.getBalance(userA).equals(userAInitValue);
        assert accountManager.getBalance(userB).equals(userBInitValue);
        assert accountManager.getBalance(userC).equals(userCInitValue);
    }

    @Test
    public void testProcessNewTransactionWithCTxBetween4Peers() throws BlockException, PoWException, InterruptedException {
        // send if_to_0xabcd_value_2 0xabcd 0xabca 2
        String userA = "0xabcdef41";
        String userB = "0xabcdef42";
        String userC = "0xabcdef43";
        String userD = "0xabcdef44";
        AccountManager accountManager = AccountManager.getInstance();
        accountManager.addValue(userA, 100);
        accountManager.addValue(userB, 50);
        accountManager.addValue(userC, 50);
        accountManager.addValue(userD, 50);

        Miner miner = new Miner();
        Double userAInitValue = accountManager.getBalance(userA);
        Double userBInitValue = accountManager.getBalance(userB);
        Double userCInitValue = accountManager.getBalance(userC);
        Double userDInitValue = accountManager.getBalance(userD);

        // A -> B with to A as condition
        int value = 2;
        tryMiningCTx(userA, userB, miner, value);
        assert accountManager.getBalance(userA).equals(userAInitValue - value);
        assert accountManager.getBalance(userB).equals(userBInitValue);
        assert accountManager.getBalance(userC).equals(userCInitValue);
        assert accountManager.getBalance(userD).equals(userDInitValue);

        // C->D with to C as condition
        tryMiningCTx(userC, userD, miner, value);
        assert accountManager.getBalance(userA).equals(userAInitValue - value);
        assert accountManager.getBalance(userB).equals(userBInitValue);
        assert accountManager.getBalance(userC).equals(userCInitValue - value);
        assert accountManager.getBalance(userD).equals(userDInitValue);

        // B->C with to B as condition
        tryMiningCTx(userB, userC, miner, value);
        assert accountManager.getBalance(userA) == userAInitValue - value;
        assert accountManager.getBalance(userB) == userBInitValue - value;
        assert accountManager.getBalance(userC) == userCInitValue - value;
        assert accountManager.getBalance(userD) == userDInitValue;

        // D->A with to D as condition
        tryMiningCTx(userD, userA, miner, value);
        assert accountManager.getBalance(userA).equals(userAInitValue);
        assert accountManager.getBalance(userB).equals(userBInitValue);
        assert accountManager.getBalance(userC).equals(userCInitValue);
        assert accountManager.getBalance(userD).equals(userDInitValue);
    }

    // paper test
    @Test
    public void testProcessNewTransactionWithCTxBetween6PeersWithRandomPairs() throws BlockException, PoWException, InterruptedException {
        // send if_to_0xabcd_value_2 0xabcd 0xabca 2
        List<String> userList = new ArrayList<>();
        userList.add("0xabcdef61");
        userList.add("0xabcdef62");
        userList.add("0xabcdef63");
        userList.add("0xabcdef64");
        userList.add("0xabcdef65");
        userList.add("0xabcdef66");
        AccountManager accountManager = AccountManager.getInstance();
        for (String user : userList) {
            accountManager.addValue(user, 50);
        }

        Miner miner = new Miner();
        HashMap<String, Double> userBalance = new HashMap<>();
        for (String user : userList) {
            // this sentence should be put after new Miner as the balance may change when add block
            userBalance.put(user, accountManager.getBalance(user));
        }

        // A -> B with to A as condition
        // while all to
        JGraphTWrapper jGraphTWrapper = new JGraphTWrapper();
        NoDuplicatedRandomHelper toRandomHelper = new NoDuplicatedRandomHelper(userList.size());
        NoDuplicatedRandomHelper fromRandomHelper = new NoDuplicatedRandomHelper(userList.size());
        for (int count = 0; count < userList.size(); ++count) {
            ExchangeUserPair exchangeUserPair = findExchangeUserPair(fromRandomHelper, toRandomHelper, jGraphTWrapper,
                    userList);
            // select one which is select to
            int fromNumber = exchangeUserPair.getFromNumber();
            int toNumber = exchangeUserPair.getToNumber();

            String userFrom = userList.get(fromNumber);
            String userTo = userList.get(toNumber);
            int value = 2;
            tryMiningCTx(userFrom, userTo, miner, value);
            jGraphTWrapper.addEdge(userFrom, userTo, value);
            String user;
            if (fromRandomHelper.isAllFetched()) {
                for (int i = 0; i < userList.size(); ++i) {
                    user = userList.get(i);
                    //assert accountManager.getBalance(user) == userBalance.get(user);
                }
            } else {
                int[] fromStatusClone = fromRandomHelper.getStatusClone();
                for (int i = 0; i < fromStatusClone.length; ++i) {
                    user = userList.get(i);
                    if (1 == fromStatusClone[i]) {
                        //assert accountManager.getBalance(user) == userBalance.get(user) - value;
                    } else {
                        //assert accountManager.getBalance(user) == userBalance.get(user);
                    }
                }
            }
        }
    }

    @Test
    public void testFindExchangeUserPair() {
        List<String> userList = new ArrayList<>();
        userList.add("0xabcdef61");
        userList.add("0xabcdef62");
        userList.add("0xabcdef63");
        userList.add("0xabcdef64");
        userList.add("0xabcdef65");
        userList.add("0xabcdef66");
        JGraphTWrapper jGraphTWrapper = new JGraphTWrapper();
        NoDuplicatedRandomHelper toRandomHelper = new NoDuplicatedRandomHelper(userList.size());
        NoDuplicatedRandomHelper fromRandomHelper = new NoDuplicatedRandomHelper(userList.size());
        for (int i = 0; i < userList.size(); ++i) {
            ExchangeUserPair exchangeUserPair = findExchangeUserPair(fromRandomHelper, toRandomHelper, jGraphTWrapper, userList);
            System.out.println(String.format("Pair %s - %s", exchangeUserPair.fromNumber, exchangeUserPair.toNumber));
            jGraphTWrapper.addEdge(userList.get(exchangeUserPair.fromNumber), userList.get(exchangeUserPair.toNumber), 3);
        }
        assert jGraphTWrapper.getWeaklyConnectedAlsoStronglyConnectedDiagrams().get(0).size() == 6;
    }

    ExchangeUserPair findExchangeUserPair(NoDuplicatedRandomHelper fromRandomHelper, NoDuplicatedRandomHelper toRandomHelper,
                                          JGraphTWrapper jGraphTWrapper, List<String> userList) {
        int toNumber = toRandomHelper.getNumber();
        int fromNumber = fromRandomHelper.getNumber();
        System.out.println(String.format("Init from %d, to %d ", fromNumber, toNumber));
        while (!isMatched(jGraphTWrapper, userList.get(fromNumber), userList.get(toNumber))) {
            if (!fromRandomHelper.putBackWithAtLeastOneLeft(fromNumber)) {
                System.out.println("[Error] Cannot switch to other node for from peer.");
                break;
            }
            fromNumber = fromRandomHelper.getNumber();
        }
        System.out.println(String.format("After from %d, to %d", fromNumber, toNumber));
        return new ExchangeUserPair(fromNumber, toNumber);
    }

    @Test
    public void testIsMatchedWithSpecialCase() {
        JGraphTWrapper j = new JGraphTWrapper();
        List<String> userList = new ArrayList<>();
        userList.add("0xabcdef61");
        userList.add("0xabcdef62");
        userList.add("0xabcdef63");
        userList.add("0xabcdef64");
        userList.add("0xabcdef65");
        userList.add("0xabcdef66");
        int fromNumber1 = 1;
        int toNumber1 = 0;
        int fromNumber2 = 0;
        int toNumber2 = 2;
        int fromNumber3 = 4;
        int toNumber3 = 1;
        int fromNumber4 = 5;
        int toNumber4 = 3;
        int value = 4;
        boolean isLast = false;
        String fromUser1 = userList.get(fromNumber1), toUser1 = userList.get(toNumber1);
        String fromUser2 = userList.get(fromNumber2), toUser2 = userList.get(toNumber2);
        String fromUser3 = userList.get(fromNumber3), toUser3 = userList.get(toNumber3);
        String fromUser4 = userList.get(fromNumber4), toUser4 = userList.get(toNumber4);
        // user equal is the same
        j.addEdge(fromUser1, toUser1, value);
        j.addEdge(fromUser2, toUser2, value);
        j.addEdge(fromUser3, toUser3, value);
        j.addEdge(fromUser4, toUser4, value);
        assert !isMatched(j, toUser2, fromUser3);
        assert isMatched(j, toUser2, fromUser4);
    }

    @Test
    public void testIsMatched() {
        JGraphTWrapper j = new JGraphTWrapper();
        List<String> userList = new ArrayList<>();
        userList.add("0xabcdef61");
        userList.add("0xabcdef62");
        userList.add("0xabcdef63");
        userList.add("0xabcdef64");
        userList.add("0xabcdef65");
        userList.add("0xabcdef66");
        int fromNumber1 = 1;
        int fromNumber2 = 5;
        int toNumber1 = 0;
        int toNumber2 = 3;
        int fromNumber3 = 4;
        int toNumber3 = 2;
        int value = 4;
        boolean isLast = true;
        String fromUser1 = userList.get(fromNumber1), toUser1 = userList.get(toNumber1);
        String fromUser2 = userList.get(fromNumber2), toUser2 = userList.get(toNumber2);
        String fromUser3 = userList.get(fromNumber3), toUser3 = userList.get(toNumber3);
        // user equal is the same
        assert !isMatched(j, fromUser1, fromUser1);
        isLast = false;
        assert isMatched(j, fromUser1, toUser1);
        j.addEdge(fromUser1, toUser1, value);
        j.addEdge(fromUser2, toUser2, value);
        assert !isMatched(j, fromUser1, toUser1);
        assert !isMatched(j, toUser1, fromUser1);
        assert isMatched(j, fromUser1, fromUser2);

        j.addEdge(fromUser3, toUser3, value);
        assert !isMatched(j, fromUser3, toUser3);
        assert !isMatched(j, toUser3, fromUser3);
        assert isMatched(j, fromUser1, toUser2);
        assert isMatched(j, toUser1, fromUser2);

        j.addEdge(toUser2, fromUser1, value);
        assert !isMatched(j, fromUser1, toUser1);
        assert !isMatched(j, toUser1, fromUser1);
        assert !isMatched(j, fromUser1, toUser2);
        assert !isMatched(j, toUser1, fromUser2);
    }

    boolean isMatched(JGraphTWrapper jGraphTWrapper, String fromUser, String toUser) {
        if (fromUser.equals(toUser)) return false;

        return !jGraphTWrapper.isWeaklyConnect(fromUser, toUser);
    }

    private void tryMiningCTx(String userFrom, String userTo, Miner miner, int value) throws BlockException, PoWException, InterruptedException {
        String data = "if_to_" + userFrom + "_value_" + value;
        Transaction transaction;
        int interactionId = 0;
        transaction = new ConditionalAssociationTransaction(interactionId, new Condition(userFrom, userTo, value));
        transaction.setData(data);
        transaction.setFrom(userFrom);
        transaction.setToAndValue(userTo, value);
        transaction.setId();
        miner.addTransaction(transaction);
        miner.setForceStop();// only run once
        miner.setRunInterval(0);
        miner.mine();
    }
}
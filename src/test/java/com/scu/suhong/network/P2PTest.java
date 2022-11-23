package com.scu.suhong.network;

import Service.BlockchainService;
import Service.BlockchainServiceThread;
import com.scu.suhong.block.BlockChain;
import com.scu.suhong.miner.Miner;
import junit.framework.TestCase;
import org.junit.Test;
import util.StringHelper;
import util.ThreadHelper;

import java.io.IOException;
import java.net.InetAddress;

public class P2PTest extends TestCase {

    public void testSendTransactionMsgToListener() throws IOException {
        P2P p2P = new P2P();
        String msg1 = "msg1";
        assert(p2P.isNewMessage(msg1.getBytes()));
        p2P.markAsOldReceivedMessage(msg1.getBytes());
        assert(!p2P.isNewMessage(msg1.getBytes()));
        String msg2 = "msg2";
        assert(p2P.isNewMessage(msg2.getBytes()));
    }

    public void testSendTransactionMsgToListener1() throws IOException, NetworkException {
        NetworkListener networkListener = new NetworkListener() {
            @Override
            public void onNetworkMsg(byte[] message, InetAddress address, int port) {
                System.out.println("[Test] testSendTransactionMsgToListener: get one message from " + address + ":" + port);
            }
        };
        P2P p2P = new P2P(networkListener);
        String msg1 = "msg1";
        InetAddress address = null;
        int port = 7601;
        p2P.sendMsgToLocalListener(msg1.getBytes(), address, port);
        System.out.println("[Test] Begin to send to 127.0.0.1");
        p2P.send(msg1.getBytes(), "127.0.0.1", StringHelper.getDefaultPort());
        ThreadHelper.safeSleep(2*1000);
        System.out.println("[Test] Begin to send to neighbours");
        p2P.sendDataToNeighbours(msg1.getBytes());
    }

    public void testProcessData() throws IOException {
        BlockChain.reset();// update the block state

        Miner miner = new Miner();

        P2P p2P = new P2P(miner);
        byte[] testData = "{\"id\":\"123\"}".getBytes();

        byte[] ipAddr = new byte[]{(byte)192, (byte)168, 30, 1};
        InetAddress address = InetAddress.getByAddress(ipAddr);
        int port = 7601;
        assert(p2P.processData(testData, address, port));
        assert(!p2P.processData(testData, address, port));
        assert(!p2P.processData(testData, address, port));

        String testDataString = StringHelper.getQueryTopBlockMsg() + ":" + "127.0.0.1";
        testData = testDataString.getBytes();
        assert(p2P.processData(testData, address, port));
        assert(p2P.processData(testData, address, port));
    }

    public void testProcessDataWithDynamicalAsset() throws IOException {
        BlockChain.reset();// update the block state
        Miner miner = new Miner();
        P2P p2P = new P2P(miner);
        byte[] testData = "{\"b\":{\"ts\":[{\"owner\":\"owner\",\"code\":\"public class AssetT11 extends DynamicalAsset{\\n @Override\\n public boolean check(){\\n return true;\\n }\\n public AssetT11 getIntstance(){\\n return (AssetT11)@GlobalInstance;\\n }\\n}\\n \",\"blockIndex\":\"5\",\"data\":\"data\",\"DynamicalAsset\":\"identifier\",\"gas\":10000,\"h\":\"516AB9B12C221D6E63034FD882C1C3F5\",\"className\":\"AssetT11\",\"id\":909798928,\"kv\":{\"k1\":\"v1\",\"k2\":\"v2\",\"k3\":\"v3\"}}]},\"h\":{\"p\":\"000000282C27BABD938A90D167176E40\",\"timeStamp\":\"2019.11.14_04.44.20\",\"index\":5,\"n\":508397141,\"miner\":\"111?45678_01\"},\"hash\":\"000000C20946D59DF80F92D3DB86E17A\"}".getBytes();

        byte[] ipAddr = new byte[]{(byte)192, (byte)168, 30, 1};
        InetAddress address = InetAddress.getByAddress(ipAddr);
        int port = 9601;
        assert(p2P.processData(testData, address, port));
        assert(!p2P.processData(testData, address, port));
    }

    @Test
    public void testProcessBlockWithACPTransaction() throws IOException {
        BlockchainService blockchain = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchain);
        Thread mainThread = new Thread(blockchainServiceThread, "Blockchain service in P2P test");
        mainThread.setPriority(Thread.MIN_PRIORITY);
        mainThread.start();
        Miner miner = BlockchainService.getInstance().getMiner();
        while (true){
            ThreadHelper.safeSleep(1*1000);
            miner = BlockchainService.getInstance().getMiner();
            if (null != miner) break;
        }
        System.out.println("\n[Test][Debug] Blockchain service is ready");
        P2P p2P = null;
        NetworkService networkService = null;
        while (true){
            ThreadHelper.safeSleep(1*1000);
            networkService = BlockchainService.getInstance().testGetNetworkService();
            if (null == networkService) continue;;
            p2P = networkService.testGetP2P();
            if (null != p2P) break;;
        }
        System.out.println("\n[Test][Debug] Network service is ready");

        byte[] testData = "{\"b\":{\"ts\":[{\"ActionCommonTransaction\":\"ActionCommonTransaction\",\"owner\":\"111?default\",\"ACPTokenTransaction\":\"ACPTokenTransaction\",\"blockIndex\":\"14\",\"data\":\"\",\"gas\":0,\"h\":\"E147D9E6BCADE9842F29F4F6181769AF\",\"className\":\"ACPTokenTransaction\",\"conditionName\":\"IncomingRandomTrigger1\",\"id\":366164370,\"blockchainID\":\"111\"}]},\"h\":{\"p\":\"00000C6566FCA8AEB3B04790F342C301\",\"timeStamp\":\"2020.03.18_11.31.07\",\"index\":14,\"n\":958177448,\"miner\":\"111?45678_02\"},\"hash\":\"00000E86EC7A9FA71682875B75FFE207\"}".getBytes();

        byte[] ipAddr = new byte[]{(byte)192, (byte)168, 1, 103};
        InetAddress address = InetAddress.getByAddress(ipAddr);
        int port = 9604;// external address in peer config
        assert(p2P.processData(testData, address, port));
        assert(!p2P.processData(testData, address, port));
    }

    public void testSend() throws IOException, InterruptedException {
        P2PConfiguration.getInstance().setDeviationWaitTime(1000);
        P2PConfiguration.getInstance().setMeanWaitTime(2000);

        P2P p2p = new P2P(new Miner("No service"));
        p2p.send("P2Ptest".getBytes(), "127.0.0.1", 9601);
        p2p.send("P2Ptest".getBytes(), "127.0.0.1", 9601);
        p2p.send("P2Ptest".getBytes(), "127.0.0.1", 9601);
        Thread.sleep(5000);
    }
}
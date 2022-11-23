package Service;

import account.AccountManager;
import com.scu.suhong.miner.Miner;
import com.scu.suhong.smartcontract.nondeterminacy.State;
import com.scu.suhong.smartcontract.nondeterminacy.StateSequence;
import com.scu.suhong.smartcontract.nondeterminacy.StateSyncManager;
import consensus.pow.MiningConfiguration;
import mockit.Mocked;
import org.junit.Test;
import util.ThreadHelper;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BlockchainServiceTest {

    @Test
    public void testService() throws InterruptedException, IOException {
        BlockchainService blockchain = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchain);
        Thread mainThread = new Thread(blockchainServiceThread, "Block chain test main thread");
        mainThread.start();
        Thread.sleep(1000);// sleep 1 seconds
        List<String> arguments = new ArrayList<>();
        arguments.add("Tx1");
        arguments.add("0xabc");
        arguments.add("0xabd");
        arguments.add("12");
        blockchain.triggerTransaction(arguments);
        arguments.set(0, "Tx2");
        blockchain.triggerTransaction(arguments);
        arguments.set(0, "Tx3");
        blockchain.triggerTransaction(arguments);
        Thread.sleep(2000);// sleep 2 seconds
        blockchain.stopService();
        mainThread.join();
    }

    @Test
    public void testDuplicateService() throws InterruptedException, IOException {
        BlockchainService blockchain = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchain);
        Thread mainThread = new Thread(blockchainServiceThread, "Block chain test main thread");
        mainThread.start();
        Thread.sleep(1000);// sleep 1 seconds
        List<String> arguments = new ArrayList<>();
        arguments.add("Tx1");
        arguments.add("0xabc");
        arguments.add("0xabd");
        arguments.add("12");
        blockchain.triggerTransaction(arguments);
        blockchain.triggerTransaction(arguments); // TO DO add some assert
        Thread.sleep(2000);// sleep 2 seconds
        blockchain.stopService();
        mainThread.join();
    }

    @Test
    public void testStateSyncMessageSend() throws InterruptedException, IOException {
        BlockchainService blockchain = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchain);
        Thread mainThread = new Thread(blockchainServiceThread, "Block chain test main thread");
        mainThread.start();
        while (!blockchain.isReady()) {
            Thread.sleep(1000);// sleep 1 seconds
            System.out.println("Blockchain is not ready");
        }
        List<String> arguments = new ArrayList<>();
        String smartContractName = "smart contract";
        String varietyName = "variety name";
        String value = "state1#2020-2-2";
        arguments.add(smartContractName);
        arguments.add(varietyName);
        arguments.add(value);
        blockchain.triggerStateSyncTransaction(arguments);

        Thread.sleep(1000);// sleep 2 seconds

        StateSequence stateSequence = StateSyncManager.getStateSequence(smartContractName, varietyName);
        State state = new State(value);
        assert stateSequence.doesStateExist(state);

        blockchain.stopService();
        mainThread.join();
    }

    @Test
    public void testStateSyncMessageReceive() throws InterruptedException, IOException {
        BlockchainService blockchain = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchain);
        Thread mainThread = new Thread(blockchainServiceThread, "Block chain test main thread");
        mainThread.start();
        while (!blockchain.isReady()) {
            Thread.sleep(1000);// sleep 1 seconds
            System.out.println("Blockchain is not ready");
        }

        String smartContractName = "smart contract";
        String varietyName = "variety name";
        String value = "state1#2020-2-2";

        String receivedMessage = "{\"owner\":\"\",\"SyncTransaction\":\"identifier\",\"blockIndex\":0,\"smartContractName\":\"smart contract\",\"gas\":0,\"h\":\"09C0677A763022672B95D6FEA4AB1898\",\"id\":1060887404,\"varietyName\":\"variety name\",\"value\":\"state1#2020-2-2\"}";
        InetAddress testIp=InetAddress.getByName("10.10.10.10");
        int testPort = 7601;
        blockchain.testGetNetworkService().testGetP2P().processData(receivedMessage.getBytes(), testIp, testPort);

        Thread.sleep(1000);// sleep 2 seconds

        StateSequence stateSequence = StateSyncManager.getStateSequence(smartContractName, varietyName);
        State state = new State(value);
        assert stateSequence.doesStateExist(state);

        blockchain.stopService();
        mainThread.join();
    }

    @Test
    public void testTryGetConditionalParameters() {
        BlockchainService blockchainService = BlockchainService.getInstance();
        String transactionData = "if_to_0xabcd_value_2";
        HashMap<String, String> conditionalParameters = blockchainService.tryGetConditionalParameters(transactionData);
        assert conditionalParameters.get("value").equals("2");
        assert conditionalParameters.get("to").equals("0xabcd");
    }

    @Test
    public void testTriggerConditionalTransaction(@Mocked final Miner miner) throws InterruptedException, IOException {
        BlockchainService blockchain = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchain);
        Thread mainThread = new Thread(blockchainServiceThread, "Block chain test main thread");
        mainThread.start();
        Thread.sleep(1000);// sleep 1 seconds
        List<String> arguments = new ArrayList<>(); // send data from to value <asset type>
        arguments.add("if_to_0xabcd_value_2");
        arguments.add("0xabcd");
        arguments.add("0xabd");
        arguments.add("2");
        blockchain.triggerTransaction(arguments);
        Thread.sleep(2000);// sleep 2 seconds
        blockchain.stopService();
        mainThread.join();
    }

    @Test
    public void testTriggerNotaryTransaction(@Mocked final Miner miner) throws InterruptedException, IOException {
        MiningConfiguration.testSetRequiredZeroCount(1);
        BlockchainService blockchain = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchain);
        Thread mainThread = new Thread(blockchainServiceThread, "Block chain test main thread");
        mainThread.start();
        while (null == blockchain.getMiner()) ThreadHelper.safeSleepSecond(1);
        blockchain.getMiner().setRunInterval(1);
        Thread.sleep(1000);// sleep 1 seconds
        AccountManager.getInstance().addValue("from", 3);
        List<String> arguments = new ArrayList<>(); // send data from to value <asset type>
        arguments.add("10001");
        arguments.add("notary_first_txonbc1");
        arguments.add("from");
        arguments.add("to");
        arguments.add("2");
        blockchain.triggerCommonCrosschainTransaction(arguments);
        Thread.sleep(2000);// sleep 2 seconds
        blockchain.stopService();
    }

    @Test
    public void replaceNullToEmpty() {
        String test = "null";
        assert BlockchainService.replaceNullToEmpty(test).isEmpty();
        test = "NULL";
        assert BlockchainService.replaceNullToEmpty(test).isEmpty();
        test = "nUll";
        assert BlockchainService.replaceNullToEmpty(test).isEmpty();
        test = "NUll";
        assert BlockchainService.replaceNullToEmpty(test).isEmpty();

        test = "abc";
        assert test.equals(BlockchainService.replaceNullToEmpty(test));
    }
}
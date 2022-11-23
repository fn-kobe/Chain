package com.scu.suhong.smartcontract.nondeterminacy.embedded;

import Service.BlockchainService;
import Service.BlockchainServiceThread;
import com.scu.suhong.smartcontract.nondeterminacy.StateSyncManager;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NBSStateTest {

    @Test
    public void start() throws InterruptedException {
        BlockchainService blockchain = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchain);
        Thread mainThread = new Thread(blockchainServiceThread, "Block chain test main thread");
        mainThread.start();
        while (!blockchain.isReady()) {
            Thread.sleep(1000);// sleep 1 seconds
            System.out.println("Blockchain is not ready");
        }

        String NBSIp = "http://127.0.0.1:8080";
        String runTime = "5";//5 seconds
        NBSState.start(NBSIp, runTime);

        // Please make sure there is some record in the history file
        assert 0 != StateSyncManager.getStateSequence(NBSState.smartContractName, NBSState.variety1).getStateListCopy().size();

        blockchain.stopService();
        mainThread.join();
    }

    @Test
    public void startWithoutHistory() throws InterruptedException {
        BlockchainService blockchain = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchain);
        Thread mainThread = new Thread(blockchainServiceThread, "Block chain test main thread");
        mainThread.start();
        while (!blockchain.isReady()) {
            Thread.sleep(1000);// sleep 1 seconds
            System.out.println("Blockchain is not ready");
        }

        String NBSIp = "http://127.0.0.1:8080";
        String runTime = "5";//5 seconds
        NBSState.startWithoutHistory(NBSIp, runTime);

        blockchain.stopService();
        mainThread.join();
    }

    @Test
    public void getStateFromNBS() throws IOException {
        String NBSIp = "http://127.0.0.1:8080";
        String r = NBSState.getStateFromNBS(NBSIp);
        System.out.println(r);
    }

    @Test
    public void getStateHistoryFromNBS() throws IOException {
        String NBSIp = "http://127.0.0.1:8080";
        String r = NBSState.getStateHistoryFromNBS(NBSIp);
        System.out.println(r);
    }


    @Test
    public void cc(){
        List<State> checkStateSequence = new ArrayList<>();
        List<State> expectStateSequence = new ArrayList<>();
        State s1 = new State(1, "s1");
        State s2 = new State(2, "s2");
        State s3 = new State(3, "s3");
        State s4 = new State(4, "s4");
        checkStateSequence.add(s1);
        checkStateSequence.add(s2);
        checkStateSequence.add(s3);

        expectStateSequence.add(s1);
        expectStateSequence.add(s2);
        expectStateSequence.add(s3);

        assert compare(checkStateSequence, expectStateSequence).equals("correct");

        checkStateSequence.clear();
        checkStateSequence.add(s2);
        checkStateSequence.add(s1);
        checkStateSequence.add(s3);
        assert !compare(checkStateSequence, expectStateSequence).equals("correct");

        checkStateSequence.clear();
        checkStateSequence.add(s1);
        checkStateSequence.add(s3);
        assert compare(checkStateSequence, expectStateSequence).equals("correct");

        checkStateSequence.clear();
        assert compare(checkStateSequence, expectStateSequence).equals("correct");

        checkStateSequence.clear();
        checkStateSequence.add(s1);
        checkStateSequence.add(s2);
        checkStateSequence.add(s3);
        checkStateSequence.add(s4);
        assert compare(checkStateSequence, expectStateSequence).equals("correct");

        expectStateSequence.clear();
        assert compare(checkStateSequence, expectStateSequence).equals("correct");
    }


    class State {
        int i = 0;
        String s = "";

        public State(int i, String s) {
            this.i = i;
            this.s = s;
        }
    }

    String compare(List<State> snbc, List<State> ess){
        //The position of the previous state found in ESS
        int previousPosition = -1;
        for (State s_sync : snbc) {
            for (int i = 0; i < ess.size(); ++i)
                if (s_sync.s.equals(ess.get(i).s)) {
                    // The position of the previous state is after the current one
                    // It is wrong as the time order is not as in ESS
                    if (previousPosition >= i) {
                        return "error";

                    }
                    previousPosition = i;
                }
        }
        return "correct";
    }
}

package model;

import junit.framework.TestCase;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OneChainTest extends TestCase {
    OneChain oneChain = new OneChain();

    public void testSimulateNode(){
        oneChain.simulateNode();
    }

    public void testSimulateProduce(){
        Queue<String> txQueue = new ConcurrentLinkedQueue<>();
        oneChain.simulateProduce(txQueue);
    }

    public void testMining() {
        Queue<String> txQueue = new ConcurrentLinkedQueue<>();
        txQueue.add("t1");
        txQueue.add("t2");
        oneChain.simulateMining(txQueue);
    }

}
package com.scu.suhong.network;

import com.scu.suhong.miner.Miner;
import com.scu.suhong.transaction.Transaction;
import junit.framework.TestCase;
import util.StringHelper;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class NetworkServiceTest extends TestCase {
    private Miner miner = new Miner();

    public void testStartService() throws InterruptedException, IOException, NoSuchAlgorithmException {
        Thread minerThread = new Thread(miner, "test network service");
        minerThread.start();

        NetworkService networkService = new NetworkService(miner);
        Thread listenerThread = new Thread(networkService, "test socket listner");
        listenerThread.start();

        // send
        Thread.sleep(500);
        System.out.println("Try to send message");
        Transaction t = new Transaction();
        t.setData("test tx1");
        networkService.sendTransaction(t);

        Thread.sleep(500);
        miner.setForceStop();
        networkService.setForceStop();

        listenerThread.join();
        minerThread.join();
    }

    public void testStartServiceWithMessage() throws InterruptedException, IOException, NoSuchAlgorithmException {
        Thread minerThread = new Thread(miner, "test network service");
        minerThread.start();

        NetworkService networkService = new NetworkService(miner);
        Thread listenerThread = new Thread(networkService, "test socket listner");
        listenerThread.start();

        // send
        Thread.sleep(500);
        System.out.println("[Test] Try to send message");
        P2P p2P = networkService.testGetP2P();
        p2P.send("{Tx: test message}", "127.0.0.1", StringHelper.getDefaultPort());

        Thread.sleep(500);
        miner.setForceStop();
        networkService.setForceStop();

        listenerThread.join();
        minerThread.join();
    }

}
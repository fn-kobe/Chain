package com.scu.suhong.network;

import com.scu.suhong.dynamic_definition.AbstractTransaction;

import java.io.IOException;

public class NetworkService implements Runnable {
    private P2P p2p;

    public NetworkService(NetworkListener listener) {
        p2p = new P2P(listener);
    }

    public NetworkService(NetworkListener listener, int port, P2P.P2PType p2PType) {
        p2p = new P2P(listener, port, p2PType);
    }

    public void startService() throws NetworkException {
        p2p.beginToListen();
    }

    // send to peers
    public synchronized void sendTransaction(AbstractTransaction transaction) throws IOException {
        if (!transaction.isValid()){
            System.out.println("[NetworkService] Transaction format error: " + transaction.getJson());
        }

        p2p.send(transaction);
    }

    @Override
    public void run() {
        try {
            startService();
        } catch (NetworkException e) {
            System.out.printf("[NetworkService] Exception in Listener: \n" + e);
        }
    }

    public void setForceStop(){
        p2p.setForceStop();
    }

    public P2P testGetP2P(){
        return p2p;
    }
}

package com.scu.suhong.network;

import org.apache.log4j.Logger;
import util.FileLogger;
import util.ThreadHelper;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class P2PThread implements Runnable {
    static Logger logger = FileLogger.getLogger();

    DatagramPacket packet;
    DatagramSocket sendSocket;
    byte[] msg;

    public P2PThread(DatagramPacket packet, DatagramSocket socket, byte[] msg) {
        this.packet = packet;
        this.sendSocket = socket;
        this.msg = msg;
    }

    @Override
    public void run() {
        try {
            ThreadHelper.p2PSimulateDelay();
            sendSocket.send(packet);
            logger.debug("[P2PThread] Finish to send transaction message");
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("[P2PThread] Failed to send transaction message: " + msg.toString());
        }
    }
}

package com.scu.suhong.network;

import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.block.Block;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import consensus.pow.MiningConfiguration;
import org.apache.log4j.Logger;
import util.BufferHelper;
import util.FileLogger;
import util.StringHelper;
import util.TimeHelper;

import java.io.IOException;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class P2P {
    static Logger logger = FileLogger.getLogger();
    static DatagramSocket sendReceiveSocket;
    static HashMap<String, String> receivedMessageHash;
    static HashMap<String, String> sentMessageHash;
    final int runInterval = 600 * 1000;//10 seconds
    int sendReceivePort = -1;
    P2PConfiguration p2PConfiguration;
    P2PType p2PType = P2PType.EInterBlockchain;
    private NetworkListener listener;
    private boolean forceStop;

    ;

    public P2P() {
        commonInit();
    }

    public P2P(NetworkListener listener) {
        this.listener = listener;
        commonInit();
    }

    public P2P(NetworkListener listener, int port, P2PType p2PType) {
        this.listener = listener;
        sendReceivePort = port;
        this.p2PType = p2PType;
        commonInit();
    }

    public P2P(NetworkListener listener, int port) {
        this.listener = listener;
        sendReceivePort = port;
        commonInit();
    }

    static synchronized public void markAsOldReceivedMessage(byte[] data) {
        String receivedHash;
        try {
            receivedHash = StringHelper.byteArrayToHexString(MD5Hash.getValue(data));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }
        receivedMessageHash.put(receivedHash, receivedHash);
    }

    static synchronized public void markAsOldSentMessage(Block block) {
        markAsOldSentMessage(block.getJson().toString());
    }

    static synchronized public void markAsOldSentMessage(AbstractTransaction t) {
        markAsOldSentMessage(t.getJson().toString());
    }

    static synchronized public void markAsOldSentMessage(String msg) {
        String sentHash;
        try {
            sentHash = StringHelper.byteArrayToHexString(MD5Hash.getValue(msg.getBytes()));
            logger.info("[P2P]Mark message as old:" + sentHash);
            logger.info("[P2P]The message sent is:" + msg);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }
        sentMessageHash.put(sentHash, sentHash);
    }

    static synchronized public boolean isNewMessage(byte[] data) {
        String receivedHash;
        try {
            receivedHash = StringHelper.byteArrayToHexString(MD5Hash.getValue(data));
        } catch (NoSuchAlgorithmException e) {
            logger.info(e.getMessage());
            e.printStackTrace();
            return false;
        }
        if (receivedMessageHash.containsKey(receivedHash) || sentMessageHash.containsKey(receivedHash)) {
            logger.info(String.format("[P2P] Packet %s already got or was sent by ourselves, skip sending to miner\n", receivedHash));
            return false;
        } else {
            logger.info("[P2P] New packet received with hash: " + receivedHash);
        }
        return true;
    }

    private void commonInit() {
        forceStop = false;
        p2PConfiguration = P2PConfiguration.getInstance();
        receivedMessageHash = new HashMap<>();
        sentMessageHash = new HashMap<>();
        sendReceivePort = MiningConfiguration.getSelfListenPort();
        safeInitSocket();
    }

    // Only get internal and external address with respect to P2pType
    List<String> getPeerAddressList() {
        return p2PConfiguration.getPeerAddressListWithoutSelf();
    }

    // Only get internal and external address with respect to P2pType
    List<Integer> getPeerPortList() {
        return p2PConfiguration.getPeerPortListWithoutSelf();
    }

    // Get internal and external address
    List<String> getAllBlockchainObserverAddressList() {
        return p2PConfiguration.getAllBlockchainObserverAddressList();
    }

    // Get internal and external address
    List<Integer> getAllBlockchainObserverPortList() {
        return p2PConfiguration.getAllBlockchainObserverPortList();
    }

    /*     Skip isNewMessage check for transaction, as we can send the transaction with the same data
         the same sender, receiver, and data
         If the transaction is changed to unique, please check isNewMessage.
         */
    public void send(AbstractTransaction t) throws IOException {
        System.out.println("[P2P] try to send transaction to peers");
        markAsOldSentMessage(t);
        // we only send transaction to internal peers as they will mining
        send(t.getJson().toString(), P2PConfiguration.getInstance().getPeerAddressListWithoutSelf(),
                P2PConfiguration.getInstance().getPeerPortListWithoutSelf());
    }

    /*     Skip isNewMessage check for block
            As we may send the block to other again and again for computing node.
            If the send is separated, we can check isNewMessage
     */
    public void send(Block block) throws IOException {
        System.out.println("[P2P] try to send block to peers");
        markAsOldSentMessage(block);
        // if send to external this will force the blockchain to have the relation of  (BC' BC) (BC BC'')  BC' will send package
        // to BC and will reagdred as external. While its external is BC''
        //send(block.getJson().toString(), getAllPeerAddress());// if send to external this will
        //send(block.getJson().toString(), getPeerAddressList(), getPeerPortList());// only forward to peers and external peers will query
        // Send to all peer at 2020-03-09
        send(block.getJson().toString(), getAllBlockchainObserverAddressList(), getAllBlockchainObserverPortList());
    }

    public void send(byte[] msg) throws IOException {
        send(msg, getPeerAddressList(), getPeerPortList());
    }

    void dumpList(List list){
        String r = "";
        for (int i = 0; i < list.size(); ++i){
            r += " " + list.get(i);
        }
        System.out.println(r);
    }

    public void send(String msg, List<String> ipAddresses, List<Integer> peerPorts) throws IOException {
        if (ipAddresses.size() != peerPorts.size()) {
            System.out.println("[P2P][ERROR] The number of ip address is not the same with peer port when sending string msg");
            dumpList(ipAddresses);dumpList(peerPorts);
            return;
        }

        for (int i = 0; i < ipAddresses.size(); ++i) {
            System.out.println("[P2P][Debug] Send to " + ipAddresses.get(i) + ":" + peerPorts.get(i) + " at " + TimeHelper.getCurrentTimeUsingCalendar());
            send(msg, ipAddresses.get(i), peerPorts.get(i));
        }
    }

    public void send(byte[] msg, List<String> ipAddresses, List<Integer> peerPorts) throws IOException {
        if (ipAddresses.size() != peerPorts.size()) {
            System.out.println("[P2P][ERROR] The number of ip address is not the same with peer port when sending byte msg");
            dumpList(ipAddresses);dumpList(peerPorts);
            return;
        }
        for (int i = 0; i < ipAddresses.size(); ++i) {
            send(msg, ipAddresses.get(i), peerPorts.get(i));
        }
    }

    public void send(String msg, String ipAddress, int peerPort) throws IOException {
        send(msg.getBytes(), ipAddress, peerPort);
    }

    public void send(byte[] msg, String ipAddress, int peerPort) throws IOException {
        //TO DO open this log want to debug the message
        if (null == sendReceiveSocket) {
            initSocket();
        }

        DatagramPacket packet = new DatagramPacket(msg,
                msg.length,
                InetAddress.getByName(ipAddress),
                peerPort);

        P2PPerformanceThread.getInstance().addNewSendCount(msg.length);
        if (P2PConfiguration.getInstance().isExternal(ipAddress, peerPort)){
            P2PPerformanceThread.getInstance().addNewExternalSendCount(msg.length);
        }

        Thread sendThread = new Thread(new P2PThread(packet, sendReceiveSocket, msg), "Message send thread");
        sendThread.start();
    }

    void beginToListen() throws NetworkException {
        beginToListen(sendReceivePort);
    }

    synchronized private void safeInitSocket() {
        try{
            initSocket();
        } catch (SocketException e){
            System.out.println("[P2P][ERROR] Fail to init send and receive port ");
            sendReceiveSocket = null;
        }
    }

    synchronized private void initSocket() throws SocketException {
        if (null == sendReceiveSocket) {
            System.out.println("[P2P] Try to init send and receive port " + sendReceivePort);
            sendReceiveSocket = new DatagramSocket(sendReceivePort);
            sendReceiveSocket.setSoTimeout(runInterval);
        }
    }

    void beginToListen(int port) throws NetworkException {
        try {
            logger.info("[P2P] Begin to listen on port: " + port);
            if (null == sendReceiveSocket) {
                initSocket();
            }
            byte[] buf = new byte[BufferHelper.getMaxBufferSize()];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            while (!this.forceStop) {
                logger.debug("[P2P] Listen transaction packet");
                try {
                    sendReceiveSocket.receive(packet);
                } catch (SocketTimeoutException e) {
                    logger.debug("[P2P] Socket receive timeout, try receive again");
                    continue;
                }
                byte[] data = Arrays.copyOf(packet.getData(), packet.getLength()); // remove unnecessary chars
                P2PPerformanceThread.getInstance().addNewReceiveCount(data.length);
                if (P2PConfiguration.getInstance().isExternal(packet.getAddress().getHostAddress(), packet.getPort())){
                    P2PPerformanceThread.getInstance().addNewExternalReceiveCount(data.length);
                }
                processData(data, packet.getAddress(), packet.getPort());
            }
        } catch (IOException e) {
            logger.info(e);
            throw new NetworkException();
        } finally {
            shutDown();
        }
        logger.info("[P2P] End of socket listen");
    }

    public synchronized boolean processData(byte[] data, InetAddress address, int port) throws IOException {
        if (isNewMessage(data)) {
            logger.info("[P2P] Process new packet");
            sendMsgToLocalListener(data, address, port);
            if (StringHelper.isGetTopBlockRequest(data)) {// don't forward get top block msg
                logger.info("[P2P] Query top block message. Don't forward it");
                return true;
            }
            // We require there are internal peers and we only forward the internal blockchain message
            // External blockchain message is synced directly with external BC currently
            // You can change this and pay attentions to that the external msg(blocks) can be got from an internal address
            if (!getPeerAddressList().isEmpty() && !p2PConfiguration.isExternal(address.getHostAddress(), port)) {
                sendDataToNeighbours(data);
            }
            markAsOldReceivedMessage(data);
            return true;
        } else {
            return false;
        }
    }

    public void sendMsgToLocalListener(byte[] messageContent, InetAddress address, int port) {
        logger.info("[P2P] Send network message to network listener (Miner which will process or forward)");
        try {
            listener.onNetworkMsg(messageContent, address, port);
        } catch (Exception e) {
            System.out.printf("[P2P][WARN] Send message %s from %s at port %d error: %s\n", new String(messageContent),
                    (null == address) ? "null":address.getHostAddress(), port, e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendDataToNeighbours(byte[] msg) throws IOException {
        send(msg);
    }

    public void setForceStop() {
        this.forceStop = true;
    }

    void shutDown() {
        if (null != sendReceiveSocket) {
            sendReceiveSocket.close();
        }
    }

    public enum P2PType {
        EInterBlockchain
    }
}


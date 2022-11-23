package com.scu.suhong.network;

import java.net.InetAddress;

public interface NetworkListener {
    void onNetworkMsg(byte[] message, InetAddress address, int port);
}

package com.scu.suhong.network;

import junit.framework.TestCase;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class P2PConfigurationTest extends TestCase {

    public void testGetPeerAddress() {
        //prepare
        creatTestPeerAddressConfigurationFile();
        P2PConfiguration p2PConfiguration = P2PConfiguration.getInstance();
        List<String> peerAddressList = p2PConfiguration.getPeerAddressList();
        assert(3 == peerAddressList.size());
        assert(peerAddressList.get(peerAddressList.size()-1).equals("127.0.0.1"));
    }

    private void creatTestPeerAddressConfigurationFile(){
        final String testIP1 = "1.1.1.1";
        final String testIP2 = "202.115.23.100";
        final String lineChanger = "\n";
        try {
            FileOutputStream out = new FileOutputStream(P2PConfiguration.getPeerAddressConfigurationFile());
            out.write(testIP1.getBytes());
            out.write(lineChanger.getBytes());
            out.write(testIP2.getBytes());
            out.write(lineChanger.getBytes());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
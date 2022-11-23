package com.scu.suhong.block;

import junit.framework.TestCase;
import util.AddressPort;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static com.scu.suhong.block.BlockchainState.resourcePeersFileName;

public class BlockchainStateTest extends TestCase {

    public void testIsWithStorageResource() {
        deleteStorageResourceFile();
        creatTestPeerAddressConfigurationFile();
        BlockchainState.resetResourcePeers();
        List<AddressPort> resourcePeers = BlockchainState.getResourcePeers();
        deleteStorageResourceFile();
        BlockchainState.resetResourcePeers();
        assert(resourcePeers.size() == 2);
    }

    private void deleteStorageResourceFile() {
        File f = new File(resourcePeersFileName);
        System.out.println("Storage resource file delete: " + f.delete());
    }

    private void creatTestPeerAddressConfigurationFile(){
        final String testIP1 = "1.1.1.1";
        final String testIP2 = "202.115.23.100";
        final String lineChanger = "\n";
        try {
            FileOutputStream out = new FileOutputStream(resourcePeersFileName);
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
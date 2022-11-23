package com.scu.suhong.block;

import util.AddressPort;
import util.StringHelper;

import java.util.List;

public class BlockchainState {
    static List<AddressPort> resourcePeers;
    static String resourcePeersFileName = "resourcePeers";

    public static synchronized List<AddressPort> getResourcePeers() {
        if (null == resourcePeers) {
            resourcePeers = loadResourcePeers();
        }
        return resourcePeers;
    }

    // Only for test
    public static void resetResourcePeers() {
        resourcePeers = null;
    }

    public static List<AddressPort> loadResourcePeers() {
        return StringHelper.loadIPAndPortFromFile(resourcePeersFileName);
    }


    public static boolean hasPeers() {
        if(null == resourcePeers){
            getResourcePeers();
        }
        return !resourcePeers.isEmpty();// Simply implementation, only just whether resource peer is set or not
    }
}

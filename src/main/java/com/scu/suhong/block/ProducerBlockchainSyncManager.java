package com.scu.suhong.block;

import com.scu.suhong.network.P2PConfiguration;
import util.StringHelper;

import java.util.ArrayList;
import java.util.List;

public class ProducerBlockchainSyncManager extends  BlockchainSyncManager {
    public ProducerBlockchainSyncManager(ExternalBlockchainManager blockChain) {
        super(blockChain);
        isExternal = true;
        this.identify = "external";
    }

    @Override
    public List<String> getPeerAddressListWithoutSelf() {
        return P2PConfiguration.getInstance().getProducerPeerAddressListWithoutSelf();
    }
}

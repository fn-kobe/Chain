package com.scu.suhong.block;

import com.scu.suhong.network.P2PConfiguration;

import java.net.InetAddress;

public class BlockchainSyncManagerSelector {
    BlockchainSyncManager blockchainSyncManager;
    BlockchainSyncManager externalBlockchainSyncManager;
    P2PConfiguration p2PConfiguration;

    public BlockchainSyncManagerSelector(BlockchainSyncManager blockchainSyncManager,
                                         BlockchainSyncManager externalBlockchainSyncManager,
                                         P2PConfiguration p2PConfiguration) {
        this.blockchainSyncManager = blockchainSyncManager;
        this.externalBlockchainSyncManager = externalBlockchainSyncManager;
        this.p2PConfiguration = p2PConfiguration;
    }

    // We only synchronize from producer blockchain as we will get its blockchain for us
    public BlockchainSyncManager getBlockchainSyncManager(InetAddress address, int port, byte[] msg){
        boolean isExternal = p2PConfiguration.isProducerPeer(address.getHostAddress(), port);
        if (isExternal) {
            System.out.println("[BlockchainSyncManagerSelector]  The address is from external peers ");
            return externalBlockchainSyncManager;
        }
        //Removed as we need to sync producer blockchain directly from it
        // Internal nodes may forward the external block
//        if (isForeighBlock(msg)){
//            System.out.println("[BlockchainSyncManagerSelector]  The block is from external and use external BlockchainSyncManager");
//            return externalBlockchainSyncManager;
//        }

        System.out.println("[BlockchainSyncManagerSelector]  The block is not from external address or does not contain external block and use internal BlockchainSyncManager");
        // default is internal sync manager
        return blockchainSyncManager;
    }

    private boolean isForeighBlock(byte[] msg) {
       if (BlockMessageState.EExternalBlock == BlockchainSyncManager.MsgBlockState(msg)) return true;

       return false;
    }
}

package com.scu.suhong.sync;

import Service.BlockchainService;
import com.scu.suhong.block.BlockchainFileDumper;
import consensus.pow.MiningConfiguration;
import util.ThreadHelper;

public class BlockchainSyncService implements Runnable {
    static BlockchainSyncService blockchainSyncService = null;
    boolean forceStop = false;

    private BlockchainSyncService() {
    }

    public static synchronized BlockchainSyncService getInstance() {
        if (null == blockchainSyncService) {
            blockchainSyncService = new BlockchainSyncService();
        }
        return blockchainSyncService;
    }

    @Override
    public void run() {
        int interval = MiningConfiguration.getDefaultBlockchainSyncInterval();
        // for test
        BlockchainFileDumper blockchainFileDumper = new BlockchainFileDumper();
        ThreadHelper.safeSleep(interval * 10);

        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(interval);

        do {
            ThreadHelper.safeSleep(interval);
            if (forceStop) {
                break;
            }
            BlockchainService.getInstance().getMiner().syncLatestBlockListFromPeers();
            blockchainFileDumper.dumpAll();// For test
        } while (true);
    }

    public void stopService() {
        forceStop = true;
    }
}

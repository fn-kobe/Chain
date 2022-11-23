package com.scu.suhong;

import Service.BlockchainService;
import Service.BlockchainServiceThread;
import asset.service.AssetServiceThread;
import com.scu.suhong.network.P2PPerformanceThread;
import com.scu.suhong.sync.BlockchainSyncService;
import com.scu.suhong.transaction.exchangeMode.EMServerTransactionService;
import command.CommandServiceThread;
import util.ThreadHelper;

/**
 * Hello world!
 */
public class App {

    public static void main(String[] args) {
        System.out.println("Version MBC_1.0");
        BlockchainService blockchain = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchain);
        Thread mainThread = new Thread(blockchainServiceThread, "Block chain main thread");
        mainThread.setPriority(Thread.MIN_PRIORITY);
        mainThread.start();

        CommandServiceThread commandServiceThread = new CommandServiceThread();
        Thread commandThread = new Thread(commandServiceThread, "User command parser");
        commandThread.start();

        CommandServiceThread fileCommandServiceThread = new CommandServiceThread();
        fileCommandServiceThread.setCommandInputFromFile(true);
        Thread fileCommandThread = new Thread(fileCommandServiceThread, "User command parser from files");
        fileCommandThread.start();

        AssetServiceThread assetServiceThread = AssetServiceThread.getInstance();
        Thread assetThread = new Thread(assetServiceThread, "MultiTypeAsset thread");
        assetThread.start();

        BlockchainSyncService blockchainSyncService = BlockchainSyncService.getInstance();
        Thread syncLatestBlockListThread = new Thread(blockchainSyncService, "To sync the latest block list");
        syncLatestBlockListThread.start();

        P2PPerformanceThread performanceService = P2PPerformanceThread.getInstance();
        Thread performanceThread = new Thread(performanceService, "Record P2P receive and send data");
        performanceThread.start();

        EMServerTransactionService emServerTransactionService = EMServerTransactionService.getInstance();
        Thread emServerTransactionThread = new Thread(emServerTransactionService, "process server transaction");
        emServerTransactionThread.start();

        try {
            // For paper test only
            //initTestAccountBanlance();
            ThreadHelper.safeSleep(1000);

            mainThread.join();
            commandThread.join();
            assetThread.join();
            syncLatestBlockListThread.join();
            emServerTransactionThread.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

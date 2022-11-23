package com.scu.suhong.block;

import account.AccountManager;
import org.jetbrains.annotations.NotNull;
import util.FileHelper;
import util.RandomHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

// The main purpose is for test. To dump the blockchain and see the synced content
public class BlockchainFileDumper {
    static private final String dumpFolder = "Blockchain_dump";
    private int dumpedTransactionNumber = 0;
    private int dumpedExternalTransactionNumber = 0;

    public BlockchainFileDumper() {
        FileHelper.createFolderIfNotExist(dumpFolder);
    }

    public void dumpAll(){
        // avoid dump two many
        if (BlockChain.getInstance().testAndSetNewTransactionAdded(false)){
            dumpInternal();
        }

        HashMap<String, ExternalBlockchain> externalBlockchainChainIDMap = ExternalBlockchainManager.getExternalBlockchainChainIDMap();
        for (String chainId : externalBlockchainChainIDMap.keySet()){
            ExternalBlockchain blockChain = externalBlockchainChainIDMap.get(chainId);
            if (blockChain.testAndSetNewTransactionAdded(false)) {
                dumpExternal(blockChain, chainId);
            }
        }
    }

    // Include internal blockchain and its balance
    static public void dumpInternal(){
        dumpInternal("");
    }

    static public void dumpInternal(String additionalMessage){
        String dump = BlockChain.getInstance().dump();
        dump += "\n" + AccountManager.getInstance().dump();
        String fileName = "internal_" + getCurrentDataStringWithRandom()+ "_" + additionalMessage + getDumpPostFix();
        FileHelper.createFile(dumpFolder + File.separator + fileName, dump, false);
    }

    static public void dumpExternal(String additionalMessage){
        String dump = BlockChain.getExternalManager().dump();
        dump += "\n" + AccountManager.getInstance().dump();
        String fileName = "external_" + getCurrentDataStringWithRandom()+ "_" + additionalMessage + getDumpPostFix();
        FileHelper.createFile(dumpFolder + File.separator + fileName, dump, false);
    }

    static public void dumpExternal(BlockChain blockChain, String chainId){
        String dump = blockChain.dump();
        String fileName = "external_" + chainId + "_" + getCurrentDataStringWithRandom() + getDumpPostFix();
        FileHelper.createFile(dumpFolder + File.separator + fileName, dump, false);
    }

    @NotNull
    static public String getCurrentDataString() {
        Date date =  new Date();
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd_hh.mm.ss");
        return format.format(date);
    }

    @NotNull
    static public String getCurrentDataStringWithRandom() {
        RandomHelper randomHelper = new RandomHelper(1000);
        return getCurrentDataString() + "_" + randomHelper.getNumber();
    }

    public static String getDumpPostFix(){
        return ".dmp";
    }
}

package com.scu.suhong.block;

import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.miner.Miner;

import java.util.HashMap;

public class ExternalBlockchainManager {
    static HashMap<String, ExternalBlockchain> exBlockchainIdMap = new HashMap<>();
    static private Miner miner;

    static synchronized public ExternalBlockchain getWorker(String chainId){
        if (null == chainId || chainId.isEmpty()){
            System.out.println("[ExternalBlockchain][ERROR] chain is not set or empty");
            return null;
        }
        System.out.println("[ExternalBlockchain][INFO] try to work on external chain with id " + chainId);
        if (!exBlockchainIdMap.containsKey(chainId)){
            ExternalBlockchain externalBlockchain = new ExternalBlockchain(chainId);
            externalBlockchain.setMiner(miner);
            externalBlockchain.init(BlockDBHandler.getExternalInstance(chainId).loadChainList());
            exBlockchainIdMap.put(chainId, externalBlockchain);
        }
        return exBlockchainIdMap.get(chainId);
    }

    public String dump() {
        String r = "";
        for (String chainId : exBlockchainIdMap.keySet()){
            r += getWorker(chainId).dump();
        }
        return r;
    }

    public String dump(int dumpAmount) {
        String r = "";
        for (String chainId : exBlockchainIdMap.keySet()){
            r += getWorker(chainId).dump(dumpAmount);
        }
        return r;
    }

    static HashMap<String, ExternalBlockchain> getExternalBlockchainChainIDMap(){
        return exBlockchainIdMap;
    }

    public void setMiner(Miner miner) {
        this.miner = miner;
    }

    static public void sendToMinerForProcess(AbstractTransaction transaction) {
        miner.addTransaction(transaction);
    }
}

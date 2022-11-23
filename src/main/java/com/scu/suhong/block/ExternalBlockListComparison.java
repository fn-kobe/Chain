package com.scu.suhong.block;

import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.dynamic_definition.TransactionHelper;
import com.scu.suhong.transaction.CrosschainInterface;

import java.util.List;

public class ExternalBlockListComparison {
    public void process(List<Block> oldBlockList, List<Block> newBlockList) {
        // ExternalBlockListComparison -> find the disappeared Tx and send it to be mined in internal blockchain
        BlockListComparison comparison = new BlockListComparison();
        // First select the rebanch disappeared tx by neighbor blockchain
        comparison.compare(oldBlockList, newBlockList);
        process(comparison);
    }

    public static void process(BlockListComparison comparison) {
        if (comparison.isEmpty()) {
            System.out.println("[ExternalBlockListComparison][WARN] No changes in the external blockchain");
            return;
        }

        // Package all new and disappeared Tx into internal blockchain
        List<AbstractTransaction> newTxList = comparison.getNewTxList();
        for (AbstractTransaction t: newTxList){
            // We don't want the self Tx to ba sent back
            if (t instanceof CrosschainInterface && (t.isExternalTransaction())){
                System.out.printf("[ExternalBlockListComparison][Info] Succed to send external transaction %d to miner %s\n",
                        t.getId(), t.getIndication());
                ExternalBlockchainManager.sendToMinerForProcess(t);
            } else {
                System.out.println("[ExternalBlockListComparison][Info] Skip to send new transaction to miner as it's not a cross-chain transaction " + t.getJson().toString());
            }
        }

        // Mark disappeared transaction
        List<AbstractTransaction> disappearedTxList = comparison.getDisappearedTxList();
        for (AbstractTransaction t: disappearedTxList){
            if (t instanceof CrosschainInterface && (t.isExternalTransaction())){
                TransactionHelper.markAsRebranchDisappeared(t);
                System.out.printf("[ExternalBlockListComparison][Info] Succed to send Disappeared external transaction %d to miner %s\n",
                        t.getId(), t.getIndication());
                ExternalBlockchainManager.sendToMinerForProcess(t);
            } else {
                System.out.println("[ExternalBlockListComparison][Info] Not a cross-chain Disappeared transaction " + t.getJson().toString());
            }
        }
    }
}

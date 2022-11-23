package com.scu.suhong.block;

import util.ArrayHelper;

import java.util.ArrayList;
import java.util.List;

public class ExternalBlockchain extends BlockChain {
    List<Block> oldBlockList = new ArrayList<>();
    ExternalBlockListComparison externalBlockListComparison = new ExternalBlockListComparison();

    ExternalBlockchain(String chainId) {
        identify = "external";
        this.chainId = chainId;
    }

    public void init(List<Block> blockList) {
        blockListBalanceProcessor = new ExternalBlockListBalanceProcessor();
        init(blockList, true);
    }

    @Override
    public void handleDisappearedTransaction(List<Block> newBlockList) {
        // Not to do in external as it will be handled in ExternalBlockListComparison
    }

    @Override
    public boolean addBlock(Block block) throws BlockException {
        // We skip check for genesis block whose index is 0
        if (0 != block.getBlockIndex() && !BlockChecker.isMatchBlockchain(this, block, true)) {
            logger.info("[Blockchain][external][WARN] Invalid block, skip to add to main chain");
            return false;
        }
        //Currently, we don't use divide. If want to do this, please open it.
        //ConditionalDivisionTransactionHelper.getInstance().tryAddNewBlock(block);
        blockList.add(block);

        trySealCrossChainTransactionIntoInternalBlockchain();
        logger.info("[Blockchain][external] Succeed to add one new block to external chain");
        return true;
    }


    public boolean saveBlockToDB(Block block) throws BlockException {
        // save to local DB
        if (BlockDBHandler.getInstance().saveTopBlock(block, true)) {
            logger.info(String.format("[Blockchain][" + identify + "][INFO] Succeed to save block: %d to DB", block.getBlockIndex()));
            return true;
        } else {
            logger.info("[Blockchain][saveNewBlock][" + identify + "][ERROR] Failed to save block to DB");
            return false;
        }
    }

    // ExternalBlockchain.addBlock(ExTxs) ->trySealCrossChainTransactionIntoInternalBlockchain
    // -> Miner.addTransaction->Blockchain(internal).addBlock(exTx_in_internal_blockchain)-> CrosschainTransactionHandler
    synchronized void trySealCrossChainTransactionIntoInternalBlockchain() {
        // we skip the external genesis block
        if (0 < blockList.size()) {
            externalBlockListComparison.process(oldBlockList, blockList);
        } else {
            logger.info("[Blockchain][external][WARN] blocklist size is 1");

        }
        oldBlockList = ArrayHelper.copy(blockList);
    }

    @Override
    // External blockchain only update list and then ssend tto internal blockchain for blance process
    public void handleBlockListChange() throws BlockException {
        trySealCrossChainTransactionIntoInternalBlockchain();
    }
}

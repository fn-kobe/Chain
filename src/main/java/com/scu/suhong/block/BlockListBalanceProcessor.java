package com.scu.suhong.block;

import account.AccountManager;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.*;
import com.scu.suhong.transaction.multipleTypeExchange.CrosschainExchangeCommonProcessor;
import com.scu.suhong.transaction.multipleTypeExchange.NotaryExchangeProcessor;
import hashlocking.HashLockingProcessor;
import util.FileLogger;

import java.util.List;

public class BlockListBalanceProcessor {
    static org.apache.log4j.Logger logger = FileLogger.getLogger();
    List<Block> blockList = null;

    // Mark all disappeared transaction
    public void process(List<Block> blockList) throws BlockException {
        BlockListComparison.markTransactionAsDisappearedWhenNeighborRebranch(blockList);
        this.blockList = blockList;
        handleInternalStatusFromNewBlockchain();
    }

    boolean handleInternalStatusFromNewBlockchain() throws BlockException {
        logger.info("[BlockListBalanceProcessor][INFO] Begin to process new status when blockchain major update");
        resetInternalStatus();
        return updateNewInternalStatus();
    }

    void resetInternalStatus() {
        // moved to updateNewInternalStatus for atom operation
    }

    boolean updateNewInternalStatus() throws BlockException {
        if (!BlockDBHandler.getInstance().saveTopBlock(blockList.get(blockList.size() - 1))) {
            logger.info("[BlockListBalanceProcessor][ERROR] Failed to save block to DB");
            return false;
        }

        logger.info("[BlockListBalanceProcessor][INFO] Succeed to save block to DB");

        AccountManager.getInstance().reset(blockList);
        ConditionalAssociationTransactionHelper.getInstance().reset(blockList);
        CrosschainTransactionHandler.getInstance().reset(blockList);
        CrosschainExchangeCommonProcessor.getInstance().reset(blockList);
        NotaryExchangeProcessor.getInstance().reset(blockList);
        HashLockingProcessor.getInstance().reset(blockList);

        return true;
    }

    // Don't use currently, as it may require more complicated algorithm even undo formed circle
    public void processComparisonBalance(BlockListComparison comparison) {
        if (null == comparison) {
            System.out.println("[BlockListBalanceProcessor][WARN] BlockListComparison is empty");
            return;
        }

        // handle balance
        List<AbstractTransaction> newTxList = comparison.getNewTxList();
        for (AbstractTransaction t: newTxList){
            AccountManager.getInstance().processTransactionBalance(t);
            if (t instanceof CrosschainInterface){
                CrosschainTransactionHandler.getInstance().tryAddTransaction((CrosschainTransaction) t);
            } else if (t instanceof ConditionalAssociationTransaction) {
                ConditionalAssociationTransactionHelper.getInstance().processCTx((ConditionalAssociationTransaction) t);
            }
        }

        List<AbstractTransaction> disappearedTxList = comparison.getDisappearedTxList();
        for (AbstractTransaction t: disappearedTxList){
            AccountManager.getInstance().callbackCTx(t);
            if (t instanceof CrosschainInterface){
                CrosschainTransactionHandler.getInstance().callbackCTx((CrosschainTransaction) t);
            } else if (t instanceof ConditionalAssociationTransaction) {
                ConditionalAssociationTransactionHelper.getInstance().callbackCTx((ConditionalAssociationTransaction) t);
            }
        }
    }
}

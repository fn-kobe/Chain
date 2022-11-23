package com.scu.suhong.block;

public class ExternalBlockListBalanceProcessor extends BlockListBalanceProcessor {
    @Override
        // Don't check reset internal balance as the cross-chain transaction will be pakcgaed to internal blockchain
        // All other will be not be reset, we balance is sent to internal blockchain
    boolean handleInternalStatusFromNewBlockchain() throws BlockException {
        return true;//
    }
}

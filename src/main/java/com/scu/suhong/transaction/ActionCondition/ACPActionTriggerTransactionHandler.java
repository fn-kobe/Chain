package com.scu.suhong.transaction.ActionCondition;

import com.scu.suhong.block.Block;
import com.scu.suhong.block.BlockChain;
import com.scu.suhong.dynamic_definition.AbstractTransaction;

public class ACPActionTriggerTransactionHandler {
    private static ACPActionTriggerTransactionHandler instance = new ACPActionTriggerTransactionHandler();
    ACPAction action = null;
    boolean isLoggedInitialInfo = false;// used to only output the log for ignoring when blockchain loading

    private ACPActionTriggerTransactionHandler() {
    }

    public void setAction(ACPAction action) {
        this.action = action;
    }

    public ACPAction getAction() {
        return action;
    }

    public static ACPActionTriggerTransactionHandler getInstance() {
        return instance;
    }

    public void tryAddNewBlock(Block block) {
        if (!isInProcessibleState()){
            return;
        }

        for (AbstractTransaction t : block.getTransactions()) {
            tryProcessTransaction(t);
        }
    }

    public void tryProcessTransaction(AbstractTransaction t){
        if (!isInProcessibleState()){
            return;
        }

        if (!(t instanceof ACPCommonTransaction)) return;

        if (t instanceof ACPSettingTransaction) {
            action = new ACPAction();
        }

        if (null == action){
            System.out.println("[ACPActionTriggerTransactionHandler][WARN] No action processor configured for the action condition pair treatment");
            return;
        } else {
            action.onIncomingConditionTransaction((ACPCommonTransaction) t);
        }
    }

    boolean isInProcessibleState(){
        // Currently design is not to hanldle when initialize the blockchain, as miner is null and crash
        // Change when try to handle this
        if (BlockChain.isInInitialState() ){
            if (!isLoggedInitialInfo) {
                System.out.println("[ACPActionTriggerTransactionHandler][Info] Currently we do not handle acp transaction when loading blockchain");
                isLoggedInitialInfo = true;
            }
            return false;
        }
        return true;
    }
}

package com.scu.suhong.transaction;

import account.AccountManager;
import com.scu.suhong.block.Block;
import com.scu.suhong.dynamic_definition.AbstractTransaction;

import java.util.ArrayList;
import java.util.List;

public class CrosschainTransactionHandlerThread implements Runnable {
    private static CrosschainTransactionHandlerThread instance = new CrosschainTransactionHandlerThread();

    List<CrosschainTransaction> crosschainTransactionList;

    public CrosschainTransactionHandlerThread() {
        crosschainTransactionList = new ArrayList<>();
    }

    public static CrosschainTransactionHandlerThread getInstance() {
        return instance;
    }

    @Override
    public void run() {

    }

    public boolean processExternalTransaction(CrosschainTransaction crosschainTransaction) {
        if (!tryFreezeAccount(crosschainTransaction)) return false;

        crosschainTransactionList.add(crosschainTransaction);
        doProcessExternalTransaction(crosschainTransaction);

        return true;
    }

    private boolean tryFreezeAccount(CrosschainTransaction crosschainTransaction) {
        if (AccountManager.isExternalAddress(crosschainTransaction.getFrom())) {
            System.out.printf("[CrosschainTransactionHandlerThread] No need to froze the balance of external account %s from Tx: %s\n"
                    ,crosschainTransaction.getFrom(), crosschainTransaction.Dump());
            return true;
        }

        AccountManager accountManager = AccountManager.getInstance();
        if (!accountManager.canTransferValue(crosschainTransaction.getFrom(), crosschainTransaction.getAssetType(), crosschainTransaction.getValue())) {
            return false;
        }
        accountManager.subValue(crosschainTransaction.getFrom(), crosschainTransaction.getAssetType(), crosschainTransaction.getValue());
        return true;
    }

    public void doProcessExternalTransaction(CrosschainTransaction crosschainTransaction) {
        if (crosschainTransaction.doesMatched(crosschainTransactionList)) {
            System.out.println("[CrosschainTransactionHandlerThread] External transaction matched the interaction condition\n" + crosschainTransaction.Dump());
            //remove all related transaction from list
            int removedAccount = 0;
            CrosschainTransaction requiredExTx = crosschainTransaction.getRequiredCrosschainTransaction();
            while (!CrosschainTransaction.isTransactionSameInExternalCase(requiredExTx, crosschainTransaction)) {
                crosschainTransactionList.remove(requiredExTx);
                processBalanceInSameBlockchain(requiredExTx);
                ++removedAccount;

                requiredExTx = requiredExTx.getRequiredCrosschainTransaction();
            }
            crosschainTransactionList.remove(crosschainTransaction);
            processBalanceInSameBlockchain(crosschainTransaction);
            ++removedAccount;

            System.out.println("[CrosschainTransactionHandlerThread] Removed " + removedAccount + " matched ExternalTransactionThreads");
        }
    }

    // Only process the balance in the same blockchain
    void processBalanceInSameBlockchain(CrosschainTransaction exTx) {
        if (!AccountManager.isExternalAddress(exTx.getTo())) {
            System.out.println("[CrosschainTransactionHandlerThread] Balance is now given to  " + exTx.getTo() + " with value: " + exTx.getValue());
            AccountManager.getInstance().addValue(exTx.getTo(), exTx.getValue());
        }
    }

    // Only for test
    public int testGetExternalTransactionListSize() {
        return crosschainTransactionList.size();
    }

    public void tryAddNewBlock(Block block) {
        for (AbstractTransaction transaction : block.getTransactions()) {
            if (!(transaction instanceof CrosschainTransaction)) continue;

            processExternalTransaction((CrosschainTransaction) transaction);
        }
    }

    public void tryAddTransaction(CrosschainTransaction transaction) {
        processExternalTransaction(transaction);
    }

    public void callbackCTx(CrosschainTransaction transaction) {
        AccountManager.getInstance().addValue(transaction.getFrom(), transaction.getValue());
        crosschainTransactionList.remove(transaction);
        System.out.println("[CrosschainTransactionHandlerThread][ERROR] ----------------------------------------------------------");
        System.out.println("[CrosschainTransactionHandlerThread][ERROR] callback for cross-chain happens, Please implement this");
        System.out.println("[CrosschainTransactionHandlerThread][ERROR] ----------------------------------------------------------");
        // TO DO
    }


    public void reset() {
        crosschainTransactionList = new ArrayList<>();
    }
}

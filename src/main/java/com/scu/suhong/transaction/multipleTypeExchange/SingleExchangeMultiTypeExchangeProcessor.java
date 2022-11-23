package com.scu.suhong.transaction.multipleTypeExchange;

import account.AccountManager;
import com.scu.suhong.transaction.Transaction;
import util.TimeHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SingleExchangeMultiTypeExchangeProcessor {
    List<RequiredData> requiredList = new ArrayList<>();
    int exchangeId = 0;
    String requiredTxListType = "";
    boolean doesComplete = false;
    Timer timer = new Timer(true);
    boolean isTimerStarted = false;
    int transactionMaxWaitTime = 20 * 60 * 1000;// 20  minutes
    int runInterval = 2 * 1000;// 2 seconds
    int maxRunCount = transactionMaxWaitTime / runInterval;
    int currentRunCount = 0;
    final String notaryKeyWord = "notary";

    // For performance count
    Long exchangeBeginTime = 0l;

    List<MultiTypeExchangeTransaction> appearedTransactionList = new ArrayList<>();

    public SingleExchangeMultiTypeExchangeProcessor(int exchangeId) {
        this.exchangeId = exchangeId;
    }

    public boolean process(MultiTypeExchangeTransaction transaction) {
        System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][process][%d] Begin to process transaction %d\n", exchangeId, transaction.getId());
        if (!checkProcessType(transaction)) {
            System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][process][%d] Cannot process as type changed in one exchange\n", exchangeId);
            return false;
        }

        if (transaction.getRequiredTxListType().equals("fixed")) {
            if (!processFixed(transaction)) return false;
        } else {
            if (!processVariable(transaction)) return false;
        }
        if ((!AccountManager.isExternalAddress(transaction.getFrom())) && (!tryFreezeAccount(transaction))) {
            System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][process][%d] Balance '%s' of internal sender is not enough\n", exchangeId, transaction.getFrom());
            return false;
        }

        checkStatus();
        return true;
    }

    synchronized void checkTimer() {
        if (isTimerStarted) {
            return;
        }
        exchangeBeginTime = TimeHelper.getEpoch();
        System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][process][%d] Timer starts at %s\n", exchangeId, TimeHelper.getCurrentTimeUsingCalendar());

        isTimerStarted = true;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                ++currentRunCount;
                if (currentRunCount >= maxRunCount) {// timed out
                    System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][process][WARN][%d] Exchange timed out at %s\n", exchangeId, TimeHelper.getCurrentTimeUsingCalendar());
                    // restore all balance
                    for (MultiTypeExchangeTransaction transaction : appearedTransactionList) {
                        restoreInternalSenderBalance(transaction);
                    }
                    resetStatus();
                } else {// not timed out check condition matched or not
                    checkStatus();
                }
            }
        }, runInterval, runInterval);
    }

    boolean processFixed(MultiTypeExchangeTransaction transaction) {
        List<RequiredData> newRequiredDataList = transaction.getCopiedRequiredDataList();
        if (requiredList.isEmpty()) {
            requiredList = transaction.getCopiedRequiredDataList();
        } else {
            for (RequiredData newData : newRequiredDataList) {
                if (!isInList(newData, requiredList)) {
                    System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][processFixed][ERROR] %s is not in required list\n", newData.getJson().toString());
                    System.out.println("[SingleExchangeMultiTypeExchangeProcessor][processFixed], it is required that each transaction to have the same required list");
                    return false; // already in list. skip to add
                }
            }

            for (RequiredData newData : requiredList) {
                if (!isInList(newData, newRequiredDataList)) {
                    System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][processFixed][ERROR] %s is not same as ones in new required list\n", newData.getJson().toString());
                    System.out.println("[SingleExchangeMultiTypeExchangeProcessor][processFixed], it is required that each transaction to have the same required list");
                    return false; // already in list. skip to add
                }
            }
        }

        //now add self to appeared list
        if (!isInList(transaction, appearedTransactionList)) {
            appearedTransactionList.add(transaction);
        } else {
            System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][processFixed][WARN] %s already exists in process\n", transaction.getId());
        }
        return true;
    }

    boolean processVariable(MultiTypeExchangeTransaction transaction) {
        System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][processVariable] try to process %s\n", transaction.getJson().toString());
        List<RequiredData> newRequiredDataList = transaction.getCopiedRequiredDataList();
        for (RequiredData newData : newRequiredDataList) {
            if (isInList(newData, requiredList)) {
                System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][processVariable] %s is already in required list\n", newData.getJson().toString());
                continue; // already in list. skip to add
            } else {
                System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][processVariable] %s is is added in in required list\n", newData.getJson().toString());
                requiredList.add(newData);
            }
        }

        //now add self to appeared list
        if (!isInList(transaction, appearedTransactionList)) {
            appearedTransactionList.add(transaction);
        } else {
            System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][processVariable][WARN] %s already exists in process\n", transaction.getId());
        }
        return true;
    }

    private boolean checkProcessType(MultiTypeExchangeTransaction transaction) {
        if (requiredTxListType.isEmpty()) requiredTxListType = transaction.getRequiredTxListType();
        if (!requiredTxListType.equals(transaction.getRequiredTxListType())) {
            System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][process][%d] Type not matched %s to %s in tx\n",
                    exchangeId, requiredTxListType, transaction.getRequiredTxListType());

            return false;
        }
        return true;
    }

    synchronized void checkStatus() {
        checkTimer();

        for (RequiredData requiredData : requiredList) {
            if (!doesItAppear(requiredData, appearedTransactionList)) {
                System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][process][%d] requiredData '%s' does not appear\n",
                        exchangeId, requiredData.getJson().toString());
                return;
            }
        }

        System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][checkStatus][%d] Required transactions match\n", exchangeId);
        boolean matched = true;
        // External condition check
        for (MultiTypeExchangeTransaction transaction : appearedTransactionList) {
            if (!transaction.isExternalConditionDone()) {// ongoing, return
                System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][checkStatus][%d] transaction %d does not match condition\n", exchangeId, transaction.getId());
                return;
            }
            if (!transaction.doesExternalConditionMatch()) {// Complete but false
                matched = false;
            }
        }

        // complete whether matched or refused by one condition. Timeout will process in timer callback
        if (matched) {
            System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][process][%d] **** Exchange matches. Begin to perform further action at %s\n", exchangeId, TimeHelper.getCurrentTimeUsingCalendar());
            System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][process][%d] **** Exchange matches. Total time %d\n", exchangeId, TimeHelper.getEpoch() - exchangeBeginTime);

            for (MultiTypeExchangeTransaction transaction : appearedTransactionList) {
                sendToInternalReceiverBalance(transaction);
            }
        } else { // Not matched
            System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][process][%d] **** Exchange NOT matches. Begin to restore the balance of sender at %s\n", exchangeId, TimeHelper.getCurrentTimeUsingCalendar());
            for (MultiTypeExchangeTransaction transaction : appearedTransactionList) {
                restoreInternalSenderBalance(transaction);
            }
        }

        resetStatus();
    }

    void restoreInternalSenderBalance(MultiTypeExchangeTransaction t){
        if (AccountManager.isInternalChain(t.getChainID())){
            AccountManager.getInstance().addValue(t.getFrom(), t.getAssetType(), t.getValue());
        }
    }

    void sendToInternalReceiverBalance(MultiTypeExchangeTransaction t){
        if (AccountManager.isInternalChain(t.getChainID())){
            AccountManager.getInstance().addValue(t.getTo(), t.getAssetType(), t.getValue());
        }
    }

    private void resetStatus() {
        System.out.printf("[SingleExchangeMultiTypeExchangeProcessor][process][%d] Reset processor\n", exchangeId);

        timer.cancel();
        exchangeId = -1;
        currentRunCount = 0;
        requiredTxListType = "";
        appearedTransactionList = new ArrayList<>();
        requiredList = new ArrayList<>();
        doesComplete = true;
    }

    private boolean tryFreezeAccount(Transaction transaction) {
        AccountManager accountManager = AccountManager.getInstance();
        if (!accountManager.canTransferValue(transaction.getFrom(), transaction.getAssetType(), transaction.getValue())) {
            return false;
        }
        accountManager.subValue(transaction.getFrom(), transaction.getAssetType(), transaction.getValue());
        return true;
    }

    boolean isInList(RequiredData newData, List<RequiredData> requiredList) {
        for (RequiredData requiredData : requiredList) {
            if (newData.doesMatch(requiredData)) {
                return true;
            }
        }
        return false;
    }

    boolean isInList(MultiTypeExchangeTransaction newTransaction, List<MultiTypeExchangeTransaction> requiredList) {
        for (MultiTypeExchangeTransaction requiredTransaction : requiredList) {
            if (requiredTransaction.doesMatch(newTransaction)) {
                return true;
            }
        }
        return false;
    }

    boolean doesItAppear(RequiredData requiredData, List<MultiTypeExchangeTransaction> appearedList) {
        for (MultiTypeExchangeTransaction appearedTx : appearedList) {
            if (appearedTx.doesMatchRequired(requiredData)) {
                return true;
            }
        }
        return false;
    }

    public int getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(int exchangeId) {
        this.exchangeId = exchangeId;
    }

    public void setTransactionMaxWaitTime(int transactionMaxWaitTime) {
        this.transactionMaxWaitTime = transactionMaxWaitTime;
        maxRunCount = transactionMaxWaitTime / runInterval;
    }

    public boolean complete() {
        return doesComplete;
    }
}

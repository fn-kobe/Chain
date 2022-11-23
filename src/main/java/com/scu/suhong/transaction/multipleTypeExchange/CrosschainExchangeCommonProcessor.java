package com.scu.suhong.transaction.multipleTypeExchange;

import account.AccountManager;
import com.scu.suhong.block.Block;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.CommonCrosschainTransaction;
import com.scu.suhong.transaction.Transaction;
import util.TimeHelper;

import java.util.*;

public class CrosschainExchangeCommonProcessor {
    static CrosschainExchangeCommonProcessor instance;
    CrosschainExchangeCommonProcessorThread processorRunnable;

    Map<Integer, List<Transaction>> exchangeAllTxListMap;
    Map<Integer, List<Integer>> exchangeProcessedTxIdListMap;
    Map<Integer, Integer> exchangeStartTimeMap;

    // Transaction data of this kind: CRTX_<...>;<requiredData, chainID:from:to:assetType:value:data>;<providedData>
    String crosschainCommonProcessorKeyword = "CRTX_";
    int timeoutValue = 5 * 60;// 5 minutes

    private CrosschainExchangeCommonProcessor() {
        exchangeAllTxListMap = new HashMap<>();
        exchangeProcessedTxIdListMap = new HashMap<>();
        exchangeStartTimeMap = new HashMap<>();
        processorRunnable = new CrosschainExchangeCommonProcessorThread();
    }

    static public CrosschainExchangeCommonProcessor getInstance() {
        if (null == instance){
            instance = new CrosschainExchangeCommonProcessor();
            instance.startThread();
        }

        return instance;
    }

    void startThread(){
        Thread thread = new Thread(processorRunnable);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    List<Transaction> getExchangeAllTransactionList(int exchangeId) {
        List<Transaction> r = exchangeAllTxListMap.get(exchangeId);
        if (null == r){
            r = new ArrayList<>();
            if (!exchangeStartTimeMap.containsKey(exchangeId)){
                System.out.printf("[CrosschainExchangeCommonProcessor][DEBUG] Begin exchange at %s\n",
                        TimeHelper.getCurrentTimeUsingCalendar());
                exchangeStartTimeMap.put(exchangeId, Math.toIntExact(TimeHelper.getEpochSeconds()));
            }
        }
        exchangeAllTxListMap.put(exchangeId, r);
        return r;
    }

    List<Transaction> getExchangeUnprocessedTransactionList(int exchangeId) {
        List<Transaction> r = new ArrayList<>();
        List<Transaction> allTransactionList = getExchangeAllTransactionList(exchangeId);
        for (Transaction t : allTransactionList){
            if (!isProcessed(exchangeId, t.getId())) r.add(t);
        }
        return r;
    }

    List<Integer> getExchangeProcessedTransactionIdList(int exchangeId) {
        List<Integer> r = exchangeProcessedTxIdListMap.get(exchangeId);
        if (null == r) r = new ArrayList<>();
        exchangeProcessedTxIdListMap.put(exchangeId, r);
        return r;
    }

    boolean isInExchange(int exchangeId, int txId) {
        List<Transaction> allTransactionList = getExchangeAllTransactionList(exchangeId);
        for (Transaction t : allTransactionList) {
            if (t.getId() == txId) return true;
        }
        return false;
    }

    boolean isProcessed(int exchangeId, int txId) {
        List<Integer> processedTxList = getExchangeProcessedTransactionIdList(exchangeId);
        return processedTxList.contains(txId);
    }

    public void tryAddNewBlock(Block block) {
        for (AbstractTransaction transaction : block.getTransactions()) {
            if (!(transaction instanceof CommonCrosschainTransaction)) continue;

            processNewTx((CommonCrosschainTransaction) transaction);
        }
    }

    public boolean processNewTx(CommonCrosschainTransaction newTx) {
        String d = newTx.getData();
        int exchangeId = newTx.getInteractionId();
        System.out.printf("[CrosschainExchangeCommonProcessor][INFO] Begin to process exchange %d of transaction %d\n",
                exchangeId, newTx.getId());
        if (!d.startsWith(crosschainCommonProcessorKeyword)) {
            return false;
        }

        System.out.println("[CrosschainExchangeCommonProcessor][DEBUG] Get common cross chain transaction " + newTx.getId());
        List<Transaction> exchangeAllTransactionList = getExchangeAllTransactionList(exchangeId);//start time is recored
        exchangeAllTransactionList.add(newTx);
        if (AccountManager.isInternalChain(newTx.getBlockchainId())) {
            System.out.printf("[CrosschainExchangeCommonProcessor][INFO] Delay receiver %s to receives the balance %d\n", newTx.getTo(), newTx.getValue());
            AccountManager.getInstance().subValue(newTx.getFrom(), newTx.getValue());
        }

        List<Transaction> derivedTransactionList = new ArrayList<>();
        List<Transaction> hasNotMatched = new ArrayList<>();
        handleExchange(exchangeId, newTx, derivedTransactionList, hasNotMatched);
        if (hasNotMatched.isEmpty()){// all matched
            handleBalanceAfterCompletion(exchangeId, derivedTransactionList);
        }
        return true;
    }

    // derivedTransactionList is list of transactions current tx and its required tx and required tx's required tx, and so on
    // if not matched, put an element into it, as this pass the value back
    void handleExchange(int exchangeId, Transaction currentTx, List<Transaction> derivedTransactionList, List<Transaction> hasNotMatched){
        System.out.println("[CrosschainExchangeCommonProcessor][DEBUG] Begin to iterate " + currentTx.getId());
        if (!hasNotMatched.isEmpty()){
            System.out.println("[CrosschainExchangeCommonProcessor][DEBUG] Skip to iterate as some required derivation does not match" );
            return;
        }

        List<Transaction> newlyFoundTxList = findAllExpectedTx(exchangeId, currentTx);
        if (null == newlyFoundTxList || newlyFoundTxList.isEmpty()){
            System.out.printf("[CrosschainExchangeCommonProcessor][DEBUG] Transaction %d in exchange %d does not complete\n",
                    currentTx.getId(), exchangeId);
            hasNotMatched.add(currentTx);
            return;// not find
        }

        if (newlyFoundTxList.size() < getRequiredData(exchangeId, currentTx).size()) {
            System.out.printf("[CrosschainExchangeCommonProcessor][DEBUG] Appeared Tx number %d is less than required Tx %d\n",
                    newlyFoundTxList.size(), getRequiredData(exchangeId, currentTx).size());
            hasNotMatched.add(currentTx);
            return;// found while not all appears
        }

        // Not a success exchange, continue to found more
        System.out.printf("Tx list found before (without newly found): ");
        for (Transaction t : derivedTransactionList) System.out.printf(" " + t.getId());
        System.out.println("");
        for (Transaction t : newlyFoundTxList) {
            if (!hasNotMatched.isEmpty()) return;// In the cirle of the newTx, some Tx does not match, just break;
            if (derivedTransactionList.contains(t)) continue;// skip to iterate forever
            derivedTransactionList.add(t);
            handleExchange(exchangeId, t, derivedTransactionList, hasNotMatched);
        }
    }

    private void handleBalanceAfterCompletion(int exchangeId, List<Transaction> derivedTransactionList) {
        System.out.printf("[CrosschainExchangeCommonProcessor][DEBUG] *** Exchange %d complete at %s. Completion time is %d seconds\n",
                exchangeId, TimeHelper.getCurrentTimeUsingCalendar(), TimeHelper.getEpochSeconds() - exchangeStartTimeMap.get(exchangeId));
        System.out.printf("[CrosschainExchangeCommonProcessor][DEBUG] Transactions are: ");
        for (Transaction lt : derivedTransactionList) {
            List<Integer> processedTxList = exchangeProcessedTxIdListMap.get(exchangeId);
            processedTxList.add(lt.getId());
            System.out.printf(" %d", lt.getId());

            if (!lt.isExternalTransaction()) {
                AccountManager.getInstance().addValue(lt.getTo(), lt.getValue());
            }
        }
        System.out.println("");
    }

    // if found return the found transaction, else return null
    List<Transaction> findAllExpectedTx(int exchangeId, Transaction currentTx){
        List<Transaction> r = new ArrayList<>();
        List<Transaction> transactionList = getExchangeAllTransactionList(exchangeId);
        List<RequiredData> requiredDataList = getRequiredData(exchangeId, currentTx);
        for (RequiredData requiredData : requiredDataList) {
            System.out.printf("Tx %d's required data %s\n", currentTx.getId(), requiredData.getJson().toString());
            for (Transaction t : transactionList) {
                System.out.printf("Checking tx %d data %s \n", t.getId(), t.Dump());
                if (isProcessed(exchangeId, t.getId())){
                    System.out.printf("[CrosschainExchangeCommonProcessor][DEBUG] Transactions %d has been processed\n", t.getId());
                    continue;// skip processed transactions
                }
                if (RequiredData.doesMatch(requiredData, getAppearedData(exchangeId, t))) {
                    System.out.printf("[CrosschainExchangeCommonProcessor][DEBUG] Transactions %d matches\n", t.getId());
                    r.add(t);
                    break;
                }
            }
        }
        return r;
    }

    List<RequiredData> getRequiredData(int exchangeId, Transaction t){
        String transactionData = t.getData();
        String[] requiredDataArray = transactionData.split(";;");
        List<RequiredData> r = new ArrayList<>();
        for (int i = 0; i < requiredDataArray.length; ++i){
            RequiredData requiredData = getOneRequiredData(exchangeId, requiredDataArray[i]);
            if (null != requiredData){
                r.add(requiredData);
            }
        }
        return r;
    }

    RequiredData getOneRequiredData(int exchangeId, String oneRequiredData){
        String[] dataList = oneRequiredData.split(";");
        if (dataList.length < 2) return null;
        String requiredata = dataList[1];// requireddata, 0 is CRTX_...

        String[] requireList = requiredata.split(":");
        if (requireList.length < 5) return null;
        String chainID = requireList[0];
        String from = AccountManager.getShortAddress(requireList[1]);
        String to = AccountManager.getShortAddress(requireList[2]);
        String assetType = requireList[3];
        int value = Integer.parseInt(requireList[4]);
        String data = "";
        if (requireList.length > 5) data = requireList[5];

        return new RequiredData(exchangeId, chainID, from, to, assetType, value, data);
    }


    // Currently, we only put appeared data in the first section as one for all
    // This one per each required, please change it
    RequiredData getAppearedData(int exchangeId, Transaction t){
        String transactionData = t.getData();
        String[] dataArray = transactionData.split(";");
        String appearedData = "";
        if (dataArray.length >= 3) appearedData = dataArray[2];//appeared data
        return new RequiredData(exchangeId, t.getBlockchainId(), t.getFrom(), t.getTo(), t.getAssetType(), t.getValue(), appearedData);
    }

    public void reset(List<Block> blockList) {
        exchangeAllTxListMap = new HashMap<>();
        exchangeProcessedTxIdListMap = new HashMap<>();
        for (Block block : blockList) {
            tryAddNewBlock(block);
        }
    }

    // True means still need to check
    public boolean check() {
        if (exchangeAllTxListMap.isEmpty()) return false;

        Set<Integer> keySet = exchangeAllTxListMap.keySet();
        for (Integer exchangeId : keySet){
            List allTx = getExchangeAllTransactionList(exchangeId);
            List processeTxid = getExchangeProcessedTransactionIdList(exchangeId);
            if (!isAllProcessed(allTx, processeTxid)) {
                System.out.printf("[CrosschainExchangeCommonProcessor][DEBUG] begin to check unfinished exchange %d\n", exchangeId);
                return checkTimeout(exchangeId);
            }

        }
        // No need to check further, as all processed
        return false;
    }

    // True still exchange is not timeout
    boolean checkTimeout(int exchangeId){
        if (!exchangeStartTimeMap.containsKey(exchangeId)) return true;

        int startTime = exchangeStartTimeMap.get(exchangeId);
        int usedTime = Math.toIntExact(TimeHelper.getEpochSeconds() - startTime);
        if (usedTime > timeoutValue){
            processTimeout(exchangeId);
            return false;
        }

        System.out.printf("[CrosschainExchangeCommonProcessor][DEBUG] Exchange %d is not timeout. Used %d, timeout value %d\n", exchangeId, usedTime, timeoutValue);

        return true;
    }

    void processTimeout(int exchangeId){
        List<Transaction> transactionList = getExchangeUnprocessedTransactionList(exchangeId);
        for (Transaction t: transactionList){
            System.out.printf("[CrosschainExchangeCommonProcessor][DEBUG] Transaction %d has timed out in exchange %d at %s\n"
                    , t.getId(), exchangeId, TimeHelper.getCurrentTimeUsingCalendar());
            getExchangeProcessedTransactionIdList(exchangeId).add(t.getId());
            if (!t.isExternalTransaction()){
                System.out.printf("[CrosschainExchangeCommonProcessor][DEBUG] The sender %s of transaction %d has been restored %d at %s\n",
                        t.getFrom(), t.getId(), t.getValue(), TimeHelper.getCurrentTimeUsingCalendar());
                AccountManager.getInstance().addValue(t.getFrom(), t.getValue());
            }
        }
    }

    boolean isAllProcessed(List<Transaction> transactionList, List<Integer> processedTxIdList){
        for (Transaction t: transactionList){
            boolean found = false;
            for (Integer i : processedTxIdList){
                if (t.getId() == i){
                    found = true;
                    System.out.printf("[CrosschainExchangeCommonProcessor][DEBUG] Transaction %d has processed in exchange \n", t.getId());
                }
            }
            if (!found){
                System.out.printf("[CrosschainExchangeCommonProcessor][DEBUG] Transaction %d has not been processed in exchange \n", t.getId());
                return false;
            }
        }

        return true;
    }

    public void testSetTimeoutValue(int timeoutValue) {
        this.timeoutValue = timeoutValue;
    }
}

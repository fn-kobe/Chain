package com.scu.suhong.transaction.multipleTypeExchange;

import account.AccountManager;
import com.scu.suhong.block.Block;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.CommonCrosschainTransaction;
import com.scu.suhong.transaction.Transaction;
import util.TimeHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotaryExchangeProcessor{
    static NotaryExchangeProcessor instance;

    Map<Integer, List<Transaction>> hashedFirstCommitTransactionList;
    Map<Integer, List<Transaction>> hashedSecondCommitTransactionList;

    String firstCommitTransactionKeyword = "notary_first_";
    String secondCommitTransactionKeyword = "notary_second_";

    static public NotaryExchangeProcessor getInstance(){
        if (null == instance) instance = new NotaryExchangeProcessor();

        return instance;
    }

    private NotaryExchangeProcessor() {
        hashedFirstCommitTransactionList = new HashMap<>();
        hashedSecondCommitTransactionList = new HashMap<>();
    }

    List<Transaction> getFirstCommitTransactionList(int exchangeId){
        return getTransactionList(exchangeId, hashedFirstCommitTransactionList);
    }

    List<Transaction> getSecondCommitTransactionList(int exchangeId){
        return getTransactionList(exchangeId, hashedSecondCommitTransactionList);
    }

    List<Transaction> getTransactionList(int exchangeId, Map<Integer, List<Transaction>> listMap){
        if (!listMap.containsKey(exchangeId)){
            listMap.put(exchangeId, new ArrayList<>());
        }
        return listMap.get(exchangeId);
    }

    public void tryAddNewBlock(Block block) {
        for (AbstractTransaction transaction : block.getTransactions()) {
            if (!(transaction instanceof Transaction)) continue;

            process((Transaction) transaction);
        }
    }

    public boolean process(Transaction t){
        if (!(t instanceof CommonCrosschainTransaction)){
            System.out.printf("[NotaryExchangeProcessor][INFO] Transaction %d is not CommonCrosschainTransaction\n", t.getId());
            return false;
        }
        String d = t.getData();
        int exchangeId = ((CommonCrosschainTransaction) t).getInteractionId();
        System.out.printf("[NotaryExchangeProcessor][INFO] Begin to process exchange %d of transaction %d\n",
                exchangeId, t.getId());
        List<Transaction> firstTransactionList = getFirstCommitTransactionList(exchangeId);
        List<Transaction> secondTransactionList = getSecondCommitTransactionList(exchangeId);
        if (d.startsWith(firstCommitTransactionKeyword)){
            System.out.println("[NotaryExchangeProcessor][DEBUG] Get first phase transaction " + t.getId());
            firstTransactionList.add(t);
            if (AccountManager.isInternalChain(t.getBlockchainId())) {
                System.out.printf("[NotaryExchangeProcessor][INFO] Delay receiver %s to receives the balance %d\n", t.getTo(), t.getValue());
                AccountManager.getInstance().subValue(t.getFrom(), t.getValue());
            }
        } else if (d.startsWith(secondCommitTransactionKeyword)) {
            System.out.println("[NotaryExchangeProcessor][DEBUG] Get second phase transaction " + t.getId());
            secondTransactionList.add(t);
        } else {
            return false;
        }

        // Now judge the size. Notice due to the blockchain mining issues ,first transaction may come later than the second tx
        if (firstTransactionList.size() == secondTransactionList.size()){
            System.out.printf("[NotaryExchangeProcessor][INFO] *** Two phase commit of exchange %d is done at epoc %d\n",
                    exchangeId, TimeHelper.getEpoch());
            processBalance(exchangeId);
        } else {
            System.out.printf("[NotaryExchangeProcessor][INFO] Current first phase tx number is %d, and second phase tx number is %d in exchange %d\n",
                    firstTransactionList.size(), secondTransactionList.size(), exchangeId);
        }
        return true;
    }

    void processBalance(int exchangeId){
        System.out.println("[NotaryExchangeProcessor][INFO] Begin to handle balance when done");
        List<Transaction> firstCommitTransactionList = getFirstCommitTransactionList(exchangeId);
        for (Transaction t : firstCommitTransactionList){
            if (AccountManager.isInternalChain(t.getBlockchainId())) {
                System.out.printf("[NotaryExchangeProcessor][INFO] receiver %s receives the balance %d\n", t.getTo(), t.getValue());
                AccountManager.getInstance().addValue(t.getTo(), t.getValue());// here is the delayed transfer
            }
        }
        System.out.println("[NotaryExchangeProcessor][INFO] End to handle balance when done");
    }

    public void reset(List<Block> blockList){
        hashedFirstCommitTransactionList = new HashMap<>();
        hashedSecondCommitTransactionList = new HashMap<>();
        for (Block block : blockList) {
            tryAddNewBlock(block);
        }
    }
}

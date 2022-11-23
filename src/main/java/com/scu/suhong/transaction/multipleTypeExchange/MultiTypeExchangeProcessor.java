package com.scu.suhong.transaction.multipleTypeExchange;

import com.scu.suhong.block.Block;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.Transaction;

import java.util.HashMap;

public class MultiTypeExchangeProcessor {
    HashMap<Integer, SingleExchangeMultiTypeExchangeProcessor> multiTypeExchangeProcessorHashMap = new HashMap<>();

    static MultiTypeExchangeProcessor instance = new MultiTypeExchangeProcessor();
    private MultiTypeExchangeProcessor() {
    }

    public static MultiTypeExchangeProcessor getInstance() {
        return instance;
    }

    public void process(MultiTypeExchangeTransaction transaction) {
        int exchangeId = transaction.getExchangeId();
        SingleExchangeMultiTypeExchangeProcessor processor = getSingleTypeProcessor(exchangeId);
        System.out.printf("[MultiTypeExchangeProcessor][process] try to process with exchange id %d transaction id %d \n", exchangeId, transaction.getId());
        processor.process(transaction);
    }

    private synchronized SingleExchangeMultiTypeExchangeProcessor getSingleTypeProcessor(int exchangeId) {
        SingleExchangeMultiTypeExchangeProcessor processor = multiTypeExchangeProcessorHashMap.get(exchangeId);
        if (null == processor) {
            multiTypeExchangeProcessorHashMap.put(exchangeId, processor = new SingleExchangeMultiTypeExchangeProcessor(exchangeId));
        }
        return processor;
    }

    public SingleExchangeMultiTypeExchangeProcessor getProcessor(int exchnageId) {
        return getSingleTypeProcessor(exchnageId);
    }

    public void tryAddNewBlock(Block block) {
        for (AbstractTransaction transaction : block.getTransactions()) {
            if (!(transaction instanceof MultiTypeExchangeTransaction)) continue;

            else process((MultiTypeExchangeTransaction) transaction);
        }
    }
}

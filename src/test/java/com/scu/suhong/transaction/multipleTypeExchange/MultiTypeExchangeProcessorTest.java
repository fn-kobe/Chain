package com.scu.suhong.transaction.multipleTypeExchange;

import consensus.pow.MiningConfiguration;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.*;

public class MultiTypeExchangeProcessorTest {

    int exchangeId= 10;

    @Test
    public void process() {
        MultiTypeExchangeProcessor processor = MultiTypeExchangeProcessor.getInstance();

        MultiTypeExchangeTransaction transaction = new MultiTypeExchangeTransaction(exchangeId);
        transaction.setId();
        transaction.setRequiredTxListType("variable");
        processor.process(transaction);

        SingleExchangeMultiTypeExchangeProcessor singleProcessor = processor.getProcessor(transaction.getExchangeId());
        assert singleProcessor.requiredList.isEmpty();
        transaction.addRequiredData(constructRequiredData("1", exchangeId));
        processor.process(transaction);
        assert 1 == singleProcessor.requiredList.size();
        assert 1 == singleProcessor.appearedTransactionList.size();

        processor.process(transaction);
        assert 1 == singleProcessor.requiredList.size();
        assert 1 == singleProcessor.appearedTransactionList.size();

        transaction.addRequiredData(constructRequiredData("2", exchangeId));
        processor.process(transaction);
        assert 2 == singleProcessor.requiredList.size();
        assert 1 == singleProcessor.appearedTransactionList.size();

    }


    @NotNull
    RequiredData constructRequiredData(String postFix, int exchangeId){
        String requiredFrom = "from" + "_" + postFix;
        String requiredTo = "to" + "_" + postFix;
        String requiredAssetType = "assetType" + "_" + postFix;
        int requiredValue = 10000;

        return new RequiredData(exchangeId, MiningConfiguration.getBlockchainStringId(), requiredFrom, requiredTo, requiredAssetType, requiredValue);
    }
}
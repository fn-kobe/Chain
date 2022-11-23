package com.scu.suhong.transaction.multipleTypeExchange;

import account.AccountManager;
import com.scu.suhong.transaction.CommonCrosschainTransaction;
import com.scu.suhong.transaction.Transaction;
import hashlocking.HashLockingProcessor;
import org.junit.Test;
import util.RSAUtility;
import util.RandomHelper;

import static org.junit.Assert.*;

public class NotaryExchangeProcessorTest {

    @Test
    public void process() {
        NotaryExchangeProcessor processor = NotaryExchangeProcessor.getInstance();
        String from = "from";
        String to = "to";
        int value = 2;
        String  firstCommitTransactionList = processor.firstCommitTransactionKeyword;
        String  secondCommitTransactionKeyword = processor.secondCommitTransactionKeyword;
        AccountManager.getInstance().addValue(from, value);

        RandomHelper randomHelper = new RandomHelper(1, 10000);
        int interactionId = randomHelper.getNumber();
        System.out.printf("[Test] begin to test exchange with id \n", interactionId);
        Transaction t = new CommonCrosschainTransaction(interactionId);
        t.setFrom(from);
        t.setToAndValue(to, value);
        //t.setData(HashLockingProcessor.getHashLockingFlagInTransactionDataField() + keyWord);
        t.setData(firstCommitTransactionList);
        t.setId();
        processor.process(t);
        assert 0 == AccountManager.getInstance().getBalance(to);

        t.setData(secondCommitTransactionKeyword);
        processor.process(t);
        assert value == AccountManager.getInstance().getBalance(to);

    }
}
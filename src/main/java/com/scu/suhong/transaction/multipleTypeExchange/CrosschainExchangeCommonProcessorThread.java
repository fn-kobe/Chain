package com.scu.suhong.transaction.multipleTypeExchange;

import util.ThreadHelper;

public class CrosschainExchangeCommonProcessorThread implements Runnable {
    boolean shouldStop = false;
    final static int sleepInterval  = 5;
    @Override
    public void run() {
        while (!shouldStop){
            CrosschainExchangeCommonProcessor processor = CrosschainExchangeCommonProcessor.getInstance();
            if (!processor.check()) ;//NOP;//break;// do not break, optimize it later
            ThreadHelper.safeSleepSecond(5);
        }
    }

    public static int getSleepInterval() {
        return sleepInterval;
    }
}

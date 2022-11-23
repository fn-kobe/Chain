package com.scu.suhong.network;

import util.ThreadHelper;

public class P2PPerformanceThread implements Runnable {
    static P2PPerformanceThread instance = null;

   boolean outputP2PPerformance = false;
    int oldRxCount = 0;
    int newRxCount = 0;
    int oldTxCount = 0;
    int newTxCount = 0;
    int oldExternalRxCount = 0;
    int newExternalRxCount = 0;
    int oldExternalTxCount = 0;
    int newExternalTxCount = 0;
    int runInterval =1;//one seconds

    private P2PPerformanceThread() {
    }

    static public synchronized P2PPerformanceThread getInstance(){
        if (null == instance){
            instance = new P2PPerformanceThread();
        }
        return instance;
    }

    @Override
   public void run() {
     while (true) {
       if (outputP2PPerformance) {
         handlePerformance();
       } else{
         System.out.println("[P2PPerformanceThread][Performance] No need to output P2P network flow performance");
         break;
       }
     }
   }


    private void handlePerformance() {
        ThreadHelper.safeSleepSecond(runInterval);
        int currentRxCount = newRxCount;
        int currentTxCount = newTxCount;
        int currentExternalRxCount = newExternalRxCount;
        int currentExternalTxCount = newExternalTxCount;

        int rxDelta = currentRxCount - oldRxCount;
        int txDelta = currentTxCount - oldTxCount;
        int exRxDelta = currentExternalRxCount - oldExternalRxCount;
        int exTxDelta = currentExternalTxCount - oldExternalTxCount;

        System.out.printf("[P2PPerformanceThread][Performance] P2P total flow byte %d, send %d, receive %d\n",
                rxDelta + txDelta, txDelta, rxDelta);
        System.out.printf("[P2PPerformanceThread][Performance] P2P external flow byte (send+receive\tsend\treceive) \t%d\t%d\t%d\n",
                exRxDelta + exTxDelta, exTxDelta, exRxDelta);
        System.out.printf("[P2PPerformanceThread][Performance] P2P external accumulated flow byte (send+receive\tsend\treceive) \t%d\t%d\t%d\n",
                currentExternalRxCount + currentExternalTxCount, currentExternalTxCount, currentExternalRxCount);

        oldRxCount = currentRxCount;
        oldTxCount = currentTxCount;
        oldExternalRxCount = currentExternalRxCount;
        oldExternalTxCount = currentExternalTxCount;
    }

    public void addNewReceiveCount(int newCount) {
        this.newRxCount += newCount;
    }

    public void addNewSendCount(int newCount) {
        this.newTxCount += newCount;
    }

    public void addNewExternalReceiveCount(int newExternalCount) {
        this.newExternalRxCount += newExternalCount;
    }

    public void addNewExternalSendCount(int newExternalCount) {
        this.newExternalTxCount += newExternalCount;
    }
}

package com.scu.suhong.transaction.ActionCondition;

import util.ThreadHelper;

public class ACPActionThread implements Runnable {
    ACPAbstractAction action = null;
    long runInterval = 1*1000;//default one seconds
    boolean isRunning = false;

    public ACPActionThread(ACPAbstractAction action) {
        this.action = action;
    }

    @Override
    public void run() {
        isRunning = true;
        while (true) {
            System.out.println("[ACPActionThread][Info] Try to check the action processor");
            action.checkAndDoAction();
            if (action.isDone()){
                System.out.println("[ACPActionThread][Info] Exits action processing thread, due to the action has been done");
                break;
            }
            ThreadHelper.safeSleep(runInterval);
        }
        isRunning = false;
    }

    public long getRunInterval() {
        return runInterval;
    }

    public void setRunInterval(long runInterval) {
        this.runInterval = runInterval;
    }

    public boolean isRunning() {
        return isRunning;
    }
}

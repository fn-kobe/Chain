package com.scu.suhong.instantiationOptimization.inner;

public class SleepAndCallTwoExternalMethodThread implements Runnable {
    String previousSleepTime;
    String methodInternalSleepTime;
    String lastSleepTime;
    String isDelay;
    String isDisposable;

    public SleepAndCallTwoExternalMethodThread(String previousSleepTime, String methodInternalSleepTime, String lastSleepTime, String isDelay, String isDisposable) {
        this.previousSleepTime = previousSleepTime;
        this.methodInternalSleepTime = methodInternalSleepTime;
        this.lastSleepTime = lastSleepTime;
        this.isDelay = isDelay;
        this.isDisposable = isDisposable;
    }

    @Override
    public void run() {
        SleepAndCallTwoExternalMethod sct = new SleepAndCallTwoExternalMethod();
        sct.function(previousSleepTime, methodInternalSleepTime, lastSleepTime, isDelay, isDisposable);
    }
}

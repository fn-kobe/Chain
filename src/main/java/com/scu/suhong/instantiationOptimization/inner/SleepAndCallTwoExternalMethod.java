// class in "inner" package are within the process of blockchain
package com.scu.suhong.instantiationOptimization.inner;

import com.scu.suhong.instantiationOptimization.SmartContractHelper;
import util.ThreadHelper;
import util.TimeHelper;

import java.util.ArrayList;
import java.util.List;

public class SleepAndCallTwoExternalMethod {

    public void function(String previousSleepTime, String methodInternalSleepTime, String lastSleepTime, String isDelay, String isDisposable){
        long startTime = TimeHelper.getEpochSeconds();
        System.out.printf("[SleepAndCallTwoExternalMethod][INFO] begin smart contract at %s\n", startTime);
        int sleepTime = Integer.parseInt(previousSleepTime);
        System.out.printf("[SleepAndCallTwoExternalMethod][INFO] begin to sleep %d seconds\n", sleepTime);
        ThreadHelper.safeSleepSecond(sleepTime);

        String smartContractName = "NotCallingOther";
        if (isDelay.equalsIgnoreCase("delay")){
            System.out.printf("[SleepAndCallTwoExternalMethod][INFO] Delay to instantiate smart contract instance\n");
            smartContractName = "NotCallingOtherDelay";
        }

        String varietyName = "var1";
        SmartContractHelper.processNewInstance(smartContractName, varietyName);

        List<String> methodParameterList = new ArrayList<>();
        methodParameterList.add(varietyName);
        methodParameterList.add("setLoopTimes");
        methodParameterList.add("3600");
        SmartContractHelper.processMethod(methodParameterList);

        sleepTime = Integer.parseInt(methodInternalSleepTime);
        System.out.printf("[SleepAndCallTwoExternalMethod][INFO] begin to sleep %d seconds among method interval\n", sleepTime);
        ThreadHelper.safeSleepSecond(sleepTime);

        methodParameterList = new ArrayList<>();
        methodParameterList.add(varietyName);
        methodParameterList.add("writeStateToBlockchain");
        methodParameterList.add(getFlagInBc());
        SmartContractHelper.processMethod(methodParameterList);

        boolean terminationSent = false;
        if (isDisposable.equalsIgnoreCase("disposable")){
            System.out.printf("[SleepAndCallTwoExternalMethod][INFO] Disposable method, termination when out of scope\n");
            terminateVariety(varietyName);
            terminationSent = true;
        }

        sleepTime = Integer.parseInt(lastSleepTime);
        System.out.printf("[SleepAndCallTwoExternalMethod][INFO] begin to sleep %d seconds at last\n", sleepTime);
        ThreadHelper.safeSleepSecond(sleepTime);

        if (!terminationSent) {
            System.out.printf("[SleepAndCallTwoExternalMethod][INFO] Normal termination when program finish\n");
            terminateVariety(varietyName);
        }

        long endTime = TimeHelper.getEpochSeconds();
        System.out.printf("[SleepAndCallTwoExternalMethod][INFO] *** smart contract end at %s, runs %s seconds. IsDelay %s isDisposable %s\n",
                endTime, endTime-startTime, isDelay, isDisposable);
    }

    private void terminateVariety(String varietyName) {
        List<String> methodParameterList = new ArrayList<>();
        methodParameterList.add(varietyName);
        methodParameterList.add("termination");
        methodParameterList.add("termination");
        SmartContractHelper.processMethod(methodParameterList);
        SmartContractHelper.waitSmartContractToFinish(varietyName);
    }

    static String getFlagInBc(){
        return "stateBC";
    }
}

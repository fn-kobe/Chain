// classes in "embed": their code are embedded in blockchain jar, while run in a separate process by system.exec
package com.scu.suhong.instantiationOptimization.embed;

import com.scu.suhong.instantiationOptimization.RequestProcessHandler;
import com.scu.suhong.instantiationOptimization.SmartContractVarietyWrapper;
import com.scu.suhong.instantiationOptimization.RequestProviderInterface;
import util.ThreadHelper;

public class CallingOneDeeper implements RequestProviderInterface {
    private String identifier;
    String smartContractPath;

    public CallingOneDeeper(String identifier, String smartContractPath) {
        this.identifier = identifier;
        this.smartContractPath = smartContractPath;
    }

    void callAnother() {
        System.out.printf("[CallingOneDeeper][Info] Call another\n");

        String smartContractPath = getSmartContractPath();
        String smartContractName = "NotCallingOther";
        String varietyName = "nco1";

        SmartContractVarietyWrapper v1 = SmartContractVarietyWrapper.createSmartContractVariety(smartContractPath, smartContractName, getIdentify(), varietyName);
        String method = "setLoopTimes";
        String parameterList = "2";
        v1.sendRequestToVarietyMappedSCProcess(method, parameterList);
        ThreadHelper.safeSleepSecond(2);

        varietyName = "nco2";
        SmartContractVarietyWrapper v2 = SmartContractVarietyWrapper.createSmartContractVariety(smartContractPath, smartContractName, getIdentify(), varietyName);
        method = "setLoopTimes";
        parameterList = "3";
        v2.sendRequestToVarietyMappedSCProcess(method, parameterList);

        RequestProcessHandler requestProcessHandler = new RequestProcessHandler(this);
        // In a separate thread and do not affect current thread
        requestProcessHandler.processRequestInSeparateThread();
        for (int i = 0; i < 5; ++i){
            System.out.printf("[CallingOneDeeper][Info] Sleep %d time(s)\n", i);
            ThreadHelper.safeSleepSecond(3);
        }
    }

    public static void main(String args[]) {
        doAction(args);
    }

    // For test or it will be put in the method of main
    static void doAction(String[] args) {
        String identifier = args[0];
        System.out.println("[NotCallingOther][Info] Begin smart contract CallingOneDeeper with identifier " + identifier);

        String smartContractPath = args[1];
        System.out.println("[CallingOneDeeper][Info] Smart contract path is " + smartContractPath);

        // We don't care external name, and then variety name is not used. Smart contract name is ours and also ignored
        CallingOneDeeper callingOneDeeper = new CallingOneDeeper(identifier, smartContractPath);
        callingOneDeeper.callAnother();

        System.out.println("[NotCallingOther][Info] End smart contract CallingOneDeeper with identifier " + identifier);
    }

    @Override
    public String getIdentify() {
        return identifier;
    }

    public void onlyLogMethod(){
        System.out.println("[CallingOneDeeper][Info] onlyLogMethod");
    }

    public void onlyLogMethod(String log){
        System.out.println("[CallingOneDeeper][Info] onlyLogMethod " + log);
    }

    public String getSmartContractPath() {
        return smartContractPath;
    }
}

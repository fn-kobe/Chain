package com.scu.suhong.instantiationOptimization;

import util.FileHelper;
import util.ThreadHelper;
import util.TimeHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class InvocationOtherSmartContractThread implements Runnable{
    SmartContractVarietyWrapper variety;
    String identifier;

    public InvocationOtherSmartContractThread(SmartContractVarietyWrapper variety) {
        this.variety = variety;
        identifier = variety.getIdentify();
    }

    // processIdentifier is used as working folder name and identifier for interprocess communication
    @Override
    public void run() {
        // 1. create folder
        SmartContractHelper.createVarietyWorkingFolder(identifier);

        // 2. create instance
        long startTime = TimeHelper.getEpochSeconds();
        String smartContractCommand = variety.getCommand() + " " + identifier + " " + variety.getSmartContractPath();
        System.out.printf("[InvocationOtherSmartContractThread][%s][%d][INFO] Thread begins at %s, command is %s\n",
                identifier, getThreadId(), TimeHelper.getCurrentTimeUsingCalendar(), smartContractCommand);
        Process process = ThreadHelper.runExternalCommand(smartContractCommand);
        if (null == process){
            System.out.printf("[InvocationOtherSmartContractThread][%d][ERROR] Failed to create instance %s of %s in %s\n",
                    getThreadId(), variety.getSelfVarietyName(), variety.getSelfSCName(), variety.getSmartContractPath());
            return;
        }

        getOutPut(process);
        System.out.printf("[InvocationOtherSmartContractThread][%s][%d][INFO] Thread ends at %s\n",
                identifier, getThreadId(), TimeHelper.getCurrentTimeUsingCalendar());
        long endTime = TimeHelper.getEpochSeconds();
        System.out.printf("[InvocationOtherSmartContractThread][%s][%d][INFO] *** Thread takes %s seconds\n",
                identifier, getThreadId(), endTime - startTime);
    }

    long getThreadId(){
        return Thread.currentThread().getId();
    }

    void getOutPut(Process process){
        BufferedReader inputReaderBuffer = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader errorBuffer = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String line;
        try {
            while ((line = inputReaderBuffer.readLine()) != null) {
                System.out.printf("[InvocationOtherSmartContractThread][%s][%d][INFO] '%s'\n", identifier, getThreadId(), line);
            }
            inputReaderBuffer.close();
            while ((line = errorBuffer.readLine()) != null) {
                System.out.printf("[InvocationOtherSmartContractThread][%s][%d][ERROR] '%s'\n", identifier, getThreadId(), line);
            }
            errorBuffer.close();
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("[InvocationOtherSmartContractThread][%s][%d][Info] Command is done\n", identifier, getThreadId());

    }
}

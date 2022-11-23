package com.scu.suhong.instantiationOptimization;

import com.scu.suhong.block.Block;
import com.scu.suhong.block.BlockChain;
import org.jetbrains.annotations.NotNull;
import util.FileHelper;
import util.ThreadHelper;
import util.TimeHelper;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestProcessingUtility {
    int processRequestNumber = 0;
    String identifier;
    RequestProviderInterface requestProvider;
    private int runInterval = 1;
    private static final String blockListDumpFolder = "Blockchain_dump";

    public RequestProcessingUtility(RequestProviderInterface requestProvider) {
        this.requestProvider = requestProvider;
        identifier = requestProvider.getIdentify();
    }

    //                                                                 File communication (Request_)
    // Invocation path: sm1->[ standing variety of sm2 on sm1 side ]------------------------->   sm2 (Dispatch request to method)
    // This is sm2 part common process function
    // parameterList is separated by ';'
    public synchronized RequestState processRequest() {
        int currentRequestId = processRequestNumber + 1;
        System.out.printf("[RequestProcessingUtility][Debug] Begin to process request No %d\n", currentRequestId);
        SmartContractHelper.createVarietyWorkingFolder(identifier);
        String path = SmartContractHelper.getSmartContractWorkingFolder(identifier);
        String fileName = path + File.separator + getRequestFilePrefix() + currentRequestId;
        String content = FileHelper.loadContentFromFile(fileName).replaceAll("\n", "");
        if (null == content || content.isEmpty()) {
            return RequestState.EOK;// no request
        }

        String[] methodParameter = content.split(SmartContractHelper.getFieldSeparator());
        String method = methodParameter[0];
        String parameterList = "";
        if (methodParameter.length > 1) {
            parameterList = methodParameter[1];
        }

        if (null != method && !method.isEmpty()) {
            if (method.equals(SmartContractHelper.getTerminationRequest())) {
                System.out.println("[RequestProcessingUtility][Info] Termination request found at " + TimeHelper.getEpochSeconds());
                return RequestState.ETerminated;
            }

            if (dispatchRequestToMethod(method, parameterList)) {
                ++processRequestNumber;
                return RequestState.EOK;
            }
        }

        return RequestState.EFailed;// request file is not correct
    }


    @NotNull
    public String getRequestFilePrefix() {
        return SmartContractHelper.getRequestFilePrefix();
    }

    boolean dispatchRequestToMethod(String request, String parameterList) {
        String[] parameterArray = null;
        if (!parameterList.isEmpty()) {
            parameterArray = parameterList.split(SmartContractHelper.getParameterSeparator());
        }
        return SmartContractHelper.runMethod(requestProvider, request, parameterArray);
    }

    public void waitStateToBeConfirmed(List<String> stateList) {
        if (stateList.isEmpty()) {
            System.out.println("[RequestProcessingUtility][INFO] No state to be checked in blockchain");
            return;
        }

        while (true) {
            if (!doesBlockContainsAllStates(stateList)) {
                ThreadHelper.safeSleepSecond(runInterval);
            } else {
                System.out.println("[RequestProcessingUtility][INFO] All states has been put to blockchain");
                break;
            }
        }
    }

    boolean doesBlockContainsAllStates(List<String> stateList) {
        Map<String, String> foundStateList = new HashMap<>();
        checkInBlockDumpFolder(stateList, foundStateList);

        boolean doesAllStatesFound = true;
        for (String s : stateList) {
            if (!foundStateList.containsKey(s)) {
                doesAllStatesFound = false;
                System.out.printf("[RequestProcessingUtility][INFO] State %s has NOT been found in block\n", s);
                break;
            }
        }

        if (doesAllStatesFound) {
            System.out.printf("[RequestProcessingUtility][INFO] All states has been found in block\n");
        }
        return doesAllStatesFound;
    }

    private void checkInBlockDumpFolder(List<String> stateList, Map<String, String> foundStateList) {
        for (String s : stateList) {
            if (!FileHelper.doesFolderContain(blockListDumpFolder, s)) continue;

            System.out.printf("[RequestProcessingUtility][INFO] State %s has been found\n", s);
            foundStateList.put(s, s);
        }
    }
}

package com.scu.suhong.instantiationOptimization;

import org.junit.Test;
import util.FileHelper;

import java.io.File;

public class SmartContractVarietyWrapperTest {

    @Test
    public void processRequest() {
        SmartContractVarietyWrapper variety = new SmartContractVarietyWrapper(SmartContractHelper.getDefaultSmartContractPath(),
                "dummySmartContractName", "variety", "root", false);
        String requestFileName = variety.getSmartContractWorkingFolder() + File.separator + SmartContractHelper.getRequestFilePrefix() + 1;
        FileHelper.deleteFile(requestFileName);
        RequestProcessingUtility requestProcessingUtility = new RequestProcessingUtility(variety);
        assert RequestState.EOK == requestProcessingUtility.processRequest();// no request

        // Only test "getParentNameChain" CommonSCVariety
        String request = "getParentNameChain" + SmartContractHelper.fieldSeparator + "";
        FileHelper.createFile(requestFileName, request);
        assert RequestState.EOK == requestProcessingUtility.processRequest();

        requestFileName = variety.getSmartContractWorkingFolder() + File.separator + SmartContractHelper.getRequestFilePrefix() + 2;
        FileHelper.deleteFile(requestFileName);
        request = SmartContractHelper.terminationRequest + SmartContractHelper.fieldSeparator + "";
        FileHelper.createFile(requestFileName, request);
        assert RequestState.ETerminated == requestProcessingUtility.processRequest();
    }


    @Test
    public void sendAndProcessRequest() {
        String smartContractName = "dummySmartContractName";
        SmartContractInstanceConfig config = new SmartContractInstanceConfig();
        config.isDelayedInstance = true;// manually set it to no thread
        SmartContractVarietyWrapper variety = new SmartContractVarietyWrapper(SmartContractHelper.getDefaultSmartContractPath()
                , smartContractName, "testVar1", "root", false);
        assert variety.sendRequestToVarietyMappedSCProcess("getParentNameChain", "");
        RequestProcessingUtility requestProcessingUtility = new RequestProcessingUtility(variety);
        assert RequestState.EOK == requestProcessingUtility.processRequest();

        assert variety.sendRequestToVarietyMappedSCProcess(SmartContractHelper.terminationRequest, "");
        assert RequestState.ETerminated == requestProcessingUtility.processRequest();
    }
}
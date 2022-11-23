package com.scu.suhong.instantiationOptimization.embed;

import com.scu.suhong.instantiationOptimization.SmartContractHelper;
import org.junit.Test;
import util.FileHelper;

import static org.junit.Assert.*;

public class NotCallingOtherTest {

    @Test
    public void testLoopMaxTimes() {
        String identifier = "1.2.3";
        String testLoopTimes = "3";//only loop 3 times as we want it is done quickly and there is no state to be confirmed

        String smartContractPath = SmartContractHelper.getDefaultSmartContractPath();
        String[] parameterList = {identifier, smartContractPath, testLoopTimes};

        NotCallingOtherDelay.doAction(parameterList);
    }

    @Test
    public void testRequestDone() {
        String identifier = "1.2.3";

        String smartContractPath = SmartContractHelper.getDefaultSmartContractPath();
        String[] parameterList = {identifier, smartContractPath};

        int requestId = 1;
        String requestFileName = SmartContractHelper.getRequestFileFullPath(identifier, requestId) ;
        String content = "testTwoParameters:firstParameter;secondParameter";
        FileHelper.createFile(requestFileName, content);

        ++requestId;
       SmartContractHelper.sendTerminationRequest(identifier, requestId);
        NotCallingOtherDelay.doAction(parameterList);
    }
}
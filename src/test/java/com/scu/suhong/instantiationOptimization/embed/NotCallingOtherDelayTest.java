package com.scu.suhong.instantiationOptimization.embed;

import com.scu.suhong.instantiationOptimization.SmartContractHelper;
import org.junit.Test;

import static org.junit.Assert.*;

public class NotCallingOtherDelayTest {

    @Test
    public void testDelay() {
        String identifier = "1.2.3";
        String testLoopTimes = "3";//only loop 3 times as we want it is done quickly and there is no state to be confirmed

        String smartContractPath = SmartContractHelper.getDefaultSmartContractPath();
        String[] parameterList = {identifier, smartContractPath, testLoopTimes};

        NotCallingOtherDelay.doAction(parameterList);
    }
}
package com.scu.suhong.instantiationOptimization;

import com.scu.suhong.instantiationOptimization.embed.NotCallingOther;
import org.junit.Test;

public class SmartContractHelperTest {

    @Test
    public void runMethod() {
        NotCallingOther notCallingOther = new NotCallingOther("SmartContractHelperTest");
        String[] parameters = {"5"};
        assert SmartContractHelper.runMethod(notCallingOther, "setLoopTimes", parameters);
        assert SmartContractHelper.runMethod(notCallingOther, "function", null);
    }
}
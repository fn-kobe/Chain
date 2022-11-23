package com.scu.suhong.instantiationOptimization.inner;

import org.junit.Test;
import util.FileHelper;

import static org.junit.Assert.*;

public class SleepAndCallTwoExternalMethodTest {

    @Test
    public void function() {
        FileHelper.createFile("testDump", SleepAndCallTwoExternalMethod.getFlagInBc());

        SleepAndCallTwoExternalMethod sct = new SleepAndCallTwoExternalMethod();
        sct.function("3", "2", "3", "NotDelay", "NoDisposable");

    }
}
package com.scu.suhong.instantiationOptimization.embed;

import com.scu.suhong.instantiationOptimization.RootInstantiationManager;
import com.scu.suhong.instantiationOptimization.SmartContractHelper;
import com.scu.suhong.instantiationOptimization.SmartContractInstanceConfig;
import com.scu.suhong.instantiationOptimization.SmartContractVarietyWrapper;
import org.junit.Test;
import util.FileHelper;
import util.ThreadHelper;

import static org.junit.Assert.*;

public class CallingOneDeeperTest {

    @Test
    public void callAnother() {
        String identifier = "1.2.3";

        String smartContractPath = SmartContractHelper.getDefaultSmartContractPath();
        String[] parameterList = {identifier, smartContractPath};

        CallingOneDeeper.doAction(parameterList);
    }



    @Test
    public void testSendAndProcessRequest() {
        String smartContractName = "CallingOneDeeper";
        prepare(smartContractName, "");

        String varietyName = "callingOneDeeperVar1";
        RootInstantiationManager root = RootInstantiationManager.getInstance();
        SmartContractVarietyWrapper callingOneDeeperVar1 = root.createSmartContractVariety(smartContractName, varietyName);
        String method = "onlyLogMethod";
        callingOneDeeperVar1.sendRequestToVarietyMappedSCProcess(method,"");

        ThreadHelper.safeSleepSecond(10);
    }

    @Test
    public void testSendAndProcessRequestWithDifferentJarName() {
        String smartContractName = "CallingOneDeeper";
        prepare(smartContractName, "blockchain.jar");

        String varietyName = "callingOneDeeperVar1";
        RootInstantiationManager root = RootInstantiationManager.getInstance();
        SmartContractVarietyWrapper callingOneDeeperVar1 = root.createSmartContractVariety(smartContractName, varietyName);
        String method = "onlyLogMethod";
        callingOneDeeperVar1.sendRequestToVarietyMappedSCProcess(method,"");

        ThreadHelper.safeSleepSecond(10);
    }

    // Smart contract is inside the jar file
    void prepare(String smartContractName, String jarName){
        if (null == jarName || jarName.isEmpty()) jarName = smartContractName;

        // smart contract file
        //In out test, all smart contract is inside blockchain.jar
        String smartContractJarFullPath = SmartContractInstanceConfig.getSmartContractFullPath(jarName);
        FileHelper.copyFileByForce("classes\\artifacts\\blockchain_jar\\blockchain.jar", smartContractJarFullPath);

        // configuration file
        String configFileFullPath = SmartContractInstanceConfig.getConfigurationFullPath(smartContractName);
        //prepare configuration file
        FileHelper.deleteFile(configFileFullPath);

        String content =
                "[instantiation]\n" +
                "isDelayedInstance = no\n" +
                "instanceMustMethods = function\n" +
                "[command]\n" +
                "Launcher=java -cp\n" +
                "runCommand = " + jarName + "\n" +
                "ClosePost = com.scu.suhong.instantiationOptimization.embed." + smartContractName;
        FileHelper.createFile(configFileFullPath, content);
    }
}
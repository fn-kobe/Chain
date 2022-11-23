package com.scu.suhong.instantiationOptimization;

import org.junit.Test;
import util.FileHelper;
import util.ThreadHelper;

public class RootInstantiationManagerTest {

    @Test
    public void createNotCallingOtherVariety() {
        RootInstantiationManager instance = RootInstantiationManager.getInstance();
        String smartContractName = "NotCallingOther";
        prepareNotCallingOther(smartContractName);

        String varietyName = "varNCO1";
        SmartContractVarietyWrapper varNCO1 = instance.createSmartContractVariety(smartContractName, varietyName);
        varNCO1.sendRequestToVarietyMappedSCProcess("setLoopTimes", "5");

        ThreadHelper.safeSleepSecond(10);
    }

    void prepareNotCallingOther(String smartContractName){
        String scName = smartContractName;

        // smart contract file
        //In out test, all smart contract is inside blockchain.jar
        String smartContractFile = SmartContractInstanceConfig.getSmartContractFullPath(scName);
        FileHelper.copyFileByForce("classes\\artifacts\\blockchain_jar\\blockchain.jar", smartContractFile);

        // configuration file
        String configFileName = SmartContractInstanceConfig.getConfigurationFullPath(scName);
        //prepare configuration file
        FileHelper.deleteFile(configFileName);

        String content =
                "[instantiation]\n" +
                "isDelayedInstance = no\n" +
                "instanceMustMethods = function\n" +
                "[command]\n" +
                "Launcher=java -cp\n" +
                "runCommand = " + scName + "\n" +
                "ClosePost = com.scu.suhong.instantiationOptimization.embed.NotCallingOther";
        FileHelper.createFile(configFileName, content);
    }

    @Test
    public void createCallingOneDeeperVariety() {
        RootInstantiationManager instance = RootInstantiationManager.getInstance();
        String smartContractName = "CallingOneDeeper";
        prepareCallingOneDeeper(smartContractName);

        String varietyName = "varCOD1";
        SmartContractVarietyWrapper varCOD1 = instance.createSmartContractVariety(smartContractName, varietyName);
        varCOD1.sendRequestToVarietyMappedSCProcess("setLoopTimes", "5");

        ThreadHelper.safeSleepSecond(10);
    }

    void prepareCallingOneDeeper(String smartContractName){
        String scName = smartContractName;

        // smart contract file
        //In out test, all smart contract is inside blockchain.jar
        String smartContractFile = SmartContractInstanceConfig.getSmartContractFullPath(scName);
        FileHelper.copyFileByForce("classes\\artifacts\\blockchain_jar\\blockchain.jar", smartContractFile);

        // configuration file
        String configFileName = SmartContractInstanceConfig.getConfigurationFullPath(scName);
        //prepare configuration file
        FileHelper.deleteFile(configFileName);

        String content =
                "[instantiation]\n" +
                "isDelayedInstance = no\n" +
                "instanceMustMethods = function\n" +
                "[command]\n" +
                "Launcher=java -cp\n" +
                "runCommand = " + scName + "\n" +
                "ClosePost = com.scu.suhong.instantiationOptimization.embed." + smartContractName;
        FileHelper.createFile(configFileName, content);
    }
}
package com.scu.suhong.instantiationOptimization;

import org.junit.Test;
import util.FileHelper;

public class SmartContractInstanceConfigTest {

    @Test
    public void testConfig() {
        String scPath = SmartContractHelper.getDefaultSmartContractPath();
        System.out.println("[Test] Default smart contract path is " + scPath);
        String scName = "test";

        String varietyName = "var1";
        String configFileName = SmartContractInstanceConfig.getConfigurationFullPath(scName);
        //prepare configuration file
        FileHelper.deleteFile(configFileName);

        String content = "[instantiation]\n" +
                "isDelayedInstance = yes\n" +
                "instanceMustMethods = f1;f2\n" +
                "[command]\n" +
                "Launcher=java -jar\n" +
                "runCommand = test\n" +
                "ClosePost = com.scu.suhong.instantiationOptimization.embed.NotCallingOther";
        FileHelper.createFile(configFileName, content);

        SmartContractInstanceConfig config = new SmartContractInstanceConfig(scPath, scName, varietyName,"root",  null);
        assert config.isDelayedInstance;
        assert 2 == config.instanceMustMethods.size();
        assert config.isInstanceMust("f1");
        SmartContractVarietyWrapper variety = SmartContractVarietyWrapper.createSmartContractVariety(config, false);
        String command = "java -jar (.*)test com.scu.suhong.instantiationOptimization.embed.NotCallingOther";
        assert variety.getCommand().matches(command);

        scName = "ConfigDoesNotExist";
        config = new SmartContractInstanceConfig(scPath, scName, varietyName,"root", null);
        assert !config.isDelayedInstance;
    }

    @Test
    public void testConfigMissingItem() {
        String scPath = SmartContractHelper.getDefaultSmartContractPath();
        System.out.println("[Test] Default smart contract path is " + scPath);
        String scName = "test";

        String varietyName = "var1";
        String configFileName = SmartContractInstanceConfig.getConfigurationFullPath(scName);
        //prepare configuration file
        FileHelper.deleteFile(configFileName);

        String content = "[instantiation]\n" +
                "isDelayedInstance = yes\n" +
                "instanceMustMethods = \n" +
                "[command]\n" +
                "Launcher=java -jar\n" +
                "runCommand = test\n" +
                "ClosePost = com.scu.suhong.instantiationOptimization.embed.NotCallingOther";
        FileHelper.createFile(configFileName, content);

        SmartContractInstanceConfig config = new SmartContractInstanceConfig(scPath, scName, varietyName,"root",  null);
        assert config.isDelayedInstance;
        assert 0 == config.instanceMustMethods.size();
        assert !config.isInstanceMust("any");
        SmartContractVarietyWrapper variety = SmartContractVarietyWrapper.createSmartContractVariety(config, false);
        String command = "java -jar (.*)test com.scu.suhong.instantiationOptimization.embed.NotCallingOther";
        assert variety.getCommand().matches(command);
    }

}
package com.scu.suhong.instantiationOptimization;

import org.apache.logging.log4j.core.util.FileUtils;
import util.IniFileHelper;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static util.FileHelper.doesFileOrFolderExist;

public class SmartContractInstanceConfig {
    Thread thread;
    boolean isDelayedInstance = false;
    List<String> instanceMustMethods = new ArrayList<>();
    static final String methodSeparator = ";";

    String smartContractPath;
    String smartContractName;
    String varietyName;
    String runCommand;
    String smartContractLauncher;
    String smartContractClosePost;
    private String parentNameChain;

    public SmartContractInstanceConfig(
            String smartContractPath, String smartContractName, String varietyName, String parentNameChain, Thread thread) {
        this.thread = thread;
        this.smartContractPath = smartContractPath;
        this.smartContractName = smartContractName;
        this.varietyName = varietyName;
        this.parentNameChain = parentNameChain;

        init();
    }

    //Only for test
    public SmartContractInstanceConfig(){}

    void init(){
        String configName = getConfigurationFullPath();
        if (!doesFileOrFolderExist(configName)){
            System.out.printf("[SmartContractInstanceConfig][INFO] No config file for smart contract %s\n", smartContractName);
            return;
        }
        System.out.printf("[SmartContractInstanceConfig][INFO] Begin to load smart contract config file %s\n", configName);
        String section = "instantiation";

        // isDelayedInstance
        String key = "isDelayedInstance";
        String value = getInitValueReplacingNullToEmpty(configName, section, key);
        if (value.equalsIgnoreCase("yes")){
            isDelayedInstance = true;
        } else {
            isDelayedInstance = false;
        }

        // instanceMustMethods
        key = "instanceMustMethods";
        value = getInitValueReplacingNullToEmpty(configName, section, key);
        if (!value.isEmpty()){
            String [] instanceMustMethodList = value.split(methodSeparator);
            instanceMustMethods = Arrays.asList(instanceMustMethodList);
        }

        section = "command";
        // smartContractLauncher
        key = "Launcher";
        smartContractLauncher = getInitValueReplacingNullToEmpty(configName, section, key);

        // runCommand
        key = "runCommand";
        runCommand = getInitValueReplacingNullToEmpty(configName, section, key);

        // runCommand
        key = "ClosePost";
        smartContractClosePost = getInitValueReplacingNullToEmpty(configName, section, key);
    }

    String getInitValueReplacingNullToEmpty(String fileName, String section, String key){
        IniFileHelper iniFileHelper = new IniFileHelper();
        String value = iniFileHelper.getValue(fileName, section, key);
        if (null != value){
            value.trim();
        } else {
            value = "";
        }
        return value;
    }

    public String getConfigurationFullPath(){
        return getConfigurationFullPath(smartContractName);
    }

    // For easier to test
    static public String getConfigurationFullPath(String smartContractName){
        return  SmartContractHelper.getDefaultSmartContractPath() + File.separator + smartContractName + ".scc";
    }

    static public String getSmartContractFullPath(String smartContractName){
        return  SmartContractHelper.getDefaultSmartContractPath() + File.separator + smartContractName;
    }

    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public boolean isInstanceMust(String method){
        for (String m : instanceMustMethods){
            if (m.equals(method)) return true;
        }
        return false;
    }


    public boolean isDelayedInstantiation() {
        // root variety as layer 0 only boot other smart contract. aand then no instance
        if (varietyName.equals(SmartContractHelper.getRootVarietyName())) return true;

        return isDelayedInstance;
    }

    public boolean isRunning() {
        return null != thread;
    }

    public String getSmartContractPath() {
        return smartContractPath;
    }

    public String getSmartContractName() {
        return smartContractName;
    }

    public String getVarietyName() {
        return varietyName;
    }

    public String getParentNameChain() {
        return parentNameChain;
    }

    public String getSmartContractLauncher() {
        return smartContractLauncher;
    }

    public String getSmartContractClosePost() {
        return smartContractClosePost;
    }

    public String getRunCommand() {
        return runCommand;
    }
}

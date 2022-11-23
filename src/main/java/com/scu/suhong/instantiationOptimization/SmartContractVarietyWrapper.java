package com.scu.suhong.instantiationOptimization;

import util.FileHelper;
import util.ThreadHelper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SmartContractVarietyWrapper implements RequestProviderInterface {
    int requestId = 0; // Used to differentiate request to called smart contract
    private SmartContractInstanceConfig selfConfig;
    private String parentIdentifier = "root."; // '.' is the root smart contract variety directly
    private String selfVarietyName = "defaultVarName";// used to call others, not for myself
    private String smartContractPath = "";
    private String selfSmartContractName = "";
    private final Map<String, SmartContractInstanceConfig> varietyId2ConfigMap = new HashMap();

    public SmartContractVarietyWrapper(String smartContractPath, String smartContractName, String varietyName, String parentIdentifier, boolean nowCreated) {
        System.out.printf("[SmartContractVarietyWrapper][INFO] Try to create CommonSCVariety with path: '%s' smartContractName: '%s' variety: '%s' parentnamechain: '%s'\n",
                smartContractPath, smartContractName, varietyName, parentIdentifier);
        this.smartContractPath = smartContractPath;
        this.selfSmartContractName = smartContractName;
        this.selfVarietyName = varietyName;
        this.parentIdentifier = parentIdentifier;


        String identifier = getIdentify();
        SmartContractInstanceConfig config = createSmartContractConfig(identifier, smartContractPath, smartContractName, varietyName);
        setSelfConfig(config);
        if (!config.isDelayedInstantiation() || nowCreated) {
            System.out.println("[SmartContractVarietyWrapper][INFO] Try to start thread of " + smartContractName);
            config.setThread(createThread(this));
        } else{
            System.out.println("[SmartContractVarietyWrapper][INFO] Delay to start thread of " + smartContractName);
        }
        System.out.printf("[SmartContractVarietyWrapper][INFO] Succeed to create CommonSCVariety with path: '%s' smartContractName: '%s' variety: '%s' parentnamechain: '%s'\n",
                smartContractPath, smartContractName, varietyName, parentIdentifier);
    }

    public SmartContractVarietyWrapper(String smartContractPath, String smartContractName, String varietyName, String parentIdentifier,
                                       boolean nowCreated, SmartContractInstanceConfig config) {
        this.smartContractPath = smartContractPath;
        this.selfSmartContractName = smartContractName;
        this.selfVarietyName = varietyName;
        this.parentIdentifier = parentIdentifier;
        this.selfConfig = config;
    }

    static public synchronized SmartContractVarietyWrapper createSmartContractVariety(SmartContractInstanceConfig config, boolean nowCreated) {
        return createSmartContractVariety(config.getSmartContractPath(),
                config.getSmartContractName(), config.getVarietyName(), config.getParentNameChain() , nowCreated, config);
    }

    static public synchronized SmartContractVarietyWrapper createSmartContractVariety(
            String smartContractPath, String smartContractName, String parentNameChain, String varietyName) {
        return createSmartContractVariety(smartContractPath, smartContractName, varietyName, parentNameChain,false);
    }

    // We require names of varieties to be unique in a process of a smart contract or errors in creation
    // createSmartContractInstance is also called by delayed instance method
    static public synchronized SmartContractVarietyWrapper createSmartContractVariety(
            String smartContractPath, String smartContractName, String varietyName, String parentNameChain, boolean nowCreated){
        return new SmartContractVarietyWrapper(smartContractPath, smartContractName, varietyName, parentNameChain, nowCreated);
    }

    static public synchronized SmartContractVarietyWrapper createSmartContractVariety(
            String smartContractPath, String smartContractName, String varietyName, String parentNameChain, boolean nowCreated, SmartContractInstanceConfig config) {
        return new SmartContractVarietyWrapper(smartContractPath, smartContractName, varietyName, parentNameChain, nowCreated, config);
    }

    SmartContractInstanceConfig createSmartContractConfig(
            String identifier, String smartContractPath, String smartContractName, String varietyName) {
        // 0. check if already created
        if (varietyId2ConfigMap.containsKey(identifier)) {
            System.out.printf("[InstantiationManager][WARN] same config has been created twice for %s of %s\n",
                    identifier, smartContractName);
            return varietyId2ConfigMap.get(identifier);
        }

        SmartContractInstanceConfig config = new SmartContractInstanceConfig(smartContractPath, smartContractName, varietyName, generateIdentifier(), null);
        varietyId2ConfigMap.put(identifier, config);
        return config;
    }

    SmartContractInstanceConfig getSmartContractConfig(String varietyName) {
        String identifier = getVarietyIdentify(varietyName);
        if (varietyId2ConfigMap.containsKey(identifier)) {
            return varietyId2ConfigMap.get(identifier);
        }
        System.out.printf("[SmartContractVarietyWrapper][ERROR] Variety %s is still not created \n", varietyName);
        return null;
    }

    static synchronized Thread createThread(SmartContractVarietyWrapper variety) {
        Thread thread = new Thread(new InvocationOtherSmartContractThread(variety), "");
        thread.start();
        System.out.printf("[SmartContractVarietyWrapper][%d][INFO] Thead %d of variety %s of sc %s has been  created\n",
                Thread.currentThread().getId(), thread.getId(), variety.getSelfVarietyName(), variety.getSelfSCName() );
        return thread;
    }

    //                                                                 File communication (Request_)
    // Invocation path: sm1->[ standing variety of sm2 on sm1 side ]------------------------->   sm2 (Dispatch request to method)
    // This is sm1 part common sending function
    // Send to this variety
    // parameterList is separated by ';'
    // By file
    // Format: Request_<id>: method;;parameterList
    public synchronized boolean sendRequestToVarietyMappedSCProcess(String method, String parameterList) {
        return doSendRequestToVariety(method, parameterList);
    }

    public synchronized boolean sendTerminationRequestToVarietyMappedSCProcess() {
        return doSendRequestToVariety(SmartContractHelper.getTerminationRequest(), "");
    }

    private synchronized boolean doSendRequestToVariety(String method, String parameterList) {
        System.out.printf("[SmartContractVarietyWrapper][INFO] Try to send request to method %s\n", method);
        if (selfConfig.isInstanceMust(method) && !selfConfig.isRunning()) {
            System.out.printf("[SmartContractVarietyWrapper][INFO] Try to create delayed instance of %s at method %s\n", selfVarietyName, method);
            Thread thread = createThread(this);
            selfConfig.setThread(thread);
        }

        int currentRequestId = requestId + 1;

        SmartContractHelper.createVarietyWorkingFolder(getIdentify());
        String path = SmartContractHelper.getSmartContractWorkingFolder(getIdentify());

        String tmpFileName = path + File.separator + "tmp_" + currentRequestId;
        String fileName = SmartContractHelper.getRequestFileFullPath(getIdentify(), currentRequestId);
        FileHelper.deleteFile(tmpFileName);
        if (!FileHelper.createFile(tmpFileName, method + SmartContractHelper.getFieldSeparator() + parameterList)){
            System.out.printf("[SmartContractVarietyWrapper][ERROR] Failed to create temporary file %s\n", tmpFileName);
            return false;
        }
        FileHelper.deleteFile(fileName);
        if (!FileHelper.renameFile(tmpFileName, fileName)){
            System.out.printf("[SmartContractVarietyWrapper][ERROR] Failed to move file %s to %s\n", tmpFileName, fileName);
            return false;
        }
        // call method by pipe

        ++requestId;
        return true;
    }

    public void waitSmartContractToFinish(){
        while (selfConfig.getThread().isAlive()){
            ThreadHelper.safeSleepSecond(1);
        }
        System.out.printf("[SmartContractVarietyWrapper][INFO] Smart contract instance %s has terminated\n",
                selfConfig.varietyName);
    }

    public void setSelfConfig(SmartContractInstanceConfig selfConfig) {
        this.selfConfig = selfConfig;
    }

    public String getParentIdentifier() {
        return parentIdentifier;
    }

    public void setParentIdentifier(String parentIdentifier) {
        this.parentIdentifier = parentIdentifier;
    }

    public String getSelfVarietyName() {
        return selfVarietyName;
    }

    public void setVarietyName(String selfName) {
        this.selfVarietyName = selfName;
    }

    public String getSmartContractPath() {
        return smartContractPath;
    }

    public void setSmartContractPath(String smartContractPath) {
        this.smartContractPath = smartContractPath;
    }

    public String getSelfSCName() {
        return selfSmartContractName;
    }

    public void setSelfSCName(String selfSCName) {
        this.selfSmartContractName = selfSCName;
    }

    public String generateIdentifier() {
        return parentIdentifier + getIdentifySeparator() + selfVarietyName;
    }


    public String getIdentify() {
        return generateIdentifier();
    }

    public String getVarietyIdentify(String varietyName) {
        return generateIdentifier() + getIdentifySeparator() + varietyName;
    }

    public String getIdentifySeparator() {
        return ".";
    }

    public String getFullPath() {
        return smartContractPath + File.pathSeparator + selfSmartContractName;
    }

    public String getCommand() {
        String r = smartContractPath + File.separator + selfConfig.getRunCommand();
        String smartContractLauncher = selfConfig.getSmartContractLauncher();
        String smartContractClosePost = selfConfig.getSmartContractClosePost();
        if (null != smartContractLauncher && !smartContractLauncher.isEmpty()) {
            r = smartContractLauncher + " " + r + " " + smartContractClosePost;
        }
        return r;
    }

    public String getSmartContractWorkingFolder() {
        return SmartContractHelper.getSmartContractWorkingFolder(getIdentify());
    }
}

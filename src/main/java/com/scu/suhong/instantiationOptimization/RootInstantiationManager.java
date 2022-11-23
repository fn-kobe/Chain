package com.scu.suhong.instantiationOptimization;

import java.util.HashMap;
import java.util.Map;

// We only control root instance and later smart contract instance is a variety
public class RootInstantiationManager {
    private static RootInstantiationManager instance = new RootInstantiationManager();
    // it is a delayed smart contract to no instantiation at all
    // and just forward request to other smart contract
    Map<String, SmartContractVarietyWrapper> name2VarietyMap;

    static public RootInstantiationManager getInstance(){
        return instance;
    }

    private RootInstantiationManager() {
        name2VarietyMap = new HashMap<>();
    }

    // Whether create immediately or not, it depends on the config file of that smart contract
    public synchronized SmartContractVarietyWrapper createSmartContractVariety(String smartContractName, String varietyName) {
        return createSmartContractVariety(smartContractName, varietyName, false);
    }

    public synchronized SmartContractVarietyWrapper createSmartContractVariety(String smartContractName, String varietyName, boolean forceCreation) {
          if (name2VarietyMap.containsKey(varietyName)){
            System.out.printf("[RootInstantiationManager][ERROR] Variety %s has alread been created at top level\n", varietyName);
            return null;
        }
        SmartContractVarietyWrapper variety = SmartContractVarietyWrapper.createSmartContractVariety(
                SmartContractHelper.getDefaultSmartContractPath(), smartContractName, "", varietyName, forceCreation);
        name2VarietyMap.put(varietyName, variety);
        return variety;
    }

    public synchronized boolean sendRequestToVariety(String varietyName, String method, String parameterList) {
        if (name2VarietyMap.containsKey(varietyName)) {
            return name2VarietyMap.get(varietyName).sendRequestToVarietyMappedSCProcess(method, parameterList);
        }

        System.out.printf("[RootInstantiationManager][ERROR] No variety %s is created to invoke method of %s\n",
                varietyName, method);
        return false;
    }

   public SmartContractVarietyWrapper getVariety(String varietyName){
        return name2VarietyMap.get(varietyName);
    }
}

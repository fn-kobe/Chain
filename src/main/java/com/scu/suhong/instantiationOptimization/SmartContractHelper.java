package com.scu.suhong.instantiationOptimization;

import com.scu.suhong.instantiationOptimization.inner.SleepAndCallTwoExternalMethod;
import com.scu.suhong.instantiationOptimization.inner.SleepAndCallTwoExternalMethodThread;
import util.FileHelper;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SmartContractHelper {
    static final String smartContractSubFolder = "smartContract";
    static final String parameterSeparator = ";";// several parameters for one function
    static final String fieldSeparator = ":";
    static final String terminationRequest = "termination";
    static final String forceNewInstance = "force";

    public static String getRootDummyPath(){
        return "rootDummyPath";
    }

    public static String getRootSmartContractName(){
        return "rootSmartContractName";
    }

    public static String getRootVarietyName(){
        return "rootVarietyName";
    }

    public static boolean runMethod(Object object, String methodName, String[] parameterArray){
        Method method= null;
        int parameterCount = 0;
        if (null != parameterArray) parameterCount = parameterArray.length;
        try {
            Method[] methods = object.getClass().getDeclaredMethods();
            Method tmp;
            for (int i = 0; i < methods.length; ++i){
                tmp = methods[i];
                if (tmp.getName().equals(methodName) && tmp.getParameterCount() == parameterCount){
                    method = tmp;
                    break;
                }
            }
            if (null != method) {
                method.invoke(object, parameterArray);
                return true;
            }
            System.out.printf("[SmartContractHelper][ERROR] Method %s with %d parameter(s) is not found for class %s\n",
                    methodName, parameterCount, object.getClass().getName());
            return false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

    static public String getDefaultSmartContractPath(){
        return System.getProperty("user.dir") + File.separator + smartContractSubFolder;
    }

    static public String getGeneralSmartContractWorkingFolder(){
        return "SmartContractWorkingFolder";
    }

    static public boolean createVarietyWorkingFolder(String identifier){
        FileHelper.createFolderIfNotExist(SmartContractHelper.getGeneralSmartContractWorkingFolder());
        String path = SmartContractHelper.getSmartContractWorkingFolder(identifier);
        return FileHelper.createFolderIfNotExist(path);
    }

    static public String getSmartContractWorkingFolder(String identifier) {
        return getGeneralSmartContractWorkingFolder() + File.separator + identifier;
    }

    static public String getRequestFileFullPath(String identifier, int requestId) {
        return getSmartContractWorkingFolder(identifier) + File.separator + SmartContractHelper.getRequestFilePrefix() + requestId;
    }

    static public void sendTerminationRequest(String identifier, int requestId){
        FileHelper.createFile(SmartContractHelper.getRequestFileFullPath(identifier, requestId),
                SmartContractHelper.getTerminationRequest());
    }

    static public String getRequestFilePrefix(){
        return "request_";
    }

    public static String getParameterSeparator() {
        return parameterSeparator;
    }

    public static String getFieldSeparator() {
        return fieldSeparator;
    }

    public static String getTerminationRequest() {
        return terminationRequest;
    }

    public static boolean processMethod(List<String> parameterArray) {
        return processMethod(parameterArray.toArray(new String[parameterArray.size()]));
    }

    public static boolean processMethod(String[] parameterArray) {
        String varietyName = parameterArray[0];
        List<String> methodList = new ArrayList<>();
        List<String> parameterList = new ArrayList<>();
        for (int j = 1; j < parameterArray.length; j += 2) {
            methodList.add(parameterArray[j]);
            if (j + 1 >= parameterArray.length) {// special handling for last empty parameter
                parameterList.add("");
            } else {
                parameterList.add(parameterArray[j + 1]);
            }
        }

        return sendBatchRequestToVariety(varietyName, methodList, parameterList);
    }

    static boolean sendBatchRequestToVariety(String varietyName, List<String> methodLList, List<String> parameterList){
        RootInstantiationManager instantiationManager = RootInstantiationManager.getInstance();
        if (methodLList.size() != parameterList.size()){
            System.out.printf("[InstantiationTransactionProcessor][ERROR][ByName] method list %d is not smae as the number in parameter list %d\n",
                    methodLList.size(), parameterList.size());
            for (String m : methodLList) System.out.println("[InstantiationTransactionProcessor][ERROR] method " + m);
            for (String p : parameterList) System.out.println("[InstantiationTransactionProcessor][ERROR] parameter " + p);
            return false;
        }

        for (int i = 0; i < methodLList.size(); ++i) {
            System.out.printf("[InstantiationTransactionProcessor][INFO] Try to invoke method '%s' of '%s' with parameters '%s'\n", varietyName, methodLList.get(i), parameterList.get(i));
            if (!instantiationManager.sendRequestToVariety(varietyName, methodLList.get(i), parameterList.get(i))){
                System.out.printf("[InstantiationTransactionProcessor][INFO] Failed to invoke method '%s' of '%s' with parameters '%s'\n", varietyName, methodLList.get(i), parameterList.get(i));
                return false;
            }
        }
        return true;
    }

    public static void processNewInstance( String smartContractName, String varietyName){
        String[] parameterArray = {smartContractName, varietyName};
        processNewInstance(parameterArray);
    }

    public static void processNewInstance(String[] parameterArray){
        String smartContractName = parameterArray[0];
        String varietyName = parameterArray[1];
        boolean forceInstantiation = false;
        if (parameterArray.length > 2){
            if (parameterArray[2].equals(forceNewInstance)) forceInstantiation= true;
        }
        System.out.printf("[InstantiationTransactionProcessor][INFO] Begin to create %s for smart contract %s\n", varietyName, smartContractName);
        RootInstantiationManager.getInstance().createSmartContractVariety(smartContractName, varietyName, forceInstantiation);
    }

    public static SmartContractVarietyWrapper processNewAndInvocation(String[] parameterArray, String d){
        if (parameterArray.length < 3) {
            System.out.printf("[InstantiationTransactionProcessor][WARN] Parameters '%s' are not enough. Skip to instantiate and invoke \n", d);
            return null;
        }

        String smartContractName = parameterArray[0];
        String varietyName = parameterArray[1];

        List<String> methodList = new ArrayList<>();
        List<String> parameterList = new ArrayList<>();
        for (int j = 2; j < parameterArray.length; j += 2){
            methodList.add(parameterArray[j]);
            if (j + 1 >= parameterArray.length){// special handling for last empty parameter
                parameterList.add("");
            } else {
                parameterList.add(parameterArray[j + 1]);
            }
        }

        RootInstantiationManager instantiationManager = RootInstantiationManager.getInstance();
        // We do not add force as it will call method. If this is not ok, please change it as it is also easier way to handle
        SmartContractVarietyWrapper variety = instantiationManager.createSmartContractVariety(smartContractName, varietyName);
        sendBatchRequestToVariety(variety, methodList, parameterList);
        return variety;
    }

    public static boolean processDisposable(String[] parameterArray, String d){
        if (parameterArray.length < 3) {
            System.out.printf("[InstantiationTransactionProcessor][WARN] Parameters '%s' are less than expected. Skip to instantiate and invoke \n", d);
            return false;
        }
        SmartContractVarietyWrapper varietyWrapper = processNewAndInvocation(parameterArray, d);
        if (null == varietyWrapper){
            return false;
        }
        return varietyWrapper.sendTerminationRequestToVarietyMappedSCProcess();
    }

    static void sendBatchRequestToVariety(SmartContractVarietyWrapper variety, List<String> methodLList, List<String> parameterList){
        if (methodLList.size() != parameterList.size()){
            System.out.printf("[InstantiationTransactionProcessor][ERROR][ByVariety] method list %d is not smae as the number in parameter list %d\n",
                    methodLList.size(), parameterList.size());
            for (String m : methodLList) System.out.println("[InstantiationTransactionProcessor][ERROR] method " + m);
            for (String p : parameterList) System.out.println("[InstantiationTransactionProcessor][ERROR] parameter " + p);
            return;
        }

        for (int i = 0; i < methodLList.size(); ++i) {
            System.out.printf("[InstantiationTransactionProcessor][INFO] Try to invoke method '%s' parameters '%s'\n", methodLList.get(i), parameterList.get(i));
            variety.sendRequestToVarietyMappedSCProcess(methodLList.get(i), parameterList.get(i));
        }
    }

    public static boolean processInnerSmartContract(String[] parameterArray, String d) {
        String scName = parameterArray[0];
        if (!scName.equals("SleepAndCallTwoExternalMethod")){
            System.out.printf("[InstantiationTransactionProcessor][ERROR] Not support inner smart contract '%s'\n", scName);
            return false;
        }
        if (parameterArray.length < 6) {
            System.out.printf("[InstantiationTransactionProcessor][WARN] Parameters '%s' are less than expected. Skip to instantiate and invoke \n", d);
            return false;
        }

        SleepAndCallTwoExternalMethodThread sc = new SleepAndCallTwoExternalMethodThread(parameterArray[1], parameterArray[2], parameterArray[3], parameterArray[4], parameterArray[5]);
        Thread t = new Thread(sc);
        t.start();
        return true;
    }

    public static void waitSmartContractToFinish(String varietyName){
        RootInstantiationManager instantiationManager = RootInstantiationManager.getInstance();
        SmartContractVarietyWrapper variety = instantiationManager.getVariety(varietyName);
        if (null == variety){
            System.out.printf("[InstantiationTransactionProcessor][ERROR] Cannot find variety '%s'\n", varietyName);
            return;
        }

        variety.waitSmartContractToFinish();
    }
}

package com.scu.suhong.smartcontract.lifecycleFlexibility;

import org.jetbrains.annotations.NotNull;
import util.FileHelper;
import util.GasHandler;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class LifecycleUtility {
	final static String smartContractWorkPath = "smartContract";
	final static String javaFilePostFix = ".java";

	static JavaCodeCompiler javaCodeCompiler = new JavaCodeCompiler();
	static Map<String, Object> instantiatedObjectMap = new HashMap<>();
	static Map<String, String> instantiatedCodeNameMap = new HashMap<>();
	static Map<String, String> codeNameInstanceMap = new HashMap<>();

	// We do not consider the version here for  simplicity, and we consider it later
	public static boolean putCodeToBC(String fileName, String code, Map<String, String> kVParameters){
		//0. prepare
		FileHelper.createFolderIfNotExist(smartContractWorkPath);
		// Only used in Life cycle utility to record whether it has been twice compiled
		String deploymentFileName = getDeploymentFileName(fileName);

		// 1. store code
		// 1.1 handle existing smart contract
		if (FileHelper.doesFileOrFolderExist(deploymentFileName)){
			if (!kVParameters.containsKey("forceOverwrite")) {
				System.out.printf("[LifecycleUtility][WARN] Skip to update existing smart contract file %s\n", deploymentFileName);
				return false;
			} else{
				System.out.printf("[LifecycleUtility][WARN] Force to update existing smart contract file %s\n", deploymentFileName);
			}
		}

		String decodedCode = new String (Base64.getDecoder().decode(code));
		FileHelper.createFile(deploymentFileName, decodedCode);

		// 2. compile it
		long testGas = 10000;
		javaCodeCompiler.shouldCheckGas(false);
		try {
			// file will be checked at compiler side, and we just pass file name to compiler.
			return javaCodeCompiler.compile(getClassName(fileName), "", decodedCode);
		} catch (ClassCastException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean instantiation(String codeName, String instanceName, Map<String, String> kVParameters){
		//1. check whether code is there and compiled successfully - by class found for java
		Class compiledClass = javaCodeCompiler.getClassByName(getClassName(codeName));
		if (null == compiledClass){
			System.out.printf("[LifecycleUtility][ERROR] No code is found when try to instantiate %s\n", codeName);
			return false;
		}

		//2. check whether the instance has been instantiated before
		if (instantiatedObjectMap.containsKey(instanceName)){
			System.out.printf("[LifecycleUtility][ERROR] Instance %s for code has been instantiated\n", instanceName, codeName);
			return false;
		}

		Object o = javaCodeCompiler.getInstance(getClassName(codeName));
		if (null == o){
			System.out.printf("[LifecycleUtility][ERROR] Failed to instantiate %s with instance name\n", codeName, instanceName);
		}

		System.out.printf("[LifecycleUtility][INFO] Succeed to instantiate %s with instance name %s\n", codeName, instanceName);
		instantiatedObjectMap.put(instanceName, o);
		instantiatedCodeNameMap.put(instanceName, codeName);
		codeNameInstanceMap.put(codeName, instanceName);
		return true;
	}

	public static boolean invocation(String instanceName, String methodName, String returnValue, Map<String, String> kVParameters){
		// check whether the according instance can be found or not
		if (!instantiatedObjectMap.containsKey(instanceName)){
			System.out.printf("[LifecycleUtility][ERROR] Failed to invoke %s, as its instance is not found\n", instanceName);
			return false;
		}

		try {
			if (returnValue.equalsIgnoreCase("boolean")) {
				javaCodeCompiler.runBooleanMethod(instantiatedObjectMap.get(instanceName),
								instantiatedCodeNameMap.get(instanceName), methodName);
				return true;
			} else if (returnValue.equalsIgnoreCase("string")) {
				javaCodeCompiler.runMethod(instantiatedObjectMap.get(instanceName),
								instantiatedCodeNameMap.get(instanceName), methodName);
				return true;
		} else if (returnValue.equalsIgnoreCase("common")) {
			javaCodeCompiler.runMethod(instantiatedObjectMap.get(instanceName),
							instantiatedCodeNameMap.get(instanceName), methodName);
			return true;
		}
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			return false;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return false;
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			return false;
		}

		System.out.printf("[LifecycleUtility][INFO] Not supported return value '%s' for method '%s'\n", returnValue, methodName);
		return false;
	}


	@NotNull
	static String getDeploymentFileName(String fileName) {
		return smartContractWorkPath + File.separator + FileHelper.getFileNameByClassOrFileName(fileName);
	}

	static boolean cleanDeploymentFileName(String fileName) {
		javaCodeCompiler.cleanCompiledFile(fileName);
		return FileHelper.deleteFile(getDeploymentFileName(fileName));
	}

	static boolean cleanInstantiation(String codeName) {
		if (!instantiatedObjectMap.containsKey(codeName)) return false;

		instantiatedObjectMap.remove(codeName);
		return true;
	}

	static boolean clean(String codeName) {
		boolean r =	cleanDeploymentFileName(codeName);
		if (!cleanInstantiation(codeName)) r = false;
		return r;
	}

	public static String getClassName(String fileOrClassName){
		if (fileOrClassName.endsWith(javaFilePostFix)){
			return fileOrClassName.split(javaFilePostFix)[0];
		}
		return fileOrClassName;
	}

	public static boolean hasInstance(String codeName) {
		return codeNameInstanceMap.containsKey(codeName);
	}
}

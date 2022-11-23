package com.scu.suhong.smartcontract.lifecycleFlexibility;

import org.junit.Test;
import util.FileHelper;

import java.io.File;
import java.util.HashMap;

public class LifecycleUtilityTest {
	String testCodeName = "TestClass";
	String testInstantiationName = "TestClass";
	String testMethodName = "testMethod";

	@Test
	public void putCodeToBC() {
		assert doPutCodeToBC();
	}

	private boolean doPutCodeToBC() {
		String tempFolderName = "temp";
		FileHelper.createFolderIfNotExist(tempFolderName);
		String tempFileName = tempFolderName + File.separator + testCodeName + ".java";
		String code = "public class " + testCodeName + " {\n";
		code += "int i = 0;\n";
		code += "public void " + testMethodName + "(){\n";
		code += "System.out.printf(\"[Test][INFO][***********] " + testMethodName + " in " + testCodeName + ". "+ "Test function called successfully!\\n\");\n";
		code += "}\n";
		code += "}\n";
		FileHelper.createFile(tempFileName, code);
		String decodedString = FileHelper.getBase64FileString(tempFileName);

		LifecycleUtility.cleanDeploymentFileName(testCodeName);
		return LifecycleUtility.putCodeToBC(testCodeName, decodedString, new HashMap<>());
	}

	@Test
	public void instantiation() {
		assert doInstantiation();
		// re-instantiate twice with the same name error
		assert !LifecycleUtility.instantiation(testCodeName, testInstantiationName, new HashMap<>());
	}

	private boolean doInstantiation() {
		doPutCodeToBC();

		LifecycleUtility.cleanInstantiation(testInstantiationName);
		return LifecycleUtility.instantiation(testCodeName, testInstantiationName, new HashMap<>());
	}

	@Test
	public void invocation() {
		assert doInvocation();
		assert !LifecycleUtility.invocation(testInstantiationName, testMethodName, "unknown", new HashMap<>());
	}

	private boolean doInvocation() {
		doInstantiation();
		return LifecycleUtility.invocation(testInstantiationName, testMethodName, "common", new HashMap<>());
	}

	@Test
	public void testGetClassName() {
		String className = "TestCalssName";
		String name = className + ".java";
		assert className.equals(LifecycleUtility.getClassName(name));
		assert className.equals(LifecycleUtility.getClassName(className));
	}

}
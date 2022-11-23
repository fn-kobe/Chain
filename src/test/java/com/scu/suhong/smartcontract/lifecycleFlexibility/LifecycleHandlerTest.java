package com.scu.suhong.smartcontract.lifecycleFlexibility;

import com.scu.suhong.transaction.Transaction;
import mockit.Expectations;
import mockit.FullVerifications;
import org.junit.Test;
import util.FileHelper;

import java.io.File;
import java.util.Map;

public class LifecycleHandlerTest {
	String codeName = "TestPuttingCode"; // we use Java as smart contract language
	String code = "int i = 0;";
	String instanceName = "instance1"; // we use Java as smart contract language
	String methodName = "method1";
	String separator = LifecycleHandler.lifecycleSeparator;

	@Test
	public void testProcessRealTransaction() {
		LifecycleHandler handler = LifecycleHandler.getInstance();
		Transaction t = new Transaction();
		String tempFolderName = "temp";
		createTestFile(codeName, methodName, tempFolderName);
		String code = FileHelper.getBase64FileString(FileHelper.getFileNameByClassOrFileName(codeName, tempFolderName));
		assert null != code && !code.isEmpty();
		// String codeName, String code, List<String> kVParameters
		String data = LifecycleHandler.lifecycleTxKeyword + LifecycleHandler.lifecycleSeparator +
						LifecycleHandler.lifecycleStagePuttingCode +LifecycleHandler.lifecycleStageInstantiation + LifecycleHandler.lifecycleStageInvocation + LifecycleHandler.lifecycleSeparator +
						LifecycleHandler.getParameterKeyCodeName() + LifecycleHandler.getKeyValueSeparator() + codeName + LifecycleHandler.getPairsSeparator() +
						LifecycleHandler.getParameterKeyCode() + LifecycleHandler.getKeyValueSeparator() + code + LifecycleHandler.getPairsSeparator() +
						LifecycleHandler.getParameterKeyInstance() + LifecycleHandler.getKeyValueSeparator() + instanceName + LifecycleHandler.getPairsSeparator() +
						LifecycleHandler.getParameterKeyMethod() + LifecycleHandler.getKeyValueSeparator() + methodName;
		System.out.println("[TEST][****] " + data);
		t.setData(data);
		t.setKeyValueParameter("gas#1000000");
		t.setId();
		LifecycleHandler.clean(codeName);
		assert handler.process(t);
		System.out.println("\n[TEST][INFO] Test duplicated case, which will fail\n");
		assert !handler.process(t);// Failed as already existing
	}

	@Test
	public void testProcessPuttingCode() {
		mockLifecycleUtilityPutCodeToBC();

		LifecycleHandler handler = LifecycleHandler.getInstance();
		Transaction t = new Transaction();
		// String codeName, String code, List<String> kVParameters
		String data = LifecycleHandler.lifecycleTxKeyword + separator + LifecycleHandler.lifecycleStagePuttingCode +
						separator + codeName + separator + code;
		t.setData(data);
		t.setId();
		assert handler.process(t);
	}

	@Test
	public void testProcessInstantiation() {
		mockLifecycleUtilityinstantiation();

		LifecycleHandler handler = LifecycleHandler.getInstance();
		Transaction t = new Transaction();
		// String codeName, String instanceName, List<String> kVParameters
		String data = LifecycleHandler.lifecycleTxKeyword + separator + LifecycleHandler.lifecycleStageInstantiation +
						separator + codeName + separator + instanceName;
		t.setData(data);
		t.setId();
		assert handler.process(t);
	}

	@Test
	public void testProcessInvocation() {
		mockLifecycleUtilityinvocation();

		LifecycleHandler handler = LifecycleHandler.getInstance();
		Transaction t = new Transaction();
		// String codeName, String code, List<String> kVParameters
		String data = LifecycleHandler.lifecycleTxKeyword + separator + LifecycleHandler.lifecycleStageInvocation +
						separator + instanceName + separator + methodName;
		t.setData(data);
		t.setId();
		assert handler.process(t);
	}

	@Test
	public void testProcessAll() {
		mockLifecycleUtilityAllMethod();

		LifecycleHandler handler = LifecycleHandler.getInstance();
		Transaction t = new Transaction();
		// String codeName, String code, List<String> kVParameters
		String data = LifecycleHandler.lifecycleTxKeyword + separator + LifecycleHandler.lifecycleStagePuttingCode +
						LifecycleHandler.lifecycleStageInstantiation + LifecycleHandler.lifecycleStageInvocation +
						separator + instanceName + separator + methodName;
		t.setData(data);
		t.setId();
		assert handler.process(t);

		new FullVerifications() {{
			LifecycleUtility.putCodeToBC(anyString, anyString, (Map<String, String>) any);
			LifecycleUtility.instantiation(anyString, anyString, (Map<String, String>) any);
			LifecycleUtility.invocation(anyString, anyString, "boolean", (Map<String, String>) any);
		}};
	}

	private boolean createTestFile(String testCodeName, String testMethodName, String tempFolderName){
		FileHelper.createFolderIfNotExist(tempFolderName);
		String tempFileName = tempFolderName + File.separator + FileHelper.getFileNameByClassOrFileName(testCodeName);
		String code = "public class " + testCodeName + " {\n";
		code += "int i = 0;\n";
		code += "public void " + testMethodName + "(){\n";
		code += "System.out.printf(\"[Test][INFO][***********] " + testMethodName + " in " + testCodeName + ". "+ "Test function called successfully!\\n\");\n";
		code += "}\n";
		code += "}\n";
		return FileHelper.createFile(tempFileName, code);
	}

	private void mockLifecycleUtilityAllMethod() {
		mockLifecycleUtilityPutCodeToBC();
		mockLifecycleUtilityinstantiation();
		mockLifecycleUtilityinvocation();
	}

	private void mockLifecycleUtilityinvocation() {
		new Expectations(LifecycleUtility.class) {
			{
				LifecycleUtility.invocation(anyString, anyString, "boolean", (Map<String, String>) any);
				System.out.println("[TEST][MOCK] Simulate to invoke code");
				result = true;
			}
		};
	}

	private void mockLifecycleUtilityinstantiation() {
		new Expectations(LifecycleUtility.class) {
			{
				LifecycleUtility.instantiation(anyString, anyString, (Map<String, String>) any);
				System.out.println("[TEST][MOCK] Simulate to instantiate code");
				result = true;
			}
		};
	}

	private void mockLifecycleUtilityPutCodeToBC() {
		new Expectations(LifecycleUtility.class) {
			{
				LifecycleUtility.putCodeToBC(anyString, anyString, (Map<String, String>) any);
				System.out.println("[TEST][MOCK] Simulate to put code to BC");
				result = true;
			}
		};
	}

}
package com.scu.suhong.smartcontract.lifecycleFlexibility;

import com.scu.suhong.block.Block;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.Transaction;
import util.GasHandler;
import util.StringHelper;

import java.util.Map;

public class LifecycleHandler {
	static LifecycleHandler instance = new LifecycleHandler();

	public final static String lifecycleTxKeyword = "lck";
	public final static String lifecycleSeparator = ":";
	public final static String lifecycleStagePuttingCode = "c";
	public final static String lifecycleStageInstantiation = "i";
	public final static String lifecycleStageInvocation = "v";

	public final static String parameterKeyCodeName = "n";
	public final static String parameterKeyCode = "c";
	public final static String parameterKeyInstance = "i";
	public final static String parameterKeyMethod = "m";

	public final static String pairsSeparator = ",";
	public final static String keyValueSeparator = "#";

	public static LifecycleHandler getInstance() {
		return instance;
	}

	private LifecycleHandler() {
	}

	public void tryAddNewBlock(Block block) {
		System.out.println("[LifecycleHandler][DEBUG] Try add new block");
		for (AbstractTransaction t : block.getTransactions()) {
			System.out.println("[LifecycleHandler][DEBUG] Try process transaction " + t.getId());
			if (t instanceof Transaction) {
				process((Transaction) t);
			}
			System.out.println("[LifecycleHandler][DEBUG] Finished to process transaction " + t.getId());
		}
	}

	Map<String, String> getKeyValueParameters(String keyValueString) {
		return StringHelper.getKeyValueParameters(keyValueString, pairsSeparator, keyValueSeparator);
	}

	public boolean process(Transaction t) {
		// Valid lifecycle format: <lifecycleKeyword>:<lifecycleStage>:<other parameters>
		String data = t.getData();
		if (null == data || !data.startsWith(lifecycleTxKeyword + lifecycleSeparator)) {
			System.out.println("[LifecycleHandler][DEBUG] Not smart contract lifecycle transaction");
			return false;
		}

		String[] txParsedDataList = data.split(lifecycleSeparator);
		final int minLifecycleLength = 3; // <lifecycleKeyword>:<lifecycleStage>:<other parameters>
		if (txParsedDataList.length < minLifecycleLength) {
			System.out.println("[LifecycleHandler][WARN] Not enough parameter for the smart contract lifecycle");
			return false;
		}

		// the 2nd is the lifecycleStage
		String lifecycleStage = txParsedDataList[1];
		String kvList = txParsedDataList[2];
		// Gas is from the parameters of transaction
		String gasString = StringHelper.getValueFromKeyValueParameters(
						"gas", t.getKeyValueParameter(), ",", "#");
		GasHandler gasHandler = new GasHandler();
		if (null != gasString && !gasString.isEmpty()) {
			gasHandler.setGivenGas(Long.parseLong(gasString));
		}

		// fix fee to process one transaction
		final long oneTransactionFee = 1000;
		gasHandler.processGas(oneTransactionFee);

		Map<String, String> kvParameterMap = getKeyValueParameters(kvList);
		boolean isProcessed = false;
		if (lifecycleStage.contains(lifecycleStagePuttingCode)) {
			// String codeName, String code, List<String> kVParameters
			String codeName = kvParameterMap.get(parameterKeyCodeName);
			String code = kvParameterMap.get(parameterKeyCode);
			long startTime = System.currentTimeMillis();
			boolean r = LifecycleUtility.putCodeToBC(codeName, code, kvParameterMap);
			long endTime = System.currentTimeMillis();
			if (!gasHandler.processGas(startTime, endTime)) return false;
			if (!r) {
				System.out.printf("[LifecycleHandler][ERROR] Failed to put a new smart contract with parameter %s\n", data);
				return false;
			}
			System.out.printf("[LifecycleHandler][INFO] Succeed to put code '%s' into blockchain of transaction %d\n", codeName, t.getId());
			isProcessed = true;
		}

		if (lifecycleStage.contains(lifecycleStageInstantiation)) {
			// String codeName, String instanceName, List<String> kVParameters

			String codeName = kvParameterMap.get(parameterKeyCodeName);
			String instanceName = kvParameterMap.get(parameterKeyInstance);

			long startTime = System.currentTimeMillis();
			boolean r = LifecycleUtility.instantiation(codeName, instanceName, kvParameterMap);
			long endTime = System.currentTimeMillis();
			if (!gasHandler.processGas(startTime, endTime)) return false;
			if (!r) {
				System.out.printf("[LifecycleHandler][ERROR] Failed to instantiate a new smart contract with parameter %s\n", data);
				return false;
			}
			System.out.printf("[LifecycleHandler][INFO] Succeed to instantiate '%s' of '%s' by transaction %d\n", instanceName, codeName, t.getId());
			isProcessed = true;
		}

		if (lifecycleStage.contains(lifecycleStageInvocation)) {
			// String instanceName, String methodName, List<String> kVParameters
			String instanceName = kvParameterMap.get(parameterKeyInstance);
			String methodName = kvParameterMap.get(parameterKeyMethod);
			long startTime = System.currentTimeMillis();
			boolean r = LifecycleUtility.invocation(instanceName, methodName, "common", kvParameterMap);
			long endTime = System.currentTimeMillis();
			if (!gasHandler.processGas(startTime, endTime)) return false;
			if (!r) {
				System.out.printf("[LifecycleHandler][ERROR] Failed to invoke a new smart contract with parameter %s\n", data);
				return false;
			}
			System.out.printf("[LifecycleHandler][INFO] Succeed to invoke '%s' of '%s' by transaction %d\n", methodName, instanceName, t.getId());
			isProcessed = true;
		}

		if (!isProcessed) {
			System.out.printf("[LifecycleHandler][ERROR] No lifecycle keyword is in transaction %d\n", t.getId());
		} else {
			System.out.printf("[LifecycleHandler][INFO] Total gas used is %d\n", gasHandler.getTotalUsedGas());
			System.out.printf("[LifecycleHandler][INFO] Succeed to process transaction %d\n", t.getId());
		}
		return isProcessed;
	}

	public boolean hasInstance(String codeName) {
		return LifecycleUtility.hasInstance(codeName);
	}

	public static boolean clean(String codeName) {
		return LifecycleUtility.clean(codeName);
	}

	public static String getPairsSeparator() {
		return pairsSeparator;
	}

	public static String getKeyValueSeparator() {
		return keyValueSeparator;
	}

	public static String getParameterKeyCodeName() {
		return parameterKeyCodeName;
	}

	public static String getParameterKeyCode() {
		return parameterKeyCode;
	}

	public static String getParameterKeyInstance() {
		return parameterKeyInstance;
	}

	public static String getParameterKeyMethod() {
		return parameterKeyMethod;
	}
}

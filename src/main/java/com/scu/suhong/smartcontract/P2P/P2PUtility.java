package com.scu.suhong.smartcontract.P2P;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class P2PUtility {
	public final static String groupKeyConnector = "_";

	public static void updateCounter(int value, String groupKey, Map<String, Integer> counterMap, String counterName){
		// we update both all, group, address big type, and with its sub id
		updateSingleCounter(value, groupKey, counterMap);
		updateTypeCounter(value,groupKey,counterMap);
		outputCounter(counterMap, counterName);
	}

	public static void updateTypeCounter(int value, String groupKey, Map<String, Integer> counterMap){
		String typeCounterKey = getSCTypeFromKey(groupKey);
		if (counterMap.containsKey(typeCounterKey)) {
			value += counterMap.get(typeCounterKey);
		}
		updateSingleCounter(value, typeCounterKey, counterMap);

	}

	public static void updateSingleCounter(int value, String groupKey, Map<String, Integer> counterMap){
		if (!counterMap.containsKey(groupKey)) counterMap.put(groupKey, 0);
		counterMap.replace(groupKey, value);
	}

	public static void outputCounter(Map<String, Integer> counter, String counterName){
		Set<String> keyset = counter.keySet();
		String r = "";
		for (String key : keyset){
			r += "\t" + key + " " + counter.get(key);
		}
		System.out.printf("[P2PUtility][Counter][DEBUG] Output %s counter: %s\n", counterName, r);
	}

	public static int safeParseInteger(String value){
		int r;
		try {
			r = Integer.parseInt(value);
		}catch (NumberFormatException e){
			System.out.println("[P2PUtility][DEBUG] Counter format error. May caused by initial empty value");
			r = 0;
		}
		return r;
	}

	public static  String makeKey(String p2pSCType, String p2pSCTypeId) {
		return p2pSCType + groupKeyConnector + p2pSCTypeId;
	}

	public static  String getSCTypeFromKey(String groupKey) {
		return groupKey.split(groupKeyConnector)[0];
	}

}

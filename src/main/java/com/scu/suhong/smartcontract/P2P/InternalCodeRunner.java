package com.scu.suhong.smartcontract.P2P;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class InternalCodeRunner {
	static Map<String, String> outOfWorkSCMap = new HashMap<>();
	final static String simulateOOW = "out_of_work";

	// TO DO extension if possible
	static public boolean run(String internalCode){
		System.out.printf("[InternalCodeRunner][DEBUG] Run smart contract after condition check is done. " +
						"Currently it is done in the internal runner and try to enhance it in future\n");
		System.out.println("[InternalCodeRunner][INFO] Run code of " + internalCode);
		return true;
	}

	static public boolean doesTheCodeSimulateOutOfWork(String internalCode, String scName){
		System.out.println("[InternalCodeRunner][DEBUG] Check code of " + internalCode);
		if (internalCode.contains(simulateOOW.toLowerCase())){
			System.out.printf("[InternalCodeRunner][WARN] Smart contract %s is out of work.\n", scName);
			outOfWorkSCMap.put(scName, scName);
			return true;
		}
		return false;
	}

	static public boolean isSCOutOfWork(String scName){
		return outOfWorkSCMap.containsKey(scName);
	}
}

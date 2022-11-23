package com.scu.suhong.smartcontract.nondeterminacy;

import com.scu.suhong.transaction.SyncTransaction;

import java.util.HashMap;
import java.util.Map;

public class StateSyncManager {
    static final private String stateSyncKeyWord = "stateSyncKeyWord";
    static final private String stateSyncKeyWordSeparator = "::";
    static private Map<String, StateSequence> stateSequenceMap = new HashMap<>();

    // After get state from NBS, update the local state
    public boolean update(String smartContractName, String varietyName, State s){
        return syncLocalState(smartContractName, varietyName, s);
    }

    // format stateSyncKeyWord:<smart contract name>:<variety name>:<value>
    public static boolean process(String msg) {
        if (null == msg || msg.isEmpty()) return false;

        SyncTransaction syncTransaction = SyncTransaction.createFromJson(msg);
        if (null == syncTransaction) return false;

        return syncLocalState(syncTransaction.getSmartContractName(),
                syncTransaction.getVarietyName(), new State(syncTransaction.getValue()));
    }

    public static boolean syncLocalState(String smartContractName, String varietyName, String value) {
        State state = new State(value);
        return syncLocalState(smartContractName, varietyName, state);
    }

    public static boolean syncLocalState(String smartContractName, String varietyName, State state) {
        String stateKey = formVarietyName(smartContractName, varietyName);
        StateSequence stateSequence = stateSequenceMap.get(stateKey);
        if (null == stateSequence){
            stateSequence = new StateSequence();
        }

        if (!stateSequence.add(state)) return false;

        stateSequenceMap.put(stateKey, stateSequence);
        return true;
    }

    public static String formVarietyName(String smartContractName, String varietyName) {
        return smartContractName + "_" + varietyName;
    }

    public static StateSequence getStateSequence(String smartContractName, String varietyName){
        return getStateSequence(formVarietyName(smartContractName, varietyName));
    }

    public static StateSequence getStateSequence(String varietyName){
        return stateSequenceMap.get(varietyName);
    }

    public static String toSyncString(String smartContractName, String varietyName, State s){
        return stateSyncKeyWord + stateSyncKeyWordSeparator + smartContractName + stateSyncKeyWordSeparator
                + varietyName + stateSyncKeyWordSeparator + s.getJson().toString();
    }
}

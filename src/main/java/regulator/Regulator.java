package regulator;

import java.util.HashMap;
import java.util.Map;


public class Regulator {
    static Map<String, Regulator> regulatorMap;
    String name; // for debug the instance
    RegulationWorker regulationWorker;

    private Regulator(String name) {
        this.name = name;
        regulationWorker = new RegulationWorker(name);
    }

    synchronized static public Regulator getInstance(String name) {
        return doGetInstance(name);
    }
    synchronized static public Regulator getInstance(String name, String regulationAssetType) {
        return doGetInstance(name + regulationAssetType);
    }

    static public Regulator doGetInstance(String name) {
        if (null == regulatorMap) {
            regulatorMap = new HashMap<String, Regulator>();
        }
        if (!regulatorMap.containsKey(name)){
            regulatorMap.put(name, new Regulator(name));
        }
        return regulatorMap.get(name);
    }

    public boolean isRegulated() {
        return !regulationWorker.getRegulationType().equals(RegulationType.ENone);
    }

    public boolean startRegulation(RegulationType regulationType, int granularity1, int granularity2){
        return regulationWorker.startRegulation(regulationType, granularity1, granularity2);
    }

    public boolean startRegulation(RegulationType regulationType, int granularity1){
        return regulationWorker.startRegulation(regulationType, granularity1, 0);
    }

    // No matter which kind of regulation currently is,
    // Stop the regulation
    public void stopRegulation() {
        regulationWorker.stopRegulation();
    }

    // The caller know what kinds of regualtion type it is now
    // And want to just stop this kind of regulation
    public boolean stopRegulation(RegulationType regulationType){
        return regulationWorker.stopRegulation(regulationType);
    }

    // Give whether current is OK or not and increase used account
    public boolean canProcessNext(){
        int allowedAmount = getAllowedAmount();
        if (-1 == allowedAmount){
            return true;
        }
        if (0 == allowedAmount){
            return false;
        }
        return true;
    }

    public int getAllowedAmount(){
        int allowedAmount = regulationWorker.getAllowedAmount();
        int usedTxAmount = regulationWorker.getUsedTxAmount();
        System.out.println("[Regulator][" + name + "] The allowed mount is: " + allowedAmount + " usedTxAmount: " + usedTxAmount);
        return allowedAmount;
    }

    public int getAllowedSpeed(){
        return regulationWorker.getAllowedSpeed();
    }

    public void increaseUsedTxAmount(){
        if (!isRegulated()){
            System.out.println("[Regulator][" + name + "] The state is not regulated, skip increase used TxAmount");
            return;
        }
        regulationWorker.increaseUsedTxAmount();
    }

    public RegulationType getRegulationType() {
        return regulationWorker.getRegulationType();
    }
}

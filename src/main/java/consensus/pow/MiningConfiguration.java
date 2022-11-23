package consensus.pow;

import util.AddressPort;

public class MiningConfiguration {
    static MiningConfigurationHandler instance = new MiningConfigurationHandler("MiningConfiguration");

    // TO DO calculate the difficulty dynamically due to time
    static public int getRequiredZeroCount() {
        return instance.getRequiredZeroCount();
    }

    static public int getBlockchainId() {
        return instance.getBlockchainId();
    }

    static public String getBlockchainStringId() {
        return String.valueOf(instance.getBlockchainId());
    }

    static public int getDefaultBlockchainSyncInterval() {
        return instance.getDefaultBlockchainSyncInterval();
    }

    static public int getGenesisNouce() {
        return instance.getGenesisNouce();
    }

    // TO DO calculate the difficulty dynamically due to time
    static public String getMiningAccount() {
        return instance.getMiningAccount();
    }

    static public String getConsensusType(){
        return instance.getConsensusType();
    }

    static public String getNodeId(){
        return instance.getNodeId();
    }

    static public void testSetNodeIdValue(String nodeId){
        instance.testSetNodeIdValue(nodeId);
    }

    static public boolean isHashMatched(String hashValue) {
        return instance.isHashMatched(hashValue);
    }

    public static void setDifficulty(int difficulty) {
        instance.difficulty = difficulty;
    }

    // Seconds, need to be configurable
    static public int getMaxTryTime() {
        return instance.getMaxTryTime();
    }

    //only for test
    static public void testSetRequiredZeroCount(int newDifficulty) {
        instance.difficulty = newDifficulty;
    }

    static public int getSelfListenPort() {
        return instance.getSelfListenPort();
    }

    static public synchronized String getHostIP() {
        return instance.getHostIP();
    }

    static public boolean isSelf(AddressPort another) {
        return instance.isSelf(another);
    }

    static public void testSetChainId(int newID) {
        instance.testSetChainId(newID);
    }
}

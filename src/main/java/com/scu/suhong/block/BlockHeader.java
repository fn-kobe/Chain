package com.scu.suhong.block;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import util.JSONObjectHelper;
import util.TimeHelper;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BlockHeader implements Serializable {
    String previousHash;
    int blockIndex;
    int blockNounce; // It will be used to find the hash of previousHash and transaction root hash

    String timeStamp;
    String miner;
    String consensusType;// PoW or PoS and so on, default is PoW
    String nodeId;

    public BlockHeader(String previousHash, int blockNounce, int blockIndex) {
        this.previousHash = previousHash;
        this.blockNounce = blockNounce;
        this.blockIndex = blockIndex;
        commonInit();
    }

    public BlockHeader() {
        this.previousHash = "";
        this.blockNounce = 0;
        this.blockIndex = -1;
        commonInit();
    }

    void commonInit(){
        this.miner = "";
        this.consensusType = "";
        this.nodeId = "";
        this.timeStamp = getCurrentDataString();
    }

    public boolean isValid() {
        // Add if other check here
        return true;
    }

    public String Dump() {
        String dump = "<header>\n";
        dump += "Previous hash:" + previousHash + "\n";
        dump += "Block nounce:" + blockNounce + "\n";
        dump += "Block index:" + blockIndex + "\n";
        dump += "TimeStamp:" + timeStamp + "\n";
        dump += "Miner:" + miner + "\n";
        dump += "ConsensusType:" + consensusType + "\n";
        dump += "NodeId:" + nodeId + "\n";
        dump += "</header>\n";
        return dump;
    }

    public JSONObject getJson() {
        JSONObject object = new JSONObject();
        object.put("p", previousHash);
        object.put("n", blockNounce);
        object.put("index", blockIndex);
        object.put("timeStamp", timeStamp);
        object.put("miner", miner);
        object.put("consensusType", consensusType);
        object.put("nodeId", nodeId);
        return object;
    }

    static public BlockHeader createFromJson(JSONObject object) {
        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setPreviousHash(JSONObjectHelper.safeGetString(object, "p"));
        blockHeader.setBlockNounce((int) JSONObjectHelper.safeGet(object, "n"));
        blockHeader.setBlockIndex((int) JSONObjectHelper.safeGet(object, "index"));
        blockHeader.setTimeStamp(JSONObjectHelper.safeGetString(object, "timeStamp"));
        blockHeader.setMiner( JSONObjectHelper.safeGetString(object, "miner"));
        blockHeader.setConsensusType( JSONObjectHelper.safeGetString(object, "consensusType"));
        blockHeader.setNodeId( JSONObjectHelper.safeGetString(object, "nodeId"));
        return blockHeader;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(String previousHash) {
        this.previousHash = previousHash;
    }

    public int getBlockNounce() {
        return blockNounce;
    }

    public void setBlockNounce(int blockNounce) {
        this.blockNounce = blockNounce;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public void setBlockIndex(int blockIndex) {
        this.blockIndex = blockIndex;
    }

    public int getPreviousBlockNumber() {
        return 0;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public boolean isEqual(BlockHeader another) {
        if (!previousHash.equals(another.previousHash)) return false;
        if (blockIndex != another.blockIndex) return false;
        if (blockNounce != another.blockNounce) return false;
        if (!miner.equals(another.miner)) return false;
        if (!nodeId.equals(another.nodeId)) return false;
        return true;
    }

    public String getMiner() {
        return miner;
    }

    public void setMiner(String miner) {
        this.miner = miner;
    }

    public String getConsensusType() {
        return consensusType;
    }

    @NotNull
    public String getCurrentDataString() {
        return TimeHelper.getCurrentDataStringByDot();
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public void setConsensusType(String consensusType) {
        this.consensusType = consensusType;
    }
}

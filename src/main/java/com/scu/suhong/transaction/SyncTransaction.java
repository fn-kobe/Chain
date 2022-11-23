package com.scu.suhong.transaction;

import account.AccountManager;
import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.dynamic_definition.TransactionHelper;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import util.JSONObjectHelper;

import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class SyncTransaction implements AbstractTransaction {
    private String smartContractName = "";
    private String varietyName = "";
    private String value = "";
    private String owner = "";
    private int gas;
    private int id;
    private int blockIndex;
    private String hash = "";
    private Long miningTime = 0l;
    private int txIndex = 0;

    public SyncTransaction() {
    }

    public SyncTransaction(String smartContractName, String varietyName, String value) {
        this.smartContractName = smartContractName;
        this.varietyName = varietyName;
        this.value = value;
        id = generateRandomID();
    }

    @Override
    public void postAction() {
        System.out.println("[SyncTransaction][Info] No post action");
        // no post action for sync, the blockchain internal logical handle it
    }

    @Override
    public String getBlockchainId() {
        String address = getOwner();
        String bcId = AccountManager.getBlockchainIDFromAddress(address);

        if (bcId.isEmpty()) {
            System.out.println("[Transaction][WARN] Cannot get blockchain id from address " + address);
        }
        return bcId;
    }

    @Override
    public String getOwnerWithoutChainId(){
        return AccountManager.getShortAddress(getOwner());
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int newID) {
        id = newID;
    }

    public void setId() {
        while (-1 == id) {
            id = generateRandomID();
        }
    }

    int generateRandomID() {
        final int minRandom = 1;
        final int maxRandom = Integer.MAX_VALUE;
        Random r = new Random();
        return r.nextInt((maxRandom - minRandom) + 1) + minRandom;
    }

    @Override
    public int getBlockIndex() {
        return blockIndex;
    }

    @Override
    public int getTxIndex() {
        return txIndex;
    }

    @Override
    public int getUnifiedIndex() {
        return 100*blockIndex + txIndex;
    }

    @Override
    public void setBlockIndex(int blockId) {
        this.blockIndex = blockId;
    }

    @Override
    public void setTxIndex(int txIndex) {
        this.txIndex = txIndex;
    }

    @Override
    public boolean check() {
        System.out.println("[SyncTransaction][Info] use default check function");
        return true;
    }

    @Override
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("SyncTransaction", "identifier");// as the identifier used in createfromjson method
        json.put("id", getId());
        json.put("gas", getGas());
        json.put("owner", getOwner());
        json.put("smartContractName", smartContractName);
        json.put("varietyName", varietyName);
        json.put("value", value);
        json.put("h", getHash());
        json.put("blockIndex", blockIndex);
        return json;
    }

    public static SyncTransaction createFromJson(String msg) {
        return createFromJson(new JSONObject(msg));
    }

    public static SyncTransaction createFromJson(JSONObject object) {
        if (!object.has("SyncTransaction")) { // One transaction must have Data
            return null;
        }
        SyncTransaction t = new SyncTransaction();
        t.setId(JSONObjectHelper.safeGetInt(object, "id"));
        t.setGas(JSONObjectHelper.safeGetInt(object, "gas"));
        t.setOwner(JSONObjectHelper.safeGetString(object, "owner"));
        t.setSmartContractName(JSONObjectHelper.safeGetString(object, "smartContractName"));
        t.setVarietyName(JSONObjectHelper.safeGetString(object, "varietyName"));
        t.setValue(JSONObjectHelper.safeGetString(object, "value"));
        t.setHash(JSONObjectHelper.safeGetString(object, "h"));
        t.setBlockIndex(JSONObjectHelper.safeGetInt(object, "blockIndex"));
        t.setMiningTime(JSONObjectHelper.safeGetLong(object, "miningTime"));
        return t;
    }

    @Override
    public boolean doesMarkedAsDisappear() {
        return TransactionHelper.doesMarkedAsDisappear(this);
    }

    @Override
    public boolean isExternalTransaction() {
        return false;
    }

    @Override
    public String getData() {
        return "";
    }

    @Override
    public void setData(String data) {
        System.out.println("[SyncTransaction][Info] Data field is not used in the sync transaction");
    }

    @Override
    public String getIndication() {
        return "SyncTransaction without check and post package action";
    }

    @Override
    public String getHash() {
        if (hash == null || hash.isEmpty()) {
            hash = calculateDataHash();
        }
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public void setHash() {
        this.hash = calculateDataHash();
    }

    // blockID is not as the hash as it will change
    public String calculateDataHash() {
        if (-1 == getId()) {
            System.out.println("[SyncTransaction][ERROR] Transaction id is not set before setting hash");
            return "";
        }

        String h = "";
        try {
            h = MD5Hash.getValue("SyncTransaction" + h);
            h = MD5Hash.getValue(getId() + h);
            if (null != getOwner()) {
                h = MD5Hash.getValue(getOwner() + h);
            }
            if (null != getSmartContractName()) {
                h = MD5Hash.getValue(getSmartContractName() + h);
            }
            if (null != getVarietyName()) {
                h = MD5Hash.getValue(getVarietyName() + h);
            }
            if (null != getValue()) {
                h = MD5Hash.getValue(getValue() + h);
            }
            h = MD5Hash.getValue(String.valueOf(getGas() + h));
            return h;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Cannot find MD5 algorithm in makeHash");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isSimilar(AbstractTransaction anotherTransaction) {
        SyncTransaction at = (SyncTransaction) anotherTransaction;
        if (!getSmartContractName().equals(at.getSmartContractName())) return false;
        if (!getVarietyName().equals(at.getVarietyName())) return false;
        if (getValue().equals(at.getValue())) return false;
        if (!getOwner().equals(at.getOwner())) return false;
        if (getGas() != at.getGas()) return false;
        if (getBlockIndex() != at.getBlockIndex()) return false;

        return true;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String Dump() {
        return Dump("");
    }

    public String Dump(String additionalMsg) {
        return getJson().toString();
    }

    @Override
    public String checkFailMessage() {
        return "Fail in SynTransaction";
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public int getGas() {
        return gas;
    }

    @Override
    public void setGas(int gas) {
        this.gas = gas;
    }

    @Override
    // 0 is to define an asset, 1 is to define and init an asset, 2 is to init an asset
    public int getLifecyleType() {
        return 2;
    }

    @Override
    // 0 is to define an asset, 1 is to define and init an asset, 2 is to init an asset
    public void setLifecyleType(int lifecyleType) {
        System.out.println("[SyncTransaction][WARN] Only init is supported currently");
    }

    @Override
    public boolean isOnlyAssetDefinition() {
        return false;
    }

    @Override
    public boolean isOnlyAssetInitiation() {
        return true;
    }

    @Override
    public Long getMiningTime() {
        return miningTime;
    }

    @Override
    public void setMiningTime(Long miningTime) {
        this.miningTime = miningTime;
    }

    @Override
    public boolean isInternalTx() {
        return false;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (!(o instanceof SyncTransaction)) {
            return 1;
        }
        SyncTransaction otherTransaction = (SyncTransaction) o;
        return this.getHash().compareTo(otherTransaction.getHash());
    }

    public String getSmartContractName() {
        return smartContractName;
    }

    public void setSmartContractName(String smartContractName) {
        this.smartContractName = smartContractName;
    }

    public String getVarietyName() {
        return varietyName;
    }

    public void setVarietyName(String varietyName) {
        this.varietyName = varietyName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

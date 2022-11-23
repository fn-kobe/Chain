package com.scu.suhong.dynamic_definition;

import account.AccountManager;
import com.scu.suhong.Hash.MD5Hash;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import util.JSONObjectHelper;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

public class DynamicalAsset implements AbstractTransaction {
    private String specifiedDerivedClassName = "DynamicalAsset";
    private String code = "";
    private String data = "";
    private String owner = "";
    private int gas;
    private HashMap<String, String> keyValueMembers;
    private int id;
    private int blockIndex;
    private String hash="";
    // 0 is to define an asset, 1 is to define and init an asset, 2 is to init an asset
    private int lifecyleType = 0;
    private Long miningTime = 0l;
    private int txIndex = 0;

    public DynamicalAsset(){
        keyValueMembers = new HashMap<>();
    }

    public static boolean check(DynamicalAsset transaction) {
        if (transaction.isOnlyAssetDefinition()) return true;
        if (!transaction.check()){
            System.out.println("[DynamicalAsset][ERROR] Transaction check failed");
            return false;
        }
        return true;
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
    public void postAction(){
        System.out.println("[DynamicalAsset][Info] Default post action");
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSpecifiedDerivedClassName() {
        return specifiedDerivedClassName;
    }

    public void setSpecifiedDerivedClassName(String specifiedDerivedClassName) {
        this.specifiedDerivedClassName = specifiedDerivedClassName;
    }

    public String addKeyValue(String key, String value){
        if (!keyValueMembers.containsKey(key)){
            keyValueMembers.put(key, value);
            return "";
        }
        return keyValueMembers.replace(key, value);
    }

    public String getValueByKey(String key){
        if (!keyValueMembers.containsKey(key)){
            return "";
        }
        return keyValueMembers.get(key);
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int newID){
        id = newID;
    }

    public void setId() {
        while (-1 == id) {
            id = generateRandomID();
        }
    }

    final int minRandom = 1;
    final int maxRandom = Integer.MAX_VALUE;
    int generateRandomID() {
        Random r = new Random();
        return r.nextInt((maxRandom - minRandom) + 1) + minRandom;
    }

    @Override
    public boolean check() {
        System.out.println("[DynamicalAsset][Info] use default check function");;
        return true;
    }

    public void copy(DynamicalAsset another){
        setId(another.getId());
        setGas(another.getGas());
        setOwner(another.getOwner());
        setSpecifiedDerivedClassName(another.getSpecifiedDerivedClassName());
        setCode(another.getCode());
        setData(another.getData());
        this.keyValueMembers = another.keyValueMembers;
        setHash(another.getHash());
        setBlockIndex(another.blockIndex);
        setMiningTime(another.getMiningTime());
        setLifecyleType(another.getLifecyleType());
    }

    @Override
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("DynamicalAsset", "identifier");// as the identifier used in createfromjson method
        json.put("id", getId());
        json.put("gas", getGas());
        json.put("owner", getOwner());
        json.put("className", specifiedDerivedClassName);
        json.put("code", code);
        json.put("data", data);
        json.put("kv", new JSONObject(keyValueMembers));
        json.put("h", getHash());
        json.put("blockIndex", String.valueOf(getBlockIndex()));
        json.put("lifecyleType", String.valueOf(getLifecyleType()));
        return json;
    }


    public static DynamicalAsset createFromJson(JSONObject object) {
        if (!object.has("DynamicalAsset")) { // One transaction must have Data
            return null;
        }
        DynamicalAsset dynamicalAsset = new DynamicalAsset();
        dynamicalAsset.setId(JSONObjectHelper.safeGetInt(object, "id"));
        dynamicalAsset.setGas(JSONObjectHelper.safeGetInt(object, "gas"));
        dynamicalAsset.setOwner(JSONObjectHelper.safeGetString(object, "owner"));
        dynamicalAsset.setSpecifiedDerivedClassName(JSONObjectHelper.safeGetString(object, "className"));
        dynamicalAsset.setCode(JSONObjectHelper.safeGetString(object, "code"));
        dynamicalAsset.setData(JSONObjectHelper.safeGetString(object, "data"));
        dynamicalAsset.setKv((JSONObject) object.get("kv"));
        dynamicalAsset.setHash(JSONObjectHelper.safeGetString(object, "h"));
        dynamicalAsset.setBlockIndex(JSONObjectHelper.safeGetInt(object, "blockIndex"));
        dynamicalAsset.setMiningTime(JSONObjectHelper.safeGetLong(object, "miningTime"));
        dynamicalAsset.setLifecyleType(JSONObjectHelper.safeGetInt(object, "lifecyleType"));
        return dynamicalAsset;
    }

    public void setKv(JSONObject object){
        keyValueMembers = new HashMap<>();
        Iterator<String> keysItr = object.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            String value = (String) object.get(key);
            keyValueMembers.put(key, value);
        }
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
        return data;
    }

    @Override
    public void setData(String data) {
        this.data = data;
    }

    @Override
    public String getIndication() {
        return "DynamicalAsset without check and post package action";
    }

    @Override
    public String getHash() {
        if (hash == null || hash.isEmpty()) {
            hash = calculateDataHash();
        }
        return hash;
    }

    @Override
    public void setHash() {
        this.hash = calculateDataHash();
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    // blockID is not as the hash as it will change
    public String calculateDataHash() {
        if (-1 == getId()){
            System.out.println("[DynamicalAsset][ERROR] Transaction id is not set before setting hash");
            return "";
        }

        String h = "";
        try {
            h = MD5Hash.getValue("DynamicalAsset" + h);
            if (null != getData()) {
                h = MD5Hash.getValue(getData() + h);
            }
            h = MD5Hash.getValue(getId() + h);
            if (null != getOwner()) {
                h = MD5Hash.getValue(getOwner() + h);
            }
            if (null != getCode()) {
                h = MD5Hash.getValue(getCode() + h);
            }
            Iterator it = keyValueMembers.entrySet().iterator();
            while (it.hasNext()){
                Map.Entry<String, String> keyValue = (Map.Entry<String, String>) it.next();
                h = MD5Hash.getValue(keyValue.getKey() + keyValue.getValue() + h);
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
        DynamicalAsset at = (DynamicalAsset) anotherTransaction;
        if (!getCode().equals(at.getCode())) return false;
        if (!getData().equals(at.getData())) return false;
        if (!getOwner().equals(at.getOwner())) return false;
        if (getGas() != at.getGas()) return false;
        if (getBlockIndex() != at.getBlockIndex()) return false;
        if (keyValueMembers.equals(at.keyValueMembers)) return false;

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

    @Override
    public String checkFailMessage() {
        return "Please check validation in sub class or gas.";
    }

    public String Dump(String additionalMsg) {
        return getJson().toString();
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (!(o instanceof DynamicalAsset)) {
            return 1;
        }
        DynamicalAsset otherTransaction = (DynamicalAsset) o;
        return this.getHash().compareTo(otherTransaction.getHash());
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
        return blockIndex*100 + txIndex;
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
    public int getLifecyleType() {
        return lifecyleType;
    }

    // 0 is to define an asset, 1 is to define and init an asset, 2 is to init an asset
    @Override
    public void setLifecyleType(int lifecyleType) {
        this.lifecyleType = lifecyleType;
    }

    @Override
    public boolean isOnlyAssetDefinition() {
        return 0 == lifecyleType;
    }

    @Override
    public boolean isOnlyAssetInitiation() {
        return 2 == lifecyleType;
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
}

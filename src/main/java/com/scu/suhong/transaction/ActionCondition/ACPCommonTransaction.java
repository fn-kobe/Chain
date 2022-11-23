package com.scu.suhong.transaction.ActionCondition;

import account.AccountManager;
import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.dynamic_definition.TransactionHelper;
import com.scu.suhong.transaction.CrosschainInterface;
import consensus.pow.MiningConfiguration;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import util.JSONObjectHelper;

import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class ACPCommonTransaction implements AbstractTransaction {
    final int minRandom = 1;
    final int maxRandom = Integer.MAX_VALUE;
    protected String specifiedDerivedClassName = "ActionCommonTransaction";
    protected String data = "";
    protected String owner = "";
    protected int gas;
    protected int id = -1;
    protected int blockIndex;
    protected String blockchainID = "";
    protected String hash = "";
    private Long miningTime = 0l;
    private int txIndex = 0;

    public ACPCommonTransaction() {
    }

    public static boolean check(ACPCommonTransaction transaction) {
        return true;
    }

    public static boolean initFromJson(JSONObject object, ACPCommonTransaction t) {
        if (!object.has("ActionCommonTransaction")) { // One transaction must have Data
            return false;
        }

        t.setId(JSONObjectHelper.safeGetInt(object, "id"));
        t.setGas(JSONObjectHelper.safeGetInt(object, "gas"));
        t.setOwner(JSONObjectHelper.safeGetString(object, "owner"));
        t.setSpecifiedDerivedClassName(JSONObjectHelper.safeGetString(object, "className"));
        t.setData(JSONObjectHelper.safeGetString(object, "data"));
        t.setHash(JSONObjectHelper.safeGetString(object, "h"));
        t.setLifecyleType(JSONObjectHelper.safeGetInt(object, "lifecyleType"));
        t.setBlockIndex(JSONObjectHelper.safeGetInt(object, "blockIndex"));
        t.setMiningTime(JSONObjectHelper.safeGetLong(object, "miningTime"));
        t.setBlockchainID(JSONObjectHelper.safeGetString(object, "blockchainID"));

        return true;
    }

    @Override
    public void postAction() {
        System.out.println("[ActionCommonTransaction][Info] Default post action");
    }

    public String getSpecifiedDerivedClassName() {
        return specifiedDerivedClassName;
    }

    public void setSpecifiedDerivedClassName(String specifiedDerivedClassName) {
        this.specifiedDerivedClassName = specifiedDerivedClassName;
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
        if (blockchainID.isEmpty()){
            blockchainID = MiningConfiguration.getBlockchainStringId();
        }
    }

    int generateRandomID() {
        Random r = new Random();
        return r.nextInt((maxRandom - minRandom) + 1) + minRandom;
    }

    @Override
    public boolean check() {
        System.out.println("[ActionCommonTransaction][Info] use default check function");
        ;
        return true;
    }

    static public void copy(ACPCommonTransaction to, ACPCommonTransaction from) {
        to.setId(from.getId());
        to.setGas(from.getGas());
        to.setOwner(from.getOwner());
        to.setSpecifiedDerivedClassName(from.getSpecifiedDerivedClassName());
        to.setData(from.getData());
        to.setHash(from.getHash());
        to.setBlockIndex(from.getBlockIndex());
        to.setMiningTime(from.getMiningTime());
        to.setBlockchainID(from.getBlockchainId());
    }

    @Override
    public JSONObject getJson() {
        return getJson(this);
    }

    static public JSONObject getJson(ACPCommonTransaction t) {
        JSONObject json = new JSONObject();
        json.put("ActionCommonTransaction", "ActionCommonTransaction");// as the identifier used in createfromjson method
        json.put("id", t.getId());
        json.put("gas", t.getGas());
        json.put("owner", t.getOwner());
        json.put("className", t.getSpecifiedDerivedClassName());
        json.put("data", t.getData());
        json.put("h", t.getHash());
        json.put("blockIndex", String.valueOf(t.getBlockIndex()));
        json.put("blockchainID", t.getBlockchainId());
        return json;
    }

    @Override
    public boolean doesMarkedAsDisappear() {
        return TransactionHelper.doesMarkedAsDisappear(this);
    }

    // Used as external as to send cross chain
    @Override
    public boolean isExternalTransaction(){
        String address = getOwner();
        return address.contains(AccountManager.getAddressConnectSymbol())
                && !address.startsWith(MiningConfiguration.getBlockchainId() + AccountManager.getAddressConnectSymbol());
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
        return "[ActionCommonTransaction] ActionCommonTransaction without check and post package action";
    }

    @Override
    public String getHash() {
        if (hash == null || hash.isEmpty()) {
            hash = calculateDataHash(this);
        }
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public void setHash() {
        this.hash = calculateDataHash(this);
    }

    // block ID is not as the hash as it will change
    public String calculateDataHash(ACPCommonTransaction ACPCommonTransaction) {
        if (-1 == ACPCommonTransaction.getId()) {
            System.out.println("[ActionCommonTransaction][ERROR] Transaction id is not set before setting hash");
            return "";
        }

        String h = "";
        try {
            h = MD5Hash.getValue("ActionCommonTransaction" + h);
            if (null != ACPCommonTransaction.getData()) {
                h = MD5Hash.getValue(ACPCommonTransaction.getData() + h);
            }
            h = MD5Hash.getValue(ACPCommonTransaction.getId() + h);
            if (null != ACPCommonTransaction.getOwner()) {
                h = MD5Hash.getValue(ACPCommonTransaction.getOwner() + h);
            }
            h = MD5Hash.getValue(String.valueOf(ACPCommonTransaction.getGas()) + h);
            h = MD5Hash.getValue(ACPCommonTransaction.getBlockchainId() + h);
            return h;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Cannot find MD5 algorithm in makeHash");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isSimilar(AbstractTransaction another) {

        ACPCommonTransaction at = (ACPCommonTransaction) another;
        if (null == at) return false;
        if (!getData().equals(at.getData())) return false;
        if (!getOwner().equals(at.getOwner())) return false;
        if (getGas() != at.getGas()) return false;
        if (getBlockIndex() != at.getBlockIndex()) return false;
        if (getBlockchainId() != at.getBlockchainId()) return false;

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
        if (!(o instanceof ACPCommonTransaction)) {
            return 1;
        }
        ACPCommonTransaction otherTransaction = (ACPCommonTransaction) o;
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
    public String getOwner() {
        return owner;
    }

    @Override
    public String getOwnerWithoutChainId(){
        return AccountManager.getShortAddress(owner);
    }

    @Override
    public void setOwner(String owner) {
        if (null == owner || owner.isEmpty()){
            System.out.println("[ActionCommonTransaction][WARN] Sender address is empty and we skip setting it as the transaction owner");
            return;
        }

        this.owner = AccountManager.getFullAddress(owner);
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
        return 2;//always init
    }

    @Override
    public void setLifecyleType(int lifecyleType) {
        //always init, no need to set
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
    public String getBlockchainId() {
        if (!blockchainID.isEmpty()) return blockchainID;

        String address = getOwner();
        System.out.println("[Transaction][WARN] blockchain id is not set, try to get from owner address " + address);
        String bcId = AccountManager.getBlockchainIDFromAddress(address);

        if (bcId.isEmpty()) {
            System.out.println("[Transaction][WARN] Cannot get blockchain id from address " + address);
        }
        return bcId;
    }

    public void setBlockchainID(String blockchainID) {
        this.blockchainID = blockchainID;
    }
}

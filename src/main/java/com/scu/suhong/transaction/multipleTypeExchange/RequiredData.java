package com.scu.suhong.transaction.multipleTypeExchange;

import account.AccountManager;
import org.json.JSONObject;
import util.JSONObjectHelper;

public class RequiredData {
    String from;
    String to;
    String assetType;
    int value;
    String data;

    String chainID;
    int exchangeId;

    public RequiredData() {
    }

    public RequiredData(int exchangeId, String chainID, String from, String to, String assetType, int value) {
        initData(exchangeId, chainID, from, to, assetType, value, "");
    }
    public RequiredData(int exchangeId, String chainID, String from, String to, String assetType, int value, String data) {
        initData(exchangeId, chainID, from, to, assetType, value, data);
    }

    void initData(int exchangeId, String chainID, String from, String to, String assetType, int value, String date) {
        this.from = from;
        this.to = to;
        this.assetType = assetType;
        this.value = value;
        this.chainID = chainID;
        this.exchangeId = exchangeId;
        this.data = date;
    }

    public boolean doesMatch(RequiredData requiredData){
        return doesMatch(this, requiredData);
    }

    static public boolean doesMatch(RequiredData requirement, RequiredData receivedTransactionData){
        if (! (requirement.getExchangeId() == receivedTransactionData.getExchangeId())) return false;
        if (! (requirement.getChainID().equals(receivedTransactionData.getChainID()))) return false;
        // as chain id is compared, here we only compare short address
        if (!doesShortAddressMatch(requirement.getFrom(), receivedTransactionData.getFrom())) return false;
        if (!doesShortAddressMatch(requirement.getTo(), receivedTransactionData.getTo())) return false;
        if (!AccountManager.doesAssetTypeMatch(requirement.getAssetType(), receivedTransactionData.getAssetType())){
            return false;
        }
        if (! (requirement.getValue() == receivedTransactionData.getValue())) return false;
        if (!requirement.data.isEmpty()){
            if (!requirement.data.equals(receivedTransactionData.data)) return false;
        }

        return true;
    }

    static boolean doesShortAddressMatch(String address1, String address2){
        return AccountManager.getShortAddress(address1).equals(AccountManager.getShortAddress(address2));
    }

    static public RequiredData createFromJson(JSONObject o){
        RequiredData data = new RequiredData();
        data.setFrom(JSONObjectHelper.safeGetString(o, "from"));
        data.setTo(JSONObjectHelper.safeGetString(o, "to"));
        data.setAssetType(JSONObjectHelper.safeGetString(o, "assetType"));
        data.setValue(JSONObjectHelper.safeGetInt(o, "value"));
        data.setChainID(JSONObjectHelper.safeGetString(o, "chainID"));
        data.setExchangeId(JSONObjectHelper.safeGetInt(o, "exchangeId"));
        return data;
    }

    JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("from", getFrom());
        json.put("to", getTo());
        json.put("assetType", getAssetType());
        json.put("value", getValue());
        json.put("chainID", getChainID());
        json.put("exchangeId", getExchangeId());
        return json;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public int getValue() {
        return value;
    }

    public String getChainID() { return chainID; }

    public void setChainID(String chainID) {
        this.chainID = chainID;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(int exchangeId) {
        this.exchangeId = exchangeId;
    }
}

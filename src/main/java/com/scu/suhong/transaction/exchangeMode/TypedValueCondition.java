package com.scu.suhong.transaction.exchangeMode;

import org.json.JSONObject;

public class TypedValueCondition extends  ValueCondition {
    String assetType = "";

    public TypedValueCondition(double value, Condition condition) {
        super(value, condition);
    }

    public TypedValueCondition(double value, Condition condition, String assetType) {
        super(value, condition);
        this.assetType = assetType;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    @Override
    public String toString() {
        return getJson().toString();
    }

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("value", value);
        json.put("condition", condition.toString());
        json.put("assetType", assetType);
        return json;
    }

    public static TypedValueCondition fromJson(String jsonString){
        return fromJson(new JSONObject(jsonString));
    }

    public static TypedValueCondition fromJson(JSONObject json){
        TypedValueCondition valueCondition = null;
        int value = json.getInt("value");
        Condition condition = Condition.fromString(json.getString("condition"));
        String assetType = json.getString("assetType");
        if (null != condition) {
            valueCondition = new TypedValueCondition(value, condition, assetType);
        }
        return valueCondition;
    }
}

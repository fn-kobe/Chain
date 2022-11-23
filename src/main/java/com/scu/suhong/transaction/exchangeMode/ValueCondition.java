package com.scu.suhong.transaction.exchangeMode;

import org.json.JSONObject;

public class ValueCondition {
    double value;
    Condition condition;

    public ValueCondition(double value, Condition condition) {
        this.value = value;
        this.condition = condition;
    }

    public ValueCondition(String value, Condition condition) {
        try {
            this.value = Double.parseDouble(value);
        } catch (NumberFormatException e){
            e.printStackTrace();
            this.value = 0;
        }
        this.condition = condition;
    }

    public double getValue() {
        return value;
    }

    public Condition getCondition() {
        return condition;
    }

    public boolean isMatched(){
        // We only expect the true as the condition is OK
        return condition.getResult() == ConditionResult.True;
    }

    public int getLoopNumber() {
        return condition.getLoopNumber();
    }

    public void setLoopNumber(int loopNumber) {
        condition.setLoopNumber(loopNumber);
    }

    @Override
    public String toString() {
        return getJson().toString();
    }

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("value", value);
        json.put("condition", condition.toString());
        return json;
    }

    public static ValueCondition fromJson(String jsonString){
        return fromJson(new JSONObject(jsonString));
    }

    public static ValueCondition fromJson(JSONObject json){
        ValueCondition valueCondition = null;
        int value = json.getInt("value");
        Condition condition = Condition.fromString(json.getString("condition"));
        if (null != condition) {
            valueCondition = new ValueCondition(value, condition);
        }
        return valueCondition;
    }
}

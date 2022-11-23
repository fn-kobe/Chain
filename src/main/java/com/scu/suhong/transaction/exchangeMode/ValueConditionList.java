package com.scu.suhong.transaction.exchangeMode;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ValueConditionList {
    // arranged by the value
    List<ValueCondition> valueConditions;
    int loopNumber = 0;

    public int getLoopNumber() {
        return loopNumber;
    }

    public void setLoopNumber(int loopNumber) {
        for (ValueCondition valueCondition: valueConditions) {
            valueCondition.setLoopNumber(loopNumber);
        }
        this.loopNumber = loopNumber;
    }

    public ValueConditionList() {
        valueConditions = new ArrayList<>();
    }

    // This is a simple implementation
    // We can analysis some sequence impact if two value condition is matched
    public ValueCondition getMatchedCondition(){
        for (ValueCondition valueCondition: valueConditions) {
            if (valueCondition.isMatched()){
                return valueCondition;
            }
        }
        return null;
    }

    public double getMaxValue(){
        double r = 0;
        for (ValueCondition valueCondition: valueConditions) {
           if (valueCondition.getValue() > r){
               r = valueCondition.getValue();
           }
        }
        return r;
    }

    public double getMinValue(){
        double r = Double.MAX_VALUE;
        for (ValueCondition valueCondition: valueConditions) {
            if (valueCondition.getValue() < r){
                r = valueCondition.getValue();
            }
        }
        return r;
    }

    public ValueCondition getMaxMatchedCondition(){
        ValueCondition r =  null;
        for (ValueCondition valueCondition: valueConditions) {
            if (valueCondition.isMatched()){
                if (null == r || r.getValue() < valueCondition.getValue()) r = valueCondition;
            }
        }
        return r;
    }

    public ValueCondition getMinMatchedCondition(){
        ValueCondition r =  null;
        for (ValueCondition valueCondition: valueConditions) {
            if (valueCondition.isMatched()){
                if (null == r || r.getValue() > valueCondition.getValue()) r = valueCondition;
            }
        }
        return r;
    }

    public void addValueConditionPair(ValueCondition valueCondition){
        int position = 0;
        for (int i = 0; i < valueConditions.size(); ++i){
            if (valueConditions.get(i).getValue() < valueCondition.getValue()){
                ++position;
            } else {
                break;
            }
        }
        valueConditions.add(position, valueCondition);
    }

    public List<ValueCondition> getValueConditions() {
        return valueConditions;
    }

    @Override
    public String toString(){
        return getJson().toString();
    }

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        JSONArray array = new JSONArray();
        for (ValueCondition v : valueConditions) {
            array.put(v.getJson());
        }
        json.put("array", array);
        return json;
    }

    public static ValueConditionList fromJson(String jsonString){
        return fromJson(new JSONObject(jsonString));
    }

    @Nullable
    public static ValueConditionList fromJson(JSONObject json) {
        ValueConditionList valueConditionList = new ValueConditionList();
        JSONArray array = (JSONArray)json.get("array");
        ValueCondition valueCondition = null;
        for (int i = 0; i < array.length(); i++) {
            valueCondition = ValueCondition.fromJson(array.getJSONObject(i));
            valueConditionList.addValueConditionPair(valueCondition);
        }
        return valueConditionList;
    }

}

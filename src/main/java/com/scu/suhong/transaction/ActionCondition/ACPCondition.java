package com.scu.suhong.transaction.ActionCondition;

import org.json.JSONObject;

public class ACPCondition {
    String name = "";

    // Used for random trigger
    // We inbemdded the parameter to the transaction
    // In fact, they should calculate with topology
    int totalSuccessiveActionNumber = 0;
    int maxAllowedActionNumber = 0;
    private int maxWaitingTime = 0;

    boolean isValid = true;
    final static String separator = ":";

    public ACPCondition(String name, int maxAllowedActionNumber, int totalSuccessiveActionNumber, int maxWaitingTime) {
        this.name = name;
        this.maxAllowedActionNumber = maxAllowedActionNumber;
        this.totalSuccessiveActionNumber = totalSuccessiveActionNumber;
        this.maxWaitingTime = maxWaitingTime;
    }

    public ACPCondition() {
        isValid = false;
    }

    public ACPCondition(String formatString) {
        init(this, formatString);
    }

    static public boolean init(ACPCondition acpCondition, String formatString) {
        String formatArray[] = formatString.split(separator);
        if (formatArray.length != 4){
            acpCondition.isValid =false;
            System.out.println("[ACPCondition][ERROR] It is not enough parameters to format ACPCondition with " + formatString);
            return false;
        }

        acpCondition.name = formatArray[0];
        try {
            acpCondition.maxAllowedActionNumber = Integer.parseInt(formatArray[1]);
            acpCondition.totalSuccessiveActionNumber = Integer.parseInt(formatArray[2]);
            acpCondition.maxWaitingTime = Integer.parseInt(formatArray[3]);
        } catch (NumberFormatException e){
            e.printStackTrace();
            acpCondition.isValid = false;
            return false;
        }
        acpCondition.isValid = true;
        return true;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getString() {
        return getJson().toString();
    }

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("ACPCondition", formatString());// as the identifier used in createfromjson method
        return json;
    }

    static public ACPCondition fromJson(String object) {
        return fromJson(new JSONObject(object));
    }

    static public ACPCondition fromJson(JSONObject object) {
        ACPCondition condition = new ACPCondition();
        ACPCondition.fromJson(object, condition);
        return condition;
    }

    static public boolean fromJson(JSONObject json, ACPCondition condition) {
        String formatString = json.getString("ACPCondition");// as the identifier used in createfromjson method
        if (null == formatString) {
            condition.isValid = false;
            return false;
        }

        return init(condition, formatString);
    }

    String formatString(){
        return name + separator + maxAllowedActionNumber + separator + totalSuccessiveActionNumber + separator + maxWaitingTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxAllowedActionNumber() {
        return maxAllowedActionNumber;
    }

    public int getMaxWaitingTime() {
        return maxWaitingTime;
    }

    public void setMaxAllowedActionNumber(int maxAllowedActionNumber) {
        this.maxAllowedActionNumber = maxAllowedActionNumber;
    }

    public void setTotalSuccessiveActionNumber(int totalSuccessiveActionNumber) {
        this.totalSuccessiveActionNumber = totalSuccessiveActionNumber;
    }

    public void setMaxWaitingTime(int maxWaitingTime) {
        this.maxWaitingTime = maxWaitingTime;
    }

    public int getTotalSuccessiveActionNumber() {
        return totalSuccessiveActionNumber;
    }

    public boolean isTheSame(ACPCondition condition) {
        if (!name.equals(condition.getName())) return false;
        if (maxAllowedActionNumber != condition.getMaxAllowedActionNumber()) return false;
        if (totalSuccessiveActionNumber != condition.getTotalSuccessiveActionNumber()) return false;

        return true;
    }
}

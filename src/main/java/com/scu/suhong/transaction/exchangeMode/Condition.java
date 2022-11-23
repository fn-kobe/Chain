package com.scu.suhong.transaction.exchangeMode;

import com.scu.suhong.ExpressDelivery.InformationQuery;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

// TO DO: json
public class Condition {
    // Now we only use ConditionResult.True as the only result
    // If wants to use ConditionResult.False, add a new condition which contains the false condition string
    ConditionResult result = ConditionResult.Undefined;

    // Lower case of the expect result
    String expectResultString = "";
    String oppositeResultString = "";
    // conditionContract is a runnable program
    String conditionContract = "";
    int loopNumber = 0;

    public ConditionResult getResult() {
        if (!conditionContract.isEmpty()) {
            updateConditionState();
        }
        return result;
    }

    public void setResult(ConditionResult result) {
        this.result = result;
    }

    public int getLoopNumber() {
        return loopNumber;
    }

    public void setLoopNumber(int loopNumber) {
        this.loopNumber = loopNumber;
    }

    public void setExpectResultString(String expectResultString) {
        this.expectResultString = expectResultString.toLowerCase();
    }

    @Override
    public String toString() {
        return getJson().toString();
    }

    public Condition() {
        this.result = ConditionResult.Undefined;
    }

    // loopNumber is -1 for unlimited loop
    public Condition(ConditionResult result, int loopNumber, String contract, String expectResultString, String oppositeResultString) {
        this.result = result;
        this.conditionContract = contract;
        this.expectResultString = expectResultString;
        this.oppositeResultString = oppositeResultString;
        this.loopNumber = loopNumber;
    }

    static public Condition fromString(String objectString) {
        JSONObject object = new JSONObject(objectString);
        return createFromJson(object);
    }

    static public Condition createFromJson(JSONObject object){
        try {
            return new Condition(ConditionResult.valueOf(object.getString("result")),
                    object.getInt("loopNumber"), object.getString("contract"),
                    object.getString("expectResultString"), object.getString("oppositeResultString"));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("result", result.name());
        json.put("expectResultString", expectResultString);
        json.put("oppositeResultString", oppositeResultString);
        json.put("contract", conditionContract);
        json.put("loopNumber", loopNumber);
        return json;
    }

    public String getConditionContract() {
        return conditionContract;
    }

    public void setConditionContract(String conditionContract) {
        this.conditionContract = conditionContract;
    }

    public void updateConditionState() {
        if (conditionContract.isEmpty()) return;
        if (expectResultString.isEmpty()) {
            System.out.println("[Condition][ERROR] No expect result for the command.");
            return;
        }
        if (oppositeResultString.isEmpty()) {
            System.out.println("[Condition][ERROR] No opposite result for the command.");
            return;
        }

        if (conditionContract.startsWith("http")) {
            processHttpRequest();
        } else {
            processCommand();
        }
    }

    public void processCommand() {
        try {
            String std = runCommandAndGetOutput().get("std").toString().toLowerCase();
            System.out.println("[Condition][Debug] command output is: " + std);
            String stdError = runCommandAndGetOutput().get("error").toString().toLowerCase();
            if (!stdError.isEmpty()) {
                System.out.println("[Condition][Error] command  error output is: " + stdError);
            }
            if (std.contains(oppositeResultString)) result = ConditionResult.False;
            else if (std.contains(expectResultString)) result = ConditionResult.True;
            else result = ConditionResult.Ongoing;
        } catch (IOException e) {
            result = ConditionResult.Undefined;
        }
    }

    public void processHttpRequest() {
        // begin index "http '
        String parameter = conditionContract.substring(5);
        String[] parameters = parameter.split(":");
        HashMap<String, String> parameterMap = new HashMap();
        for (int i = 1; i < parameters.length; ++i, ++i) {
            parameterMap.put(parameters[i - 1].trim(), parameters[i].trim());
        }
        if (parameterMap.isEmpty()) {
            result = ConditionResult.Undefined;
            return;
        }

        if (InformationQuery.doesContainFeatureString("", parameterMap, expectResultString)) {
            result = ConditionResult.True;
        }else if(InformationQuery.doesContainFeatureString("", parameterMap, oppositeResultString)){
            result = ConditionResult.False;
        }
        else {
            result = ConditionResult.Ongoing;
        }
    }

    public boolean isMatch(Condition another) {
        if (!expectResultString.equals(another.expectResultString)) {
            System.out.printf("%s doesn't match %s in expectResultString\n", expectResultString, another.expectResultString);
            return false;
        }
        if (!conditionContract.equals(another.conditionContract)) {
            System.out.printf("%s doesn't match %s in conditionContract\n", conditionContract, another.conditionContract);
            return false;
        }
        if (!oppositeResultString.equals(another.oppositeResultString)) {
            System.out.printf("%s doesn't match %s in oppositeResultString\n", oppositeResultString, another.oppositeResultString);
            return false;
        }
        //loopNumber and result is not take as match parameter, as it is the repeat count

        return true;
    }

    public JSONObject runCommandAndGetOutput() throws IOException {
        Runtime rt = Runtime.getRuntime();
        String[] commands = (conditionContract + " " + loopNumber).split(" ");
        Process process = rt.exec(commands);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        String oneOutput = "";
        String oneLine;
        while ((oneLine = stdInput.readLine()) != null) {
            oneOutput += oneLine;
        }
        JSONObject r = new JSONObject();
        r.put("std", oneOutput);

        oneOutput = "";
        while ((oneLine = stdError.readLine()) != null) {
            oneOutput += oneLine;
        }
        r.put("error", oneOutput);
        return r;
    }

    public void setOppositeResultString(String oppositeResultString) {
        this.oppositeResultString = oppositeResultString;
    }
}

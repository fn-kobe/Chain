package com.scu.suhong.transaction.exchangeMode;

import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;

public class ConditionTest {

    @Test
    public void conditionStringTest() {
        Condition condition = new Condition(ConditionResult.False, 1,  "contract", "true", "flase");
        Condition conditionNew = Condition.fromString(condition.getJson().toString());
        assert conditionNew.getJson().toString().equals(condition.toString());
    }

    @Test
    public void runCommandAndGetOutput() throws IOException {
        Condition condition = new Condition();
        condition.setLoopNumber(1);
        condition.setConditionContract(".\\test\\conditionContract\\echoback  true");
        JSONObject r = condition.runCommandAndGetOutput();
        System.out.println(r.toString());
    }

    @Test
    public void runOutputCommandAndGetOutput() throws IOException {
        Condition condition = new Condition();
        condition.setLoopNumber(1);
        condition.setConditionContract(".\\test\\conditionContract\\outputFile");
        JSONObject r = condition.runCommandAndGetOutput();
        System.out.println(r.toString());
    }

    @Test
    public void processHttpRequest() {
        Condition condition = new Condition();
        condition.setLoopNumber(1);
        condition.setConditionContract("http com:ems:nu:1072864318531");

        condition.setExpectResultString("\"state\":\"2\"");
        condition.setOppositeResultString("\"state\":\"notexits\"");
        ConditionResult result = condition.getResult();
        System.out.println(result);
        assert result.equals(ConditionResult.Ongoing);

        condition.setExpectResultString("\"state\":\"0\"");
        result = condition.getResult();
        System.out.println(result);
        assert result.equals(ConditionResult.True);
    }

    @Test
    public void updateConditionState() {
        Condition condition = new Condition();
        condition.setExpectResultString("true");
        condition.setOppositeResultString("false");
        assert ConditionResult.Undefined == condition.getResult();
        condition.setConditionContract(".\\test\\conditionContract\\echoback  ongoing");
        condition.updateConditionState();
        assert ConditionResult.Ongoing == condition.getResult();

        condition.setConditionContract(".\\test\\conditionContract\\echoback  true");
        condition.updateConditionState();
        assert ConditionResult.True == condition.getResult();

        condition.setConditionContract(".\\test\\conditionContract\\notexist  False");
        condition.updateConditionState();
        assert ConditionResult.Undefined == condition.getResult();

        condition.setConditionContract(".\\test\\conditionContract\\echoback");
        condition.updateConditionState();
        assert ConditionResult.Ongoing == condition.getResult();
    }

}
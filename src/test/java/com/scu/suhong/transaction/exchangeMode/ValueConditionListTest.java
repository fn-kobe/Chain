package com.scu.suhong.transaction.exchangeMode;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class ValueConditionListTest {

    @Test
    public void getMatchedCondition() {
        ValueConditionList valueConditionList = new ValueConditionList();
        Condition condition = createCondition(ConditionResult.True);
        ValueCondition valueCondition = new ValueCondition(3, condition);
        valueConditionList.addValueConditionPair(valueCondition);
        assert null != valueConditionList.getMatchedCondition();

        valueConditionList = new ValueConditionList();
        condition = createCondition(ConditionResult.False);
        valueCondition = new ValueCondition(0, condition);
        valueConditionList.addValueConditionPair(valueCondition);

        condition = createCondition(ConditionResult.True);
        valueCondition = new ValueCondition(1, condition);
        valueConditionList.addValueConditionPair(valueCondition);
        assert 1 == valueConditionList.getMatchedCondition().getValue();
    }

    @NotNull
    private Condition createCondition(ConditionResult result) {
        Condition condition = new Condition();
        condition.setResult(result);
        return condition;
    }

    @Test
    public void fromAndToJson() {
        ValueConditionList valueConditionList = new ValueConditionList();
        Condition condition = createCondition(ConditionResult.True);
        ValueCondition valueCondition = new ValueCondition(3, condition);
        valueConditionList.addValueConditionPair(valueCondition);

        valueConditionList = new ValueConditionList();
        condition = createCondition(ConditionResult.False);
        valueCondition = new ValueCondition(0, condition);
        valueConditionList.addValueConditionPair(valueCondition);

        String jsonString = valueConditionList.toString();
        System.out.println(jsonString);
        ValueConditionList valueConditionListJson = ValueConditionList.fromJson(jsonString);
        assert null != valueConditionListJson;
    }

    @Test
    public void addValueConditionPair() {
        ValueConditionList valueConditionListOne = new ValueConditionList();
        Condition condition = createCondition(ConditionResult.True);
        ValueCondition valueCondition1 = new ValueCondition(3, condition);

        condition = createCondition(ConditionResult.False);
        ValueCondition valueCondition2 = new ValueCondition(0, condition);

        condition = createCondition(ConditionResult.True);
        ValueCondition valueCondition3 = new ValueCondition(1, condition);

        valueConditionListOne.addValueConditionPair(valueCondition1);
        valueConditionListOne.addValueConditionPair(valueCondition2);
        valueConditionListOne.addValueConditionPair(valueCondition3);

        ValueConditionList valueConditionListTwo = new ValueConditionList();
        valueConditionListTwo.addValueConditionPair(valueCondition3);
        valueConditionListTwo.addValueConditionPair(valueCondition1);
        valueConditionListTwo.addValueConditionPair(valueCondition2);

        assert valueConditionListOne.getJson().toString().equals(valueConditionListTwo.getJson().toString());
    }
}
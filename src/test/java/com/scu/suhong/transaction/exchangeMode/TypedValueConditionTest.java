package com.scu.suhong.transaction.exchangeMode;

import org.junit.Test;

public class TypedValueConditionTest {

    @Test
    public void toStringTest() {
        TypedValueCondition valueCondition = new TypedValueCondition(10, new Condition(), "");
        String jsonString = valueCondition.toString();
        System.out.println(jsonString);

        TypedValueCondition valueConditionFromJson = TypedValueCondition.fromJson(jsonString);
        assert valueConditionFromJson.getAssetType().equals("");
    }
}
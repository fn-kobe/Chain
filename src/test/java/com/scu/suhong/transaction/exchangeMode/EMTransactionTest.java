package com.scu.suhong.transaction.exchangeMode;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class EMTransactionTest {
    @Test
    public void testJson() {
        EMTransaction transaction = new EMTransaction();
        transaction.addIncomingValueConditionPair(createValueCondition());
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.False, 12));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.False, 21));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.True, 22));
        String jsonString = transaction.getJson().toString();

        EMTransaction transactionJson = EMTransaction.fromJson(jsonString);
        assert null != transactionJson;
        assert transactionJson.getJson().toString().equals(transaction.getJson().toString());
    }

    @NotNull
    private ValueCondition createValueCondition() {
        return createValueCondition(ConditionResult.True, 3);
    }

    @NotNull
    private ValueCondition createValueCondition(ConditionResult result, int value) {
        Condition condition = createCondition(result);
        ValueCondition valueCondition = new ValueCondition(value, condition);
        return valueCondition;
    }

    private Condition createCondition(ConditionResult result) {
        Condition condition = new Condition();
        condition.setResult(result);
        return condition;
    }
}
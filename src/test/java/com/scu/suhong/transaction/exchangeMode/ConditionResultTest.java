package com.scu.suhong.transaction.exchangeMode;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConditionResultTest {

    @Test
    public void isDone() {
        ConditionResult result = ConditionResult.False;
        assert result.isDone();
        result = ConditionResult.True;
        assert result.isDone();

        result = ConditionResult.Ongoing;
        assert !result.isDone();
        result = ConditionResult.Undefined;
        assert !result.isDone();
    }
}
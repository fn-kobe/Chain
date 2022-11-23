package com.scu.suhong.transaction.exchangeMode;

public class TimesChangeCondition extends Condition {
    int timesToBeTrue = 0;
    int calledTimes = 0;

    public TimesChangeCondition(int timesToBeTrue) {
        this.timesToBeTrue = timesToBeTrue;
    }

    public ConditionResult getResult() {
        if (timesToBeTrue <= calledTimes) {
            return ConditionResult.True;
        }
        ++calledTimes;
        return ConditionResult.False;
    }
}
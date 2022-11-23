package com.scu.suhong.transaction.exchangeMode;

public class RatioValueCondition extends ValueCondition {
    public RatioValueCondition(double ratio, Condition condition) {
        super(ratio, condition);
    }

    public double getRatio() {
        return getValue();
    }
}

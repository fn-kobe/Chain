package com.scu.suhong.transaction.exchangeMode;

public enum ConditionResult {
    Undefined("Undefined"),
    Ongoing("Ongoing"),
    True("True"),
    False("False");

    public boolean isDone(){
        return this.equals(ConditionResult.True) || this.equals(ConditionResult.False);
    }

    private String name;
    ConditionResult(String name) {
        this.name = name;
    }
}

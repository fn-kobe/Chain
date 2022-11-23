package com.scu.suhong.transaction.ActionCondition;

public interface ACPAbstractAction {
    boolean onIncomingConditionTransaction(ACPCommonTransaction ACPTokenTransaction);

    boolean onIncomingConditionTransaction(ACPTriggerTransaction conditionTransaction);

    boolean onIncomingConditionTransaction(ACPTokenTransaction ACPTokenTransaction);

    boolean onIncomingConditionTransaction(ACPSettingTransaction t);

    void checkAndDoAction();

    boolean isDone();
}

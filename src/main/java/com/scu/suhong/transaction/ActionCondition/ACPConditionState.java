package com.scu.suhong.transaction.ActionCondition;

public enum ACPConditionState {
    ENone,
    EWaiting,// waiting for condition to come,
    EWaitingToSendTT, // condition comes. tt is required while tt is not sent
    EWaitingAfterSentTT, // condition comes. tt is required and tt has been sent
    EError,
    EReject,
    ERun
}

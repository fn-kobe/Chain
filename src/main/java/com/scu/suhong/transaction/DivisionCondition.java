package com.scu.suhong.transaction;

import org.apache.log4j.Logger;
import util.FileLogger;

import java.util.ArrayList;
import java.util.List;

// message format: matchedPartition_p1_p2
public class DivisionCondition extends Transaction implements ConditionalDivisionTransactionInterface {
    static Logger logger = FileLogger.getLogger();

    static final String matchedPartitionKeyword = "matchedPartition";
    static final String matchedPartitionSeparator = "_";
    String msg = "";
    List<String> conditionList = new ArrayList<>();

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg){
        if (!isValid(msg)){
            logger.warn("[DivisionCondition] Invalid division condition " + msg);
            return;
        }

        conditionList.clear();
        this.msg = msg;
        String msgArray[] = msg.split(matchedPartitionSeparator);
        //skip the first one as it is matchedPartitionKeyword, then we start from 1
        for (int i = 1; i < msgArray.length; ++i){
            conditionList.add(msgArray[i]);
        }
    }

    public final List<String> getConditionList() {
        return conditionList;
    }

    public static DivisionCondition construct(String msg){
        if (!isValid(msg)) return null;
        DivisionCondition conditionTransaction = new DivisionCondition();
        conditionTransaction.setMsg(msg);
        return conditionTransaction;
    }

    DivisionCondition() {
    }

    static boolean isValid(String msg){
        if (null == msg) return false;

        return msg.contains(matchedPartitionKeyword);
    }
}

package com.scu.suhong.transaction;

import account.AccountManager;
import org.apache.log4j.Logger;
import util.FileLogger;

import java.util.*;

// Used to pay to the receiver part by part, like payment by installment
// Like one customer get the goods and pay 30%, then after one month it pays 30%, and two month pay the left
// format: divisionIf_p1_25_p2_30 -> if p1 25% valid, if p2 30% valid
// format: divisionIf_p1+p2_25_p2_30 -> if p1+p2 25% valid, if p2  another 30% valid
public class ConditionalDivisionTransaction extends Transaction implements ConditionalDivisionTransactionInterface {
    static Logger logger = FileLogger.getLogger();

    static final String conditionKeyword = "divisionIf";
    static final String conditionSeparator = "_";
    static final String conditionInternalSeparator = "\\+"; // p1+p2
    String conditionString = "";

    // p1, p2 -> 25

    List<DivisionConditionPair> divisionConditionPairList = new ArrayList<>();

    List<String> receivedConditionList = new ArrayList<>();
    private ConditionalDivisionTransaction() {
    }

    public boolean processCondition(DivisionCondition condition) {
        receivedConditionList.addAll(condition.getConditionList());

        boolean isProcessed = false;
        for (DivisionConditionPair divisionConditionPair: divisionConditionPairList){
            if (!divisionConditionPair.isProcessed() && divisionConditionPair.isConditionMatched(receivedConditionList)){
                isProcessed = true;
                divisionConditionPair.setProcessed(true);
                AccountManager.getInstance().addValue(getTo(), divisionConditionPair.getPercent() * getValue() / 100);
            }
        }
        return isProcessed;
    }

    public boolean setCondition(String conditionString) {
        if (!isValid(conditionString)) {
            logger.warn("[ConditionalDivisionTransaction] Invalid condition division transaction" + conditionString);
            return false;
        }

        String conditionStringArray[] = conditionString.split(conditionSeparator);
        if (1 != conditionStringArray.length % 2){
            logger.warn("[ConditionalDivisionTransaction] Condition division transaction string parameter number not valid: "
                    + conditionString);
            return false;
        }

        this.conditionString = conditionString;
        for (int i = 1; i < conditionStringArray.length; i += 2) { // p1+p2_25 then add 2
            String conditionInternalArray[] = conditionStringArray[i].split(conditionInternalSeparator);
            DivisionConditionPair pair = DivisionConditionPair.construct(Arrays.asList(conditionInternalArray), conditionStringArray[i + 1]);
            if (null == pair) return false;

            divisionConditionPairList.add(pair);
        }
        int totalPercent = 0;
        for (DivisionConditionPair pair : divisionConditionPairList){
            totalPercent += pair.getPercent();
        }
        if (100 != totalPercent){
            logger.error("[ConditionalDivisionTransaction] Total percent is not 100: " + conditionString);
            return false;
        }
        return true;
    }

    public static ConditionalDivisionTransaction construct(String msg) {
        if (!isValid(msg)) return null;

        ConditionalDivisionTransaction conditionTransaction = new ConditionalDivisionTransaction();
        if (!conditionTransaction.setCondition(msg)) return null;
        return conditionTransaction;
    }

    public List<DivisionConditionPair> getDivisionConditionPairList() {
        return divisionConditionPairList;
    }

    static boolean isValid(String msg) {
        if (null == msg) return false;

        return msg.contains(conditionKeyword);
    }

    public boolean isAllDivisionProcessed() {
        for (DivisionConditionPair pair : divisionConditionPairList){
            if (!pair.isConditionMatched(receivedConditionList)){
                return false;
            }
        }
        return true;
    }
}

class DivisionConditionPair{
    static Logger logger = FileLogger.getLogger();

    protected List<String> conditionList = new ArrayList<>();
    protected int percent;
    boolean isProcessed = false;

    public static DivisionConditionPair construct(List<String> conditionList, String percentString) {
        int percent = 0;
        try {
            percent = Integer.parseInt(percentString);
        } catch (NumberFormatException e){
            logger.error("[DivisionConditionPair] Error in percent expression " + percentString );
            return null;
        }

        DivisionConditionPair pair = new DivisionConditionPair();
        pair.setPercent(percent);
        pair.setConditionList(conditionList);
        return pair;
    }

    public boolean isProcessed() {
        return isProcessed;
    }

    public void setProcessed(boolean processed) {
        isProcessed = processed;
    }

    public List<String> getConditionList() {
        return conditionList;
    }

    public void setConditionList(List<String> conditionList) {
        this.conditionList = conditionList;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public boolean isConditionMatched(List<String> receivedConditionList){
        for (String condition : this.conditionList){
            if (!receivedConditionList.contains(condition)) return false;
        }
        return true;
    }
}

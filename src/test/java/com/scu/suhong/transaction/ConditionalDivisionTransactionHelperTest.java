package com.scu.suhong.transaction;

import account.AccountManager;
import javafx.util.Pair;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ConditionalDivisionTransactionHelperTest {
    private String userA = "0x13570";
    private String userB = "0x13571";
    private String userC = "0x13572";
    private String userD = "0x13573";
    private String userE = "0x13574";
    private String userF = "0x13575";
    private String userG = "0x13576";
    private String userH = "0x13577";
    private String userI = "0x13578";

    AccountManager accountManager = AccountManager.getInstance();
    final int initValue = 1000;

    @Test
    public void testConstructDivisionTransactionCTx() {
        String msg1 = "if_sd_sd";
        ConditionalDivisionTransaction transaction1 = ConditionalDivisionTransaction.construct(msg1);
        assert null == transaction1;

        String msg11 = "divisionIf_c1_25";
        ConditionalDivisionTransaction transaction11 = ConditionalDivisionTransaction.construct(msg11);
        assert null == transaction11;

        String msg2 = "divisionIf_c1_25_c2";
        ConditionalDivisionTransaction transaction2 = ConditionalDivisionTransaction.construct(msg2);
        assert null == transaction2;

        String msg3 = "divisionIf_c1_100";
        ConditionalDivisionTransaction transaction3 = ConditionalDivisionTransaction.construct(msg3);
        assert null != transaction3;

        String msg4 = "divisionIf_c1_25_c2_75";
        ConditionalDivisionTransaction transaction4 = ConditionalDivisionTransaction.construct(msg4);
        assert null != transaction4;

        String msg5 = "divisionIf_c1_50_c2+c1_50";
        ConditionalDivisionTransaction transaction5 = ConditionalDivisionTransaction.construct(msg5);
        assert null != transaction5;
    }

    @Test
    public void testConstructDivisionConditionCTx() {
        String msg1 = "if_sd_sd";
        DivisionCondition condition1 = DivisionCondition.construct(msg1);
        assert null == condition1;

        String msg2 = "matchedPartition_c1";
        DivisionCondition condition2 = DivisionCondition.construct(msg2);
        assert null != condition2;
        assert condition2.getConditionList().size() == 1;

        String msg3 = "matchedPartition_c1_c2";
        DivisionCondition condition3 = DivisionCondition.construct(msg3);
        assert null != condition3;
        assert condition3.getConditionList().size() == 2;
    }

    @Test
    public void testProcessOneStep() {
        initUser();
        assert accountManager.getBalance(userA) == initValue;
        assert accountManager.getBalance(userB) == initValue;

        ConditionalDivisionTransactionHelper helper = new ConditionalDivisionTransactionHelper();

        String txMsg1 = "divisionIf_c1_100";
        int value = 100;
        ConditionalDivisionTransaction transaction1 = constructDivisionTransaction(userA, userB, value, txMsg1);
        assert null != transaction1;

        helper.processCTx(transaction1);
        assert helper.getUnmatchedDivisionTransactionList().size() == 1;
        assert initValue - value == accountManager.getBalance(userA);
        assert accountManager.getBalance(userB) == initValue;

        String conditionMsg1 = "matchedPartition_c1";
        DivisionCondition condition1 = constructDivisionCondition(userA, userB, conditionMsg1);
        assert null != condition1;

        helper.processCTx(condition1);
        assert helper.getUnmatchedDivisionTransactionList().size() == 0;
        // Transaction A --> B , condition A --> B and then partially pay to B
        System.out.println(accountManager.getBalance(userA) + " : " + accountManager.getBalance(userB));
        assert initValue - value == accountManager.getBalance(userA);
        assert accountManager.getBalance(userB) == initValue + value;
    }

    // Transaction A --> B , condition A --> B and then partially pay to B
    @Test
    public void testProcessFourSteps() {
        initUser();
        assert accountManager.getBalance(userA) == initValue;
        assert accountManager.getBalance(userB) == initValue;

        ConditionalDivisionTransactionHelper helper = new ConditionalDivisionTransactionHelper();

        String txMsg1 = "divisionIf_c1_30_c2_25_c3_35_c4_10";
        int value = 100;
        ConditionalDivisionTransaction transaction1 = constructDivisionTransaction(userA, userB, value, txMsg1);
        assert null != transaction1;

        helper.processCTx(transaction1);
        assert helper.getUnmatchedDivisionTransactionList().size() == 1;
        assert initValue - value == accountManager.getBalance(userA);
        assert accountManager.getBalance(userB) == initValue;

        String conditionMsgArray[] = {"matchedPartition_c1", "matchedPartition_c2", "matchedPartition_c3", "matchedPartition_c4"};
        int percent[] = {30, 25, 35, 10};
        List<Integer> sequenceList = getSequence(4);

        int totalPercent = 0;
        for (Integer s : sequenceList) {
            int i = s - 1;
            totalPercent += percent[i];
            processConditionTransaction(helper, 100, conditionMsgArray[i], totalPercent);
        }
        assert helper.isAllTransactionDone();
    }


    List<Integer> getSequence(int way) {
        List<Integer> sequenceList = new ArrayList<>();
        if (1 == way) {
            sequenceList.add(1);
            sequenceList.add(2);
            sequenceList.add(3);
            sequenceList.add(4);
        } else if (2 == way) {
            sequenceList.add(2);
            sequenceList.add(4);
            sequenceList.add(1);
            sequenceList.add(3);
        } else if (3 == way) {
            sequenceList.add(4);
            sequenceList.add(3);
            sequenceList.add(2);
            sequenceList.add(1);
        } else if (4 == way) {
            sequenceList.add(1);
            sequenceList.add(3);
            sequenceList.add(2);
            sequenceList.add(4);
        }
        return sequenceList;
    }

    List<Pair<Integer, Integer>> getSequencePercentPair(int way) {
        List<Pair<Integer, Integer>> sequencePercentList = new ArrayList<>();
        if (1 == way) {
            sequencePercentList.add(new Pair<>(1, 30));
            sequencePercentList.add(new Pair<>(2, 0));
            sequencePercentList.add(new Pair<>(3, 60));
            sequencePercentList.add(new Pair<>(4, 10));
        } else if (2 == way) {
            sequencePercentList.add(new Pair<>(2, 0));
            sequencePercentList.add(new Pair<>(4, 10));
            sequencePercentList.add(new Pair<>(1, 30));
            sequencePercentList.add(new Pair<>(3, 60));
        } else if (3 == way) {
            sequencePercentList.add(new Pair<>(4, 10));
            sequencePercentList.add(new Pair<>(3, 0));
            sequencePercentList.add(new Pair<>(2, 25));
            sequencePercentList.add(new Pair<>(1, 65));
        } else if (4 == way) {
            sequencePercentList.add(new Pair<>(1, 30));
            sequencePercentList.add(new Pair<>(3, 35));
            sequencePercentList.add(new Pair<>(2, 25));
            sequencePercentList.add(new Pair<>(4, 10));
        }
        return sequencePercentList;
    }

    void processConditionTransaction(ConditionalDivisionTransactionHelper helper, int value, String conditionMsg, int percent) {
        DivisionCondition condition = constructDivisionCondition(userA, userB, conditionMsg);
        assert null != condition;
        helper.processCTx(condition);
        assert initValue - value == accountManager.getBalance(userA);
        System.out.println("[Result] Current balance of receiver is: " + accountManager.getBalance(userB));
        assert accountManager.getBalance(userB) == initValue + value * percent / 100;
    }

    // Transaction A --> B , condition A --> B and then partially pay to B
    @Test
    public void testProcessFourStepsCombination() {
        initUser();
        assert accountManager.getBalance(userA) == initValue;
        assert accountManager.getBalance(userB) == initValue;

        ConditionalDivisionTransactionHelper helper = new ConditionalDivisionTransactionHelper();

        String txMsg1 = "divisionIf_c1_30_c2+c3_25_c3+c1_35_c4_10";
        int value = 100;
        ConditionalDivisionTransaction transaction = constructDivisionTransaction(userA, userB, value, txMsg1);
        assert null != transaction;
        helper.processCTx(transaction);

        String conditionMsgArray[] = {"matchedPartition_c1", "matchedPartition_c2", "matchedPartition_c3", "matchedPartition_c4"};
        List<Pair<Integer, Integer>> sequencePercentList = getSequencePercentPair(4); // 1 to 4
        int totalPercent = 0;
        for (Pair<Integer, Integer>  pair: sequencePercentList) {
            int i = pair.getKey() - 1;
            totalPercent += pair.getValue();
            processConditionTransaction(helper, 100, conditionMsgArray[i], totalPercent);
        }
        assert helper.isAllTransactionDone();
    }

    DivisionCondition constructDivisionCondition(String from, String to, String data) {
        DivisionCondition divisionCondition = DivisionCondition.construct(data);
        divisionCondition.setFrom(from);
        divisionCondition.setToAndValue(to, 0);
        return divisionCondition;
    }

    ConditionalDivisionTransaction constructDivisionTransaction(String from, String to, int value, String data) {
        ConditionalDivisionTransaction transaction = ConditionalDivisionTransaction.construct(data);
        transaction.setFrom(from);
        transaction.setToAndValue(to, value);
        return transaction;
    }

    void initUser() {
        accountManager.addValue(userA, initValue);
        accountManager.addValue(userB, initValue);
        accountManager.addValue(userC, initValue);
        accountManager.addValue(userD, initValue);
        accountManager.addValue(userE, initValue);
        accountManager.addValue(userF, initValue);
        accountManager.addValue(userG, initValue);
        accountManager.addValue(userH, initValue);
        accountManager.addValue(userI, initValue);
    }
}
package com.scu.suhong.transaction;

import account.AccountManager;
import com.scu.suhong.graph.JGraphTWrapper;
import org.junit.Test;
import util.NoDuplicatedRandomHelper;
import util.ThreadHelper;
import util.TimeHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConditionalAssociationTransactionHelperTest {
    AccountManager accountManager = AccountManager.getInstance();
    int interactionId = 1;
    private String userA = "0x13571";
    private String userB = "0x13572";
    private String userC = "0x13573";
    private String userD = "0x13574";
    private String userE = "0x13575";
    private String userF = "0x13576";
    private String userG = "0x13577";
    private String userH = "0x13578";
    private String userI = "0x13579";
    private String conditionalMsg = "testConditionalMsg";

    @Test
    public void testAddNewCTxBetweenTwoPerson() {
        initUser();
        assert accountManager.getBalance(userA) == 100;
        assert accountManager.getBalance(userB) == 100;

        ConditionalAssociationTransactionHelper helper = ConditionalAssociationTransactionHelper.getInstance();
        helper.processCTx(constructConditionalTransaction(interactionId, userA, userB, 2));
        assert accountManager.getBalance(userA) == 98;
        assert accountManager.getBalance(userB) == 100;
        helper.exportGraph(interactionId);

        helper.processCTx(constructConditionalTransaction(interactionId, userB, userA, 2));
        assert accountManager.getBalance(userA) == 100;
        assert accountManager.getBalance(userB) == 100;
    }

    @Test
    public void testAddNewCTxBetweenThreePerson() {
        initUser();
        assert accountManager.getBalance(userA) == 100;
        assert accountManager.getBalance(userB) == 100;
        assert accountManager.getBalance(userC) == 100;

        ConditionalAssociationTransactionHelper helper = ConditionalAssociationTransactionHelper.getInstance();
        helper.processCTx(constructConditionalTransaction(interactionId, userA, userB, 2));
        assert accountManager.getBalance(userA) == 98;
        assert accountManager.getBalance(userB) == 100;
        assert accountManager.getBalance(userC) == 100;

        helper.processCTx(constructConditionalTransaction(interactionId, userC, userA, 2));
        assert accountManager.getBalance(userA) == 98;
        assert accountManager.getBalance(userB) == 100;
        assert accountManager.getBalance(userC) == 98;

        helper.processCTx(constructConditionalTransaction(interactionId, userB, userC, 2));
        assert accountManager.getBalance(userA) == 100;
        assert accountManager.getBalance(userB) == 100;
        assert accountManager.getBalance(userC) == 100;
    }

    @Test
    public void testGenerateFiveUniqueThreeTimes() {
        NoDuplicatedRandomHelper helper = new NoDuplicatedRandomHelper(5);
        System.out.println(helper.getNumber() + 1);
        System.out.println(helper.getNumber() + 1);
        System.out.println(helper.getNumber() + 1);
        System.out.println(helper.getNumber() + 1);
        System.out.println(helper.getNumber() + 1);
    }

    @Test
    public void testAddNewCTxBetweenFivePersonFreely() {
        int ctSequence1[] = {5, 1, 4, 2, 3};
        int ctSequence2[] = {3, 1, 4, 5, 2};
        int ctSequence3[] = {4, 3, 2, 5, 1};
        String userArray[] = {userA, userB, userC, userD, userE};

        initUser();
        for (int i = 0; i < userArray.length; ++i) {
            assert accountManager.getBalance(userArray[i]) == 100;
        }
        printUserBalance(userArray);

        ConditionalAssociationTransactionHelper helper = ConditionalAssociationTransactionHelper.getInstance();

        int targetArray[] = ctSequence1;
//        //int targetArray[] = ctSequence2;
//        for (int i = 0; i < targetArray.length; ++i) {
//            helper.processTx(constructConditionalTransaction(interactionId, userArray[targetArray[i] - 1], userArray[(targetArray[i]) % 5], 2));
//            printUserBalance(userArray);
//            if (i < targetArray.length - 1) {
//                assert accountManager.getBalance(userArray[targetArray[i] - 1]) == 98;
//            }
//        }
//        for (int i = 0; i < targetArray.length; ++i) {
//            assert accountManager.getBalance(userArray[i]) == 100;
//        }

        helper.processCTx(constructConditionalTransaction(interactionId, userB, userC, 2));
        printUserBalance(userArray);
        helper.processCTx(constructConditionalTransaction(interactionId, userA, userE, 2));
        printUserBalance(userArray);
        helper.processCTx(constructConditionalTransaction(interactionId, userC, userD, 2));
        printUserBalance(userArray);
        helper.processCTx(constructConditionalTransaction(interactionId, userD, userB, 2));
        printUserBalance(userArray);
        helper.processCTx(constructConditionalTransaction(interactionId, userE, userA, 2));
        printUserBalance(userArray);


        for (int i = 0; i < targetArray.length; ++i) {
            assert accountManager.getBalance(userArray[i]) == 100;
        }

    }

    @Test
    public void testAddNewCTxWithMultiUser() {
        initUser();
        assert accountManager.getBalance(userA) == 100;
        assert accountManager.getBalance(userB) == 100;
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;

        ConditionalAssociationTransactionHelper helper = ConditionalAssociationTransactionHelper.getInstance();
        helper.processCTx(constructConditionalTransaction(interactionId, userA, userB, 4));
        assert 0 == accountManager.getBalance(userA).compareTo((double) 96);
        assert accountManager.getBalance(userB) == 100;
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;

        String b2CAndD = userC + "_" + 3 + "_" + userD + "_" + 1;
        helper.processCTx(constructConditionalTransaction(interactionId, userB, b2CAndD, 4));
        assert 0 == accountManager.getBalance(userA).compareTo((double) 96);
        assert 0 == accountManager.getBalance(userB).compareTo((double) 96);
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;

        ConditionalAssociationTransaction t = constructConditionalTransaction(interactionId, userC, userA, 3);
        helper.processCTx(t);// refused to process one condition transaction twice
        assert 0 == accountManager.getBalance(userA).compareTo((double) 99);
        assert 0 == accountManager.getBalance(userB).compareTo((double) 99);
        assert 0 == accountManager.getBalance(userC).compareTo((double) 100);
        assert accountManager.getBalance(userD) == 100;
        helper.processCTx(t);// refused to process one condition transaction twice
        assert 0 == accountManager.getBalance(userA).compareTo((double) 99);
        assert 0 == accountManager.getBalance(userB).compareTo((double) 99);
        assert 0 == accountManager.getBalance(userC).compareTo((double) 100);
        assert accountManager.getBalance(userD) == 100;

        helper.processCTx(constructConditionalTransaction(interactionId, userD, userA, 1));
        assert accountManager.getBalance(userA) == 100;
        assert accountManager.getBalance(userB) == 100;
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;
    }

    @Test
    public void testAddNewCTxWithMultiUserUnfrozen() {
        String[] userArray = {userA, userB, userC, userD, userE, userF};
        // // this is got before with random, to make test more unify, we adapt this array
        //int valueRandomBefore[] = {91, 74, 75, 65, 61, 87};
        Double[] valueRandomBefore = {100d, 100d, 100d, 100d, 100d, 100d};
        Map<String, Double> userInitBalance = new HashMap<>();
        // first we get 30 to 100 value randomly
        //RandomHelper randomHelper = new RandomHelper(70);
        for (int i = 0; i < userArray.length; ++i) {
            //int initValue = randomHelper.getNumber() + 30;
            Double initValue = valueRandomBefore[i];
            userInitBalance.put(userArray[i], initValue);
            accountManager.addValue(userArray[i], initValue);
        }
        printUserBalance(userArray, userInitBalance);

        ConditionalAssociationTransactionHelper helper = ConditionalAssociationTransactionHelper.getInstance();
        helper.processCTx(constructConditionalTransaction(interactionId, userArray[0], userArray[1], 4));
        printUserBalance(userArray, userInitBalance);
        for (int i = 0; i < userArray.length; ++i) {
            if (i == 0) {
                assert accountManager.getBalance(userArray[i]) == userInitBalance.get(userArray[i]) - 4;
            } else {
                assert accountManager.getBalance(userArray[i]) == userInitBalance.get(userArray[i]);
            }
        }

        helper.processCTx(constructConditionalTransaction(interactionId, userArray[2], userArray[3], 4));
        printUserBalance(userArray, userInitBalance);
        for (int i = 1; i < userArray.length; ++i) {
            if (i == 0 || i == 2) {
                assert accountManager.getBalance(userArray[i]) == userInitBalance.get(userArray[i]) - 4;
            } else {
                assert accountManager.getBalance(userArray[i]) == userInitBalance.get(userArray[i]);
            }
        }

        helper.processCTx(constructConditionalTransaction(interactionId, userArray[4], userArray[5], 4));
        printUserBalance(userArray, userInitBalance);
        for (int i = 1; i < userArray.length; ++i) {
            if (i == 0 || i == 2 || i == 4) {
                assert accountManager.getBalance(userArray[i]) == userInitBalance.get(userArray[i]) - 4;
            } else {
                assert accountManager.getBalance(userArray[i]) == userInitBalance.get(userArray[i]);
            }
        }

        helper.unFreezeAccount(interactionId);
        for (int i = 0; i < userArray.length; ++i) {
            assert accountManager.getBalance(userArray[i]) == userInitBalance.get(userArray[i]);
        }
        printUserBalance(userArray, userInitBalance);

    }

    void printUserBalance(String[] userArray, Map<String, Double> userInitBalance) {
        String oneLineValue = "";
        for (int i = 0; i < userArray.length; ++i) {
            System.out.println(String.format("[ConditionalAssociationTransactionHelperTest] User initial balance of %s is %f - %f"
                    , userArray[i], userInitBalance.get(userArray[i])
                    , AccountManager.getInstance().getBalance(userArray[i])));
            oneLineValue += AccountManager.getInstance().getBalance(userArray[i]) + " ";
        }
        System.out.println("Value in line is: " + oneLineValue);
    }

    void printUserBalance(String userArray[]) {
        String oneLineValue = "";
        for (int i = 0; i < userArray.length; ++i) {
            System.out.printf("[ConditionalAssociationTransactionHelperTest] User initial balance of %s is %f"
                    , userArray[i]
                    , AccountManager.getInstance().getBalance(userArray[i]));
            oneLineValue += AccountManager.getInstance().getBalance(userArray[i]) + " ";
        }
        System.out.println("Value in line is: " + oneLineValue);
    }

    @Test
    public void testAddNewCTxWithMultiUser_SmallRing() {
        initUser();
        assert accountManager.getBalance(userA) == 100;
        assert accountManager.getBalance(userB) == 100;
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;

        System.out.println("\n[Test] Begin to send the first transaction");
        ConditionalAssociationTransactionHelper helper = ConditionalAssociationTransactionHelper.getInstance();
        helper.setDiagramAlgorithmType(ConditionalAssociationTransactionHelper.DiagramAlgorithmType.E_Compute_All_Small_Ring);
        helper.processCTx(constructConditionalTransaction(interactionId, userA, userB, 4));
        assert accountManager.getBalance(userA) == 96;
        assert accountManager.getBalance(userB) == 100;
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;

        System.out.println("\n[Test] Begin to send the second transaction");
        String b2CAndD = userC + "_" + 3 + "_" + userD + "_" + 1;
        helper.processCTx(constructConditionalTransaction(interactionId, userB, b2CAndD, 4));
        assert accountManager.getBalance(userA) == 96;
        assert accountManager.getBalance(userB) == 96;
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;

        System.out.println("\n[Test] Begin to send the third transaction");
        helper.processCTx(constructConditionalTransaction(interactionId, userC, userA, 3));
        assert accountManager.getBalance(userA) == 99;
        assert accountManager.getBalance(userB) == 99;
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;

        System.out.println("\n[Test] Begin to send the fourth transaction");
        helper.processCTx(constructConditionalTransaction(interactionId, userD, userA, 1));
        assert accountManager.getBalance(userA) == 100;
        assert accountManager.getBalance(userB) == 100;
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;
    }

    @Test
    public void testAddNewCTxWithMultiUser_MergeTx_Two_Parallel() {
        initUser();

        ConditionalAssociationTransactionHelper helper = ConditionalAssociationTransactionHelper.getInstance();
        List<ConditionalAssociationTransaction> conditionalAssociationTransactionList = new ArrayList<>();
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userA, userB, 4));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userB, userC, 4));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userC, userD, 4));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userD, userA, 4));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userA, userB, 6));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userB, userE, 6));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userE, userF, 6));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userF, userA, 2));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userF, userA, 4));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userC, userD, 3));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userD, userG, 3));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userG, userC, 3));
        assert helper.planAssociationCTx(conditionalAssociationTransactionList);
    }

    @Test
    public void testAddNewCTxWithMultiUser_MergeTx_Three_Parallel() {
        initUser();

        ConditionalAssociationTransactionHelper helper = ConditionalAssociationTransactionHelper.getInstance();
        List<ConditionalAssociationTransaction> conditionalAssociationTransactionList = new ArrayList<>();
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userA, userB, 1));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userC, userA, 2));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userB, userD, 1));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userD, userA, 1));
        helper.planAssociationCTx(conditionalAssociationTransactionList, false);
        helper.resetGraphTWrapper();
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userA, userB, 2));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userB, userE, 3));
        helper.planAssociationCTx(conditionalAssociationTransactionList, false);
        helper.resetGraphTWrapper();
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userE, userA, 3));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userA, userB, 3));
        conditionalAssociationTransactionList.add(constructConditionalTransaction(interactionId, userB, userC, 2));
        helper.resetGraphTWrapper();
        assert helper.planAssociationCTx(conditionalAssociationTransactionList);
    }


    @Test
    public void testAddNewCTxWithMultiUser_MergeTx_Timeout_Then_Separation() {
        initUser();
        ConditionalAssociationTransaction conditionalAssociationTransactionArray[] = {
                constructConditionalTransaction(interactionId, userA, userB, 1)
                , constructConditionalTransaction(interactionId, userC, userA, 2)
                , constructConditionalTransaction(interactionId, userB, userD, 1)
                , constructConditionalTransaction(interactionId, userD, userA, 1)
                , constructConditionalTransaction(interactionId, userA, userB, 2)
                , constructConditionalTransaction(interactionId, userB, userE, 3)
                , constructConditionalTransaction(interactionId, userE, userA, 3)
                , constructConditionalTransaction(interactionId, userA, userB, 3)
                , constructConditionalTransaction(interactionId, userB, userC, 2)
        };

        ConditionalAssociationTransactionHelper helper = ConditionalAssociationTransactionHelper.getInstance();
        List<ConditionalAssociationTransaction> conditionalAssociationTransactionList = new ArrayList<>();
        for (int i = 0; i < conditionalAssociationTransactionArray.length; ++i) {
            conditionalAssociationTransactionList.add(conditionalAssociationTransactionArray[i]);
        }
        helper.planAssociationCTx(conditionalAssociationTransactionList);// plan

        helper.resetGraphTWrapper();
        helper.setMultiEdgeShowOptions(interactionId, JGraphTWrapper.MultiEdgeShowOptions.E_Show_Sum);
        //then try it
        for (int i = 0; i < conditionalAssociationTransactionArray.length; ++i) {
            // Just ignore this transaction to make it simulate the merge case
            if (3 == i) continue;

            helper.processCTx(conditionalAssociationTransactionArray[i]);
        }

        helper.setDiagramAlgorithmType(ConditionalAssociationTransactionHelper.DiagramAlgorithmType.E_Compute_All_Small_Ring);
        System.out.println("[ConditionalAssociationTransactionHelperTest] Simulate the timeout case");
        ThreadHelper.safeSleep(10);
        helper.doGraphCalculation(interactionId, "Timeout");
        helper.doGraphCalculation(interactionId);
        helper.doGraphCalculation(interactionId);
    }

    ConditionalAssociationTransaction constructConditionalTransaction(int interactionId, String from, String to, int value) {
        Condition c = new Condition(from, to, value);
        ConditionalAssociationTransaction t = new ConditionalAssociationTransaction(interactionId, c);
        t.setData(String.valueOf(TimeHelper.getEpoch()));
        t.setId();
        return t;
    }

    @Test
    public void testSmallRingTime() {
        int[] testTransactionNumber = {4, 8, 16, 32, 64, 128, 256, 512, 1024};
        int[] testRound = {400, 400, 200, 200, 200, 200, 200, 200}; // 20
        for (int i = 0; i < testTransactionNumber.length; ++i ) {
            doOneTest(testTransactionNumber[i], testRound[i]);
        }
    }

    private void doOneTest(int testTransactionNumber, int testRound) {
        ConditionalAssociationTransactionHelper helper = ConditionalAssociationTransactionHelper.getInstance();
        helper.setTestProhibitDump();// do not output debug graph. This is used to test the internal small ring calculation time without impact of this debug output
        ConditionalAssociationTransaction t = null;
        String userPrefix = "78900";
        int interactionId = 1001;
        for (int i = 0; i < testTransactionNumber; ++i) {
            AccountManager.getInstance().addValue(userPrefix + i, 100);
            t = constructConditionalTransaction(interactionId, userPrefix + i, userPrefix + i + 1, 1);
            helper.processCTx(t);
        }
        //

        Long startEpoc = TimeHelper.getEpoch();
        for (int i = 0; i < testRound; ++i) {
            helper.computeAllSmallRing(interactionId);
        }
        Long endEpoc = TimeHelper.getEpoch();
        Long totalTime = endEpoc - startEpoc;
        System.out.printf("[ConditionalAssociationTransactionHelperTest][PerformnaceTest][CJE][%d] Total time %d for %d round test for %d transaction, average time %d\n",
                interactionId, totalTime, testRound, testTransactionNumber, totalTime / testRound);
    }

    // add enough money to let user do next
    void initUser() {
        accountManager.addValue(userA, 100);
        accountManager.addValue(userB, 100);
        accountManager.addValue(userC, 100);
        accountManager.addValue(userD, 100);
        accountManager.addValue(userE, 100);
        accountManager.addValue(userF, 100);
        accountManager.addValue(userG, 100);
        accountManager.addValue(userH, 100);
        accountManager.addValue(userI, 100);
    }

    @Test
    public void testGetFileName() {
        ConditionalAssociationTransactionHelper helper = ConditionalAssociationTransactionHelper.getInstance();
        String fileName = helper.getFileName(interactionId);
        System.out.println(fileName);
        assert !fileName.isEmpty();
    }

    @Test
    public void testCreateDumpFolder() {
        ConditionalAssociationTransactionHelper helper = ConditionalAssociationTransactionHelper.getInstance();
        File file = new File(helper.getDumpFolderName());
        if (file.exists()) file.delete();
        assert helper.createDumpFolder();
        assert helper.createDumpFolder();
    }

}
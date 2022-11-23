package com.scu.suhong.transaction.exchangeMode;

import account.AccountManager;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import util.ThreadHelper;
import util.TimeHelper;

import java.util.ArrayList;
import java.util.List;

public class EMTransactionServiceTest {
    final static String testClient1Address = "A";
    final static String testClient2Address = "B";
    final static String testClient3Address = "C";

    final String externalType1 = "personalData";
    final String externalType2 = "copyright";
    final static int client1ToClient2 = 3;
    final static int client2ToClient1 = 22;
    final static int nClient1ToClient2 = 3;
    final static int nClient2ToClient3 = 22;
    final static int nClient3ToClient1 = 13;

    final static int externalType2Value1 = 9;
    final static int externalType2Value2 = 5;
    final static int externalType1Value1 = 20;
    final static int externalType1Value2 = 25;
    final static int internalTypeValue1 = 17;
    final static int internalTypeValue2 = 20;

    final static AccountManager accountManager = AccountManager.getInstance();

    @Test
    public void processTxBetweenTwoPeers() {
        accountManager.addValue(testClient1Address, 60);
        accountManager.addValue(testClient2Address, 60);
        Double balance1 = accountManager.getBalance(testClient1Address);
        EMTransaction emTransaction1 = createClient1Transaction();
        EMTransactionService service = EMTransactionService.getInstance();
        assert service.processTx(emTransaction1);
        assert accountManager.getBalance(testClient1Address) == balance1 - emTransaction1.getMaxFrozenValue();

        EMTransaction emTransaction2 = createClient2Transaction();
        assert service.processTx(emTransaction2);
        assert accountManager.getBalance(testClient1Address) == 60 - client1ToClient2 + client2ToClient1;
        assert accountManager.getBalance(testClient2Address) == 60 - client2ToClient1 + client1ToClient2;
    }

    @Test
    public void processTxBetweenTwoPeersWithTwoTypes() {
        String externalType = externalType1;
        accountManager.addValue(testClient1Address, 60);
        accountManager.addValue(testClient2Address, externalType, 60);
        Double balance1 = accountManager.getBalance(testClient1Address);
        EMTransaction emTransaction1 = createTwoTypesClient1Transaction();
        EMTransactionService service = EMTransactionService.getInstance();
        assert service.processTx(emTransaction1);
        assert accountManager.getBalance(testClient1Address) == balance1 - emTransaction1.getMaxFrozenValue();

        System.out.println("Begin to process transaction 2\n");
        EMTransaction emTransaction2 = createTwoTypesClient2Transaction();
        assert service.processTx(emTransaction2);

        System.out.printf("Balance type original coins for user1 %s and user2 %s\n",
                accountManager.getBalance(testClient1Address).toString(), accountManager.getBalance(testClient2Address).toString());
        System.out.printf("Balance type personalData for user1 %s and user2 %s\n",
                accountManager.getBalance(testClient1Address, externalType).toString()
                , accountManager.getBalance(testClient2Address, externalType).toString());

        assert accountManager.getBalance(testClient1Address) == 60 - internalTypeValue2;
        assert accountManager.getBalance(testClient2Address) == internalTypeValue2;

        assert accountManager.getBalance(testClient1Address, externalType) == externalType1Value1;
        assert accountManager.getBalance(testClient2Address, externalType) == 60 - externalType1Value1;
    }

    @Test
    public void processTxAmongThreePeersWithThreeTypes() {
        accountManager.addValue(testClient1Address, 60);
        accountManager.addValue(testClient2Address, externalType1, 60);
        accountManager.addValue(testClient3Address, externalType2, 60);
        System.out.println("Initial value\n");
        EMTransactionService.showAllBalance("Initial value");

        Double balance1 = accountManager.getBalance(testClient1Address);
        EMTransactionService service = EMTransactionService.getInstance();

        System.out.println("Begin to process transaction 1\n");
        EMTransaction emTransaction1 = createThreeTypesClient1Transaction();
        assert service.processTx(emTransaction1);
        assert accountManager.getBalance(testClient1Address) == balance1 - emTransaction1.getMaxFrozenValue();

        System.out.println("Begin to process transaction 2\n");
        EMTransaction emTransaction2 = createThreeTypesClient2Transaction();
        assert service.processTx(emTransaction2);

        System.out.println("Begin to process transaction 3\n");
        EMTransaction emTransaction3 = createThreeTypesClient3Transaction();
        assert service.processTx(emTransaction3);

        assert accountManager.getBalance(testClient1Address) == 60 - internalTypeValue1;
        assert accountManager.getBalance(testClient2Address) == internalTypeValue1;
        assert accountManager.getBalance(testClient3Address) == 0;

        assert accountManager.getBalance(testClient1Address, externalType1) == 0;
        assert accountManager.getBalance(testClient2Address, externalType1) == 60 - externalType1Value1;
        assert accountManager.getBalance(testClient3Address, externalType1) == externalType1Value1;

        assert accountManager.getBalance(testClient1Address, externalType2) == externalType2Value1;
        assert accountManager.getBalance(testClient2Address, externalType2) == 0;
        assert accountManager.getBalance(testClient3Address, externalType2) ==60 -  externalType2Value1;
    }

    @Test
    public void processTxAmongThreePeers() {
        accountManager.addValue(testClient1Address, 60);
        accountManager.addValue(testClient2Address, 70);
        accountManager.addValue(testClient3Address, 80);
        Double balance1 = accountManager.getBalance(testClient1Address);
        Double balance2 = accountManager.getBalance(testClient2Address);
        Double balance3 = accountManager.getBalance(testClient3Address);

        EMTransactionService service = EMTransactionService.getInstance();
        EMTransaction emTransaction1 = createNewClient1Transaction();
        assert service.processTx(emTransaction1);
        assert accountManager.getBalance(testClient1Address) == balance1 - emTransaction1.getMaxFrozenValue();

        EMTransaction emTransaction2 = createNewClient2Transaction();
        assert service.processTx(emTransaction2);
        assert accountManager.getBalance(testClient2Address) == balance2 - emTransaction2.getMaxFrozenValue();

        EMTransaction emTransaction3 = createNewClient3Transaction();
        assert service.processTx(emTransaction3);

        assert accountManager.getBalance(testClient1Address) == balance1 - nClient1ToClient2 + nClient3ToClient1;
        assert accountManager.getBalance(testClient2Address) == balance2 - nClient2ToClient3 + nClient1ToClient2;
        assert accountManager.getBalance(testClient3Address) == balance3 - nClient3ToClient1 + nClient2ToClient3;
    }

    // For paper test
    @Test
    public void testABDifferentRatio(){
        final double[] ratio = {1, 2, 4, 8, 16};
        for (int i = 0; i < ratio.length; ++i){
            System.out.println("[verification] Begin to test ratio " + ratio[i]);
            testABDifferentRatio(ratio[i], 10);
        }
    }

    // For paper test
    @Test
    public void testABDifferentRatioFraction(){
        final double[] ratio = {1, (double) 1/2, (double)1/4, (double)1/8, (double)1/16};
        for (int i = 0; i < ratio.length; ++i){
            System.out.println("[verification] Begin to test ratio " + ratio[i]);
            testABDifferentRatio(ratio[i], 64);
        }
    }

    public void testABDifferentRatio(Double ratio, int fixPaymeny){
        final String userA = "A";
        final String userB = "B";
        final int userAInitValue = 400;
        final int userBInitValue = 500;
        final int contractNumber  = 1357;
        final int prepaid = 128;
        final int toValue = fixPaymeny;

        final String testGameServerIp = "127.0.0.1";

        accountManager.reset();
        accountManager.addValue(userA, userAInitValue);
        accountManager.addValue(userB, userBInitValue);

        EMTransaction transactionA2B = createClientTransaction(userA, userB, userB, contractNumber);
        //transactionA2B.setPrePaidValue(20);
        transactionA2B.addIncomingValueConditionPair(
                createRatioValueCondition(ConditionResult.Undefined, ratio,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp, "win"));
        transactionA2B.addIncomingValueConditionPair(
                createRatioValueCondition(ConditionResult.Undefined, 0.5,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp, "lost"));
        transactionA2B.addToValueConditionPair(createValueCondition(ConditionResult.True, toValue));

        // user B is the ratio payment
        EMTransaction transactionB2A = createClientTransaction(userB, userA, userA, contractNumber);
        transactionB2A.setPrePaidValue(prepaid);
        transactionB2A.addToValueConditionPair(
                createRatioValueCondition(ConditionResult.Undefined, ratio,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp, "win"));
        transactionB2A.addToValueConditionPair(
                createRatioValueCondition(ConditionResult.Undefined, 0.5,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp, "lost"));
        transactionB2A.addIncomingValueConditionPair(createValueCondition(ConditionResult.True, toValue));

        EMTransactionService service = EMTransactionService.getInstance();
        System.out.println("[EMTransactionServiceTest][Debug] Begin to add A to B transaction");

        showAllBalance("[Verification] The initial balance of A - B");
        //service.processTx(transactionA2B);
        service.processTx(transactionB2A);

        System.out.println("[EMTransactionServiceTest][Debug] Begin to add B to A transaction");
        //service.processTx(transactionB2A);
        service.processTx(transactionA2B);

        for (int i = 0; i < 1 && !service.doGraphCalculation(contractNumber); ++i){
            System.out.println("[EMTransactionServiceTest][Debug] In process loop");
            System.out.printf("[Verification] The balance of A - B is %f - - %f \n",getBalance(userA), getBalance(userB));
            ThreadHelper.safeSleep(1 * 1000);
        }

        service.processUnFinishedTransaction();
        showAllBalance("[Verification] The final balance of A - B");
        service.reset();
    }

    // For paper test
    @Test
    public void testNon_Blockchain(){
        final String userCustomer = "Customer";
        final String userSeller = "Seller";
        List<String> userList = new ArrayList<>();
        userList.add(userCustomer);
        userList.add(userSeller);

        final int userCustomerInitValue = 60;
        final int userSellerInitValue = 70;
        final int contractNumber  = 1359;

        final String testGameServerIp = "127.0.0.1";
        final String expressQueryString = "http com:yunda:nu:3910002516618";
        final String expressResultString = "\"state\":\"3\"";

        accountManager.addValue(userCustomer, userCustomerInitValue);
        accountManager.addValue(userSeller, userSellerInitValue);

        showAllBalance("[Verification] The initial balance customer - seller", userList);

        EMTransaction paymentFromCustomerToSeller = createClientTransaction(userCustomer, userSeller, userSeller, contractNumber);
        paymentFromCustomerToSeller.addToValueConditionPair(
                createValueCondition(ConditionResult.Undefined, 20, expressQueryString, expressResultString));
        paymentFromCustomerToSeller.addIncomingValueConditionPair(createValueCondition(ConditionResult.True, 0));

        System.out.printf("[EMTransactionServiceTest][Debug] Begin to add %s to %s transaction\n", userCustomer, userSeller);
        EMTransactionService service = EMTransactionService.getInstance();
        if (!service.processTx(paymentFromCustomerToSeller)) assert false;

        showAllBalance("[Verification] The after customer pay to seller balance customer - seller", userList);

        EMTransaction transactionFromSellerToCustomer = createClientTransaction(userSeller, userCustomer, userCustomer, contractNumber);
        transactionFromSellerToCustomer.addToValueConditionPair(createValueCondition(ConditionResult.True, 0));
        transactionFromSellerToCustomer.addIncomingValueConditionPair(createValueCondition(ConditionResult.Undefined, 20,expressQueryString, expressResultString));

        System.out.println("[EMTransactionServiceTest][Debug] Begin to add A to B transaction");
        showAllBalance("[Verification] The intial balance of A - B", userList);

        System.out.println("[EMTransactionServiceTest][Debug] Begin to add B to A transaction");
        if (!service.processTx(transactionFromSellerToCustomer)) assert false;

        for (int i = 0; /*i < 5 && */!service.doGraphCalculation(contractNumber); ++i){
            System.out.println("[EMTransactionServiceTest][Debug] In process loop");
            System.out.printf("[Verification] The balance of customer - seller is %f - - %f \n",getBalance(userCustomer), getBalance(userSeller));
            // ten minutes query
            ThreadHelper.safeSleep(600 * 1000);
        }
        service.shutDown();
        showAllBalance("[Verification] The final balance of customer - seller", userList);
        assert 40 == getBalance(userCustomer);
        assert 90 == getBalance(userSeller);
    }

    // For paper test
    @Test
    public void testTwoWayHandshake() {
        int initialCoinBalance = 60;
        int initialExternalTypeBalance = 120;
        String externalType = externalType1;
        accountManager.addValue(testClient1Address, initialCoinBalance);
        accountManager.addValue(testClient2Address, initialCoinBalance);
        accountManager.addValue(testClient1Address, externalType, initialExternalTypeBalance);
        accountManager.addValue(testClient2Address, externalType, initialExternalTypeBalance);
        Double balance1 = accountManager.getBalance(testClient1Address);
        printfTwoUserTypesBalance("Initial balance", externalType);

        EMTransaction emTransaction1 = createTwoWayHandshake1Transaction();
        EMTransactionService service = EMTransactionService.getInstance();
        assert service.processTx(emTransaction1);
        assert accountManager.getBalance(testClient1Address) == balance1 - emTransaction1.getMaxFrozenValue();
        printfTwoUserTypesBalance("After the first handshake", externalType);

        System.out.println("Begin to process transaction 2\n");
        EMTransaction emTransaction2 = createTwowayHandshake2Transaction();
        assert service.processTx(emTransaction2);

        printfTwoUserTypesBalance("After the second handshake", externalType);

        assert accountManager.getBalance(testClient1Address) == initialCoinBalance - internalTypeValue2;
        assert accountManager.getBalance(testClient2Address) == initialCoinBalance + internalTypeValue2;

        assert accountManager.getBalance(testClient1Address, externalType) == initialExternalTypeBalance + externalType1Value1;
        assert accountManager.getBalance(testClient2Address, externalType) == initialExternalTypeBalance - externalType1Value1;
    }

    void printfTwoUserTypesBalance(String additionalMsg, String externalType){
        System.out.println("[EMTransactionService][verification][balance] " + additionalMsg);
        System.out.printf("[EMTransactionService][verification][balance] " + "First Tx balance type original coins for user1 %s and user2 %s\n",
                accountManager.getBalance(testClient1Address).toString()
                , accountManager.getBalance(testClient2Address).toString());
        System.out.printf("[EMTransactionService][verification][balance] " + "First Tx balance type personalData for user1 %s and user2 %s\n",
                accountManager.getBalance(testClient1Address, externalType).toString()
                , accountManager.getBalance(testClient2Address, externalType).toString());
    }

    // For paper test
    @Test
    public void testAB(){
        final String userA = "A";
        final String userB = "B";
        final int userAInitValue = 60;
        final int userBInitValue = 70;
        final int contractNumber  = 1357;

        final String testGameServerIp = "127.0.0.1";

        accountManager.addValue(userA, userAInitValue);
        accountManager.addValue(userB, userBInitValue);

        EMTransaction transactionA2B = createClientTransaction(userA, userB, userB, contractNumber);
        transactionA2B.addIncomingValueConditionPair(
                createValueCondition(ConditionResult.Undefined, 20,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp, "win"));
        transactionA2B.addIncomingValueConditionPair(
                createValueCondition(ConditionResult.Undefined, 5,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp, "lost"));
        transactionA2B.addToValueConditionPair(createValueCondition(ConditionResult.True, 10));

        EMTransaction transactionB2A = createClientTransaction(userB, userA, userA, contractNumber);
        transactionB2A.addToValueConditionPair(
                createValueCondition(ConditionResult.Undefined, 20,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp, "win"));
        transactionB2A.addToValueConditionPair(
                createValueCondition(ConditionResult.Undefined, 5,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp, "lost"));
        transactionB2A.addIncomingValueConditionPair(createValueCondition(ConditionResult.True, 10));

        EMTransactionService service = EMTransactionService.getInstance();
        System.out.println("[EMTransactionServiceTest][Debug] Begin to add A to B transaction");
        showAllBalance("[Verification] The intial balance of A - B");

        service.processTx(transactionA2B);

        System.out.println("[EMTransactionServiceTest][Debug] Begin to add B to A transaction");
        service.processTx(transactionB2A);
        showAllBalance(formatPaidMessage(transactionB2A));

        for (int i = 0; i < 5 && !service.doGraphCalculation(contractNumber); ++i){
            System.out.println("[EMTransactionServiceTest][Debug] In process loop");
            System.out.printf("[Verification] The balance of A - B is %f - - %f \n",getBalance(userA), getBalance(userB));
            ThreadHelper.safeSleep(10 * 1000);
        }
        service.shutDown();
        showAllBalance("[Verification] The final balance of A - B");
        assert 70 == getBalance(userA);
        assert 60 == getBalance(userB);
    }

    Double getBalance(String address){
        return accountManager.getBalance(address);
    }

    // for paper test data collection
    void showAllBalance(String flag){
        final  String server = "server";
        final String userA = "A";
        final String userB = "B";
        AccountManager accountManager = AccountManager.getInstance();
        System.out.printf("[EMServerTransaction][verification][%s] The balance is %s\t%f\t%f\n",
                TimeHelper.getCurrentTimeUsingCalendar(), flag,
                accountManager.getBalance(userA), accountManager.getBalance(userB));
    }

    void showAllBalance(String flag, List<String> userList){
        AccountManager accountManager = AccountManager.getInstance();
        String result = String.format("[EMServerTransaction][verification][%s] The balance is ",
                TimeHelper.getCurrentTimeUsingCalendar());
        for (String user: userList) {
            result += " " + user + " : " + accountManager.getBalance(user);
        }
        System.out.println(result);
    }

    @NotNull
    public String formatWithdrawalMessage(EMTransaction clientTransaction) {
        return "Tx" + clientTransaction.getFrom() +"_w";
    }

    @NotNull
    public String formatFailedMessage(EMTransaction clientTransaction) {
        return "Tx" + clientTransaction.getFrom() +"_f";
    }

    @NotNull
    public String formatPaidMessage(EMTransaction clientTransaction) {
        return "Tx" + clientTransaction.getFrom() +"_p";
    }

    @NotNull
    private EMTransaction createClient1Transaction() {
        EMTransaction transaction = createClientTransaction(testClient1Address, testClient2Address,
                testClient2Address, 1234);
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.True, client2ToClient1));
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.False, 32));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.False, 21));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.True, client1ToClient2));
        return transaction;
    }

    @NotNull
    private EMTransaction createClient2Transaction() {
        EMTransaction transaction = createClientTransaction(testClient2Address, testClient1Address,
                testClient1Address, 1234);
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.False, 21));
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.True, client1ToClient2));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.True, client2ToClient1));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.False, 12));
        return transaction;
    }

    @NotNull
    private EMTransaction createNewClient1Transaction() {
        EMTransaction transaction = createClientTransaction(testClient1Address, testClient3Address,
                testClient2Address, 1234);
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.True, nClient3ToClient1));
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.False, 32));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.False, 21));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.True, nClient1ToClient2));
        return transaction;
    }

    @NotNull
    private EMTransaction createNewClient2Transaction() {
        EMTransaction transaction = createClientTransaction(testClient2Address, testClient1Address,
                testClient3Address, 1234);
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.True, nClient1ToClient2));
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.False, 32));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.False, 21));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.True, nClient2ToClient3));
        return transaction;
    }

    @NotNull
    private EMTransaction createTwoTypesClient1Transaction() {
        EMTransaction transaction = createClientTransaction(testClient1Address, testClient2Address,
                testClient2Address, 1235);
        transaction.addIncomingValueConditionPair(createTypedValueCondition(ConditionResult.True, externalType1Value1, externalType1));
        transaction.addIncomingValueConditionPair(createTypedValueCondition(ConditionResult.False, externalType1Value2, externalType1));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.False, internalTypeValue1));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.True, internalTypeValue2));
        return transaction;
    }

    @NotNull
    private EMTransaction createTwoTypesClient2Transaction() {
        EMTransaction transaction = createClientTransaction(testClient2Address, testClient1Address,
                testClient1Address, 1235);
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.False, internalTypeValue1));
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.True, internalTypeValue2));
        transaction.addToValueConditionPair(createTypedValueCondition(ConditionResult.True, externalType1Value1, externalType1));
        transaction.addToValueConditionPair(createTypedValueCondition(ConditionResult.False, externalType1Value2, externalType1));
        return transaction;
    }

    @NotNull
    private EMTransaction createTwoWayHandshake1Transaction() {
        EMTransaction transaction = createClientTransaction(testClient1Address, testClient2Address,
                testClient2Address, 1261);
        transaction.addIncomingValueConditionPair(createTypedValueCondition(ConditionResult.True, externalType1Value1, externalType1));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.True, internalTypeValue2));
        return transaction;
    }

    @NotNull
    private EMTransaction createTwowayHandshake2Transaction() {
        EMTransaction transaction = createClientTransaction(testClient2Address, testClient1Address,
                testClient1Address, 1261);
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.True, internalTypeValue2));
        transaction.addToValueConditionPair(createTypedValueCondition(ConditionResult.True, externalType1Value1, externalType1));
        return transaction;
    }

    @NotNull
    private EMTransaction createThreeTypesClient1Transaction() {
        EMTransaction transaction = createClientTransaction(testClient1Address, testClient3Address,
                testClient2Address, 1236);
        transaction.addIncomingValueConditionPair(createTypedValueCondition(ConditionResult.True, externalType2Value2, externalType2));
        transaction.addIncomingValueConditionPair(createTypedValueCondition(ConditionResult.False, externalType2Value1, externalType2));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.True, internalTypeValue1));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.False, internalTypeValue2));
        return transaction;
    }

    @NotNull
    private EMTransaction createThreeTypesClient2Transaction() {
        EMTransaction transaction = createClientTransaction(testClient2Address, testClient1Address,
                testClient3Address, 1236);
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.True, internalTypeValue1));
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.False, internalTypeValue2));
        transaction.addToValueConditionPair(createTypedValueCondition(ConditionResult.True, externalType1Value1, externalType1));
        transaction.addToValueConditionPair(createTypedValueCondition(ConditionResult.False, externalType1Value2, externalType1));
        return transaction;
    }

    @NotNull
    private EMTransaction createThreeTypesClient3Transaction() {
        EMTransaction transaction = createClientTransaction(testClient3Address, testClient2Address,
                testClient1Address, 1236);
        transaction.addIncomingValueConditionPair(createTypedValueCondition(ConditionResult.True, externalType1Value1, externalType1));
        transaction.addIncomingValueConditionPair(createTypedValueCondition(ConditionResult.False, externalType1Value2, externalType1));
        transaction.addToValueConditionPair(createTypedValueCondition(ConditionResult.True, externalType2Value1, externalType2));
        transaction.addToValueConditionPair(createTypedValueCondition(ConditionResult.False, externalType2Value2, externalType2));
        return transaction;
    }

    @NotNull
    private EMTransaction createNewClient3Transaction() {
        EMTransaction transaction = createClientTransaction(testClient3Address, testClient2Address,
                testClient1Address, 1234);
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.True, nClient2ToClient3));
        transaction.addIncomingValueConditionPair(createValueCondition(ConditionResult.False, 32));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.False, 21));
        transaction.addToValueConditionPair(createValueCondition(ConditionResult.True, nClient3ToClient1));
        return transaction;
    }

    @NotNull
    private EMTransaction createClientTransaction(String from, String incoming, String payment, int contractNumber) {
        EMTransaction transaction = new EMTransaction();
        transaction.setContractNumber(contractNumber);
        transaction.setFrom(from);
        transaction.setIncomingAddress(incoming);
        transaction.setPaymentAddress(payment);
        return transaction;
    }

    @NotNull
    private ValueCondition createValueCondition(ConditionResult result, int value) {
        return createValueCondition(result, value, "", "");
    }

    @NotNull
    private ValueCondition createTypedValueCondition(ConditionResult result,int value, String assetType) {
        return createValueCondition(result, value, "", "", assetType);
    }

    private ValueCondition createRatioValueCondition(ConditionResult initState, double ratio,
                                                     String conditionCommand, String expectResultString) {
        return createValueCondition(initState, ratio, conditionCommand, expectResultString, true, "");
    }

    private ValueCondition createValueCondition(ConditionResult initState, int value,
                                                String conditionCommand, String expectResultString){
        return createValueCondition(initState, value, conditionCommand, expectResultString, "");
    }

    private ValueCondition createValueCondition(ConditionResult initState, int value,
                                                String conditionCommand, String expectResultString, String assetType) {
        return createValueCondition(initState, value, conditionCommand, expectResultString, false, assetType);
    }

    private ValueCondition createValueCondition(ConditionResult initState, double value,
                                                String conditionCommand, String expectResultString,
                                                boolean isRatioPayment, String assetType) {
        Condition condition = createCondition(initState);
        condition.setConditionContract(conditionCommand);
        condition.setExpectResultString(expectResultString);

        ValueCondition valueCondition = null;
        if (null != assetType && !assetType.isEmpty()){
            valueCondition = new TypedValueCondition(value, condition, assetType);
        }
        else if (!isRatioPayment){
            valueCondition = new ValueCondition(value, condition);
        } else {
            valueCondition = new RatioValueCondition(value, condition);
        }
        return valueCondition;
    }

    private Condition createCondition(ConditionResult result) {
        Condition condition = new Condition();
        condition.setResult(result);
        return condition;
    }

    @Test
    public void processBalance() {
        final String userA = "A";
        final String userB = "B";
        final int userAInitValue = 60;
        final int userBInitValue = 70;
        final int contractNumber  = 1357;

        final String testGameServerIp = "127.0.0.1";

        accountManager.addValue(userA, userAInitValue);
        accountManager.addValue(userB, userBInitValue);

        EMTransaction transactionA2B = createClientTransaction(userA, userB, userB, contractNumber);
        transactionA2B.addIncomingValueConditionPair(
                createValueCondition(ConditionResult.Undefined, 20,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp, "win"));
        transactionA2B.addIncomingValueConditionPair(
                createValueCondition(ConditionResult.Undefined, 5,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp, "lost"));
        transactionA2B.addToValueConditionPair(createValueCondition(ConditionResult.True, 10));

        EMTransaction transactionB2A = createClientTransaction(userB, userA, userA, contractNumber);
        transactionB2A.addToValueConditionPair(
                createValueCondition(ConditionResult.Undefined, 20,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp, "win"));
        transactionB2A.addToValueConditionPair(
                createValueCondition(ConditionResult.Undefined, 5,
                        "node test\\conditionContract\\getServerContent.js " + testGameServerIp, "lost"));
        transactionB2A.addIncomingValueConditionPair(createValueCondition(ConditionResult.True, 10));

        System.out.printf("The init balance of A - B is: %f - %f\n", accountManager.getBalance(userA), accountManager.getBalance(userB));
        // simulate the frozen process
        accountManager.subValue(userA, transactionA2B.getMaxToValue());
        accountManager.subValue(userB, transactionB2A.getMaxToValue());
        System.out.printf("The frozen balance of A - B is: %f - %f\n", accountManager.getBalance(userA), accountManager.getBalance(userB));

        EMTransactionService service = EMTransactionService.getInstance();
        service.emTransactionServiceMap.addEMTransaction(transactionA2B);
        service.emTransactionServiceMap.addEMTransaction(transactionB2A);
        service.processBalance(contractNumber);
        System.out.printf("The last balance of A - B is: %f - %f\n", accountManager.getBalance(userA), accountManager.getBalance(userB));
    }
}
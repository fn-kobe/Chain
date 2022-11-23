package com.scu.suhong.transaction.exchangeMode;

import account.AccountManager;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EMServerTransaction extends EMTransaction {
    // Used only for matched asset
    Double realPaidValue = Double.valueOf(0);
    // Used for max possible allowed asset
    Double planUsedValue = Double.valueOf(0);
    List<EMTransaction> unprocessedClientTransactionList = new ArrayList<>();
    // Indicate whether the server will pay portion according to the client in-coming, instead of use fixed payment
    // Used in the server case: payment = in-coming from client * ratio
    boolean isPaymentRatioToIncoming = false;

    Double minAllowedValue = Double.valueOf(0);

    boolean isServerTransactionBeenShutdown = false;

    boolean useMinPaymentOptimization = false;

    public EMServerTransaction(String fromAddress) {
        setFrom(fromAddress);
    }

    public EMServerTransaction(EMTransaction emTransaction) {
        super(emTransaction);
    }

    public boolean prepareService(int prePaidValue) {
        return prepareService(Double.valueOf(prePaidValue));
    }

    public boolean prepareService(Double prePaidValue) {
        this.prePaidValue = prePaidValue;
        return AccountManager.getInstance().subValue(getFrom(), prePaidValue);
    }

    public void setPaymentRatioToIncoming(){
        isPaymentRatioToIncoming = true;
    }

    @Override
    // Indicate whether the server will pay portion according to the client in-coming, instead of use fixed payment
    // Used in the server case: payment = in-coming from client * ratio
    public boolean isPaymentRatioToIncoming(){
        return isPaymentRatioToIncoming;
    }

    public boolean isUseMinPaymentOptimization() {
        return useMinPaymentOptimization;
    }

    public void setUseMinPaymentOptimization(boolean useMinPaymentOptimization) {
        this.useMinPaymentOptimization = useMinPaymentOptimization;
    }

    // Return whether the pre-paid asset can afford the client request
    public boolean tryProcessClientTransaction(EMTransaction clientTransaction) {
        if (isServiceDone()) {
            System.out.println("[EMServerTransaction] server transaction has stopped. Cannot process any client transaction");
            showAllBalance(formatFailedMessage(clientTransaction));
            return false;
        }

        if (!clientTransaction.isValid()) {
            System.out.printf("[EMServerTransaction] The client transaction from %s is not valid\n", clientTransaction.getFrom());
            showAllBalance(formatFailedMessage(clientTransaction));
            return false;
        }

        Double maxWithdrawal = getMaxWithdrawal(clientTransaction);
        Double minPayment = Double.valueOf(0);
        if (useMinPaymentOptimization){
            minPayment = getMinPayment(clientTransaction);
        }
        if (!isPrePaidEnough(maxWithdrawal, minPayment)) {
            System.out.println("[EMServerTransaction] The prepaid money of the server is not enough");
            showAllBalance(formatFailedMessage(clientTransaction));
            return false;
        }

        // Currently. client payment is in real value mode.
        Double clientPaidValue = clientTransaction.getMinToMatchedCondition().getValue();
        if (!AccountManager.getInstance().subValue(clientTransaction.getFrom(), clientPaidValue)) {
            System.out.println("[EMServerTransaction] Cannot freeze the client account");
            showAllBalance(formatFailedMessage(clientTransaction));
            return false;
        }
        // for paper test data collection
        showAllBalance(formatPaidMessage(clientTransaction));

        processClientTransaction(clientTransaction, maxWithdrawal, minPayment);
        System.out.println("[EMServerTransaction][verification] After the transaction has been processed");
        return true;
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

    // for paper test data collection
    void showAllBalance(String flag){
        final  String server = "server";
        final String userA = "A";
        final String userB = "B";
        final String userC = "C";
        final String userD = "D";
        final String userE = "E";
        final String userF = "F";
        final String userG = "G";
        AccountManager accountManager = AccountManager.getInstance();
        System.out.printf("[EMServerTransaction][verification] The balance is %s\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\n",
                flag, accountManager.getBalance(server), accountManager.getBalance(userA),
                accountManager.getBalance(userB), accountManager.getBalance(userC),
                accountManager.getBalance(userD), accountManager.getBalance(userE),
                accountManager.getBalance(userF), accountManager.getBalance(userG));
    }

    private void processClientTransaction(EMTransaction clientTransaction, Double maxWithdrawal, Double minPayment) {
        planUsedValue += maxWithdrawal - minPayment;
        if (isConditionMatched(clientTransaction)) {
            handleBalance(clientTransaction);
        } else {
            unprocessedClientTransactionList.add(clientTransaction);
        }
    }

    private void processClientTransaction(EMTransaction clientTransaction, Double maxWithdrawal) {
        processClientTransaction(clientTransaction, maxWithdrawal, Double.valueOf(0));
    }

    void handleBalance(EMTransaction clientTransaction) {
        if (clientTransaction.hasIncomingRatioCondition()){
            handleRatioValueBalance(clientTransaction);
        } else {
            handleRealValueBalance(clientTransaction);
        }
        showAllBalance(formatWithdrawalMessage(clientTransaction));
    }

    private void handleRealValueBalance(EMTransaction clientTransaction) {
        double clientWithdrawalValue = clientTransaction.getMinIncomingMatchedCondition().getValue();
        double clientPaidValue = clientTransaction.getMinToMatchedCondition().getValue();
        System.out.printf("[EMServerTransaction][Debug] Exchange mode transaction's condition is matched, " +
                "try to transfer asset value: %f with exchange asset: %f\n", clientWithdrawalValue, clientPaidValue);
        // Pay asset to the client: 1. first count the payment of the server ; 2. Add the asset to the client
        // As the server and client balance is frozen before, then here only add the withdrawal
        AccountManager.getInstance().addValue(clientTransaction.getFrom(), clientTransaction.getOutgoingAssetType(), (int) clientWithdrawalValue);
        // To avoid minus prepayment, then we only add the minimum client payment to the prepaid
        processServerBalance(clientTransaction, clientWithdrawalValue, clientPaidValue);
    }

    // Current design: in server-client mode, the client payment is real value; and the withdrawal can be ratio.
    private void handleRatioValueBalance(EMTransaction clientTransaction) {
        double clientPaidValue = clientTransaction.getMinToMatchedCondition().getValue();
        // In ratio transaction, value in the transaction is the ratio.
        double clientWithdrawalValue = clientTransaction.getMinIncomingMatchedCondition().getValue() * clientPaidValue;
        System.out.printf("[EMServerTransaction][Debug] Ratio transaction's condition is matched, " +
                "try to transfer asset value: %f with exchange asset: %f\n", clientWithdrawalValue, clientPaidValue);
        // Pay asset to the client: 1. first count the payment of the server ; 2. Add the asset to the client
        // As the server and client balance is frozen before, then here only add the withdrawal
        AccountManager.getInstance().addValue(clientTransaction.getFrom(), (int) clientWithdrawalValue);
        // To avoid minus prepayment, then we only add the minimum client payment to the prepaid
        processServerBalance(clientTransaction, clientWithdrawalValue, clientPaidValue);
    }


    private void processServerBalance(EMTransaction clientTransaction, double clientWithdrawalValue, double clientPaidValue) {
        if (useMinPaymentOptimization) {
            // To avoid minus prepayment, then we only add the minimum client payment to the prepaid
            // This is done by less add the payment to the server balance, then we can sub the real payment
            double clientMinPaymentValue = clientTransaction.getMinPayment();
            realPaidValue += clientWithdrawalValue - clientMinPaymentValue;
            // process the server balance
            AccountManager.getInstance().addValue(getFrom(), clientTransaction.getOutgoingAssetType(), clientPaidValue - clientMinPaymentValue);
        } else {
            realPaidValue += clientWithdrawalValue;
            // process the server balance
            AccountManager.getInstance().addValue(getFrom(), clientTransaction.getOutgoingAssetType(), clientPaidValue);
        }
    }

    int processUnprocessedClientTransactions() {
        if (isServiceDone()) return 0;

        int processedTransactionNumber = 0;
        Iterator<EMTransaction> it = unprocessedClientTransactionList.iterator();
        while (it.hasNext()) {
            EMTransaction t = it.next();
            if (isConditionMatched(t)) {
                handleBalance(t);
                it.remove();
                ++processedTransactionNumber;
            }
        }

        return processedTransactionNumber;
    }

    boolean isConditionMatched(EMTransaction t) {
        ValueCondition valueToCondition = t.getToMatchedCondition();
        ValueCondition valueFromCondition = t.getIncomingMatchedCondition();
        if (null != valueFromCondition && null != valueToCondition) {
            return true;
        } else {
            return false;
        }
    }

    public boolean canPerformService() {
        if (isServiceDone()) {
            return false;
        }

        if (prePaidValue - getMinAllowedValue() <= realPaidValue) {
            System.out.printf("[EMServerTransaction][Debug] Service has been shutdown, "
                    + "due to no more prepaid asset with minimum allowed value %f\n", getMinAllowedValue());
            return false;
        }

        return true;
    }

    // If ratio server, there is no real to value, have to set the min allowed value
    public Double getMinAllowedValue() {
        if (0 == minAllowedValue) return getMaxToValue();
        else return minAllowedValue;
    }

    public void setMinAllowedValue(Double minAllowedValue) {
        this.minAllowedValue = minAllowedValue;
    }

    public boolean isServiceDone() {
        if (isServerTransactionBeenShutdown) {
            System.out.println("[EMServerTransaction][Debug] Service has been shutdown");
            return true;
        }

        return false;
    }

    public void shutDownService() {
        if (isServerTransactionBeenShutdown) {
            System.out.println("[EMServerTransaction][Info] service has already been shut down.");
            return;
        }
        // Try to process as many as client request as possible
        processUnprocessedClientTransactions();
        if (getUnprocessedClientTransactionNumber() > 0) {
            System.out.printf("[EMServerTransaction][Debug] When shut down, " +
                    "there are %d unprocessed transaction\n", getUnprocessedClientTransactionNumber());
        }
        isServerTransactionBeenShutdown = true;
        if (!AccountManager.getInstance().addValue(getFrom(), prePaidValue - realPaidValue)){
            System.out.println("[EMServerTransaction][Warning] The balance is not enough now");
        }
        prePaidValue = realPaidValue;
    }

    int getUnprocessedClientTransactionNumber() {
        return unprocessedClientTransactionList.size();
    }

    Double getMaxWithdrawal(EMTransaction transaction) {
        return transaction.getMaxWithdrawal();
    }

    Double getMinPayment(EMTransaction transaction) {
        return transaction.getMinPayment();
    }

    synchronized boolean isPrePaidEnough(Double maxWithdrawal, Double minPayment) {
        if (prePaidValue >= planUsedValue + maxWithdrawal - minPayment) {
            System.out.println("[EMServerTransaction][Debug] Prepaid asset is enough to process client transaction.");
            return true;
        }
        return false;
    }

    // When calculate the hash, it will also use getting hash
    public JSONObject getJsonWithoutHash() {
        JSONObject json = super.getJsonWithoutHash();

        return json;
    }

    public static EMServerTransaction fromJson(String jsonString) {
        JSONObject object = new JSONObject(jsonString);
        if (!object.has("incoming")) { // One transaction must have Data
            return null;
        }
        EMTransaction emTransaction = EMTransaction.fromJson(object);
        EMServerTransaction serverTransaction = new EMServerTransaction(emTransaction);
        return serverTransaction;
    }

    public int compareTo(Object o) {
        if (!(o instanceof EMTransaction)) {
            return 1;
        }
        EMTransaction otherTransaction = (EMTransaction) o;
        return this.getHash().compareTo(otherTransaction.getHash());
    }
}

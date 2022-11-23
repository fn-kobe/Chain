package com.scu.suhong.transaction.exchangeMode;

import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.transaction.Transaction;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.util.List;

public class EMTransaction extends com.scu.suhong.transaction.Transaction {
    Double prePaidValue = Double.valueOf(0);
    // as the address is not the same as in the Transaction
    // Here we redefine the from and to
    private String incomingAddress;
    private String paymentAddress;
    String hash = "";
    // It is used to differ different transactions loop.
    // Suppose that there are two payments from A to B while each of them is not
    // the same contract. Then this used to differ that case.
    // The transaction with the same contractNumber will be put into one ring
    private int contractNumber = -1;
       // Whether one transaction has one in-coming pair or not for ratio transaction
    private boolean isMatched = false;
    //used in the balance calculation
    double calculatedPaid = 0;
    double calculatedIncoming = 0;
    // Case as if value > 5
    boolean isIncomingValueRange = false;
    // This value is true after the asset is frozen and before the unfrozen
    private boolean shouldReturnFrozenBalance = false;
    int loopNumber = 0;
    // One transaction can be repeat with the times field
    int times = 1;

    ValueConditionList incomingValueConditionList;
    ValueConditionList toValueConditionList;

    public EMTransaction() {
        incomingAddress = "";
        paymentAddress = "";
        this.incomingValueConditionList = new ValueConditionList();
        this.toValueConditionList = new ValueConditionList();
    }

    public EMTransaction(EMTransaction emTransaction) {
        super(emTransaction);
        this.incomingValueConditionList = emTransaction.incomingValueConditionList;
        this.toValueConditionList = emTransaction.toValueConditionList;
        this.incomingAddress = emTransaction.incomingAddress;
        this.prePaidValue = emTransaction.prePaidValue;
    }

    public EMTransaction(Transaction transaction, ValueConditionList incoming,
                         ValueConditionList to, String incomingAddress,
                         Double prePaidValue, int contractNumber) {
        super(transaction);
        this.incomingValueConditionList = incoming;
        this.toValueConditionList = to;
        this.incomingAddress = incomingAddress;
        this.prePaidValue = prePaidValue;
        this.contractNumber = contractNumber;
    }

    public boolean hasRatioCondition() {
        return hasToRatioCondition() || hasIncomingRatioCondition();
    }

    // Currently, only need in server transaction
    // Indicate whether the server will pay portion according to the client in-coming, instead of use fixed payment
    // Used in the server case: payment = in-coming from client * ratio
    public boolean isPaymentRatioToIncoming(){
        return false;
    }

    public boolean hasToRatioCondition() {
        List<ValueCondition> valueConditions = toValueConditionList.valueConditions;
        for (ValueCondition v : valueConditions){
            if (v instanceof  RatioValueCondition) return true;
        }
        return false;
    }

    public boolean isIncomingValueRange(){
        return isIncomingValueRange;
    }

    // Assert all incoming asset has the same type. Then we get the type from the first one
    // If this is not the case latter, change it
    // Fetch the type from the first value condition pair
    public String getIncomingAssetType() {
        return getFirstValueConditionAssetType(incomingValueConditionList);
    }

    // Assert all outgoing asset has the same type. If this is not the case latter, change it
    public String getOutgoingAssetType() {
        return getFirstValueConditionAssetType(toValueConditionList);
    }

     String getFirstValueConditionAssetType(ValueConditionList valueConditionList) {
        List<ValueCondition> valueConditions = valueConditionList.valueConditions;
        if (valueConditions.isEmpty()) return "";

        ValueCondition firstValueCondition = valueConditions.get(0);
        if (!(firstValueCondition instanceof  TypedValueCondition)) return "";

        return ((TypedValueCondition) firstValueCondition).getAssetType();
    }

    public boolean hasIncomingRatioCondition() {
        List<ValueCondition> valueConditions = incomingValueConditionList.valueConditions;
        for (ValueCondition v : valueConditions){
            if (v instanceof  RatioValueCondition) return true;
        }
        return false;
    }

    @Override
    public boolean isValid() {
        if (! super.isValid()) return false;
        if (incomingAddress.isEmpty() || paymentAddress.isEmpty()){
            System.out.println("[EMTransaction] There is exchange address");
            return false;
        }

        if (hasToRatioCondition() && prePaidValue <= 0){
            System.out.println("[EMTransaction][Warning] Has ratio payment while no pre-paid asset");
            return false;
        }

        if (-1 == contractNumber) return false;
        if (toValueConditionList.getValueConditions().isEmpty()
                || incomingValueConditionList.getValueConditions().isEmpty()){
            System.out.println("[EMTransaction][Warning] oValueConditionList or incomingValueConditionList is empty");
            return false;
        }

        return true;
    }

    @Override
    public String getTo() {
        return paymentAddress;
    }

    public void setPaymentAddress(String paymentAddress) {
        this.paymentAddress = paymentAddress;
    }

    public void setIncomingValueConditionList(ValueConditionList incomingValueConditionList) {
        this.incomingValueConditionList = incomingValueConditionList;
    }

    // Used to check whether the value and condition is the same or not between to transaction
    // Notice that one to may be another one's incoming, then we sort the order
    public String getValueConditionHash(){
        try {
            String toString = toValueConditionList.toString();
            String incomingString = incomingValueConditionList.toString();
            String result = "";
            if (0 < toString.compareTo(incomingString)) result = toString + incomingString;
            else result = incomingString + toString;
            return MD5Hash.getValue(result);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getToValueConditionHash(){
        try {
            return MD5Hash.getValue(toValueConditionList.toString());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getIncomingValueConditionHash(){
        try {
            return MD5Hash.getValue(incomingValueConditionList.toString());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public int getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(int contractNumber) {
        this.contractNumber = contractNumber;
    }

    public int getLoopNumber() {
        return loopNumber;
    }

    public void setLoopNumber(int loopNumber) {
        toValueConditionList.setLoopNumber(getLoopNumber());
        incomingValueConditionList.setLoopNumber(getLoopNumber());
        this.loopNumber = loopNumber;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public double getMaxFrozenValue(){
        if (hasToRatioCondition()) return prePaidValue;

        return toValueConditionList.getMaxValue() > prePaidValue ? toValueConditionList.getMaxValue() : prePaidValue;
    }

    public String getIncomingAddress() {
        return incomingAddress;
    }

    public void setIncomingAddress(String incomingAddress) {
        this.incomingAddress = incomingAddress;
    }

    public ValueCondition getToMatchedCondition() {
        return toValueConditionList.getMatchedCondition();
    }

    // The ratio is only for payment not for the incoming, as incoming is decided by others.
    public double getMaxPaymentRatio(){
        return toValueConditionList.getMaxValue();
    }

    public double getMaxToValue(){
        return toValueConditionList.getMaxValue();
    }

    public double getMinToValue(){
        return toValueConditionList.getMinValue();
    }

    public ValueCondition getMaxToMatchedCondition() {
        return toValueConditionList.getMaxMatchedCondition();
    }

    public ValueCondition getMinToMatchedCondition() {
        return toValueConditionList.getMinMatchedCondition();
    }

    public ValueConditionList getToCondition() {
        return toValueConditionList;
    }

    public ValueConditionList getFromCondition() {
        return toValueConditionList;
    }

    public boolean isConditionMatched(){
        if (null == getToMatchedCondition() || null ==getIncomingMatchedCondition()) return false;

        return true;
    }

    public ValueCondition getIncomingMatchedCondition() {
        return incomingValueConditionList.getMatchedCondition();
    }

    public double getMaxIncomingValue() {
        return incomingValueConditionList.getMaxValue();
    }

    public ValueCondition getMaxIncomingMatchedCondition() {
        return incomingValueConditionList.getMaxMatchedCondition();
    }

    public ValueCondition getMinIncomingMatchedCondition() {
        return incomingValueConditionList.getMinMatchedCondition();
    }

    public ValueConditionList getIncomingValueConditionList() {
        return incomingValueConditionList;
    }

    public Double getMaxWithdrawal() {
        Double maxWithdrawal = Double.valueOf(0);
        for (ValueCondition valueCondition: incomingValueConditionList.getValueConditions()){
            if (valueCondition.getValue() > maxWithdrawal) maxWithdrawal = valueCondition.getValue();
        }
        // The valuecondition should be either ratio or realvalue all instead of mixed.
        if (hasIncomingRatioCondition()){
            maxWithdrawal *= getMaxToValue();
        }
        return maxWithdrawal;
    }

    public Double getMinPayment() {
        Double minPayment = Double.MAX_VALUE;
        for (ValueCondition valueCondition: toValueConditionList.getValueConditions()){
            if (valueCondition.getValue() < minPayment) minPayment = valueCondition.getValue();
        }
        // The payment is not ratio in current design
        return minPayment;
    }

    public ValueConditionList getToValueConditionList() {
        return toValueConditionList;
    }

    public boolean addToValueConditionPair(ValueCondition valueCondition) {
        toValueConditionList.addValueConditionPair(valueCondition);
        return true;
    }

    public void addIncomingValueConditionPair(ValueCondition valueCondition) {
        incomingValueConditionList.addValueConditionPair(valueCondition);
    }

    public Double getPrePaidValue() {
        return prePaidValue;
    }

    public void setPrePaidValue(int prePaidValue) {
        setPrePaidValue(Double.valueOf(prePaidValue));
    }

    public void setPrePaidValue(Double prePaidValue) {
        this.prePaidValue = prePaidValue;
    }

    public boolean isMatched() {
        return isMatched;
    }

    public void setMatched() {
        isMatched = true;
    }

    public boolean isShouldReturnFrozenBalance() {
        return shouldReturnFrozenBalance;
    }

    public void setShouldReturnFrozenBalance(boolean shouldReturnFrozenBalance) {
        this.shouldReturnFrozenBalance = shouldReturnFrozenBalance;
    }

    public String Dump() {
        return getJson().toString();
    }

    public JSONObject getJson() {
        JSONObject json = getJsonWithoutHash();
        json.put("hash", getHash());
        return json;
    }

    // When calculate the hash, it will also use getting hash
    public JSONObject getJsonWithoutHash() {
        JSONObject json = new JSONObject();
        json.put("normal", super.getJsonWithoutHash());
        json.put("incoming",incomingValueConditionList.getJson());
        json.put("to",toValueConditionList.getJson());
        json.put("incomingAddress", incomingAddress);
        json.put("prePaidValue", prePaidValue);
        json.put("contractNumber", contractNumber);
        return json;
    }

    public static EMTransaction fromJson(String jsonString){
        JSONObject object = new JSONObject(jsonString);
        return fromJson(object);
    }

    @Nullable
    public static EMTransaction fromJson(JSONObject object) {
        if (!object.has("incoming")) { // One transaction must have Data
            return null;
        }
        Transaction transaction = Transaction.createFromJson((JSONObject) object.get("normal"));
        EMTransaction emTransaction = new EMTransaction(transaction,
                ValueConditionList.fromJson((JSONObject) object.get("incoming")),
                ValueConditionList.fromJson((JSONObject) object.get("to")),
                object.getString("incomingAddress"),
                object.getDouble("prePaidValue"),
                object.getInt("contractNumber"));
        return emTransaction;
    }

    public int compareTo(Object o) {
        if (!(o instanceof EMTransaction)) {
            return 1;
        }
        EMTransaction otherTransaction = (EMTransaction) o;
        return this.getHash().compareTo(otherTransaction.getHash());
    }

    public String getHash() {
        if (hash == null || hash.isEmpty()) {
            calculateDataHash();
        }
        return hash;
    }

    public String calculateDataHash() {
        try {
            hash = MD5Hash.getValue(getJsonWithoutHash().toString());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            hash = "";
        }
        super.setHash(hash);
        return hash;
    }
}

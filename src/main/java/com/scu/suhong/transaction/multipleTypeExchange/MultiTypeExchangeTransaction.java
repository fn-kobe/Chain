package com.scu.suhong.transaction.multipleTypeExchange;

import account.AccountManager;
import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.network.P2PConfiguration;
import com.scu.suhong.transaction.CrosschainMultiTypeExchangeTransaction;
import com.scu.suhong.transaction.Transaction;
import com.scu.suhong.transaction.exchangeMode.ConditionResult;
import consensus.pow.MiningConfiguration;
import org.json.JSONArray;
import org.json.JSONObject;
import util.FileLogger;
import util.JSONObjectHelper;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class MultiTypeExchangeTransaction extends Transaction {
    static org.apache.log4j.Logger logger = FileLogger.getLogger();

    int exchangeId = 0;
    String chainID = "";
    String requiredTxListType = "fixed";//fixed or variable or req for required tx
    private List<RequiredData> requiredDataList = new ArrayList<>();
    // Currently, we onnly support one command. If want 'or', 'and' and so, please change this
    ExchangeExternalCondition condition = null;

    public MultiTypeExchangeTransaction(int exchangeId) {
        this.exchangeId = exchangeId;
        chainID = MiningConfiguration.getBlockchainStringId();
    }

    public MultiTypeExchangeTransaction(int exchangeId, String chainID) {
        this.exchangeId = exchangeId;
        this.chainID = chainID;
    }

    public MultiTypeExchangeTransaction(int exchangeId, String chainID, String type) {
        this.exchangeId = exchangeId;
        this.chainID = chainID;
        this.requiredTxListType = type;
    }

    // constructor for create from json
    public MultiTypeExchangeTransaction(Transaction transaction, int exchangeId, String chainID) {
        super(transaction);
        this.exchangeId = exchangeId;
        this.chainID = chainID;
    }

    // constructor for create from json
    public MultiTypeExchangeTransaction(Transaction transaction, String type, int exchangeId, String chainID) {
        super(transaction);
        this.exchangeId = exchangeId;
        this.chainID = chainID;
        this.requiredTxListType = type;
    }

    public String getRequiredTxListType() {
        return requiredTxListType;
    }

    public boolean doesMatchRequired(RequiredData requiredData){
        RequiredData appearedData = toRequiredData();
        if (! (requiredData.getExchangeId() == appearedData.getExchangeId())) return false;
        if (! (requiredData.getChainID().equals(appearedData.getChainID()))) return false;
        // We allow from is any address
        if (!requiredData.getFrom().isEmpty() && !doesAddressMatch(requiredData.getFrom(), appearedData.getFrom())) return false;
        if (!doesAddressMatch(requiredData.getTo(), appearedData.getTo())) return false;
        if (!requiredData.getAssetType().equals(appearedData.getAssetType())) return false;
        if (! (requiredData.getValue() == appearedData.getValue())) return false;

        return true;
    }

    // Before this invocation, please chainID is compared
    boolean doesAddressMatch(String required, String appeared){
        if (required.isEmpty()) return true;
        return AccountManager.getShortAddress(required).equals(AccountManager.getShortAddress(appeared));
    }

    public boolean doesMatch(RequiredData requiredData){
        return this.toRequiredData().doesMatch(requiredData);
    }

    // used to judge whether two transaction express the same transaction from the user
    // as it may be sealed by two different nodes
    public boolean doesMatch(MultiTypeExchangeTransaction transaction){
        if (this.getId() == transaction.getId() && this.getBlockchainId().equals(transaction.getBlockchainId())) return true;

        // Translate required data to compare
        return toRequiredData().doesMatch(transaction.toRequiredData());
    }

    static public int safeGetPositiveInt(JSONObject object, String key) {
        int value = -1;
        try {
            value = JSONObjectHelper.safeGetInt(object, key);
        } catch (NumberFormatException e) {
            logger.warn("[Condition] No valid value in condition " + JSONObjectHelper.safeGetString(object, key));
        }
        return value;
    }

    static public MultiTypeExchangeTransaction createFromJson(JSONObject exTx) {
        if (!exTx.has("reqTxList")) { // One transaction must have Data
            return null;
        }

        int interactionId = safeGetPositiveInt(exTx, "exchangeId");
        String chainID = JSONObjectHelper.safeGetString(exTx, "chainID");
        String requiredTxListType = JSONObjectHelper.safeGetString(exTx, "requiredTxListType");
        Transaction transaction = Transaction.createFromJson((JSONObject) exTx.get("super"));
        MultiTypeExchangeTransaction multiTypeExchangeTransaction = new MultiTypeExchangeTransaction(transaction, requiredTxListType, interactionId, chainID);

        JSONArray reqTxList = (JSONArray) exTx.get("reqTxList");
        for (int i = 0; i < reqTxList.length(); ++i){
            multiTypeExchangeTransaction.addRequiredData(RequiredData.createFromJson((JSONObject) reqTxList.get(i)));
        }

        JSONObject conditionObject = (JSONObject) exTx.get("condition");
        ExchangeExternalCondition condition = ExchangeExternalCondition.createFromJson(conditionObject);
        multiTypeExchangeTransaction.setCondition(condition);

        return multiTypeExchangeTransaction;
    }

    // We will use full name for external transactions
    @Override
    public void setFrom(String from) {
        if (null == from || from.isEmpty()) {
            System.out.println("[MultiTypeExchangeTransaction] Sender address is empty and we skip setting it");
            return;
        }

        super.setFrom(AccountManager.getFullAddress(from, chainID));
    }

    @Override
    public void setToAndValue(String to, int value) {
        if (null == to || to.isEmpty()) {
            System.out.println("[MultiTypeExchangeTransaction] Receiver address is empty and we skip setting it ");
            return;
        }

        super.setToAndValue(AccountManager.getFullAddress(to, chainID), value);
    }

    public int getExchangeId() {
        return exchangeId;
    }

    public String getChainID() {
        return chainID;
    }

    public List<RequiredData> getCopiedRequiredDataList() {
        List<RequiredData> requiredDataList = new ArrayList<>();
        for (RequiredData data : this.requiredDataList){
            requiredDataList.add(data);
        }
        return requiredDataList;
    }

    RequiredData toRequiredData(){
        return new RequiredData(exchangeId, getChainID(), getFrom(), getTo(), getAssetType(), getValue());
    }

    public List<RequiredData> getRequiredDataListWithSelf() {
        List<RequiredData> multiTypeExchangeTransactionList = new ArrayList<>();
        multiTypeExchangeTransactionList.addAll(requiredDataList);
        addRequiredData(multiTypeExchangeTransactionList, toRequiredData());
        return multiTypeExchangeTransactionList;
    }

    // we will set unique transaction
    public void addRequiredData(List<RequiredData> requiredDataList, RequiredData requiredData) {
        for (RequiredData t : requiredDataList){
            if (t.doesMatch(requiredData)){
                System.out.println("[MultiTypeExchangeTransaction][addRequiredData] find same required transaction. Skip add.");
                return;
            }
        }
        requiredDataList.add(requiredData);
    }

    // we will set unique transaction
    public void addRequiredData(RequiredData requiredData) {
        if (requiredData.getTo().isEmpty()) requiredData.setTo(getTo());

        addRequiredData(requiredDataList, requiredData);
    }

    @Override
    public String Dump() {
        return getJson().toString();
    }

    @Override
    public JSONObject getJson() {
        JSONObject json = getSelfJson();

        JSONArray jsonReqTxArray = new JSONArray();
        if (null != requiredDataList) {
            for (RequiredData d : requiredDataList)
            jsonReqTxArray.put(d.getJson());
        }
        json.put("reqTxList", jsonReqTxArray);

        JSONObject conditionObject = new JSONObject();
        if (null != condition){
            conditionObject = condition.getJson();
        }
        json.put("condition", conditionObject);

        return json;
    }

    JSONObject getSelfJson() {
        JSONObject json = new JSONObject();
        json.put("exchangeId", exchangeId);
        json.put("chainID", chainID);
        json.put("requiredTxListType", requiredTxListType);
        json.put("super", super.getJson());
        return json;
    }

    public int compareTo(Object o) {
        if (!(o instanceof MultiTypeExchangeTransaction)) {
            return 1;
        }
        MultiTypeExchangeTransaction otherTransaction = (MultiTypeExchangeTransaction) o;
        return this.getHash().compareTo(otherTransaction.getHash());
    }

    public String getHash() {
        if (hash == null || hash.isEmpty()) {
            hash = calculateDataHash();
        }
        return hash;
    }

    public String calculateDataHash() {
        String h = super.calculateDataHash();
        try {
            h = MD5Hash.getValue(String.valueOf(exchangeId + h));
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Cannot find MD5 algorithm in makeHash");
            e.printStackTrace();
        }
        return h;
    }

    public void setRequiredTxListType(String requiredTxListType) {
        this.requiredTxListType = requiredTxListType;
    }

    public void setCondition(ExchangeExternalCondition condition) {
        this.condition = condition;
    }

    ConditionResult externalConditionResult = null;
    public boolean isExternalConditionDone(){
        if (null == condition) {
            return true;
        }
        externalConditionResult = condition.doesExternalConditionMatch();
        if (externalConditionResult.equals(ConditionResult.Ongoing) || externalConditionResult.equals(ConditionResult.Undefined)) return false;

        return true;
    }

    public boolean doesExternalConditionMatch() {
        if (null == condition) {
            return true;
        }

        if (externalConditionResult.equals(ConditionResult.True)) return true;

        return false;
    }
}
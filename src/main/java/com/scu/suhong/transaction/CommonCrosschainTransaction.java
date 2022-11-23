package com.scu.suhong.transaction;

import com.scu.suhong.Hash.MD5Hash;
import consensus.pow.MiningConfiguration;
import org.json.JSONObject;
import util.JSONObjectHelper;

import java.security.NoSuchAlgorithmException;

public class CommonCrosschainTransaction extends Transaction implements CrosschainInterface {
    int interactionId = 0;

    public CommonCrosschainTransaction() {
    }

    public CommonCrosschainTransaction(int interactionId) {
        this.interactionId = interactionId;
    }

    public CommonCrosschainTransaction(Transaction transaction, int interactionId) {
        super(transaction);
        this.interactionId = interactionId;
    }

    static public CommonCrosschainTransaction createFromJson(String jsonString) {
        return createFromJson(new JSONObject(jsonString));
    }

    static public CommonCrosschainTransaction createFromJson(JSONObject exTx) {
        if (!exTx.has("CommonCrosschainTransaction")) { // One transaction must have Data
            return null;
        }

        int interactionId = safeGetPositiveInt(exTx, "interactionId");
        Transaction transaction = Transaction.createFromJson((JSONObject) exTx.get("super"));
        return new CommonCrosschainTransaction(transaction, interactionId);
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

    @Override
    public String Dump() {
        return getJson().toString();
    }

    public String toString(){
        return getJson().toString();
    }

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("CommonCrosschainTransaction", "CommonCrosschainTransaction");
        json.put("interactionId", interactionId);
        json.put("super", super.getJson());
        return json;
    }

    public int compareTo(Object o) {
        if (!(o instanceof CommonCrosschainTransaction)) {
            return 1;
        }
        CommonCrosschainTransaction otherTransaction = (CommonCrosschainTransaction) o;
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
            h = MD5Hash.getValue(String.valueOf(interactionId + h));
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Cannot find MD5 algorithm in makeHash");
            e.printStackTrace();
        }
        return h;
    }

    public void setInteractionId(int interactionId) {
        this.interactionId = interactionId;
    }

    public int getInteractionId() {
        return interactionId;
    }
}

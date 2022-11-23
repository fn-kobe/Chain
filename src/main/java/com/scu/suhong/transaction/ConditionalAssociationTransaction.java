package com.scu.suhong.transaction;

import com.scu.suhong.Hash.MD5Hash;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import util.FileLogger;

import java.security.NoSuchAlgorithmException;

public class ConditionalAssociationTransaction extends Transaction {
    static Logger logger = FileLogger.getLogger();

    int interactionId = 0;
    Condition condition;
    boolean isNormalTransaction; // normal transaction can also be regarded as one special conditional Tx

    public ConditionalAssociationTransaction(int interactionId, Condition condition) {
        this.interactionId = interactionId;
        from = condition.getFrom();
        this.condition = condition;
        toValuePair = condition.geToValuePair();
        isNormalTransaction = false;
    }

    public ConditionalAssociationTransaction(Transaction transaction, int interactionId, Condition condition) {
        super(transaction);
        this.interactionId = interactionId;
        this.condition = condition;
        isNormalTransaction = false;
    }

    public ConditionalAssociationTransaction(Condition condition, boolean isNormalTransaction) {
        this.condition = condition;
        this.isNormalTransaction = isNormalTransaction;
    }

    static public ConditionalAssociationTransaction createFromJson(JSONObject object) {
        if (!object.has("ConditionalAssociationTransaction")) { // One transaction must have Data
            return null;
        }
        Condition condition = Condition.createFromJson((JSONObject) object.get("condition"));
        Transaction transaction = Transaction.createFromJson((JSONObject) object.get("normal"));
        int interactionId = object.getInt("interactionId");

        ConditionalAssociationTransaction conditionalAssociationTransaction = new ConditionalAssociationTransaction(transaction, interactionId, condition);
        conditionalAssociationTransaction.setInteractionId(interactionId);
        return conditionalAssociationTransaction;
    }

    public Condition getCondition() {
        return condition;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public boolean isDependent(ConditionalAssociationTransaction anotherCTx) {
        if (anotherCTx.condition.getTo().equals(condition.getFrom())) return true;
        return false;
    }

    public String Dump() {
        String dump = "<condition>\n";
        dump += "from:" + from + "\n";
        dump += "to:" + toValuePair.Dump() + "\n";
        dump += "value:" + getValue() + "\n";
        dump += "interactionId:" + getInteractionId() + "\n";
        dump += "</condition>\n";
        return super.Dump(dump);
    }

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("ConditionalAssociationTransaction", "ConditionalAssociationTransaction");
        json.put("normal", super.getJson());
        json.put("interactionId",getInteractionId());
        json.put("condition", condition.getJson());
        return json;
    }

    public int compareTo(Object o) {
        if (!(o instanceof ConditionalAssociationTransaction)) {
            return 1;
        }
        ConditionalAssociationTransaction otherTransaction = (ConditionalAssociationTransaction) o;
        return this.getHash().compareTo(otherTransaction.getHash());
    }

    public String getHash() {
        if (hash == null || hash.isEmpty()) {
            hash = calculateDataHash();
        }
        return hash;
    }

    @Override
    public String calculateDataHash() {
        String h = super.calculateDataHash();
        h += condition.calculateDataHash();
        try {
            h += MD5Hash.getValue(String.valueOf(interactionId));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        return h;
    }

    public int getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(int interactionId) {
        this.interactionId = interactionId;
    }
}

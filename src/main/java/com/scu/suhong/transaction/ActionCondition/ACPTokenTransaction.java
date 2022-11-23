package com.scu.suhong.transaction.ActionCondition;

import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.CrosschainInterface;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import util.JSONObjectHelper;

import java.security.NoSuchAlgorithmException;

public class ACPTokenTransaction extends ACPCommonTransaction implements CrosschainInterface {
    // the transaction associated condition, which specifies how many actions can happen
    String conditionName = "";

    public ACPTokenTransaction() {
        specifiedDerivedClassName = "ACPTokenTransaction";
    }

    public static ACPTokenTransaction createFromJson(JSONObject object) {
        ACPTokenTransaction acpTokenTransaction = new ACPTokenTransaction();
        if (!initFromJson(object, acpTokenTransaction)) return null;
        return acpTokenTransaction;
    }

    public static boolean initFromJson(JSONObject object, ACPTokenTransaction acpTokenTransaction) {
        if (!object.has("ACPTokenTransaction")) { // One transaction must have Data
            return false;
        }
        if (!ACPCommonTransaction.initFromJson(object, acpTokenTransaction)) return false;

        acpTokenTransaction.setConditionName(JSONObjectHelper.safeGetString(object, "conditionName"));
        return true;
    }

    public String getConditionName() {
        return conditionName;
    }

    public void setConditionName(String conditionName) {
        this.conditionName = conditionName;
    }

    public void copy(ACPTokenTransaction to, ACPTokenTransaction from) {
        ACPCommonTransaction.copy(to, from);
        setConditionName(from.getConditionName());
    }

    @Override
    public JSONObject getJson() {
        return getJson(this);
    }

    static public JSONObject getJson(ACPTokenTransaction t) {
        JSONObject json = ACPCommonTransaction.getJson(t);
        json.put("ACPTokenTransaction", "ACPTokenTransaction");// as the identifier used in createfromjson method
        json.put("conditionName", t.getConditionName());
        return json;
    }

    @Override
    public String calculateDataHash(ACPCommonTransaction t) {
        String h = super.calculateDataHash(t);
        if (h.isEmpty()) return ""; // error

        try {
            h = MD5Hash.getValue("ACPTokenTransaction" + h);
            if (null != getConditionName()) {
                h = MD5Hash.getValue(getConditionName() + h);
            }
            return h;
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Cannot find MD5 algorithm in makeHash");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isSimilar(AbstractTransaction anotherTransaction) {
        ACPTokenTransaction at = (ACPTokenTransaction) anotherTransaction;
        if (!super.isSimilar(anotherTransaction)) return false;
        if (!getConditionName().equals(at.getConditionName())) return false;

        return true;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (!(o instanceof ACPTriggerTransaction)) {
            return 1;
        }
        ACPTriggerTransaction otherTransaction = (ACPTriggerTransaction) o;
        return this.getHash().compareTo(otherTransaction.getHash());
    }
}

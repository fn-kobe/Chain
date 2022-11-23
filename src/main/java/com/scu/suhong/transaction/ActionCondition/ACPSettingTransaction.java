package com.scu.suhong.transaction.ActionCondition;

import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import util.JSONObjectHelper;

import java.security.NoSuchAlgorithmException;

public class ACPSettingTransaction extends ACPCommonTransaction {
    // Parameter to select incoming condition for action
    String actionType = "";
    String incomingConditionName = "";
    String incomingConditionNameSender = "";
    String incomingConditionBlockchainID = "";
    String outgoingConditionName ="";
    String successiveActionMaxWaitingTime;
    String successiveActionTotalPeerNumber;
    String successiveActionMaxAllowedPeerNumber;

    public ACPSettingTransaction() {
        specifiedDerivedClassName = "ACPActionParameterSetTransaction";
    }

    public static ACPSettingTransaction createFromJson(JSONObject object) {
        ACPSettingTransaction t = new ACPSettingTransaction();
        if (!initFromJson(object, t)) return null;
        return t;
    }

    public static boolean initFromJson(JSONObject object, ACPSettingTransaction t) {
        if (!object.has("ACPActionParameterSetTransaction")) { // One transaction must have Data
            return false;
        }
        if (!ACPCommonTransaction.initFromJson(object, t)) return false;

        t.setActionType(JSONObjectHelper.safeGetString(object, "actionType"));
        t.setIncomingConditionName(JSONObjectHelper.safeGetString(object, "incomingConditionName"));
        t.setIncomingConditionNameSender(JSONObjectHelper.safeGetString(object, "incomingConditionNameSender"));
        t.setIncomingConditionBlockchainID(JSONObjectHelper.safeGetString(object, "incomingConditionBlockchainID"));
        t.setOutgoingConditionName(JSONObjectHelper.safeGetString(object, "outgoingConditionName"));
        t.setSuccessiveActionMaxWaitingTime(JSONObjectHelper.safeGetString(object, "successiveActionMaxWaitingTime"));
        t.setSuccessiveActionTotalPeerNumber(JSONObjectHelper.safeGetString(object, "successiveActionTotalPeerNumber"));
        t.setSuccessiveActionMaxAllowedPeerNumber(JSONObjectHelper.safeGetString(object, "successiveActionMaxAllowedPeerNumber"));
        return true;
    }


    static public void copy(ACPSettingTransaction to, ACPSettingTransaction from) {
        ACPCommonTransaction.copy(to, from);
        to.setActionType(from.getActionType());
        to.setIncomingConditionName(from.getIncomingConditionName());
        to.setIncomingConditionNameSender(from.getIncomingConditionNameSender());
        to.setIncomingConditionBlockchainID(from.getIncomingConditionBlockchainID());
        to.setOutgoingConditionName(from.getOutgoingConditionName());
        to.setSuccessiveActionMaxWaitingTime(from.getSuccessiveActionMaxWaitingTime());
        to.setSuccessiveActionTotalPeerNumber(from.getSuccessiveActionTotalPeerNumber());
        to.setSuccessiveActionMaxAllowedPeerNumber(from.getSuccessiveActionMaxAllowedPeerNumber());
    }

    @Override
    public JSONObject getJson() {
        return getJson(this);
    }

    public String getString(){
        return getJson().toString();
    }

    static public JSONObject getJson(ACPSettingTransaction t) {
        JSONObject json = ACPCommonTransaction.getJson(t);
        json.put("ACPActionParameterSetTransaction", "ACPActionParameterSetTransaction");// as the identifier used in createfromjson method
        json.put("actionType", t.getActionType());
        json.put("incomingConditionName", t.getIncomingConditionName());
        json.put("incomingConditionNameSender", t.getIncomingConditionNameSender());
        json.put("incomingConditionBlockchainID", t.getIncomingConditionBlockchainID());
        json.put("outgoingConditionName", t.getOutgoingConditionName());
        json.put("successiveActionMaxWaitingTime", t.getSuccessiveActionMaxWaitingTime());
        json.put("successiveActionTotalPeerNumber", t.getSuccessiveActionTotalPeerNumber());
        json.put("successiveActionMaxAllowedPeerNumber", t.getSuccessiveActionMaxAllowedPeerNumber());
        return json;
    }

    @Override
    public String calculateDataHash(ACPCommonTransaction t) {
        String h = super.calculateDataHash(t);
        if (h.isEmpty()) return ""; // error

        try {
            h = MD5Hash.getValue("TokenTransaction" + h);
            if (null != getActionType()) {
                h = MD5Hash.getValue(getActionType() + h);
            }
            if (null != getIncomingConditionName()) {
                h = MD5Hash.getValue(getIncomingConditionName() + h);
            }
            if (null != getIncomingConditionNameSender()) {
                h = MD5Hash.getValue(getIncomingConditionNameSender() + h);
            }
            if (null != getIncomingConditionBlockchainID()) {
                h = MD5Hash.getValue(getIncomingConditionBlockchainID() + h);
            }
            if (null != getOutgoingConditionName()) {
                h = MD5Hash.getValue(getOutgoingConditionName() + h);
            }
            if (null != getSuccessiveActionMaxWaitingTime()) {
                h = MD5Hash.getValue(getSuccessiveActionMaxWaitingTime() + h);
            }
            if (null != getSuccessiveActionTotalPeerNumber()) {
                h = MD5Hash.getValue(getSuccessiveActionTotalPeerNumber() + h);
            }
            if (null != getSuccessiveActionMaxAllowedPeerNumber()) {
                h = MD5Hash.getValue(getSuccessiveActionMaxAllowedPeerNumber() + h);
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
        ACPSettingTransaction at = (ACPSettingTransaction) anotherTransaction;
        if (!super.isSimilar(anotherTransaction)) return false;
        if (!getActionType().equals(at.getActionType())) return false;
        if (!getIncomingConditionName().equals(at.getIncomingConditionName())) return false;
        if (!getIncomingConditionNameSender().equals(at.getIncomingConditionNameSender())) return false;
        if (!getIncomingConditionBlockchainID().equals(at.getIncomingConditionBlockchainID())) return false;
        if (!getOutgoingConditionName().equals(at.getOutgoingConditionName())) return false;
        if (!getSuccessiveActionMaxWaitingTime().equals(at.getSuccessiveActionMaxWaitingTime())) return false;
        if (!getSuccessiveActionTotalPeerNumber().equals(at.getSuccessiveActionTotalPeerNumber())) return false;
        if (!getSuccessiveActionMaxAllowedPeerNumber().equals(at.getSuccessiveActionMaxAllowedPeerNumber())) return false;

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

    public String getIncomingConditionName() {
        return incomingConditionName;
    }

    public void setIncomingConditionName(String incomingConditionName) {
        this.incomingConditionName = incomingConditionName;
    }

    public String getIncomingConditionBlockchainID() {
        return incomingConditionBlockchainID;
    }

    public void setIncomingConditionBlockchainID(String incomingConditionBlockchainID) {
        this.incomingConditionBlockchainID = incomingConditionBlockchainID;
    }

    public String getIncomingConditionNameSender() {
        return incomingConditionNameSender;
    }

    public void setIncomingConditionNameSender(String incomingConditionNameSender) {
        this.incomingConditionNameSender = incomingConditionNameSender;
    }

    public String getOutgoingConditionName() {
        return outgoingConditionName;
    }

    public void setOutgoingConditionName(String outgoingConditionName) {
        this.outgoingConditionName = outgoingConditionName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public void setSuccessiveActionMaxWaitingTime(String successiveActionMaxWaitingTime) {
        this.successiveActionMaxWaitingTime = successiveActionMaxWaitingTime;
    }

    public void setSuccessiveActionTotalPeerNumber(String successiveActionTotalPeerNumber) {
        this.successiveActionTotalPeerNumber = successiveActionTotalPeerNumber;
    }

    public void setSuccessiveActionMaxAllowedPeerNumber(String successiveActionMaxAllowedPeerNumber) {
        this.successiveActionMaxAllowedPeerNumber = successiveActionMaxAllowedPeerNumber;
    }

    public String getSuccessiveActionMaxWaitingTime() {
        return successiveActionMaxWaitingTime;
    }

    public String getSuccessiveActionTotalPeerNumber() {
        return successiveActionTotalPeerNumber;
    }

    public String getSuccessiveActionMaxAllowedPeerNumber() {
        return successiveActionMaxAllowedPeerNumber;
    }
}

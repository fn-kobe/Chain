package com.scu.suhong.transaction.ActionCondition;

import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.CrosschainInterface;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import util.JSONObjectHelper;
import util.RandomHelper;
import util.StringHelper;

import java.security.NoSuchAlgorithmException;

//ACP means action condition pair
public class ACPTriggerTransaction extends ACPCommonTransaction implements CrosschainInterface {
    private ACPCondition condition = null;
    // indicating which token will be selected
    // the token id is sorted in increasse order and select the randomSelectedNumber_th blockchain
    private int randomSelectedNumber = -1;

    public ACPTriggerTransaction() {
        commonInit("defaultConditionName", 0, 0, 0);
    }

    public ACPTriggerTransaction(String conditionName) {
        commonInit(conditionName, 0, 0, 0);
    }

    public ACPTriggerTransaction(String conditionName, int maxAllowedActionNumber, int totalSuccessiveActionNumber, int maxWaitingTime) {
        commonInit(conditionName, maxAllowedActionNumber, totalSuccessiveActionNumber, maxWaitingTime);
    }

    private void commonInit(String conditionName, int maxAllowedActionNumber, int totalSuccessiveActionNumber, int maxWaitingTime) {
        condition = new ACPCondition(conditionName, maxAllowedActionNumber, totalSuccessiveActionNumber, maxWaitingTime);
        specifiedDerivedClassName = "ACPConditionTransaction";
    }

    public boolean generateSelectedNumberIfRandomTrigger(){
        if (null == condition){
            System.out.println("[ACPConditionTransaction][ERROR] No condition is set to generate random select number");
            return false;
        }
        int totalSuccessiveActionNumber = condition.getTotalSuccessiveActionNumber();
        int maxAllowedActionNumber = condition.getMaxAllowedActionNumber();
        if (0 != totalSuccessiveActionNumber && 0 != maxAllowedActionNumber){
            RandomHelper randomHelper = new RandomHelper(0, totalSuccessiveActionNumber);
            randomSelectedNumber = randomHelper.getNumber();
            System.out.println("[ACPConditionTransaction][Info] Generated successive id " + randomSelectedNumber);
            return true;
        }

        System.out.println("[ACPConditionTransaction][Info] No random trigger, no need to generate the selected successive id");
        return false;
    }

    public int getRandomSelectedNumber() {
        return randomSelectedNumber;
    }

    public int getRandomSelectedNumberByHash() {
        return StringHelper.getRandomNumberByHash(getHash());
    }

    public void setRandomSelectedNumber(int randomSelectedNumber) {
        this.randomSelectedNumber = randomSelectedNumber;
    }

    public static ACPTriggerTransaction createFromJson(JSONObject object) {
        ACPTriggerTransaction ACPTriggerTransaction = new ACPTriggerTransaction();
        if (!initFromJson(object, ACPTriggerTransaction)) return null;
        return ACPTriggerTransaction;
    }

    public static boolean initFromJson(JSONObject object, ACPTriggerTransaction t) {
        if (!object.has("ActionTriggerTransaction")) { // One transaction must have Data
            return false;
        }
        if (!ACPCommonTransaction.initFromJson(object, t)) return false;

        t.setCondition(ACPCondition.fromJson(JSONObjectHelper.safeGetString(object,"condition")));
        t.setRandomSelectedNumber(JSONObjectHelper.safeGetInt(object, "randomSelectedNumber"));
        return true;
    }

    static public JSONObject getJson(ACPTriggerTransaction t) {
        JSONObject json = ACPCommonTransaction.getJson(t);
        json.put("ActionTriggerTransaction", "ActionTriggerTransaction");// as the identifier used in createfromjson method
        String condition = "";
        if (null != t.getCondition()) condition = t.getCondition().getString();
        else {
            System.out.println("\n\n[Test] Condition is not set in ActionTriggerTransaction");
        }
        json.put("condition", condition);
        json.put("randomSelectedNumber", t.getRandomSelectedNumber());
        return json;
    }

    public ACPCondition getCondition() {
        return condition;
    }

    public void setCondition(ACPCondition condition) {
        this.condition = condition;
    }

    //    acpCondition.maxAllowedActionNumber = Integer.parseInt(formatArray[1]);
    //    acpCondition.totalSuccessiveActionNumber = Integer.parseInt(formatArray[2]);
    //    acpCondition.maxWaitingTime = Integer.parseInt(formatArray[3]);
    public void setCondition(String conditionString) {
        ACPCondition condition = new ACPCondition(conditionString);
        if (condition.isValid()) {
            this.condition = condition;
        } else {
            this.condition = null;
        }
    }

    public void copy(ACPTriggerTransaction to, ACPTriggerTransaction from) {
        ACPCommonTransaction.copy(to, from);
        setCondition(from.getCondition());
    }

    @Override
    public JSONObject getJson() {
        return getJson(this);
    }

    @Override
    public String calculateDataHash(ACPCommonTransaction t) {
        String h = super.calculateDataHash(t);
        if (h.isEmpty()) return ""; // error

        try {
            h = MD5Hash.getValue("ActionTriggerTransaction" + h);
            if (null != getCondition()) {
                h = MD5Hash.getValue(getCondition() + h);
            }
            if (null != condition) {
                h = MD5Hash.getValue(condition.getString() + h);
            }
            if (-1 != randomSelectedNumber) {
                h = MD5Hash.getValue(String.valueOf(randomSelectedNumber) + h);
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
        ACPTriggerTransaction at = (ACPTriggerTransaction) anotherTransaction;
        if (!super.isSimilar(anotherTransaction)) return false;
        if (!getCondition().getString().equals(at.getCondition().getString())) return false;
        if (randomSelectedNumber != at.randomSelectedNumber) return false;

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

    public String getConditionName() {
        return condition.getName();
    }
}

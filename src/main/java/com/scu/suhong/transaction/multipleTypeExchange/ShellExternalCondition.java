package com.scu.suhong.transaction.multipleTypeExchange;

import com.scu.suhong.transaction.exchangeMode.Condition;
import com.scu.suhong.transaction.exchangeMode.ConditionResult;
import org.json.JSONObject;

public class ShellExternalCondition extends ExchangeExternalCondition {
    Condition condition = new Condition();

    public ShellExternalCondition(String contract, String result, String oppositeResult) {
        condition.setConditionContract(contract);
        condition.setExpectResultString(result);
        condition.setOppositeResultString(oppositeResult);
    }

    public ShellExternalCondition(Condition condition){
        this.condition = condition;
    }

    @Override
    public ConditionResult doesExternalConditionMatch() {
        return condition.getResult();
    }

    static public ShellExternalCondition createFromJson(JSONObject o){
        ShellExternalCondition condition = new ShellExternalCondition(Condition.createFromJson((JSONObject) o.get("shell")));
        return condition;
    }

    @Override
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("shell", condition.getJson());
        return json;
    }
}

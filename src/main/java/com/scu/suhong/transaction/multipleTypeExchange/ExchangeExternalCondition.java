package com.scu.suhong.transaction.multipleTypeExchange;

import com.scu.suhong.transaction.exchangeMode.ConditionResult;
import org.json.JSONObject;

public class ExchangeExternalCondition {
    ConditionResult doesExternalConditionMatch(){return null;}
    JSONObject getJson(){return null;}
    public static ExchangeExternalCondition createFromJson(JSONObject object){
        if (object.has("wait")){
            return WaitExternalCondition.createFromJson(object);
        } else if (object.has("shell")){
            return ShellExternalCondition.createFromJson(object);
        } else {
            System.out.println("[ExchangeExternalCondition][createFromJson] no keyword for ExchangeExternalCondition sub class to create object");
            return null;
        }
    }
}

package com.scu.suhong.transaction.multipleTypeExchange;

import Service.BlockchainService;
import com.scu.suhong.transaction.exchangeMode.ConditionResult;
import org.json.JSONObject;
import util.JSONObjectHelper;

public class WaitExternalCondition extends ExchangeExternalCondition {
    int waitPeriod = 0;
    int startMiningPeriod = 0;

    public WaitExternalCondition(int waitPeriod) {
        this.waitPeriod = waitPeriod;
    }

    public void startCounnt(){
        startMiningPeriod = BlockchainService.getInstance().getMiner().getMinedProcessCount();
    }

    @Override
    public ConditionResult doesExternalConditionMatch() {
        if (0 == startMiningPeriod){
            System.out.println("[WaitExternalCondition][doesExternalConditionMatch][WARN] Start period is not set. Try to start it");
            startCounnt();
        }

        int currentMiningPeriod = BlockchainService.getInstance().getMiner().getMinedProcessCount();
        if (currentMiningPeriod - startMiningPeriod >= waitPeriod) return ConditionResult.True;
        return ConditionResult.Ongoing;
    }

    static public WaitExternalCondition createFromJson(JSONObject o){
        WaitExternalCondition condition = new WaitExternalCondition(JSONObjectHelper.safeGetInt(o, "wait"));
        return condition;
    }

    @Override
    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("wait", waitPeriod);
        return json;
    }
}

package com.scu.suhong.transaction;

import com.scu.suhong.transaction.multipleTypeExchange.ExchangeExternalCondition;
import com.scu.suhong.transaction.multipleTypeExchange.MultiTypeExchangeTransaction;
import com.scu.suhong.transaction.multipleTypeExchange.RequiredData;
import org.json.JSONArray;
import org.json.JSONObject;
import util.JSONObjectHelper;

public class CrosschainMultiTypeExchangeTransaction extends MultiTypeExchangeTransaction  implements CrosschainInterface {
    public CrosschainMultiTypeExchangeTransaction(int exchangeId, String chainID) {
        super(exchangeId, chainID);
    }

    public CrosschainMultiTypeExchangeTransaction(int exchangeId, String chainID, String type) {
        super(exchangeId, chainID, type);
    }

    public CrosschainMultiTypeExchangeTransaction(Transaction transaction, String requiredTxListType, int interactionId, String chainID) {
        super(transaction, requiredTxListType, interactionId, chainID);
    }


    static public CrosschainMultiTypeExchangeTransaction createFromJson(JSONObject exTx) {
        if (!exTx.has("crosschain")) { // One transaction must have Data
            return null;
        }

        int interactionId = safeGetPositiveInt(exTx, "exchangeId");
        String chainID = JSONObjectHelper.safeGetString(exTx, "chainID");
        String requiredTxListType = JSONObjectHelper.safeGetString(exTx, "requiredTxListType");
        Transaction transaction = Transaction.createFromJson((JSONObject) exTx.get("super"));
        CrosschainMultiTypeExchangeTransaction crosschainTransaction = new CrosschainMultiTypeExchangeTransaction(
                transaction, requiredTxListType, interactionId, chainID);

        JSONArray reqTxList = (JSONArray) exTx.get("reqTxList");
        for (int i = 0; i < reqTxList.length(); ++i){
            crosschainTransaction.addRequiredData(RequiredData.createFromJson((JSONObject) reqTxList.get(i)));
        }

        JSONObject conditionObject = (JSONObject) exTx.get("condition");
        ExchangeExternalCondition condition = ExchangeExternalCondition.createFromJson(conditionObject);
        crosschainTransaction.setCondition(condition);

        return crosschainTransaction;
    }

    @Override
    public JSONObject getJson() {
        JSONObject json = super.getJson();
        json.put("crosschain", "crosschain");
        return json;
    }
}

package com.scu.suhong.transaction;

import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.dynamic_definition.DynamicalAsset;
import com.scu.suhong.dynamic_definition.DynamicalAssetProcessor;
import com.scu.suhong.transaction.ActionCondition.ACPSettingTransaction;
import com.scu.suhong.transaction.ActionCondition.ACPTriggerTransaction;
import com.scu.suhong.transaction.ActionCondition.ACPTokenTransaction;
import com.scu.suhong.transaction.multipleTypeExchange.MultiTypeExchangeTransaction;
import org.json.JSONObject;

public class TransactionFactory {
    static public boolean isSupportedTransactionJson(JSONObject object) {
        if (object.has("crosschain") || object.has("reqTxList") || object.has("reqTx")
                || object.has("condition") || object.has("account") || object.has("DynamicalAsset")
                || object.has("ActionTriggerTransaction") || object.has("ACPTokenTransaction")
                || object.has("ACPActionParameterSetTransaction") || object.has("CommonCrosschainTransaction")
                || object.has("d"))
        { // One transaction must have Data
            return true;
        }

        return false;
    }

    static public AbstractTransaction createFromJson(JSONObject object) {
        if (object.has("crosschain")) { // One transaction must have Data
            return CrosschainMultiTypeExchangeTransaction.createFromJson(object);
        }

        if (object.has("reqTxList")) { // One transaction must have Data
            return MultiTypeExchangeTransaction.createFromJson(object);
        }

        if (object.has("reqTx")) { // One transaction must have Data
            return CrosschainTransaction.createFromJson(object);
        }

        if (object.has("ConditionalAssociationTransaction")) { // One transaction must have Data
            return ConditionalAssociationTransaction.createFromJson(object);
        }

        if (object.has("account")) { // One transaction must have Data
            return AssetCreateTransaction.createFromJson(object);
        }

        if (object.has("DynamicalAsset")) { // Dynamical asset should have key value pairs
            DynamicalAsset dynamicalAsset = DynamicalAsset.createFromJson(object);
            if (null == dynamicalAsset) {
                System.out.println("[TransactionFactory][ERROR] Load dynamical asset error");
                return null;
            }

            dynamicalAsset = DynamicalAssetProcessor.preProcessDynamicalTransaction(dynamicalAsset);
            if (null == dynamicalAsset) {
                System.out.println("[TransactionFactory][ERROR] Load dynamical derived asset error");
            }
            return dynamicalAsset;
        }

        if (object.has("ActionTriggerTransaction")) { // One transaction must have Data
            return ACPTriggerTransaction.createFromJson(object);
        }

        if (object.has("ACPTokenTransaction")) { // One transaction must have Data
            return ACPTokenTransaction.createFromJson(object);
        }

        if (object.has("ACPActionParameterSetTransaction")) { // One transaction must have Data
            return ACPSettingTransaction.createFromJson(object);
        }

        if (object.has("CommonCrosschainTransaction")) { // One transaction must have Data
            return CommonCrosschainTransaction.createFromJson(object);
        }

        if (object.has("d")) { // One transaction must have Data
            return Transaction.createFromJson(object);
        }

        return null;
    }
}

package com.scu.suhong.transaction;

import com.scu.suhong.transaction.multipleTypeExchange.RequiredData;
import com.scu.suhong.transaction.multipleTypeExchange.ShellExternalCondition;
import com.scu.suhong.transaction.multipleTypeExchange.WaitExternalCondition;
import consensus.pow.MiningConfiguration;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class CrosschainMultiTypeExchangeTransactionTest {
    int exchangeId= 10;
    String chainID = MiningConfiguration.getBlockchainStringId();

    @Test
    public void testJsonAndCreate() {
        CrosschainMultiTypeExchangeTransaction transaction = new CrosschainMultiTypeExchangeTransaction(exchangeId, chainID);
        String requiredFrom = "from";
        String requiredTo = "to";
        String requiredAssetType = "assetType";
        int requiredValue = 10000;

        String requiredFrom2 = "from2";
        String requiredTo2 = "to2";
        String requiredAssetType2 = "assetType2";
        int requiredValue2 = 10002;

        transaction.addRequiredData(new RequiredData(exchangeId, chainID, requiredFrom, requiredTo, requiredAssetType, requiredValue));
        transaction.addRequiredData(new RequiredData(exchangeId, chainID, requiredFrom2, requiredTo2, requiredAssetType2, requiredValue2));
        transaction.setCondition(new WaitExternalCondition(2));

        JSONObject json = transaction.getJson();
        String transactionString = json.toString();
        System.out.println(json.toString());
        assert transactionString.contains(requiredFrom);
        assert transactionString.contains(requiredTo);
        assert transactionString.contains(requiredAssetType);
        assert transactionString.contains(String.valueOf(requiredValue));
        assert transactionString.contains(requiredFrom2);
        assert transactionString.contains(requiredTo2);
        assert transactionString.contains(requiredAssetType2);
        assert transactionString.contains(String.valueOf(requiredValue2));

        CrosschainMultiTypeExchangeTransaction transaction2 = CrosschainMultiTypeExchangeTransaction.createFromJson(json);
        String transaction2String = transaction2.getJson().toString();
        System.out.println(transaction2String);
        assert transactionString.equals(transaction2String);

        transaction.setCondition(new ShellExternalCondition("ls", "true", "flase"));
        transactionString = transaction.getJson().toString();
        json = transaction.getJson();
        transaction2 = CrosschainMultiTypeExchangeTransaction.createFromJson(json);
        transaction2String = transaction2.getJson().toString();
        System.out.println(transaction2String);
        assert transactionString.equals(transaction2String);
    }
}
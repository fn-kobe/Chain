package com.scu.suhong.transaction.multipleTypeExchange;

import consensus.pow.MiningConfiguration;
import org.json.JSONObject;
import org.junit.Test;

public class MultiTypeExchangeTransactionTest {
    int exchangeId= 10;
    String chainID = MiningConfiguration.getBlockchainStringId();

    @Test
    public void getRequiredMultiTypeExchangeTransactionsWithSelf() {
        MultiTypeExchangeTransaction transaction = new MultiTypeExchangeTransaction(exchangeId);
        assert 1 == transaction.getRequiredDataListWithSelf().size();
        assert transaction.doesMatch(transaction.toRequiredData());

        transaction.addRequiredData(transaction.toRequiredData());
        assert 1 == transaction.getRequiredDataListWithSelf().size();
    }

    @Test
    public void testJsonAndCreate() {
        MultiTypeExchangeTransaction transaction = new MultiTypeExchangeTransaction(exchangeId);
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

        MultiTypeExchangeTransaction transaction2 = MultiTypeExchangeTransaction.createFromJson(json);
        String transaction2String = transaction2.getJson().toString();
        System.out.println(transaction2String);
        assert transactionString.equals(transaction2String);

        transaction.setCondition(new ShellExternalCondition("ls", "true", "flase"));
        transactionString = transaction.getJson().toString();
        json = transaction.getJson();
        transaction2 = MultiTypeExchangeTransaction.createFromJson(json);
        transaction2String = transaction2.getJson().toString();
        System.out.println(transaction2String);
        assert transactionString.equals(transaction2String);
    }

}
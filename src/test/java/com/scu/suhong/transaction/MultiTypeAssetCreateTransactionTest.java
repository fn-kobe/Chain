package com.scu.suhong.transaction;

import org.json.JSONObject;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class MultiTypeAssetCreateTransactionTest {
    @Test
    public void fromJson() {
        String assetType = "createAsset";
        int amount = 100;
        String account = "createAccount";
        AssetCreateTransaction transaction = new AssetCreateTransaction(assetType, amount, account);
        JSONObject object = transaction.getJson();
        System.out.println(object.toString());
        AssetCreateTransaction createTransaction = AssetCreateTransaction.fromJson(object);

        assert createTransaction.getAssetType().equals(assetType);
        assert createTransaction.getAmount() == amount;
        assert createTransaction.getAccount().equals(account);
    }

}
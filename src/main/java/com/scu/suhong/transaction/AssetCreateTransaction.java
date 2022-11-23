package com.scu.suhong.transaction;

import com.scu.suhong.Hash.MD5Hash;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;

public class AssetCreateTransaction extends Transaction{
    // String assetType = ""; // reuse assetType in transaction
    int amount = 0;
    String account = "";

    public AssetCreateTransaction(String assetType, int amount, String account) {
        this.assetType = assetType;
        this.amount = amount;
        this.account = account;
        setFrom("create");
        setToAndValue(account, amount);
        setId();
    }

    public AssetCreateTransaction(Transaction transaction, int amount, String account) {
        super(transaction);
        this.amount = amount;
        this.account = account;
    }

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        json.put("super", super.getJson());
        json.put("amount", amount);
        json.put("account", account);
        return json;
    }

    // For network propagated transaction
    static public AssetCreateTransaction createFromJson(JSONObject crTx) {
        return fromJson(crTx);
    }

    @Nullable
    public static AssetCreateTransaction fromJson(JSONObject object) {
        if (!object.has("account")) { // One transaction must have Data
            return null;
        }
        Transaction transaction = Transaction.createFromJson((JSONObject) object.get("super"));
        AssetCreateTransaction assetCreateTransaction = new AssetCreateTransaction(transaction,
                object.getInt("amount"),
                object.getString("account"));

        return assetCreateTransaction;
    }

    public String Dump() {
        String dump = "<create>\n";
        dump += "assetType:" + assetType + "\n";
        dump += "amount:" + amount + "\n";
        dump += "account:" + account + "\n";
        dump += "</creat>\n";
        return super.Dump(dump);
    }

    public int compareTo(Object o) {
        if (!(o instanceof AssetCreateTransaction)) {
            return 1;
        }
        AssetCreateTransaction otherTransaction = (AssetCreateTransaction) o;
        return this.getHash().compareTo(otherTransaction.getHash());
    }

    public String getHash() {
        if (hash == null || hash.isEmpty()) {
            hash = calculateDataHash();
        }
        return hash;
    }

    public String calculateDataHash() {
        String h = super.calculateDataHash();
        try {
            h += MD5Hash.getValue(assetType + h);
            h += MD5Hash.getValue(amount + h);
            h += MD5Hash.getValue(account + h);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        ;
        return h;
    }

    public int getAmount() {
        return amount;
    }

    public String getAccount() {
        return account;
    }
}

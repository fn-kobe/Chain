package com.scu.suhong.block;

import com.scu.suhong.transaction.Transaction;
import org.json.JSONObject;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;

public class TransactionTest {

    @Test
    public void testGetTransactionHash() throws NoSuchAlgorithmException {
        Transaction t = new Transaction();
        t.setAssetType("Big_data");
        t.setData("test Data");
        t.setPublicKey("test public key");

        System.out.println(t.getTransactionDataHash());
    }

    @Test
    public void testIsValid() throws NoSuchAlgorithmException {
        Transaction t = new Transaction();
        t.setAssetType("Big Data");
        t.setData("test Data");
        t.setPublicKey("test public key");
        t.setHash(t.calculateDataHash());
//        assert (t.isValid());
    }

    @Test
    public void testJson() {
        Transaction t = new Transaction();
        String testType = "Test Type";
        String testData = "Test Data";
        String testPrivateKey = "Test privateKey";
        String testPublicKey = "Test public key";
        String from = "from";
        String to = "to";
        int value = 0;
        t.setAssetType(testType);
        t.setData(testData);
        t.setPublicKey(testPublicKey);
        t.setPrivateKey(testPrivateKey);
        t.setHash(t.calculateDataHash());
        t.setFrom(from);
        t.setToAndValue(to, String.valueOf(value));
        JSONObject object = t.getJson();
        Transaction tn = Transaction.createFromJson(object);
        assert (0 == t.compareTo(tn));
    }
}
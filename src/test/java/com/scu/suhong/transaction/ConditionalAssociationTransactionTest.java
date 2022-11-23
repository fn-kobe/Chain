package com.scu.suhong.transaction;

import org.json.JSONObject;
import org.junit.Test;

public class ConditionalAssociationTransactionTest {
    @Test
    public void testCalculateDataHash(){
        Condition condition = new Condition("from", "to", 2);
        int interactionId = 1;
        ConditionalAssociationTransaction t1 = new ConditionalAssociationTransaction(interactionId, condition);
        t1.setId();
        ConditionalAssociationTransaction t11 = ConditionalAssociationTransaction.createFromJson(t1.getJson());
        ConditionalAssociationTransaction t2 = new ConditionalAssociationTransaction(interactionId, condition);
        t2.setId();
        assert t1.calculateDataHash().equals(t11.calculateDataHash());
        assert !t1.calculateDataHash().equals(t2.calculateDataHash());
    }

    @Test
    public void testJson() {
        Transaction normalTransaction = new Transaction();
        String testType = "Test Type";
        String testData = "Test Data";
        String testPrivateKey = "Test privateKey";
        String testPublicKey = "Test public key";
        String from = "from";
        String to = "to";
        int value = 0;
        normalTransaction.setAssetType(testType);
        normalTransaction.setData(testData);
        normalTransaction.setPublicKey(testPublicKey);
        normalTransaction.setPrivateKey(testPrivateKey);
        normalTransaction.setHash(normalTransaction.calculateDataHash());
        normalTransaction.setFrom(from);
        normalTransaction.setToAndValue(to, String.valueOf(value));
        normalTransaction.setId();
        Condition condition = new Condition("from", "to", 2);
        int interactionId = 111;
        Transaction conditionalTransaction = new ConditionalAssociationTransaction(normalTransaction, interactionId,  condition);
        JSONObject object = conditionalTransaction.getJson();
        System.out.println(object.toString());
        Transaction conditionalTransactionJson = ConditionalAssociationTransaction.createFromJson(object);
        assert !(0 == conditionalTransactionJson.compareTo(normalTransaction));
        assert (0 == conditionalTransaction.compareTo(conditionalTransactionJson));
        assert interactionId == ((ConditionalAssociationTransaction) conditionalTransaction).getInteractionId();
        System.out.println(conditionalTransactionJson.Dump());
        assert conditionalTransaction.Dump().equals(conditionalTransactionJson.Dump());
    }

    @Test
    public void testID(){
        int interactionId = 1;
        ConditionalAssociationTransaction conditionalAssociationTransaction1 = new ConditionalAssociationTransaction(interactionId, new Condition("", "", 0));
        ConditionalAssociationTransaction conditionalAssociationTransaction2 = new ConditionalAssociationTransaction(interactionId, new Condition("", "", 0));
        conditionalAssociationTransaction1.setData("");
        conditionalAssociationTransaction2.setData("");
        System.out.println(conditionalAssociationTransaction1.getId()) ;
        System.out.println(conditionalAssociationTransaction2.getId()) ;
        assert conditionalAssociationTransaction1.getId() != conditionalAssociationTransaction2.getId();
    }

    @Test
    public void testHash() {
        String jsonString = "{\"ConditionalAssociationTransaction\":\"ConditionalAssociationTransaction\",\"normal\":{\"pr\":\"\",\"blockIndex\":\"-1\",\"t\":\"\",\"d\":\"1585377293803\",\"pu\":\"\",\"gas\":0,\"h\":\"EDB2DE50CA9472FCE1E3F4A34E97FB12C11BCEAD6B2767FA3865B8CEB3250FFF1E6E0A04D20F50967C64DAC2D639A577\",\"from\":\"111?789002\",\"id\":608594326,\"to\":\"111?789003_1_789004_4\",\"value\":\"5\"},\"interactionId\":1100,\"condition\":{\"from\":\"111?789002\",\"to\":\"\",\"value\":\"5\"}}";
        ConditionalAssociationTransaction transaction = ConditionalAssociationTransaction.createFromJson(new JSONObject(jsonString));

        System.out.printf("\n[Test] Get hash %s vs calculated hash %s\n", transaction.getHash(), transaction.calculateDataHash());
        assert !transaction.getHash().equals(transaction.calculateDataHash());

        jsonString = "{\"ConditionalAssociationTransaction\":\"ConditionalAssociationTransaction\",\"normal\":{\"pr\":\"\",\"blockIndex\":\"-1\",\"t\":\"\",\"d\":\"1585377293803\",\"pu\":\"\",\"gas\":0,\"h\":\"EDB2DE50CA9472FCE1E3F4A34E97FB12C11BCEAD6B2767FA3865B8CEB3250FFF1E6E0A04D20F50967C64DAC2D639A577\",\"from\":\"111?789002\",\"id\":608594326,\"to\":\"111?789003_1_789004_4\",\"value\":\"5\"},\"interactionId\":1100,\"condition\":{\"from\":\"111?789002\",\"to\":\"\",\"value\":\"5\"}}";
        ConditionalAssociationTransaction transaction2 = ConditionalAssociationTransaction.createFromJson(new JSONObject(jsonString));
        System.out.printf("\n[Test] Get hash %s vs calculated hash %s vs Force %s\n", transaction2.getHash(), transaction2.calculateDataHash());
        assert transaction2.getHash().equals(transaction2.calculateDataHash());
    }
}
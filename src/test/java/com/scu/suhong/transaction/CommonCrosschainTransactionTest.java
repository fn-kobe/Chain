package com.scu.suhong.transaction;

import com.scu.suhong.dynamic_definition.AbstractTransaction;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class CommonCrosschainTransactionTest {

    @Test
    public void createFromJson() {
        int interactionId = 10001;
        String from = "from";
        String to = "to";
        int value = 1;
        CommonCrosschainTransaction cct = new CommonCrosschainTransaction(interactionId);
        cct.setFrom(from);
        cct.setToAndValue(to, value);
        cct.setId();

        String jsonString = cct.toString();
        System.out.println("[Test] " + jsonString);
        System.out.println("[Test] blockchain id " + cct.getBlockchainId());
        System.out.println("[Test] interaction id " + cct.getInteractionId());
        CommonCrosschainTransaction other = CommonCrosschainTransaction.createFromJson(jsonString);

        assert  null != other;
        assert other.toString().equals(jsonString);
        assert other.getInteractionId() == cct.getInteractionId();
    }

    @Test
    public void createFromString() {
        String networkString = "{\"super\":{\"pr\":\"\",\"miningTime\":1587552025860,\"blockIndex\":\"11\",\"t\":\"\",\"d\":\"notary_first_txonbc2\",\"pu\":\"\",\"gas\":0,\"h\":\"E700F06A11D218558A51EE71BB578B79\",\"from\":\"222?789002\",\"id\":1295147204,\"to\":\"222?789003\",\"value\":\"2\"},\"interactionId\":10081,\"CommonCrosschainTransaction\":\"CommonCrosschainTransaction\"}";

        AbstractTransaction t = TransactionFactory.createFromJson(new JSONObject(networkString));
        assert t instanceof CrosschainInterface;
        assert t.isExternalTransaction();
        assert (t instanceof CrosschainInterface && (t.isExternalTransaction()));
    }
}
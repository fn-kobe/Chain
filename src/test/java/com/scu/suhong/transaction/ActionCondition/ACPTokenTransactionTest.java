package com.scu.suhong.transaction.ActionCondition;

import consensus.pow.MiningConfiguration;
import org.json.JSONObject;
import org.junit.Test;
import util.TimeHelper;

public class ACPTokenTransactionTest {

    @Test
    public void testJson() {
        String conditionName = "conditionName";
        int nextParalleledActionNumber = 1;
        int blockIndex = 12;
        int gas = 1;
        String date = "data";
        int lifeCycle = 2;
        String owner = "owner";

        ACPTokenTransaction t = new ACPTokenTransaction();
        t.setConditionName(conditionName);
        t.setBlockchainID(MiningConfiguration.getBlockchainStringId());
        t.setBlockIndex(blockIndex);
        t.setMiningTime(TimeHelper.getEpoch());
        t.setData(date);
        t.setGas(gas);
        t.setLifecyleType(lifeCycle);
        t.setOwner(owner);

        t.setId();
        t.setHash();

        JSONObject object = t.getJson();
        System.out.println("[Test] the hash is " + object.toString());

        ACPTokenTransaction nt = ACPTokenTransaction.createFromJson(object);
        assert nt.isSimilar(t);
        assert nt.getConditionName().equals(conditionName);
        assert nt.getBlockchainId().equals(t.getBlockchainId());
        assert nt.getBlockIndex() == blockIndex;
        assert nt.getData().equals(date);
        assert nt.getGas() == gas;
        assert nt.getLifecyleType() == lifeCycle;
        assert nt.getOwner().equals(owner);
        assert nt.getId() == t.getId();
        assert nt.getHash() == t.getHash();
    }
}
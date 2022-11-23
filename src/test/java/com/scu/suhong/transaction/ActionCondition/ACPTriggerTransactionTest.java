package com.scu.suhong.transaction.ActionCondition;

import consensus.pow.MiningConfiguration;
import org.json.JSONObject;
import org.junit.Test;
import util.TimeHelper;

public class ACPTriggerTransactionTest {
    @Test
    public void testJson() {
        String condition = "condition:1:5:10";
        int blockIndex = 12;
        int gas = 1;
        String date = "data";
        int lifeCycle = 2;
        String owner = "owner";

        ACPTriggerTransaction t = new ACPTriggerTransaction();
        t.setCondition(condition);
        t.setBlockchainID(MiningConfiguration.getBlockchainStringId());
        t.setBlockIndex(blockIndex);
        t.setMiningTime(TimeHelper.getEpoch());
        t.setData(date);
        t.setGas(gas);
        t.setLifecyleType(lifeCycle);
        t.setOwner(owner);

        t.generateSelectedNumberIfRandomTrigger();
        t.setId();
        t.setHash();

        JSONObject object = t.getJson();
        System.out.println("[Test] the hash is " + object.toString());

        ACPTriggerTransaction nt = ACPTriggerTransaction.createFromJson(object);
        assert nt.isSimilar(t);
        assert nt.getCondition().isTheSame(new ACPCondition(condition));
        assert nt.getBlockchainId().equals(t.getBlockchainId());
        assert nt.getBlockIndex() == blockIndex;
        assert nt.getData().equals(date);
        assert nt.getGas() == gas;
        assert nt.getLifecyleType() == lifeCycle;
        assert nt.getOwner().equals(t.getOwner());
        assert nt.getRandomSelectedNumber() == t.getRandomSelectedNumber();
        assert nt.getRandomSelectedNumber() != -1;
        assert nt.getId() == t.getId();
        assert nt.getHash() == t.getHash();
    }

    @Test
    public void generateRandomSelectedNumber() {
        ACPTriggerTransaction t = new ACPTriggerTransaction();
        assert !t.generateSelectedNumberIfRandomTrigger();

        int allowedNumber = 1;
        int totalnumber = 5;
        int maxWaitTime = 80;
        int[] flag = new int[totalnumber];
        for (int i = 0; i < totalnumber; ++i) flag[i] = 0;

        t.setCondition("testTriggerCondition:" + allowedNumber + ":"
                + totalnumber + ":" + Integer.valueOf(maxWaitTime) + "100");
        // this loop should be large enough to make each number is generated
        for (int i = 0; i < 20; ++i) {
            assert t.generateSelectedNumberIfRandomTrigger();
            int r = t.getRandomSelectedNumber();
            flag[r] = 1;
        }

        for (int i = 0; i < totalnumber; ++i) assert 1 == flag[i];
    }

    @Test
    public void getRandomSelectedNumberByHash() {
        ACPTriggerTransaction t = new ACPTriggerTransaction();
        t.setId();

        int allowedNumber = 1;
        int totalnumber = 5;
        int maxWaitTime = 80;
        int[] flag = new int[totalnumber];
        for (int i = 0; i < totalnumber; ++i) flag[i] = 0;

        t.setCondition("testTriggerCondition:" + allowedNumber + ":"
                + totalnumber + ":" + Integer.valueOf(maxWaitTime) + "100");

        // One tx should generate the same random
        assert t.generateSelectedNumberIfRandomTrigger();
        int r = t.getRandomSelectedNumberByHash();
        System.out.println(r);
        int r2 = t.getRandomSelectedNumberByHash();
        assert r == r2;
    }
}

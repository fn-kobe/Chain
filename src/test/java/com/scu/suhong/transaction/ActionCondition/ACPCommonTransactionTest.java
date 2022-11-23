package com.scu.suhong.transaction.ActionCondition;

import consensus.pow.MiningConfiguration;
import org.junit.Test;

import static org.junit.Assert.*;

public class ACPCommonTransactionTest {

    @Test
    public void getOwnerWithoutChainId() {
        MiningConfiguration.testSetChainId(333);
        String ownerShortName = "IncomingRandomTrigger1Sender";
        String ownerFullName = MiningConfiguration.getBlockchainStringId() + "?" + "IncomingRandomTrigger1Sender";
        ACPCommonTransaction t = new ACPCommonTransaction();
        t.setOwner(ownerShortName);
        System.out.println("[Test] input are " + ownerShortName + " and " + ownerFullName);
        assert ownerFullName.equals(t.getOwner());
        assert ownerShortName.equals(t.getOwnerWithoutChainId());

        String ownerFullNameFromAnotherChain = "555?IncomingRandomTrigger1Sender";
        t.setOwner(ownerFullNameFromAnotherChain);
        assert !t.getOwnerWithoutChainId().contains("?");
    }
}
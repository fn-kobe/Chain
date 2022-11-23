package com.scu.suhong.transaction.ActionCondition;

import java.util.Comparator;

public class ACPTokenTransactionComparatorByBlockchainId implements Comparator<ACPTokenTransaction> {

    @Override
    public int compare(ACPTokenTransaction o1, ACPTokenTransaction o2) {
        return o1.getBlockchainId().compareTo(o2.getBlockchainId());
    }
}

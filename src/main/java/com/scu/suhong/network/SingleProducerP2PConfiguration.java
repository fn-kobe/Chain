package com.scu.suhong.network;

import util.AddressPort;

import java.util.List;

public class SingleProducerP2PConfiguration extends SimpleP2PConfiguration{
    String chainId;
    String consensus;
    int difficult;
    List<AddressPort> addressPortList;

    public SingleProducerP2PConfiguration(String configurationFileName, String chainId, String consensus,
                                          int difficult, List<AddressPort> addressPortList) {
        super(configurationFileName);
        this.chainId = chainId;
        this.consensus = consensus;
        this.difficult = difficult;
        this.addressPortList = addressPortList;
    }

    public String getChainId() {
        return chainId;
    }

    public String getConsensus() {
        return consensus;
    }

    public int getDifficult() {
        return difficult;
    }

    public List<AddressPort> getAddressPortList() {
        return addressPortList;
    }

    // As ip is not line by line, but in a field, we have to override this mmethod
    @Override
    public List<AddressPort> getAddressListFromFile(){
        return addressPortList;
    }

    //Only for test. Real implementation should be configuration file
    public void testSetDifficult(int difficult) {
        this.difficult = difficult;
    }
}

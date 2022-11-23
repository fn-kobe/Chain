package com.scu.suhong.network;

import org.junit.Test;
import util.AddressPort;
import util.FileHelper;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class ProducerP2PConfigurationTest {
    String externalChainProducerName = "externalChainProducer";

    /*
    * [producer1]
    chainId = 555
    consensus=PoW
    difficult=4
    peers = 127.0.0.1:9201 127.0.0.1:9202 127.0.0.1:9203

    [producer2]
    chainId = 999
    consensus=PoS
    difficult=0
    peers = 127.0.0.1:9207 127.0.0.1:9208 127.0.0.1:9209
    * */

    @Test
    public void getAllChainId() {
        prepareConfigurationFile();
        ProducerP2PConfiguration configuration = new ProducerP2PConfiguration(externalChainProducerName);
        Set<String> chainIdSet = configuration.getAllChainId();
        assert !chainIdSet.isEmpty();
        for (String chainId : chainIdSet) {
            SingleProducerP2PConfiguration single = configuration.getConfiguration(chainId);
            assert !single.getAddressPortList().isEmpty();
            System.out.println("chainId: "+ single.getChainId());
            System.out.println("consensus: "+ single.getConsensus());
            System.out.println("difficult: "+ single.getDifficult());
            printOutAddressSet(single.getAddressPortList());
        }
    }

    void printOutAddressSet(List<AddressPort> addressPortSet){
        String r = "Producer address port: ";
        for (AddressPort addressPort : addressPortSet){
            r += addressPort.getAddress() + ":" + addressPort.getPort() + "\t";
        }
        System.out.println(r);
    }

    void prepareConfigurationFile(){
        String producerConfiguration = "[producer1]\n" +
                "chainId = 555\n" +
                "consensus=PoW\n" +
                "difficult=4\n" +
                "peers = 127.0.0.1:9201 127.0.0.1:9202 127.0.0.1:9203\n" +
                "\n" +
                "[producer2]\n" +
                "chainId = 999\n" +
                "consensus=PoS\n" +
                "difficult=0\n" +
                "peers = 127.0.0.1:9207 127.0.0.1:9208 127.0.0.1:9209";

        FileHelper.deleteFile(externalChainProducerName);
        FileHelper.createFile(externalChainProducerName, producerConfiguration);
    }
}
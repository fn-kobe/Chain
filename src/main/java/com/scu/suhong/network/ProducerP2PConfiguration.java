package com.scu.suhong.network;

import util.AddressPort;
import util.IniFileHelper;
import util.StringHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ProducerP2PConfiguration{
    String configurationFileName;
    String chainIdKey="chainId";
    String consensusKey="consensus";
    String difficultKey="difficult";
    String addressPortKey="peers";
    String peersSeparator = " ";

    HashMap<String, SingleProducerP2PConfiguration> singleProducerP2PConfigurationChainIdMap;

    final static String producerSectionPrefix = "producer";
    final static int maxProducerNumber = 100;// from producer1 to producer100

    ProducerP2PConfiguration(String configurationFileName) {
        this.configurationFileName = configurationFileName;
        singleProducerP2PConfigurationChainIdMap = new HashMap<>();
        loadContent();
    }

    public void loadContent(){
        IniFileHelper iniFileHelper = new IniFileHelper(configurationFileName);
        for (int i = 1; i < maxProducerNumber; ++i){
            String sectionName = producerSectionPrefix + i;
            if (!iniFileHelper.doesSectionExist(sectionName)) break;// we assume that sections are continuously
            String chainId = iniFileHelper.getValue(sectionName, chainIdKey);
            if (singleProducerP2PConfigurationChainIdMap.containsKey(chainId)){
                System.out.printf("[ProducerP2PConfiguration][WARN] External producer chain %s already exits\n", chainId);
                continue;
            }
            String consensus = iniFileHelper.getValue(sectionName, consensusKey);
            int difficult = Integer.parseInt(iniFileHelper.getValue(sectionName, difficultKey));
            String peers = iniFileHelper.getValue(sectionName, addressPortKey);
            String[] separatedPeers = peers.split(peersSeparator);
            List<AddressPort> addressPortList = new ArrayList<>();
            for (String onePeer : separatedPeers){
                AddressPort addressPort= StringHelper.getAddressPort(onePeer);
                if (null != addressPort) addressPortList.add(addressPort);
            }
            if (addressPortList.isEmpty()) continue;
            SingleProducerP2PConfiguration configuration = new SingleProducerP2PConfiguration(
                    configurationFileName, chainId, consensus, difficult, addressPortList);
            singleProducerP2PConfigurationChainIdMap.put(chainId, configuration);
            System.out.printf("[ProducerP2PConfiguration][DEBUG] Succeed to add external producer chain %s\n", chainId);
        }
    }

    SingleProducerP2PConfiguration getConfiguration(String chainId){
        if (singleProducerP2PConfigurationChainIdMap.isEmpty()) loadContent();
        return singleProducerP2PConfigurationChainIdMap.get(chainId);
    }

    public Set<String> getAllChainId(){
        return singleProducerP2PConfigurationChainIdMap.keySet();
    }

    public boolean isExternalPeer(String address, int port) {
        for (String chainId : singleProducerP2PConfigurationChainIdMap.keySet()){
            SingleProducerP2PConfiguration configuration = getConfiguration(chainId);
            if (configuration.isExternalPeer(address, port)) return true;
        }
        return false;
    }

    public String getProducerChainId(String ip, int port) {
        for (String chainId : singleProducerP2PConfigurationChainIdMap.keySet()){
            SingleProducerP2PConfiguration configuration = getConfiguration(chainId);
            List<AddressPort> externalPeerAddressPortList = configuration.getExternalPeerAddressPortList();
            for (AddressPort addressPort : externalPeerAddressPortList){
                if (addressPort.getAddress().equals(ip) && addressPort.getPort() == port) return chainId;
            }
        }
        return "";
    }
}

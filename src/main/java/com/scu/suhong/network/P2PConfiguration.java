package com.scu.suhong.network;

import consensus.pow.MiningConfiguration;
import consensus.pow.MiningConfigurationHandler;
import util.AddressPort;
import util.GaussianHelper;
import util.StringHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class P2PConfiguration {
    private static P2PConfiguration instance;

    static final String peerAddressConfigurationFile = "peerAddressConfiguration";
    static private final String externalChainProducerFile = "externalChainProducer";
    static private final String externalChainConsumerFile = "externalChainConsumer";
    private SimpleP2PConfiguration externalChainConsumerConfiguration;
    private ProducerP2PConfiguration externalChainProducerConfiguration;
    int defaultPort = 0;
    GaussianHelper gaussianHelper;
    private List<String> peerIPList;
    private List<Integer> peerPortList;
    private List<AddressPort> peerAddressPortList;
    private List<String> peerAddressListWithoutSelf;
    private List<Integer> peerPortListWithoutSelf;

    private List<String> allBlockchainObserverIPList;
    private List<Integer> allBlockchainObserverPortList;

    private P2PConfiguration() {
        gaussianHelper = new GaussianHelper();
        externalChainConsumerConfiguration = new SimpleP2PConfiguration(externalChainConsumerFile);
        externalChainProducerConfiguration = new ProducerP2PConfiguration(externalChainProducerFile);
        defaultPort = StringHelper.getDefaultPort();
    }

    public static String getPeerAddressConfigurationFile() {
        return peerAddressConfigurationFile;
    }

    static public P2PConfiguration getInstance() {
        if (null == instance) {
            instance = new P2PConfiguration();
        }
        return instance;
    }

    // P2P configure use external producer as internal is done by MiningConfigurationHandler
    public boolean isHashMatched(String chainId, String blockHash) {
        SingleProducerP2PConfiguration single = getProducerP2PConfiguration(chainId);
        if (null == single){
            System.out.println("[P2PConfiguration][ERROR] P2PConfiguration is used for foreign blockchain, while no chainId is provided for non-genesis block " +
                    blockHash);
            return false;
        }
        return MiningConfigurationHandler.isHashMatched(single.getDifficult(), 0, blockHash);
    }

    public String getProducerChainId(String ip, int port){
        return externalChainProducerConfiguration.getProducerChainId(ip, port);
    }

    public List<String> getPeerAddressList() {
        if (null == peerIPList || peerIPList.isEmpty()) {
            loadPeerAddressPortFromFile(peerAddressConfigurationFile);
        }
        return peerIPList;
    }

    public List<AddressPort> getPeerAddressPortList() {
        if (null == peerAddressPortList || peerAddressPortList.isEmpty()) {
            loadPeerAddressPortFromFile(peerAddressConfigurationFile);
        }
        return peerAddressPortList;
    }

    private boolean loadPeerAddressPortFromFile(String fileName) {
        if (null == peerIPList) peerIPList = new ArrayList<>();
        if (null == peerPortList) peerPortList = new ArrayList<>();
        if (null == peerAddressPortList) peerAddressPortList = new ArrayList<>();

        List<AddressPort> addressPortList = StringHelper.loadIPAndPortFromFile(fileName);
        if (null == addressPortList || addressPortList.isEmpty()) return false;

        for (AddressPort addressPort : addressPortList) {
            if (!addressPort.isValid()) continue;
            // IP<:port>, then ip is at the beginning
            peerAddressPortList.add(addressPort);
            peerIPList.add(addressPort.getAddress());
            peerPortList.add(addressPort.getPort());
        }

        peerIPList.add("127.0.0.1"); // add local address
        peerPortList.add(MiningConfiguration.getSelfListenPort());
        return true;
    }

    public List<String> getAllBlockchainObserverAddressList() {
        if (null == allBlockchainObserverIPList || allBlockchainObserverIPList.isEmpty()) {
            allBlockchainObserverIPList = new ArrayList<>();
            allBlockchainObserverIPList.addAll(getPeerAddressListWithoutSelf());
            allBlockchainObserverIPList.addAll(externalChainConsumerConfiguration.getExternalPeerAddressListWithoutSelf());
        }
        return allBlockchainObserverIPList;
    }

    public List<Integer> getAllBlockchainObserverPortList() {
        if (null == allBlockchainObserverPortList || allBlockchainObserverPortList.isEmpty()) {
            allBlockchainObserverPortList = new ArrayList<>();
            allBlockchainObserverPortList.addAll(getPeerPortListWithoutSelf());
            allBlockchainObserverPortList.addAll(externalChainConsumerConfiguration.getExternalPeerPortListWithoutSelf());
        }
        return allBlockchainObserverPortList;
    }

    public List<String> getPeerAddressListWithoutSelf() {
        if (null == peerAddressListWithoutSelf) {
            loadPeerAddressListWithoutSelf();
        }
        return peerAddressListWithoutSelf;
    }

    public List<Integer> getPeerPortListWithoutSelf() {
        if (null == peerPortListWithoutSelf) {
            loadPeerAddressListWithoutSelf();
        }
        return peerPortListWithoutSelf;
    }

    private void loadPeerAddressListWithoutSelf() {
        if (null == peerAddressListWithoutSelf || null == peerPortListWithoutSelf) {
            peerAddressListWithoutSelf = new ArrayList<>();
            peerPortListWithoutSelf = new ArrayList<>();
            for (AddressPort addressPort : getPeerAddressPortList()) {
                if (addressPort.isValid() && !MiningConfiguration.isSelf(addressPort)) {
                    peerAddressListWithoutSelf.add(addressPort.getAddress());
                    peerPortListWithoutSelf.add(addressPort.getPort());
                }
            }
        }
    }

    public List<String> getProducerPeerAddressListWithoutSelf() {
        List<String> r = new ArrayList<>();
        Set<String> chainIdList = externalChainProducerConfiguration.getAllChainId();
        for (String chainId : chainIdList){
            r.addAll(getProducerP2PConfiguration(chainId).getExternalPeerAddressListWithoutSelf());
        }
        return r;
    }

    public SingleProducerP2PConfiguration getProducerP2PConfiguration(String chainId){
        if (null == chainId || chainId.isEmpty()){
            System.out.println("[P2PConfiguration][ERROR] chain id is empty when load for external producer");
            return null;
        }
        return externalChainProducerConfiguration.getConfiguration(chainId);
    }

    public List<String> getProducerPeerAddressListWithoutSelf(String chainId) {
        return getProducerP2PConfiguration(chainId).getExternalPeerAddressListWithoutSelf();
    }

    public List<Integer> getProducerPeerPortListWithoutSelf(String chainId) {
        return getProducerP2PConfiguration(chainId).getExternalPeerPortListWithoutSelf();
    }

    public List<String> getProducerPeerAddressList() {
        List<String> r = new ArrayList<>();
        Set<String> chainIdList = externalChainProducerConfiguration.getAllChainId();
        for (String chainId : chainIdList){
            r.addAll(getProducerP2PConfiguration(chainId).getExternalPeerAddressList());
        }
        return r;
    }

    public List<String> getProducerPeerAddressList(String chainId) {
        return getProducerP2PConfiguration(chainId).getExternalPeerAddressList();
    }

    public List<Integer> getProducerPeerPortList(String chainId) {
        return getProducerP2PConfiguration(chainId).getExternalPeerPortList();
    }

    public List<AddressPort> getProducerPeerAddressPortList() {
        List<AddressPort> r = new ArrayList<>();
        Set<String> chainIdList = externalChainProducerConfiguration.getAllChainId();
        for (String chainId : chainIdList){
            r.addAll(getProducerP2PConfiguration(chainId).getExternalPeerAddressPortList());
        }
        return r;
    }

    public List<AddressPort> getProducerPeerAddressPortList(String chainId) {
        return getProducerP2PConfiguration(chainId).getExternalPeerAddressPortList();
    }

    public List<String> getConsumerPeerAddressListWithoutSelf() {
        return externalChainConsumerConfiguration.getExternalPeerAddressListWithoutSelf();
    }

    public List<Integer> getConsumerPeerPortListWithoutSelf() {
        return externalChainConsumerConfiguration.getExternalPeerPortListWithoutSelf();
    }

    public List<String> getConsumerPeerAddressList() {
        return externalChainConsumerConfiguration.getExternalPeerAddressList();
    }

    public List<Integer> getConsumerPeerPortList() {
        return externalChainConsumerConfiguration.getExternalPeerPortList();
    }

    public List<AddressPort> getConsumerPeerAddressPortList() {
        return externalChainConsumerConfiguration.getExternalPeerAddressPortList();
    }

    // Only for test
    public void testAddProducerPeerAddress(String chainId, String address, int port) {
        getProducerP2PConfiguration(chainId).testAddExternalPeerAddress(address, port);
    }

    // Only for test
    public void testAddConsumerPeerAddress(String chainId, String address, int port) {
        getProducerP2PConfiguration(chainId).testAddExternalPeerAddress(address, port);
    }

    // Only for test
    public void testAddInternalPeerAddress(String address, int port) {
        if (null == peerIPList) peerIPList = new ArrayList<>();
        if (null == peerPortList) peerPortList = new ArrayList<>();
        if (null == peerAddressPortList) peerAddressPortList = new ArrayList<>();

        peerIPList.add(address);
        peerPortList.add(port);
        peerAddressPortList.add(new AddressPort(address, port));
    }

    public boolean isExternal(String address, int port) {
        return isConsumerPeer(address,port) || isProducerPeer(address, port);
    }

    public boolean isProducerPeer(String address, int port) {
        return externalChainProducerConfiguration.isExternalPeer(address, port);
    }

    public boolean isConsumerPeer(String address, int port) {
        return  externalChainConsumerConfiguration.isExternalPeer(address, port);
    }

    public boolean isInternalPeer(String address, int port) {
        List<AddressPort> externalPeerAddress = getPeerAddressPortList();
        for (AddressPort addressPort : externalPeerAddress) {
            if (addressPort.isTheSame(address, port)) {
                return true;
            }
        }
        return false;
    }

    private List<AddressPort> loadPeerAddressFromFile(String fileName) {
        return StringHelper.loadIPAndPortFromFile(fileName);
    }

    public void setMeanWaitTime(int meanWaitTime) {
        gaussianHelper.setMeanValue(meanWaitTime);
    }

    public void setDeviationWaitTime(int deviationWaitTime) {
        gaussianHelper.setDeviationValue(deviationWaitTime);
    }

    // set max time to 0 means no wait time.
    public void stopWaitTime() {
        gaussianHelper.stop();
    }

    // milli- seconds
    // Gaussian wait time
    public int getWaitTime() {
        return gaussianHelper.getWaitTime();
    }

    public void testSetRequiredZeroCount(String chainId, int difficult) {
        getProducerP2PConfiguration(chainId).testSetDifficult(difficult);
    }
}

package com.scu.suhong.network;

import consensus.pow.MiningConfiguration;
import util.AddressPort;
import util.GaussianHelper;
import util.StringHelper;

import java.util.ArrayList;
import java.util.List;

// Simple means, the configuration file only has the ip list - used for consumer
// While for producer, it has more content and then it has to be derived
public class SimpleP2PConfiguration {
    String externalPeerAddressConfigurationFile = "";
    int defaultPort = 0;
    private List<String> externalPeerAddressList;
    private List<Integer> externalPeerPortList;
    private List<AddressPort> externalPeerAddressPortList;
    private List<String> externalPeerAddressListWithoutSelf;
    private List<Integer> externalPeerPortListWithoutSelf;

    SimpleP2PConfiguration(String configurationFile) {
        this.externalPeerAddressConfigurationFile = configurationFile;
        defaultPort = StringHelper.getDefaultPort();
    }

    public List<String> getExternalPeerAddressListWithoutSelf() {
        if (null == externalPeerAddressListWithoutSelf) {
            loadExternalPeerAddressListWithoutSelf();
        }
        return externalPeerAddressListWithoutSelf;
    }

    public List<Integer> getExternalPeerPortListWithoutSelf() {
        if (null == externalPeerPortListWithoutSelf) {
            loadExternalPeerAddressListWithoutSelf();
        }
        return externalPeerPortListWithoutSelf;
    }

    private void loadExternalPeerAddressListWithoutSelf() {
        loadExternalPeerAddressList();
    }

    public List<String> getExternalPeerAddressList() {
        if (null == externalPeerAddressList) {
            loadExternalPeerAddressList();
        }
        return externalPeerAddressList;
    }

    public List<Integer> getExternalPeerPortList() {
        if (null == externalPeerPortList) {
            loadExternalPeerAddressList();
        }
        return externalPeerPortList;
    }

    public List<AddressPort> getExternalPeerAddressPortList() {
        if (null == externalPeerAddressPortList) {
            loadExternalPeerAddressList();
        }
        return externalPeerAddressPortList;
    }

    private boolean loadExternalPeerAddressList() {
        if (null == externalPeerAddressList) externalPeerAddressList = new ArrayList<>();
        if (null == externalPeerPortList) externalPeerPortList = new ArrayList<>();
        if (null == externalPeerAddressPortList) externalPeerAddressPortList = new ArrayList<>();
        if (null == externalPeerAddressListWithoutSelf) externalPeerAddressListWithoutSelf = new ArrayList<>();
        if (null == externalPeerPortListWithoutSelf) externalPeerPortListWithoutSelf = new ArrayList<>();

        List<AddressPort> addressPortList = getAddressListFromFile();
       if (null == addressPortList || addressPortList.isEmpty()) return false;

        for (AddressPort addressPort : addressPortList) {
            if (!addressPort.isValid()) {
                continue;
            }
            externalPeerAddressList.add(addressPort.getAddress());
            externalPeerPortList.add(addressPort.getPort());
            externalPeerAddressPortList.add(addressPort);
            if (!MiningConfiguration.isSelf(addressPort)) {
                externalPeerAddressListWithoutSelf.add(addressPort.getAddress());
                externalPeerPortListWithoutSelf.add(addressPort.getPort());
            }
        }
        return true;
    }

    public List<AddressPort> getAddressListFromFile(){
       return StringHelper.loadIPAndPortFromFile(externalPeerAddressConfigurationFile);
    }

    // Only for test
    public void testAddExternalPeerAddress(String address, int port) {
        if (null == externalPeerAddressList) externalPeerAddressList = new ArrayList<>();
        if (null == externalPeerPortList) externalPeerPortList = new ArrayList<>();
        if (null == externalPeerAddressPortList) externalPeerAddressPortList = new ArrayList<>();
        externalPeerAddressList.add(address);
        externalPeerPortList.add(port);
        externalPeerAddressPortList.add(new AddressPort(address, port));
    }

    public boolean isExternalPeer(String address, int port) {
        List<AddressPort> externalPeerAddress = getExternalPeerAddressPortList();
        for (AddressPort addressPort : externalPeerAddress) {
            if (addressPort.isTheSame(address, port)) {
                return true;
            }
        }
        return false;
    }
}

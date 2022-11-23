package com.scu.suhong.transaction.exchangeMode;

import com.scu.suhong.graph.JGraphTWrapper;
import util.NoDuplicatedRandomHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EMTransactionServiceMap {
    // ContractNumber -> JGraphTWrapper
    Map<Integer, JGraphTWrapper> jGraphTWrapperHashMap = new HashMap<>();
    // ContractNumber -> JGraphTWrapper to assume one exchange loop have only one random system
    Map<Integer, NoDuplicatedRandomHelper> noDuplicatedRandomHelperMap = new HashMap<>();
    // ContractNumber -> EMTransaction list to support the ratio test
    Map<Integer, List<EMTransaction>> contractToEMTransactionList = new HashMap<>();
    // Random number to one EMTransaction - randomEMTransactionId as weight
    // As the edge in Graphviz cannot accept string, then we have to generate one unique number to map weight and transaction
    Map<Integer, EMTransaction> emWeightTransactionMap = new HashMap<>();
    // For reversely lookup the randomEMTransactionId
    // EM hash -> randomEMTransactionId as weight to form the diagram
    Map<String, Integer> emTransactionWeightMap = new HashMap<>();
    // Used to prevent duplicated add one transaction into the loop
    Map<String, EMTransaction> usedEmTransactionMap = new HashMap<>();
    // Used to record the processed transaction
    Map<Integer, Integer> processedContractNumberMap = new HashMap<>();

    int maxNoDuplicatedRandomNumber = 65535;

    public void reset(){
        jGraphTWrapperHashMap.clear();
        emWeightTransactionMap.clear();
        emTransactionWeightMap.clear();
        noDuplicatedRandomHelperMap.clear();
        processedContractNumberMap.clear();
    }

    public boolean isContractProcessDone(Integer contractNumber){
        return null != processedContractNumberMap.get(contractNumber);
    }

    public void markContractAsDone(Integer contractNumber){
        processedContractNumberMap.put(contractNumber, contractNumber);
    }

    public void addEMTransaction(EMTransaction emTransaction){
        List<EMTransaction> txList = contractToEMTransactionList.get(emTransaction.getContractNumber());
        if (null == txList) {
            txList = new ArrayList<>();
            txList.add(emTransaction);
            contractToEMTransactionList.put(emTransaction.getContractNumber(), txList);
        } else {
            txList.add(emTransaction);
            contractToEMTransactionList.replace(emTransaction.getContractNumber(), txList);
        }
    }

    public List<EMTransaction> getTransactionList(Integer contractNumber){
        return contractToEMTransactionList.get(contractNumber);
    }

    public EMTransaction getWithdrawalTransaction(Integer contractNumber, String cursorTransactionAddress){
        List<EMTransaction> list = contractToEMTransactionList.get(contractNumber);
        if (null == list) return null;
        for (EMTransaction t : list){
            // The incoming transaction's 'to' field is the 'from' field of the this transaction
            if (t.getIncomingAddress().equals(cursorTransactionAddress)){
                return t;
            }
        }
        return null;
    }

    public EMTransaction getPaymentTransaction(Integer contractNumber, String cursorTransactionAddress){
        List<EMTransaction> list = contractToEMTransactionList.get(contractNumber);
        if (null == list) return null;
        for (EMTransaction t : list){
            if (t.getTo().equals(cursorTransactionAddress)){
                return t;
            }
        }
        return null;
    }

    public boolean tryAdd(EMTransaction emTransaction){
        if (!emTransaction.isValid()){
            System.out.println("[EMTransactionServiceMap][INFO] EMTransaction is not valid");
            return false;
        }
        if (null != usedEmTransactionMap.get(emTransaction.getHash())) {
            System.out.println("[EMTransactionServiceMap][INFO] EMTransaction has already been added");
            return false;
        }

        usedEmTransactionMap.put(emTransaction.getHash(), emTransaction);
        NoDuplicatedRandomHelper randomHelper = tryGetNoDuplicatedRandomHelper(emTransaction.getContractNumber());
        int emTransactionRandomId = randomHelper.getNumber();
        emWeightTransactionMap.put(emTransactionRandomId, emTransaction);
        emTransactionWeightMap.put(emTransaction.getHash(), emTransactionRandomId);
        addEMTransaction(emTransaction);
        return true;
    }

    public int tryGetTransactionWeight(EMTransaction emTransaction){
        return emTransactionWeightMap.get(emTransaction.getHash());
    }

    public JGraphTWrapper tryGetJGraphTWrapper(EMTransaction emTransaction) {
        return tryGetJGraphTWrapper(emTransaction.getContractNumber());
    }
    public JGraphTWrapper tryGetJGraphTWrapper(Integer contractNumber) {
        JGraphTWrapper jGraphTWrapper = getJGraphTWrapper(contractNumber);
        if (null == jGraphTWrapper){
            jGraphTWrapper = new JGraphTWrapper();
            addGraphTWrapper(contractNumber, jGraphTWrapper);
        }
        return jGraphTWrapper;
    }

    public JGraphTWrapper getJGraphTWrapper(Integer contractNumber) {
        return jGraphTWrapperHashMap.get(contractNumber);
    }

    public boolean tryAddJGraphTWrapper(Integer contractNumber, JGraphTWrapper jGraphTWrapper) {
        if (null == getJGraphTWrapper(contractNumber)){
            addGraphTWrapper(contractNumber, jGraphTWrapper);
            return true;
        }
        return false;
    }

    public void addGraphTWrapper(Integer contractNumber, JGraphTWrapper jGraphTWrapper) {
        this.jGraphTWrapperHashMap.put(contractNumber, jGraphTWrapper);
    }

    public EMTransaction getEmTransaction(Integer emTransactionWeight) {
        return emWeightTransactionMap.get(emTransactionWeight);
    }

    public boolean tryAddEmTransaction(Integer emTransactionRandomId, EMTransaction emTransaction) {
        if (null == getEmTransaction(emTransactionRandomId)){
            addEmTransaction(emTransactionRandomId, emTransaction);
            return true;
        }
        return false;
    }

    public void addEmTransaction(Integer emTransactionRandomId, EMTransaction emTransaction) {
        this.emWeightTransactionMap.put(emTransactionRandomId, emTransaction);
    }

    public NoDuplicatedRandomHelper tryGetNoDuplicatedRandomHelper(Integer contractNumber) {
        NoDuplicatedRandomHelper noDuplicatedRandomHelper = getNoDuplicatedRandomHelper(contractNumber);
        if (null == noDuplicatedRandomHelper){
            noDuplicatedRandomHelper = new NoDuplicatedRandomHelper(maxNoDuplicatedRandomNumber);
            addNoDuplicatedRandomHelper(contractNumber, noDuplicatedRandomHelper);
        }
        return noDuplicatedRandomHelper;
    }

    public NoDuplicatedRandomHelper getNoDuplicatedRandomHelper(Integer contractNumber) {
        return noDuplicatedRandomHelperMap.get(contractNumber);
    }

    public boolean tryAddNoDuplicatedRandomHelper(Integer contractNumber, NoDuplicatedRandomHelper noDuplicatedRandomHelper) {
        if (null == getNoDuplicatedRandomHelper(contractNumber)){
            addNoDuplicatedRandomHelper(contractNumber, noDuplicatedRandomHelper);
            return true;
        }
        return false;
    }
    public void addNoDuplicatedRandomHelper(Integer contractNumber, NoDuplicatedRandomHelper noDuplicatedRandomHelper) {
        this.noDuplicatedRandomHelperMap.put(contractNumber, noDuplicatedRandomHelper);
    }

    public void setMaxNoDuplicatedRandomNumber(int maxNoDuplicatedRandomNumber) {
        this.maxNoDuplicatedRandomNumber = maxNoDuplicatedRandomNumber;
    }
}

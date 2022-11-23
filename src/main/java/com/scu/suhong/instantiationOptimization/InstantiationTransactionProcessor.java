package com.scu.suhong.instantiationOptimization;

import com.scu.suhong.block.Block;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InstantiationTransactionProcessor {
    static InstantiationTransactionProcessor instance;
    static final String instantiationTxKeyword = "InstantiationTx";
    static final String newSmartContract = "NewSmartContract";
    static final String newAndInvocation = "NewAndCallMethod";
    static final String disposableInvocation = "Disposable"; // new and process and terminate
    static final String innerSmartContract = "innerSmartContract"; // new and process and terminate
    static final String method = "Method";
    static String fieldSeparator = SmartContractHelper.getFieldSeparator();

    static public synchronized InstantiationTransactionProcessor getInstance(){
        if (null == instance){
            instance = new InstantiationTransactionProcessor();
        }
        return instance;
    }

    public boolean process(Transaction t){
        String d = t.getData();
        if (!d.startsWith(instantiationTxKeyword)){
            return false;//not processed
        }

        // 0 is instantiationTxKeyword, 1 is new or method, later is parameters
        String[] dataArray = d.split(SmartContractHelper.getFieldSeparator());
        if (dataArray.length < 4) {
            System.out.printf("[InstantiationTransactionProcessor][WARN] Parameters '%s' are less than 4. Skip to process\n", d);
            return false;
        }
        String keyword = dataArray[1];
        // parameterArray does not contain instantiationTxKeyword and keyword
        String[] parameterArray = Arrays.copyOfRange(dataArray, 2, dataArray.length);
        if (keyword.equals(newSmartContract)){
            SmartContractHelper.processNewInstance(parameterArray);

        } else if (keyword.equals(method)){
            return SmartContractHelper.processMethod(parameterArray);

        } else if (keyword.equals(newAndInvocation)){
            return null == SmartContractHelper.processNewAndInvocation(parameterArray, d);

        } else if (keyword.equals(disposableInvocation)){
            return SmartContractHelper.processDisposable( parameterArray, d);

        } else if (keyword.equals(innerSmartContract)){
            return SmartContractHelper.processInnerSmartContract( parameterArray, d);
        } else {
            System.out.println("[InstantiationTransactionProcessor][WARN] Unknown instantiation keyword " + keyword);
            return false;
        }
        return true;
    }


    public void tryAddNewBlock(Block block) {
        // Termination Tx should be processed at last, or other request may be sealed in the same block
        // which is not processed as we cannot ensure the sequence of Txs
        Transaction newInstanceTransaction = null;
        List<Transaction> normalTransactionList = new ArrayList<>();
        Transaction delayToProcessedTerminationTx = null;
        for (AbstractTransaction at : block.getTransactions()) {
            if (!(at instanceof Transaction)) continue;

            Transaction t = (Transaction) at;
            if (isTerminationTx(t)){
                delayToProcessedTerminationTx = t;
            } else if (isNewInstanceTransaction(t)) {
                newInstanceTransaction = t;
            } else{
                normalTransactionList.add(t);
            }
        }

        if (null != newInstanceTransaction)
        {
            System.out.println("[InstantiationTransactionProcessor][INFO]Process new instance request firstly");
            process(newInstanceTransaction);
        }

        for (Transaction t: normalTransactionList){
            System.out.println("[InstantiationTransactionProcessor][INFO]Process normal transaction request");
            process(t);
        }

        if (null != delayToProcessedTerminationTx){
            System.out.println("[InstantiationTransactionProcessor][INFO]Process delayed termination request at last");
            process(delayToProcessedTerminationTx);
        }

    }

    boolean isTerminationTx(Transaction t){
        String d = t.getData();
        return d.contains(SmartContractHelper.getTerminationRequest());
    }

    boolean isNewInstanceTransaction(Transaction t){
        String d = t.getData();
        return d.contains(newSmartContract) || d.contains(newAndInvocation);
    }
}

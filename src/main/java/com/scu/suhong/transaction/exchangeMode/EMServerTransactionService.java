package com.scu.suhong.transaction.exchangeMode;

import util.ThreadHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EMServerTransactionService extends  EMTransactionService implements Runnable {
    List<EMServerTransaction> serverTransactions;
    static EMServerTransactionService serverTransactionThreadInstance;
    boolean isForceStop;


    public void setForceStop(boolean forceStop) {
        isForceStop = forceStop;
    }

    EMServerTransactionService() {
        serverTransactions = new ArrayList<>();
        isForceStop = false;
    }

    public static synchronized EMServerTransactionService getInstance(){
        if (null == serverTransactionThreadInstance) {
            serverTransactionThreadInstance = new EMServerTransactionService();
        }
        return serverTransactionThreadInstance;
    }

    public void addServerTransaction(EMServerTransaction serverTransaction){
        serverTransactions.add(serverTransaction);
    }

    public boolean tryProcess(EMTransaction emTransaction) {
        boolean r = true;
        System.out.printf("[EMServerTransactionService][Info] Received transaction with %d repeat\n", emTransaction.getTimes());
        for (int i = 0; i < emTransaction.getTimes(); ++i){
            System.out.printf("[EMServerTransactionService][Info] Try to process number %d of the transaction\n", i + 1);
            emTransaction.setLoopNumber(i + 1); // times start from 1
            emTransaction.calculateDataHash();
            if (!tryProcessOneTimeTransaction(emTransaction)){
                r = false;
            };
        }
        return r;
    }

    public boolean tryProcessOneTimeTransaction(EMTransaction emTransaction) {
        for(EMServerTransaction serverTransaction : serverTransactions){
            // address is the same and  the condition should be the same
            if (emTransaction.getTo().equals(serverTransaction.getFrom())){

                if (!checkToAndIncomingCondition(emTransaction, serverTransaction)) {
                    System.out.println("[EMServerTransactionService] Client condition doesn't match the server condition, skip to process it");
                    return false;
                }

                if (!serverTransaction.tryProcessClientTransaction(emTransaction)){
                    System.out.println("[EMServerTransactionService] Cannot process EMTransaction");
                    return false;
                }

                System.out.println("[EMServerTransactionService] Finished to process transaction from " + emTransaction.getFrom());
                return true;
            }
        }
        return false;
    }

    public boolean checkToAndIncomingCondition(EMTransaction emTransaction, EMServerTransaction serverTransaction) {
        // Get the incoming transaction of emTransaction
        if (!isPaymentWithdrawalConditionMatch(emTransaction, serverTransaction)) {
            System.out.println("[EMServerTransactionService][Warning] Transaction doesn't match its incoming condition");
            return false;
        }
        // One transaction can be the payment and the withdrawal in the exchange
        // payment->A->withdrawal
        if (!isPaymentWithdrawalConditionMatch(serverTransaction, emTransaction)) {
            System.out.println("[EMServerTransactionService][Warning] Transaction doesn't match its out-going condition");
            return false;
        }

        return true;
    }

    @Override
    public void run() {
        while (!serverTransactions.isEmpty() && !isForceStop){
            System.out.println("[EMServerTransactionService][Debug] Try to check and process client transaction");
            ThreadHelper.safeSleep(5000); // 5 seconds to process the transaction
            Iterator<EMServerTransaction> it = serverTransactions.iterator();
            while (it.hasNext()){
                EMServerTransaction serverTransaction = it.next();
                serverTransaction.processUnprocessedClientTransactions();
                if (!serverTransaction.canPerformService()){
                    serverTransaction.shutDownService();
                    it.remove();
                }
            }
            //when no server transaction, break
            if (serverTransactions.isEmpty()) break;
        }
    }
}

package model;

import util.PoissonHelper;
import util.ThreadHelper;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OneChain {
    static final int miningTxNumber = 10;
    //default mining time is 10 seconds
    PoissonHelper miningTimeRandom = new PoissonHelper(10);
    PoissonHelper produceRandom = new PoissonHelper(10);
    int produceTurn = 0;

    static final int simulateNodeCount = 2;

    int conditionBlockCount = 0;

    boolean shouldStop = false;
    String conditionCountBlockCharacterString = "";
    String specialTransaction = "";
     String invalidConditionCountBlockCharacterString = "";

    public static int getMiningTxNumber() {
        return miningTxNumber;
    }

    public void setMiningMeanTime(int miningTime){
        miningTimeRandom = new PoissonHelper(miningTime);
    }

    public void setStop() {
        this.shouldStop = true;
    }

    public int getConditionBlockCount() {
        return conditionBlockCount;
    }

    public void beginCountBlock(){
        conditionBlockCount = 0;
    }

    public void beginConditionCountBlock(String characterString){
        // clear now and will only count after the key characters are found
        conditionCountBlockCharacterString = characterString;
        conditionBlockCount = 0;
    }

    public void setInvalidConditionCountBlock(String characterString){
        // clear now and will only count after the key characters are found
        invalidConditionCountBlockCharacterString = characterString;
        conditionBlockCount = 0;
    }

    public void setSpecialTransaction(String specialTransaction){
        this.specialTransaction = specialTransaction;
    }

    public void simulateNodeWithPreloadTransactions(int preLoadTransactionNumber)
    {
        Queue<String> txQueue = new ConcurrentLinkedQueue<>();
        for (int i = 0; !shouldStop; ++i ) {
            System.out.println("[simulateNode] simulate count: " + i);
            simulateProduce(txQueue, preLoadTransactionNumber);
            simulateMining(txQueue);
        }
    }

    public void simulateNode()
    {
        Queue<String> txQueue = new ConcurrentLinkedQueue<>();
        for (int i = 0; !shouldStop; ++i ) {
            System.out.println("[simulateNode] simulate count: " + i);
            simulateProduce(txQueue);
            simulateMining(txQueue);
        }
    }


    public void simulateProduce(Queue<String> transactions){
        simulateProduce(transactions,0);
    }

    public void simulateProduce(Queue<String> transactions, int preLoadTransactions){
        int produceNumber = produceRandom.getPoissonRandom();
        String producedTx = null;
        ++produceTurn;
        for (int i = 0; i < produceNumber + preLoadTransactions; ++i){
            producedTx = "Tx turn " + produceTurn + " with Tx number: " + i;
            transactions.add(producedTx);
            //System.out.printf("\t[Produce] produce TX: %s\n", producedTx);
        }
        int specialTx = 0;
        if (!specialTransaction.isEmpty()){
            transactions.add(specialTransaction);
            specialTransaction = "";
            ++specialTx;
        }
        System.out.printf("\t[Produce] produce %d TXs\n", produceNumber + specialTx);
    }

    public void simulateMining(Queue<String> transactions) {
        boolean isTxProcessed = false;
        for (int i = 0; i < miningTxNumber && !transactions.isEmpty(); ++i) {
            isTxProcessed = true;
            String tr = transactions.poll();
            if (tr.equals(conditionCountBlockCharacterString)){
                System.out.println("Special TX has been found!");
                conditionCountBlockCharacterString = "";
            }
            //System.out.println("\t[Mining] Package the Tx to " + tr);
        }
        // simulating the mining process
        if (isTxProcessed){
            int sleepTime  = miningTimeRandom.getPoissonRandom();
            System.out.printf("\t[Mining] Begin to simulate mining for : %d seconds\n", sleepTime);
            ThreadHelper.safeSleepSecond(sleepTime);
            System.out.printf("\t[Mining] Succeed to seal one block\n");
        }
        if (!invalidConditionCountBlockCharacterString.isEmpty()){
            conditionBlockCount = 0;
        } else if (conditionCountBlockCharacterString.isEmpty()) {
            ++conditionBlockCount;
        }
    }

}

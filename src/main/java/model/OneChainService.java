package model;

public class OneChainService{
    int preLoadTransactionNumber;

    public void startService(OneChain oneChain, int preLoadTransactionNumber){
        OneChainRunnable runable = new OneChainRunnable(oneChain);
        this.preLoadTransactionNumber = preLoadTransactionNumber;
        Thread thread = new Thread(runable);
        thread.start();
    }

    public void startService(OneChain oneChain){
        startService(oneChain, 0);
    }

    public class OneChainRunnable implements Runnable {
        OneChain oneChain = null;

        public OneChainRunnable(OneChain oneChain) {
            this.oneChain = oneChain;
        }

        @Override
        public void run() {
            oneChain.simulateNodeWithPreloadTransactions(preLoadTransactionNumber);
        }
    }
}

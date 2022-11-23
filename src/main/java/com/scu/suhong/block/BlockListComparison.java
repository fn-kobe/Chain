package com.scu.suhong.block;

import com.scu.suhong.dynamic_definition.TransactionHelper;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.CrosschainInterface;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockListComparison {
    List<AbstractTransaction> disappearedTxList;
    List<AbstractTransaction> newTxList;

    public BlockListComparison() {
        disappearedTxList = new ArrayList<>();
        newTxList = new ArrayList<>();
    }

    // Incoming list: has the "RD:" starts in data fields to mark as disappeared by rebranch of the neighbor
    // Outcoming all the transactions should be disappeared
    // The reason is that we cann't delete it from the blockchain in our blockchain although it is disappeared in neighbored blockchain by rebranch
    // Notice: there may be possible rebranch in current blockchain and the neighbored blockchain
    public static void markTransactionAsDisappearedWhenNeighborRebranch(List<Block> orginalBlockList) {
        // find all disappeared transaction when rebranch
        List<AbstractTransaction> rebranchMarkedTransactionList = new ArrayList<>();
        for (int i = orginalBlockList.size() - 1; i >= 0; --i) {
            Block block = orginalBlockList.get(i);
            for (AbstractTransaction t : block.getTransactions()) {
                if (TransactionHelper.doesMarkedAsDisappearByNeighborRebranch(t)) {
                    // Currently, we only limit the disappear in crosschain transaction and in neighbor blockchain
                    if (!(t instanceof CrosschainInterface) && t.isExternalTransaction())
                        continue;

                    rebranchMarkedTransactionList.add(t);
                }
            }
        }

        // remove later transaction, assume blocklist is long and disappeared Tx is short
        for (int i = 0; i < orginalBlockList.size(); ++i) {
            Block block = orginalBlockList.get(i);
            for (AbstractTransaction t : block.getTransactions()) {
                if (shouldBeMarkedAsDisapeared(t, block.getBlockIndex(), rebranchMarkedTransactionList)) {
                    // we just mark the transaction
                    TransactionHelper.markAsDisappeared(t);
                }
            }
        }
    }

    static boolean shouldBeMarkedAsDisapeared(AbstractTransaction t, int transactionBlockId, List<AbstractTransaction> rebranchMarkedTransactionList) {
        // Currently, we only limit the disappear in crosschain transaction and in neighbor blockchain
        if (!(t instanceof CrosschainInterface) && t.isExternalTransaction()) return false;

        for (AbstractTransaction at : rebranchMarkedTransactionList) {
            int markedTransactionBlockId = at.getBlockIndex();
            if (markedTransactionBlockId > transactionBlockId) {
                if (t.isSimilar(at)) return true;
            }
        }
        return false;
    }

    // We want to find the disappeared and the new transaction in the new blockchain list by  block order
    // Assume new blockchain list is not shorter than the old
    public void compare(List<Block> oldBlockList, List<Block> newBlockList) {
        disappearedTxList = new ArrayList<>();
        newTxList = new ArrayList<>();

        if (oldBlockList == null || null == newBlockList) {
            System.out.println("[BlockListComparison][WARN] One of the blocklist to compare is empty");
            return;
        }

        if (oldBlockList.size() > newBlockList.size()) {
            System.out.println("[BlockListComparison][WARN] Size of the old blockchain list is not shorter than the new");
            return;
        }

        for (int i = 0; i < oldBlockList.size(); ++i) {
            List<AbstractTransaction> oldTransactionList = oldBlockList.get(i).getTransactions();
            List<AbstractTransaction> newTransactionList = newBlockList.get(i).getTransactions();
            int postion = -1;
            for (AbstractTransaction t : oldTransactionList) {
                postion = findPositionInAnotherList(t, newTransactionList);
                if (-1 == postion) {
                    //tryAddToListWithDisappearenceCheck(t, disappearedTxList);
                    disappearedTxList.add(t);
                    System.out.printf("[BlockListComparison][INFO] transaction %d disappears from block at " + oldBlockList.get(i).getBlockIndex() + "\n",
                            t.getId());
                }
            }
            for (AbstractTransaction t : newTransactionList) {
                //if (!findPositionInAnotherList(t, oldTransactionList)) tryAddToListWithDisappearenceCheck(t, newTxList);
                postion = findPositionInAnotherList(t, oldTransactionList);
                if (-1 == postion) {
                    newTxList.add(t);
                    System.out.printf("[BlockListComparison][INFO] transaction %d appear at block  " + postion + "\n",
                            t.getId());
                }
            }
        }

        // The transactions in one blockchain will be added all
        for (int i = oldBlockList.size(); i < newBlockList.size(); ++i) {
            List<AbstractTransaction> newTransactionList = newBlockList.get(i).getTransactions();
            for (AbstractTransaction t : newTransactionList) {
                //tryAddToListWithDisappearenceCheck(t, newTxList);
                newTxList.add(t);
                System.out.printf("[BlockListComparison][INFO] transaction %d appear at block  " + i + "\n",
                        t.getId());
            }
        }
        //remove the transaction which has been both removed and added
        removeBothNewAndDisappearedTx();
    }

    public void removeBothNewAndDisappearedTx() {
        Iterator<AbstractTransaction> disappearedTxIt = disappearedTxList.iterator();
        while (disappearedTxIt.hasNext()){
            AbstractTransaction disappearedTx = disappearedTxIt.next();

            Iterator<AbstractTransaction> newTxIt = newTxList.iterator();
            while (newTxIt.hasNext()){
                AbstractTransaction newTx = newTxIt.next();
                if (-1 == newTx.getId() || -1 == disappearedTx.getId()){
                    System.out.println("[BlockListComparison][WARN] Transaction id is -1");
                    continue;
                }
                if (newTx.getId() == disappearedTx.getId() && newTx.getBlockchainId().equals(disappearedTx.getBlockchainId())){
                    newTxIt.remove();
                    disappearedTxIt.remove();
                }
            }
        }
    }

    int findPositionInAnotherList(AbstractTransaction transaction, List<AbstractTransaction> anotherList) {
        for (int i = 0; i < anotherList.size(); ++i) {
            if (transaction.getJson().similar(anotherList.get(i).getJson())) return i;
        }
        return -1;
    }

    public List<AbstractTransaction> getDisappearedTxList() {
        return disappearedTxList;
    }

    public List<AbstractTransaction> getNewTxList() {
        return newTxList;
    }

    public boolean isEmpty() {
        return getDisappearedTxList().isEmpty() && getNewTxList().isEmpty();
    }
}

package com.scu.suhong.transaction;

import account.AccountManager;
import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.block.Block;
import com.scu.suhong.block.BlockChain;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import consensus.pow.MiningConfiguration;
import org.json.JSONObject;
import util.JSONObjectHelper;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import static com.scu.suhong.dynamic_definition.TransactionHelper.getSymbolSeparator;

public class CrosschainTransaction extends Transaction implements CrosschainInterface {
    int blockchainId = -1;
    int interactionId = 0;
    private CrosschainTransaction requiredCrosschainTransaction = null;

    public CrosschainTransaction(int interactionId) {
        this.blockchainId = MiningConfiguration.getBlockchainId();
        this.interactionId = interactionId;
    }

    public CrosschainTransaction(int blockchainId, int interactionId) {
        this.blockchainId = blockchainId;
        this.interactionId = interactionId;
    }

    public CrosschainTransaction(Transaction transaction, int blockchainId, int interactionId) {
        super(transaction);
        this.blockchainId = blockchainId;
        this.interactionId = interactionId;
    }

    // The data may be added prefix "*D:", we try to remove it
    public String getRealData(){
        String originalData = data;
        String[] dataList = originalData.split(getSymbolSeparator());
        if (1 == dataList.length) return originalData;

        return dataList[1];
    }

    @Override
    public boolean isSimilar(AbstractTransaction t2) {
        if (t2 instanceof CrosschainTransaction) {
            return isTransactionSameInExternalCase(this, (CrosschainTransaction) t2);
        }else{
            return this.isSimilar(t2);
        }
    }

    public static boolean isTransactionSameInExternalCase(CrosschainTransaction exTx1, CrosschainTransaction exTx2) {
        if (null == exTx1 || null == exTx2) return false;

        if (!exTx1.getBlockchainId().equals(exTx2.getBlockchainId())) return false;
        if (exTx1.getInteractionId() != exTx2.getInteractionId()) return false;
        if (!exTx1.from.equals(exTx2.from)) return false;
        if (!exTx1.to.equals(exTx2.to)) return false;
        if (exTx1.getValue() != exTx2.getValue()) return false;

        return true;
    }

    public static boolean doesCrosschainTransactionExistBlockchain(CrosschainTransaction exTx) {
        List<Block> blockList = BlockChain.getInstance().getBlockList();
        for (Block block : blockList) {
            List<AbstractTransaction> transactionList = block.getTransactions();
            for (AbstractTransaction transactionFromBlock : transactionList) {
                if (!(transactionFromBlock instanceof CrosschainTransaction)) continue;

                CrosschainTransaction exTransactionFromBlock = (CrosschainTransaction) transactionFromBlock;
                if (!isTransactionSameInExternalCase(exTransactionFromBlock, exTx)) continue;

                return true;
            }
        }
        return false;
    }

    static public int safeGetPositiveInt(JSONObject object, String key) {
        int value = -1;
        try {
            value = JSONObjectHelper.safeGetInt(object, key);
        } catch (NumberFormatException e) {
            logger.warn("[Condition] No valid value in condition " + JSONObjectHelper.safeGetString(object, key));
        }
        return value;
    }

    static public CrosschainTransaction createFromJson(JSONObject exTx) {
        if (!exTx.has("reqTx")) { // One transaction must have Data
            return null;
        }

        int blockchainId = safeGetPositiveInt(exTx, "blockchainId");
        int interactionId = safeGetPositiveInt(exTx, "interactionId");
        Transaction transaction = Transaction.createFromJson((JSONObject) exTx.get("super"));
        CrosschainTransaction crosschainTransaction = new CrosschainTransaction(transaction, blockchainId, interactionId);

        JSONObject reqTx = (JSONObject) exTx.get("reqTx");
        CrosschainTransaction reqCrosschainTransaction = null;
        if (reqTx.has("super")) {
            int reqBlockchainId = safeGetPositiveInt(reqTx, "blockchainId");
            int reqInteractionId = safeGetPositiveInt(reqTx, "interactionId");
            Transaction reqTransaction = Transaction.createFromJson((JSONObject) reqTx.get("super"));
            reqCrosschainTransaction = new CrosschainTransaction(reqTransaction, reqBlockchainId, reqInteractionId);
        }
        crosschainTransaction.addRequiredCrosschainTransaction(reqCrosschainTransaction);
        return crosschainTransaction;
    }

    // We will use full name for external transactions
    @Override
    public void setFrom(String from) {
        if (null == from || from.isEmpty()) {
            System.out.println("[CrosschainTransaction] Sender address is empty and we skip setting it");
            return;
        }

        // The blockchainId maybe default(current blockchainId) or another blockchainId(i.e. of the requiredCrosschainTransaction)
        from = AccountManager.getFullAddress(from, blockchainId);
        super.setFrom(from);
    }

    @Override
    public void setToAndValue(String to, int value) {
        if (null == to || to.isEmpty()) {
            System.out.println("[CrosschainTransaction] Receiver address is empty and we skip setting it");
            return;
        }

        to = AccountManager.getFullAddress(to, blockchainId);
        super.setToAndValue(to, value);
    }

    public boolean doesMatched(final List<CrosschainTransaction> crosschainTransactionList) {
        System.out.printf("[CrosschainTransaction][Debug] Current external transaction (%s) from %s to %s\n",
                doesCrosschainTransactionExistBlockchain(this) ? "in blockchain" : "not in blockcchain", this.from, this.to);

        CrosschainTransaction exTx = requiredCrosschainTransaction;
        if (null == exTx) {
            System.out.println("[CrosschainTransaction][Debug] No requiredExternal transaction for current transaction");
            return false;
        }
        while (exTx != null) {
            System.out.printf("[CrosschainTransaction][Debug] Required external transaction (%s) from %s to %s \n",
                    doesCrosschainTransactionExistBlockchain(exTx) ? "in blockchain" : "not in blockcchain", exTx.from, exTx.to);
            // No required Tx exists, we wait to check further and here just return false
            if (!doesCrosschainTransactionExistBlockchain(exTx) || !doesCrosschainTransactionExistBlockchain(this)) {
                return false;
            }

            // A -> B -> C -> .. -> A, this cover the last ->A,assume no branch happens
            // We have checked every transaction and back to us. The circle is formed, returnd true.
            if (CrosschainTransaction.isTransactionSameInExternalCase(exTx, this)) {
                return true;
            }

            if (null == exTx.requiredCrosschainTransaction) { // we try format the global view of the required external loop
                CrosschainTransaction findRequireExTx = findSameExTxFromReceivedTxList(exTx, crosschainTransactionList);
                if (null != findRequireExTx && null != findRequireExTx.requiredCrosschainTransaction) {
                    exTx.requiredCrosschainTransaction = findRequireExTx.requiredCrosschainTransaction;
                }
            }
            exTx = exTx.requiredCrosschainTransaction;
        }
        System.out.println("[CrosschainTransaction][Debug] No match fouund");
        return false;
    }

    CrosschainTransaction findSameExTxFromReceivedTxList(final CrosschainTransaction exTx, final List<CrosschainTransaction> crosschainTransactionList) {
        if (null == exTx.requiredCrosschainTransaction) {
            for (CrosschainTransaction t : crosschainTransactionList) {
                if (CrosschainTransaction.isTransactionSameInExternalCase(exTx, t)) return t;
            }
        }
        return null;
    }

    // Check whether requiredtx is the requiredCrosschainTransaction of originalTx
    public boolean isRequiredTransaction(CrosschainTransaction toBeCheckedTx) {
        return isTransactionSameInExternalCase(requiredCrosschainTransaction, toBeCheckedTx);
    }

    // return number > 0 when no error. -1 when error
    public String getBlockchainId() {
        return String.valueOf(blockchainId);
    }

    public int getInteractionId() {
        return interactionId;
    }

    public CrosschainTransaction getRequiredCrosschainTransaction() {
        return requiredCrosschainTransaction;
    }

    public void addRequiredCrosschainTransaction(CrosschainTransaction requiredCrosschainTransaction) {
        this.requiredCrosschainTransaction = requiredCrosschainTransaction;
    }

    @Override
    public String Dump() {
        return getJson().toString();
    }

    public JSONObject getJson() {
        JSONObject json = getSelfJson();

        JSONObject jsonReqTx = new JSONObject();
        if (null != requiredCrosschainTransaction) {
            jsonReqTx = requiredCrosschainTransaction.getSelfJson();
        }

        json.put("reqTx", jsonReqTx);
        return json;
    }

    JSONObject getSelfJson() {
        JSONObject json = new JSONObject();
        json.put("blockchainId", blockchainId);
        json.put("interactionId", interactionId);
        json.put("super", super.getJson());
        return json;
    }

    public int compareTo(Object o) {
        if (!(o instanceof CrosschainTransaction)) {
            return 1;
        }
        CrosschainTransaction otherTransaction = (CrosschainTransaction) o;
        return this.getHash().compareTo(otherTransaction.getHash());
    }

    public String getHash() {
        if (hash == null || hash.isEmpty()) {
            hash = calculateDataHash();
        }
        return hash;
    }

    public String calculateDataHash() {
        String h = super.calculateDataHash();
        try {
            h = MD5Hash.getValue(String.valueOf(blockchainId + h));
            h = MD5Hash.getValue(String.valueOf(interactionId + h));
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Cannot find MD5 algorithm in makeHash");
            e.printStackTrace();
        }
        return h;
    }
}

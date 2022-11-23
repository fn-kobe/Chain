package com.scu.suhong.block;

import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.Transaction;
import com.scu.suhong.transaction.TransactionFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BlockBody implements Serializable {
    List<AbstractTransaction> transactions = new CopyOnWriteArrayList<>();

    public void addTransaction(AbstractTransaction transaction) {
        transactions.add(transaction);
    }

    public void addTransaction(List<AbstractTransaction> transactions) {
        for (AbstractTransaction t: transactions
             ) {
            this.transactions.add(t);
        }
    }

    public String safeGetTransactionListRootHash() {
        try {
            return getTransactionListRootHash();
        } catch (BlockException e) {
            return "";
        }
    }

    // TO DO use real Merkle tree
    public String getTransactionHash() throws BlockException {
        if (transactions.isEmpty()){
            return null;
        }
        Collections.sort(transactions); // to make the MD5 unique
        String hash = "";
        for (AbstractTransaction transaction: transactions){
            try {
                hash = MD5Hash.getValue(transaction.getHash() + hash);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                throw new BlockException();
            }
        }
        return hash;
    }

    // TO DO use real Merkle tree
    public String safeGetTransactionHash()  {
        String r = "";
        try {
            r = getTransactionHash();
        } catch (BlockException e) {
            e.printStackTrace();
            return "";
        }
        return r;
    }

    // TO DO use real Merkle tree
    public String getTransactionListRootHash() throws BlockException {
        if (transactions.isEmpty()){
            return null;
        }
        Collections.sort(transactions); // to make the MD5 unique
        String hash = "";
        for (AbstractTransaction transaction: transactions){
            try {
                hash = MD5Hash.getValue(transaction.getData() + hash);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                throw new BlockException();
            }
        }
    return hash;
    }

    public boolean isValid()
    {
        for (AbstractTransaction t: transactions
                ) {
            if (!t.isValid()){
                return false;
            }
        }
        return true;
    }

    public String Dump(){
        String dump = "<body>\n";
        dump += "<transactions>\n";
        for (AbstractTransaction t: transactions
             ) {
            dump += t.Dump();
        }
        dump += "</transactions>\n";
        dump += "</body>\n";
        return dump;
    }

    public JSONObject getJson() {
        JSONObject json = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for (AbstractTransaction t: transactions
                ) {
            jsonArray.put(t.getJson());
        }
        json.put("ts", jsonArray);
        return json;
    }

    static public BlockBody createFromJson(JSONObject object){
        BlockBody blockBody = new BlockBody();
        JSONArray jsonArray = (JSONArray) object.get("ts");
        for (int i = 0; i < jsonArray.length(); ++i){
            JSONObject o = (JSONObject) jsonArray.get(i);
            if (TransactionFactory.isSupportedTransactionJson(o)) {
                AbstractTransaction t = TransactionFactory.createFromJson(o);
                if (null != t) {
                    blockBody.addTransaction(t);
                } else {
                    System.out.println("[Blockbody][ERROR] Create trasnaction for block failed");
                    return null;
                }
            }
        }
        return blockBody;
    }

    public List<AbstractTransaction> getTransactions() {
        return transactions;
    }

    public boolean isEqual(BlockBody body) {
        // currently we ignore thr body isEqual
        return true;
    }
}

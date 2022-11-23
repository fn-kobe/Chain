package com.scu.suhong.block;

import com.scu.suhong.transaction.Transaction;
import junit.framework.TestCase;
import org.junit.Test;

public class BlockBodyTest {

    @Test
    public void testGetTransactionListHash() throws BlockException {
        BlockBody blockBody1 = new BlockBody();
        BlockBody blockBody2 = new BlockBody();
        Transaction t1 = new Transaction();
        t1.setData("test1");
        t1.setHash();

        Transaction t2 = new Transaction();
        t2.setData("ntest2");
        t2.setHash();

        Transaction t3 = new Transaction();
        t3.setData("ztest3");
        t3.setHash();

        blockBody1.addTransaction(t1);
        blockBody1.addTransaction(t2);
        blockBody1.addTransaction(t3);

        blockBody2.addTransaction(t3);
        blockBody2.addTransaction(t2);
        blockBody2.addTransaction(t1);

        System.out.println(blockBody1.getTransactionListRootHash());
        assert(blockBody1.getTransactionListRootHash().equals(blockBody2.getTransactionListRootHash()));
        assert(blockBody1.getTransactionHash().equals(blockBody2.getTransactionHash()));
    }

    @Test
    public void testCreateFromJson() {
        BlockBody blockBody = createBlockBody("Test transaction Data");
        BlockBody blockBodyNew = BlockBody.createFromJson(blockBody.getJson());
        assert blockBodyNew.Dump().equals(blockBody.Dump());
    }

    static public BlockBody createBlockBody(String data) {
        BlockBody blockBody = new BlockBody();
        Transaction transaction = new Transaction();
        transaction.setData(data);
        return blockBody;
    }
}
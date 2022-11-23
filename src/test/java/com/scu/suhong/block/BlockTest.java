package com.scu.suhong.block;

import com.scu.suhong.dynamic_definition.DynamicalAsset;
import com.scu.suhong.transaction.Transaction;
import consensus.pow.MiningConfiguration;
import consensus.pow.PoW;
import junit.framework.TestCase;
import org.json.JSONObject;
import util.StringHelper;

public class BlockTest extends TestCase {

    public void testCreateFromJson() {
        BlockHeader h = new BlockHeader();
        String previousHash = "00F2E456";
        h.setPreviousHash(previousHash);

        BlockBody b = new BlockBody();
        Transaction transaction = new Transaction();
        transaction.setData("Test transaction Data");
        transaction.setAssetType("Simple Data");
        transaction.setFrom("from");
        transaction.setToAndValue("to", 0);
        transaction.setId();
        transaction.setHash(transaction.calculateDataHash());
        b.addTransaction(transaction);

        Block block = new Block(h, b );
        block.setConsensusType(StringHelper.getPoSConsensusFlag());
        block.setBlockNounce(PoW.safeFindBlockNounce(previousHash, b.safeGetTransactionHash()));

        Block blockNew = Block.createFromJson(block.getJson());
        System.out.println(blockNew.Dump());
        System.out.println(block.Dump());
        assert blockNew.Dump().equals(block.Dump());
        assert blockNew.isBlockListValid();

        Block nullBlock = Block.createFromJson(new JSONObject("{\"id\":\"test\"}"));
        assert nullBlock == null;
    }

    public void testCreateFromJsonWithDynamicalAsset() {
        System.out.println("[BlockTest][Info] Change mining difficult to 3");
        MiningConfiguration.testSetRequiredZeroCount(3);
        BlockHeader h = new BlockHeader();
        String previousHash = "00F2E456";
        h.setPreviousHash(previousHash);

        BlockBody b = new BlockBody();
        DynamicalAsset da = new DynamicalAsset();
        da.addKeyValue("book","Einstein");
        da.addKeyValue("day","5");
        da.setCode("code");
        b.addTransaction(da);

        Block block = new Block(h, b );
        block.setBlockNounce(PoW.safeFindBlockNounce(previousHash, b.safeGetTransactionHash()));

        Block blockNew = Block.createFromJson(block.getJson());
        System.out.println(blockNew.Dump());
        System.out.println(block.Dump());
        assert blockNew.Dump().equals(block.Dump());
        assert blockNew.isBlockListValid();
    }
}
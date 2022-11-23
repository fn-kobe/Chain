package com.scu.suhong.transaction;

import account.AccountManager;
import com.scu.suhong.block.Block;
import com.scu.suhong.block.BlockBody;
import com.scu.suhong.block.BlockChain;
import com.scu.suhong.block.BlockHeader;
import consensus.pow.MiningConfiguration;
import consensus.pow.PoW;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CrosschainTransactionTest {
    @Test
    public void doesCrosschainConditionalTransactionExist() {
        int blockchainId = 123;
        int interactionId = 10;
        String from = MiningConfiguration.getBlockchainId() + AccountManager.getAddressConnectSymbol() + "1_from";
        String to = MiningConfiguration.getBlockchainId() + AccountManager.getAddressConnectSymbol() + "2_to";
        int value = 10;
        CrosschainTransaction crosschainTx = createCrosschainiTransaction(blockchainId, interactionId, from, to, value);

        blockchainId = 234;
        interactionId = 10;
        from = "222_1_from";
        to = "222_2_to";
        value = 10;
        CrosschainTransaction requiredcrosschainTx = createCrosschainiTransaction(blockchainId, interactionId, from, to, value);

        crosschainTx.addRequiredCrosschainTransaction(requiredcrosschainTx);
        assert CrosschainTransaction.isTransactionSameInExternalCase(crosschainTx.getRequiredCrosschainTransaction(), requiredcrosschainTx);
        assert !CrosschainTransaction.isTransactionSameInExternalCase(requiredcrosschainTx, crosschainTx);

        assert !CrosschainTransaction.doesCrosschainTransactionExistBlockchain(crosschainTx);
        assert !CrosschainTransaction.doesCrosschainTransactionExistBlockchain(requiredcrosschainTx);

        BlockChain blockChain = BlockChain.getInstance();
        blockChain.testSetBlockChain(new ArrayList<>());
        Block block = Block.constructEmptyBlock();
        block.addTransaction(crosschainTx);
        blockChain.getBlockList().add(block);

        assert CrosschainTransaction.doesCrosschainTransactionExistBlockchain(crosschainTx);
        assert !CrosschainTransaction.doesCrosschainTransactionExistBlockchain(requiredcrosschainTx);

        block = Block.constructEmptyBlock();
        block.addTransaction(requiredcrosschainTx);
        blockChain.getBlockList().add(block);

        assert CrosschainTransaction.doesCrosschainTransactionExistBlockchain(crosschainTx);
        assert CrosschainTransaction.doesCrosschainTransactionExistBlockchain(requiredcrosschainTx);
    }

    @Test
    public void testInteractionMatch() {
        int blockchainId = 123;
        int interactionId = 10;
        String from = "111_1_from";
        String to = "111_2_to";
        int value = 10;
        CrosschainTransaction crosschainTxInBlockchain1 = createCrosschainiTransaction(blockchainId, interactionId, from, to, value);
        // From network, one crosschainTx only know one requiredcrosschainTx. It cannot have the whole map
        CrosschainTransaction crosschainTxInBlockchain1_json = createCrosschainiTransaction(blockchainId, interactionId, from, to, value);

        blockchainId = 234;
        interactionId = 10;
        from = "222_1_from";
        to = "222_2_to";
        value = 10;
        CrosschainTransaction crosschainTxInBlockchain2 = createCrosschainiTransaction(blockchainId, interactionId, from, to, value);
        CrosschainTransaction crosschainTxInBlockchain2_json = createCrosschainiTransaction(blockchainId, interactionId, from, to, value);

        blockchainId = 345;
        interactionId = 10;
        from = "333_1234_from";
        to = "333_1234_to";
        value = 10;
        CrosschainTransaction crosschainTxInBlockchain3 = createCrosschainiTransaction(blockchainId, interactionId, from, to, value);
        CrosschainTransaction crosschainTxInBlockchain3_json = createCrosschainiTransaction(blockchainId, interactionId, from, to, value);

        crosschainTxInBlockchain1.addRequiredCrosschainTransaction(crosschainTxInBlockchain2_json);
        crosschainTxInBlockchain2.addRequiredCrosschainTransaction(crosschainTxInBlockchain3_json);

        BlockChain blockChain = BlockChain.getInstance();
        Block block = Block.constructEmptyBlock();
        block.addTransaction(crosschainTxInBlockchain1);
        blockChain.getBlockList().add(block);

        List<CrosschainTransaction> crosschainTransactionList = new ArrayList<>();
        crosschainTransactionList.add(crosschainTxInBlockchain1);
        crosschainTransactionList.add(crosschainTxInBlockchain2);
        crosschainTransactionList.add(crosschainTxInBlockchain3);
        assert !crosschainTxInBlockchain1.doesMatched(crosschainTransactionList);
        assert !crosschainTxInBlockchain2.doesMatched(crosschainTransactionList);
        assert !crosschainTxInBlockchain3.doesMatched(crosschainTransactionList);

        crosschainTxInBlockchain3.addRequiredCrosschainTransaction(crosschainTxInBlockchain1_json);

        block = Block.constructEmptyBlock();
        block.addTransaction(crosschainTxInBlockchain3);
        blockChain.getBlockList().add(block);
        assert !crosschainTxInBlockchain1.doesMatched(crosschainTransactionList);
        assert !crosschainTxInBlockchain2.doesMatched(crosschainTransactionList);
        assert !crosschainTxInBlockchain3.doesMatched(crosschainTransactionList);

        block = Block.constructEmptyBlock();
        block.addTransaction(crosschainTxInBlockchain2);
        blockChain.getBlockList().add(block);
        assert crosschainTxInBlockchain1.doesMatched(crosschainTransactionList);
        assert crosschainTxInBlockchain2.doesMatched(crosschainTransactionList);
        assert crosschainTxInBlockchain3.doesMatched(crosschainTransactionList);
    }

    @NotNull
    private CrosschainTransaction createCrosschainiTransaction(int blockchainId, int interactionId,
                                                               String from, String to, int value) {
        CrosschainTransaction requiredcrosschainTx = new CrosschainTransaction(blockchainId, interactionId);
        requiredcrosschainTx.setFrom(from);
        requiredcrosschainTx.setToAndValue(to, value);
        return requiredcrosschainTx;
    }

    @Test
    public void dump() {
        int blockchainId = 123;
        int interactionId = 10;
        String crosschainTxKey = "reqTx";
        CrosschainTransaction crosschainTx = new CrosschainTransaction(blockchainId, interactionId);
        crosschainTx.addRequiredCrosschainTransaction(crosschainTx);

        String dumpString = crosschainTx.Dump();
        System.out.println(dumpString);
        assert dumpString.contains(crosschainTxKey);
        assert dumpString.contains(String.valueOf(blockchainId));
        assert dumpString.contains(String.valueOf(interactionId));

        CrosschainTransaction createdcrosschainTx = CrosschainTransaction.createFromJson(crosschainTx.getJson());
        assert createdcrosschainTx.getBlockchainId() == String.valueOf(blockchainId);
        assert createdcrosschainTx.getInteractionId() == interactionId;
        assert CrosschainTransaction.isTransactionSameInExternalCase(crosschainTx, createdcrosschainTx);
        // assert 0 == createdcrosschainTx.compareTo(crosschainTx); // commit as id and hash of the transaction is not different
    }

    @Test
    public void testCreateFromJson() {
        BlockHeader h = new BlockHeader();
        String previousHash = "00F2E456";
        h.setPreviousHash(previousHash);

        BlockBody b = new BlockBody();
        int blockchainId = 123;
        int interactionId = 10;
        String crosschainTxKey = "reqTx";
        CrosschainTransaction crosschainTx = new CrosschainTransaction(blockchainId, interactionId);
        crosschainTx.addRequiredCrosschainTransaction(crosschainTx);
        crosschainTx.setFrom("from");
        crosschainTx.setHash(crosschainTx.calculateDataHash());
        b.addTransaction(crosschainTx);

        Block block = new Block(h, b);
        block.setBlockNounce(PoW.safeFindBlockNounce(previousHash, b.safeGetTransactionHash()));

        Block blockNew = Block.createFromJson(block.getJson());
        System.out.println("Original block dump:\n" + block.Dump());
        System.out.println("New block dump:\n" + blockNew.Dump());
        assert blockNew.Dump().equals(block.Dump());
        assert blockNew.isBlockListValid();

        Block nullBlock = Block.createFromJson(new JSONObject("{\"id\":\"test\"}"));
        assert nullBlock == null;
    }
}
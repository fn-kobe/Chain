package com.scu.suhong.block;

import account.AccountManager;
import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.network.P2PConfiguration;
import com.scu.suhong.transaction.Transaction;
import consensus.pow.MiningConfiguration;
import org.json.JSONArray;
import org.json.JSONObject;
import util.FileLogger;
import util.StringHelper;
import util.TimeHelper;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Block implements Externalizable {
    static org.apache.log4j.Logger logger = FileLogger.getLogger();
    private BlockHeader h;
    private BlockBody b;

    public Block(BlockHeader h, BlockBody b) {
        this.h = h;
        this.b = b;
    }

    static public Block createFromJson(JSONObject object) {
        if (!object.has("h") || !object.has("b")) { // must have header or body
            return null;
        }
        JSONObject ho = (JSONObject) object.get("h");
        BlockHeader h = BlockHeader.createFromJson(ho);

        JSONObject bo = (JSONObject) object.get("b");
        BlockBody b = BlockBody.createFromJson(bo);
        if (null == h || null == b){
            return null;
        }

        return new Block(h, b);
    }

    static public ArrayList<Block> createFromJson(JSONArray objectArray) {
        ArrayList<Block> blocks = new ArrayList<>();
        Block b;
        for (int i = 0; i < objectArray.length(); ++i) {
            b = createFromJson((JSONObject) objectArray.get(i));
            if (b == null) {
                logger.error("[Block] error in object array");
                return null;
            }
            blocks.add(b);
        }
        return blocks;
    }

    static public boolean isBlockListValid(List<Block> topBlocks, boolean isExternal) {
        if (topBlocks.isEmpty()) {
            logger.warn("[Block] The block array is not valid due to length is empty");
            return false;
        }
        Block currentBlock = topBlocks.get(0);
        Block nextBlock;
        for (int i = 1; i < topBlocks.size(); ++i) { // skip 0 as we did it in the above sentence
            nextBlock = topBlocks.get(i);
            if (!currentBlock.isBlockListValid(isExternal)) {
                logger.error(String.format("[Block][MisMatch] Block %d is not valid (c)", currentBlock.getBlockIndex()));
                return false;
            }
            if (!nextBlock.isBlockListValid(isExternal)) {
                logger.error(String.format("[Block][MisMatch] Block %d is not valid (n)", currentBlock.getBlockIndex()));
                return false;
            }
            if (!currentBlock.safeGetBlockHash().equals(nextBlock.getPreviousHash())) {
                logger.error(String.format("[Block][MisMatch] currentBlock has: %s, next hash: %s", currentBlock.safeGetBlockHash(), nextBlock.safeGetBlockHash()));
                return false;
            }
            if ((currentBlock.getBlockIndex() + 1) != nextBlock.getBlockIndex()) {
                logger.error(String.format("[Block][MisMatch] currentBlock index: %d, next index: %d", currentBlock.getBlockIndex(), nextBlock.getBlockIndex()));
                return false;
            }
            currentBlock = nextBlock;
        }
        return true;
    }

    static public Block constructEmptyBlock() {
        BlockHeader h = new BlockHeader(); // no previous block hash
        Transaction transaction = new Transaction();
        transaction.setData("");
        transaction.setId();
        transaction.setHash();
        BlockBody b = new BlockBody(); // no transaction
        return new Block(h, b);
    }

    static public Block constructBlock(String previousHash, int blockNounce, int blockIndex) {
        BlockHeader h = new BlockHeader(previousHash, blockNounce, blockIndex); // no previous block hash
        BlockBody b = new BlockBody(); // no transaction
        return new Block(h, b);
    }

    static public Block constructBlock(String previousHash, int blockNounce, int blockIndex, Transaction t) {
        BlockHeader h = new BlockHeader(previousHash, blockNounce, blockIndex); // no previous block hash
        BlockBody b = new BlockBody(); // no transaction
        if (null != t) b.addTransaction(t);
        return new Block(h, b);
    }

    static public String getBlockListJson(List<Block> blocks) {
        String blocksJson = "{\"blocks\" :[";
        Block b = null;
        for (int i = 0; i < blocks.size(); ++i) {
            b = blocks.get(i);

            if (0 != i) blocksJson += ",";
            blocksJson += b.getJson();
        }
        blocksJson += "]}";
        return blocksJson;
    }

    public BlockHeader getHeader() {
        return h;
    }

    public BlockBody getBody() {
        return b;
    }

    // the block hash is determined by previous hash, transaction hash and the nounce
    // this function is mainly design to isMatchBlockchain the block validity
    // NOTE: not use to find the nounce
    public String getBlockHash() throws BlockException {
        try {
            return MD5Hash.getValue(h.previousHash, b.getTransactionHash(), h.blockNounce);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new BlockException();
        }
    }

    public String safeGetBlockHash() {
        String r = "";
        try {
            r = getBlockHash();
        } catch (BlockException e) {
            e.printStackTrace();
            return "";
        }
        return r;
    }

    public boolean isBlockListValid() {
        return isBlockListValid( false);
    }

    public boolean isBlockListValid(boolean isExternal) {
        if (0 == getBlockIndex()){
            System.out.println("[Block][INFO] Assume block 0 is always correct");
            return true;
        }

        if (!isHashMathed(getChainId(), isExternal)) return false;

        if (!h.isValid()) {
            System.out.println("[Block][ERROR]Block header is invalid: " + getJson().toString());
            return false;
        }
        if (!b.isValid()) {
            System.out.println("[Block][ERROR]Block body is invalid: " + getJson().toString());
            return false;
        }
        return true;
    }

    public boolean isHashMathed(String chainId, boolean isExternal) {
        try {
            if (isExternal) {
                if (StringHelper.isPoWConsensusFlag(getConsensusType())) {
                    if (!P2PConfiguration.getInstance().isHashMatched(chainId, getBlockHash())) {
                        System.out.println("[Block][ERROR] Required hash zero count(external) is not matched: " + getBlockHash() + " of " + getJson().toString());
                        return false;
                    }
                }
                if (StringHelper.isPoSConsensusFlag(getConsensusType())){
                    // TO DO, check the signature
                    return true;
                }
            }

            if (StringHelper.isPoWConsensusFlag(getConsensusType())) {
                if (!MiningConfiguration.isHashMatched(getBlockHash())) {
                    System.out.println("[Block][ERROR] Required hash zero count is not matched: " + getBlockHash() + " of " + getJson().toString());
                    return false;
                }
                return true;
            }

            if (StringHelper.isPoSConsensusFlag(getConsensusType())){
                // TO DO, check the signature
                return true;
            }
        } catch (BlockException e) {
            return false;
        }
        return true;
    }

    public List<AbstractTransaction> getTransactions() {
        return b.getTransactions();
    }

    public JSONObject getJson() {
        JSONObject object = new JSONObject();
        object.put("h", h.getJson());
        object.put("hash", safeGetBlockHash());
        object.put("b", b.getJson());
        return object;
    }

    public boolean isEqual(Block another) {
        if (!h.isEqual(another.getHeader())) return false;
        if (!b.isEqual(another.getBody())) return false;
        return true;
    }

    public String Dump() {
        String dump = "<block>\n";
        dump += h.Dump();
        dump += "Block hash: " + safeGetBlockHash() + "\n";
        dump += b.Dump();
        dump += "</block>\n";
        return dump;
    }

    // used to find the nounce
    public String getTransactionListRootHash() throws BlockException {
        return b.getTransactionListRootHash();
    }

    public boolean isNounceMatched() throws BlockException {
        return MiningConfiguration.isHashMatched(getBlockHash());
    }

    public String getPreviousHash() {
        return h.getPreviousHash();
    }

    public void setPreviousHash(String previousHash) {
        h.setPreviousHash(previousHash);
    }

    public int getBlockNounce() {
        return h.getBlockNounce();
    }

    public void setBlockNounce(int blockNounce) {
        h.setBlockNounce(blockNounce);
    }

    public int getPreviousBlockNumber() {
        return h.getPreviousBlockNumber();
    }

    public int getBlockIndex() {
        return h.getBlockIndex();
    }

    public void setBlockIndexAndMiningTime(int blockIndex) {
        h.setBlockIndex(blockIndex);
        int txIndex = 1;
        for (AbstractTransaction t : getTransactions()){
            t.setBlockIndex(h.getBlockIndex());
            t.setTxIndex(++txIndex);
            t.setMiningTime(TimeHelper.getEpoch());
        }
    }

    public void addTransaction(AbstractTransaction t) {
        t.setBlockIndex(h.getBlockIndex());
        t.setTxIndex(b.getTransactions().size() + 1);
        t.setMiningTime(TimeHelper.getEpoch());
        b.addTransaction(t);
    }

    @Override
    public void writeExternal(ObjectOutput objectOutput) throws IOException {

    }

    @Override
    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {

    }

    public String getTransactionHash() throws BlockException {
        return b.getTransactionHash();
    }

    public int getTransactionNumber() {
        return getTransactions().size();
    }

    public String getMiner() {
        return h.getMiner();
    }

    public String getChainId(){
        return AccountManager.getBlockchainIDFromAddress(getMiner());
    }

    public void setMiner(String miner) {
        h.setMiner(miner);
    }

    public boolean hasTransaction(){
        return !getTransactions().isEmpty();
    }

    public void setNodeId(String nodeId) {
        h.setNodeId(nodeId);
    }

    public void setConsensusType(String consensusType) {
        h.setConsensusType(consensusType);
    }

    public String getConsensusType() {
        return h.getConsensusType();
    }
}

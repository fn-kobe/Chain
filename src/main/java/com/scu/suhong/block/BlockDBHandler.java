package com.scu.suhong.block;

import account.AccountManager;
import org.apache.log4j.Logger;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteOptions;
import org.json.JSONObject;
import util.FileLogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.fusesource.leveldbjni.JniDBFactory.*;

public class BlockDBHandler {
    private static BlockDBHandler blockDBHandler = null;
    private static HashMap<String, BlockDBHandler> externalBlockDBHandlerMap = new HashMap<>();
    Logger logger = FileLogger.getLogger();
    private String blockDBName;
    private String topBlockHashDBName;
    private String transactionDBName;
    private String topBlockHashKey;
    private String handlerName;
    private DB blockDB;
    private DB topBlockHashDB;
    private DB transactionDB;
    private WriteOptions writeOptions;

    private BlockDBHandler(boolean isExternal) {
        // The external DB name begins with "ex_"
        if (!isExternal) {
            blockDBName = "blocks";
            topBlockHashDBName = "topBlockHash";
            transactionDBName = "transaction";
            topBlockHashKey = "topBlockHashKey";
            handlerName = "internal";
        } else {
            blockDBName = "ex_blocks";
            topBlockHashDBName = "ex_topBlockHash";
            transactionDBName = "ex_transaction";
            topBlockHashKey = "ex_topBlockHashKey";
            handlerName = "external";
        }

        writeOptions = new WriteOptions();
        writeOptions.sync(true);
    }

    public static synchronized BlockDBHandler getInstance() {
        if (null != blockDBHandler) {
            return blockDBHandler;
        }
        blockDBHandler = new BlockDBHandler(false);
        if (!blockDBHandler.safeOpenDB()) {
            blockDBHandler = null;
        }

        return blockDBHandler;
    }

    public static synchronized BlockDBHandler getExternalInstance(String chainId) {
        if (externalBlockDBHandlerMap.containsKey(chainId)) {
            return externalBlockDBHandlerMap.get(chainId);
        }
        BlockDBHandler externalBlockDBHandler = new BlockDBHandler(true);
        if (!externalBlockDBHandler.safeOpenDB(chainId)) {
            externalBlockDBHandler = null;
        } else {
            externalBlockDBHandlerMap.put(chainId, externalBlockDBHandler);
        }

        return externalBlockDBHandler;
    }

    private boolean safeOpenDB() {
        return safeOpenDB("");
    }

    private boolean safeOpenDB(String chainId) {
        Options options = new Options();
        options.createIfMissing(true);
        try {
            blockDB = factory.open(new File(blockDBName + chainId), options);
            topBlockHashDB = factory.open(new File(topBlockHashDBName + chainId), options);
            transactionDB = factory.open(new File(transactionDBName + chainId), options);
        } catch (IOException e) {
            logger.info("[BlockDBHandler][" + handlerName +"][Error] exception happen when open DB ");
            e.printStackTrace();
            safeCloseDB();
            return false;
        }
        return true;
    }

    public void dumpBlockDB() {
        logger.info("[BlockDBHandler][" + handlerName +"] Begin to dump the block DB ");
        DBIterator iterator = blockDB.iterator();
        for (iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
            String key = asString(iterator.peekNext().getKey());
            String value = asString(iterator.peekNext().getValue());
            logger.info(key + " = " + value);
        }
    }

    public boolean saveTopBlock(Block block) throws BlockException {
        return saveTopBlock(block, false);
    }

    public boolean saveTopBlock(Block block, boolean isExternal) throws BlockException {
        if (saveBlock(block, isExternal)) {
            topBlockHashDB.put(bytes(topBlockHashKey), bytes(block.getBlockHash()), writeOptions);
            return true;
        }
        System.out.println("[BlockDBHandler][" + handlerName +"][ERROR] Failed to save top block");
        return false;
    }

    // Not used separate, then no DB open and close. DB should be opened before
    boolean save(Block block) {
        return save(block, false);
    }

    boolean save(Block block, boolean isExternal) {
        try {
            logger.info("[BlockDBHandler][" + handlerName +"] Begin to save the block " + block.getBlockHash());
            return saveBlock(block, isExternal);
        } catch (BlockException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean saveBlock(Block block, boolean isExternal) throws BlockException {
        if (0 != block.getBlockIndex() && !block.isBlockListValid(isExternal)) {
            logger.info("[BlockDBHandler][" + handlerName +"][ERROR] Skip to save as block is invalid ");
            return false;
        }

        blockDB.put(bytes(block.getBlockHash()), bytes(block.getJson().toString()), writeOptions);
        //BlockFileDataHandler.saveBlock(block);// Only for test, save to file for size calculation

        // this is for paper test, remove if not test the size test
        // Try to save the transaction Data according yo MultiTypeAsset type
/*        for (Transaction t : block.getBody().transactions){
            BlockFileDataHandler.saveTransaction(t, block.getBlockHash());
        }*/
        return true;
    }

    public void save(List<Block> blockList) throws BlockException {
        save(blockList, false);
    }
    public void save(List<Block> blockList, boolean isExternal) throws BlockException {
        logger.info("[BlockDBHandler][" + handlerName +"] Begin to save the block chain");
        if (blockList.isEmpty()) {
            logger.info("[BlockDBHandler][" + handlerName +"] Empty blockchain, skip saving");
            return;
        }
        for (Block block : blockList
        ) {
            save(block, isExternal);
        }
        Block topBlock = blockList.get(blockList.size() - 1);
        topBlockHashDB.put(bytes(topBlockHashKey), bytes(topBlock.getBlockHash()), writeOptions);
    }

    public String getTopBlockHashDB() {
        byte[] topBlockHashValue = topBlockHashDB.get(topBlockHashKey.getBytes());
        if (null == topBlockHashValue || 0 == topBlockHashValue.length) {
            return "";
        }
        return asString(topBlockHashValue);
    }

    // Should open DB before
    public Block loadBlock(String blockHash) {
        logger.info("[BlockDBHandler][" + handlerName +"] Begin to load the block " + blockHash);
        byte[] blockByte = blockDB.get(bytes(blockHash));
        if (null == blockByte || 0 == blockByte.length) {
            return null;
        }
        logger.info("[BlockDBHandler][" + handlerName +"] Begin to load the block " + new String(blockByte));
        String blockJson = new String(blockByte);
        return Block.createFromJson(new JSONObject(blockJson));
    }

    public List<Block> loadChainList() {
        logger.info("[BlockDBHandler][" + handlerName +"] Begin to load the block for " + blockDBName);
        ArrayList<Block> tempBlockList = new ArrayList<>();
        byte[] topBlockHash = topBlockHashDB.get(bytes(topBlockHashKey));
        byte[] currentBlockHash = topBlockHash;
        Block block;
        while (null != currentBlockHash && 0 != currentBlockHash.length) {
            if (null == (block = loadBlock(new String(currentBlockHash)))) {
                break;
            }
            tempBlockList.add(block);
            currentBlockHash = bytes(block.getPreviousHash());
            AccountManager.getInstance().addValue(block.getMiner(), Double.valueOf(AccountManager.getMiningReward()));
        }

        Collections.reverse(tempBlockList); // To make sure the first block is geneses block
        logger.info("[BlockDBHandler][" + handlerName +"][DEBUG] Load blockchain from DB with size: " + tempBlockList.size());
        return tempBlockList;
    }

    public void clearAllData() {
        Options options = new Options();
        try {
            factory.destroy(new File(blockDBName), options);
            factory.destroy(new File(topBlockHashDBName), options);
            factory.destroy(new File(transactionDBName), options);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void safeCloseDB() {
        try {
            if (null != blockDB) blockDB.close();
            if (null != topBlockHashDB) topBlockHashDB.close();
            if (null != transactionDB) transactionDB.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void shutDown() {
        safeCloseDB();
    }
}

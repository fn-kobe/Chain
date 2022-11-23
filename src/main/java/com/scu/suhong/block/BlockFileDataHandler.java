package com.scu.suhong.block;

import com.scu.suhong.transaction.Transaction;
import org.apache.log4j.Logger;
import util.FileLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class BlockFileDataHandler {
    static Logger logger = FileLogger.getLogger();

    static String blockFileDataFolder = "blockfiles";

    static public boolean createFolderIfNotExist(String folderName) {
        File dir = new File(folderName);
        if (dir.exists()) {
            return true;
        }
        return dir.mkdir();
    }

    static public boolean deleteFolder(String folderName) {
        File dir = new File(folderName);
        return dir.delete();
    }

    static public void saveBlock(Block block) {
        createFolderIfNotExist(blockFileDataFolder);
        FileOutputStream file;
        String fileName = "." + File.separator + blockFileDataFolder + File.separator + block.safeGetBlockHash();
        try {
            file = new FileOutputStream(fileName);
        } catch (Exception e) {
            e.printStackTrace();
           logger.info("[BlockFileDataHandler][ERROR] failed to save block" + block.getJson().toString());
            return;
        }
        try {
            file.write(block.getJson().toString().getBytes());
           logger.info("[BlockFileDataHandler] Succeed to save block" + fileName);
        } catch (IOException e) {
            e.printStackTrace();
           logger.info("[BlockFileDataHandler][ERROR] Failed to save block" + block.getJson().toString());
        } finally {
            safeCloseFile(file);
        }
    }

    static public void saveBlock(List<Block> blockList) {
        for (Block b : blockList
                ) {
            saveBlock(b);
        }
    }

    public static void saveTransaction(Transaction t, String blockHash) {
        String assetFolderName = t.getAssetType();
        createFolderIfNotExist(assetFolderName);
        FileOutputStream file;
        try {
            file = new FileOutputStream("." + File.separator + assetFolderName + File.separator + t.getHash() + "_" + blockHash);
        } catch (Exception e) {
            e.printStackTrace();
           logger.info("[BlockFileDataHandler][ERROR] failed to save transaction" + t.getJson().toString());
            return;
        }
        try {
            file.write(t.getJson().toString().getBytes());
           logger.info("[BlockFileDataHandler] Succeed to save transaction" + t.getJson().toString());
        } catch (IOException e) {
            e.printStackTrace();
           logger.info("[BlockFileDataHandler][ERROR] failed to save transaction" + t.getJson().toString());
        } finally {
            safeCloseFile(file);
        }
    }

    private static void safeCloseFile(FileOutputStream file) {
        try {
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

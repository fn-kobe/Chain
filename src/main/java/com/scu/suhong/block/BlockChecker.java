package com.scu.suhong.block;

import org.apache.log4j.Logger;
import util.FileLogger;

public class BlockChecker {
    static Logger logger = FileLogger.getLogger();
    static public Boolean isMatchBlockchain(BlockChain currentBlockChain, Block pendingBlock, boolean isExternal) throws BlockException {
        logger.info("[BlockChecker] Begin to check whether one block is valid or not");
        String topBlockHash = currentBlockChain.getTopBlockHash();
        // 1. the current pendingBlock must point to the current blockchain top element
        if (!topBlockHash.isEmpty()){
           logger.info(String.format("[BlockChecker] Compare block chain top block hash:%s with previous hash in pending block hash: %s.\n",
                    topBlockHash,
                    pendingBlock.getPreviousHash()));
            //TO DO iterator for fork condition
            if (!topBlockHash.equals(pendingBlock.getPreviousHash())){
               logger.info(String.format("[BlockChecker][WARN] Block chain top block hash:%s is not the same as previous hash in pending block hash: %s\n",
                        topBlockHash,
                        pendingBlock.getPreviousHash()));
                return false;
            }
        }
        // 2. the block should be valid
        return pendingBlock.isBlockListValid(isExternal);
    }

}

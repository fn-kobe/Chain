package com.scu.suhong.block;

import account.AccountManager;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.dynamic_definition.DynamicalAsset;
import com.scu.suhong.dynamic_definition.DynamicalAssetProcessor;
import com.scu.suhong.instantiationOptimization.InstantiationTransactionProcessor;
import com.scu.suhong.miner.Miner;
import com.scu.suhong.smartcontract.P2P.P2PHandler;
import com.scu.suhong.smartcontract.lifecycleFlexibility.LifecycleHandler;
import com.scu.suhong.transaction.*;
import com.scu.suhong.transaction.ActionCondition.ACPActionTriggerTransactionHandler;
import com.scu.suhong.transaction.multipleTypeExchange.CrosschainExchangeCommonProcessor;
import com.scu.suhong.transaction.multipleTypeExchange.MultiTypeExchangeProcessor;
import com.scu.suhong.transaction.multipleTypeExchange.NotaryExchangeProcessor;
import consensus.pow.MiningConfiguration;
import hashlocking.HashLockingProcessor;
import util.ArrayHelper;
import util.FileLogger;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BlockChain {
    static final String genesisMiningAccount = "0xabcd";
    static org.apache.log4j.Logger logger = FileLogger.getLogger();
    static BlockChain blockChainInstance;
    static ExternalBlockchainManager externalBlockchainManager;
    static Lock chainInstanceLock = new ReentrantLock();
    static boolean isInInitialState = false;
    // The geneses block is block[0] and the first one is the latest block.
    List<Block> blockList;
    Miner miner;
    String identify = "internal";
    BlockListBalanceProcessor blockListBalanceProcessor;
    BlockListComparison comparison;
    // Just used to dump. If other function, please reform when chain goes to rebranch
    int transactionTotalNumber = 0;
    boolean isNewTransactionAdded = false;
    int transactionRemoveWaitBlockMingPeriod = 15;
    Map<String, Object> classInstanceList;
    String chainId = "";// used mainly for external producer blockchain

    BlockChain() {
        blockListBalanceProcessor = new BlockListBalanceProcessor();
        this.blockList = new CopyOnWriteArrayList<Block>();
        comparison = new BlockListComparison();
        classInstanceList = new HashMap<>();
    }

    //static Logger logger = LogManager.getLogger(BlockChain.class.getName());

    BlockChain(List<Block> blockList, boolean initGenesisBlockIfNeed) {
        init(blockList, initGenesisBlockIfNeed);
    }

    static public synchronized BlockChain getInstance() {
        chainInstanceLock.lock();
        if (null != blockChainInstance) {
            chainInstanceLock.unlock();
            return blockChainInstance;
        }

        blockChainInstance = new BlockChain();
        blockChainInstance.init(BlockDBHandler.getInstance().loadChainList());

        chainInstanceLock.unlock();
        return blockChainInstance;
    }

    static public synchronized ExternalBlockchainManager getExternalManager() {
        chainInstanceLock.lock();
        if (null != externalBlockchainManager) {
            chainInstanceLock.unlock();
            return externalBlockchainManager;
        }

        // External chain also generation the genesis block and it will be force update later
        externalBlockchainManager = new ExternalBlockchainManager();
        chainInstanceLock.unlock();
        return externalBlockchainManager;
    }

    // Only for test
    static public void reset() {
        externalBlockchainManager = null;
        blockChainInstance = null;
    }

    // TO DO create from file
    static public Block constructGenesisBlock() {
        Block genesisBlock = Block.constructEmptyBlock();
        genesisBlock.setBlockIndexAndMiningTime(0);
        genesisBlock.setMiner(genesisMiningAccount);
        // // 00000004AC9FB91A7C74CDE1D7845192   -1108112160      000000023D07F6C2A1BD78DDD456E019 1289986143
        genesisBlock.setBlockNounce(MiningConfiguration.getGenesisNouce()); // 0000000C91B7D0459D6B0E0EA163B149 - 7 piece of 0
        return genesisBlock;
    }

    public List<Block> getBlockList() {
        return blockList;
    }

    public List<Block> getCopiedBlockList() {
        return ArrayHelper.copy(blockList);
    }

    public void setMiner(Miner miner) {
        this.miner = miner;
    }

    // Only for test
    public void testRest() {
        blockList = new ArrayList<>();
        blockList.add(constructGenesisBlock());
        miner = null;
    }

    // Only for test
    public void testRestBlockList() {
        blockList = new ArrayList<>();
        blockList.add(constructGenesisBlock());
    }

    public boolean isReady() {
        if (!BlockchainState.hasPeers()) { // will init immediately
            return true;
        }
        return blockList.size() > 0;// return true if block is received from other node
    }

    public void init(List<Block> blockList) {
        blockListBalanceProcessor = new BlockListBalanceProcessor();
        init(blockList, true);
    }

    public void init(List<Block> blockList, boolean initGenesisBlockIfNeed) {
        isInInitialState = true;
        this.blockList = new CopyOnWriteArrayList<Block>();
        if (null != blockList && !blockList.isEmpty()) {
            for (Block block : blockList
            ) {
                try {
                    this.addBlock(block);
                } catch (BlockException e) {
                    e.printStackTrace();
                    break;
                }
            }
        } else if (initGenesisBlockIfNeed) {
            //create geneses block
            try {
                this.saveNewBlock(constructGenesisBlock());
            } catch (BlockException e) {
                logger.info(e);
                blockChainInstance = null;
            }
        }
        testAndSetNewTransactionAdded(true);// we dump the blockchain list after initialized
        isInInitialState = false;
    }

    static public boolean isInInitialState(){
        return isInInitialState;
    }

    public String getTopBlockHash() throws BlockException {
        String r = "";
        if (!blockList.isEmpty()) {
            r = blockList.get(blockList.size() - 1).getBlockHash();
        }
        return r;
    }

    public Block getLatestBlock() {
        Block r = null;
        if (!blockList.isEmpty()) {
            r = blockList.get(blockList.size() - 1);
        }
        return r;
    }

    // Original blockchain 0->1-> ...-> latest
    // Return  the same order l-n+1->l-n+2-> ...-> latest(l)
    public List<Block> doGetLatestBlock(int n) {
        List latestBlocks = new ArrayList();
        Block r = null;
        int currentBlockListSize = blockList.size();
        // make aure start >= 0
        int start = (n > currentBlockListSize) ? 0 : currentBlockListSize - n;
        int end = currentBlockListSize;
        if (!blockList.isEmpty()) {
            for (int i = start; i < end; ++i)
                latestBlocks.add(blockList.get(i));
        }
        return latestBlocks;
    }

    // Orginal blockchain 0->1-> ...-> latest
    // Return  the same order s-n+1->s-n+2-> ...-> s
    public List<Block> getLatestBlock(int n, int startIndex) {
        if (-1 == startIndex) return doGetLatestBlock(n);
        else return doGetLatestBlock(n, startIndex);
    }

    // startIndex-n+1 -> startIndex-n+2 -> ... -> startIndex
    public List<Block> doGetLatestBlock(int n, int endIndex) {
        List latestBlocks = new ArrayList();
        Block r = null;

        if (endIndex > blockList.size() - 1) {
            System.out.println("[Blockchain][" + identify + "][WARN] Start parameter error by peer block query " + endIndex + " > " + (blockList.size() - 1));
            return latestBlocks;
        }

        //make sure startIndex-n+1 >= 0
        if (n > endIndex + 1) n = endIndex + 1;

        int start = endIndex - n + 1;

        if (!blockList.isEmpty()) {
            for (int i = start; i < endIndex + 1; ++i) // start is included
                latestBlocks.add(blockList.get(i));
        }
        return latestBlocks;
    }

    public Block getBlock(int blockNumber) {
        return blockList.get(blockNumber);
    }

    public boolean saveNewBlock(Block block) throws BlockException {
        if (!addBlock(block)) {
            System.out.printf("[Blockchain][" + identify + "][WARN] failed to save block to blockchain in memory with %d transactions\n", block.getTransactionNumber());
            return false;
        }
        if (!block.getTransactions().isEmpty()) testAndSetNewTransactionAdded(true);
        return saveBlockToDB(block);
    }

    public boolean saveBlockToDB(Block block) throws BlockException {
        // save to local DB
        if (BlockDBHandler.getInstance().saveTopBlock(block)) {
            logger.info(String.format("[Blockchain][" + identify + "][INFO] Succeed to save block: %d to DB", block.getBlockIndex()));
            return true;
        } else {
            logger.info("[Blockchain][saveNewBlock][" + identify + "][ERROR] Failed to save block to DB");
            return false;
        }
    }

    // ExternalBlockchain.addBlock(ExTxs) ->trySealCrossChainTransactionIntoInternalBlockchain
    // -> Miner.addTransaction->Blockchain(internal).addBlock(exTx_in_internal_blockchain)-> CrosschainTransactionHandler
    void tryAddActionConditionPairToHandler(Block block) {
        ACPActionTriggerTransactionHandler.getInstance().tryAddNewBlock(block);
    }

    void tryAddCrossChainTransactionToHandler(Block block) {
        CrosschainTransactionHandler.getInstance().tryAddNewBlock(block);
    }

    void tryAddNotaryTransactionToHandler(Block block) {
        NotaryExchangeProcessor.getInstance().tryAddNewBlock(block);
    }

    void tryAddInstantiationTransactionToHandler(Block block) {
        InstantiationTransactionProcessor.getInstance().tryAddNewBlock(block);
    }

    void tryAddCrosschainCommonProcessor(Block block) {
        CrosschainExchangeCommonProcessor.getInstance().tryAddNewBlock(block);
    }

	void tryAddSmartContractLifeCycleProcessor(Block block) {
		LifecycleHandler.getInstance().tryAddNewBlock(block);
	}

	void tryAddP2PSmartContractProcessor(Block block) {
		P2PHandler.getInstance().tryAddNewBlock(block);
	}

	void tryTransactionToHashLockingHandler(Block block) {
		HashLockingProcessor processor = HashLockingProcessor.getInstance();
		if (null == processor) {
			System.out.println("[Blockchain][WARN] Hash locking processor is not ready. SKip send to it to process");
			return;
		}
		HashLockingProcessor.getInstance().tryAddNewBlock(block);
	}

    void tryAddMultiTypeExchangeTransactionToHandler(Block block){
        MultiTypeExchangeProcessor.getInstance().tryAddNewBlock(block);
    }

    public boolean hasDuplicatedTransaction(Block newBlock) {
        // we only check recent 8 blocks' ID
        for (Block inChainBlock : blockList) {
            if (isBlocksContainSameTransaction(inChainBlock, newBlock)) return true;
        }
        return false;
    }

    boolean isBlocksContainSameTransaction(Block inChainBlock, Block newBlock) {
        // Only check 8 blocks
        int count = 0;
        for (AbstractTransaction inChainTransaction : inChainBlock.getTransactions()) {
            for (AbstractTransaction newTransaction : newBlock.getTransactions()) {
                if (!inChainTransaction.getBlockchainId().equals(newTransaction.getBlockchainId())) continue;

                if (inChainTransaction.getId() == newTransaction.getId()) {
                    logger.error(String.format("[BlockChain][" + identify + "] The transaction id %d in the new block is the same as one in main chain", inChainTransaction.getId()));
                    return true;
                }
                // internal tx
                if (inChainTransaction.isInternalTx() && newTransaction.isInternalTx() && inChainTransaction.getData().equals(newTransaction.getData())) {
                    logger.error(String.format("[BlockChain][" + identify + "] The internal transaction %s in the new block is the same as one in main chain", inChainTransaction.getData()));
                    return true;
                }
            }
            ++count;
            if (count >= 8) break;
        }
        return false;
    }

    public boolean updateBlockListWithLongerChain(List<Block> anotherBlockList) throws BlockException {
        if (anotherBlockList.size() < 1) {
            System.out.println("[Blockchain][" + identify + "][WARN] Peer blockchain is empty. Skip to add");
            return true;
        }

        if (anotherBlockList.get(anotherBlockList.size() - 1).getBlockIndex() <= blockList.get(blockList.size() - 1).getBlockIndex()) {
            System.out.println("[Blockchain][" + identify + "][WARN] The top block of to be added array is not longer than the current, skip");
            return true;
        }

        Block anotherLowestBlock = anotherBlockList.get(0);
        int lowestBlockNumber = anotherLowestBlock.getBlockIndex();

        List<Block> newBlockList = new CopyOnWriteArrayList<Block>();
        Block block = null;
        for (int i = 0; i < lowestBlockNumber; ++i) { // copy the block before difference to new block list
            block = blockList.get(i);
            newBlockList.add(block);
            if (!block.getTransactions().isEmpty()) testAndSetNewTransactionAdded(true);
            // Aleardy saved as in blocklist. Here we only copy the data to new block list
        }
        for (int i = 0; i < anotherBlockList.size(); ++i) { // copy the difference from another
            block = anotherBlockList.get(i);
            newBlockList.add(block);
            if (!block.getTransactions().isEmpty()) testAndSetNewTransactionAdded(true);
            saveBlockToDB(block);
        }

        handleDisappearedTransaction(newBlockList);

        blockList = newBlockList;
        handleBlockListChange();
        System.out.println("[Blockchain][" + identify + "][Info] Succeed to update blockchain from peers with longer block list");
        return true;
    }

    // will be override in external
    public void handleDisappearedTransaction(List<Block> newBlockList) {
        // For internal blockchain handle missing crosschain transactions. As crosschain transaction should not be missing
        comparison.compare(blockList, newBlockList);
        for (AbstractTransaction transaction : comparison.getDisappearedTxList()) {
            if (transaction instanceof CrosschainTransaction) {
                System.out.println("[Blockchain][" + identify + "][Info] Crosschain transaction has been rebranched. Try to readded it to miner");
                miner.addTransaction(transaction);
            }
        }
    }

    // will be override in external
    public void handleBlockListChange() throws BlockException {
        blockListBalanceProcessor.process(ArrayHelper.copy(blockList));
        DynamicalAssetProcessor.processAddFromLongerChain(ArrayHelper.copy(blockList));
        processInstantiation(ArrayHelper.copy(blockList));
    }

    // This is the simplest process as time limited
    void processInstantiation(List<Block> blockList){
        InstantiationTransactionProcessor processor = InstantiationTransactionProcessor.getInstance();
        for (Block b: blockList) {
            processor.tryAddNewBlock(b);
        }
    }

    // @return old instance
    public Object setGlobalAssetInstance(String className, Object objectInstance){
        if (!classInstanceList.containsKey(className)){
            classInstanceList.put(className, objectInstance);
            return null;
        } else {
            return classInstanceList.replace(className, objectInstance);
        }
    }

    public Object getGlobalAssetInstance(String className){
        if (classInstanceList.containsKey(className)){
            return classInstanceList.get(className);
        } else{
            DynamicalAsset dynamicalAsset = DynamicalAssetProcessor.getInstance(className);
            if (null != dynamicalAsset) {
                classInstanceList.put(className, dynamicalAsset);
            } else {
                System.out.printf("[Blockchain][" + identify + "][ERROR] can not init instance of %s to find", className);
            }
            return dynamicalAsset;
        }
    }

    public boolean addBlock(Block block) throws BlockException {
        if (!BlockChecker.isMatchBlockchain(this, block, false)) {
            logger.info("[Blockchain][" + identify + "][WARN] Invalid block, skip to add to main chain");
            return false;
        }
        //Currently, we don't use divide. If want to do this, please open it.
        //ConditionalDivisionTransactionHelper.getInstance().tryAddNewBlock(block);
        blockList.add(block);
        transactionTotalNumber += block.getTransactionNumber();

        // After being added into the blockchain. Notify the listener as they think the block has been added into the blockchain
        // Internal one blockchain  will process the balance; or we will not
        AccountManager.getInstance().processBlock(block);
        ConditionalAssociationTransactionHelper.getInstance().tryAddNewBlock(block);
        DynamicalAssetProcessor.processBlockAdd(block);

        tryAddCrossChainTransactionToHandler(block);
        tryAddActionConditionPairToHandler(block);
        tryAddMultiTypeExchangeTransactionToHandler(block);
        tryTransactionToHashLockingHandler(block);
        tryAddNotaryTransactionToHandler(block);
        tryAddInstantiationTransactionToHandler(block);
        tryAddCrosschainCommonProcessor(block);
        tryAddSmartContractLifeCycleProcessor(block);
        tryAddP2PSmartContractProcessor(block);
        logger.info("[Blockchain][" + identify + "] Succeed to add one new block to main chain");
        if (block.hasTransaction()) isNewTransactionAdded = true;
        return true;
    }

    // used for miner to remove the transactions already be selaed into main chain to avoid the dulpicatedly seal one transaction
    public List<AbstractTransaction> removeTransactionExistInBlockchain(List<AbstractTransaction> transactionList, int fromTopNumber) {
        List<Block> blockList = getCopiedBlockList();
        int blockHeight = blockList.size();
        Iterator<AbstractTransaction> transactionIterator = transactionList.iterator();
        while (transactionIterator.hasNext()) {
            AbstractTransaction t = transactionIterator.next();
            int pos = checkTransactionPositionInBlockchain(t, fromTopNumber, blockList);
            if (-1 != pos) {
                transactionIterator.remove();
                System.out.printf("[Blockchain][" + identify + "][WARN] Skip to mining transaction (id:%d) as already been mined\n",
                        t.getId());
                if (transactionRemoveWaitBlockMingPeriod < blockHeight - pos) {
                    System.out.printf("[Blockchain][" + identify + "] Remove transaction (id:%d) from mining pool as already been mined after %d blocks\n",
                            t.getId(), blockHeight - pos);
                    miner.addTransaction(t);
                }
            }
        }
        return transactionList;
    }

    public int checkTransactionPositionInBlockchain(AbstractTransaction t, int fromTopNumber, List<Block> blockList) {
        int blockListSize = blockList.size();
        for (int i = blockListSize - 1; i >= 0 && i > (blockListSize - 1 - fromTopNumber); --i) {
            Block block = blockList.get(i);
            for (AbstractTransaction transaction : block.getTransactions()) {
                if (t.getId() == transaction.getId() && t.getBlockchainId().equals(transaction.getBlockchainId())) {
                    if (t.doesMarkedAsDisappear()) {
                        System.out.println("[Blockchain][" + identify + "] Transaction previous marked as disappeared. Now it appears and we add it");
                        return -1;
                    } else {
                        System.out.printf("[Blockchain][" + identify + "] Transaction exists in blockchain of block %d and we skip to process it\n",
                                block.getBlockIndex());
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    public void sendToMinerForProcess(AbstractTransaction transaction) {
        miner.addTransaction(transaction);
    }

    public synchronized boolean testAndSetNewTransactionAdded(boolean newValue) {
        boolean oldValue = isNewTransactionAdded;
        isNewTransactionAdded = newValue;
        return oldValue;
    }

    // only for test
    public void testSetBlockChain(List<Block> blockList) {
        this.blockList = blockList;
    }

    public String dump() {
        String dump = "<" + identify + " block chain>\n";
        List<Block> copiedBlockList = getCopiedBlockList();
        for (Block b : copiedBlockList
        ) {
            dump += b.Dump();
            dump += "\n";
        }
        dump += "</" + identify + " block chain>\n";
        return dump;
    }

    public String dump(int dumpAmount) {
        String dump = "<" + identify + " block chain>\n";

        List<Block> copiedBlockList = getCopiedBlockList();

        int currentTopIndex = copiedBlockList.size();
        int beginIndex = 0;
        if (dumpAmount < 0) {
            beginIndex = currentTopIndex + dumpAmount;
            if (beginIndex < 0) beginIndex = 0;
            dumpAmount = -dumpAmount;
        }

        for (int i = beginIndex; i < dumpAmount; ++i) {
            dump += copiedBlockList.get(i).Dump();
            dump += "\n";
        }
        dump += "</" + identify + " block chain>\n";
        return dump;
    }
}

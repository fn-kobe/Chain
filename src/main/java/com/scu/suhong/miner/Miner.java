package com.scu.suhong.miner;

import account.AccountManager;
import com.scu.suhong.Hash.MD5Hash;
import com.scu.suhong.block.*;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.dynamic_definition.DynamicalAsset;
import com.scu.suhong.dynamic_definition.DynamicalAssetProcessor;
import com.scu.suhong.network.NetworkListener;
import com.scu.suhong.network.P2P;
import com.scu.suhong.network.P2PConfiguration;
import com.scu.suhong.smartcontract.nondeterminacy.StateSyncManager;
import com.scu.suhong.transaction.TransactionFactory;
import consensus.pow.MiningConfiguration;
import consensus.pow.PoW;
import consensus.pow.PoWException;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import util.*;

import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.scu.suhong.network.P2PConfiguration.getInstance;


public class Miner implements Runnable, NetworkListener {
    static Logger logger = FileLogger.getLogger();
    // TO DO make it multithread array
    List<AbstractTransaction> transactionList = new ArrayList<AbstractTransaction>();
    Lock transactionListLock = new ReentrantLock();
    boolean forceStop = false;
    P2P p2p;
    P2PConfiguration p2PConfiguration = getInstance();
    BlockChain blockChain;
    String miningAccount = "";
    String consensusType = StringHelper.getPoWConsensusFlag();
    String nodeId = "";
    HashMap<String, Integer> addressPoSUsedRight = new HashMap<String, Integer>();

    HashMap<String, String> sentBlockHash = new HashMap<>();
    int runInterval = 1000;//5 seconds by default
    int transactionAllowedSize = 340 * 12; // we only process certain number at a mining time 170 + 170
    int lastProcessedTransactionNumber = 0; // Used for the transaction process speed measurement
    BlockchainSyncManager blockchainSyncManager;
    BlockchainSyncManager externalBlockchainSyncManager;
    BlockchainSyncManager currentBlockchainSyncManager;
    BlockchainSyncManagerSelector blockchainSyncManagerSelector;
    int recentTransactionCheckBlockNumber = 10;

    int minedProcessCount = 0;
    int nextPeerCount = 0;

    // Not to start any service
    int nextExternalPeerCount = 0;

    public Miner() {
        p2p = new P2P();
        blockChain = BlockChain.getInstance();
        blockChain.setMiner(this);
        miningAccount = MiningConfiguration.getMiningAccount();
        nodeId = MiningConfiguration.getNodeId();
        consensusType = MiningConfiguration.getConsensusType();
        blockchainSyncManager = new BlockchainSyncManager(blockChain);

        BlockChain.getExternalManager().setMiner(this);
        externalBlockchainSyncManager = new ProducerBlockchainSyncManager(BlockChain.getExternalManager());

        blockchainSyncManagerSelector = new BlockchainSyncManagerSelector(blockchainSyncManager,
                externalBlockchainSyncManager, p2PConfiguration);
    }

    // Used for test
    public Miner(String NoService) {
    }

    // Used for paper test
    public int getTransactionListCount() {
        return transactionList.size();
    }

    // Used for unit test
    public AbstractTransaction testGetLatestTransaction() {
        if (0 == transactionList.size()) return null;
        return transactionList.get(transactionList.size() - 1);
    }

    @Override
    public void run() {
        try {
            mine();
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("[Miner] Exception happened when mining: " + e);
        }
    }

    public int getRunInterval() {
        return runInterval;
    }

    public void setRunInterval(int runInterval) {
        this.runInterval = runInterval * 1000;
    }

    // for test
    public int getMinedProcessCount() {
        return minedProcessCount;
    }

    public int getTransactionAllowedSize() {
        return transactionAllowedSize;
    }

    public void setTransactionAllowedSize(int transactionAllowedSize) {
        this.transactionAllowedSize = transactionAllowedSize;
    }

    public void mine() throws BlockException, PoWException, InterruptedException {
        logger.info("[Miner] Begin to mine " + java.lang.Thread.currentThread().getId());

        do {
            processNewTransaction();
            //int randomVariety = new RandomHelper().getNumber(5 * 1000);//
            logger.info(String.format("[Miner] Wait %d for the %d th round mine with %d Tx in transaction list\n"
                    , runInterval /*+ randomVariety*/
                    , minedProcessCount
                    , transactionList.size()));
            Thread.sleep(runInterval /*+ randomVariety*/);// fix value + random to let different blockchain size when interval is big
            ++minedProcessCount;
        } while (!forceStop);
        processExit();
        logger.info("[Miner] Exit mine");
    }

    // Only for test purpose
    public void testProcessNewTransaction() throws BlockException, PoWException {
        processNewTransaction();
    }

    void processNewTransaction() throws BlockException, PoWException {
        if (transactionList.isEmpty()) { // process certain number of transaction every time
            logger.info("[Miner] There are no pending transactions, begin to mine empty block");
        }

        BlockBody b = new BlockBody();
        List<AbstractTransaction> fetchedTransactionList = fetchTransaction();
        b.addTransaction(fetchedTransactionList);

        Block block = new Block(new BlockHeader(), b);
        Block latestBlock = blockChain.getLatestBlock();
        block.setPreviousHash(latestBlock.getBlockHash());
        //ThreadHelper.minerSimulateDelay();
        logger.info("[Miner] Begin to mine with Tx amount " + block.getTransactions().size());
        int nounce = findNounceForBlock(block);
        if (-1 == nounce) {
            if (StringHelper.isPoWConsensusFlag(consensusType)) {
                logger.info("[Miner] No matched nounce found");
            } else if (StringHelper.isPoSConsensusFlag(consensusType)){
                logger.info("[Miner] Pos not selected on this node");
            }
            return;
        }

        block.setBlockNounce(nounce);
        block.setBlockIndexAndMiningTime(latestBlock.getBlockIndex() + 1);
        block.setMiner(AccountManager.getFullAddress(miningAccount));
        block.setConsensusType(consensusType);
        block.setNodeId(nodeId);
        logger.info(String.format("[Miner] Succeed to mine one block with %d Txs and block index %d "
                , block.getTransactions().size()
                , block.getBlockIndex()));

        if (!BlockChain.getInstance().hasDuplicatedTransaction(block)) {
            if (blockChain.saveNewBlock(block)) {
                logger.info("[Miner][INFO] Succeed to seal one block into main chain"
                        + " at epoch : " + TimeHelper.getCurrentTimeUsingCalendar());
                broadcastBlock(block);
            } else if (!fetchedTransactionList.isEmpty()) {
                System.out.printf("[Miner] Block is invalid, skip to broadcast. Put back %d transaction to mining queue\n", fetchedTransactionList.size());
                putbackTransaction(fetchedTransactionList);
            }
        } else {
            // Already in blockchain, skip to put it back. While it still has the chance to lose when rebranching
            logger.warn("[Miner] Block has duplicated transaction in blockchain, skip to broadcast");
        }

    }

    int findNounceForBlock(Block block) throws BlockException, PoWException {
        if (consensusType.isEmpty() || consensusType.equalsIgnoreCase(StringHelper.getPoWConsensusFlag())) {
            return PoW.findBlockNounce(block.getPreviousHash(), block.getTransactionHash());
        } else if (consensusType.equalsIgnoreCase(StringHelper.getPoSConsensusFlag())){
            String selectMiningResult = processPoSRight();
            if (selectMiningResult.isEmpty()) return -1;
            // TO DO sign with pos account
            miningAccount = selectMiningResult;
            return 0;
        }
        System.out.println("[Miner] Unsupported consensus " + consensusType);
        return -1;
    }

    String getPosAccount(){
        AccountManager accountManager = AccountManager.getInstance();
        Set<String> allInternalAssetAccountList = accountManager.getAllAccount(AccountManager.getDefaultType());

        int maxRight = Integer.MIN_VALUE;
        String maxRightAccount = "";
        int useRight = 0;
        for (String account : allInternalAssetAccountList){
            int totalRight = accountManager.getBalance(account).intValue();
            useRight = addressPoSUsedRight.containsKey(account) ? addressPoSUsedRight.get(account) : 0;
            int leftRight = totalRight - useRight;
            if (leftRight > maxRight){
                maxRight = leftRight;
                maxRightAccount = account;
            }
        }
        int rightCost = AccountManager.getMiningReward();// to make this account sleep some time
        addressPoSUsedRight.put(maxRightAccount, useRight + rightCost);
        return maxRightAccount;
    }

    int getSelectPosAccountNodeIndex(String account){
        int random = StringHelper.getRandomNumberByHash(MD5Hash.safeGetValue(account.getBytes()));
        int peerCount = P2PConfiguration.getInstance().getPeerAddressListWithoutSelf().size();
        int selectedNodeIndex = random % peerCount;
        System.out.printf("[Miner][INFO] Selected account %s is on node %d\n", account, selectedNodeIndex + 1);
        return selectedNodeIndex;
    }

    // Calculate a relative equal PoS consensus
    String processPoSRight() {
        String account = getPosAccount();
        if (account.isEmpty()){
            addressPoSUsedRight = new HashMap<>();//rest all rights have been used
            account = getPosAccount();
        }

        int selectedNodeIndex = getSelectPosAccountNodeIndex(account);
        int nodeIdNumber = Integer.parseInt(MiningConfiguration.getNodeId());
        if (0 == nodeIdNumber){
            System.out.println("[Miner][WARN] node id is 0. Not correct for current workaround");
        } else{
            System.out.printf("[Miner][INFO] Selected account %s is on node %d\n", account, selectedNodeIndex + 1);
            if (selectedNodeIndex + 1 == nodeIdNumber){
                return account;
            }
        }
        return "";
    }

    private void putbackTransaction(List<AbstractTransaction> fetchedTransactionList) {
        for (AbstractTransaction t : fetchedTransactionList) {
            addTransaction(t);
        }
    }

    public List<AbstractTransaction> fetchTransaction() {
        ArrayList<AbstractTransaction> transactions = new ArrayList<>();
        lockTransactionList(FunctionHelper.getFunctionName());

        int itemFetchedCount = 0;
        int transactionTotalSize = 0; // for count transaction's size
        AbstractTransaction t;
        RandomHelper randomHelper = new RandomHelper();
        Map<String, Integer> assetTypeMap = new HashMap<>();
        int tryTime = 0;
        for (itemFetchedCount = 0
             ; (!transactionList.isEmpty()) && (transactionTotalSize < transactionAllowedSize)
                     && tryTime < 100
                ; ++itemFetchedCount, ++tryTime) {
            int itemFetch = randomHelper.getNumber(transactionList.size());// select randomly

            t = transactionList.remove(itemFetch);
            transactions.add(t);
            transactionTotalSize += t.getJson().toString().length();
            logger.info("[Miner] fetched one Tx " + t.getJson().toString());
        }

        logger.info(String.format("[Miner] Change transaction processed number. Old value %s [EPOCH]: %s\n"
                , lastProcessedTransactionNumber,
                TimeHelper.getCurrentTimeUsingCalendar()));

        lastProcessedTransactionNumber = transactionList.size();
        logger.info(String.format("[Miner] Fetched %d Tx, and remain : %d Tx, [EPOCH]: %s\n"
                , itemFetchedCount
                , lastProcessedTransactionNumber
                , TimeHelper.getCurrentTimeUsingCalendar()));
        unlockTransactionList(FunctionHelper.getFunctionName());
        return BlockChain.getInstance().removeTransactionExistInBlockchain(transactions, recentTransactionCheckBlockNumber);
    }

    // speed is per seconds
    public int getLastProcessedTransactionSpeed() {
        return lastProcessedTransactionNumber * 1000 / runInterval;
    }

    private void processExit() throws BlockException {
        //We save the block after mining, then there is no need to save again
        // Only required, if fork condition
        // TO DO if fork is introduced
//        if (BlockchainState.hasPeers()) {
//            BlockDBHandler blockDBHandler = BlockDBHandler.getInstance();
//            blockDBHandler.save(blockChain.getBlockList());
//        }
    }

    private void broadcastBlock(Block block) {
        logger.info("[Miner] Begin to broadcast seal block");
        // Try three times if exception happens
        try {
            send(block);
            logger.info("[Miner] Succeed to broadcast seal block");
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("[Miner][ERROR] Failed to broadcast seal block");
        }
    }

    private void send(Block block) throws IOException {
        if (!block.isBlockListValid()) {// only internal block
            logger.info("[Miner] Skip send, invalid block format: " + block.getJson());
            return;
        }
        p2p.send(block);
    }

    public void send(List<Block> blocks) throws IOException {
        logger.info("[Miner] Begin to send block list with size " + blocks.size());
        if (blocks.size() > 500) { // currently we just allow 500 blocks
            System.out.println("[Miner] The number of blocks has exceed the max. If true, please consider reconstruct");
        }
        p2p.send(Block.getBlockListJson(blocks), p2PConfiguration.getAllBlockchainObserverAddressList(), p2PConfiguration.getAllBlockchainObserverPortList());
        logger.info("[Miner] Succeed to send block list with size " + blocks.size());
    }

    @Override
    public void onNetworkMsg(byte[] msg, InetAddress address, int port) {
        String stringMsg = new String(msg); // Miner only process string message
        String msgHash = null;
        try {
            msgHash = StringHelper.byteArrayToHexString(MD5Hash.getValue(msg));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("[Miner]  Get network message from " + address.getHostAddress() + ":" + port + " "
                + stringMsg + " at time " + TimeHelper.getCurrentTimeUsingCalendar());
        currentBlockchainSyncManager = blockchainSyncManagerSelector.getBlockchainSyncManager(address, port, msg);
        if (currentBlockchainSyncManager.tryProcessBlockQueryMsg(msg, msgHash)) {
            return;
        } else if (tryProcessSmartContractStateSyncMsg(msg, msgHash, address)) {
            return;
        } else if (tryProcessTransactionMsg(msg, msgHash, address, port)) {
            return;
        } else {
            if (!currentBlockchainSyncManager.tryProcessBlockMsg(msg, msgHash, address, port)) {
                logger.warn("[Miner] Msg is not processed " + msgHash);
            }
        }
    }

    private boolean tryProcessTransactionMsg(byte[] msg, String msgHash, InetAddress address, int port) {
        if (p2PConfiguration.isExternal(address.getHostAddress(), port)) {
            logger.info("[Miner] Skip to try process as transaction, as the address is foreign " + address.getHostAddress() + ":" + port);
            return false;
        }
        String stringMsg = new String(msg);
        JSONObject jo = blockchainSyncManager.translateToObject(stringMsg);
        if (null != jo && TransactionFactory.isSupportedTransactionJson(jo)) {
            logger.info("[Miner] Transaction message received " + msgHash);
            AbstractTransaction transaction;
            if (null != (transaction = TransactionFactory.createFromJson(jo))) {
                onTransactionMsg(transaction);
            }
            return true;
        }

        return false;
    }

    private boolean tryProcessSmartContractStateSyncMsg(byte[] msg, String msgHash, InetAddress address) {
        String stringMsg = new String(msg);
        return StateSyncManager.process(stringMsg);
    }

    public void onTransactionMsg(AbstractTransaction transaction) {
        addTransaction(transaction);
    }

    public boolean addTransaction(AbstractTransaction transaction) {
        lockTransactionList(FunctionHelper.getFunctionName());
        for (AbstractTransaction t : transactionList) {
            if (transaction.getId() == t.getId() && transaction.getBlockchainId().equals(t.getBlockchainId())) {
                System.out.printf("[Miner][WARN] Duplicated transactions with the same ID %d from same blockchain %s found, skip to add\n", t.getId(), t.getBlockchainId());
                return false;
            }
        }
        boolean r = false;
        logger.info(String.format("[Miner] Try to add new transaction, at EPOCH: %s\n", TimeHelper.getCurrentTimeUsingCalendar()));

        //handle pre-compile and init the dynamical asset
        AbstractTransaction newTransaction = transaction;
        boolean isOK;
        if (transaction instanceof DynamicalAsset) {
            newTransaction = DynamicalAssetProcessor.preProcessDynamicalTransaction((DynamicalAsset) transaction);
            isOK = (null != newTransaction) && DynamicalAsset.check((DynamicalAsset) newTransaction);
        } else {
            isOK = newTransaction.check();
        }

        // handle check
        if (isOK) {
            transactionList.add(newTransaction);
            logger.info(String.format("[Miner] Succeed to add new transaction, at EPOCH: %s\n", TimeHelper.getCurrentTimeUsingCalendar()));
            r = true;
        } else {
            logger.warn(String.format("[Miner][WARN] Failed to add transaction as %s",
                    (null != newTransaction) ? newTransaction.checkFailMessage() : "can not init instance"));
        }

        logger.info(String.format("[Miner] Current transaction list size: %d, at EPOCH: %s\n"
                , transactionList.size()
                , TimeHelper.getCurrentTimeUsingCalendar()));
        unlockTransactionList(FunctionHelper.getFunctionName());
        return r;
    }

    //
    public void lockTransactionList(String additionalMsg) {
        //logger.info("[Miner][DEBUG] Try to lock TransactionList, " + additionalMsg);
        transactionListLock.lock();
    }

    private void unlockTransactionList(String additionalMsg) {
        //logger.info("[Miner][DEBUG] Try to un lock TransactionList," + additionalMsg);
        transactionListLock.unlock();
    }

    public void setForceStop() {
        this.forceStop = true;
    }

    public void syncLatestBlockListFromPeers() {
        blockchainSyncManager.syncLatestBlockListFromPeers(getNextPeerAddressPort());

        if (!getInstance().getProducerPeerAddressList().isEmpty()) {// only sync when have producer address
            externalBlockchainSyncManager.syncLatestBlockListFromPeers(getNextProducerPeerAddressPort());
        }
    }

    AddressPort getNextPeerAddressPort() {
        AddressPort r = null;
        List<AddressPort> peerAddressList = getInstance().getPeerAddressPortList();
        for (int i = 0; i < peerAddressList.size(); ++i) {
            r = peerAddressList.get(nextPeerCount);
            ++nextPeerCount;
            if (nextPeerCount >= peerAddressList.size()) nextPeerCount = 0;
            if (!MiningConfiguration.isSelf(r)) return r;
        }
        return r;
    }

    AddressPort getNextProducerPeerAddressPort() {
        AddressPort r = null;
        List<AddressPort> externalPeerAddress = getInstance().getProducerPeerAddressPortList();
        for (int i = 0; i < externalPeerAddress.size(); ++i) {
            r = externalPeerAddress.get(nextExternalPeerCount);
            ++nextExternalPeerCount;
            if (nextExternalPeerCount >= externalPeerAddress.size()) nextExternalPeerCount = 0;
            if (!MiningConfiguration.isSelf(r)) return r;
        }
        return r;
    }
}

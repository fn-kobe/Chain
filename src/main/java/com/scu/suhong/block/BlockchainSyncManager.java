package com.scu.suhong.block;

import Service.BlockchainService;
import account.AccountManager;
import com.scu.suhong.miner.QueryTopBlocksInformation;
import com.scu.suhong.network.NetworkException;
import com.scu.suhong.network.P2P;
import com.scu.suhong.network.P2PConfiguration;
import consensus.pow.MiningConfiguration;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import util.AddressPort;
import util.FileLogger;
import util.StringHelper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class BlockchainSyncManager {
    static Logger logger = FileLogger.getLogger();
    P2P p2p;
    P2PConfiguration p2PConfiguration;
    BlockChain internalBlockChain;
    ExternalBlockchainManager exBlockchainManager;
    String identify; //  Used to differtiate different sync manager between internal and external
    boolean isExternal;
    int blockchainUpdatePeriod = -1;// we use the mining period to measurement
    BlockchainSyncNetworkListener blockchainSyncNetworklistener;
    List<String> peerAddressWithoutSelfAddress;
    private int defaultQueryTopNumber = 10; // this is default to suppose that the difference is only 10000 blocks, TO DO change is not enough
    private int defaultBlockQueryNumber = 10;
    private int skipLogaccount = 0;// avoid too many logs

    public BlockchainSyncManager(BlockChain blockChain) {
        p2p = new P2P();
        p2PConfiguration = P2PConfiguration.getInstance();
        this.internalBlockChain = blockChain;
        this.identify = "internal";
        isExternal = false;
        blockchainUpdatePeriod = -1;
        blockchainSyncNetworklistener = new BlockchainSyncNetworkListener();
        blockchainSyncNetworklistener.setSyncP2P(p2p);
    }

    public BlockchainSyncManager(ExternalBlockchainManager exBlockchainManager) {
        p2p = new P2P();
        p2PConfiguration = P2PConfiguration.getInstance();
        this.exBlockchainManager = exBlockchainManager;
        this.identify = "external";
        isExternal = true;
        blockchainUpdatePeriod = -1;
        blockchainSyncNetworklistener = new BlockchainSyncNetworkListener();
        blockchainSyncNetworklistener.setSyncP2P(p2p);
    }

    // As there are many ternal chain, and then we will get by chain id.
    // For unification, this is sued for internal block and the parameter is from internal chain id
    BlockChain getCurrentBlockchain(String chainId){
        if (null == chainId || chainId.isEmpty() || MiningConfiguration.getBlockchainStringId().equals(chainId)){
            if (null == chainId || chainId.isEmpty()){
                logger.info("[BlockchainSyncManager][WARN] no chain id found to get BlockchainSyncManager");
            }
            return internalBlockChain;
        }
        return exBlockchainManager.getWorker(chainId);
    }

    @NotNull
    public static String formatQueryTopBlockMsg() throws UnknownHostException {
        return formatQueryTopBlockMsg(1);
    }

    @NotNull
    public static String formatQueryTopBlockMsg(int number) {
        return formatQueryTopBlockMsg(number, -1);
    }

    public static String formatQueryTopBlockMsg(int number, int start) {
        String selfIpAddress = MiningConfiguration.getHostIP();
        return StringHelper.getQueryTopBlockMsg() +
                StringHelper.getQueryTopBlockMsgSeparator() +
                selfIpAddress +
                StringHelper.getQueryTopBlockMsgSeparator() +
                number +
                StringHelper.getQueryTopBlockMsgSeparator() +
                start +
                StringHelper.getQueryTopBlockMsgSeparator() +
                MiningConfiguration.getSelfListenPort();
    }

    static public QueryTopBlocksInformation getQueryTopBlocksInformation(String msgText) throws NetworkException {
        if (msgText.contains(StringHelper.getQueryTopBlockMsg())) {
            logger.info("[BlockchainSyncManager] Got queryTopBlocks message: " + msgText);
            String[] msgArray = msgText.split(StringHelper.getQueryTopBlockMsgSeparator());
            if (msgArray.length < 4) {
                logger.info("[BlockchainSyncManager] getResourcePeerAddressMsg format error: " + msgText);
                throw new NetworkException();
            }
            String peerAddress = StringHelper.getIPv4String(msgArray[1]);
            int numberOfBlockToQuery = Integer.parseInt(msgArray[2]);
            int startIndexOfBlockToQuery = Integer.parseInt(msgArray[3]);
            int port = StringHelper.getDefaultPort();
            if (msgArray.length > 4) port = Integer.valueOf(msgArray[4]);
            else {
                System.out.println("[BlockchainSyncManager][WARN] peer port is not specified in the message, use default port " +port);
            }
            return new QueryTopBlocksInformation(peerAddress, numberOfBlockToQuery, startIndexOfBlockToQuery, port);
        }
        return null;
    }

    public static JSONObject translateToObject(String transactionMsg) {
        try {

            return new JSONObject(transactionMsg);
        } catch (JSONException e){
            System.out.println("[BlockchainSyncManager][Info] Not a Json object");
            return null;
        }
    }

    public boolean tryProcessBlockMsg(byte[] msg, String msgHash, InetAddress address, int port) {
        if (!doProcessBlockMsg(msg, msgHash, address, port)) {
            resetUpdate();
            return false;
        }
        return true;
    }

    // reset blockchainUpdatePeriod to make the sync can be started again
    public void resetUpdate() {
        blockchainUpdatePeriod = -1;
    }

    public boolean doProcessBlockMsg(byte[] msg, String msgHash, InetAddress address, int port) {
        List<Block> blockList = tryGetBlockListFromMsg(msg);
        if (null == blockList || blockList.isEmpty()) return false;

        if (1 == blockList.size()) {// not a block array, then try latest block
            if (0 == blockList.get(0).getBlockIndex()){
                System.out.printf("[BlockchainSyncManager][%s][WARN] Skip to process only genesis block update\n", identify);
                return true;
            }
            return tryProcessLatestBlock(msg, blockList.get(0), address, port);
        }

        List<Block> anotherBlockLists = blockList;
        if (null != anotherBlockLists || !anotherBlockLists.isEmpty()) {
            if (!shouldWeUpdate(anotherBlockLists)) {
                logger.warn("[BlockchainSyncManager][" + identify + "] Received block list is not valid or not longer than us: " + msgHash);
                return false;
            }
            return tryProcessBlockListUpdate(anotherBlockLists, address, port);
        }
        return false;
    }

    static public BlockMessageState MsgBlockState(byte[] msg) {
        List<Block> blockList = tryGetBlockListFromMsg(msg);
        if (null == blockList || blockList.isEmpty()) return BlockMessageState.ENoneBlock;

        // we only test the last block to see whether it is external or internal, as the first block, which may be the genesis block, whose miner has no block id
        // as we assume that internal and external will not be mixed togethor
        Block block = blockList.get(blockList.size()-1);
        if (AccountManager.isExternalAddress(block.getMiner())) return BlockMessageState.EExternalBlock;

        return BlockMessageState.EInternalBlock;
    }

    static public List<Block> tryGetBlockListFromMsg(byte[] msg) {
        List<Block> r = null;

        String stringMsg = new String(msg);
        JSONObject jo = translateToObject(stringMsg);
        if (null == jo) return null;
        JSONArray blockList = jo.optJSONArray("blocks");
        if (null == blockList) {// not a block array, then try latest block
            Block block = Block.createFromJson(jo);
            if (null != block){
                r = new ArrayList<>();
                r.add(block);
            }
        } else {
            r = Block.createFromJson(blockList);
        }
        return r;
    }

    String getChainId(List<Block> blockList){
        if (null == blockList || blockList.isEmpty()) return "";

        return blockList.get(blockList.size() -1).getChainId();
    }

    public boolean shouldWeUpdate(List<Block> anotherBlockLists) {
        if (!Block.isBlockListValid(anotherBlockLists, isExternal)) return false;

        String chainId = getChainId(anotherBlockLists);
        BlockChain blockChain = getCurrentBlockchain(chainId);
        if (null == blockChain){
            logger.info("[BlockchainSyncManager][" + identify + "][ERROR] Blockchain is not ready for " + chainId);
            return false;
        }
        if (0 == blockChain.getBlockList().size()) {
            logger.info("[BlockchainSyncManager][" + identify + "] Stored blockchain is empty");
            return true;
        }
        //Fixed at 2020-03-09 why get from blockchainSyncNetworklistener?
        //Block anotherLatestBlock = blockchainSyncNetworklistener.getLatestBlock();
        //if (null == anotherLatestBlock) anotherLatestBlock = anotherBlockLists.get(anotherBlockLists.size() - 1);
        Block anotherLatestBlock = anotherBlockLists.get(anotherBlockLists.size() - 1);
        Block selfLatestBlock = blockChain.getLatestBlock();
        // if the blockchain is less than us, no action
        int delta = anotherLatestBlock.getBlockIndex() - selfLatestBlock.getBlockIndex();
        if (delta <= 0) {
            System.out.printf("[BlockchainSyncManager][" + identify + "]  Peer blockchain length %d with latest index %d is not longer than ours %d (selfLatestBlock.getBlockIndex())\n",
                    anotherBlockLists.size(), anotherLatestBlock.getBlockIndex(), selfLatestBlock.getBlockIndex());
            return false;
        }

        logger.info("[BlockchainSyncManager][" + identify + "]  Peer blockchain is longer than ours with delts: " + delta + ". Blockchain is " + blockChain.identify);
        return true;
    }

    public boolean updateCurrentBlockchain(Block anotherLatestBlock, InetAddress address, int port) {
        BlockChain blockChain = getCurrentBlockchain(anotherLatestBlock.getChainId());
        if (anotherLatestBlock.isBlockListValid(isExternal)) {
            int anotherLatestIndex = anotherLatestBlock.getBlockIndex();
            int localLatestIndex = -1;// If not synced before, this is -1
            if (null != blockChain.getLatestBlock()) localLatestIndex = blockChain.getLatestBlock().getBlockIndex();
            // We treat this case sepcially as it always happens one block advance. Other miner may find first
            if (-1 != localLatestIndex && anotherLatestIndex == localLatestIndex + 1) {
                if (anotherLatestBlock.getPreviousHash().equals(blockChain.getLatestBlock().safeGetBlockHash())) {
                    try {
                        blockChain.saveNewBlock(anotherLatestBlock);
                    } catch (BlockException e) {
                        System.out.println("[BlockchainSyncManager][ERROR][" + identify + "] [Error] expeption haappen when add block ");
                        e.printStackTrace();
                        return false;
                    }
                    return true;
                } // else not one block advance, have to sync
            }

            if (anotherLatestIndex > localLatestIndex) {
                System.out.println("[BlockchainSyncManager][WARN][" + identify + "]  Another block is longer than us, try to sync it");
                return syncLatestBlockListFromPeers(address, port);
            } else {
                System.out.println("[BlockchainSyncManager][" + identify + "]  We have longest or equal blockchain");
                return false;
                // NOP as few block
            }
        } else {
            try {
                logger.info("[BlockchainSyncManager][" + identify + "] Block is invalid with block hash " + anotherLatestBlock.getBlockHash());
            } catch (BlockException e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    int getMiningPeriod() {
        return BlockchainService.getInstance().getMiner().getMinedProcessCount();
    }

    int waitMiningPeriod() {
        return 1;
    }

    public synchronized boolean syncLatestBlockListFromPeers(InetAddress address, int port) {
        if (null == address) return syncLatestBlockListFromPeers("", StringHelper.getDefaultPort());

        return syncLatestBlockListFromPeers(address.getHostAddress(), port);
    }

    public synchronized boolean syncLatestBlockListFromPeers(AddressPort addressPort) {
        if (null == addressPort || !addressPort.isValid()){
            System.out.println("[BlockchainSyncManager][" + identify + "][ERROR] Peer address is not configured or valid. Please check configuration file ");
            return false;
        }
        return syncLatestBlockListFromPeers(addressPort.getAddress(), addressPort.getPort());
    }

    public synchronized boolean syncLatestBlockListFromPeers(String address, int port) {
        if (-1 != blockchainUpdatePeriod && getMiningPeriod() < blockchainUpdatePeriod + waitMiningPeriod()) {
            ++skipLogaccount;
            if (10 <= skipLogaccount) {
                logger.info("[BlockchainSyncManager][" + identify + "] Already in query, skip");
                skipLogaccount = 0;
            }
            return true;
        }

        blockchainSyncNetworklistener.resetQueriedBlockLists();
        blockchainUpdatePeriod = getMiningPeriod();
        boolean r = queryLatestBlock(defaultQueryTopNumber, -1, address, port);
        if (!r) resetUpdate();
        return r;
    }

    public boolean queryLatestBlock(int number, int start, String address, int port) {
        if (null == address || address.isEmpty()) {// Only for internal peer
            return queryLatestBlock(number, start, p2p, getPeerAddressListWithoutSelf(), getPeerPortListWithoutSelf());
        }
        return queryLatestBlock(number, start, p2p, address, port);//both for internal and external blockchain peers
    }

    // will be override in external class to external address
    public List<String> getPeerAddressListWithoutSelf() {
        return P2PConfiguration.getInstance().getPeerAddressListWithoutSelf();
    }

    public List<Integer> getPeerPortListWithoutSelf() {
        return P2PConfiguration.getInstance().getPeerPortListWithoutSelf();
    }

    public boolean queryLatestBlock(int number, int start, P2P queryP2P, String peerIpAddress, int peerPort) {
        if (!p2PConfiguration.isInternalPeer(peerIpAddress, peerPort) && !p2PConfiguration.isProducerPeer(peerIpAddress, peerPort)){
            System.out.printf("[BlockchainSyncManager][WARN] No need to sync as the address %s:%d is not internal or producer address\n"
                    , peerIpAddress, peerPort);
            return false;
        }

        List<String> peerAddressList = new ArrayList<>();
        List<Integer> peerPortList = new ArrayList<>();
        peerAddressList.add(peerIpAddress);
        peerPortList.add(peerPort);
        return queryLatestBlock(number, start, queryP2P, peerAddressList, peerPortList);
    }

    public boolean queryLatestBlock(int number, int start, P2P queryP2P, List<String> peerIpAddressList, List<Integer> peerPortList) {
        if (!peerIpAddressList.isEmpty()) {
            System.out.printf("[BlockchainSyncManager][" + identify + "]  Begin to query %d block , which begins from %d (-1 means the latest block)\n", number, start);
            try {
                String msgToSend = formatQueryTopBlockMsg(number, start);
                logger.info("[BlockchainSyncManager][" + identify + "] [DEBUG] Try to send query message ");
                queryP2P.send(msgToSend, peerIpAddressList, peerPortList);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            logger.warn("[BlockchainSyncManager][" + identify + "] [WARN] No peers to query latest block");
            return false;
        }
        return true;
    }

    public boolean tryProcessLatestBlock(byte[] msg, Block latestBlock, InetAddress address, int port) {
        String stringMsg = new String(msg);
        if (null != latestBlock) {
            logger.info("[BlockchainSyncManager][" + identify + "]  Block message received " + stringMsg);
            return updateCurrentBlockchain(latestBlock, address, port);
        }

        logger.info("[BlockchainSyncManager][" + identify + "]  Not a block message or has error in it:  " + stringMsg);
        return false;
    }

    private boolean tryProcessBlockListUpdate(List<Block> anotherBlockLists, InetAddress address, int port) {
        BlockChain blockChain = getCurrentBlockchain(getChainId(anotherBlockLists));
        blockchainUpdatePeriod = getMiningPeriod();
        logger.info("[BlockchainSyncManager][" + identify + "]  Begin to process block list update");
        // Check block lower blocks, should be the same
        Block anotherOldestBlock = anotherBlockLists.get(0);
        if (anotherOldestBlock.getBlockIndex() > blockChain.getBlockList().size() - 1) {
            System.out.printf("[BlockchainSyncManager][" + identify + "]  Peer (%s:%d) blockchain list %d is far from ours: %d\n",
                    address.getHostAddress(), port, anotherOldestBlock.getBlockIndex(), blockChain.getBlockList().size() - 1);
        }

        blockchainSyncNetworklistener.addQueriedBlockLists(anotherBlockLists);
        blockchainSyncNetworklistener.setPeerAddress(address);
        blockchainSyncNetworklistener.setPeerPort(port);

        return updateOrStillQuery(blockchainSyncNetworklistener);
    }

    private boolean updateOrStillQuery(BlockchainSyncNetworkListener blockchainSyncNetworklistener) {
        int latestEqualBlockIndex = getLatestEqualBlockIndex(blockchainSyncNetworklistener.getQueriedBlockLists());
        // found the same. then update it
        if (0 <= latestEqualBlockIndex) {
            //Update
            return null != updateCurrentBlockchain(blockchainSyncNetworklistener.getQueriedBlockLists(), latestEqualBlockIndex);
        }

        // Not found, if already genesis block
        List<Block> syncBlockList = blockchainSyncNetworklistener.getQueriedBlockLists();
        int anotherBlockListsOldestIndex = 0;
        if (!syncBlockList.isEmpty()) {
            anotherBlockListsOldestIndex = syncBlockList.get(0).getBlockIndex();
            // Already found the genesis block is not the same, have to force replace
            if (anotherBlockListsOldestIndex == 0) {
                logger.warn("[BlockchainSyncManager][" + identify + "] [WARN] The genesis block of the to-be-update block is not the same as the current latest block, force update");
                // udpate
                return null != updateCurrentBlockchain(blockchainSyncNetworklistener.getQueriedBlockLists(), 0);
            }
        }

        // continue to query
        if (!queryLatestBlock(defaultBlockQueryNumber,
                anotherBlockListsOldestIndex - 1,
                blockchainSyncNetworklistener.getSyncP2P(),
                blockchainSyncNetworklistener.getPeerAddress().getHostAddress(),
                blockchainSyncNetworklistener.getPeerPort())) {
            logger.error("[BlockchainSyncManager][" + identify + "]  Send blockchain query information error to " +
                    blockchainSyncNetworklistener.getPeerAddress().getHostAddress());
            resetUpdate();
            return false;
        }

        return true;
    }

    public List<Block> updateCurrentBlockchain(List<Block> anotherBlockLists, int startIndex) {
        BlockChain blockChain = getCurrentBlockchain(getChainId(anotherBlockLists));
        List<Block> blockList = new ArrayList<>();
        for (int i = startIndex; i < anotherBlockLists.size(); ++i) {
            blockList.add(anotherBlockLists.get(i));
        }
        try {
            blockChain.updateBlockListWithLongerChain(blockList);
        } catch (BlockException e) {
            e.printStackTrace();
            logger.info("[BlockchainSyncManager][" + identify + "]  Cannot for add");
            resetUpdate();
            return null;
        }
        resetUpdate();
        return blockList;
    }

    private int getLatestEqualBlockIndex(List<Block> anotherBlockLists) {
        BlockChain blockChain = getCurrentBlockchain(getChainId(anotherBlockLists));
        Block block;
        int result = -1;
        for (int i = 0; i < anotherBlockLists.size(); ++i) {
            block = anotherBlockLists.get(i);
            if (block.getBlockIndex() + 1 > blockChain.getBlockList().size())
                break;// more than the current blockchain return

            if (block.isEqual(blockChain.getBlock(block.getBlockIndex()))) {
                logger.error("[BlockchainSyncManager][" + identify + "]  Unequal block index found " + i);
                result = i; // plus -1 to get the index
            }
        }
        return result;
    }

    public boolean tryProcessBlockQueryMsg(byte[] msg, String msgHash) {
        return -1 != doProcessBlockQueryMsg(msg, msgHash);
    }

    public int doProcessBlockQueryMsg(byte[] msg, String msgHash) {
        String stringMsg = new String(msg);
        QueryTopBlocksInformation queryTopBlocksInformation;
        try {
            queryTopBlocksInformation = getQueryTopBlocksInformation(stringMsg);
        } catch (NetworkException e) {
            e.printStackTrace();
            return -1;
        }
        if (null == queryTopBlocksInformation) {
            logger.info("[BlockchainSyncManager][" + identify + "]  Message is not to query top block message: " + msgHash);
            return -1;
        }
        System.out.printf("[BlockchainSyncManager][" + identify + "]  Get top block message %d received " + msgHash + "\n", queryTopBlocksInformation.getStartBlockIndex());

        // We should send internal blockchain data to external peers, so we use BlockChain.getInstance()
        List<Block> blockchainList = BlockChain.getInstance().getLatestBlock(getNumberOfQueryBlock(queryTopBlocksInformation), getQueryStartIndex(queryTopBlocksInformation));
        if (blockchainList.size() > 0) {
            logger.info("[BlockchainSyncManager][" + identify + "] Reply with query top block message");
            safeSend(blockchainList, queryTopBlocksInformation.getPeerAddress(), queryTopBlocksInformation.getPeerPort());
        } else {
            logger.warn("[BlockchainSyncManager][" + identify + "] [WARN] No block matches the query parameter " + stringMsg);
        }
        return blockchainList.size();
    }

    // We should send internal blockchain data to external peers, so we use BlockChain.getInstance()
    int getQueryStartIndex(QueryTopBlocksInformation queryTopBlocksInformation) {
        if (-1 != queryTopBlocksInformation.getStartBlockIndex()) {
            return queryTopBlocksInformation.getStartBlockIndex();
        }
        return BlockChain.getInstance().getBlockList().size() - 1;
    }

    // We should send internal blockchain data to external peers, so we use BlockChain.getInstance()
    int getNumberOfQueryBlock(QueryTopBlocksInformation queryTopBlocksInformation) {
        if (-1 != queryTopBlocksInformation.getNumberOfQueryBlock()) {
            return queryTopBlocksInformation.getNumberOfQueryBlock();
        }
        return BlockChain.getInstance().getBlockList().size();
    }

    public void safeSend(List<Block> blocks, String address, int port) {
        try {
            logger.info("[BlockchainSyncManager][" + identify + "]  Begin to send block list " + Block.getBlockListJson(blocks) + " to " + address + ":" + port);
            send(blocks, address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(List<Block> blocks, String address, int port) throws IOException {
        logger.info("[BlockchainSyncManager][" + identify + "]  Begin to send block list with size " + blocks.size());
        if (blocks.size() > 500) { // currently we just allow 500 blocks
            System.out.println("[BlockchainSyncManager][" + identify + "]  The number of blocks has exceed the max. If true, please consider reconstruct");
        }
        String blockListJson = Block.getBlockListJson(blocks);
        if (!blockListJson.isEmpty()) {
            p2p.send(blockListJson, address, port);
            logger.info("[BlockchainSyncManager][" + identify + "]  Succeed to send block list with size " + blocks.size());
        } else {
            logger.info("[BlockchainSyncManager][" + identify + "][ERROR]  Failed to send block list");
        }
    }

    class BlockchainSyncNetworkListener {
        List<Block> queriedBlockLists = new ArrayList<>();
        P2P syncP2P;
        InetAddress peerAddress;
        int peerPort = 0;

        public P2P getSyncP2P() {
            return syncP2P;
        }

        public void setSyncP2P(P2P syncP2P) {
            this.syncP2P = syncP2P;
        }

        public InetAddress getPeerAddress() {
            return peerAddress;
        }

        public void setPeerAddress(InetAddress peerAddress) {
            this.peerAddress = peerAddress;
        }

        public int getPeerPort() {
            return peerPort;
        }

        public void setPeerPort(int peerPort) {
            this.peerPort = peerPort;
        }

        public void addQueriedBlockLists(List<Block> newComingQueriedBlockLists) {
            if (null == newComingQueriedBlockLists || newComingQueriedBlockLists.isEmpty()) {
                logger.warn("[BlockchainSyncNetworkListener][" + identify + "]  Get empty update list. Skip to process");
                return;
            }

            if (null == this.queriedBlockLists || this.queriedBlockLists.isEmpty()) {
                this.queriedBlockLists = newComingQueriedBlockLists;
                return;
            }

            List<Block> alreadyQueriedBlockLists = this.queriedBlockLists;
            if (alreadyQueriedBlockLists.get(0).getPreviousHash().equals(newComingQueriedBlockLists.get(newComingQueriedBlockLists.size() - 1).safeGetBlockHash())) {
                // With the small index block to be at the front
                this.queriedBlockLists = newComingQueriedBlockLists;
                this.queriedBlockLists.addAll(alreadyQueriedBlockLists);
            } else { // Not get expect sequence
                Block newComingQueriedBlockListsLatestBlock = newComingQueriedBlockLists.get(newComingQueriedBlockLists.size() - 1);
                Block alreadyQueriedLatestBlock = alreadyQueriedBlockLists.get(alreadyQueriedBlockLists.size() - 1);
                if (newComingQueriedBlockListsLatestBlock.getBlockIndex() > alreadyQueriedLatestBlock.getBlockIndex() + maxRefreshLength()) {
                    this.queriedBlockLists = newComingQueriedBlockLists;
                    logger.warn("[BlockchainSyncNetworkListener][" + identify + "]  Blockchain from peer is far from us. Refresh current cached block in sync");
                } else if (newComingQueriedBlockListsLatestBlock.getBlockIndex() == alreadyQueriedLatestBlock.getBlockIndex() + maxRefreshLength() &&
                        newComingQueriedBlockLists.size() > this.queriedBlockLists.size()) {
                    // If the same index, it should be the query more block, then we requey the longer
                    this.queriedBlockLists = newComingQueriedBlockLists;
                    logger.warn("[BlockchainSyncNetworkListener][" + identify + "]  Blockchain from peer is same as cached, while the new one has the longer block. Refresh current cached block in sync");
                } else {
                    logger.warn("[BlockchainSyncNetworkListener][" + identify + "][WARN]  Blockchain from peer with index error " +
                            newComingQueriedBlockListsLatestBlock.getBlockIndex() + " : " + (alreadyQueriedLatestBlock.getBlockIndex() + maxRefreshLength()));
                    resetUpdate();

                }
            }
        }

        int maxRefreshLength() {
            return 0;
        }

        public void resetQueriedBlockLists() {
            this.queriedBlockLists = new ArrayList<>();
        }

        public Block getLatestBlock() {
            if (queriedBlockLists.isEmpty()) return null;

            return queriedBlockLists.get(queriedBlockLists.size() - 1);
        }

        public List<Block> getQueriedBlockLists() {
            return queriedBlockLists;
        }
    }
}

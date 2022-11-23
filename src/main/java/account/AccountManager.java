package account;

import com.scu.suhong.block.Block;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.transaction.AssetCreateTransaction;
import com.scu.suhong.transaction.ConditionalAssociationTransaction;
import com.scu.suhong.transaction.CrosschainInterface;
import com.scu.suhong.transaction.Transaction;
import com.scu.suhong.transaction.multipleTypeExchange.MultiTypeExchangeTransaction;
import consensus.pow.MiningConfiguration;
import util.FileLogger;

import java.util.*;
import java.util.regex.Pattern;

public class AccountManager {
    static org.apache.log4j.Logger logger = FileLogger.getLogger();

    private static AccountManager instance;
    private static int miningReward = 50; // TO DO, not use the fixed value .we fix this value
    static final String defaultType = "internalCoin";
    private HashMap<String, MultiTypeAccountManager> multiTypeAccountManagerList;

    private AccountManager() {
        System.out.println("[AccountManager] Construct");
        multiTypeAccountManagerList = new HashMap<>();
        initMulitpleAssetAccountBalance();
    }

    public static synchronized AccountManager getInstance() {
        if (null == instance) {
            instance = new AccountManager();
        }
        return instance;
    }

    // Only for test
    static synchronized AccountManager testGetInstance(String reload) {
        instance = new AccountManager();
        return instance;
    }

    public Set<String> getAllAccount(String type){
        return multiTypeAccountManagerList.get(type).getAllAccount();
    }

    public static String getDefaultType(){
        return defaultType;
    }

    // Full address is: chainId?adress; '?' is used to connect them
    // if the address has already chainId, we just return; otherwise we add the chainId
    public static String getFullAddress(String address) {
        if (address.contains(getAddressConnectSymbol())) return address;
        else {
            String chainIdPreix = MiningConfiguration.getBlockchainId() + getAddressConnectSymbol();
            return chainIdPreix + address;
        }
    }

    public static String getFullAddress(String address, String chainID) {
        if (address.contains(getAddressConnectSymbol())) return address;

        if (!chainID.isEmpty()) {
            String chainIdPreix = chainID + getAddressConnectSymbol();
            return chainIdPreix + address;
        } else {
            return getFullAddress(address);
        }
    }

    public static boolean doesAssetTypeMatch(String t1, String t2){
        if (null == t1 || t1.isEmpty()) t1 = defaultType;
        if (null == t2 || t2.isEmpty()) t2 = defaultType;

        return t1.equals(t2);
    }

    public static String getFullAddress(String address, int blockchainId) {
        if (-1 == blockchainId) return getFullAddress(address);
        else return doGetFullAddress(address, blockchainId);
    }

    private static String doGetFullAddress(String address, int blockchainId) {
        if (address.contains(getAddressConnectSymbol())) return address;
        else {
            String chainIdPreix = blockchainId + getAddressConnectSymbol();
            return chainIdPreix + address;
        }
    }

    public static String getBlockchainIDFromAddress(String address) {
        if (!address.contains(getAddressConnectSymbol())) return "";
        return address.split(Pattern.quote(getAddressConnectSymbol()))[0];
    }

    // Short address is that full name removes the 'chainId?'
    // if the address has no chainId, we just return; otherwise we remove the chainId
    public static String getShortAddress(String address) {
        if (address.contains(getAddressConnectSymbol())) return address.split(Pattern.quote(getAddressConnectSymbol()))[1];
        else return address;
    }

    public static String getAddressConnectSymbol() {
        return "?";
    }

    public static int getMiningReward() {
        return miningReward;
    }

    public static boolean isExternalAddress(String address) {
        return address.contains(getAddressConnectSymbol()) && !address.startsWith(MiningConfiguration.getBlockchainId() + getAddressConnectSymbol());
    }

    public static boolean isExternalChain(String chainID) {
        return !isInternalChain(chainID);
    }

    public static boolean isInternalChain(String chainID) {
        return chainID.equals(MiningConfiguration.getBlockchainStringId());
    }

    synchronized boolean doesTypeExits(String type) {
        return multiTypeAccountManagerList.containsKey(type);
    }

    synchronized MultiTypeAccountManager getTypedAccountManager(String assetType) {
        String type = assetType;
        if (null == assetType || assetType.isEmpty()) type = defaultType;

        if (!multiTypeAccountManagerList.containsKey(type)) {
            System.out.println("[AccountManager][getTypedAccountManager] try to create " + type + " in " + this + " with typed manager:  " + multiTypeAccountManagerList.keySet());
            multiTypeAccountManagerList.put(type, new MultiTypeAccountManager(type));
        }
        return multiTypeAccountManagerList.get(type);
    }

    public Set<String> getAllAssetTypes() {
        return multiTypeAccountManagerList.keySet();
    }

    public Double getBalance(String address) {
        return getBalance(address, defaultType);
    }

    public Double getBalance(String address, String type) {
        return getTypedAccountManager(type).getBalance(address);
    }

    void DebugOnly() {
        Thread.dumpStack();
    }

    void DebugOnly(String address) {
        System.out.println(address + " with balance " + getBalance(address));
        Thread.dumpStack();
    }

    void DebugOnly(String address, Double value, String op) {
        System.out.println(address + " with balance " + getBalance(address) + " with op: " + op + " with value: " + value);
        Thread.dumpStack();
    }

    public void reset() {
        //DebugOnly();
        System.out.println("[AccountManager][reset] try to create reset");

        multiTypeAccountManagerList = new HashMap<>();
    }

    public synchronized void reset(List<Block> blockList) {
        //DebugOnly();
        HashMap newMultiTypeAccountManagerList = new HashMap<>();
        for (Block block : blockList) {
            processBlock(block);
        }
        System.out.println("[AccountManager][reset] try to create reset with list");
        multiTypeAccountManagerList = newMultiTypeAccountManagerList;
        initMulitpleAssetAccountBalance();
    }

    public boolean changeValue(String address, String type, Double value) {
        //DebugOnly(address, value, "changevalue");
        return getTypedAccountManager(type).changeValue(address, value);
    }

    public boolean addValue(String address, int value) {
        return addValue(address, defaultType, value);
    }

    public boolean addValue(String address, String type, int value) {
        //DebugOnly(address, (double) value, "addValue");
        return getTypedAccountManager(type).addValue(address, Double.valueOf(value));
    }

    public boolean addValue(String address, Double value) {
        return addValue(address, defaultType, value);
    }

    public boolean addValue(String address, String type, Double value) {
        //DebugOnly(address, value, "addValue");
        return getTypedAccountManager(type).addValue(address, value);
    }

    public boolean subValue(String address, int value) {
        return subValue(address, defaultType, value);
    }

    public boolean freezeValue(String address, int value) {
        return freezeValue(address, defaultType, value);
    }

    public Double getFreezeValue(String address) {
        //DebugOnly(address, (double) value, "subValue");
        return getFreezeValue(address, defaultType);
    }

    public boolean unFreezeValue(String address, int value) {
        return unFreezeValue(address, defaultType, value);
    }

    public boolean subValue(String address, String type, int value) {
        //DebugOnly(address, (double) value, "subValue");
        return getTypedAccountManager(type).subValue(address, Double.valueOf(value));
    }

    public boolean freezeValue(String address, String type, int value) {
        //DebugOnly(address, (double) value, "subValue");
        if (0 == value) return true;
        return getTypedAccountManager(type).freezeValue(address, Double.valueOf(value));
    }

    public Double getFreezeValue(String address, String type) {
        //DebugOnly(address, (double) value, "subValue");
        return getTypedAccountManager(type).getFreezeValue(address);
    }

    public boolean unFreezeValue(String address, String type, int value) {
        //DebugOnly(address, (double) value, "subValue");
        return getTypedAccountManager(type).unFreezeValue(address, Double.valueOf(value));
    }

    public boolean subValue(String address, Double value) {
        return subValue(address, defaultType, value);
    }

    public boolean subValue(String address, String type, Double value) {
        //DebugOnly(address, value, "subValue");
        return getTypedAccountManager(type).subValue(address, value);
    }

    public boolean canSubValue(String address, double value) {
        return canSubValue(address, defaultType, value);
    }

    public boolean canSubValue(String address, String type, double value) {
        return getTypedAccountManager(type).canSubValue(address, value);
    }

    public boolean canTransferValue(String address, double value) {
        return canTransferValue(address, defaultType, value);
    }

    public boolean canTransferValue(String address, String type, double value) {
        return getTypedAccountManager(type).canSubValue(address, value);
    }

    public boolean transferValue(String from, String to, int value) {
        //DebugOnly(from, (double) value, "transferValue to " + to);

        return transferValue(from, to, defaultType, value);
    }

    public boolean transferValue(String from, String to, String type, int value) {
        return getTypedAccountManager(type).transferValue(from, to, Double.valueOf(value));
    }

    public boolean transferValue(String from, String to, Double value) {
        //DebugOnly(from, value, "transferValue to " + to);

        return transferValue(from, to, defaultType, value);
    }

    public boolean transferValue(String from, String to, String type, Double value) {
        //DebugOnly(from, value, "transferValue to " + to);

        return getTypedAccountManager(type).transferValue(from, to, value);
    }

    // If can transfer, return true and the value is subtracted
    public boolean canProcessTransaction(Transaction t) {
        if (isExternalAddress(t.getFrom())) return true;
        if (t instanceof AssetCreateTransaction) return true;

        String from = t.getFrom();
        String to = t.getTo();
        if (from.isEmpty() || to.isEmpty()) {
            logger.warn("[AccountManager] No from or to address in one transaction, assume it is not an exchange transaction");
            return true;
        }
        int value = t.getValue();
        String type = t.getOutgoingAssetType();
        return canTransferValue(from, type, value);
    }

    public void processBlock(Block block) {
        String miner = block.getMiner();
        if (null != miner && !miner.isEmpty() && !isExternalAddress(miner)) {
            //TO DO the reward is not consider the branch condition, consider this in future
            addValue(block.getMiner(), Double.valueOf(miningReward)); // the award is default type asset
        }

        for (AbstractTransaction t : block.getTransactions()) {
            // Immediately transfer only for the normal transaction and CTx will in other way
//            if (!(t instanceof ConditionalAssociationTransaction) &&!t.getFrom().isEmpty() && !t.getTo().isEmpty()){
//                transferValue(t.getFrom(), t.getTo(), t.getOutgoingAssetType(), Double.valueOf(t.getValue()));
//            }
            // Only process legacy transaction
            if (t instanceof Transaction) processTransactionBalance((Transaction) t);
        }
    }

    public void processTransactionBalance(AbstractTransaction at) {
        if (!(at instanceof Transaction)) return;

        Transaction t = (Transaction) at;
        if (t instanceof AssetCreateTransaction) {
            processAssetCreate(t);
        } else if (!isDelayPaidTransaction(t)) {
            if (!t.getFrom().isEmpty() && !t.getTo().isEmpty()) {
                transferValue(t.getFrom(), t.getTo(), t.getOutgoingAssetType(), Double.valueOf(t.getValue()));
            } else logger.error("[AccountManager][ERROR] No sender or receiver address in the transaction");

        } else {
            if (!isExternalAddress(t.getFrom())) {
                logger.info("[AccountManager] Skip process the asset. The asset will be frozen later in its processor");
            }
        }
    }

    private void processAssetCreate(Transaction t) {
        System.out.println("[AccountManager] process create asset transaction");
        AccountManager accountManager = AccountManager.getInstance();
        if (accountManager.doesTypeExits(t.getAssetType())) {
            System.out.printf("[AccountManager][ERROR] MultiTypeAsset type %s already exists/n", t.getAssetType());
            return;
        }
        accountManager.addValue(t.getTo(), t.getAssetType(), t.getValue());
        System.out.printf("[AccountManager][Info] Account %s is now with %d %s/n", t.getTo(), t.getValue(),
                accountManager.getBalance(t.getTo(), t.getAssetType()));
    }

    public void callbackCTx(AbstractTransaction at) {
        if (!(at instanceof Transaction)) return;

        Transaction t = (Transaction) at;
        if (!isDelayPaidTransaction(t)) {
            if (!t.getFrom().isEmpty() && !t.getTo().isEmpty()) {
                transferValue(t.getTo(), t.getFrom(), t.getOutgoingAssetType(), Double.valueOf(t.getValue()));
            } else logger.error("[AccountManager][ERROR] No sender or receiver address in the transaction");

        } else {
            if (!isExternalAddress(t.getFrom())) {
                logger.info("[AccountManager] Skip transaction the asset. The asset will be frozen later");
            }
        }
    }

    public boolean isDelayPaidTransaction(Transaction t) {
        return t instanceof ConditionalAssociationTransaction || t instanceof CrosschainInterface || t instanceof MultiTypeExchangeTransaction;

    }

    // For test or onitor purpose
    public String dump() {
        String d = "";
        Iterator it = multiTypeAccountManagerList.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            d += ((MultiTypeAccountManager) entry.getValue()).dump();
            d += "\n";
        }
        return d;
    }

    // For init multiple asset
    private void initMulitpleAssetAccountBalance() {
        final int initTestValue = 100;

        final int totalInitCountNumber =16;// 789001, 789002 and so on
        final String userPrefix = "78900";
        System.out.printf("[AccountManager][INFO] Add balance for users (with address from %s1 to %s%d) to test. This can ignore the requirement of the mining process\n"
                , userPrefix, userPrefix, totalInitCountNumber);
        for (int i = 1 ; i <= totalInitCountNumber; ++i ){
            addValue(userPrefix + i, initTestValue + i);
        }

        System.out.printf("[AccountManager][INFO] Add typed balance for users (with address from %s1 to %s%d) to test. This can ignore the requirement of the mining process\n"
                , userPrefix, userPrefix, totalInitCountNumber);
        final String assetTypePrefix = "assetType";
        for (int i = 1 ; i <= totalInitCountNumber; ++i ){
            addValue(userPrefix + i, assetTypePrefix + i, initTestValue + i);
        }
    }
}

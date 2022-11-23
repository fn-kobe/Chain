package consensus.pow;

import org.apache.log4j.Logger;
import util.AddressPort;
import util.FileLogger;
import util.IniFileHelper;
import util.StringHelper;

import java.net.SocketException;

public class MiningConfigurationHandler {
    static final int defaultDifficulty = 3;
    static final int defaultAllowedNumber = 0;// 0 is not limited
    static final String defaultMiningAccount = "0xabcd";
    static final String defaultConsensusType = "PoS";
    static final String defaultNodeId = "defaultNodeId";
    // difficulty section
    static final String difficultySection = "difficulty";
    static final String difficultyKey = "zero_count";
    static final String allowedNumberKey = "allowed_number";
    static final String blockchainSection = "blockchain";
    static final String blockchainIdKey = "blockchainId";
    static final String genesisNouceKey = "genesis_nouce";
    static final String blockchainSyncIntervalKey = "blockchain_sync_interval";
    static final int defaultBlockchainSyncInterval = 1000;// one seocond
    // mining section
    static final String miningSection = "mining";
    static final String account = "account";
    static final String consensus = "consensus";
    static final String nodeId = "nodeId";
    static final int defaultBlockchainId = 123;
    static final String networkSection = "network";
    static final String self_address_port = "self_address_port";
    static Logger logger = FileLogger.getLogger();
    static String miningAccount = defaultMiningAccount;
    static String consensusType = defaultConsensusType;
    static String nodeIdValue = defaultNodeId;
    // 1289986143, 1417598154, 1149023650, -210709181, -1740289998 and -1108112160 can also be used (-1622010549 is for 6 zeros)
    static int defaultGenesisNouce = 220455692;
    static AddressPort addressPort = null;
    int difficulty = -1;
    int allowedNumber = -1;
    int genesisNouce = -1;
    String configFileName;
    int blockchainSyncInterval = -1;
    int blockchainId = -1;

    MiningConfigurationHandler(String configFileName) {
        this.configFileName = configFileName;
    }

    // TO DO calculate the difficulty dynamically due to time
    public int getRequiredZeroCount() {
        if (-1 != difficulty) return difficulty;

        IniFileHelper iniFileHelper = new IniFileHelper();
        try {
            int configureDifficulty = Integer.parseInt(iniFileHelper.getValue(configFileName, difficultySection, difficultyKey));
            if (0 != configureDifficulty) difficulty = configureDifficulty;
        } catch (NumberFormatException e) {
            logger.warn("[MiningConfiguration] hash zero_count configuration is wrong or not configured, use default value " + defaultDifficulty);
            difficulty = defaultDifficulty;
        }
        logger.info("[MiningConfiguration] hash zero_count configuration is " + difficulty);
        return difficulty;
    }

    // It is used to reduce the difficulty of mining. Such as from 6 zeros to 7 zeros will cause a lot of time
    // Then we allow some allowed number here. For example: zero = 5, allow = 3. Then 000 00 0, 000 00 1, 000 00 2 are OK
    public int getRequiredAllowedCount() {
        if (-1 != allowedNumber) return allowedNumber;

        IniFileHelper iniFileHelper = new IniFileHelper();
        try {
            int configureAllowedNumber = Integer.parseInt(iniFileHelper.getValue(configFileName, difficultySection, allowedNumberKey));
            if (0 != configureAllowedNumber) allowedNumber = configureAllowedNumber;
        } catch (NumberFormatException e) {
            logger.warn("[MiningConfiguration] Allowed number configuration is wrong or not configured, use default value " + defaultAllowedNumber);
            allowedNumber = defaultAllowedNumber;
        }
        if (allowedNumber < 0) {
            logger.warn("[MiningConfiguration] Allowed number configuration is less than 0, use default value " + defaultAllowedNumber);
            allowedNumber = defaultAllowedNumber;
        }
        return allowedNumber;
    }

    public int getBlockchainId() {
        if (-1 != blockchainId) return blockchainId;

        IniFileHelper iniFileHelper = new IniFileHelper();
        try {
            int configureBlockchainId = Integer.parseInt(iniFileHelper.getValue(configFileName, blockchainSection, blockchainIdKey));
            if (0 != configureBlockchainId) blockchainId = configureBlockchainId;
        } catch (NumberFormatException e) {
            logger.warn("[MiningConfiguration] blockchainId configuration is wrong or not configured, use default value " + defaultBlockchainId);
            blockchainId = defaultBlockchainId;
        }
        return blockchainId;
    }

    public int getDefaultBlockchainSyncInterval() {
        if (-1 != blockchainSyncInterval) return blockchainSyncInterval;

        IniFileHelper iniFileHelper = new IniFileHelper();
        try {
            int configureBlockchainSyncInterval = Integer.parseInt(iniFileHelper.getValue(configFileName, blockchainSection, blockchainSyncIntervalKey));
            if (0 != configureBlockchainSyncInterval) blockchainSyncInterval = configureBlockchainSyncInterval;
        } catch (NumberFormatException e) {
            logger.warn("[MiningConfiguration] blockchain sync interval configuration is wrong or not configured, use default value " + defaultBlockchainSyncInterval);
            blockchainSyncInterval = defaultBlockchainSyncInterval;
        }
        return blockchainSyncInterval;
    }

    public int getGenesisNouce() {
        if (-1 != genesisNouce) return genesisNouce;

        IniFileHelper iniFileHelper = new IniFileHelper();
        try {
            int configureGenesisNounce = Integer.parseInt(iniFileHelper.getValue(configFileName, blockchainSection, genesisNouceKey));
            if (0 != configureGenesisNounce) genesisNouce = configureGenesisNounce;
        } catch (NumberFormatException e) {
            logger.warn("[MiningConfiguration] genesis configuration is wrong or not configured, use default value " + defaultGenesisNouce);
            genesisNouce = defaultGenesisNouce;
        }
        return genesisNouce;
    }

    // TO DO calculate the difficulty dynamically due to time
    public String getMiningAccount() {
        IniFileHelper iniFileHelper = new IniFileHelper();
        String configMiningAccount = iniFileHelper.getValue(configFileName, miningSection, account);
        if (configMiningAccount.isEmpty()) {
            logger.warn("[MiningConfiguration] Mining configuration is wrong or not configured, use default account " + defaultMiningAccount);
            miningAccount = defaultMiningAccount;
        } else {
            logger.info("[MiningConfiguration] Got ming account " + configMiningAccount);
            miningAccount = configMiningAccount;
        }
        return miningAccount;
    }

    public String getConsensusType() {
        IniFileHelper iniFileHelper = new IniFileHelper();
        String configConsensusType = iniFileHelper.getValue(configFileName, miningSection, consensus);
        if (configConsensusType.isEmpty()) {
            logger.warn("[MiningConfiguration] Consensus configuration is wrong or not configured, use default consensus " + defaultConsensusType);
            consensusType = defaultConsensusType;
        } else {
            logger.info("[MiningConfiguration] Got consensus type " + configConsensusType);
            consensusType = configConsensusType;
        }
        return consensusType;
    }


    public String getNodeId() {
        IniFileHelper iniFileHelper = new IniFileHelper();
        String configNodeId = iniFileHelper.getValue(configFileName, miningSection, nodeId);
        if (configNodeId.isEmpty()) {
            logger.warn("[MiningConfiguration] Node configuration is wrong or not configured, use default node id " + defaultNodeId);
            nodeIdValue = defaultNodeId;
        } else {
            logger.info("[MiningConfiguration] Got node id " + configNodeId);
            nodeIdValue = configNodeId;
        }
        return nodeIdValue;
    }

    public static void testSetNodeIdValue(String nodeIdValue) {
        MiningConfigurationHandler.nodeIdValue = nodeIdValue;
    }

    public boolean isHashMatched(String hashValue) {
        return isHashMatched(getRequiredZeroCount(), getRequiredAllowedCount(), hashValue);
    }

    static public boolean isHashMatched(int requireZeroCount, int requiredAllowedCount, String hashValue) {
        for (int i = 0; i < requireZeroCount; ++i) {
            if ('0' != hashValue.charAt(i)) {
                return false;
            }
        }

        // Not configures allowed number, just return
        if (0 == requiredAllowedCount) return true;

        // Else do the allowed configuration
        if (hashValue.charAt(requireZeroCount) < '0' + requiredAllowedCount) return true;
        else return false;
    }

    // TO DO calculate the difficulty dynamically due to time
    public Integer getSelfListenPort() {
        if (null == addressPort || !addressPort.isValid()) loadAddressPort();

        return addressPort.getPort();
    }

    AddressPort loadAddressPort(){
        IniFileHelper iniFileHelper = new IniFileHelper();
        String selfAddressPort = iniFileHelper.getValue(configFileName, networkSection, self_address_port);
        addressPort = StringHelper.getAddressPort(selfAddressPort);
        if (!addressPort.isValid()) {
            addressPort.setAddressPort("127.0.0.1", StringHelper.getDefaultPort());
            logger.warn("[MiningConfiguration] Address and port configuration is wrong or not configured, use default " +
                    addressPort.getAddress() + ":" + addressPort.getPort());
        } else {
            logger.info("[MiningConfiguration] Got address and port " + addressPort.getAddress() + ":" + addressPort.getPort());
        }
        return addressPort;
    }

    public synchronized String getHostIP() {
        if (null == addressPort || !addressPort.isValid()) loadAddressPort();
        return addressPort.getAddress();
    }

    public boolean isSelf(AddressPort another) {
        if (null == addressPort || !addressPort.isValid()) loadAddressPort();

        return addressPort.isTheSame(another);
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    // Seconds, need to be configurable
    public int getMaxTryTime() {
        return 20000000;
    }

    //only for test
    public void testSetRequiredZeroCount(int newDifficulty) {
        difficulty = newDifficulty;
    }

    public void testSetAllowedNumber(int newAllowedNumber) {
        allowedNumber = newAllowedNumber;
    }

    public void testSetChainId(int newID) {
        blockchainId = newID;
    }
}

package Service;

import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.dynamic_definition.AssetCompiler;
import com.scu.suhong.dynamic_definition.DynamicalAsset;
import com.scu.suhong.miner.Miner;
import com.scu.suhong.network.NetworkService;
import com.scu.suhong.smartcontract.nondeterminacy.StateSyncManager;
import com.scu.suhong.smartcontract.nondeterminacy.embedded.NBSState;
import com.scu.suhong.transaction.*;
import com.scu.suhong.transaction.ActionCondition.*;
import com.scu.suhong.transaction.multipleTypeExchange.MultiTypeExchangeTransaction;
import com.scu.suhong.transaction.multipleTypeExchange.RequiredData;
import com.scu.suhong.transaction.multipleTypeExchange.ShellExternalCondition;
import com.scu.suhong.transaction.multipleTypeExchange.WaitExternalCondition;
import consensus.pow.MiningConfiguration;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.FileLogger;
import util.RandomHelper;
import util.TimeHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class BlockchainService implements BlockchainInterface {
    static Logger logger = FileLogger.getLogger();

    private static BlockchainService blockchainService;
    private Miner miner;
    private NetworkService networkService;
    boolean isStarted;
    private final String dataSeparator = "_";
    private final String conditionFlag = "if";
    private final String conditionTo = "to";
    private final String conditionValue = "value";
    private RandomHelper randomHelper = new RandomHelper(1, Integer.MAX_VALUE);

    private BlockchainService() {
        isStarted = false;
    }

    public static synchronized BlockchainService getInstance() {
        if (null == blockchainService) {
            blockchainService = new BlockchainService();
        }
        return blockchainService;
    }

    public boolean isReady() {
        return null != networkService;
    }

    @Override
    public void startService() {
        System.out.println("[BlockchainService] Begin to start service");
        if (isStarted) {
            System.out.println("[BlockchainService] Service already started");
            return;//already started
        }
        System.out.println("[BlockchainService] Begin to start miner");
        Thread minerThread = startMiner();
        System.out.println("[BlockchainService] Begin to start network service");
        Thread listenerThread = startNetworkService();

        try {
            listenerThread.join();
            minerThread.join();
        } catch (InterruptedException e) {
            System.out.println("[BlockchainService] Exception happened when run blockchain service:\n");
            e.printStackTrace();
        }
    }

    @NotNull
    private Thread startMiner() {
        System.out.println("[BlockchainService] start miner");
        miner = new Miner();
        Thread minerThread = new Thread(miner, "Blockchain service");
        minerThread.start();
        return minerThread;
    }

    // Only for test
    public void testStartMiner() {
        startMiner();
    }

    @NotNull
    private Thread startNetworkService() {
        networkService = new NetworkService(miner);
        Thread listenerThread = new Thread(networkService, "Blockchain socket listener");
        listenerThread.start();
        return listenerThread;
    }

    @Override
    public void triggerTransaction(List<String> arguments) throws IOException {
        triggerTransaction(arguments, false);
    }

    @Override
    public void triggerTransaction(List<String> arguments, boolean isInternalTx) throws IOException {
        // TO DO parse the msg into according field of transaction
        String from = arguments.get(1);
        String transactionData = arguments.get(0);

	//	Transaction t = tryGetDivisionTransaction(transactionData);
	//	if (t == null) t = tryGetAssociationTransaction(from, transactionData);
	//	if (t == null) t = new Transaction();
		Transaction t = new Transaction();

		if (isInternalTx) {
			System.out.println("[BlockchainService][INFO] Try to send internal transaction");
			t.setData(Transaction.getInternalTxPrefix() + (String) arguments.get(0));
		} else {
			t.setData(transactionData);
		}
		t.setFrom(from);
		t.setToAndValue(arguments.get(2), arguments.get(3));
		if (arguments.size() >= 5) {
			final String pairsSeparator = ",";
			final String keyValueSeparator = "#";
			String otherParameters = arguments.get(4);
			if (!otherParameters.contains(keyValueSeparator)) {// old format when not using key value pairs
				String assetType = arguments.get(4);
				t.setAssetType(assetType);
			} else {
				t.setKeyValueParameter(otherParameters);
			}
		}
		t.setId();
		processTransaction(t);
	}

    @Override
    public void triggerCommonCrosschainTransaction(List<String> arguments) throws IOException {
        CommonCrosschainTransaction t = new CommonCrosschainTransaction();
        t.setInteractionId(Integer.parseInt(arguments.get(0)));
        t.setData(arguments.get(1)); // sendcomct interactionId data from to value <asset type>
        t.setFrom(arguments.get(2)); // sendcomct interactionId data from to value <asset type>
        t.setToAndValue(arguments.get(3), arguments.get(4)); // sendcomct interactionId data from to value <asset type>
        String assetType = "";
        if (arguments.size() >= 6) {
            assetType = arguments.get(5);
        }
        t.setAssetType(assetType);
        t.setId();
        processTransaction(t);
    }

    @Override
    public void triggerStateSyncTransaction(List<String> arguments) throws IOException {
        // TO DO parse the msg into according field of transaction
        String smartContractName = arguments.get(0);
        String varietyName = arguments.get(1);
        String value = arguments.get(2);

        triggerStateSyncTransaction(smartContractName, varietyName, value);
    }

    @Override
    // used when this node get state from NBS and then update the whole network
    public void updateNBSState(String smartContractName, String varietyName, String value)  throws IOException {
        triggerStateSyncTransaction(smartContractName, varietyName, value);
    }

    @Override
    public void triggerStateSyncTransaction(String smartContractName, String varietyName, String value)  throws IOException {
        SyncTransaction t = new SyncTransaction(smartContractName, varietyName, value);

        t.setId();
        processTransaction(t);
    }

    @Override
    // sendct from to value or sendct from to1_value1_to2_value2... totalValue
    public void triggerConditionalTransaction(List<String> arguments) throws IOException {
        // TO DO parse the msg into according field of transaction
        ConditionalAssociationTransaction t = createConditionalTransaction(arguments);
        processTransaction(t);
    }

    // interactionId from to value or sendct from to1_value1_to2_value2... totalValue
    private ConditionalAssociationTransaction createConditionalTransaction(List<String> arguments) {
        int interactionId = Integer.parseInt(arguments.get(0));
        Condition c = new Condition(arguments.get(1), arguments.get(2), arguments.get(3));
        ConditionalAssociationTransaction conditionalAssociationTransaction = new ConditionalAssociationTransaction(interactionId, c);
        conditionalAssociationTransaction.setData(String.valueOf(TimeHelper.getEpoch()));
        conditionalAssociationTransaction.setId();
        // Test
        conditionalAssociationTransaction.getHash();
        return conditionalAssociationTransaction;
    }

    @Override
    // from incomingAddress paymentAddress value contractNumber
    public void triggerExternalTransaction(List<String> arguments) throws IOException {
        // TO DO parse the msg into according field of transaction
        CrosschainTransaction t = createExternalTransaction(arguments);
        processTransaction(t);
    }

    //sendex interactionId from to value reqChainId reqFrom reqTo reqValue
    private CrosschainTransaction createExternalTransaction(List<String> arguments) {
        int interactionId = Integer.parseInt(arguments.get(0));
        String from = arguments.get(1);
        String to = arguments.get(2);
        int value = Integer.parseInt(arguments.get(3));
        int reqChainId = Integer.parseInt(arguments.get(4));
        String reqFrom = arguments.get(5);
        String reqTo = arguments.get(6);
        int reqValue = Integer.parseInt(arguments.get(7));
        //int randomId = Integer.parseInt(arguments.get(8));

        CrosschainTransaction requiredTransaction = new CrosschainTransaction(reqChainId, interactionId);
        requiredTransaction.setFrom(reqFrom);
        requiredTransaction.setToAndValue(reqTo, reqValue);
        requiredTransaction.setId();
        //requiredTransaction.setId(randomId);

        CrosschainTransaction crosschainTransaction = new CrosschainTransaction(interactionId);
        crosschainTransaction.setFrom(from);
        crosschainTransaction.setToAndValue(to, value);
        crosschainTransaction.setId();
        crosschainTransaction.addRequiredCrosschainTransaction(requiredTransaction);
        return crosschainTransaction;
    }

    //sendex interactionId from to value reqChainId reqFrom reqTo reqValue
    public void triggerCreateAsset(List<String> arguments) throws IOException {
        System.out.println("\n[BlockchainService] Begin to batch send transaction(for TEST purpose)");
        String assetType = arguments.get(0);// the first parameter should be MultiTypeAsset type
        int amount = Integer.parseInt(arguments.get(1));// the second parameter is amount
        String account = arguments.get(2);// the third parameter is own account
        //int randomId = Integer.parseInt(arguments.get(8));

        AssetCreateTransaction transaction = new AssetCreateTransaction(assetType, amount, account);
        blockchainService.processTransaction(transaction);
    }

    @Override
    // exchangeid <fix|var> from to assetType assetAmount "from:<to>:assetType:assetAmount;from:<to>:assetType:assetAmount;..."
    public void triggerMultipleTypeTransaction(List<String> arguments) throws IOException {
        // TO DO parse the msg into according field of transaction
        MultiTypeExchangeTransaction t = createMultipleTypeTransaction(arguments);
        processTransaction(t);
    }

    @Override
    // sendcmt exchangeid <fixed|variable> from to assetType assetAmount "requiredChainID:from:<to>:assetType:assetAmount;requiredChainID:from:<to>:assetType:assetAmount;..." data
    public void triggerCrosschainMultipleTypeTransaction(List<String> arguments) throws IOException {
        // TO DO parse the msg into according field of transaction
        CrosschainMultiTypeExchangeTransaction t = createCrosschainMultipleTypeTransaction(arguments);
        processTransaction(t);
    }

    @Override
    // sendacptrigger incomingName maxWaitingTime totalSuccessiveActionNumber maxAllowedActionNumber data incomingOwner
    public void triggerSendACPTriggerTransaction(List<String> arguments) throws IOException {
        // TO DO parse the msg into according field of transaction
        ACPTriggerTransaction t = createACPConditionTransaction(arguments);
        processTransaction(t);
    }

    // sendacptrigger incomingName totalSuccessiveActionNumber maxAllowedActionNumber maxWaitingTime data sender
    private ACPTriggerTransaction createACPConditionTransaction(List<String> arguments) {
        if (arguments.size() >= 6) {
            System.out.println("[CMD] Not enough parameters for send action condition transaction");
        }
        String incomingName = arguments.get(0);
        int maxWaitingTime = Integer.parseInt(arguments.get(1));
        int totalSuccessiveActionNumber = Integer.parseInt(arguments.get(2));
        int maxAllowedActionNumber = Integer.parseInt(arguments.get(3));
        String data = arguments.get(4);
        String sender = arguments.get(5);

        ACPTriggerTransaction t = new ACPTriggerTransaction();
        ACPCondition c = new ACPCondition();
        c.setName(incomingName);
        c.setMaxAllowedActionNumber(maxAllowedActionNumber);
        c.setTotalSuccessiveActionNumber(totalSuccessiveActionNumber);
        c.setMaxWaitingTime(maxWaitingTime);
        t.setCondition(c);
        t.setBlockchainID(MiningConfiguration.getBlockchainStringId());
        t.setOwner(sender);
        t.setData(data);
        t.generateSelectedNumberIfRandomTrigger();
        t.setId();

        return t;
    }

    @Override
    // sendacptrigger sender incomingName totalSuccessiveActionNumber maxAllowedActionNumber maxWaitingTime data incomingOwner
    public void triggerSendACPActionSettingTransaction(List<String> arguments) throws IOException {
        // TO DO parse the msg into according field of transaction
        ACPSettingTransaction t = createACPActionSettingTransaction(arguments);
        processTransaction(t);
    }

    // setacp sender actionType, incomingName ,incomingOwner, incomingBlockchainId, outgoingName, successiveActionMaxWaitingTime,
    // successiveActionTotalPeerNumber successiveActionMaxAllowedPeerNumber
    private ACPSettingTransaction createACPActionSettingTransaction(List<String> arguments) {
        if (arguments.size() < 9) {
            System.out.println("[CMD] Not enough parameters for setting actioin conditioin pair. Format :" +
                    "sender, actionType, incomingName ,incomingOwner, incomingBlockchainId, outgoingName, successiveActionMaxWaitingTime," +
                    " successiveActionTotalPeerNumber successiveActionMaxAllowedPeerNumber");
            return null;
        }

        int i = 0;
        String sender = replaceNullToEmpty(arguments.get(i++));
        String actionType = replaceNullToEmpty(arguments.get(i++));
        String incomingName = replaceNullToEmpty(arguments.get(i++));
        String incomingOwner = replaceNullToEmpty(arguments.get(i++));
        String incomingBlockchainId = replaceNullToEmpty(arguments.get(i++));
        String outgoingName = replaceNullToEmpty(arguments.get(i++));
        String successiveActionMaxWaitingTime = replaceNullToEmpty(arguments.get(i++));
        String successiveActionTotalPeerNumber = replaceNullToEmpty(arguments.get(i++));
        String successiveActionMaxAllowedPeerNumber = replaceNullToEmpty(arguments.get(i++));

        ACPSettingTransaction t = new ACPSettingTransaction();
        t.setOwner(sender);
        t.setActionType(actionType);
        t.setIncomingConditionName(incomingName);
        t.setIncomingConditionNameSender(incomingOwner);
        t.setIncomingConditionBlockchainID(incomingBlockchainId);
        t.setOutgoingConditionName(outgoingName);
        t.setSuccessiveActionMaxWaitingTime(successiveActionMaxWaitingTime);
        t.setSuccessiveActionTotalPeerNumber(successiveActionTotalPeerNumber);
        t.setSuccessiveActionMaxAllowedPeerNumber(successiveActionMaxAllowedPeerNumber);
        t.setId();

        return t;
    }

    static String replaceNullToEmpty(String original){
        if (null == original || original.isEmpty() || original.toLowerCase().equals("null")){
            return "";
        }
        return original;
    }

    @Override
    // senddt owner gas classname key:value;key:value "data" "code"
    // 0 is to define an asset, 1 is to define and init an asset, 2 is to init an asset
    public void triggerDynamicalDefinitionTransaction(List<String> arguments) throws IOException {
        // TO DO parse the msg into according field of transaction
        DynamicalAsset t = createDynamicalTransaction(arguments, 0);
        if (null == t){
            System.out.println("\n[BlockchainService][ERROR] Cannot create dynamical definition transaction");
            return;
        }
        processTransaction(t);
    }

    @Override
    // senddoit owner gas classname key:value;key:value "data" "code"
    public void triggerOverrideAndInitTransaction(List<String> arguments) throws IOException {
        // TO DO parse the msg into according field of transaction
        DynamicalAsset t = createDynamicalTransaction(arguments, 1);
        if (null == t){
            System.out.println("\n[BlockchainService][ERROR] Cannot create dynamical definition and init transaction");
            return;
        }
        processTransaction(t);
    }

    @Override
    // senddit owner gas classname key:value;key:value "data"
    public void triggerDynamicalInitTransaction(List<String> arguments) throws IOException {
        // TO DO parse the msg into according field of transaction
        DynamicalAsset t = createDynamicalTransaction(arguments, 2);
        if (null == t){
            System.out.println("\n[BlockchainService][ERROR] Cannot create dynamical init transaction");
            return;
        }
        processTransaction(t);
    }

    @Override
    public void triggerInternalSmartContract(List<String> arguments) throws IOException{
        String smartContractName = arguments.get(0);
        String parameters = arguments.get(1);
        if (smartContractName.equals("NBSState")){
            String[] parameterArray = parameters.split("#");
            if (parameterArray.length < 2){
                System.out.println("[BlockchainService][ERROR] Parameters to trigger internal command NBSSate are not enough");
                return;
            }
            String NBSIp = parameterArray[0];
            String runTimeString = parameterArray[1];
            boolean history = false;
            int historyResetTime = 0;
            if (parameterArray.length > 2 ) {
                String parameter = parameterArray[2];
                if (parameter.contains("h")) history = true;
                if (parameter.contains("m")){
                    historyResetTime = Integer.parseInt(parameterArray[3]);
                }
                if (parameter.contains("r")) {
                    NBSState.setRandomDiscard(true);
                    if (parameterArray.length > 3) NBSState.setRandomDiscardNumber(Integer.parseInt(parameterArray[3]));
                }
            }
            if (history){
                NBSState.start(NBSIp, runTimeString);
            } else if (historyResetTime > 0) {
                NBSState.startMixMode(NBSIp, runTimeString, historyResetTime);
            } else {
                NBSState.startWithoutHistory(NBSIp, runTimeString);
            }
        } else {// add internal smart contract if want
            System.out.println("[BlockchainService][ERROR] Command is not supported now " + smartContractName);
        }
    }

    //  senddoit/senddt/senddit owner gas classname key:value;key:value <"data"> <"code">
    DynamicalAsset createDynamicalTransaction(List<String> arguments, int lifestyle) {
        // TO DO add the gas reduction logics here. We ingore it here and assume each one has
        String owner = arguments.get(0);
        int gas = Integer.parseInt(arguments.get(1));
        String className = arguments.get(2);
        String oldClassName = className;
        int randomId = randomHelper.getNumber();

        String keyValueList = arguments.get(3);

        String data = "";
        if (arguments.size() > 4) {
            data = arguments.get(4);
        }

        String code = "";
        if (arguments.size() > 5) {// we allow the code to be empty
            for (int i = 5; i < arguments.size(); ++i) {
                code += arguments.get(i) + " ";
            }
            code  = code.replaceAll(AssetCompiler.getLinechangerReplacement(),"\n");
        }

        // 0 is to define an asset, 1 is to define and init an asset, 2 is to init an asset
        if (1 == lifestyle){// only for override and init case to replace class name to a temporary one
            className += randomId;
            code = code.replace(oldClassName, className);
        }

        DynamicalAsset dynamicalAsset = new DynamicalAsset();
        dynamicalAsset.setOwner(owner);
        dynamicalAsset.setGas(gas);
        dynamicalAsset.setSpecifiedDerivedClassName(className);
        dynamicalAsset.setData(data);
        dynamicalAsset.setCode(code);
        dynamicalAsset.setLifecyleType(lifestyle);
        String[] keyValueArray = keyValueList.split(";");
        for (int i = 0; i < keyValueArray.length; ++i){
            String [] keyValue = keyValueArray[i].split(":");
            if (2 == keyValue.length) dynamicalAsset.addKeyValue(keyValue[0], keyValue[1]);
            else{
                System.out.printf("[BlockchainService][WARN] key value parameter error\n", keyValueArray[i]);
            }
        }

        dynamicalAsset.setId(randomId);
        return dynamicalAsset;
    }

    //// sendmt exchangeid <fix|var> from to assetType assetAmount "from:<to>:assetType:assetAmount;from:<to>:assetType:assetAmount;..."
    MultiTypeExchangeTransaction createMultipleTypeTransaction(List<String> arguments) {
        int exchangeId = Integer.parseInt(arguments.get(0));
        String requiredTxtype = arguments.get(1);
        String from = arguments.get(2);
        String to = arguments.get(3);
        String assetType = arguments.get(4);
        int value = Integer.parseInt(arguments.get(5));

        MultiTypeExchangeTransaction requiredTransaction = new MultiTypeExchangeTransaction(exchangeId, requiredTxtype);
        requiredTransaction.setFrom(from);
        requiredTransaction.setAssetType(assetType);
        requiredTransaction.setToAndValue(to, value);
        requiredTransaction.setId();

        String embeddedLogic =arguments.get(6);

        // parse the required data
        if (null != embeddedLogic && !embeddedLogic.isEmpty()){
            String[] embeddedLogicItemList = embeddedLogic.split(";");
            String requiredTxChainID = String.valueOf(MiningConfiguration.getBlockchainId());
            for (int i = 0; i < embeddedLogicItemList.length; ++i){
                if (!tryProcessExternalCondition(requiredTransaction, embeddedLogicItemList[i])) {
                    tryProcessRequiredData(exchangeId, requiredTransaction, embeddedLogicItemList[i]);
                }
            }
        }
        return requiredTransaction;
    }

    // sendcmt exchangeid <fixed|variable> from to assetType assetAmount "requiredChainID:from:<to>:assetType:assetAmount:reqData;requiredChainID:from:<to>:assetType:assetAmount:reqData;..." data
    CrosschainMultiTypeExchangeTransaction createCrosschainMultipleTypeTransaction(List<String> arguments) {
        int exchangeId = Integer.parseInt(arguments.get(0));
        String requiredTxtype = arguments.get(1);
        String from = arguments.get(2);
        String to = arguments.get(3);
        String assetType = arguments.get(4);
        int value = Integer.parseInt(arguments.get(5));
        String chainID = String.valueOf(MiningConfiguration.getBlockchainId());

        CrosschainMultiTypeExchangeTransaction t = new CrosschainMultiTypeExchangeTransaction(exchangeId, chainID, requiredTxtype);
        t.setFrom(from);
        t.setAssetType(assetType);
        t.setToAndValue(to, value);
        t.setId();

        String embeddedLogic =arguments.get(6);

        // parse the required data
        if (null != embeddedLogic && !embeddedLogic.isEmpty()){
            String[] embeddedLogicItemList = embeddedLogic.split(";");
            for (int i = 0; i < embeddedLogicItemList.length; ++i){
                if (!tryProcessExternalCondition(t, embeddedLogicItemList[i])) {
                    tryProcessRequiredData(exchangeId, t, embeddedLogicItemList[i]);
                }
            }
        }
        String data = "";
        if (arguments.size() > 7) {
            data = arguments.get(7);
        }
        t.setData(data);
        return t;
    }

    private void tryProcessRequiredData(int exchangeId, MultiTypeExchangeTransaction requiredTransaction, String embeddedLogicItem) {
        String[] requiredData = embeddedLogicItem.split(":"); // requiredChainID:from:<to>:assetType:assetAmount;..."
        if (requiredData.length < 4) return;
        if (4 == requiredData.length){
            requiredTransaction.addRequiredData(new RequiredData(exchangeId, MiningConfiguration.getBlockchainStringId(),processAnyFromAddress(requiredData[0]),
                    processThisToAddress(requiredData[1]), requiredData[2], Integer.parseInt(requiredData[3])));
        } else {
            // int exchangeId, String requiredData, String from, String to, String assetType, int value
            requiredTransaction.addRequiredData(new RequiredData(exchangeId, processAnyFromAddress(requiredData[0]), processAnyFromAddress(requiredData[1]),
                    processThisToAddress(requiredData[2]), requiredData[3], Integer.parseInt(requiredData[4])));
        }
    }

    //wait:<number>;shell <command result oppositeResult>
    boolean tryProcessExternalCondition(MultiTypeExchangeTransaction transaction, String embeddedLogicItem){
        final String waitFlag = "wait";
        final String shellFlag = "shell";
        String[] requiredData = embeddedLogicItem.split(":");
        if (embeddedLogicItem.startsWith(waitFlag)){
            if (requiredData.length != 2){
                System.out.println("[BlockchainService][tryProcessExternalCondition][ERROR] Shell command error. wait number");
            } else {
                WaitExternalCondition condition = new WaitExternalCondition(Integer.parseInt(requiredData[1]));
                transaction.setCondition(condition);
            }
        } else if (embeddedLogicItem.startsWith(shellFlag)){
            if (requiredData.length != 4){
                System.out.println("[BlockchainService][tryProcessExternalCondition][ERROR] Shell command error. shell <command result oppositeResult>");
            } else {
                ShellExternalCondition condition = new ShellExternalCondition(requiredData[1], requiredData[2], requiredData[3]);
                transaction.setCondition(condition);
            }
        } else {
            return false;
        }
        return true;
    }

    String processAnyFromAddress(String original){
        if (original.equals("any")) return "";
        return original;
    }

    String processThisToAddress(String original){
        if (original.equals("this")) return "";// we just return empty and will be added when setting
        return original;
    }

    @Nullable
    private Transaction tryGetAssociationTransaction(String from, String transactionData) {
        int interactionId = 0;
        System.out.println("[BlockchainService][ERROR] tryGetAssociationTransaction manually set interactionId in this way");
        System.out.println("[BlockchainService][ERROR] Please send ConditionalAssociationTransaction from other way");
        Transaction t;
        HashMap<String, String> conditionalParameters = tryGetConditionalParameters(transactionData);
        if (!conditionalParameters.containsKey(conditionTo) || !conditionalParameters.containsKey(conditionValue)) {
            logger.error("[BlockchainService] Conditional transaction miss some parameters in the data field: " + transactionData);
            return null;
        }
        int value = 0;
        try {
            value = Integer.parseInt(conditionalParameters.get(conditionValue));
        } catch (NumberFormatException e) {
            logger.error("[BlockchainService] Value is not number in data field of the conditional transaction");
            return null;
        }
        Condition condition = new Condition(from, conditionalParameters.get(conditionTo), value);
        t = new ConditionalAssociationTransaction(interactionId, condition);
        return t;
    }

    HashMap<String, String> tryGetConditionalParameters(String transactionData) {
        String[] dataList = transactionData.split(dataSeparator);
        HashMap<String, String> commandPairs = new HashMap<>();
        //assume the format is if key value key value -> 1 == dataList.length % 2
        if (dataList.length > 1 && dataList[0].equals(conditionFlag) && 1 == dataList.length % 2) {
            for (int i = 1; i < dataList.length; ) {
                commandPairs.put(dataList[i++], dataList[i++]);
            }
        }
        return commandPairs;
    }

    Transaction tryGetDivisionTransaction(String transactionData) {
        Transaction transaction = null;
        if (null == (transaction = ConditionalDivisionTransaction.construct(transactionData))) {
            transaction = DivisionCondition.construct(transactionData);
        }
        return transaction;
    }

    public void processTransaction(AbstractTransaction t) throws IOException {
        if (t instanceof SyncTransaction) {
            SyncTransaction syncTransaction = (SyncTransaction) t;
            StateSyncManager.syncLocalState(syncTransaction.getSmartContractName()
                    , syncTransaction.getVarietyName(), syncTransaction.getValue());
        } else {
            miner.addTransaction(t);// also send the transaction to ourselves
        }
        // iInternal Tx is sent out, as it is used in each smart contract
        // Or each node sends out an internal tx
        if (!t.isInternalTx()) {
            networkService.sendTransaction(t);
        }
    }

    @Override
    public void stopService() {
        networkService.setForceStop();
        miner.setForceStop();
    }

    public int getLastProcessedTransactionSpeed() {
        return miner.getLastProcessedTransactionSpeed();
    }

    public int getMinerRunInterval() {
        return miner.getRunInterval();
    }

    // Just for test some time the miner thread is not started
    public Miner getMiner() {
        return miner;
    }

    // Used for paper test
    public int getTransactionListCount() {
        return miner.getTransactionListCount();
    }

    public NetworkService testGetNetworkService(){
        return networkService;
    }
}

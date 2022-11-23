package com.scu.suhong.transaction.ActionCondition;

import Service.BlockchainService;
import consensus.pow.MiningConfiguration;
import util.RandomHelper;
import util.ThreadHelper;
import util.TimeHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Condition -> Action -> Condition
public class ACPAction implements ACPAbstractAction {
    List<ACPTriggerTransaction> conditionTransactionList;
    List<ACPTokenTransaction> tokenTransactionList;
    ACPTokenTransaction[] sortedTokenTransactionList;
    // We don't want to use the lock and use version, which may cause run more times while it onl waste some time.
    int processConditionVersion;
    int latestConditionVersion;
    ACPActionThread actionRunnable;
    Thread actionThread;
    boolean doesConditionCome;

    // Only token state
    ACPTokenState acpTokenState;
    // State include condition and token state
    ACPConditionState conditionState;

    // Incoming condition which this action should match to run the action
    // Outgoing condition is sent to trigger the successive actions of this action, which is created when required
    String incomingConditionName = "";
    String incomingConditionBlockchainID = "";
    String incomingConditionNameSender = "";
    // The incoming condition from blockchain list, which matches the above incoming parameters
    // While it is from (other) blockchain, which contains more information,
    // such as total action peers and whether allowed actions number to run
    ACPCondition incomingCondition = null;
    ACPTriggerTransaction incomingTriggerTransaction = null;
    final static int tokenTransactionDelta = 1;
    final static int outGoingTriggerDelta = 22;

    boolean shouldSendResultConditionToSuccessiveActions;
    boolean doesResultConditionSentToSuccessiveActions;

    //Action type
    String actionType = "";

    // Used to format outgoing condition for successvie actions in random trigger
    private int maxAllowedSuccessiveActionNumber;
    private int totalSuccessiveActionNumber;
    private int maxSuccessiveActionWaitingTime;

    private long initialTimeSecondCount;
    private String outgoingConditionName;

    // Currently, we use the actionttpe as the sender name for the tt and successive trigger
    // Change if more considering
    private String ttAndSuccessiveSender = "";

    public ACPAction() {
        init();
    }

    public ACPAction(String incomingConditionName, String outgoingConditionName) {
        init(incomingConditionName, outgoingConditionName);
    }

    public void reset() {
        init(incomingConditionName, outgoingConditionName);
    }

    void init() {
        init("", "");
    }
    void init(String incomingConditionName, String outgoingConditionName) {
        conditionTransactionList = new ArrayList<>();
        tokenTransactionList = new ArrayList<>();
        processConditionVersion = 0;
        latestConditionVersion = 0;
        maxAllowedSuccessiveActionNumber = 0;
        totalSuccessiveActionNumber = 0;
        maxSuccessiveActionWaitingTime = 0;
        initialTimeSecondCount = 0;
        actionRunnable = new ACPActionThread(this);
        doesConditionCome = false;
        acpTokenState = acpTokenState.ENone;
        conditionState = ACPConditionState.ENone;
        doesResultConditionSentToSuccessiveActions = false;
        shouldSendResultConditionToSuccessiveActions = false;
        this.incomingConditionName = incomingConditionName;
        this.outgoingConditionName = outgoingConditionName;
    }

    public void setIncomingConditionName(String incomingConditionName) {
        this.incomingConditionName = incomingConditionName;
    }

    public void setOutgoingConditionName(String outgoingConditionName) {
        this.outgoingConditionName = outgoingConditionName;
    }

    public void setSuccessiveActionNumber(int successiveActionNumber) {
        this.totalSuccessiveActionNumber = successiveActionNumber;
    }

    public void setIncomingConditionBlockchainID(String incomingConditionBlockchainID) {
        this.incomingConditionBlockchainID = incomingConditionBlockchainID;
    }

    public void setIncomingConditionNameSender(String incomingConditionNameSender) {
        this.incomingConditionNameSender = incomingConditionNameSender;
    }

    @Override
    public void checkAndDoAction() {
        // First we get current state to decide what will do the the transaction
        conditionState = getConditionState();
        // we separate those three actions, process action, send condition and sendToken
        if (shouldSendToken()) {
            trySendTokenTransaction();
        }

        if (ACPConditionState.ERun == conditionState) {
            processAction();
        }

        checkAndSendResultConditionToSuccessiveActioins();
    }

    @Override
    public boolean onIncomingConditionTransaction(ACPCommonTransaction t) {
        System.out.println("[ACPAction][Info] Try to process action transaction with id " + t.getId());
        if (t instanceof ACPTriggerTransaction) return onIncomingConditionTransaction((ACPTriggerTransaction)t);
        if (t instanceof  ACPTokenTransaction) return onIncomingConditionTransaction((ACPTokenTransaction) t);
        if (t instanceof ACPSettingTransaction) return onIncomingConditionTransaction((ACPSettingTransaction) t);

        System.out.println("[ACPAction][ERROR] Not support ACPCommonTransaction");
        return false;
    }

    String dump()
    {
        String r = "Required trigger condition: ";
        r += "name - " + incomingConditionName;
        r += ", sender - " + incomingConditionNameSender;
        r += ", chainID - " + incomingConditionBlockchainID;

        return r;
    }

    @Override
    public boolean onIncomingConditionTransaction(ACPTriggerTransaction t) {
        System.out.println("[ACPAction][Info] New incoming condition transaction (trigger transaction)'" + t.getJson().toString() + "'");
        System.out.println("[ACPAction][Debug] Current acp action configuration. " + dump());
        ++latestConditionVersion;
        conditionTransactionList.add(t);
        ACPCondition condition = t.getCondition();
        if (null != condition && condition.getName().equals(incomingConditionName)
        &&(incomingConditionBlockchainID.isEmpty() || incomingConditionBlockchainID.equals(t.getBlockchainId()))
        &&(incomingConditionNameSender.isEmpty() || incomingConditionNameSender.equals(t.getOwnerWithoutChainId())))
        {
            incomingTriggerTransaction = t;
            incomingCondition = condition;
            sendToAction();
        } else {
            if (null == incomingCondition) {
                outPutMisMatchedField(t);
            } else {
                System.out.println("[ACPAction][Info] No need to process this incoming trigger condition as we have got a matched before");
            }
        }
        return true;
    }

    void outPutMisMatchedField(ACPTriggerTransaction t){
        ACPCondition condition = t.getCondition();
        if (null == condition) {
            System.out.println("[ACPAction][ERROR] no condition field in condition transaction");
            return;
        }

        if (!incomingConditionBlockchainID.equals(t.getBlockchainId())) {
            System.out.printf("[ACPAction][ERROR] Condition blockchain id %s is not the same as required %s\n", t.getBlockchainId(), incomingConditionBlockchainID);
            return;
        }


        if (!incomingConditionNameSender.equals(t.getOwnerWithoutChainId())) {
            String logLevel = "ERROR";
            // If the blockchain ID is the same, it may trigger to successive, this is OK
            if (t.getBlockchainId().equals(MiningConfiguration.getBlockchainStringId())) logLevel = "WARN";
            System.out.printf("[ACPAction][%s] Condition Sender %s is not the same as required %s\n",
                    logLevel, t.getOwnerWithoutChainId(), incomingConditionNameSender);
            return;
        }
    }

    @Override
    public boolean onIncomingConditionTransaction(ACPTokenTransaction t) {
        System.out.println("[ACPAction][Info] New token transaction (tt)'" + t.getConditionName() + "'");

        if (!t.getConditionName().equals(incomingConditionName)){
            System.out.printf("[ACPAction][Info] Token name '%s' does not match required '%s'\n", t.getConditionName(), incomingConditionName);
            return false;
        }

        ++latestConditionVersion;
        tokenTransactionList.add(t);
        sendToAction();
        return true;
    }

    @Override
    public boolean onIncomingConditionTransaction(ACPSettingTransaction t) {
        System.out.println("[ACPAction][Info] Get ACP action parameter setting parameters " + t.getString());
        if (t.getIncomingConditionName().isEmpty()){
            System.out.println("[ACPAction][ERROR] No incoming trigger name");
            return false;
        }

        setActionType(t.getActionType());
        setIncomingConditionName(t.getIncomingConditionName());
        setIncomingConditionNameSender(t.getIncomingConditionNameSender());
        setIncomingConditionBlockchainID(t.getIncomingConditionBlockchainID());
        setOutgoingConditionName(t.getOutgoingConditionName());
        setMaxSuccessiveActionWaitingTime(t.getSuccessiveActionMaxWaitingTime());
        setTotalSuccessiveActionNumber(t.getSuccessiveActionTotalPeerNumber());
        setMaxAllowedSuccessiveActionNumber(t.getSuccessiveActionMaxAllowedPeerNumber());
        return true;
    }

    // To run asynchronously
    public void sendToAction() {
        System.out.println("[ACPAction][Info] Send to action");

        if (null == actionThread || !actionRunnable.isRunning()) {
            actionThread = new Thread(actionRunnable, "action processing thread for " + actionType);
            actionThread.start();
        } else {
            System.out.println("[ACPAction][Info] action processing thread for " + actionType + " is running. Skip to start a new thread");
        }
    }

    // If the incoming condition is random trigger, its the max allowed actions number is not 0.
    boolean shouldSendToken() {
        if (null == incomingCondition){
            System.out.println("[ACPAction][ERROR] Incoming condition does not get. Waiting ...");
            return false;
        }

        if (0 == incomingCondition.getMaxAllowedActionNumber()){
            System.out.println("[ACPAction][Info] Incoming condition is all trigger. No need to send tt");
            return false;
        }

        System.out.printf("[ACPAction][INFO] Incoming condition is random trigger and allows %d actions to run\n", incomingCondition.getMaxAllowedActionNumber());
        return true;
    }

    synchronized boolean checkAndSendResultConditionToSuccessiveActioins() {
        if (!canSendConditionToSuccessiveAction()) return false; // already send

        doesResultConditionSentToSuccessiveActions = true;
        ACPTriggerTransaction outgoingTransaction = new ACPTriggerTransaction();
        outgoingTransaction.setBlockchainID(MiningConfiguration.getBlockchainStringId());

        ACPCondition outgoingCondition = new ACPCondition(outgoingConditionName, maxAllowedSuccessiveActionNumber
                ,totalSuccessiveActionNumber, maxSuccessiveActionWaitingTime);
        outgoingTransaction.setCondition(outgoingCondition);
        ttAndSuccessiveSender = actionType + "_outogingTriggerSender";
        // Owner is full address and the blockchainID of the sender can be deduced from this
        // Then we ignore to set blockchain id
        outgoingTransaction.setOwner(ttAndSuccessiveSender);
        outgoingTransaction.setData(incomingTriggerTransaction.getData());// pass on the data from initial trigger data
        outgoingTransaction.generateSelectedNumberIfRandomTrigger();
        int id = incomingTriggerTransaction.getId() + outGoingTriggerDelta;
        outgoingTransaction.setId(id);

        BlockchainService.getInstance().getMiner().addTransaction(outgoingTransaction);
        System.out.printf("[ACPAction][Info] Outgoing trigger with id %d has been added to miner\n", id);
        return true;
    }

    private boolean canSendConditionToSuccessiveAction() {
        if (null == incomingCondition){
            System.out.println("[ACPAction][ERROR] Action is not triggered as no incoming condition. Skip to send");
            return false;
        }

        if (!shouldSendResultConditionToSuccessiveActions || doesResultConditionSentToSuccessiveActions) {
            System.out.printf("[ACPAction][Info] Skip to send result condition (outgoing trigger) to successive actions as %s\n",
                    (!shouldSendResultConditionToSuccessiveActions) ? "condition does not match" : " it has already been sent");
            return false;
        }

        if (0 != maxAllowedSuccessiveActionNumber && 0 == totalSuccessiveActionNumber){
            System.out.println("[ACPAction][ERROR] Total successive action number is not set, while it is random trigger");
            return false;
        }
        return true;
    }

    ACPConditionState getConditionState() {
        if (isFinalConditionState(conditionState)) {
            System.out.println("[ACPAction][INFO] Current state is final. Skip to check the state further");
            return conditionState;
        }

        if (!doesConditionCome) {
            if (null == incomingCondition) {
                System.out.println("[ACPAction][ERROR] No incoming trigger. ");
                return ACPConditionState.EError;
            }
            for (ACPTriggerTransaction incomingTransaction : conditionTransactionList) {
                if (incomingTransaction.getCondition().isTheSame(incomingCondition)){
                    doesConditionCome = true;
                }
            }
        }
        if (!doesConditionCome) return ACPConditionState.EWaiting;

        //Matched
        // all trigger, not limited number
        if (null == incomingCondition){
            System.out.println("[ACPAction][INFO] Incoming condition is not set");
            return ACPConditionState.EError;
        } else if (0 == incomingCondition.getMaxAllowedActionNumber()) {
            System.out.println("[ACPAction][INFO] All trigger condition. No need to check token and condition matches");
            return ACPConditionState.ERun;
        }

        if (isFinalTokenState(acpTokenState)) {
            System.out.println("[ACPAction][INFO] Skip to check token state as it is in final state " + acpTokenState);
        } else {
            System.out.println("[ACPAction][INFO] Random trigger condition. Begin to check token");
            acpTokenState = judegTokenState();
        }
        System.out.println("[ACPAction][INFO] Current token state " + acpTokenState);

        if (acpTokenState == ACPTokenState.ENoneToken) return ACPConditionState.ERun;
        if (acpTokenState == ACPTokenState.ERun) return ACPConditionState.ERun;
        if (acpTokenState == ACPTokenState.EReject) return ACPConditionState.EReject;
        if (acpTokenState == ACPTokenState.EError) return ACPConditionState.EError;

        // acpTokenState == ACPTokenState.EWaiting || acpTokenState == ACPTokenState.ENone)
        // Others, waiting. Two case: before or after tt sent, as (1) condition matches, (2) Not in final tt state.
        // Do not change current state for EWaitingToSendTT and EWaitingAfterSentTT
        if (conditionState != ACPConditionState.EWaitingAfterSentTT) {
            return ACPConditionState.EWaitingToSendTT;
        }
        return  ACPConditionState.EWaitingAfterSentTT;
    }

    boolean isFinalConditionState(ACPConditionState conditionState) {
        if (conditionState == ACPConditionState.EError || conditionState == ACPConditionState.EReject
                || conditionState == ACPConditionState.ERun) return true;
        return false;
    }

    boolean isFinalTokenState(ACPTokenState s) {
        if (s == ACPTokenState.EError || s == ACPTokenState.EReject
                || s == ACPTokenState.ERun) return true;
        return false;
    }

    boolean isFinalTokenState(ACPConditionState s) {
        if (s == ACPConditionState.EError || s == ACPConditionState.EReject
                || s == ACPConditionState.ERun) return true;
        return false;
    }

    private void processAction() {
        String defaultAction = "default";
        if (actionType.isEmpty() || actionType.equals(defaultAction)) { // default
            RandomHelper r = new RandomHelper(1, 10);
            ThreadHelper.safeSleepSecond(10 + r.getNumber());// simulate a time-consuming action
            System.out.printf("[ACPAction][Info] *** Succeed to execute ACP action %s\n", defaultAction);
            shouldSendResultConditionToSuccessiveActions = true;
        } else {
            // TO DO
        }
    }

    synchronized boolean trySendTokenTransaction() {
        if (conditionState != ACPConditionState.EWaitingToSendTT) {
            System.out.println("[ACPAction][Info] Skip to send token transaction(tt), as not in correct state " + conditionState);
            return false; // already send
        }
        conditionState = ACPConditionState.EWaitingAfterSentTT;

        ACPTokenTransaction t = new ACPTokenTransaction();
        t.setBlockchainID(MiningConfiguration.getBlockchainStringId());
        t.setConditionName(incomingConditionName);
        ttAndSuccessiveSender = actionType + "_ttSender";// ALthough we use the action type as ttAndSuccessiveSender, we want to make those two issues and an assignmeent here
        t.setOwner(ttAndSuccessiveSender);
        // We set the id of tt to relate to that of incoming trigger transaction
        // to make there is only one transaction eixts in blockchain, as currently we use id in miner
        // to elimnate the duplicated transactions
        int ttId = incomingTriggerTransaction.getId() + tokenTransactionDelta;
        t.setId(ttId);

        // Directly put to Miner to eliminate many token from each peer.
        // Internal transaction
        BlockchainService.getInstance().getMiner().addTransaction(t);
        if (0 == initialTimeSecondCount){
            initialTimeSecondCount = (System.currentTimeMillis() / 1000);
            System.out.println("[ACPAction][Info] Initial time count (seconds) " + initialTimeSecondCount);
        }
        System.out.printf("[ACPAction][Info] Send out token transaction with id %d for incoming random trigger %d\n",
                ttId, incomingTriggerTransaction.getId());

        return true;
    }

    ACPTokenState judegTokenState() {
        if (null == incomingCondition){
            System.out.println("[ACPAction][ERROR] No incoming condition, Waiting.");
            return acpTokenState.EError;
        }
        if (0 == incomingCondition.getMaxAllowedActionNumber()) {
            System.out.println("[ACPAction][WARN] Not a random trigger");
            return acpTokenState.ENoneToken;
        }

        // Both the max parameter number does not reach and max waiting time does not reach
        if (!doesAllActionsSendTheirToken() && !doesMaxWaitingTimeReach()) {
            return acpTokenState.EWaiting;
        }

        System.out.println("[ACPAction][INFO] Max waiting time and waiting action reached. Begin to process further");
        processConditionVersion = latestConditionVersion;

        // Try to find the target blockchain
        ACPTokenTransaction selectTokenTransaction = getSelectTokenTransaction();
        if (null == selectTokenTransaction) {
            System.out.println("[ACPAction][INFO] No token transaction found for random trigger " + incomingCondition.getName());
            return ACPTokenState.EError;
        }
        System.out.printf("[ACPAction][INFO] Token transaction found at blockchain %s with the selecting order %d\n",
                selectTokenTransaction.getBlockchainId(), incomingTriggerTransaction.getRandomSelectedNumber());

        if (!selectTokenTransaction.blockchainID.equals(MiningConfiguration.getBlockchainStringId())) {
            System.out.printf("[ACPAction][INFO] Selected token transaction %s is not ours %s. Skip to do the actions.\n",
                    selectTokenTransaction.blockchainID, MiningConfiguration.getBlockchainStringId());
            return acpTokenState.EReject;
        }

        return acpTokenState.ERun;
    }

    ACPTokenTransaction getSelectTokenTransaction(){
        if (!sortTokenList()) {
            System.out.println("[ACPAction][ERROR] Token transaction list is sorted or no token in it");
            return null;
        }

        // Currently we only choose the highest blockchain id
        int selectedBcId = incomingTriggerTransaction.getRandomSelectedNumberByHash() % sortedTokenTransactionList.length;
        return sortedTokenTransactionList[selectedBcId];
    }

    boolean sortTokenList(){
        if (null == tokenTransactionList || tokenTransactionList.isEmpty()){
            System.out.println("[ACPAction][ERROR] No token transaction received. Skip to sort tokenTransactionList");
            return false;
        }

        sortedTokenTransactionList = tokenTransactionList.toArray(new ACPTokenTransaction[tokenTransactionList.size()]);
        Arrays.sort(sortedTokenTransactionList, new ACPTokenTransactionComparatorByBlockchainId());
        return true;
    }

    private boolean doesMaxWaitingTimeReach() {
        long maxWaitingTime = incomingCondition.getMaxWaitingTime();
        if (0 == maxWaitingTime || 0 == initialTimeSecondCount) {
            System.out.printf("[ACPAction][INFO] Max waiting waiting time %d is not set or initial time %d is not set. Timeout will not be used\n", maxWaitingTime, initialTimeSecondCount);
            return false;
        }

        // Time passed is more than maxWaitingTime
        long passTime = (System.currentTimeMillis() / 1000) - initialTimeSecondCount;
        System.out.printf("[ACPAction][INFO] The set max waiting time is %d, and time passed is %d from %d at %s\n"
                , maxWaitingTime, passTime, initialTimeSecondCount, TimeHelper.getCurrentTimeUsingCalendar());
        return maxWaitingTime <= passTime;
    }

    private boolean doesAllActionsSendTheirToken() {
        int totalActionNumber = incomingCondition.getTotalSuccessiveActionNumber();
        if (0 == totalActionNumber) {
            System.out.println("[ACPAction][INFO] The total action number in incoming condition is not set. We check timeout");
            return false;
        }

        // When more tt than threshold
        System.out.printf("[ACPAction][INFO] The total actions number in incoming condition %d, and token transactions got is %d\n"
                , totalActionNumber, tokenTransactionList.size());
        boolean r = totalActionNumber <= tokenTransactionList.size();
        if (r){
            System.out.println("[ACPAction][INFO] All actions send their options.");
        }
        return r;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public void setMaxAllowedSuccessiveActionNumber(String maxAllowedSuccessiveActionNumber) {
        this.maxAllowedSuccessiveActionNumber = safeGetNumber(maxAllowedSuccessiveActionNumber, 0);
    }

    public void setTotalSuccessiveActionNumber(String totalSuccessiveActionNumber) {
        this.totalSuccessiveActionNumber = safeGetNumber(totalSuccessiveActionNumber, 0);
    }

    public void setMaxSuccessiveActionWaitingTime(String maxSuccessiveActionWaitingTime) {
        this.maxSuccessiveActionWaitingTime = safeGetNumber(maxSuccessiveActionWaitingTime, 0);
    }

    int safeGetNumber(String number, int defaultValue){
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e){
            e.printStackTrace();
            System.out.println("[ACPAction][ERROR] Error parameter, use default value " + defaultValue);
            return defaultValue;
        }
    }

    public String getActionType() {
        return actionType;
    }

    // Settinig transaction must set the action type even if it a default action
    public boolean isValid(){
        return (null != actionType) && (!actionType.isEmpty());
    }

    @Override
    public boolean isDone() {
        return isFinalTokenState(conditionState);
    }

    //for unit test
    public boolean testDoesIncomingTrigerReach(){
        return null != incomingCondition;
    }

    //for unit test
    public boolean testDoesOutgoingTriggerSent(){
        return doesResultConditionSentToSuccessiveActions;
    }
}

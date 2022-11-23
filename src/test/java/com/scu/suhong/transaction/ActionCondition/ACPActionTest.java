package com.scu.suhong.transaction.ActionCondition;

import Service.BlockchainService;
import Service.BlockchainServiceThread;
import com.scu.suhong.block.BlockChain;
import consensus.pow.MiningConfiguration;
import org.junit.Test;
import util.ThreadHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ACPActionTest {

    @Test
    public void checkAndSendResultConditionToSuccessiveActioins() {
        prepareBlockchain();

        String incomingConditionName = "test incoming condition";
        String outgoingConditionName = "test outgoing condition";
        int maxAllowedActionNumber = 0, totalSuccessiveActionNumber = 2, maxWaitingTime = 10;
        ACPAction action = new ACPAction(incomingConditionName, outgoingConditionName);
        assert !action.checkAndSendResultConditionToSuccessiveActioins();

        ACPTriggerTransaction incomingTransaction = new ACPTriggerTransaction(
                incomingConditionName, maxAllowedActionNumber, totalSuccessiveActionNumber, maxWaitingTime);
        incomingTransaction.generateSelectedNumberIfRandomTrigger();
        incomingTransaction.setId();
        action.onIncomingConditionTransaction(incomingTransaction);

        action.shouldSendResultConditionToSuccessiveActions = true;
        assert !action.checkAndSendResultConditionToSuccessiveActioins();

        ACPTriggerTransaction conditionTransaction = new ACPTriggerTransaction();
        conditionTransaction.setCondition(new ACPCondition(incomingConditionName, maxAllowedActionNumber, totalSuccessiveActionNumber, maxWaitingTime));
        conditionTransaction.generateSelectedNumberIfRandomTrigger();
        conditionTransaction.setId();
        assert !action.doesResultConditionSentToSuccessiveActions;
        action.onIncomingConditionTransaction(conditionTransaction);
        ThreadHelper.safeSleepSecond(3);
        assert action.doesResultConditionSentToSuccessiveActions;
    }

    @Test
    public void shouldSendTokenWhenCorrectParametersAreSet() {
        String incomingConditionName = "test incoming condition";
        String outgoingConditionName = "test outgoing condition";
        ACPAction action = new ACPAction(incomingConditionName, outgoingConditionName);
        assert !action.shouldSendToken();

        System.out.println("[Test] After set incoming condition");
        ACPTriggerTransaction conditionTransaction = new ACPTriggerTransaction();
        conditionTransaction.setCondition(new ACPCondition(incomingConditionName, 0, 3, 10));
        conditionTransaction.generateSelectedNumberIfRandomTrigger();
        conditionTransaction.setId();
        action.onIncomingConditionTransaction(conditionTransaction);
        assert !action.shouldSendToken();

        conditionTransaction.setCondition(new ACPCondition(incomingConditionName, 1, 3, 10));
        action.onIncomingConditionTransaction(conditionTransaction);
        assert action.shouldSendToken();
    }

    @Test
    public void wholeProcess() throws IOException {
        MiningConfiguration.setDifficulty(0);
        prepareBlockchain();

        String incomingConditionName = "test incoming condition";
        String incomingConditionSender = "incomingConditionSender";
        String outgoingConditionName = "test outgoing condition";
        int blockMiningSleepTime = 10 * 1000;
        ACPAction action = new ACPAction(incomingConditionName, outgoingConditionName);
        action.setSuccessiveActionNumber(1);
        action.setIncomingConditionBlockchainID(MiningConfiguration.getBlockchainStringId());
        BlockchainService blockchainService = BlockchainService.getInstance();
        ACPActionTriggerTransactionHandler handler = ACPActionTriggerTransactionHandler.getInstance();
        handler.setAction(action);

        System.out.println("\n\n[Test] Begin to test wait, as no tt reaches");
        ACPTriggerTransaction conditionTransaction = new ACPTriggerTransaction();
        conditionTransaction.setCondition(new ACPCondition(incomingConditionName, 1, 3, 1));
        conditionTransaction.generateSelectedNumberIfRandomTrigger();
        conditionTransaction.setId();
        blockchainService.processTransaction(conditionTransaction);
        ThreadHelper.safeSleep(blockMiningSleepTime);
        assert ACPTokenState.EWaiting == action.acpTokenState;
        System.out.println("\n[Test] Dump blockchain \n" + BlockChain.getInstance().dump());

        ACPTokenTransaction tt = new ACPTokenTransaction();
        tt.setConditionName(incomingConditionName);
        System.out.println("\n\n[Test] Begin to test run. The case is that there are two tt, and our blockchain id is bigger, then we can run");
        tt.setBlockchainID(MiningConfiguration.getBlockchainStringId());
        int testId = 12366;
        tt.setId(testId);
        action.shouldSendResultConditionToSuccessiveActions = true;
        blockchainService.processTransaction(tt);
        ThreadHelper.safeSleep(blockMiningSleepTime  + 1);// Bigger than max wait time
        assert ACPTokenState.ERun == action.judegTokenState();
        // One is sent here manaully(simulate send from other blockchain) and another one is internal mechanism
        assert 2 == action.tokenTransactionList.size();
        assert action.doesResultConditionSentToSuccessiveActions;
    }

    @Test
    public void sendTokenTransaction() {
        prepareBlockchain();

        String incomingConditionName = "test incoming condition";
        String outgoingConditionName = "test outgoing condition";
        ACPAction action = new ACPAction(incomingConditionName, outgoingConditionName);
        action.setActionType("default");
        assert ACPTokenState.EError == action.judegTokenState();
        assert !action.trySendTokenTransaction();

        System.out.println("\n\n[Test] Begin to test EError, as no blockch id and owner not set");
        action.setIncomingConditionBlockchainID(MiningConfiguration.getBlockchainStringId());
        String sender = "sender";
        action.setIncomingConditionNameSender(sender);
        ACPTriggerTransaction conditionTransaction = new ACPTriggerTransaction();
        conditionTransaction.setCondition(new ACPCondition(incomingConditionName, 1, 3, 1));
        conditionTransaction.generateSelectedNumberIfRandomTrigger();
        conditionTransaction.setId();
        action.onIncomingConditionTransaction(conditionTransaction);
        action.setSuccessiveActionNumber(1);
        ThreadHelper.safeSleep(3*1000);
        assert null == action.incomingCondition;
        assert ACPTokenState.ENone == action.acpTokenState;

        System.out.println("\n\n[Test] Begin to test wait, as no tt reaches");
        conditionTransaction.setBlockchainID(MiningConfiguration.getBlockchainStringId());
        conditionTransaction.setOwner(sender);
        conditionTransaction.setId();
        action.onIncomingConditionTransaction(conditionTransaction);
        action.setSuccessiveActionNumber(1);
        System.out.println("\n\n[Test][Debug] Current state " + action.acpTokenState);
        //refresh the state
        action.getConditionState();
        assert ACPTokenState.EWaiting == action.acpTokenState;

        System.out.println("\n\n[Test] Begin to test reject. The case is that there is only one tt, which is another blockchain id. Then this chain is rejected");
        ACPTokenTransaction tt = new ACPTokenTransaction();
        tt.setConditionName(incomingConditionName);
        // should be smaller than current blockchain id to let the third assert pass
        String otherBlockchainID = "018";
        tt.setBlockchainID(otherBlockchainID);
        action.onIncomingConditionTransaction(tt);
        ThreadHelper.safeSleep(2*1000);
        assert ACPTokenState.EReject == action.judegTokenState();

        System.out.println("\n\n[Test] Begin to test run. The case is that there are two tt, and our blockchain id is bigger, then we can run");
        tt.setBlockchainID(MiningConfiguration.getBlockchainStringId());
        action.onIncomingConditionTransaction(tt);
        ThreadHelper.safeSleep(2*1000);
        assert ACPTokenState.ERun == action.judegTokenState();

        ThreadHelper.safeSleep(2 * 1000);
    }

    @Test
    public void getSelectTokenTransaction() {
        prepareBlockchain();

        ACPAction action = new ACPAction();

        String conditionName = "incomingConditionName";
        int maxAllowedActionNumber = 1, totalSuccessiveActionNumber = 3, maxWaitingTime = 20;
        ACPSettingTransaction setTransaction = new ACPSettingTransaction();
        setTransaction.setId();
        setTransaction.setIncomingConditionName(conditionName);
        setTransaction.setSuccessiveActionMaxAllowedPeerNumber(String.valueOf(maxAllowedActionNumber));
        setTransaction.setSuccessiveActionTotalPeerNumber(String.valueOf(totalSuccessiveActionNumber));
        setTransaction.setSuccessiveActionMaxWaitingTime(String.valueOf(maxWaitingTime));
        action.onIncomingConditionTransaction(setTransaction);

        ACPTriggerTransaction incomingTransaction = new ACPTriggerTransaction(
                conditionName, maxAllowedActionNumber, totalSuccessiveActionNumber, maxWaitingTime);
        incomingTransaction.generateSelectedNumberIfRandomTrigger();
        incomingTransaction.setId();
        action.onIncomingConditionTransaction(incomingTransaction);


        List<ACPTokenTransaction> tokenTransactionArray = new ArrayList<>();
        for (int i = 0; i < totalSuccessiveActionNumber; ++i) {
            ACPTokenTransaction tt = new ACPTokenTransaction();
            tt.setBlockchainID(String.format("%d%d%d", (i+1), (i+1), (i+1) ));
            tt.setId();
            tt.setConditionName(conditionName);
            tokenTransactionArray.add(tt);
        }

        for (int i = 0; i < tokenTransactionArray.size(); ++i) {
            assert action.onIncomingConditionTransaction(tokenTransactionArray.get(i));
        }

        ACPTokenTransaction acpTokenTransaction = action.getSelectTokenTransaction();
        int selectedNumber = incomingTransaction.getRandomSelectedNumber();
        System.out.printf("\n[Test] selectedNumber %d and acpTokenTransaction bcid is %s\n", selectedNumber, acpTokenTransaction.getBlockchainId());
        assert selectedNumber < totalSuccessiveActionNumber;
        assert selectedNumber >= 0;
        assert acpTokenTransaction.isSimilar(tokenTransactionArray.get(selectedNumber));
    }

    //    @Test
//    public void sendACPActionSettingTransaction() {
//        prepareBlockchain();
//
//        String actionType = "default";
//        String incomingConditionName = "test_incoming_condition";
//        String incomingConditionSender = "test_incoming_sender";
//        String incomingBlockchainId = MiningConfiguration.getBlockchainStringId();
//        String outgoingConditionName = "test_outgoing_condition";
//        String successiveActionMaxWaitingTime = "1";
//        String successiveActionTotalPeerNumber = "3";
//        String successiveActionMaxAllowedPeerNumber = "1";
//        String acpActionSettingOwner = "acpActionSettingOwner";
//
//        ACPActionParameterSetTransaction t = new ACPActionParameterSetTransaction();
//        t.setActionType(actionType);
//        t.setIncomingConditionName(incomingConditionName);
//        t.setIncomingConditionNameSender(incomingConditionSender);
//        t.setIncomingConditionBlockchainID(incomingBlockchainId);
//        t.setOutgoingConditionName(outgoingConditionName);
//        t.setSuccessiveActionMaxWaitingTime(successiveActionMaxWaitingTime);
//        t.setSuccessiveActionTotalPeerNumber(successiveActionTotalPeerNumber);
//        t.setSuccessiveActionMaxAllowedPeerNumber(successiveActionMaxAllowedPeerNumber);
//        t.setOwner(acpActionSettingOwner);
//        t.setId();
//
//        ACPActionTriggerTransactionHandler handler = ACPActionTriggerTransactionHandler.getInstance();
//        handler.tryProcessTransaction(t);
//        ThreadHelper.safeSleep(2*1000);
//
//        ACPAction action = handler.getAction();
//        assert action.isValid();
//        System.out.println("\n\n[Test] Parameter has been set succesffully");
//
//        System.out.println("\n\n[Test] Begin to test wait, as no tt reaches");
//        ACPConditionTransaction conditionTransaction = new ACPConditionTransaction();
//        conditionTransaction.setCondition(new ACPCondition(incomingConditionName, 1, 3, 1));
//        conditionTransaction.setBlockchainID(MiningConfiguration.getBlockchainStringId());
//        conditionTransaction.setOwner(incomingConditionSender);
//        conditionTransaction.setId();
//
//        handler.tryProcessTransaction(conditionTransaction);
//        ThreadHelper.safeSleep(10*1000);
//        action.getConditionState();//refresh
//        assert ACPTokenState.EWaiting == action.acpTokenState;
//
//        System.out.println("\n\n[Test] Begin to test reject. The case is that there is only one tt, which is another blockchain id. Then this chain is rejected");
//        ACPTokenTransaction tt = new ACPTokenTransaction();
//        tt.setConditionName(incomingConditionName);
//        // should be smaller than current blockchain id to let the third assert pass
//        String otherBlockchainID = "018";
//        tt.setBlockchainID(otherBlockchainID);
//        handler.tryProcessTransaction(tt);
//        ThreadHelper.safeSleep(2*1000);
//        assert ACPTokenState.EReject == action.judegTokenState();
//
//        System.out.println("\n\n[Test] Begin to test run. The case is that there are two tt, and our blockchain id is bigger, then we can run");
//        tt.setBlockchainID(MiningConfiguration.getBlockchainStringId());
//        handler.tryProcessTransaction(tt);
//        ThreadHelper.safeSleep(2*1000);
//        assert ACPTokenState.ERun == action.judegTokenState();
//
//        ThreadHelper.safeSleep(2 * 1000);
//    }

    private void prepareBlockchain() {
        BlockchainService blockchain = BlockchainService.getInstance();
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread(blockchain);
        Thread mainThread = new Thread(blockchainServiceThread, "ACP action test");
        mainThread.start();
        while (true) {
            ThreadHelper.safeSleep(1 * 1000);
            if (null != blockchain.getMiner()) {
                break;
            }
        }
    }
}
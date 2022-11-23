package command;

import Service.BlockchainService;
import Service.BlockchainServiceThread;
import account.AccountManager;
import com.scu.suhong.block.Block;
import com.scu.suhong.block.BlockChain;
import com.scu.suhong.dynamic_definition.AbstractTransaction;
import com.scu.suhong.dynamic_definition.AssetCompiler;
import com.scu.suhong.dynamic_definition.DynamicalAsset;
import com.scu.suhong.smartcontract.lifecycleFlexibility.LifecycleHandler;
import com.scu.suhong.transaction.ActionCondition.ACPAction;
import com.scu.suhong.transaction.ActionCondition.ACPActionTriggerTransactionHandler;
import com.scu.suhong.transaction.AssetCreateTransaction;
import com.scu.suhong.transaction.CommonCrosschainTransaction;
import com.scu.suhong.transaction.CrosschainInterface;
import com.scu.suhong.transaction.CrosschainTransaction;
import com.scu.suhong.transaction.multipleTypeExchange.MultiTypeExchangeProcessor;
import com.scu.suhong.transaction.multipleTypeExchange.MultiTypeExchangeTransaction;
import com.sun.javafx.binding.StringFormatter;
import consensus.pow.MiningConfiguration;
import org.junit.Test;
import util.FileHelper;
import util.RandomHelper;
import util.ThreadHelper;

import java.util.ArrayList;
import java.util.List;

public class CommandServiceThreadTest {

    private final static String user1 = "0x123451";
    private final static String user2 = "0x123452";

    private final static int user1InitBanlance = 20;
    private final static int user2InitBanlance = 20;
    private final static int testValue = 3;
    static int speed = 8;
    static int stdQueueSize = 80;
    static int testTimes = 10;

    @Test
    public void testDoCommand() throws InterruptedException {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        CommandServiceThread commandServiceThread = new CommandServiceThread();
        Thread.sleep(2000); // sleep a while

        sendBatchData(commandServiceThread);
        sendBigData(commandServiceThread);
        dumpCommand(commandServiceThread);

        Thread.sleep(2000); // sleep a while
        exitCommand(commandServiceThread);
        blockThread.join();
    }

    @Test
    public void testActionConditionPair() {
        String incomingName = "incoming";
        String ougoingName = "outgoing";
        String actionType = "default";
        String incomingChainId = MiningConfiguration.getBlockchainStringId();
        String incomingOwner = "incomingOwner";
        String successiveActionMaxWaitingTime = "10";
        String successiveActionTotalPeerNumber = "3";
        String successiveActionMaxAllowedPeerNumber = "1";

        MiningConfiguration.testSetRequiredZeroCount(1);
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        BlockchainService.getInstance().getMiner().setRunInterval(1);

        System.out.println("\n\n[Test] Begin to set the parameter for action processor by transactioin");
        CommandServiceThread commandServiceThread = new CommandServiceThread();
        // setacp actionType, incomingName ,incomingOwner, incomingBlockchainId, outgoingName, successiveActionMaxWaitingTime,
        // successiveActionTotalPeerNumber successiveActionMaxAllowedPeerNumber
        String commandLine = "setacp" + " " + actionType + " " + incomingName + " " + incomingOwner + " " + incomingChainId + " " + ougoingName
                + " " + successiveActionMaxWaitingTime + " " + successiveActionTotalPeerNumber + " " + successiveActionMaxAllowedPeerNumber;
        // sendex interactionId from to value reqChainId reqFrom reqTo reqValue
        commandServiceThread.processUserCommand(commandLine);
        ThreadHelper.safeSleep(1 * 1000);

        System.out.println("\n\n[Test] Begin to check the state of action processor");
        ACPActionTriggerTransactionHandler handler = ACPActionTriggerTransactionHandler.getInstance();
        ACPAction action = handler.getAction();
        int maxMiningWaitingTime = 30; // 30 seconds
        for (int i = 0; i < maxMiningWaitingTime; ++i) {
            if (null == action) action = handler.getAction();
            if (null != action && action.isValid()) {
                System.out.println("[Test] setacp is mined, stop to wait");
                break;
            }
            ThreadHelper.safeSleep(1 * 1000);
        }

        assert null != action;
        assert action.isValid();

        System.out.println("\n\n[Test] Begin to trigger the action by sending a trigger condition transaction");
        String maxWaitingTime = "10";
        String totalSuccessiveActionNumber = "3";
        String maxAllowedActionNumber = "1";
        String data = "data";
        String sender = incomingOwner;// to match the requirement in setacp
        // send acp condition transaction manually
        // sendacpcondition incomingName maxWaitingTime totalSuccessiveActionNumber maxAllowedActionNumber data sender
        commandLine = "sendacpcondition" + " " + incomingName + " " + maxWaitingTime + " " + totalSuccessiveActionNumber + " " + maxAllowedActionNumber
                + " " + data + " " + sender;
        commandServiceThread.processUserCommand(commandLine);
        ThreadHelper.safeSleep(1 * 1000);

        for (int i = 0; i < maxMiningWaitingTime; ++i) {
            if (action.testDoesIncomingTrigerReach()) {
                System.out.println("[Test] sendacpcondition is mined, stop to wait");
                break;
            }
            ThreadHelper.safeSleep(1 * 1000);
        }

        System.out.println("\n\n[Test] Begin to wait to send tt and finish");
        assert action.testDoesIncomingTrigerReach();
        ThreadHelper.safeSleep((Integer.parseInt(successiveActionMaxWaitingTime) + 10) * 1000);// successiveActionMaxWaitingTime + ensuring time
        assert action.testDoesOutgoingTriggerSent();
    }

    @Test
    public void testSendExternalTransaction() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        int initBlockchainSize = BlockChain.getInstance().getBlockList().size();

        BlockchainService.getInstance().getMiner().setRunInterval(1);

        String from = "56799", to = "89034", reqFrom = "389056", reqTo = "903456";
        int interactionId = 12, value = 10, reqChainId = 333, reqValue = 10;
        int fromInitValue = 20;
        AccountManager.getInstance().addValue(from, fromInitValue);
        assert fromInitValue == AccountManager.getInstance().getBalance(from);

        CommandServiceThread commandServiceThread = new CommandServiceThread();

        String commandLine = "sendex" + " " + interactionId + " " + from + " " + to
                + " " + value + " " + reqChainId + " " + reqFrom + " " + reqTo + " " + reqValue;
        // sendex interactionId from to value reqChainId reqFrom reqTo reqValue
        commandServiceThread.processUserCommand(commandLine);

        ThreadHelper.safeSleep(3 * 1000);
        boolean found = false;
        while (!found) {
            List<Block> blockList = BlockChain.getInstance().getBlockList();
            for (int i = initBlockchainSize; i < blockList.size(); ++i) {
                Block b = blockList.get(i);
                for (AbstractTransaction t : b.getTransactions()) {
                    if (t instanceof CrosschainInterface) {
                        CrosschainTransaction ct = (CrosschainTransaction) t;
                        if (ct.getBlockchainId() == MiningConfiguration.getBlockchainStringId() && ct.getInteractionId() == interactionId) {
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                if (found) break;
                ThreadHelper.safeSleep(1 * 1000);
            }
        }
        System.out.println("[Test] the balance is: " + AccountManager.getInstance().getBalance(from));
        assert fromInitValue - value == AccountManager.getInstance().getBalance(from);
    }

    @Test
    public void testSendMultipleTypeTransaction() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        int initBlockchainSize = BlockChain.getInstance().getBlockList().size();

        BlockchainService.getInstance().getMiner().setRunInterval(1);

        int exchangeId = 12;
        String requiredType = "fixed";

        String from1 = "from1";
        String assetType1 = "assetType1";
        int value1 = 41;
        String from2 = "from2";
        String assetType2 = "assetType2";
        int value2 = 42;
        String from3 = "from3";
        String assetType3 = "assetType3";
        int value3 = 43;

        // 1->2 at 1 re at 3; 2-3>3 at 2 re: at1; 3->1 at3 : re:at2
        // from:<to>:assetType:assetAmount;
        String requiredDataList = "any:" + from1 + ":" + assetType3 + ":" + value3;
        requiredDataList += ";any:" + from2 + ":" + assetType1 + ":" + value1;
        requiredDataList += ";any:" + from3 + ":" + assetType2 + ":" + value2;

        int fromInitValue = 200;
        AccountManager.getInstance().addValue(from1, assetType1, fromInitValue);
        AccountManager.getInstance().addValue(from2, assetType2, fromInitValue);
        AccountManager.getInstance().addValue(from3, assetType3, fromInitValue);

        CommandServiceThread commandServiceThread = new CommandServiceThread();

        // 1->2 at 1 re at 3; 2-3>3 at 2 re: at1; 3->1 at3 : re:at2
        // sendmt exchangeid <fix|var> from to assetType assetAmount "from:<to>:assetType:assetAmount;from:<to>:assetType:assetAmount;..."
        String commandLine = "sendmt" + " " + exchangeId + " " + requiredType + " " + from1 + " " + from2
                + " " + assetType1 + " " + value1 + " " + requiredDataList;
        // sendex interactionId from to value reqChainId reqFrom reqTo reqValue
        commandServiceThread.processUserCommand(commandLine);
        commandLine = "sendmt" + " " + exchangeId + " " + requiredType + " " + from2 + " " + from3
                + " " + assetType2 + " " + value2 + " " + requiredDataList;
        // sendex interactionId from to value reqChainId reqFrom reqTo reqValue
        commandServiceThread.processUserCommand(commandLine);
        commandLine = "sendmt" + " " + exchangeId + " " + requiredType + " " + from3 + " " + from1
                + " " + assetType3 + " " + value3 + " " + requiredDataList;
        // sendex interactionId from to value reqChainId reqFrom reqTo reqValue
        commandServiceThread.processUserCommand(commandLine);

        ThreadHelper.safeSleep(3 * 1000);
        boolean found = false;
        while (!found) {
            List<Block> blockList = BlockChain.getInstance().getBlockList();
            for (int i = initBlockchainSize; i < blockList.size(); ++i) {
                Block b = blockList.get(i);
                for (AbstractTransaction t : b.getTransactions()) {
                    if (t instanceof MultiTypeExchangeTransaction) {
                        MultiTypeExchangeTransaction mt = (MultiTypeExchangeTransaction) t;
                        if (mt.getExchangeId() == exchangeId) {
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                if (found) break;
                ThreadHelper.safeSleep(1 * 1000);
            }
        }
        ThreadHelper.safeSleep(3 * 1000);
        System.out.println("[Test] The balance is: " + AccountManager.getInstance().getBalance(from1, assetType1));
        assert MultiTypeExchangeProcessor.getInstance().getProcessor(exchangeId).complete();
        assert fromInitValue - value1 == AccountManager.getInstance().getBalance(from1, assetType1);
        assert fromInitValue - value2 == AccountManager.getInstance().getBalance(from2, assetType2);
        assert fromInitValue - value3 == AccountManager.getInstance().getBalance(from3, assetType3);

        assert value1 == AccountManager.getInstance().getBalance(from2, assetType1);
        assert value2 == AccountManager.getInstance().getBalance(from3, assetType2);
        assert value3 == AccountManager.getInstance().getBalance(from1, assetType3);
    }

    @Test
    public void testSendCrosschainMultipleTypeTransaction() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        int initBlockchainSize = BlockChain.getInstance().getBlockList().size();

        BlockchainService.getInstance().getMiner().setRunInterval(1);

        int exchangeId = 12;
        String requiredType = "fixed";
        String requiredChainID = MiningConfiguration.getBlockchainStringId();

        String user1 = "user1";
        String user2 = "user2";

        String assetType1 = "assetType1";
        int value1 = 41;
        String assetType2 = "assetType2";
        int value2 = 42;

        // 1->2 at 1 re at 2; 2->1 ;  2->1 at 2 re at 1; 1->2
        // requiredChainID:from:<to>:assetType:assetAmount;requiredChainID:from:<to>:assetType:assetAmount;...
        String requiredDataList = requiredChainID + ":" + user1 + ":" + user2 + ":" + assetType1 + ":" + value1;
        requiredDataList += ";" + requiredChainID + ":" + user2 + ":" + user1 + ":" + assetType2 + ":" + value2;

        int fromInitValue = 200;
        AccountManager.getInstance().addValue(user1, assetType1, fromInitValue);
        AccountManager.getInstance().addValue(user2, assetType2, fromInitValue);

        CommandServiceThread commandServiceThread = new CommandServiceThread();

        // 1->2 at 1 re at 3; 2-3>3 at 2 re: at1; 3->1 at3 : re:at2
        // sendcmt exchangeid <fixed|variable> from to assetType assetAmount "requiredChainID:from:<to>:assetType:assetAmount;requiredChainID:from:<to>:assetType:assetAmount;..." data
        String commandLine = "sendcmt" + " " + exchangeId + " " + requiredType + " " + user1 + " " + user2
                + " " + assetType1 + " " + value1 + " " + requiredDataList;
        commandServiceThread.processUserCommand(commandLine);
        commandLine = "sendcmt" + " " + exchangeId + " " + requiredType + " " + user2 + " " + user1
                + " " + assetType2 + " " + value2 + " " + requiredDataList + " And_the_second";
        // sendex interactionId from to value reqChainId reqFrom reqTo reqValue
        commandServiceThread.processUserCommand(commandLine);

        ThreadHelper.safeSleep(3 * 1000);
        boolean found = false;
        while (!found) {
            List<Block> blockList = BlockChain.getInstance().getBlockList();
            for (int i = initBlockchainSize; i < blockList.size(); ++i) {
                Block b = blockList.get(i);
                for (AbstractTransaction t : b.getTransactions()) {
                    if (t instanceof MultiTypeExchangeTransaction) {
                        MultiTypeExchangeTransaction mt = (MultiTypeExchangeTransaction) t;
                        if (mt.getExchangeId() == exchangeId) {
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                if (found) break;
                ThreadHelper.safeSleep(1 * 1000);
            }
        }
        ThreadHelper.safeSleep(3 * 1000);
        System.out.println("[Test] The balance is: " + AccountManager.getInstance().getBalance(user1, assetType1));
        assert MultiTypeExchangeProcessor.getInstance().getProcessor(exchangeId).complete();
        assert fromInitValue - value1 == AccountManager.getInstance().getBalance(user1, assetType1);
        assert fromInitValue - value2 == AccountManager.getInstance().getBalance(user2, assetType2);

        assert value1 == AccountManager.getInstance().getBalance(user2, assetType1);
        assert value2 == AccountManager.getInstance().getBalance(user1, assetType2);
    }

    @Test
    public void testSendCommonCrosschainTransaction() {
        // First load the blockchain service
        MiningConfiguration.testSetRequiredZeroCount(0);
        BlockChain.getInstance().testRest();;
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(1000);

        BlockchainService.getInstance().getMiner().setRunInterval(1);

        String user1 = "789001";
        String user2 = "789002";
        int value = 2;
        RandomHelper randomHelper = new RandomHelper(1, 100000);
        int exchangeId = randomHelper.getNumber();
        System.out.println("[Test] Begin to test common crosschain exxchange with id " + exchangeId);
        CommandServiceThread commandServiceThread = new CommandServiceThread();

        AccountManager accountManager = AccountManager.getInstance();
        Double user1InitialBalance = accountManager.getBalance(user1);
        Double user2InitialBalance = accountManager.getBalance(user2);
        String commandLine = StringFormatter.format("sendcomct %d notary_first_txonbc1 %s %s %d",
                exchangeId, user1, user2, value).getValue();
        commandServiceThread.processUserCommand(commandLine);

        commandLine = StringFormatter.format("sendcomct %d notary_second_txonbc2 %s %s %d",
                exchangeId, user2, user1, 0).getValue();
        // sendex interactionId from to value reqChainId reqFrom reqTo reqValue
        commandServiceThread.processUserCommand(commandLine);

        ThreadHelper.safeSleep(3 * 1000);
        boolean found = false;
        while (!found) {
            List<Block> blockList = BlockChain.getInstance().getBlockList();
            for (int i = 0; i < blockList.size(); ++i) {
                Block b = blockList.get(i);
                for (AbstractTransaction t : b.getTransactions()) {
                    if (t instanceof CommonCrosschainTransaction) {
                        CommonCrosschainTransaction cct = (CommonCrosschainTransaction) t;
                        System.out.println("[Test][INFO] transaction exchange id " + cct.getInteractionId());
                        if (cct.getInteractionId() == exchangeId) {
                            if (cct.getData().contains("notary_second_txonbc2")) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) break;
                }
                if (found) break;
                ThreadHelper.safeSleep(1 * 1000);
            }
        }
        ThreadHelper.safeSleep(3 * 1000);
        System.out.printf("%s %s - %s,%s %s - %s\n", user1, user1InitialBalance.toString(), accountManager.getBalance(user1).toString(),
                user2, user2InitialBalance.toString(), accountManager.getBalance(user2).toString());
        assert 0 == Double.compare(user1InitialBalance - value, accountManager.getBalance(user1));
        assert 0 == Double.compare(user2InitialBalance + value, accountManager.getBalance(user2));
    }

    @Test
    public void testSendConditionalTransaction() {
        // First load the blockchain service
        MiningConfiguration.testSetRequiredZeroCount(1);
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        int initBlockchainSize = BlockChain.getInstance().getBlockList().size();
        BlockchainService.getInstance().getMiner().setRunInterval(1);

        // The following user accounts have been initialized in AccountManager's construction, please check if not
        String userA = "789001", userB = "789002", userC = "789003", userD = "789004";
        String commandSeparator = " ";
        int waitTime = 4 * 1000;
        int interactionId = 111;

        AccountManager accountManager = AccountManager.getInstance();
        assert accountManager.getBalance(userA) == 100;
        assert accountManager.getBalance(userB) == 100;
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;

        System.out.println("\n[Test] Begin to send the first transaction");
        String cmd = "sendct " + interactionId + " " + userA + commandSeparator + userB + " 4";
        CommandServiceThread commandServiceThread = new CommandServiceThread();
        commandServiceThread.processUserCommand(cmd);
        ThreadHelper.safeSleep(waitTime);
        assert accountManager.getBalance(userA) == 96;
        assert accountManager.getBalance(userB) == 100;
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;

        System.out.println("\n[Test] Begin to send the second transaction");
        cmd = "sendct " + interactionId + " " + userB + commandSeparator + userC + "_3" + "_"  + userD + "_1" + commandSeparator + "4";
        commandServiceThread.processUserCommand(cmd);
        ThreadHelper.safeSleep(waitTime);
        assert accountManager.getBalance(userA) == 96;
        assert accountManager.getBalance(userB) == 96;
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;

        System.out.println("\n[Test] Begin to send the third transaction");
        cmd = "sendct " + interactionId + " " + userC + commandSeparator + userA + commandSeparator + "3";
        commandServiceThread.processUserCommand(cmd);
        ThreadHelper.safeSleep(waitTime);
        assert accountManager.getBalance(userA) == 99;
        assert accountManager.getBalance(userB) == 99;
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;

        System.out.println("\n[Test] Begin to send the fourth transaction");
        cmd = "sendct " + interactionId + " " + userD + commandSeparator + userA + commandSeparator + "1";
        commandServiceThread.processUserCommand(cmd);
        ThreadHelper.safeSleep(waitTime);
        assert accountManager.getBalance(userA) == 100;
        assert accountManager.getBalance(userB) == 100;
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;

        ThreadHelper.safeSleep(1* 1000);
    }

    @Test
    public void testSendCommonConditionalTransaction() {
        // First load the blockchain service
        MiningConfiguration.testSetRequiredZeroCount(1);
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        int initBlockchainSize = BlockChain.getInstance().getBlockList().size();
        BlockchainService.getInstance().getMiner().setRunInterval(1);

        // The following user accounts have been initialized in AccountManager's construction, please check if not
        String userA = "789001", userB = "789002", userC = "789003", userD = "789004";
        String commandSeparator = " ";
        int waitTime = 4 * 1000;
        int interactionId = 111;

        AccountManager accountManager = AccountManager.getInstance();
        assert accountManager.getBalance(userA) == 100;
        assert accountManager.getBalance(userB) == 100;
        assert accountManager.getBalance(userC) == 100;
        assert accountManager.getBalance(userD) == 100;

        System.out.println("\n[Test] Begin to send the first transaction");
        String cmd = "sendcomct 10081 CRTX_11;222:789002:789001:internalCoin:2:Agree;Agree;;CRTX_11;222:789002:789001:internalCoin:10:Agree;Agree 789001 789002 2";
        CommandServiceThread commandServiceThread = new CommandServiceThread();
        commandServiceThread.processUserCommand(cmd);
        ThreadHelper.safeSleep(waitTime);

        ThreadHelper.safeSleep(1* 1000);
    }

    @Test
    public void testCreateAsset() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        int initBlockchainSize = BlockChain.getInstance().getBlockList().size();

        BlockchainService.getInstance().getMiner().setRunInterval(1);
        MiningConfiguration.setDifficulty(0);

        String account = "0x123456";
        String assetType = "createTypeTest";
        int amount = 12345678;

        CommandServiceThread commandServiceThread = new CommandServiceThread();

        // create assetType amount account
        String commandLine = "create" + " " + assetType + " " + amount + " " + account;
        // sendex interactionId from to value reqChainId reqFrom reqTo reqValue
        commandServiceThread.processUserCommand(commandLine);

        ThreadHelper.safeSleep(3 * 1000);
        boolean found = false;
        while (!found) {
            List<Block> blockList = BlockChain.getInstance().getBlockList();
            for (int i = initBlockchainSize; i < blockList.size(); ++i) {
                Block b = blockList.get(i);
                for (AbstractTransaction t : b.getTransactions()) {
                    if (t instanceof AssetCreateTransaction) {
                        AssetCreateTransaction at = (AssetCreateTransaction) t;
                        if (at.getAccount().equals(account)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                if (found) break;
                ThreadHelper.safeSleep(1 * 1000);
            }
        }
        ThreadHelper.safeSleep(1 * 1000);
        System.out.println("[Test] The balance is: " + AccountManager.getInstance().getBalance(account, assetType));
        assert amount == AccountManager.getInstance().getBalance(account, assetType);

    }

    //2019-08-05
    @Test
    public void testSendMultipleTypeTransactionBetweenTwoPersonWithFixedTypeAndCreatedType() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        int initBlockchainSize = BlockChain.getInstance().getBlockList().size();

        MiningConfiguration.setDifficulty(0);
        BlockchainService.getInstance().getMiner().setRunInterval(1);

        int exchangeId = 10;
        String from1 = "78900_01";
        String from2 = "78900_02";
        String assetType1 = "assetType1";
        String assetType2 = "assetTypeCreate";
        int value1 = 10;
        int value2 = 20;

        int fromInitValue = 200;
        AccountManager.getInstance().reset();
        AccountManager.getInstance().addValue(from1, assetType1, fromInitValue);
        AccountManager.getInstance().addValue(from2, assetType2, fromInitValue);

        CommandServiceThread commandServiceThread = new CommandServiceThread();

        // 1->2 at 1 re at2; 2->1 at 2 re: at1
        // sendmt exchangeid <fix|var> from to assetType assetAmount "from:<to>:assetType:assetAmount;from:<to>:assetType:assetAmount;..."
        String commandLine = "sendmt 10 fixed 78900_01 78900_02  assetType1 10 78900_01:78900_02:assetType1:10;78900_02:78900_01:assetTypeCreate:20";
        // sendex interactionId from to value reqChainId reqFrom reqTo reqValue
        commandServiceThread.processUserCommand(commandLine);

        commandLine = "sendmt 10 fixed 78900_02 78900_01  assetTypeCreate 20 78900_01:78900_02:assetType1:10;78900_02:78900_01:assetTypeCreate:20";
        commandServiceThread.processUserCommand(commandLine);

        boolean found = false;
        while (!found) {
            List<Block> blockList = BlockChain.getInstance().getBlockList();
            for (int i = initBlockchainSize; i < blockList.size(); ++i) {
                Block b = blockList.get(i);
                for (AbstractTransaction t : b.getTransactions()) {
                    if (t instanceof MultiTypeExchangeTransaction) {
                        MultiTypeExchangeTransaction mt = (MultiTypeExchangeTransaction) t;
                        if (mt.getExchangeId() == exchangeId) {
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                if (found) break;
                ThreadHelper.safeSleep(1 * 1000);
            }
        }
        ThreadHelper.safeSleep(2 * 1000);
        System.out.println("[Test] The balance is: " + AccountManager.getInstance().getBalance(from1, assetType1));
        assert MultiTypeExchangeProcessor.getInstance().getProcessor(exchangeId).complete();
        assert fromInitValue - value1 == AccountManager.getInstance().getBalance(from1, assetType1);
        assert fromInitValue - value2 == AccountManager.getInstance().getBalance(from2, assetType2);

        assert value1 == AccountManager.getInstance().getBalance(from2, assetType1);
        assert value2 == AccountManager.getInstance().getBalance(from1, assetType2);
    }

    @Test
    public void testSendMultipleTypeTransactionBetweenTwoPersonWithFixedType() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        int initBlockchainSize = BlockChain.getInstance().getBlockList().size();

        MiningConfiguration.setDifficulty(1);
        BlockchainService.getInstance().getMiner().setRunInterval(1);

        int exchangeId = 10;
        String from1 = "78900_01";
        String from2 = "78900_02";
        String assetType1 = "assetType1";
        String assetType2 = "assetType2";
        int value1 = 10;
        int value2 = 20;

        int fromInitValue = 200;
        AccountManager.getInstance().reset();
        AccountManager.getInstance().addValue(from1, assetType1, fromInitValue);
        AccountManager.getInstance().addValue(from2, assetType2, fromInitValue);

        CommandServiceThread commandServiceThread = new CommandServiceThread();

        // 1->2 at 1 re at2; 2->1 at 2 re: at1
        // sendmt exchangeid <fix|var> from to assetType assetAmount "from:<to>:assetType:assetAmount;from:<to>:assetType:assetAmount;..."
        String commandLine = "sendmt 10 fixed 78900_01 78900_02  assetType1 10 78900_01:78900_02:assetType1:10;78900_02:78900_01:assetType2:20";
        // sendex interactionId from to value reqChainId reqFrom reqTo reqValue
        commandServiceThread.processUserCommand(commandLine);

        commandLine = "sendmt 10 fixed 78900_02 78900_01  assetType2 20 78900_01:78900_02:assetType1:10;78900_02:78900_01:assetType2:20";
        commandServiceThread.processUserCommand(commandLine);

        boolean found = false;
        while (!found) {
            List<Block> blockList = BlockChain.getInstance().getBlockList();
            for (int i = initBlockchainSize; i < blockList.size(); ++i) {
                Block b = blockList.get(i);
                for (AbstractTransaction t : b.getTransactions()) {
                    if (t instanceof MultiTypeExchangeTransaction) {
                        MultiTypeExchangeTransaction mt = (MultiTypeExchangeTransaction) t;
                        if (mt.getExchangeId() == exchangeId) {
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                if (found) break;
                ThreadHelper.safeSleep(1 * 1000);
            }
        }
        ThreadHelper.safeSleep(2 * 1000);
        System.out.println("[Test] The balance is: " + AccountManager.getInstance().getBalance(from1, assetType1));
        assert MultiTypeExchangeProcessor.getInstance().getProcessor(exchangeId).complete();
        assert fromInitValue - value1 == AccountManager.getInstance().getBalance(from1, assetType1);
        assert fromInitValue - value2 == AccountManager.getInstance().getBalance(from2, assetType2);

        assert value1 == AccountManager.getInstance().getBalance(from2, assetType1);
        assert value2 == AccountManager.getInstance().getBalance(from1, assetType2);
    }


    @Test
    public void testSendMultipleTypeTransactionBetweenTwoPersonWithFixedTypeWithWaitCondition() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        int initBlockchainSize = BlockChain.getInstance().getBlockList().size();

        MiningConfiguration.setDifficulty(1);
        BlockchainService.getInstance().getMiner().setRunInterval(1);

        int exchangeId = 10;
        String from1 = "78900_01";
        String from2 = "78900_02";
        String assetType1 = "assetType1";
        String assetType2 = "assetType2";
        int value1 = 10;
        int value2 = 20;

        int fromInitValue = 200;
        AccountManager.getInstance().reset();
        AccountManager.getInstance().addValue(from1, assetType1, fromInitValue);
        AccountManager.getInstance().addValue(from2, assetType2, fromInitValue);

        CommandServiceThread commandServiceThread = new CommandServiceThread();

        // 1->2 at 1 re at2; 2->1 at 2 re: at1
        // sendmt exchangeid <fix|var> from to assetType assetAmount "from:<to>:assetType:assetAmount;from:<to>:assetType:assetAmount;..."
        String commandLine = "sendmt 10 fixed 78900_01 78900_02  assetType1 10 78900_01:78900_02:assetType1:10;78900_02:78900_01:assetType2:20;wait:2";
        // sendex interactionId from to value reqChainId reqFrom reqTo reqValue
        commandServiceThread.processUserCommand(commandLine);

        commandLine = "sendmt 10 fixed 78900_02 78900_01  assetType2 20 78900_01:78900_02:assetType1:10;78900_02:78900_01:assetType2:20;shell:.\\test\\conditionContract\\outputFile:true:false";
        commandServiceThread.processUserCommand(commandLine);

        boolean found = false;
        while (!found) {
            List<Block> blockList = BlockChain.getInstance().getBlockList();
            for (int i = initBlockchainSize; i < blockList.size(); ++i) {
                Block b = blockList.get(i);
                for (AbstractTransaction t : b.getTransactions()) {
                    if (t instanceof MultiTypeExchangeTransaction) {
                        MultiTypeExchangeTransaction mt = (MultiTypeExchangeTransaction) t;
                        if (mt.getExchangeId() == exchangeId) {
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                if (found) break;
                ThreadHelper.safeSleep(1 * 1000);
            }
        }
        ThreadHelper.safeSleep(6 * 1000);
        System.out.println("[Test] The balance is: " + AccountManager.getInstance().getBalance(from1, assetType1));
        assert MultiTypeExchangeProcessor.getInstance().getProcessor(exchangeId).complete();
        assert fromInitValue - value1 == AccountManager.getInstance().getBalance(from1, assetType1);
        assert fromInitValue - value2 == AccountManager.getInstance().getBalance(from2, assetType2);

        assert value1 == AccountManager.getInstance().getBalance(from2, assetType1);
        assert value2 == AccountManager.getInstance().getBalance(from1, assetType2);
    }

    @Test
    public void testSendExternalTransactionFromFile() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        int initBlockchainSize = BlockChain.getInstance().getBlockList().size();

        BlockchainService.getInstance().getMiner().setRunInterval(1);

        String from = "56799", to = "89034", reqFrom = "389056", reqTo = "903456";
        int interactionId = 12, value = 10, reqChainId = 333, reqValue = 10;
        int fromInitAddValue = 20;
        double fromInitValue = fromInitAddValue + AccountManager.getInstance().getBalance(from);
        AccountManager.getInstance().addValue(from, fromInitAddValue);
        assert fromInitValue - AccountManager.getInstance().getBalance(from) < 0.001d;

        CommandServiceThread commandServiceThread = new CommandServiceThread();

        commandServiceThread.setCommandInputFromFile(true);
        String commandContent = "sendex" + " " + interactionId + " " + from + " " + to
                + " " + value + " " + reqChainId + " " + reqFrom + " " + reqTo + " " + reqValue;
        String commandContent2 = "sendex" + " " + (interactionId + 3) + " " + from + " " + to
                + " " + value + " " + reqChainId + " " + reqFrom + " " + reqTo + " " + reqValue;
        FileHelper.createFolderIfNotExist(CommandServiceThread.getCommandFileFolder());
        FileHelper.createFile(CommandServiceThread.getFileFullPathName(), commandContent + "\n" + commandContent2);
        assert FileHelper.loadAssetFromFile(CommandServiceThread.getFileFullPathName()).contains("sendex");
        // sendex interactionId from to value reqChainId reqFrom reqTo reqValue
        assert commandServiceThread.processUserCommand();

        ThreadHelper.safeSleep(500);
        assert commandServiceThread.processUserCommand();

        ThreadHelper.safeSleep(3 * 1000);
        boolean found = false;
        int maxTryAccount = 30;// 30 round
        int triedAccount = 0;
        for (triedAccount = 0; triedAccount < maxTryAccount; ++triedAccount) {
            List<Block> blockList = BlockChain.getInstance().getBlockList();
            for (int i = initBlockchainSize; i < blockList.size(); ++i) {
                Block b = blockList.get(i);
                for (AbstractTransaction t : b.getTransactions()) {
                    if (t instanceof CrosschainInterface) {
                        CrosschainTransaction ct = (CrosschainTransaction) t;
                        if (ct.getBlockchainId() == MiningConfiguration.getBlockchainStringId() && ct.getInteractionId() == interactionId) {
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }
                if (found) break;
            }
            if (found) break;
            ThreadHelper.safeSleep(1 * 1000);
        }
        System.out.println("[Test] the balance is: " + AccountManager.getInstance().getBalance(from));
        assert triedAccount < maxTryAccount;
        assert fromInitValue - value == AccountManager.getInstance().getBalance(from);
    }

    @Test
    public void processSendDynamicalTransaction() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        int initBlockchainSize = BlockChain.getInstance().getBlockList().size();

        BlockchainService.getInstance().getMiner().setRunInterval(1);

        CommandServiceThread commandServiceThread = new CommandServiceThread();

        String className = "DynamicalAsset";
        String keyValueList = "";
        keyValueList += "k1:v1";
        keyValueList += ";k2:v2";
        keyValueList += ";k3:v3";
        ;

        String code = "";
        int gas = 10000;
        // senddt gas classname key:value;key:value "code"
        String commandLine = "senddt" + " " + gas + " " + className + " " + keyValueList + " " + code;
        commandServiceThread.processUserCommand(commandLine);

        // with code
        code = "";
        className = "AssetT11";
        FileHelper.deleteFile(className + ".java");
        code += "public class " + className + " extends DynamicalAsset{" + AssetCompiler.getLinechangerReplacement();
        code += "    @Override" + AssetCompiler.getLinechangerReplacement();
        code += "    public boolean check(){" + AssetCompiler.getLinechangerReplacement();
        code += "        return true;" + AssetCompiler.getLinechangerReplacement();
        code += "    }" + AssetCompiler.getLinechangerReplacement();
        code += "}" + AssetCompiler.getLinechangerReplacement();
        commandLine = "senddt" + " " + gas + " " + className + " " + keyValueList + " " + code;
        commandServiceThread.processUserCommand(commandLine);

        ThreadHelper.safeSleep(3 * 1000);
        boolean found = false;
        while (!found) {
            List<Block> blockList = BlockChain.getInstance().getBlockList();
            for (int i = initBlockchainSize; i < blockList.size(); ++i) {
                Block b = blockList.get(i);
                for (AbstractTransaction t : b.getTransactions()) {
                    if (AssetCompiler.getClassName(t.getClass().getName()).equals(className)) {
                        found = true;
                        break;
                    }
                }
                if (found) break;
                ThreadHelper.safeSleep(1 * 1000);
            }
        }

        ThreadHelper.safeSleep(3 * 1000);
    }

    @Test
    public void processOverrideAndInitTransaction() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        int initBlockchainSize = BlockChain.getInstance().getBlockList().size();

        BlockchainService.getInstance().getMiner().setRunInterval(1);

        CommandServiceThread commandServiceThread = new CommandServiceThread();

        String className = "DynamicalAsset";
        String keyValueList = "";
        keyValueList += "k1:v1";
        keyValueList += ";k2:v2";
        keyValueList += ";k3:v3";
        ;

        String code = "";
        int gas = 10000;
        // senddt gas classname key:value;key:value "code"

        initBlockchainSize = BlockChain.getInstance().getBlockList().size();
        // with code
        code = "";
        className = "AssetT11";
        code += "public class " + className + " extends DynamicalAsset{" + AssetCompiler.getLinechangerReplacement();
        code += "    @Override" + AssetCompiler.getLinechangerReplacement();
        code += "    public boolean check(){" + AssetCompiler.getLinechangerReplacement();
        code += "        return true;" + AssetCompiler.getLinechangerReplacement();
        code += "    }" + AssetCompiler.getLinechangerReplacement();
        code += "}" + AssetCompiler.getLinechangerReplacement();
        String commandLine = "senddoit" + " " + gas + " " + className + " " + keyValueList + " " + code;
        commandServiceThread.processUserCommand(commandLine);

        ThreadHelper.safeSleep(3 * 1000);
        boolean found = false;
        while (!found) {
            List<Block> blockList = BlockChain.getInstance().getBlockList();
            for (int i = initBlockchainSize; i < blockList.size(); ++i) {
                Block b = blockList.get(i);
                for (AbstractTransaction t : b.getTransactions()) {
                    if (AssetCompiler.getClassName(t.getClass().getName()).contains(className) &&
                            !AssetCompiler.getClassName(t.getClass().getName()).equals(className)) {
                        found = true;
                        break;
                    }
                }
                if (found) break;
                ThreadHelper.safeSleep(1 * 1000);
            }
        }

        ThreadHelper.safeSleep(3 * 1000);
    }

    @Test
    public void processDefinitionAndInitTransaction() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);

        BlockChain.getInstance().testRest();
        BlockchainService.getInstance().getMiner().setRunInterval(1);

        CommandServiceThread commandServiceThread = new CommandServiceThread();

        // with code
        String className = "AssetT24";
        FileHelper.deleteFile(className + ".java");
        String commandLine = "senddt owner 10000 AssetT24 k1:v1;k2:v2 data import com.scu.suhong.block.BlockChain;::: public class AssetT24 extends DynamicalAsset{:::  public AssetT24 getIntstance(){:::      return (AssetT24) BlockChain.getInstance().getGlobalAssetInstance(\"AssetT24\");:::    }:::  @Override:::   public boolean check(){:::       String ownerKey = \"nextOwner\";:::       String owner = getIntstance().getValueByKey(ownerKey);:::       if (null == owner ||  owner.isEmpty()) return true;:::  System.out.println(\"**** \" + owner + \" vs \" + getOwner() );:::    return owner.equals(getOwner());:::   }:::  @Override:::   public void postAction() {:::       String ownerKey = \"nextOwner\";:::       String currentOwner = getIntstance().getValueByKey(ownerKey);:::       String nextOwner = \"\";:::       if (currentOwner.isEmpty()) nextOwner = \"owner\" + 1;:::       else nextOwner = currentOwner + 1;:::       getIntstance().addKeyValue(\"nextOwner\", nextOwner);:::   }:::    }:::";
        commandServiceThread.processUserCommand(commandLine);

        ThreadHelper.safeSleep(3 * 1000);
        int lastFindPos = 0;
        boolean found = false;
        while (!found) {
            List<Block> blockList = BlockChain.getInstance().getBlockList();
            int blockListSize = blockList.size();
            for (int i = lastFindPos; i < blockListSize; ++i) {
                Block b = blockList.get(i);
                for (AbstractTransaction t : b.getTransactions()) {
                    if (!(t instanceof DynamicalAsset)) continue;
                    if (((DynamicalAsset) t).getCode().contains(className)) {
                        found = true;
                        break;
                    }
                }
                if (found) break;
                ThreadHelper.safeSleep(1 * 1000);
            }
            lastFindPos = blockListSize;
        }

        //ThreadHelper.safeSleep(1000);
        AssetCompiler assetCompiler = new AssetCompiler();
        DynamicalAsset dynamicalAsset = (DynamicalAsset) assetCompiler.getInstance(className);
        assert dynamicalAsset.check();

        commandLine = "senddit owner 10000 AssetT24 book:red_sea;number:3 OKContent";
        commandServiceThread.processUserCommand(commandLine);

        found = false;
        while (!found) {
            ThreadHelper.safeSleep(2000);
            found = true;
        }

        dynamicalAsset = (DynamicalAsset) assetCompiler.getInstance(className);
        assert !dynamicalAsset.check();
        commandLine = "senddit owner 10000 AssetT24 book:red_sea;number:3 OKContent";
        commandServiceThread.processUserCommand(commandLine);

        found = false;
        while (!found) {
            ThreadHelper.safeSleep(2000);
            found = true;
        }

        dynamicalAsset = (DynamicalAsset) assetCompiler.getInstance(className);
        assert !dynamicalAsset.check();
    }

    @Test
    /*
     * We use the check whether the sender is sepcial account to do this
     * The original account is inited by the contract
     * */
    public void processDynamicalCheckTransaction() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        int initBlockchainSize = BlockChain.getInstance().getBlockList().size();

        BlockchainService.getInstance().getMiner().setRunInterval(1);

        CommandServiceThread commandServiceThread = new CommandServiceThread();

        String keyValueList = "";
        keyValueList += "k1:v1";
        keyValueList += ";k2:v2";
        keyValueList += ";k3:v3";
        ;

        String code = "";
        int gas = 10000;
        // senddt gas classname key:value;key:value "code"

        initBlockchainSize = BlockChain.getInstance().getBlockList().size();
        // with code
        code = "";
        String className = "AssetT11";
        FileHelper.deleteFile(className + ".java");
        code += "public class " + className + " extends DynamicalAsset{" + AssetCompiler.getLinechangerReplacement();
        code += "    @Override" + AssetCompiler.getLinechangerReplacement();
        code += "    public boolean check(){" + AssetCompiler.getLinechangerReplacement();
        code += "        return true;" + AssetCompiler.getLinechangerReplacement();
        code += "    }" + AssetCompiler.getLinechangerReplacement();
        code += "    public AssetT11 getIntstance(){" + AssetCompiler.getLinechangerReplacement();
        code += "        return (" + className + ")" + AssetCompiler.getAssetInstanceSymbol() + ";" + AssetCompiler.getLinechangerReplacement();
        code += "    }" + AssetCompiler.getLinechangerReplacement();
        code += "}" + AssetCompiler.getLinechangerReplacement();
        String owner = "owner";
        String data = "data";
        String commandLine = "senddt" + " " + owner + " " + gas + " " + className + " " + keyValueList + " " + data + " " + code;
        commandServiceThread.processUserCommand(commandLine);

        ThreadHelper.safeSleep(3 * 1000);
        boolean found = false;
        DynamicalAsset dynamicalAssetInBlockchain = null;
        while (!found) {
            List<Block> blockList = BlockChain.getInstance().getBlockList();
            for (int i = initBlockchainSize; i < blockList.size(); ++i) {
                Block b = blockList.get(i);
                for (AbstractTransaction t : b.getTransactions()) {
                    if (AssetCompiler.getClassName(t.getClass().getName()).contains(className) &&
                            !AssetCompiler.getClassName(t.getClass().getName()).equals(className)) {
                        dynamicalAssetInBlockchain = (DynamicalAsset) t;
                        assert t.getGas() == gas;
                        assert t.getOwner().equals(owner);
                        assert t.getData().equals(data);
                        assert ((DynamicalAsset) t).getCode().equals(code);
                        break;
                    }
                }
                if (null != dynamicalAssetInBlockchain) break;
                ThreadHelper.safeSleep(1 * 1000);
            }
        }

        ThreadHelper.safeSleep(3 * 1000);
    }
	
	
    @Test
    public void testSendSmartContractLifecycleTransaction() throws InterruptedException {
        String codeName = "TestPuttingCode"; // we use Java as smart contract language
        String instanceName = "instance1"; // we use Java as smart contract language
        String methodName = "method1";
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        CommandServiceThread commandServiceThread = new CommandServiceThread();
        Thread commandThread = new Thread(commandServiceThread, "test command chain service thread");
        commandThread.start();
        Thread.sleep(2000); // sleep a while to wait for the miner to be initialized
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        BlockchainService.getInstance().getMiner().setRunInterval(1);
        MiningConfiguration.setDifficulty(0);

        LifecycleHandler.clean(codeName);
        assert !LifecycleHandler.getInstance().hasInstance(codeName);
        String command = "send lck:civ:n#TestPuttingCode,c#cHVibGljIGNsYXNzIFRlc3RQdXR0aW5nQ29kZSB7CmludCBpID0gMDsKcHVibGljIHZvaWQgbWV0aG9kMSgpewpTeXN0ZW0ub3V0LnByaW50ZigiW1Rlc3RdW0lORk9dWyoqKioqKioqKioqXSBtZXRob2QxIGluIFRlc3RQdXR0aW5nQ29kZS4gVGVzdCBmdW5jdGlvbiBjYWxsZWQgc3VjY2Vzc2Z1bGx5IVxuIik7Cn0KfQo=,i#instance1,m#method1 789001 789002 0 gas#10000000";
        commandServiceThread.processUserCommandWithException(command);

        Thread.sleep(3000); // TO DO add check
        assert LifecycleHandler.getInstance().hasInstance(codeName);
    }

    @Test
    /*
     * Trigger internal smart contract
     * */
    public void processTriggerInternalSmartContract() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);
        int initBlockchainSize = BlockChain.getInstance().getBlockList().size();

        BlockchainService.getInstance().getMiner().setRunInterval(1);

        CommandServiceThread commandServiceThread = new CommandServiceThread();

        String statecheatFileName = "statecheat";
        String stateToCheat = "state3";

        FileHelper.deleteFile(statecheatFileName);
        FileHelper.createFile(statecheatFileName, stateToCheat);

        String commandLine = "isc" + " " + "NBSState" + " " + "127.0.0.1:8080#5#h";
        commandServiceThread.processUserCommand(commandLine);

        ThreadHelper.safeSleep(3 * 1000);
    }

    @Test
    /*
     * Trigger internal smart contract
     * */
    public void processTriggerInternalSmartContractWithRandomDiscard() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);

        BlockchainService.getInstance().getMiner().setRunInterval(1);
        CommandServiceThread commandServiceThread = new CommandServiceThread();

        String statecheatFileName = "statecheat";
        FileHelper.deleteFile(statecheatFileName);

        System.out.println("[Test] Random discard with positive discard");
        String commandLine = "isc" + " " + "NBSState" + " " + "127.0.0.1:8080#5#hr#3";
        commandServiceThread.processUserCommand(commandLine);

        System.out.println("[Test] Random discard with negative discard");
        commandLine = "isc" + " " + "NBSState" + " " + "127.0.0.1:8080#5#hr#-3";
        commandServiceThread.processUserCommand(commandLine);

        ThreadHelper.safeSleep(3 * 1000);
    }

    @Test
    /*
     * Trigger internal smart contract
     * */
    public void processTriggerInternalSmartContractWithMixMode() {
        // First load the blockchain service
        BlockchainServiceThread blockchainServiceThread = new BlockchainServiceThread();
        Thread blockThread = new Thread(blockchainServiceThread, "test block chain service thread");
        blockThread.start();

        ThreadHelper.safeSleep(1 * 1000);
        while (null == BlockchainService.getInstance().getMiner()) ThreadHelper.safeSleep(500);

        BlockchainService.getInstance().getMiner().setRunInterval(1);
        CommandServiceThread commandServiceThread = new CommandServiceThread();

        String statecheatFileName = "statecheat";
        FileHelper.deleteFile(statecheatFileName);

        System.out.println("[Test] Random discard with positive discard");
        String commandLine = "isc" + " " + "NBSState" + " " + "127.0.0.1:8080#10#m#5";
        commandServiceThread.processUserCommand(commandLine);

        ThreadHelper.safeSleep(3 * 1000);
    }

    private void sendBatchData(CommandServiceThread commandServiceThread) throws InterruptedException {
        //bs <asset type> <number> <interval>
        commandServiceThread.processUserCommand("bs big_data 10 100");
        Thread.sleep(1000); // sleep a while
    }

    private void sendBigData(CommandServiceThread commandServiceThread) throws InterruptedException {
        // send data from to value <asset type>
        commandServiceThread.processUserCommand("send big_data from to 1");
        Thread.sleep(1000); // sleep a while
    }

    private void exitCommand(CommandServiceThread commandServiceThread) throws InterruptedException {
        commandServiceThread.processUserCommand("exit");
        Thread.sleep(1000); // sleep a while
    }

    private void dumpCommand(CommandServiceThread commandServiceThread) throws InterruptedException {
        commandServiceThread.processUserCommand("dump");
        Thread.sleep(1000); // sleep a while
    }

    @Test
    public void testParseCommandLine() {
        CommandServiceThread commandServiceThread = new CommandServiceThread();
        Command command = commandServiceThread.parseCommandLine("send big_dataasset trtafficdata1");
        assert (CommandType.ESendTransaction == command.commandType);
        assert (2 == command.arguments.size());

        command = commandServiceThread.parseCommandLine("dump");
        assert (CommandType.EDumpBlock == command.commandType);

        command = commandServiceThread.parseCommandLine("dumpex");
        assert (CommandType.EDumpExBlock == command.commandType);

        command = commandServiceThread.parseCommandLine("exit");
        assert (CommandType.EStopService == command.commandType);
    }

    @Test
    public void testListToArray() {
        List<String> stringList = new ArrayList<>();
        stringList.add("abc");
        stringList.add("cd");
        stringList.add("ef");
        System.out.println(stringList.toString());
        System.out.println(stringList);
    }

    // For the possibility test
    public void testMinePossibility() {
        // test 10 times
        for (int i = 0; i < testTimes; ++i) {
            doTestMinePossibility();
            ThreadHelper.safeSleep(2345);
        }
    }

    private void doTestMinePossibility() {
        List<TxInfor> Txs = new ArrayList<>();
        for (int i = 0; i < stdQueueSize; ++i) {
            Txs.add(new TxInfor(0, 0));
        }

        int lastProcessed = 0;
        for (int i = 0; i < stdQueueSize * stdQueueSize; ++i) {
            addProducedTxs(Txs, produceTxs(i, 2));// 2 assure each mining queue has the targeted Tx
            lastProcessed += miningTxs(Txs, speed, i);
        }
        System.out.println("[TEST] last processed: " + lastProcessed);
    }

    // For the possibility test
    public void testAllPossibility() {
        // test 10 times
        for (int i = 0; i < testTimes; ++i) {
            doTestAllPossibility();
            ThreadHelper.safeSleep(2345);
        }
    }

    private void doTestAllPossibility() {
        List<TxInfor> Txs = new ArrayList<>();
        for (int i = 0; i < stdQueueSize; ++i) {
            Txs.add(new TxInfor(0, 0));
        }

        int lastProcessed = 0;
        for (int i = 0; i < stdQueueSize * stdQueueSize; ++i) {
            addProducedTxs(Txs, produceTxs(i));
            lastProcessed += miningTxs(Txs, speed, i);
        }
        System.out.println("[TEST] last processed: " + lastProcessed);
    }

    private int miningTxs(List<TxInfor> txs, int amount, int sequence) {
        RandomHelper randomHelper = new RandomHelper();
        int txsSize = txs.size();
        int flagArray[] = new int[txsSize];
        for (int i = 0; i < txsSize; ++i) flagArray[i] = 0;

        int result = 0;
        int random = 0;
        for (int i = 0; i < amount; ++i) {
            do {
                random = randomHelper.getNumber(txsSize - i);
            } while (flagArray[random] != 0);
            flagArray[random] = 1;
            TxInfor tf = txs.remove(random);
            if (tf.sequence == sequence && tf.number == 2) {
                ++result;
                System.out.printf("Sequence: %d mined OK\n", sequence);
            }
        }
        return result;
    }

    private void addProducedTxs(List<TxInfor> toTxs, List<TxInfor> fromTxs) {
        for (TxInfor tf : fromTxs
        ) {
            toTxs.add(tf);
        }
    }

    private List<TxInfor> produceTxs(int sequence, int specificNumber) {
        List<TxInfor> Txs = new ArrayList<>();
        RandomHelper randomHelper = new RandomHelper();
        int r = 0;
        int testArray[] = new int[stdQueueSize];
        for (int i = 0; i < stdQueueSize; ++i) testArray[i] = 0;
        boolean isSpecificNumberCreated = false;
        for (int i = 0; i < speed; ++i) {
            do {
                r = randomHelper.getNumber(stdQueueSize);
            } while (testArray[r] != 0);
            testArray[r] = 1;
            if (specificNumber == r) {
                System.out.println("[TEST] produce OK: " + sequence);
                isSpecificNumberCreated = true;
            }
            Txs.add(new TxInfor(sequence, r));
        }
        if (!isSpecificNumberCreated) {
            r = randomHelper.getNumber(speed);
            Txs.set(r, new TxInfor(sequence, specificNumber));
            isSpecificNumberCreated = true;
        }
        return Txs;
    }

    private List<TxInfor> produceTxs(int sequence) {
        List<TxInfor> Txs = new ArrayList<>();
        RandomHelper randomHelper = new RandomHelper();
        int r = 0;
        int testArray[] = new int[stdQueueSize];
        for (int i = 0; i < stdQueueSize; ++i) testArray[i] = 0;
        for (int i = 0; i < speed; ++i) {
            do {
                r = randomHelper.getNumber(stdQueueSize);
            } while (testArray[r] != 0);
            testArray[r] = 1;
            if (2 == r) {
                System.out.println("[TEST] produce OK: " + sequence);
            }
            Txs.add(new TxInfor(sequence, r));
        }
        return Txs;
    }

    class TxInfor {
        public int sequence;
        public int number;

        public TxInfor(int sequence, int number) {
            this.sequence = sequence;
            this.number = number;
        }
    }
}
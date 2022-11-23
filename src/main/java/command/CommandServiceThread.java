package command;

import Service.BlockchainService;
import account.AccountManager;
import asset.service.AssetService;
import asset.service.AssetServiceThread;
import com.scu.suhong.block.BlockDBHandler;
import com.scu.suhong.block.BlockchainFileDumper;
import com.scu.suhong.miner.MinerSetting;
import com.scu.suhong.network.P2PConfiguration;
import com.scu.suhong.sync.BlockchainSyncService;
import org.jetbrains.annotations.Nullable;
import regulator.IncreaseRegulator;
import regulator.IncreaseType;
import regulator.RegulationType;
import regulator.Regulator;
import util.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CommandServiceThread implements Runnable {
    private static final int commandFileReadInterval = 1000;
    BlockchainService blockchainService;
    AssetServiceThread assetServiceThread;
    BlockchainSyncService blockchainSyncService;
    boolean forceStop = false;
    IncreaseRegulator increaseRegulator;
    boolean isComandInputFromFile;

    public CommandServiceThread() {
        // Unify the following
        blockchainService = BlockchainService.getInstance();
        assetServiceThread = AssetServiceThread.getInstance();
        blockchainSyncService = BlockchainSyncService.getInstance();
        forceStop = false;
        isComandInputFromFile = false;
    }

    public static String getFileFullPathName() {
        return getCommandFileFolder() + File.separator + getCommandFileName();
    }

    public static String getCommandFileFolder() {
        return "command";
    }

    public static String getCommandFileName() {
        return "command";
    }

    static public void generateAndSend(IncreaseRegulator increaseRegulator
            , BlockchainService blockchainService
            , String assetType
            , String batchTxStart) throws IOException {
        if (null != increaseRegulator && !increaseRegulator.canSendNext()) return;

        List<String> arguments = new ArrayList<>();
        String transactionData = batchTxStart + "-" +
                StringHelper.generateRandomString(340, 3);
        arguments.add(transactionData);
        arguments.add("0xabc");
        arguments.add("0xabd");
        arguments.add("12");
        System.out.println("[CMD] Tx content: " + transactionData);
        if (null != assetType && !assetType.isEmpty()) {
            arguments.add(assetType);
        }
        blockchainService.triggerTransaction(arguments);
    }

    static public void generateAndSend(BlockchainService blockchainService
            , String assetType
            , String batchTxStart) throws IOException {
        List<String> arguments = new ArrayList<>();
        String transactionData = " [RandomProcess] " + blockchainService.getMiner().getMinedProcessCount()
                + " " + batchTxStart + "-" +
                StringHelper.generateRandomString(340, 3);
        arguments.add(transactionData);
        arguments.add("0xabc");
        arguments.add("0xabd");
        arguments.add("12");
        System.out.println("[CMD] Tx content: " + transactionData);
        if (null != assetType && !assetType.isEmpty()) {
            arguments.add(assetType);
        }
        blockchainService.triggerTransaction(arguments);
    }

    public void setCommandInputFromFile(boolean comandInputFromFile) {
        isComandInputFromFile = comandInputFromFile;
    }

    @Override
    public void run() {
        System.out.println("[CommandServiceThread] Begin to prepare to process comand from " + (isComandInputFromFile ? "file" : "command line"));
        while (null != BlockchainService.getInstance().getMiner())
            ThreadHelper.safeSleep(2000);// have to wait until the miner is been up
        System.out.println("[CommandServiceThread] Process comand from " + (isComandInputFromFile ? "file" : "command line"));
        do {
            if (!processUserCommand() || forceStop) {
                break;
            }
        } while (true);
    }

    public boolean processUserCommand() {
        if (isComandInputFromFile) {
            ThreadHelper.safeSleep(commandFileReadInterval);
            return processUserCommandFromFile();
        } else return processUserCommandFromCommandLine();
    }

    public boolean processUserCommandFromCommandLine() {
        System.out.println("> ");
        Scanner scanner = new Scanner(System.in);
        String commandLine;
        try {
            commandLine = scanner.nextLine();
        } catch (Exception e) {
            System.out.println("[CommandServiceThread] command scan error");
            e.printStackTrace();
            return false;
        }
        return processUserCommand(commandLine);
    }

    public boolean processUserCommandFromFile() {
        String commandFileFolder = getCommandFileFolder();
        String fileFullPathName = getFileFullPathName();
        String errorFileFullPathName = getFileFullPathName() + "_error";
        FileHelper.createFolderIfNotExist(commandFileFolder);
        String commandContent = FileHelper.loadContentFromFile(fileFullPathName, false);
        if (commandContent.isEmpty()) {
            return true;
        }


        if (processAllCommand(commandContent)) {
            System.out.println("[CommandServiceThread] Succeed to process command from file. Try to delete the original file");
            FileHelper.deleteFile(fileFullPathName);
        } else {
            try {
                Files.move(Paths.get(fileFullPathName), Paths.get(errorFileFullPathName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    boolean processAllCommand(String commandLines){
        if (null == commandLines || commandLines.isEmpty()) return false;

        String lines[] = commandLines.split("\\r?\\n");
        for (String line : lines){
            if (null == line || line.isEmpty()) continue;
            if (processUserCommand(line)) {
                System.out.println("[CommandServiceThread] Succeed to process command from file. " + line);
            } else {
                System.out.printf("[CommandServiceThread][ERROR] Failed to process command %s from file. Skip to process further\n", line);
                return false;
            }
        }

        return true;
    }

    public boolean processUserCommand(String commandLine) {
        try {
            return processUserCommandWithException(commandLine);
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean processUserCommandWithException(String commandLine) {
        Command command;
        command = parseCommandLine(commandLine);
        if (!doCommand(command)) {
            return false;
        }
        return true;
    }

    // send transaction
    // dump
    // exit
    public Command parseCommandLine(String commandLine) {
        Command command = new Command();
        System.out.println(commandLine);
        commandLine = commandLine.trim(); // remove the space in begin and end
        List<String> commandList = StringHelper.splitCommandString(commandLine);
        String commandName = commandList.get(0);
        command.arguments = commandList.subList(1, commandList.size());//remove command name
        if (commandName.equals("send")) { // send data from to value <asset type>
            command.commandType = CommandType.ESendTransaction;
        } else if (commandName.equals("sendiTx")) { // sendiTx data from to value <asset type>
            command.commandType = CommandType.ESendInternalTransaction;
        } else if (commandName.equals("sendcomct")) { // sendcomct data from to value <asset type>
            command.commandType = CommandType.ESendCommonCrosschainTransaction;
        } else if (commandName.equals("sendct")) { // sendct interactionId from to value - sendct interactionId from to1_value1_to2_value2... totalValue
            command.commandType = CommandType.ESendConditionalTransaction;
        } else if (commandName.equals("sendss")) { // sendss smart contractName varietyNamme value
            command.commandType = CommandType.ESendStateSyncTransaction;
        } else if (commandName.equals("sendex")) { // sendex interactionId from to value reqChainId reqFrom reqTo reqValue
            command.commandType = CommandType.ESendExternalTransaction;
        } else if (commandName.equals("sendmt")) {
            // sendmt exchangeid <fixed|variable> from to assetType assetAmount "from:<to>:assetType:assetAmount;wait:<number>;shell <command result oppositeResult>;..."
            command.commandType = CommandType.ESendMultipleTypeTransaction;
        } else if (commandName.equals("sendcmt")) {
            // sendcmt exchangeid <fixed|variable> from to assetType assetAmount "requiredChainID:from:<to>:assetType:assetAmount;requiredChainID:from:<to>:assetType:assetAmount;..." data
            command.commandType = CommandType.ESendCrosschainMultipleTypeTransaction;
        } else if (commandName.equals("senddt")) {//send dynamic definition transaction
            // senddt owner gas classname key:value;key:value "data" "code"
            command.commandType = CommandType.ESendDynamicalTransaction;
        } else if (commandName.equals("senddoit")) {//send dynamic override and init transaction
            // senddoit owner gas classname key:value;key:value "data" "code"
            command.commandType = CommandType.ESendOverrideAndInitTransaction;
        } else if (commandName.equals("senddit")) {//send dynamic init transaction
            // senddit owner gas classname key:value;key:value "data"
            command.commandType = CommandType.ESendDynamicalInitTransaction;
        } else if (commandName.equals("isc")) {// try to run internal smart contract
            //isc <smart contract name> <parameters joined by #>
            command.commandType = CommandType.EInternalSmartContract;
        } else if (commandName.equals("create")) { // create assetType amount account
            command.commandType = CommandType.ECreateAsset;
        } else if (commandName.equals("bs")) { // bs <asset type> <number> <interval-ms> <msg begin>
            command.commandType = CommandType.EBatchSendTransaction;
        } else if (commandName.equals("gb")) { // gb address - get balance of address
            command.commandType = CommandType.EGetBalance;
        } else if (commandName.equals("bsr")) { // bsr <asset type> <number> <mean wait time> <deviation time> <msg begin>
            command.commandType = CommandType.EBatchRandomSendTransaction;
        } else if (commandName.equals("bsrp")) { // bsrp <asset type> <number> <mean arrive number> <msg begin>
            command.commandType = CommandType.EBatchRandomSendPoissonTransaction;
        } else if (commandName.equals("ibs")) { // ibs <asset type> <increase times> <increase number> <msg begin>
            command.commandType = CommandType.EBatchIncreaseSendTransaction;
        } else if (commandName.equals("dump")) { // dump or dump number
            command.commandType = CommandType.EDumpBlock;
        } else if (commandName.equals("dumpex")) { // dumpex or dumpex number
            command.commandType = CommandType.EDumpExBlock;
        } else if (commandName.equals("dumpDB")) {
            command.commandType = CommandType.EDumpBlockDB;
        } else if (commandName.equals("exit")) {
            command.commandType = CommandType.EStopService;
        } else if (commandName.equals("buyer")) {
            command.commandType = CommandType.ESetBuyer;
        } else if (commandName.equals("reg")) { // reg s/a/sa/off/on p1 p2     reg s p1 assetType
            command.commandType = CommandType.EReg;
        } else if (commandName.equals("nsp")) { // nsp <mean simulate delay value> <deviation value> - network send parameter
            command.commandType = CommandType.ENSP;
        } else if (commandName.equals("msp")) { // msp <mean simulate delay value> <deviation value> - miner setting parameter
            command.commandType = CommandType.EMSP;
        } else if (commandName.equals("setacp")) {
            // - set parameters for action of ACP, string 'null' is for empty
            // setacp sender actionType, incomingName ,incomingOwner, incomingBlockchainId, outgoingName, successiveActionMaxWaitingTime, successiveActionTotalPeerNumber successiveActionMaxAllowedPeerNumber
            command.commandType = CommandType.SETACP;
        } else if (commandName.equals("sendacptrigger")) {
            //Used often to trigger some self triggered action, such as in the same blockchain
            // send acp condition transaction (trigger transaction) to trigger an action (manually)
            // sendacptrigger incomingName maxWaitingTime totalSuccessiveActionNumber maxAllowedActionNumber data sender
            command.commandType = CommandType.SENDACPTRIGGER;
        } else if (commandLine.isEmpty()) {
            command.commandType = CommandType.EEmpty;
        } else {
            System.out.printf("[CommandServiceThread][ERROR] Failed to process unknown type command %s", commandLine);
        }
        return command;
    }

    public boolean doCommand(Command command) {
        switch (command.commandType) {
            case ESendTransaction: {
                return processSingleSend(command);
            }
            case ESendInternalTransaction: {
                return processSingleSend(command, true);
            }
            case ESendCommonCrosschainTransaction: {
                return processCommonCrosschainSend(command);
            }
            case ESendConditionalTransaction: {
                return processSendConditionalTransaction(command);
            }
            case ESendStateSyncTransaction: {
                return processSendStateSyncTransaction(command);
            }
            case ESendExternalTransaction: {
                return processSendExternalTransaction(command);
            }
            case ESendMultipleTypeTransaction: {
                return processSendMultipleTypeTransaction(command);
            }
            case ESendCrosschainMultipleTypeTransaction: {
                return processSendCrosschainMultipleTypeTransaction(command);
            }
            case ESendDynamicalTransaction: {
                return processSendDynamicalTransaction(command);
            }
            case ESendOverrideAndInitTransaction: {
                return processOverrideAndInitTransaction(command);
            }
            case ESendDynamicalInitTransaction: {
                return processDynamicalInitTransaction(command);
            }
            case EInternalSmartContract: {
                return processInternalSmartContract(command);
            }
            case EBatchSendTransaction: {
                return processBatchSend(command);
            }
            case ECreateAsset: {
                return processCreateAsset(command);
            }
            case EGetBalance: {
                return processGetBalance(command);
            }
            case EBatchRandomSendTransaction: {
                return processBatchRandomSend(command);
            }
            case EBatchRandomSendPoissonTransaction: {
                return processBatchRandomPoissonSend(command);
            }
            case EBatchIncreaseSendTransaction: {
                return processBatchIncreaseSend(command);
            }
            case EDumpBlock: {
                System.out.println("\n[CMD] Begin to dump current block chain:");
//                String content = "";
//                if (command.arguments.size() > 0) {
//                    content = BlockChain.getInstance().dump(Integer.parseInt(command.arguments.get(0)));
//                } else {
//                    content = BlockChain.getInstance().dump();
//                    content += "\n" + AccountManager.getInstance().dump();;
//                }
//                System.out.println(content);
                BlockchainFileDumper.dumpInternal("manual");
                System.out.println("Block has been dumped To file");
                break;
            }
            case EDumpExBlock: {
                System.out.println("\n[CMD] Begin to dump external block chain:");
//                String content = "";
//                if (command.arguments.size() > 0) {
//                    content = BlockChain.getExternalInstance().dump(Integer.parseInt(command.arguments.get(0)));
//                } else {
//                    content = BlockChain.getExternalInstance().dump();
//                }
//                System.out.println(content);
                BlockchainFileDumper.dumpExternal("manual");
                System.out.println("Block has been dumped To file");
                break;
            }
            case ESetBuyer: {
                System.out.println("\n[CMD] Begin to set buyer information:");
                AssetService.setBuyer(command.arguments.get(0));
                AssetService.setBuyerKey(command.arguments.get(1));
                break;
            }
            case EReg: {
                processRegulation(command);
                break;
            }
            case ENSP: {
                processNSP(command);
                break;
            }
            case EMSP: {
                processMSP(command);
                break;
            }
            case SETACP: {
                processSETACP(command);
                break;
            }
            case SENDACPTRIGGER: {
                triggerSendACPTriggerTransaction(command);
                break;
            }
            case EDumpBlockDB: {
                System.out.println("\n[CMD] Begin to dump block DB content:");
                BlockDBHandler blockDBHandler = BlockDBHandler.getInstance();
                blockDBHandler.dumpBlockDB();
                break;
            }
            case EEmpty: {
                System.out.println("");
                break;
            }
            case EStopService: {
                processStop();
                break;
            }
            case EOther: {
                System.out.println("[CMD] Command is not support: " + command.commandType);
                return false;
            }
        }
        return true;
    }

    private void processMSP(Command command) {
        if (command.arguments.size() < 2) {
            System.out.println("[CMD] Not enough parameters for miner setting simulation parameters");
        }
        int meanValue = Integer.parseInt(command.arguments.get(0));
        int deviationValue = Integer.parseInt(command.arguments.get(1));
        MinerSetting.getInstance().setMeanWaitTime(meanValue);
        MinerSetting.getInstance().setDeviationWaitTime(deviationValue);
    }

    private void processNSP(Command command) {
        if (command.arguments.size() < 2) {
            System.out.println("[CMD] Not enough parameters for network send simulation parameters");
        }
        int meanValue = Integer.parseInt(command.arguments.get(0));
        int deviationValue = Integer.parseInt(command.arguments.get(1));
        P2PConfiguration.getInstance().setMeanWaitTime(meanValue);
        P2PConfiguration.getInstance().setDeviationWaitTime(deviationValue);
    }

    private void processStop() {
        blockchainService.stopService();
        assetServiceThread.stopService();
        blockchainSyncService.stopService();
        forceStop = true;
    }

    private boolean processSendConditionalTransaction(Command command) {
        try {
            System.out.println("\n[CMD] Begin to send EM transaction:");
            blockchainService.triggerConditionalTransaction(command.arguments);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean processSendStateSyncTransaction(Command command) {
        try {
            System.out.println("\n[CMD] Begin to send EM transaction:");
            blockchainService.triggerStateSyncTransaction(command.arguments);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean processSendExternalTransaction(Command command) {
        try {
            System.out.println("\n[CMD] Begin to send External transaction:");
            blockchainService.triggerExternalTransaction(command.arguments);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean processSendMultipleTypeTransaction(Command command) {
        try {
            System.out.println("\n[CMD] Begin to send External transaction:");
            blockchainService.triggerMultipleTypeTransaction(command.arguments);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean processSendCrosschainMultipleTypeTransaction(Command command) {
        try {
            System.out.println("\n[CMD] Begin to send External multiple transaction:");
            blockchainService.triggerCrosschainMultipleTypeTransaction(command.arguments);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean triggerSendACPTriggerTransaction(Command command) {
        try {
            System.out.println("\n[CMD] Begin to send  ACP condition transaction:");
            blockchainService.triggerSendACPTriggerTransaction(command.arguments);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private boolean processSETACP(Command command) {
        try {
            System.out.println("\n[CMD] Begin to send  ACP action parameter setting transaction:");
            blockchainService.triggerSendACPActionSettingTransaction(command.arguments);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    boolean processSendDynamicalTransaction(Command command) {
        try {
            System.out.println("\n[CMD] Begin to send dynamical transaction:");
            blockchainService.triggerDynamicalDefinitionTransaction(command.arguments);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    boolean processOverrideAndInitTransaction(Command command) {
        try {
            System.out.println("\n[CMD] Begin to send override and init transaction:");
            blockchainService.triggerOverrideAndInitTransaction(command.arguments);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    boolean processInternalSmartContract(Command command) {
        try {
            System.out.println("\n[CMD] Begin to trigger internal smart contract");
            blockchainService.triggerInternalSmartContract(command.arguments);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    boolean processDynamicalInitTransaction(Command command) {
        try {
            System.out.println("\n[CMD] Begin to send dynamical init transaction:");
            blockchainService.triggerDynamicalInitTransaction(command.arguments);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean processSingleSend(Command command) {
        return processSingleSend(command, false);
    }

    private boolean processSingleSend(Command command, boolean isInternalTx) {
        try {
            System.out.println("\n[CMD] Begin to send transaction:");
            blockchainService.triggerTransaction(command.arguments, isInternalTx);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean processCommonCrosschainSend(Command command) {
        try {
            System.out.println("\n[CMD] Begin to send common crosschain transaction.");
            blockchainService.triggerCommonCrosschainTransaction(command.arguments);
        } catch (IOException e) {
            System.out.println("[CMD] Exception happened when try to trigger command, with error " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Nullable
    private boolean processBatchSend(Command command) {
        try {
            System.out.println("\n[CMD] Begin to batch send transaction(for TEST purpose)");
            String assetType = command.arguments.get(0);// the first parameter should be MultiTypeAsset type
            int batchTimes = Integer.parseInt(command.arguments.get(1));// the first parameter is repeat times
            int sleepTime = Integer.parseInt(command.arguments.get(2));// the second parameter is send interval ms
            if (0 == batchTimes) return true;

            String batchTxStart = "";
            for (int i = 3; i < command.arguments.size(); ++i) {
                batchTxStart += command.arguments.get(i);
            }

            for (int i = 0; i < batchTimes; ++i) {
                System.out.printf("[CMD][processBatchSend] Sleep %s ms for the %d Tx to send\n", sleepTime, i);
                Thread.sleep(sleepTime);
                generateAndSend(assetType, batchTxStart);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return true;
        }
        return true;
    }

    @Nullable
    private boolean processCreateAsset(Command command) { // create assetType amount account
        try {
            System.out.println("\n[CMD] Begin to create asset");
            blockchainService.triggerCreateAsset(command.arguments);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Nullable
    private boolean processGetBalance(Command command) {
        System.out.println("\n[CMD] Begin to get balance");
        String address = command.arguments.get(0);//
        Double balance = AccountManager.getInstance().getBalance(address);
        System.out.println(String.format("[CMD] The balance of %s is %f", address, balance));
        return true;
    }

    // bsr <asset type> <number> <mean wait time> <deviation time> <msg begin>
    private boolean processBatchRandomSend(Command command) {
        try {
            System.out.println("\n[CMD] Begin to batch send transaction(for TEST purpose) randomly");
            String assetType = command.arguments.get(0);// the first parameter should be MultiTypeAsset type
            int batchTimes = Integer.parseInt(command.arguments.get(1));// the first parameter is repeat times
            int meanTime = Integer.parseInt(command.arguments.get(2));// the second parameter is send mean wait time ms
            int deviationTime = Integer.parseInt(command.arguments.get(3));// the second parameter is send deviation ms
            if (0 == batchTimes) return true;

            String batchTxStart = "";
            for (int i = 4; i < command.arguments.size(); ++i) {
                batchTxStart += command.arguments.get(i);
            }

            GaussianHelper gaussianHelper = new GaussianHelper(meanTime, deviationTime);
            int sleepTime = 0;
            for (int i = 0; i < batchTimes; ++i) {
                sleepTime = gaussianHelper.getWaitTime();
                System.out.printf("[CMD][processBatchRandomSend] Sleep %s ms for the %d Tx to send\n", sleepTime, i);
                Thread.sleep(sleepTime);
                generateAndSend(assetType, batchTxStart);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return true;
        }
        return true;
    }

    // bsrp <asset type> <number> <mean arrive number> <msg begin>
    private boolean processBatchRandomPoissonSend(Command command) {
        try {
            System.out.println("\n[CMD][processBatchRandomPoissonSend] Begin to batch send transaction poisson(for TEST purpose) randomly");
            String assetType = command.arguments.get(0);// the first parameter should be MultiTypeAsset type
            int batchTimes = Integer.parseInt(command.arguments.get(1));// the first parameter is repeat times
            int meanArrivedNumber = Integer.parseInt(command.arguments.get(2));// the second parameter is send mean number Tx
            if (0 == batchTimes) return true;

            String batchTxStart = "";
            for (int i = 4; i < command.arguments.size(); ++i) {
                batchTxStart += command.arguments.get(i);
            }

            System.out.printf("[CMD][processBatchRandomPoissonSend] asset type: %s, batchTime: %d, mean arrive number: %d\n",
                    assetType, batchTimes, meanArrivedNumber);
            PoissonHelper poissonHelper = new PoissonHelper(meanArrivedNumber);
            for (int i = 0; i < batchTimes; ++i) {
                int arriveNumber = poissonHelper.getPoissonRandom();
                for (int j = 0; j < arriveNumber; ++j) {
                    generateAndSend(assetType, batchTxStart);
                }
                Thread.sleep(6000);
                System.out.println("[CMD][processBatchRandomPoissonSend] send: " + arriveNumber);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return true;
        }
        return true;
    }

    // ibs <asset type> <number> <increase number> <msg begin>
    private boolean processBatchIncreaseSend(Command command) {
        try {
            System.out.println("\n[CMD] Begin to batch send transaction(for TEST purpose) increasingly");
            String assetType = command.arguments.get(0);// the first parameter should be MultiTypeAsset type
            int batchTimes = Integer.parseInt(command.arguments.get(1));// the first parameter is repeat times
            int increaseAmount = Integer.parseInt(command.arguments.get(2));// the second parameter is increase amount
            System.out.printf("The according parameter is: asset type %s, batchTimes: %s, increaseAmount: %s \n",
                    assetType, batchTimes, increaseAmount);
            if (0 == batchTimes) return true;

            increaseRegulator = new IncreaseRegulator(IncreaseType.EExponential);
            increaseRegulator.setIncreaseAmount(increaseAmount);
            increaseRegulator.start(blockchainService.getMinerRunInterval());
            String batchTxStart = "";
            for (int i = 4; i < command.arguments.size(); ++i) {
                batchTxStart += command.arguments.get(i);
            }

            int sleepTime = 100;// TO DO In test, please change this time
            for (int i = 0; i < batchTimes; ++i) {
                System.out.printf("[CMD][processBatchIncreaseSend] Sleep %s ms for the %d Tx to send\n", sleepTime, i);
                Thread.sleep(sleepTime);
                if (increaseRegulator.canSendNext()) {
                    generateAndSend(assetType, batchTxStart);
                } else {
                    System.out.println("[CMD][processBatchIncreaseSend]Discard the packet with respect to discard policy");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return true;
        }
        return true;
    }

    private void generateAndSend(String assetType, String batchTxStart) throws IOException {
        generateAndSend(increaseRegulator, blockchainService, assetType, batchTxStart);
    }

    private void processRegulation(Command command) {
        System.out.println("\n[CMD] Begin to regulation the network");
        String regType = command.arguments.get(0); //  // reg s/a/sa/off/on p1 p2
        RegulationType regulationType;
        String regulationAssetType = "";
        if (regType.equals("s")) {
            regulationType = RegulationType.ESpeed;
            if (command.arguments.size() < 2) {
                System.out.println("[CMD] Speed regulation parameter missing");
                return;
            }
            if (command.arguments.size() > 2) {
                regulationAssetType = command.arguments.get(2);
            }
        } else if (regType.equals("a")) {
            regulationType = RegulationType.EAmount;
            if (command.arguments.size() < 2) {
                System.out.println("[CMD] Amount regulation parameter missing");
                return;
            }
        } else if (regType.equals("sa")) { //reg sa <speed> <amount>
            regulationType = RegulationType.ESpeedAmount;
            if (command.arguments.size() < 3) {
                System.out.println("[CMD] Speed and amount regulation parameter missing");
                return;
            }
        } else if (regType.equals("off")) {
            regulationType = RegulationType.EOff;
        } else if (regType.equals("on")) {
            regulationType = RegulationType.ENone;
        } else {
            System.out.println("[CMD] Not support regulation type: " + regType);
            return;
        }

        int granularity1 = 0;
        int granularity2 = 0;
        if (command.arguments.size() > 1) {
            granularity1 = Integer.parseInt(command.arguments.get(1));  // reg s/a/sa/off/on p1 p2
        }
        if (command.arguments.size() > 2) {
            try {
                granularity2 = Integer.parseInt(command.arguments.get(2));
            } catch (NumberFormatException e) {
                // ignored as this parameter may be theasset type
            }
        }
        System.out.println(command.arguments);
        System.out.println("[CMD] [DEBUG] Start regulation with: " + regulationType.toString() + ", " + granularity1 + ", " + granularity2);
        Regulator.getInstance("Miner", regulationAssetType).startRegulation(regulationType, granularity1, granularity2);
        // "sender is used to record the regulation type and amount"
        Regulator.getInstance("Sender", regulationAssetType).startRegulation(regulationType, granularity1, granularity2);
    }
}

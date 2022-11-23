// classes in "embed": their code are embedded in blockchain jar, while run in a separate process by system.exec
package com.scu.suhong.instantiationOptimization.embed;

import Service.BlockchainService;
import com.scu.suhong.instantiationOptimization.ProcessingState;
import com.scu.suhong.instantiationOptimization.RequestProcessHandler;
import com.scu.suhong.instantiationOptimization.RequestProviderInterface;
import com.scu.suhong.transaction.Transaction;
import util.FileHelper;
import util.ThreadHelper;
import util.TimeHelper;

import java.io.File;

public class NotCallingOther implements RequestProviderInterface {
    static final int runInterval = 1;
    static String logClassName = "NotCallingOther";
    int loopTimes = 60*60*1;// One hour
    String identifier;
    RequestProcessHandler processHandler;

    public NotCallingOther(String identifier) {
        this.identifier = identifier;
    }

    public static void main(String args[]) {
        doAction(args);
    }

    static void doAction(String[] args) {
        long begintTime = TimeHelper.getEpochSeconds();
        if (args.length < 2) {
            System.out.printf("[%s][Info] Parameter missing. Parameters are: identifier smartContractPath <loopnumbers>", logClassName);
            return;
        }

        String identifier = args[0];
        System.out.printf("[%s][Info] Begin smart contract NotCallingOther with identifier %s at time %s\n",
                logClassName, identifier, begintTime);

        String smartContractPath = args[1];
        System.out.printf("[%s][Info] Smart contract path is %s\n", logClassName, smartContractPath);

        NotCallingOther i = new NotCallingOther(identifier);
        if (args.length >= 3) {
            i.setLoopTimes(args[2]);
        }
        i.function();
        long endtTime = TimeHelper.getEpochSeconds();
        System.out.printf("[%s][Info] End smart contract with identifier %s at time %s\n",
                logClassName, identifier, endtTime);
        System.out.printf("[%s][Info] *** Smart contract with identifier %s run %s seconds",
                logClassName, identifier, endtTime - begintTime);
        System.exit(0);
    }

    void function() {
        processHandler = new RequestProcessHandler(this);
        ProcessingState state = ProcessingState.ENone;
        for (int i = 0; i < loopTimes; ++i) {
            System.out.printf("[%s][Info] Current blockchain number is %d\n", logClassName, getBlockChainTransactionNumber());
            System.out.printf("[%s][Info] Check for %d time(s)\n", logClassName, i);

            if (ProcessingState.EDone == (state = processHandler.processRequest())) {
                System.out.printf("[RequestProcessHandler][%s][Info] Termination request has been received and states are confirmed in blockchain." +
                                "Exiting request processing thread\n", logClassName);
                break;
            }
            ThreadHelper.safeSleepSecond(runInterval);
        }

        if (state != ProcessingState.EDone){
            System.out.printf("[%s][ERROR] Max waiting time reached, while the request is not finished\n", logClassName);
        }
    }

    public int getBlockChainTransactionNumber() {
        // have to hard coded this or have to send message to a server.
        // As DB is locked as it cannot be loadded twice
        return 5;
    }

    public boolean checkLatestBlockTransactions() {
        // have to hard coded this or have to send message to a server.
        // As DB is locked as it cannot be loadded twice
        System.out.printf("[%s][checkLatestBlockTransactions][Info] Transaction number in latest block %d is %d\n",
                logClassName, 4, 5);
        return true;
    }

    public void setLoopTimes(String loopTimes) {
        System.out.printf("[%s][WARN] Please note the loop times should be set enough to wait the states have been put to blockchain." +
                        "The main purpose of this function is for unit test\n"
                , logClassName);
        try {
            int times = Integer.parseInt(loopTimes);
            if (times < 1) times = 1;//must be once
            this.loopTimes = times;
            System.out.printf("[%s] Run time has been changed to %s seconds", logClassName, loopTimes);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    // write state to blockchain and check it.
    public void writeStateToBlockchain(String data) {
        data = "new_" + data;
        String fileName = "command" + File.separator + "command";
        String cmd = "sendiTx " + data + " 789001 789002 0";
        while (FileHelper.doesFileOrFolderExist(fileName)){
            ThreadHelper.safeSleepSecond(1);
            System.out.printf("[%s][INFO] Sleep 1 seconds to send internal transaction\n", logClassName);
        }
        System.out.printf("[%s][INFO] Send internal command as new id\n", logClassName);
        FileHelper.createFile(fileName, cmd);
        processHandler.setStateToBeConfirmed(data);
    }

    public void testTwoParameters(String first, String second){
        System.out.printf("[%s][INFO] Parameters are %s and %s\n", logClassName, first, second);
    }

    @Override
    public String getIdentify() {
        return identifier;
    }
}

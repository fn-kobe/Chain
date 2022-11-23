package com.scu.suhong.smartcontract.nondeterminacy.embedded;

import Service.BlockchainService;
import com.scu.suhong.smartcontract.nondeterminacy.State;
import util.FileHelper;
import util.RandomHelper;
import util.ThreadHelper;
import util.TimeHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// An smart contract to get the state from an non blockchain system
public class NBSState {
    static String smartContractName = "NBSState";
    static String variety1 = "variety1";
    static boolean randomDiscard = false;
    static int randomDiscardNumber = 2;// 1 / randomDiscardRatio
    static RandomHelper randomHelper = new RandomHelper(100);
    //Used to not duplicated got the state
    static List<String> alreadyGotNBSStateArray = new ArrayList<>();

    // both query single state and history state
    public static void startMixMode(String NBSIp, String runTimeString, int historyResetTime){
        int runTime = Integer.parseInt(runTimeString);
        long beginSeconds = TimeHelper.getEpochSeconds();
        long currentSeconds = TimeHelper.getEpochSeconds();

        System.out.println("[NBSState][Info] begin to run the internal smart contract NBSState");
        int historyCount = 0;
        while (runTime > currentSeconds - beginSeconds) {
            getAndProcessState(NBSIp);

            if (historyResetTime > 0 && ++historyCount >= historyResetTime){
                System.out.println("[NBSState][Info] History query time is due. Try to query history information");
                getAndProcessHistoryState(NBSIp);
                historyCount = 0;
            }

            currentSeconds = TimeHelper.getEpochSeconds();
            ThreadHelper.safeSleep(1 * 1000);
        }
    }

    // runtime is in seconds
    public static void start(String NBSIp, String runTimeString){
        int runTime = Integer.parseInt(runTimeString);
        long beginSeconds = TimeHelper.getEpochSeconds();
        long currentSeconds = TimeHelper.getEpochSeconds();

        System.out.println("[NBSState][Info] begin to run the internal smart contract NBSState");
        while (runTime > currentSeconds - beginSeconds) {
            getAndProcessHistoryState(NBSIp);
            currentSeconds = TimeHelper.getEpochSeconds();
            ThreadHelper.safeSleep(1 * 1000);
        }
    }

    // runtime is in seconds
    public static void startWithoutHistory(String NBSIp, String runTimeString){
        int runTime = Integer.parseInt(runTimeString);
        long beginSeconds = TimeHelper.getEpochSeconds();
        long currentSeconds = TimeHelper.getEpochSeconds();

        System.out.println("[NBSState][Info] begin to run the internal smart contract NBSState");
        while (runTime > currentSeconds - beginSeconds) {
            getAndProcessState(NBSIp);
            currentSeconds = TimeHelper.getEpochSeconds();
            ThreadHelper.safeSleep(1 * 1000);
        }
    }

    private static void getAndProcessHistoryState(String NBSIp) {
        try {
            String r = getStateHistoryFromNBS(NBSIp);
            System.out.printf("[NBSState][INFO] get history state '%s' from NBS %s\n", r.replace("\n", " "), NBSIp);
            processStateData(r);
        } catch (IOException e) {
            e.printStackTrace();
            ThreadHelper.safeSleep(20*1000);
        }
    }

    private static void getAndProcessState(String NBSIp) {
        try {
            String r = getStateFromNBS(NBSIp);
            System.out.printf("[NBSState][INFO] get single state '%s' from NBS %s\n", r, NBSIp);
            processStateData(r);
        } catch (IOException e) {
            e.printStackTrace();
            ThreadHelper.safeSleep(20*1000);
        }
    }

    static boolean processStateData(String stateData) throws IOException {
        String[] states = stateData.split("\n");
        int c = 0;
        for (String stateString : states) {
            System.out.println("[NBSState][INFO] Try to  process " + (++c) + "th data of " + stateData);
           updateSate(stateString);
        }
        return true;
    }

    private static boolean updateSate(String stateString) throws IOException {
        if (shouldCheating(stateString)){
            System.out.println("[NBSState][INFO] Hide one state " + stateString);
            return true;
        }
        if (randomDiscard
                &&(
                    (randomDiscardNumber > 0 && (0 == randomHelper.getNumber() % randomDiscardNumber))
                    ||(randomDiscardNumber < 0 && (0 != randomHelper.getNumber() % randomDiscardNumber))
                )
        ){
            System.out.println("[NBSState][INFO] Random discard " + stateString);
            return true;
        }

        State state = new State(stateString);
        if (null == state.getSate() || state.getSate().isEmpty()) {
            System.out.printf("[NBSState][INFO] get empty state \n");
            return false;
        }
        BlockchainService.getInstance().updateNBSState(smartContractName, variety1, state.getValue());
        System.out.println("[NBSState][INFO] Succeed to add state " + stateString);
        return true;
    }

    static String history = "history";

    static String getStateHistoryFromNBS(String NBSIp) throws IOException {
        return getStateFromNBS(NBSIp + "/" + history);
    }

    static String getStateFromNBS(String NBSIp) throws IOException {
        StringBuilder result = new StringBuilder();
        URL url = new URL(formatIPAddress(NBSIp));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        while ((line = rd.readLine()) != null) {
            result.append(line + "\n");
        }
        rd.close();

        String stringResult = result.toString();
        boolean found = false;
        for (String r : alreadyGotNBSStateArray){
            if (r.equals(stringResult)){
                System.out.printf("[NBSState][INFO] Result %s already got and we do not process it duplicated\n", r);
                found = true;
                break;
            }
        }
        if (!found){
            alreadyGotNBSStateArray.add(stringResult);
            return stringResult;
        } else {
            return "";
        }
    }

    static String formatIPAddress(String NSBIp){
        String httpPrefix = "http://";
        if (!NSBIp.startsWith(httpPrefix)){
            return httpPrefix + NSBIp;
        }
        return NSBIp;
    }

    // Simulation of cheating
    static boolean shouldCheating(String stateString){
        State state = new State(stateString);

        String[] lines = FileHelper.loadContentFromFile("statecheat", false).split("\n");
        for (int i = 0; i < lines.length; ++i){
            if (!lines[i].isEmpty() && lines[i].equals(state.getSate())){
                System.out.printf("[NBSState][INFO] Do not send out state %s as it is configured not to send out\n", stateString);
                return true;
            }
        }

        return false;
    }

    public static boolean isRandomDiscard() {
        return randomDiscard;
    }

    public static void setRandomDiscard(boolean randomDiscard) {
        NBSState.randomDiscard = randomDiscard;
    }

    public static int getRandomDiscardNumber() {
        return randomDiscardNumber;
    }

    public static void setRandomDiscardNumber(int randomDiscardNumber) {
        NBSState.randomDiscardNumber = randomDiscardNumber;
    }
}

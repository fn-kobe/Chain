package util;

import com.scu.suhong.miner.MinerSetting;
import com.scu.suhong.network.P2PConfiguration;

import java.io.IOException;

public class ThreadHelper {
    static public void safeSleep(long sleepTime){
        try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("Sleep interrupt");
            }
        }

    static public void safeSleepSecond(long sleepTime){
       ThreadHelper.safeSleep(sleepTime * 1000);
    }

        static public void minerSimulateDelay(){
            int simulateDelayTime = MinerSetting.getInstance().getWaitTime();
            if (0 != simulateDelayTime){
                safeSleep(simulateDelayTime);
            }
        }
        static public void p2PSimulateDelay() {
            int waitTime = P2PConfiguration.getInstance().getWaitTime();
            if (0 != waitTime) {
                System.out.println("[ThreadHelper] Try to simulate delay " + waitTime + " milli seconds");
                safeSleep(waitTime);
            }
        }

        static public Process runExternalCommand(String command){
            Runtime runtime = Runtime.getRuntime();
            try {
                System.out.println("[ThreadHelper][INFO] Try to run command " + command);
                return runtime.exec(command);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }


package com.scu.suhong.miner;

import util.GaussianHelper;

// this class is dynamic setting which is not configurable by the user. It is different from mining configuration
public class MinerSetting {
    static MinerSetting instance;

    private GaussianHelper gaussianHelper;

    synchronized static public MinerSetting getInstance(){
        if (null == instance){
            instance = new MinerSetting();
        }
        return instance;
    }

    private MinerSetting() {
        gaussianHelper = new GaussianHelper();
    }

    public void setMeanWaitTime(int meanWaitTime) {
        gaussianHelper.setMeanValue(meanWaitTime);
    }

    public void setDeviationWaitTime(int deviationWaitTime) {
        gaussianHelper.setDeviationValue(deviationWaitTime);
    }

    // set max time to 0 means no wait time.
    public void stopWaitTime() {
        gaussianHelper.stop();
    }

    // milli- seconds
    // Gaussian wait time
    public int getWaitTime() {
        return gaussianHelper.getWaitTime();
    }

}

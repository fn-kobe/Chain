package util;

import java.util.Random;

public class GaussianHelper {
    int meanValue = 0;
    int deviationValue = 0;

    public GaussianHelper(int meanValue, int deviationValue) {
        this.meanValue = meanValue;
        this.deviationValue = deviationValue;
    }

    public GaussianHelper() {

    }

    public void setMeanValue(int meanValue) {
        this.meanValue = meanValue;
    }

    public void setDeviationValue(int deviationValue) {
        this.deviationValue = deviationValue;
    }

    // set max time to 0 means no wait time.
    public void stop() {
        setMeanValue(0);
    }

    // Gaussian random number
    public int getNumber() {
        if (0 >= meanValue) {
            return 0;
        }
        Random random = new Random();
        int randomValue = (int) (random.nextGaussian() * deviationValue + meanValue);

        if (randomValue < 0 || randomValue > meanValue + 3 * deviationValue) {
            System.out.println("[GaussianHelper] The random value is not normal value:" + randomValue + ". Change it");
            if (randomValue < 0) randomValue = 0;
            else randomValue = meanValue + deviationValue * 3;
        }

        return randomValue;
    }

    // milli- seconds
    // Gaussian wait time
    public int getWaitTime() {
        return getNumber();
    }
}

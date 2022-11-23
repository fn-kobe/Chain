package util;

import java.util.Random;

public class PoissonHelper {
    private double mean;
    Random randomSeed;

    public PoissonHelper(double mean) {
        this.mean = mean;
        randomSeed = new Random();
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public int getPoissonRandom() {

        double L = Math.exp(-mean);
        int k = 0;
        double p = 1.0;
        do {
            p = p * randomSeed.nextDouble();
            k++;
        } while (p > L);
        return k - 1;
    }
}

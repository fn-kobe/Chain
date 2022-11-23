package util;

import java.util.Random;

public class RandomHelper {
    int minNumber;
    int maxNumber;
    Random rand;

    public RandomHelper() {
        this.minNumber = 0;
        this.maxNumber = 0;
        rand = new Random();
    }

    public RandomHelper(int maxNumber) {
        this.minNumber = 0;
        this.maxNumber = maxNumber;
        rand = new Random();
    }

    public RandomHelper(int minNumber, int maxNumber) {
        this.minNumber = minNumber;
        this.maxNumber = maxNumber;
        rand = new Random();
    }

    public int getNumber(int maxValue){
        return rand.nextInt(maxValue);
    }

    public int getNumber(){
        return rand.nextInt(maxNumber - minNumber) + minNumber;
    }
}

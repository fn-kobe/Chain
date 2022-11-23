package util;

import java.util.ArrayList;
import java.util.List;

public class NoDuplicatedRandomHelper {
    int foundNumber[];
    RandomHelper randomHelper;
    int maxNumber;

    public NoDuplicatedRandomHelper(int maxNumber) {
        this.maxNumber = maxNumber;
        foundNumber = new int[maxNumber];
        for (int i = 0; i < maxNumber; ++i) foundNumber[i] = 0;
        randomHelper = new RandomHelper(maxNumber);
    }

    void reset(int newMaxNumber){
        this.maxNumber = newMaxNumber;
        foundNumber = new int[newMaxNumber];
        for (int i = 0; i < newMaxNumber; ++i) foundNumber[i] = 0;
        randomHelper = new RandomHelper(maxNumber);
    }

    // return -1 if all number has been fetched.
    public int getNumber(){
        if (isAllFetched()) return -1;
        int result;
        do{
            result = randomHelper.getNumber();
        } while((1 == foundNumber[result]));
        foundNumber[result] = 1;
        return result;
    }

    public int getUnFetchedNumber(){
        int r = 0;
        for (int i = 0; i < foundNumber.length; ++i){
            if (0 == foundNumber[i]){
                ++r;
            }
        }
        return r;
    }

    public int getFetchedNumber(){
        return foundNumber.length - getUnFetchedNumber();
    }

    void dumpStatus(){
        String s = "D: ";
        for (int i = 0; i < maxNumber; ++i) s += " : " + foundNumber[i];
        System.out.println(s);
    }

    public boolean putBack(int i){
        if (1 == foundNumber[i]){
            foundNumber[i] = 0;
            return true;
        }
        return false;
    }

    public boolean putBackWithAtLeastOneLeft(int i){
        boolean isOneLeft = false;
        for (int j = 0; j < maxNumber; ++j){
            if (foundNumber[j] == 0){
                isOneLeft = true;
                break;
            }
        }
        if (!isOneLeft) return false;

        if (1 == foundNumber[i]){
            foundNumber[i] = 0;
            return true;
        }
        return false;
    }

    public boolean isAllFetched(){
        boolean allFetched = true;
        for (int i = 0; i < maxNumber; ++i){
            if (0 == foundNumber[i]){
                allFetched = false;
                break;
            }
        }
        return allFetched;
    }

    public List<Integer> getFetchedNumberList(){
        List<Integer> fetchedNumbers = new ArrayList<>();
        for (int i = 0; i < maxNumber; ++i){
            if (1 == foundNumber[i]) fetchedNumbers.add(i);
        }
        return fetchedNumbers;
    }

    public List<Integer> getUnFetchedNumberList(){
        List<Integer> fetchedNumbers = new ArrayList<>();
        for (int i = 0; i < maxNumber; ++i){
            if (0 == foundNumber[i]) fetchedNumbers.add(i);
        }
        return fetchedNumbers;
    }

    public final int[] getStatusClone(){
        return foundNumber.clone();
    }

}

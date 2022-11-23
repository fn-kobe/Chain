package util;

import junit.framework.TestCase;

public class RandomHelperTest extends TestCase {

    public void testGetNumber() {
        int start = 1;
        int end = 4;
        RandomHelper randomHelper = new RandomHelper(start, end);
        int r = 0;
        for (int i = 0; i < 20; ++i) {
            r = randomHelper.getNumber();
            System.out.println("The random is: " + r);
            assert r >= start && r < end;
        }

        start = 40;
        end = 80;
        randomHelper = new RandomHelper(start, end);
        r = 0;
        for (int i = 0; i < 7; ++i) {
            r = randomHelper.getNumber();
            System.out.println("The random is: " + r);
            assert r >= start && r < end;
        }

    }
}
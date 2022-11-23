package consensus.pow;

import org.junit.Test;

import static org.junit.Assert.*;

public class MiningConfigurationHandlerTest {

    @Test
    public void isHashMatched() {
        MiningConfigurationHandler handler = new MiningConfigurationHandler("MiningConfiguration");
        handler.testSetRequiredZeroCount(6);
        handler.testSetAllowedNumber(1);// 1 only allow 0
        String sixZero = "000000";
        assert handler.isHashMatched(sixZero + "0123");
        assert !handler.isHashMatched(sixZero + "1123");

        handler.testSetAllowedNumber(3);// 0 1 2
        assert handler.isHashMatched(sixZero + "1123");
        assert handler.isHashMatched(sixZero + "2123");
        assert !handler.isHashMatched(sixZero + "3123");
    }
}
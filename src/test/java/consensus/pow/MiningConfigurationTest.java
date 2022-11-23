package consensus.pow;

import mockit.Expectations;
import mockit.Mocked;
import org.junit.Test;
import util.IniFileHelper;

public class MiningConfigurationTest {

    @Test
    public void testGetRequiredZeroCount() {
        int difficulty = MiningConfiguration.getRequiredZeroCount();
        assert difficulty == 3;
    }

    @Test
    public void testGetRequiredZeroCountFromFile(@Mocked final IniFileHelper iniFileHelper) {
        new Expectations() {{
            iniFileHelper.getValue(anyString, anyString, anyString);
            result = 4;
        }};
        int difficulty = MiningConfiguration.getRequiredZeroCount();
        assert difficulty == 4;
    }
}
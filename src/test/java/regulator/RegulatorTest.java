package regulator;

import junit.framework.TestCase;

public class RegulatorTest extends TestCase {

    public void testGetAllowedAmount() throws InterruptedException {
        Regulator regulator = Regulator.getInstance("Miner");
        regulator.stopRegulation();
        regulator.startRegulation(RegulationType.EAmount,2);
        assert regulator.getAllowedAmount() == 2;

        regulator.stopRegulation();
        regulator.startRegulation(RegulationType.ESpeed, 2);
        assert regulator.getAllowedAmount() == 0;
        Thread.sleep(2100);
        assert (regulator.getAllowedAmount() == 4);

        regulator.increaseUsedTxAmount();
        assert regulator.getAllowedAmount() == 3;
    }
}
package regulator;

import junit.framework.TestCase;

public class RegulationWorkerTest extends TestCase {

    public void testRegulation() throws InterruptedException {
        RegulationWorker regulationWorker = new RegulationWorker("Test");
        regulationWorker.increaseUsedTxAmount();
        assert -1 == regulationWorker.getAllowedAmount();

        regulationWorker.startRegulation(RegulationType.EAmount, 2, 0);
        assert 1 == regulationWorker.getAllowedAmount();

        regulationWorker.increaseUsedTxAmount();
        assert 0 == regulationWorker.getAllowedAmount();

        regulationWorker.increaseUsedTxAmount();
        assert 0 == regulationWorker.getAllowedAmount();

        regulationWorker.stopRegulation(RegulationType.EAmount);
        regulationWorker.startRegulation(RegulationType.ESpeed, 2, 0);
        Thread.sleep(2200);
        assert (4 == regulationWorker.getAllowedAmount());

        regulationWorker.startRegulation(RegulationType.EAmount, 3, 0);
        assert 3 == regulationWorker.getAllowedAmount();

        regulationWorker.startRegulation(RegulationType.EOff, 10, 10);
        assert 0 == regulationWorker.getAllowedAmount();

        assert !regulationWorker.startRegulation(RegulationType.ESpeed, 10, 0);

        testRegulationType();
    }

    public void testRegulationType() {
        RegulationWorker regulationWorker = new RegulationWorker("Test");
        regulationWorker.stopRegulation();
        assert regulationWorker.startRegulation(RegulationType.ESpeed, 10, 0);
        assert RegulationType.ESpeed == regulationWorker.getRegulationType();

        assert regulationWorker.startRegulation(RegulationType.EAmount, 10, 0);
        assert RegulationType.ESpeedAmount == regulationWorker.getRegulationType();

        regulationWorker.stopRegulation();
        assert regulationWorker.startRegulation(RegulationType.EAmount, 10, 0);
        assert RegulationType.EAmount == regulationWorker.getRegulationType();

        assert regulationWorker.startRegulation(RegulationType.ESpeed, 10, 0);
        assert RegulationType.ESpeedAmount == regulationWorker.getRegulationType();

        regulationWorker.stopRegulation();
        assert regulationWorker.startRegulation(RegulationType.ESpeedAmount, 10, 0);
        assert RegulationType.ESpeedAmount == regulationWorker.getRegulationType();

        assert regulationWorker.startRegulation(RegulationType.ENone, 0, 0 );
        assert RegulationType.ENone == regulationWorker.getRegulationType();
    }
}
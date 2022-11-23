package regulator;

import org.junit.Test;

public class IncreaseRegulatorTest {

    @Test
    public void testGetGradualIncreaseAmount() throws InterruptedException {

        // Remove the impact from previous test
        Regulator regulator = Regulator.getInstance("Sender");
        regulator.stopRegulation();

        int currentSpeed = 0;
        int defaultIncreaseAmount = 2;
        int increaseAmount = 3;

        System.out.println("[IncreaseRegulatorTest] Test interval is not set");
        IncreaseRegulator increaseRegulator = new IncreaseRegulator(IncreaseType.EExponential);
        assert 0 == increaseRegulator.getIncreaseSpeed();

        System.out.println("[IncreaseRegulatorTest] Test interval is set");
        increaseRegulator.start(10);// interval is 10 ms
        Thread.sleep(10);
        int adjustSpeed = increaseRegulator.getIncreaseSpeed();
        assert defaultIncreaseAmount == adjustSpeed;

        increaseRegulator.setIncreaseAmount(increaseAmount);
        increaseRegulator.start(10);// interval is 10 ms
        Thread.sleep(10);
        adjustSpeed = increaseRegulator.getIncreaseSpeed();
        assert increaseAmount == adjustSpeed;

        Thread.sleep(10);
        adjustSpeed = increaseRegulator.getIncreaseSpeed();
        assert increaseAmount * increaseAmount == adjustSpeed;

        Thread.sleep(10);
        adjustSpeed = increaseRegulator.getIncreaseSpeed();
        assert increaseAmount * increaseAmount == adjustSpeed; // the speed is the same as above, max amount 19 is reached
    }

    @Test
    public void testCanSendNext() throws InterruptedException {
        IncreaseRegulator increaseRegulator = new IncreaseRegulator(IncreaseType.EExponential);
        int increaseAmount = 3;

        increaseRegulator.setIncreaseAmount(increaseAmount);
        increaseRegulator.start(50);
        assert !increaseRegulator.canSendNext();
        Thread.sleep(50);
        assert increaseRegulator.canSendNext();
        assert increaseRegulator.canSendNext();
        assert increaseRegulator.canSendNext();
        assert !increaseRegulator.canSendNext(); // increaseAmount = 3;
    }
}
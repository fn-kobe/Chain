package regulator;

import util.TimeHelper;

// This class define the discard policy after the regulation is on
public class IncreaseRegulator {

    int adjustInterval = -1;
    long lastAdjustTime = 0;

    final int startSpeed = 0;
    // As the AllowedSpeed is increasing, we can just set it as currentIncreaseSpeed
    int currentIncreaseSpeed = startSpeed;

    int increaseAmount = 2;

    private int sendCount;
    private IncreaseType increaseType;
    private int increaseTime = 0;// used for logarithm type
    private Regulator regulator;

    public IncreaseRegulator(IncreaseType increaseType) {
        this.increaseType = increaseType;
        regulator = Regulator.getInstance("Sender");
    }

    public boolean canSendNext() {
        if (!isAdjustEnable()) return true; // return always true if adjust is not enabled
        // We try make the count in one period

        currentIncreaseSpeed += getIncreaseSpeed();// increase currentIncreaseSpeed
        // Extend the max, cannot send more
        if (sendCount > currentIncreaseSpeed) {
            System.out.printf("[IncreaseRegulator] sendCount:%d > currentIncreaseSpeed: %d\n"
                    , sendCount, currentIncreaseSpeed);
            return false;
        }

        // Send count is used for the increase speed limitation.
        ++sendCount;
        System.out.println("[IncreaseRegulator][canSendNext] Ok to send the Tx with sendCount: " + sendCount);
        return true;
    }

    public void setIncreaseAmount(int increaseAmount) {
        this.increaseAmount = increaseAmount;
    }

    // The interval unit is ms
    public void start(int interval) {
        reset(interval);
    }

    private void reset(int speedAdjustInterval) {
        this.adjustInterval = speedAdjustInterval;
        lastAdjustTime = TimeHelper.getEpoch();
        sendCount = 0;
        currentIncreaseSpeed = 0;
        System.out.printf("[IncreaseRegulator] Adjust interval is: %d with lastAdjustTime %d\n"
                , speedAdjustInterval, lastAdjustTime);
    }

    // The increase is  by increaseAmount(s)
    public int getIncreaseSpeed() {
        if (!isAdjustEnable()) {
            return -1;
        }

        if ((TimeHelper.getEpoch() - lastAdjustTime) < adjustInterval) {
            System.out.printf("[IncreaseRegulator] Increasing time is not reached, skip adjusting, lastAdjustTime: %d, adjustInterval: %d\n"
                    , lastAdjustTime, adjustInterval);
            return 0;// Adjust time is not reached
        }

        int newIncreaseSpeed = calculateIncreaseSpeed();
        System.out.println("[IncreaseRegulator] Begin to increase speed with amount: " + newIncreaseSpeed);
        lastAdjustTime = TimeHelper.getEpoch();
        return newIncreaseSpeed;
    }

    int calculateIncreaseSpeed() {
        int increaseSpeed = doCalculateIncreaseSpeed();
        if (isSpeedRegulation()) {
            int regulatorSpeed = regulator.getAllowedSpeed() * adjustInterval / 1000;// regulator's speed id per milliseconds
            System.out.printf("The speed: increaseSpeed:%d VS regulatorSpeed: %d\n", increaseSpeed, regulatorSpeed);
            if (increaseSpeed > regulatorSpeed) {
                System.out.println("[IncreaseRegulator] Max increase speed is reached, use the regulator speed");
                return regulatorSpeed;
            }
            return increaseSpeed;
        } else if(isAmountRegulation()){
            return increaseSpeed;
        } else {
            System.out.println("[IncreaseRegulator] TO DO add the increase support for other type except speed");
            return 0;
        }
    }

    int doCalculateIncreaseSpeed() {
        int increaseSpeed = 0;
        switch (increaseType) {
            case EExponential:
                increaseSpeed = (0 == currentIncreaseSpeed) ? increaseAmount : (currentIncreaseSpeed * (increaseAmount));
                break;

            case ELinear:
                ++increaseTime;
                increaseSpeed = increaseAmount * increaseTime;
                break;

            case ESqrt:
                ++increaseTime;
                // TO DO
                increaseSpeed = (int) Math.sqrt(increaseAmount * increaseTime);
                break;
        }

        return increaseSpeed;
    }

    private boolean isSpeedRegulation() {
        return regulator.getRegulationType() == RegulationType.ESpeed || regulator.getRegulationType() == RegulationType.ESpeedAmount;
    }

    private boolean isAmountRegulation() {
        return regulator.getRegulationType() == RegulationType.EAmount || regulator.getRegulationType() == RegulationType.ESpeedAmount;
    }

    private boolean isAdjustEnable() {
        boolean r = (-1 != adjustInterval && 0 != adjustInterval);
        System.out.printf("[IncreaseRegulator] Adjust interval is %s enabled\n", r ? "" : "NOT");
        return r;
    }

}

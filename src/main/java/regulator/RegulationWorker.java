package regulator;

import util.TimeHelper;

/* There are 3 type of regulation now
 Speed regulation, only regulate the speed, NOTE the speed unit is number/second.
 10 means 10 transactions per second
 ON/OFF regulation, can be mapped to speed regulation
 Tx amount regulation, to control the total Tx amount
 All of this can be unified to amount

 */
public class RegulationWorker {
    final int noStrictNumber = -1;// -1 means no strict
    long speedRegulationStartTime = -1;
    int regulationTxAllowSpeed = noStrictNumber;
    int regulationTxAllowAmount = noStrictNumber;
    RegulationType regulationType = RegulationType.ENone;
    int usedTxAmount = 0;
    String name; // for debug

    public RegulationWorker(String name) {
        this.name = name;
    }

    public boolean startRegulation(RegulationType regulationType, int granularity1, int granularity2){
        if (regulationType.equals(RegulationType.ENone)){
            usedTxAmount = 0;
            speedRegulationStartTime = -1;
            this.regulationType = regulationType;
            System.out.println("[RegulationWorker][" + name + "] Cancel regulation as regulation type is ENone");
            return true;
        }

        if (regulationType.equals(RegulationType.EOff)){
            usedTxAmount = 0;
            speedRegulationStartTime = -1;
            this.regulationType = regulationType;
            return true;
        }

        if (regulationType.equals(RegulationType.EAmount)){
            return startAmountRegulation(granularity1);
        }

        if (regulationType.equals(RegulationType.ESpeed)){
            return startSpeedRegulation(granularity1);
        }

        if(regulationType.equals(RegulationType.ESpeedAmount))
        {
            if (startSpeedRegulation(granularity1)){
                return startAmountRegulation(granularity2);
            }
            return false;
        }

        System.out.println("[RegulationWorker][" + name + "] Not support regulation type: " + regulationType.toString());
        return false;
    }


    public boolean startSpeedRegulation(int granularity){
        if (regulationType == RegulationType.EOff)
        {
            System.out.println("[RegulationWorker][" + name + "] The network is off, cannot perform speed control");
            return false;
        }

        if (regulationType == RegulationType.ESpeed || regulationType == RegulationType.ESpeedAmount){
            System.out.println("[RegulationWorker][" + name + "] SPEED regulation is on. Reset the speed related parameters");
        }

        if (regulationType == RegulationType.EAmount){
            System.out.println("[RegulationWorker][" + name + "] The amount regulation is on, change to amount speed regulation");
            regulationType = RegulationType.ESpeedAmount;
        } else {//ENONE
            regulationType = RegulationType.ESpeed;
        }

        this.regulationTxAllowSpeed = granularity;
        System.out.println("[RegulationWorker][" + name + "] Speed has been regulated to: " + regulationTxAllowSpeed);
        speedRegulationStartTime = TimeHelper.getEpoch();
        System.out.println("[RegulationWorker][" + name + "] Start the speed control at EPOCH: " + speedRegulationStartTime);

        return true;
    }

    public boolean startAmountRegulation(int granularity){
        if (regulationType == RegulationType.EOff)
        {
            System.out.println("[RegulationWorker][" + name + "] The network is off, cannot perform speed control");
            return false;
        }

        this.regulationTxAllowAmount = granularity;
        System.out.println("[RegulationWorker][" + name + "] Amount regulated to: " + regulationTxAllowAmount);

        if (regulationType == RegulationType.ESpeed){
            System.out.println("[RegulationWorker]" + name + "The speed regulation is on, change to amount speed regulation");
            regulationType = RegulationType.ESpeedAmount;
        } else {
            regulationType = RegulationType.EAmount;
        }

        return true;
    }

    // No matter which kind of regulation currently is,
    // Stop the regulation
    public void stopRegulation() {
        regulationType = RegulationType.ENone;
        usedTxAmount = 0;
        speedRegulationStartTime = -1;
    }

    // The caller know what kinds of regualtion type it is now
    // And want to just stop this kind of regulation
    public boolean stopRegulation(RegulationType regulationType){
        if (regulationType.equals(RegulationType.ENone)){
            System.out.println("[RegulationWorker][" + name + "][WARN] Cannot stop ENone state");
            return true;
        }

        if (regulationType.equals(this.regulationType)){ // reset all type
            usedTxAmount = 0;
            speedRegulationStartTime = -1;
            this.regulationType = RegulationType.ENone;
            return true;
        }

        if (this.regulationType.equals(RegulationType.ESpeedAmount)) {

            if (regulationType.equals(RegulationType.EAmount)) {
                this.regulationType = RegulationType.ESpeed;
            }

            if (regulationType.equals(RegulationType.ESpeed)) {
                speedRegulationStartTime = -1;
                this.regulationType = RegulationType.EAmount;
            }
            return true;
        } else {
            System.out.println("[RegulationWorker][" + name + "] Not support type: " + regulationType
                    + " and current type: " + this.regulationType);
            return false;
        }
    }

    public boolean stopSpeedRegulation(){
        if (regulationType != RegulationType.ESpeed && regulationType != RegulationType.ESpeedAmount)
        {
            System.out.println("[RegulationWorker][" + name + "] The network is in speed control state");
            return false;
        }

        speedRegulationStartTime = 0;
        if (regulationType == RegulationType.ESpeed) {
            regulationType = RegulationType.ENone;
        }
        else if (regulationType == RegulationType.ESpeedAmount){
            regulationType = regulationType.EAmount;
        }
        return true;
    }

    public boolean stopAmountRegulation(){
        if (regulationType != RegulationType.EAmount && regulationType != RegulationType.ESpeedAmount)
        {
            System.out.println("[RegulationWorker][" + name + "] The network is in amount control state");
            return false;
        }

        if (regulationType == RegulationType.EAmount) {
            regulationType = RegulationType.ENone;
        }
        else if (regulationType == RegulationType.ESpeedAmount){
            regulationType = regulationType.ESpeed;
        }
        return true;
    }

    // -1 means no control
    public int getAllowedAmount(){
        final int noRegulation = -1;
        if (regulationType.equals(RegulationType.ENone)){
            return noRegulation;
        }

        if (regulationType.equals(RegulationType.EOff)){
            return 0;
        }

        if (regulationType.equals(RegulationType.EAmount)){
            return (regulationTxAllowAmount - usedTxAmount > 0) ? regulationTxAllowAmount - usedTxAmount : 0;
        }

        int speedRegulationAmount = (int) (regulationTxAllowSpeed * getPassedSeconds());
        System.out.println("[RegulationWorker][" + name + "][DEBUG][EPOCH interval] speed regulation allowed amount: " +
                speedRegulationAmount + " : used Tx account: " + usedTxAmount +
                " with regulation type: " + regulationType +
                " with regulationTxAllowAmount: " + regulationTxAllowAmount +
                " with regulationTxAllowSpeed: " + regulationTxAllowSpeed +
                " at EPOCH %d" + TimeHelper.getEpoch());

        if (regulationType.equals(RegulationType.ESpeed)){
            return (speedRegulationAmount - usedTxAmount > 0) ? speedRegulationAmount - usedTxAmount : 0;
        }

        // RegulationType.ESpeedAmount
        int minAllowedSpeed = (speedRegulationAmount > regulationTxAllowAmount) ? regulationTxAllowAmount : speedRegulationAmount;
        return  (minAllowedSpeed -usedTxAmount > 0) ? minAllowedSpeed -usedTxAmount: 0;
    }

    private long getPassedSeconds() {
        return (TimeHelper.getEpoch() - speedRegulationStartTime) / 1000;
    }

    public void increaseUsedTxAmount(){
        ++usedTxAmount;
    }

    public int getUsedTxAmount() {
        return usedTxAmount;
    }

    public RegulationType getRegulationType() {
        return regulationType;
    }

    public int getAllowedSpeed() {
        return regulationTxAllowSpeed;
    }
}

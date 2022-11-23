package com.scu.suhong.miner;

public class ExchangeUserPair {
    int fromNumber;
    int toNumber;

    public ExchangeUserPair(int fromNumber, int toNumber) {
        this.fromNumber = fromNumber;
        this.toNumber = toNumber;
    }

    public int getToNumber() {
        return toNumber;
    }

    public void setToNumber(int toNumber) {
        this.toNumber = toNumber;
    }

    public int getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(int fromNumber) {
        this.fromNumber = fromNumber;
    }
}

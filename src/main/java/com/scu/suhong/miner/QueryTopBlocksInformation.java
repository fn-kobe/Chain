package com.scu.suhong.miner;

import java.util.ArrayList;
import java.util.List;

public class QueryTopBlocksInformation {
    private String peerAddress;
    // startBlock 100, numberOfTopBlock 4 -> 100, 99, 98, 97
    // if startBlock is -1,  numberOfTopBlock 4 -> latest, latest - 1, latest - 2, latest - 3
    private int numberOfTopBlock;
    private int startBlock = -1;
	private Integer peerPort;

    public QueryTopBlocksInformation(String peerAddress, int numberOfTopBlock, int startBlock, int peerPort) {
        this.peerAddress = peerAddress;
        this.numberOfTopBlock = numberOfTopBlock;
        this.startBlock = startBlock;
        this.peerPort = peerPort;
    }

    public String getPeerAddress() {
        return peerAddress;
    }

    public Integer getPeerPort() {
        return peerPort;
    }

    public void setPeerAddress(String peerAddress) {
        this.peerAddress = peerAddress;
    }

    public int getNumberOfQueryBlock() {
        return numberOfTopBlock;
    }

    public void setNumberOfTopBlock(int numberOfTopBlock) {
        this.numberOfTopBlock = numberOfTopBlock;
    }

    public int getStartBlockIndex() {
        return startBlock;
    }

    public void setStartBlock(int startBlock) {
        this.startBlock = startBlock;
    }
}

package com.scu.suhong.block;

import org.junit.Test;

import static org.junit.Assert.*;

public class BlockchainFileDumperTest {

    @Test
    public void getCurrentDataString() {
        BlockchainFileDumper blockchainFileDumper = new BlockchainFileDumper();
        System.out.printf(blockchainFileDumper.getCurrentDataString());
    }

    @Test
    public void dumpAll() {
        BlockchainFileDumper blockchainFileDumper = new BlockchainFileDumper();
        blockchainFileDumper.dumpAll();
    }
}
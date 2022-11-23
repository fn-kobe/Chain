package com.scu.suhong.miner;

import util.BufferHelper;

import java.io.IOException;
import java.io.PipedInputStream;

// not use
public class TransactionListener implements Runnable {
    PipedInputStream in = null;

    public PipedInputStream getPipedInputputStream() {
        in = new PipedInputStream();
        return in;
    }

    @Override
    public void run() {

        byte[] bys = new byte[BufferHelper.getMaxBufferSize()];
        try {
            in.read(bys);
            System.out.println("[TransactionListener] Message got: " + new String(bys).trim());
            in.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }
}

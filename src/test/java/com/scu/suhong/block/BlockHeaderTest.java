package com.scu.suhong.block;

import junit.framework.TestCase;

public class BlockHeaderTest extends TestCase {

    public void testCreateFromJson() {
        BlockHeader h = new BlockHeader();
        h.setBlockNounce(1234);
        h.setPreviousHash("0F233");
        BlockHeader hn = BlockHeader.createFromJson(h.getJson());
        assert h.Dump().equals(hn.Dump());
    }
}
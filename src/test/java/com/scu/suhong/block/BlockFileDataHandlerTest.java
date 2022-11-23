package com.scu.suhong.block;

import junit.framework.TestCase;

public class BlockFileDataHandlerTest extends TestCase {
    public void testCreateFolder() {
        assert BlockFileDataHandler.createFolderIfNotExist("BlockFileDataHandlerTest");
    }
}
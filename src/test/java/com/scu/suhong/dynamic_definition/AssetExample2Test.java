package com.scu.suhong.dynamic_definition;

import org.junit.Test;

import static org.junit.Assert.*;

public class AssetExample2Test {

    @Test
    public void getIntstance() {
        AssetExample2 assetExample2 = new AssetExample2();
        AssetExample2 newInstance = assetExample2.getIntstance();

        assert newInstance.check();
    }
}
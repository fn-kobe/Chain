package com.scu.suhong.ExpressDelivery;

import org.junit.Test;
import util.ThreadHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class InformationQueryTest {

    @Test
    public void doesContainFeatureString() throws IOException {
        Map parameters = new HashMap();
        parameters.put("com", "ems");
        parameters.put("nu", "1072864318531");
        boolean r = InformationQuery.doesContainFeatureString("", parameters, "\"state\":\"3\"");
        assert r;
        // The default query interval is 1000
        ThreadHelper.safeSleep(1000);
        r = InformationQuery.doesContainFeatureString("", parameters, "\"state\":\"4\"");
        assert !r;
    }

    public void getResult() throws IOException {
        Map parameters = new HashMap();
        parameters.put("com", "ems");
        parameters.put("nu", "1072864318531");
        String r = InformationQuery.getResult("", parameters);
        System.out.println(r);
    }
}
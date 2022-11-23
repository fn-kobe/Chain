package regulator;

import junit.framework.TestCase;

public class RegulationTypeTest extends TestCase {

    public void testHasRegulationType() {
        RegulationType regulationType = RegulationType.ENone;
        assert regulationType != RegulationType.ESpeedAmount;
    }
}
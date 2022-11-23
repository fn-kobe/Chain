package util;

import junit.framework.TestCase;

public class PoissonHelperTest extends TestCase {

    public void testGetPoissonRandom() {
        PoissonHelper poissonHelper = new PoissonHelper(5);
        System.out.println( poissonHelper.getPoissonRandom() );
        System.out.println( poissonHelper.getPoissonRandom() );
        System.out.println( poissonHelper.getPoissonRandom() );
        System.out.println( poissonHelper.getPoissonRandom() );
        System.out.println( poissonHelper.getPoissonRandom() );
        System.out.println( poissonHelper.getPoissonRandom() );
    }
}
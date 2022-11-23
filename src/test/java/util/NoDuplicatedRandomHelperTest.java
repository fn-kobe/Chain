package util;

import org.junit.Test;

public class NoDuplicatedRandomHelperTest {

    @Test
    public void testGetNumber() {
        int maxNumber = 6;
        NoDuplicatedRandomHelper helper = new NoDuplicatedRandomHelper(maxNumber);
        for (int i = 0; i < maxNumber; ++i){
            System.out.println(helper.getNumber());
        }
        assert -1 == helper.getNumber();

        maxNumber = 4;
        helper.reset(maxNumber);
        for (int i = 0; i < maxNumber; ++i){
            System.out.println(helper.getNumber());
        }
        assert -1 == helper.getNumber();

        maxNumber = 8;
        helper.reset(maxNumber);
        for (int i = 0; i < maxNumber; ++i){
            System.out.println(helper.getNumber());
        }
        assert -1 == helper.getNumber();
    }

    @Test
    public void testPutBack() {
        int maxNumber = 6;
        NoDuplicatedRandomHelper helper = new NoDuplicatedRandomHelper(maxNumber);
        for (int i = 0; i < maxNumber; ++i){
            int fetched = helper.getNumber();
            System.out.println(fetched);
            assert helper.getStatusClone()[fetched] == 1;
        }
        assert -1 == helper.getNumber();

        assert maxNumber == helper.getFetchedNumberList().size();
        assert 0 == helper.getUnFetchedNumberList().size();
        assert helper.isAllFetched();
        int putBackNumber  = 2;
        helper.putBack(putBackNumber);
        assert maxNumber - 1 == helper.getFetchedNumberList().size();
        assert 1 == helper.getUnFetchedNumberList().size();
        assert !helper.isAllFetched();
        assert putBackNumber == helper.getNumber();
    }
}
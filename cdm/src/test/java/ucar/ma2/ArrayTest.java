package ucar.ma2;

import org.junit.Before;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: madry
 * Date: 12/12/13
 * Time: 11:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class ArrayTest {

    private Array zeroRank;
    private IndexIterator iter;
    private int[] currentCounter;

    @Test
    public void testFactory() throws Exception {
        zeroRank = Array.factory(int.class, new int[0]);
        iter = zeroRank.getIndexIterator();
        currentCounter = iter.getCurrentCounter();
        assert currentCounter.length == 0;
    }
}

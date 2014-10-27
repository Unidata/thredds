package ucar.ma2;

import org.junit.Before;
import org.junit.Test;
import ucar.nc2.util.Misc;

import java.io.IOException;

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


  @Test
  public void testUnsigned2() {
    int nz = 1;
    int ny = 2030;
    int nx = 1354;
    int size = nz * ny * nx;

    short[] vals = new short[size];
    for (int i = 0; i < size; i++)
      vals[i] = (short) i;

    Array data = Array.factory(DataType.SHORT, new int[]{nz, ny, nx}, vals);
    data.setUnsigned(true);
    double sum = MAMath.sumDouble(data);
    double sumReduce = MAMath.sumDouble(data.reduce(0));
    assert Misc.closeEnough(sum, sumReduce);

    System.out.printf(" sum =          %f%n", MAMath.sumDouble(data));
    System.out.printf(" sum2 =          %f%n", MAMath.sumDouble(data.reduce(0)));
  }
}

package ucar.ma2;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.Assert2;

import java.lang.invoke.MethodHandles;

public class ArrayTest {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private Array zeroRank;
  private IndexIterator iter;
  private int[] currentCounter;

  @Test
  public void testFactory() throws Exception {
    zeroRank = Array.factory(DataType.INT, new int[0]);
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

    Array data = Array.factory(DataType.USHORT, new int[]{nz, ny, nx}, vals);
    double sum = MAMath.sumDouble(data);
    double sumReduce = MAMath.sumDouble(data.reduce(0));
    Assert2.assertNearlyEquals(sum, sumReduce);
  }

  // Demonstrates bug in https://github.com/Unidata/thredds/issues/581.
  @Test
  public void testConstantArray_get1DJavaArray() {
    Array array = Array.factoryConstant(DataType.INT, new int[] {3}, new int[] {47});

    // Prior to fix, the actual value returned by get1DJavaArray was {47}.
    Assert.assertArrayEquals(new int[] {47, 47, 47}, (int[]) array.get1DJavaArray(int.class));
  }

  @Test
  public void testConstantArray_createView() {
    // For all Array subtypes, assert that we can create a logical view of an Array that uses an IndexConstant.
    // This is a regression test for bug fixes I made to the various Array*.factory(Index, ...) methods.
    // Prior to the fixes, this test would raise exceptions such as:
    //   java.lang.ClassCastException: ucar.ma2.IndexConstant cannot be cast to ucar.ma2.Index1D
    //      at ucar.ma2.ArrayBoolean$D1.<init>(ArrayBoolean.java:235)
    //      at ucar.ma2.ArrayBoolean$D1.<init>(ArrayBoolean.java:226)
    //      at ucar.ma2.ArrayBoolean.factory(ArrayBoolean.java:60)
    //      at ucar.ma2.ArrayBoolean.createView(ArrayBoolean.java:103)
    //      at ucar.ma2.Array.reduce(Array.java:922)
    //      at ucar.ma2.ArrayTest.testConstantArray_reduce(ArrayTest.java:58)
    for (DataType dataType : DataType.values()) {
      Array array = Array.factoryConstant(dataType, new int[] {1, 3, 1}, null);
      Assert.assertArrayEquals(new int[] { 3 }, array.reduce().getShape());
    }
  }
}

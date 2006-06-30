package ucar.nc2;

import junit.framework.*;
import ucar.ma2.*;

import java.io.*;

/** Test nc2 read JUnit framework. */

public class TestH5eos extends TestCase {

  public TestH5eos(String name) {
    super(name);
  }

  public void testEosMetadata() {
    NetcdfFile ncfile = TestH5.open("c:/data/hdf5/HIRDLS/HIRDLS2_v0.3.1-aIrix-c3_2003d106.h5");

    Variable dset = ncfile.findVariable("HDFEOS_INFORMATION/StructMetadata-0");
    assert(null != dset );
    assert(dset.getDataType() == DataType.CHAR);

    // read entire array
    Array A;
    try {
      A = dset.read();
    }
    catch (IOException e) {
      System.err.println("ERROR reading file");
      assert(false);
      return;
    }
    assert(A.getRank() == 1);
    assert (A instanceof ArrayChar);

    ArrayChar ca = (ArrayChar) A;
    String sval = ca.getString();
    System.out.println("Len = "+sval.length());
    System.out.println("Value = "+sval);

    ////////////////
    dset = ncfile.findVariable("HDFEOS_INFORMATION/coremetadata-0");
    assert(null != dset );
    assert(dset.getDataType() == DataType.CHAR);

    // read entire array
    try {
      A = dset.read();
    }
    catch (IOException e) {
      System.err.println("ERROR reading file");
      assert(false);
      return;
    }
    assert(A.getRank() == 1);
    assert (A instanceof ArrayChar);

    ca = (ArrayChar) A;
    sval = ca.getString();
    System.out.println("Len = "+sval.length());
    System.out.println("Value = "+sval);
  }

  public static void main(String[] args) {
    TestH5eos test = new TestH5eos("fake");
    //test.readAllData( "c:/data/hdf5/msg/test.h5");
    //test.readAllData( "c:/data/hdf5/HIRDLS/HIRDLS2_v0.3.1-aIrix-c3_2003d106.h5");
    test.testEosMetadata();
  }
}

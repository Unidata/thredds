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
    //NetcdfFile ncfile = TestH5.open("c:/data/hdf5/HIRDLS/HIRDLS2_v0.3.1-aIrix-c3_2003d106.h5");
    NetcdfFile ncfile = TestH5.open("C:/doc/hdf5Conference/auraData/HIRDLS2-Aura73p_b029_2000d275.he5");

    Group root = ncfile.getRootGroup();
    Group g = root.findGroup("HDFEOS_INFORMATION");
    Variable dset = g.findVariable("StructMetadata.0");
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
    System.out.println(dset.getName());
    System.out.println(" Length = "+sval.length());
    System.out.println(" Value = "+sval);

    ////////////////
    dset = g.findVariable("coremetadata.0");
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
    System.out.println(dset.getName());
    System.out.println(" Length = "+sval.length());
    System.out.println(" Value = "+sval);
  }

  public static void main(String[] args) {
    TestH5eos test = new TestH5eos("fake");
    //test.readAllData( "c:/data/hdf5/msg/test.h5");
    //test.readAllData( "c:/data/hdf5/HIRDLS/HIRDLS2_v0.3.1-aIrix-c3_2003d106.h5");
    test.testEosMetadata();
  }
}

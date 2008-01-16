package ucar.nc2.iosp.hdf5;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.TestAll;

import java.io.*;

/** Test nc2 read JUnit framework. */

public class TestH5aura extends TestCase {

  public TestH5aura(String name) {
    super(name);
  }

  public void test1() throws IOException {
    //H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    NetcdfFile ncfile = TestH5.open(TestAll.upcShareTestDataDir+"hdf5/eos/HIRDLS/HIRDLS1_v4.0.2a-aIrix-c2_2003d106.he5");
    Variable dset = ncfile.findVariable("HDFEOS/SWATHS/HIRDLS_L1_Swath/Data Fields/Scaled Ch01 Radiance");
    dset.read();
  }

  public void test2() throws IOException {
    NetcdfFile ncfile = TestH5.open(TestAll.upcShareTestDataDir+"hdf5/eos/HIRDLS/HIRDLS2-AFGL_b027_na.he5");
    Variable dset = ncfile.findVariable("HDFEOS/SWATHS/HIRDLS/Data Fields/Altitude");
    
    //H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/dataBtree"));
    Array data = dset.read();
    assert data.getElementType() == float.class;
  }

  public void testEosMetadata() {
    //NetcdfFile ncfile = TestH5.open("c:/data/hdf5/HIRDLS/HIRDLS2_v0.3.1-aIrix-c3_2003d106.h5");
    NetcdfFile ncfile = TestH5.open(TestAll.upcShareTestDataDir+"hdf5/eos/HIRDLS/HIRDLS2-Aura73p_b029_2000d275.he5");

    Group root = ncfile.getRootGroup();
    Group g = root.findGroup("HDFEOS INFORMATION");
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
    TestH5aura test = new TestH5aura("fake");
    //test.readAllData( "c:/data/hdf5/msg/test.h5");
    //test.readAllData( "c:/data/hdf5/HIRDLS/HIRDLS2_v0.3.1-aIrix-c3_2003d106.h5");
    test.testEosMetadata();
  }
}

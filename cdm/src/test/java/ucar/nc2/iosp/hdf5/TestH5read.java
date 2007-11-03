package ucar.nc2.iosp.hdf5;

import junit.framework.*;

import java.io.*;
import java.util.*;

import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.NCdump;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Array;

/** Test nc2 read JUnit framework. */

public class TestH5read extends TestCase {

  public TestH5read( String name) {
    super(name);
  }

 public void testH5data() {
     readAllDir ("C:/data/testdata");
 }

  public void testSamples() {
    //H5header.setDebugFlags( new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    readAllDir( TestAll.upcShareTestDataDir + "hdf5/support");
    readAllDir( TestAll.upcShareTestDataDir + "hdf5/samples");
    readAllDir( TestAll.upcShareTestDataDir + "hdf5/complex");
  }

  public void testAll() {
    readAllDir( TestAll.upcShareTestDataDir + "hdf5/auraData");
    readAllDir( TestAll.upcShareTestDataDir + "hdf5/IASI");
    readAllDir( TestAll.upcShareTestDataDir + "hdf5/msg");
    readAllDir( TestAll.upcShareTestDataDir + "hdf5/wrf");
  }

  public void problemV() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = TestAll.upcShareTestDataDir + "hdf5/IASI/IASI_xxx_1C_M02_20070704193256Z_20070704211159Z_N_O_20070704211805Z.h5";
    NetcdfFile ncfile = NetcdfFile.open(filename);
    Variable v = ncfile.findVariable("/U-MARF/EPS/IASI_xxx_1C/DATA/SPECT_LAT_ARRAY");
    Array data = v.read();
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    NCdump.printArray(data, "primary_cloud", System.out, null);
    ncfile.close();
  }

  public void problem() {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    readAllData( "C:/data/testdata/compound/enumcmpnd.h5");
  }

  public static void readAllDir(String dirName) {
    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID "+dirName);
      return;
    }

    for (int i = 0; i < allFiles.length; i++) {
      String name = allFiles[i].getAbsolutePath();
      if (name.endsWith(".h5") || name.endsWith(".H5") || name.endsWith(".he5") || name.endsWith(".nc"))
        readAllData(name);
    }

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory())
        readAllDir(allFiles[i].getAbsolutePath());
    }

  }

  static public void readAllData( String filename) {
    System.out.println("\n------Reading filename "+filename);
    try {
      NetcdfFile ncfile = TestH5.open(filename);
      //System.out.println("\n"+ncfile);

      for (Variable v : ncfile.getVariables()) {
        if (v.getSize() > max_size) {
          Section s = makeSubset(v);
          System.out.println("  Try to read variable " + v.getNameAndDimensions() + " size= " + v.getSize() + " section= " + s);
          v.read(s);
        } else {
          System.out.println("  Try to read variable " + v.getNameAndDimensions() + " size= " + v.getSize());
          v.read();
        }
      }
      ncfile.close();
    } catch (Exception e) {
      e.printStackTrace();
      assert false;
    }
  }

  static int max_size = 1000 * 1000 * 10;
  static Section makeSubset(Variable v) throws InvalidRangeException {
    int[] shape = v.getShape();
    shape[0] = 1;
    Section s = new Section(shape);
    long size = s.computeSize();
    shape[0] = (int) Math.max(1, max_size / size);
    return new Section(shape);
  }



  public static void main(String[] args) {
    TestH5read test = new TestH5read("fake");
    //test.readAllData( "c:/data/hdf5/msg/test.h5");
    //test.readAllData( "c:/data/hdf5/HIRDLS/HIRDLS2_v0.3.1-aIrix-c3_2003d106.h5");
    test.readAllDir( "c:/data/hdf5/HIRDLS");
  }

}

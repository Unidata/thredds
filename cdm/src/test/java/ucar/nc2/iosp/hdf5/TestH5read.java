package ucar.nc2.iosp.hdf5;

import junit.framework.*;

import java.io.*;
import java.util.*;

import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.ma2.Section;
import ucar.ma2.InvalidRangeException;

/** Test nc2 read JUnit framework. */

public class TestH5read extends TestCase {

  public TestH5read( String name) {
    super(name);
  }

  public void test1() {
    readAllDir( TestAll.upcShareTestDataDir + "hdf5/support");
    readAllDir( TestAll.upcShareTestDataDir + "hdf5/samples");
    readAllDir( TestAll.upcShareTestDataDir + "hdf5/complex");
    readAllDir( TestAll.upcShareTestDataDir + "hdf5/msg");
  }

  public void ntestNc4() {
    readAllDir( TestAll.upcShareTestDataDir + "netcdf4");
  }

  public void testAll() {
    readAllDir( TestAll.upcShareTestDataDir + "hdf5/");
  }

  void readAllDir(String dirName) {
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

  int max_size = 1000 * 1000 * 10;
  Section makeSubset(Variable v) throws InvalidRangeException {
    int[] shape = v.getShape();
    shape[0] = 1;
    Section s = new Section(shape);
    long size = s.computeSize();
    shape[0] = (int) Math.max(1, max_size / size);
    return new Section(shape);
  }

  void readAllData( String filename) {
    System.out.println("------Reading filename "+filename);
    try {
      NetcdfFile ncfile = TestH5.open(filename);
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

  public static void main(String[] args) {
    TestH5read test = new TestH5read("fake");
    //test.readAllData( "c:/data/hdf5/msg/test.h5");
    //test.readAllData( "c:/data/hdf5/HIRDLS/HIRDLS2_v0.3.1-aIrix-c3_2003d106.h5");
    test.readAllDir( "c:/data/hdf5/HIRDLS");
  }

}

package ucar.nc2;

import junit.framework.*;

import java.io.*;
import java.util.*;

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

  public void testAll() {
    readAllDir( TestAll.upcShareTestDataDir + "hdf5/");
  }

  void readAllDir(String dirName) {
    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();

    for (int i = 0; i < allFiles.length; i++) {
      String name = allFiles[i].getAbsolutePath();
      if (name.endsWith(".h5") || name.endsWith(".H5") || name.endsWith(".he5"))
        readAllData(name);
    }

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory())
        readAllDir(allFiles[i].getAbsolutePath());
    }

  }

  void readAllData( String filename) {
    System.out.println("------Reading filename "+filename);
    try {
      NetcdfFile ncfile = TestH5.open(filename);
      for (Iterator iter = ncfile.getVariables().iterator(); iter.hasNext(); ) {
        Variable v = (Variable) iter.next();
        System.out.println("  Try to read variable "+v.getName());
        v.read();
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

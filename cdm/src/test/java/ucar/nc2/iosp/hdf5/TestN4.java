package ucar.nc2.iosp.hdf5;

import junit.framework.*;
import ucar.nc2.*;
import ucar.nc2.util.Misc;
import ucar.ma2.Section;
import ucar.ma2.Array;

import java.io.*;
import java.util.StringTokenizer;
import java.util.Collections;
import java.util.List;

/**
 * Test nc2 read JUnit framework.
 */

public class TestN4 extends TestCase {

  public TestN4(String name) {
    super(name);
  }

  public void testOpen() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/tst_enums.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    List<Variable> vars = ncfile.getVariables();
    Collections.sort(vars);
    for (Variable v : vars) System.out.println(" "+v.getName());
    System.out.println("nvars = "+ncfile.getVariables().size());
    ncfile.close();
  }

  public void testReadOne() throws IOException {
    //H5iosp.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5iosp/read"));
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/c0.nc";
    TestH5read.readAllData(filename);
  }

  public void test() throws IOException {
    //H5iosp.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5iosp/read"));
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/c0.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    Variable v = ncfile.findVariable("c213");
    Array data = v.read();
  }

  public void testEnum() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/tst_enum_data.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    Variable v = ncfile.findVariable("primary_cloud");
    Array data = v.read();
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    NCdump.printArray(data, "primary_cloud", System.out, null);
    ncfile.close();
  }

  public void testVlenStrings() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/tst_strings.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    Variable v = ncfile.findVariable("measure_for_measure_var");
    Array data = v.read();
    NCdump.printArray(data, "measure_for_measure_var", System.out, null);
    ncfile.close();
  }

  public void testCompoundVlens() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/cdm_sea_soundings.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    Variable v = ncfile.findVariable("fun_soundings");
    Array data = v.read();
    NCdump.printArray(data, "fun_soundings", System.out, null);
    ncfile.close();
  }

  public void testStrings() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/nc_test_netcdf4.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    Variable v = ncfile.findVariable("d");
    String attValue = ncfile.findAttValueIgnoreCase(v, "c", null);
    String s = H5header.showBytes(attValue.getBytes());
    System.out.println(" d:c= ("+attValue+") = "+s);
    //Array data = v.read();
    //NCdump.printArray(data, "cr", System.out, null);
    ncfile.close();
  }

  public void testReadAll() {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl(""));
    readAllDir("C:/data/netcdf4/files/");
  }

  public void readAllDir(String dirName) {
    System.out.println("---------------Reading directory " + dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      System.out.println("---------------INVALID " + dirName);
      return;
    }

    for (int i = 0; i < allFiles.length; i++) {
      String name = allFiles[i].getAbsolutePath();
      if (name.endsWith(".h5") || name.endsWith(".H5") || name.endsWith(".he5") || name.endsWith(".nc"))
        TestH5read.readAllData(name);
    }

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory())
        readAllDir(allFiles[i].getAbsolutePath());
    }

  }

  public static void main(String args[]) {
    double d1 = Double.parseDouble("-1.e+36f");
    double d2 = Double.parseDouble("-1.0E36f");
    System.out.println("d="+d1+" "+d2+" "+Misc.closeEnough(d1, d2));
  }

}

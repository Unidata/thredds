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
  String testDir = TestAll.upcShareTestDataDir + "netcdf4/";
  public TestN4(String name) {
    super(name);
  }

  public void testOpen() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = testDir+"nc4/tst_enums.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    List<Variable> vars = ncfile.getVariables();
    Collections.sort(vars);
    for (Variable v : vars) System.out.println(" "+v.getName());
    System.out.println("nvars = "+ncfile.getVariables().size());
    ncfile.close();
  }

  public void testReadAll() throws IOException {
    TestAll.readAllDir(testDir+"nc4", null);
    TestAll.readAllDir(testDir+"nc4-classic", null);
    TestAll.readAllDir(testDir+"files", null);
  }

  public void problem() throws IOException {
    //H5iosp.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5iosp/read"));
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = testDir+"files/nctest_64bit_offset.nc";
    TestAll.readAll(filename);
    NetcdfFile ncfile = TestNC2.open(filename);
    System.out.println(ncfile.toString());
    //Variable v = ncfile.findVariable("cr");
    //Array data = v.read();
  }

  public void utestEnum() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = testDir+"nc4/tst_enum_data.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    Variable v = ncfile.findVariable("primary_cloud");                        
    Array data = v.read();
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    NCdump.printArray(data, "primary_cloud", System.out, null);
    ncfile.close();
  }

  public void testVlenStrings() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = testDir+"nc4/tst_strings.nc";
    NetcdfFile ncfile = TestNC2.open(filename);
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    Variable v = ncfile.findVariable("measure_for_measure_var");
    Array data = v.read();
    NCdump.printArray(data, "measure_for_measure_var", System.out, null);
    ncfile.close();
  }

  public void testCompoundVlens() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = testDir+"vlen/cdm_sea_soundings.nc4";
    NetcdfFile ncfile = TestNC2.open(filename);
    System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
    Variable v = ncfile.findVariable("fun_soundings");
    Array data = v.read();
    NCdump.printArray(data, "fun_soundings", System.out, null);
    ncfile.close();
  }

  public void testStrings() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = testDir+"files/nc_test_netcdf4.nc4";
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

  public static void main(String args[]) throws IOException {
    new TestN4("").problem();
  }

}

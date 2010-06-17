/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.iosp.hdf5;

import junit.framework.*;
import ucar.nc2.*;
import ucar.ma2.Array;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;

import java.io.*;
import java.util.Collections;
import java.util.List;

/**
 * Test nc2 read JUnit framework.
 */

public class TestN4 extends TestCase {
  String testDir = TestAll.testdataDir + "netcdf4/";
  public TestN4(String name) {
    super(name);
  }

  public void testGlobalHeapOverun() throws IOException {
    // Global Heap 1t 13059 runs out with no heap id = 0
    String filename = testDir+"globalHeapOverrun.nc4";
    NetcdfFile ncfile = TestNC2.open(filename);
    System.out.println("\n**** testGlobalHeapOverun done\n\n" + ncfile);
    List<Variable> vars = ncfile.getVariables();
    Collections.sort(vars);
    for (Variable v : vars) System.out.println(" "+v.getName());
    System.out.println("nvars = "+ncfile.getVariables().size());
    ncfile.close();
  }

  // margolis@ucar.edu
  // I really don't think this is a problem with your code
  // may be bug in HDF5 1.8.4-patch1
  public void utestTiling() throws IOException {
    // Global Heap 1t 13059 runs out with no heap id = 0
    String filename = testDir+"tiling.nc4";
    GridDataset gridDataset = GridDataset.open(filename);
    GridDatatype grid = gridDataset.findGridByName("Turbulence_SIGMET_AIRMET" );
    System.out.printf("grid=%s%n", grid);
    grid.readDataSlice( 4, 13, 176, 216 ); // FAILS
    gridDataset.close();
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

  // LOOK this ones failing
  public void utestCompoundVlens() throws IOException {
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

package ucar.nc2.iosp.hdf5;

import junit.framework.*;
import ucar.nc2.*;
import ucar.ma2.Section;
import ucar.ma2.Array;

import java.io.IOException;
import java.io.File;

/** Test nc2 read JUnit framework. */

public class TestN4 extends TestCase {

  public TestN4( String name) {
    super(name);
  }

  public void testOpen() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/tst_solar_2.nc";
    NetcdfFile ncfile = TestNC2.open( filename);
    System.out.println( "\n**** testReadNetcdf4 done\n\n"+ncfile);
    ncfile.close();
  }

  public void testReadAll() throws IOException {
    //H5iosp.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5iosp/read"));
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header H5header/Heap"));
    String filename = "C:/data/netcdf4/files/tst_vl.nc";
    TestH5read.readAllData(filename);
  }

  public void testEnum() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/tst_enum_data.nc";
    NetcdfFile ncfile = TestNC2.open( filename);
    Variable v = ncfile.findVariable("primary_cloud");
    Array data = v.read();
    System.out.println( "\n**** testReadNetcdf4 done\n\n"+ncfile);
    NCdump.printArray(data, "primary_cloud", System.out, null);
    ncfile.close();
  }

  public void testVlenStrings() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/tst_strings.nc";
    NetcdfFile ncfile = TestNC2.open( filename);
    System.out.println( "\n**** testReadNetcdf4 done\n\n"+ncfile);
    Variable v = ncfile.findVariable("measure_for_measure_var");
    Array data = v.read();
    NCdump.printArray(data, "measure_for_measure_var", System.out, null);
    ncfile.close();
  }

  public void testCompoundVlens() throws IOException {
    //H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/cdm_sea_soundings.nc";
    NetcdfFile ncfile = TestNC2.open( filename);
    System.out.println( "\n**** testReadNetcdf4 done\n\n"+ncfile);
    Variable v = ncfile.findVariable("fun_soundings");
    Array data = v.read();
    NCdump.printArray(data, "fun_soundings", System.out, null);
    ncfile.close();
  }

  public void testStrings() throws IOException {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl("H5header/header"));
    String filename = "C:/data/netcdf4/files/c0.nc";
    NetcdfFile ncfile = TestNC2.open( filename);
    System.out.println( "\n**** testReadNetcdf4 done\n\n"+ncfile);
    Variable v = ncfile.findVariable("cr");
    Array data = v.read();
    NCdump.printArray(data, "cr", System.out, null);
    ncfile.close();
  }

  public void testAll() {
    H5header.setDebugFlags(new ucar.nc2.util.DebugFlagsImpl(""));
    readAllDir("C:/data/netcdf4/files/");
  }

  public void readAllDir(String dirName) {
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
        TestH5read.readAllData(name);
    }

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory())
        readAllDir(allFiles[i].getAbsolutePath());
    }

  }



}

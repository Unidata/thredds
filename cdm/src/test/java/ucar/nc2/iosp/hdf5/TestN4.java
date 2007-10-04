package ucar.nc2.iosp.hdf5;

import junit.framework.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.TestNC2;
import ucar.nc2.TestAll;
import ucar.nc2.Variable;
import ucar.ma2.Section;

import java.io.IOException;
import java.io.File;

/** Test nc2 read JUnit framework. */

public class TestN4 extends TestCase {

  public TestN4( String name) {
    super(name);
  }

  public void testReadHdf5() throws IOException {
    NetcdfFile ncfile = TestNC2.open( TestAll.upcShareTestDataDir + "hdf5/support/bool.h5");
    System.out.println( "**** testReadNetcdf4 done "+ncfile);
    ncfile.close();
  }

  public void testReadNetcdf4() throws IOException {
    //String filename = TestAll.upcShareTestDataDir + "netcdf4/nc4_pres_temp_4D.nc";
    String filename = "C:/data/netcdf4/groups.nc";
    NetcdfFile ncfile = TestNC2.open( filename);
    System.out.println( "\n**** testReadNetcdf4 done\n "+ncfile);
    ncfile.close();
  }

  public void testAll() {
    readAllDir("C:/data/netcdf4/");  
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

package ucar.nc2.dataset;

import junit.framework.*;

import java.io.*;

import ucar.nc2.TestAll;

/** Test writing and reading back. */

public class TestJustRead extends TestCase {
  private boolean show = false;

  public TestJustRead( String name) {
    super(name);
  }

  public void testReadConventionFiles() throws Exception {
    readAllDir( TestAll.getUpcSharePath()+ "/testdata/grid/netcdf");
  }

  void readAllDir(String dirName) throws Exception {
    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();

    for (int i = 0; i < allFiles.length; i++) {
      String name = allFiles[i].getAbsolutePath();
      if (name.endsWith(".nc"))
        doOne(name);
    }

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory())
        readAllDir(allFiles[i].getAbsolutePath());
    }

  }

  private void doOne(String filename) throws Exception {
    System.out.println("  read dataset with convention parsing= "+filename);
    NetcdfDataset ncDataset = NetcdfDataset.openDataset( filename, true, null);
    if (show) ncDataset.writeNcML( System.out, null);
    ncDataset.close();
  }
}

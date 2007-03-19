package ucar.nc2.iosp.grib;

import junit.framework.*;

import java.io.*;

import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

/** Test writing and reading back. */

public class TestJustRead extends TestCase {
  private boolean show = false;

  public TestJustRead( String name) {
    super(name);
  }

  public void testReadGrib1Files() throws Exception {
    readAllDir( TestAll.getUpcSharePath()+ "/testdata/motherlode/grid", "grib1");
  }

  public void testReadGrib2Files() throws Exception {
    readAllDir( TestAll.getUpcSharePath()+ "/testdata/motherlode/grid", "grib2");
  }

  void readAllDir(String dirName, String suffix) throws Exception {
    System.out.println("---------------Reading directory "+dirName);
    File allDir = new File( dirName);
    File[] allFiles = allDir.listFiles();

    for (int i = 0; i < allFiles.length; i++) {
      String name = allFiles[i].getAbsolutePath();
      if (name.endsWith(suffix))
        doOne(name);
    }

    for (int i = 0; i < allFiles.length; i++) {
      File f = allFiles[i];
      if (f.isDirectory())
        readAllDir(allFiles[i].getAbsolutePath(), suffix);
    }

  }

  private void doOne(String filename) throws Exception {
    System.out.println("read file= "+filename);
    NetcdfFile ncfile = NetcdfDataset.openFile( filename, null);
    System.out.println(" Generating_Process_or_Model="+ncfile.findAttValueIgnoreCase(null, "Generating_Process_or_Model", "NONE"));
    ncfile.close();
  }
}

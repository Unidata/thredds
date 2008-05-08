package ucar.nc2.dataset;

import junit.framework.TestCase;
import ucar.nc2.TestAll;
import ucar.nc2.NetcdfFile;
import ucar.nc2.TestCompare;
import ucar.nc2.dataset.NetcdfDatasetCache;

import java.io.File;

public class TestDatasetWrap extends TestCase {

  public TestDatasetWrap( String name) {
    super(name);
  }

  public void testDatasetWrap() throws Exception {
    doOne(TestAll.upcShareTestDataDir+ "grid/netcdf/nuwg/eta.nc");
    //readAllDir( TestAll.testdataDir+ "grid/netcdf");
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
    NetcdfFile ncfile = NetcdfDataset.acquireFile(filename, null);
    NetcdfDataset ncWrap = new NetcdfDataset( ncfile, true);

    NetcdfDataset ncd = NetcdfDatasetCache.acquire(filename, null);
    System.out.println(" dataset wraps= "+filename);

    TestCompare.compareFiles(ncd, ncWrap);
    ncd.close();
    ncWrap.close();
  }
}

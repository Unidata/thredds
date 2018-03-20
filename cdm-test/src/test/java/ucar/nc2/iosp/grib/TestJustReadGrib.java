/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.grib;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test reading grib files */
@Category(NeedsCdmUnitTest.class)
public class TestJustReadGrib  {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private boolean show = false;

  @Test
  public void readGrib1Files() throws Exception {
    readAllDir( TestDir.cdmUnitTestDir + "formats/grib1", null, false);
  }

  @Test
  public void readGrib2Files() throws Exception {
    readAllDir( TestDir.cdmUnitTestDir + "formats/grib2", null, false);
  }

  @Test
  public void readNcepFiles() throws Exception {
    readAllDir( TestDir.cdmUnitTestDir + "tds/ncep", null, true);
  }

  @Test
  public void readFnmocFiles() throws Exception {
    readAllDir( TestDir.cdmUnitTestDir + "tds/fnmoc", null, true);
  }

  void readAllDir(String dirName, String suffix, boolean recurse) throws Exception {
    TestDir.actOnAll(dirName, new GribFilter(), new GribAct(), recurse);
  }

  @Test
  public void testProblem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib1/testproj2.grb";
    System.out.println("read file= "+filename);
    NetcdfFile ncfile = NetcdfDataset.openFile( filename, null);
    ncfile.close();
  }

  class GribFilter implements FileFilter {

    @Override
    public boolean accept(File file) {
      if (file.isDirectory()) return false;
      String name = file.getName();
      if (name.contains(".gbx")) return false;
      if (name.contains(".ncx")) return false;
      try {
        System.out.printf("opening %s%n", file.getCanonicalPath());
      } catch (IOException e) {
        e.printStackTrace();
      }
      return true;
    }
  }

  class GribAct implements TestDir.Act {

    @Override
    public int doAct(String filename) throws IOException {
      System.out.println("read file= "+filename);
      try (NetcdfFile ncfile = NetcdfDataset.openFile( filename, null)) {
        return 1;
      }
    }
  }
}

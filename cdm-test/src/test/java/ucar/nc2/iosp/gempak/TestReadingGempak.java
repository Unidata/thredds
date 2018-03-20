/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
/**
 * User: rkambic
 * Date: Oct 22, 2009
 * Time: 3:12:19 PM
 */

package ucar.nc2.iosp.gempak;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

@Category(NeedsCdmUnitTest.class)
public class TestReadingGempak {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testCompare() throws IOException {
    doAll(TestDir.cdmUnitTestDir + "formats/gempak");
  }

  void doAll(String dirName) throws IOException {
    File dir = new File(dirName);
    System.out.printf("%nIn directory %s%n", dir.getPath());
    for (File child : dir.listFiles()) {
      if (child.isDirectory()) continue;
      System.out.printf("  Open File %s ", child.getPath());
      long start = System.currentTimeMillis();

      try ( NetcdfFile ncfile = NetcdfDataset.openFile(child.getPath(), null)) {
        String ft = ncfile.findAttValueIgnoreCase(null, "featureType", "none");
        String iosp = ncfile.getIosp().getFileTypeId();
        System.out.printf(" iosp=%s ft=%s took =%d ms%n", iosp, ft, (System.currentTimeMillis() - start));

      } catch (Throwable t) {
        System.out.printf(" FAILED =%s%n", t.getMessage());
        t.printStackTrace();
      }
    }

    for (File child : dir.listFiles()) {
      if (child.isDirectory()) doAll(child.getPath());
    }

  }

}

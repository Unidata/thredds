/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.lang.invoke.MethodHandles;

/**
 * Test DiskCache2
 *
 * @author caron
 * @since 7/21/2014
 */
public class TestDiskCache {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // https://github.com/Unidata/thredds/issues/58  from Cameron Beccario
  @Test
  public void testNotExist() throws Exception {
    DiskCache2 cache = DiskCache2.getDefault();
    File file = cache.getFile("gfs.t00z.master.grbf00.10m.uv.grib2"); // not exist
    System.out.printf("canWrite= %s%n", file.canWrite());
    assert !file.canWrite();
  }

  public void testReletivePath() throws Exception {
    String org = System.getProperty("user.dir");
    try {
      System.setProperty("user.dir", TestDir.cdmUnitTestDir);
      System.out.printf("user.dir = %s%n", System.getProperty("user.dir"));
      File pwd = new File(System.getProperty("user.dir"));

      String filename = "transforms/albers.nc";
      File rel2 = new File(pwd, filename);
      System.out.printf("abs = %s%n", rel2.getCanonicalFile());
      assert rel2.exists();
      assert rel2.canWrite();
    } finally {
      System.setProperty("user.dir", org);
    }
  }

}

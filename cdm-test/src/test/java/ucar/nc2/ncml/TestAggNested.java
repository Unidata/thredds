/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Variable;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Describe
 *
 * @author caron
 * @since 3/5/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestAggNested {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void TestNotCached() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ncml/nestedAgg/test.ncml";

    try (NetcdfDataset ncd = NetcdfDataset.acquireDataset(DatasetUrl.findDatasetUrl(filename), true ,null)) {
      Variable time = ncd.findVariable("time");
      assert time != null;
      assert time.getSize() == 19723 : time.getSize();
      // System.out.printf(" time array = %s%n", NCdumpW.toString(time.read()));
    }
  }

  @Test
  public void TestCached() throws IOException {
    try {
      NetcdfDataset.initNetcdfFileCache(10, 20, -1);

    String filename = TestDir.cdmUnitTestDir + "ncml/nestedAgg/test.ncml";
    try (NetcdfDataset ncd = NetcdfDataset.acquireDataset(DatasetUrl.findDatasetUrl(filename), true , null)) {
      Variable time = ncd.findVariable("time");
      assert time != null;
      assert time.getSize() == 19723 : time.getSize();
      //System.out.printf(" time array = %s%n", NCdumpW.toString(time.read()));
    }

      FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
      cache.showCache();
    } finally {
      NetcdfDataset.shutdown();
    }
  }

  /*@Test
  public void TestCached() throws IOException, InvalidRangeException {
    NetcdfDataset.initNetcdfFileCache(10, 20, -1);

    String filename = TestDir.cdmUnitTestDir + "agg/nestedAgg/test.ncml";
    boolean ok = true;

    System.out.printf("==========%n");
    for (int i=0; i<2; i++) {
      NetcdfDataset ncd = NetcdfDataset.acquireDataset(filename, null);
      NetcdfDataset ncd2 = NetcdfDataset.wrap(ncd, NetcdfDataset.getEnhanceAll());
      Formatter out = new Formatter();
      ok &= CompareNetcdf2.compareFiles(ncd, ncd2, out, false, false, false);
      System.out.printf("----------------%nfile=%s%n%s%n", filename, out);

      EnumSet<NetcdfDataset.Enhance> modes =  ncd2.getEnhanceMode();
      showModes(modes);
      ncd2.close();
      System.out.printf("==========%n");
    }

    FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
    cache.showCache();
    assert ok;
  }  */

}

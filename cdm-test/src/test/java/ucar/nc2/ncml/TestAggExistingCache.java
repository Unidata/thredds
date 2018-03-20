/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;

/**
 * Test aggregation cache is getting used
 *
 * @author caron
 * @since 5/21/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestAggExistingCache {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  String ncml =
          "<?xml version='1.0' encoding='UTF-8'?>\n" +
                  "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
                  "    <aggregation dimName='time' type='joinExisting' recheckEvery='15 min'>\n" +
                  "      <variableAgg name='ATssta' />\n" +
                  "      <scan dateFormatMark='AT#yyyyDDD_HHmmss' location='" + TestDir.cdmUnitTestDir + "ncml/nc/pfeg/' suffix='.nc' />\n" +
                  "    </aggregation>\n" +
                  "</netcdf>";

  @Test
  public void testCacheIsUsed() throws IOException, InvalidRangeException {
    String filename = "file:TestAggExistingCache.xml";
    System.out.printf("%s%n", filename);

    String cacheDirName = tempFolder.newFolder().getAbsolutePath() + "/";
    System.out.printf("cacheDir=%s%n", cacheDirName);
    File cacheDir = new File(cacheDirName);
    FileUtils.deleteDirectory(cacheDir); // from commons-io
    assert !cacheDir.exists();

    DiskCache2 cache = new DiskCache2(cacheDirName, false, 0, 0);
    cache.setAlwaysUseCache(true);
    Assert.assertEquals(cache.getRootDirectory(), cacheDirName);
    assert new File(cache.getRootDirectory()).exists();

    Aggregation.setPersistenceCache(cache);
    AggregationExisting.countCacheUse = 0;

    try (NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), filename, null)) {
      System.out.println(" TestNcmlAggExisting.open " + filename);
      Array ATssta = ncfile.readSection("ATssta(:,0,0,0)");
      Assert.assertEquals(4, ATssta.getSize());
    }
    Assert.assertEquals(0, AggregationExisting.countCacheUse);
    AggregationExisting.countCacheUse = 0;

    try (NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), filename, null)) {
      System.out.println(" TestNcmlAggExisting.open " + filename);
      Array ATssta = ncfile.readSection("ATssta(:,0,0,0)");
      Assert.assertEquals(4, ATssta.getSize());
    }
    Assert.assertEquals(8, AggregationExisting.countCacheUse);
  }

  String ncml2 =
    "<?xml version='1.0' encoding='UTF-8'?>\n" +
    "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
    "    <aggregation dimName='time' type='joinExisting' timeUnitsChange='true'>\n" +
    "      <scan location='B:/CM2.1R' suffix='.nc' />\n" +
    "    </aggregation>\n" +
    "</netcdf>";
  @Ignore("files not available")
  @Test
  public void testCacheTiming() throws IOException, InvalidRangeException {
    String filename = "file:testCacheTiming.xml";
    System.out.printf("%s%n", filename);

    String cacheDirName = tempFolder.newFolder().getAbsolutePath();
    System.out.printf("cacheDir=%s%n", cacheDirName);
    File cacheDir = new File(cacheDirName);
    FileUtils.deleteDirectory(cacheDir); // from commons-io
    assert !cacheDir.exists();

    DiskCache2 cache = new DiskCache2(cacheDirName, false, 0, 0);
    cache.setAlwaysUseCache(true);
    Assert.assertEquals(cache.getRootDirectory(), cacheDirName);
    assert new File(cache.getRootDirectory()).exists();

    Aggregation.setPersistenceCache(cache);
    AggregationExisting.countCacheUse = 0;

    long start = System.currentTimeMillis();

    try (NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml2), filename, null)) {
      System.out.printf("%nTestNcmlAggExisting.open %s%n", filename);
      Variable time = ncfile.findVariable("time");
      System.out.printf(" Variable %s%n", time.getNameAndDimensions());
      time.read();
    }
    System.out.printf(" countCacheUse = %d%n", AggregationExisting.countCacheUse);

    long took = System.currentTimeMillis() - start;
    System.out.printf(" first took %d msecs%n", took);

    AggregationExisting.countCacheUse = 0;
    start = System.currentTimeMillis();

    try (NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml2), filename, null)) {
      System.out.printf("%nTestNcmlAggExisting.open %s%n", filename);
      Variable time = ncfile.findVariable("time");
      System.out.printf(" Variable %s%n", time.getNameAndDimensions());
      time.read();
    }
    System.out.printf(" countCacheUse = %d%n", AggregationExisting.countCacheUse);
    took = System.currentTimeMillis() - start;
    System.out.printf(" second took %d msecs%n", took);
  }


}

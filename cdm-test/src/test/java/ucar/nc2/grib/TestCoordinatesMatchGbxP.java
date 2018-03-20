/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 *  Test reading grib coordinates match gbx, replace TestGrib1CoordsMatch

 *
 * @author caron
 * @since 7/7/2016.
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestCoordinatesMatchGbxP {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final boolean showFileCounters = true;

  @BeforeClass
  static public void before() {
    Grib.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    countersAll = GribCoordsMatchGbx.getCounters();
  }

  @AfterClass
  static public void after() {
    Grib.setDebugFlags(new DebugFlagsImpl());
    System.out.printf("countersAll = %s%n", countersAll);
  }

  static ucar.nc2.util.Counters countersAll;

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    // GRIB1
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/20141024/GFS_CONUS_80km_20141024_1200.grib1.ncx4"}); // GC SRC
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/20141024/gfsConus80_dir-20141024.ncx4"}); // PofG MRC
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/gfsConus80_dir.ncx4"}); // PofP TwoD / Best
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2/PofP/ds083.2-pofp.ncx4"}); // PofP MRUTP 41136

    // GRIB2
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/dgex/20141011/DGEX_CONUS_12km_20141011_0600.grib2.ncx4"}); // SRC 1009
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/dgex/20141011/dgex_46-20141011.ncx4"}); // TP TwoD/Best 2018 (3140)
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/dgex/dgex_46.ncx4"}); // PofP TwoD/Best 4036 (5384)
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/cfsr/cfrsAnalysis_46.ncx4"}); // CFSR single file MRC 868
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/GFS_Global_2p5deg_20150301_0000.grib2.ncx4"}); // GC SRC 33994
    result.add(new Object[]{TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4"}); // PofG took 45 minutes TwoD/Best 130953 (172166) */

    return result;
  }

  String endpoint;

  public TestCoordinatesMatchGbxP(String endpoint) {
    this.endpoint = endpoint;
  }

  @Test
  public void doOneFile() throws IOException {
    System.out.printf("%s%n", endpoint);
    ucar.nc2.util.Counters fileCounters = GribCoordsMatchGbx.getCounters();
    GribCoordsMatchGbx helper = new GribCoordsMatchGbx(endpoint, fileCounters);
    int fail = helper.readGridDataset();
    fail += helper.readCoverageDataset();
    if (showFileCounters) System.out.printf("fileCounters= %s%n", fileCounters);
    countersAll.addTo(fileCounters);

    Assert.assertEquals(0, fail);
  }


}

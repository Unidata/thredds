/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

package ucar.nc2.grib;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Variable;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Test reading grib coordinates match gbx
 */
@Category(NeedsCdmUnitTest.class)
public class TestCoordinatesMatchGbx {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final boolean showFileCounters = false;

  @BeforeClass
  static public void before() {
    Grib.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    countersAll = GribCoordsMatchGbx.getCounters();

    // cant allow caching to interfere with "get last record read from GribCollectionImmutable"
    Variable.permitCaching = false;
  }

  @AfterClass
  static public void after() {
    Grib.setDebugFlags(new DebugFlagsImpl());
    logger.debug("countersAll = {}", countersAll);
    Variable.permitCaching = true;
  }

  static ucar.nc2.util.Counters countersAll;
  ucar.nc2.util.Counters counterCurrent;

  @Test
  public void readGrib1Files() throws Exception {
    counterCurrent = countersAll.makeSubCounters();
    int fail = readAllDir(TestDir.cdmUnitTestDir + "formats/grib1", null, false);
    logger.debug("readGrib1Files = {}", counterCurrent);
    countersAll.addTo(counterCurrent);
    Assert.assertEquals(0, fail);
  }

  @Test
  public void readGrib2Files() throws Exception {
    counterCurrent = countersAll.makeSubCounters();
    int fail = readAllDir(TestDir.cdmUnitTestDir + "formats/grib2", null, false);
    logger.debug("readGrib2Files = {}", counterCurrent);
    countersAll.addTo(counterCurrent);
    Assert.assertEquals(0, fail);
  }

  @Test
  public void readNcepFiles() throws Exception {
    counterCurrent = countersAll.makeSubCounters();
    int fail = readAllDir(TestDir.cdmUnitTestDir + "tds/ncep", null, true);
    logger.debug("readNcepFiles = {}", counterCurrent);
    countersAll.addTo(counterCurrent);
    Assert.assertEquals(0, fail);
  }

  @Test
  public void readFnmocFiles() throws Exception {
    counterCurrent = countersAll.makeSubCounters();
    int fail = readAllDir(TestDir.cdmUnitTestDir + "tds/fnmoc", null, true);
    logger.debug("readFnmocFiles = {}", counterCurrent);
    countersAll.addTo(counterCurrent);
    Assert.assertEquals(0, fail);
  }

  /*
   time intervals =
   discontiguous values (240)=
   (0.000000,1.000000) (0.000000,6.000000) (6.000000,12.000000) (12.000000,18.000000) (18.000000,24.000000) (1.000000,2.000000)
   (1.000000,7.000000) (7.000000,13.000000) (13.000000,19.000000) (19.000000,25.000000) (2.000000,3.000000) (2.000000,8.000000)
   (8.000000,14.000000) (14.000000,20.000000) (20.000000,26.000000) (3.000000,4.000000) (3.000000,9.000000) (9.000000,15.000000)
   (15.000000,21.000000) (21.000000,27.000000) (4.000000,5.000000) (4.000000,10.000000) (10.000000,16.000000) (16.000000,22.000000)
   (22.000000,28.000000) (5.000000,6.000000) (5.000000,11.000000) (11.000000,17.000000) (17.000000,23.000000) (23.000000,29.000000)
   (6.000000,7.000000) (6.000000,12.000000) (12.000000,18.000000) (18.000000,24.000000) (24.000000,30.000000) (7.000000,8.000000)
   (7.000000,13.000000) (13.000000,19.000000) (19.000000,25.000000) (25.000000,31.000000) (8.000000,9.000000) (8.000000,14.000000)
   (14.000000,20.000000) (20.000000,26.000000) (26.000000,32.000000) (9.000000,10.000000) (9.000000,15.000000) (15.000000,21.000000)
   (21.000000,27.000000) (27.000000,33.000000) (10.000000,11.000000) (10.000000,16.000000) (16.000000,22.000000) (22.000000,28.000000)
   (28.000000,34.000000) (11.000000,12.000000) (11.000000,17.000000) (17.000000,23.000000) (23.000000,29.000000) (29.000000,35.000000)
   (12.000000,13.000000) (12.000000,18.000000) (18.000000,24.000000) (24.000000,30.000000) (30.000000,36.000000) (13.000000,14.000000)
   (13.000000,19.000000) (19.000000,25.000000) (25.000000,31.000000) (31.000000,37.000000) (14.000000,15.000000) (14.000000,20.000000) (
   (20.000000,26.000000) (26.000000,32.000000) (32.000000,38.000000) (15.000000,16.000000) (15.000000,21.000000) (21.000000,27.000000)
   (27.000000,33.000000) (33.000000,39.000000) (16.000000,17.000000) (16.000000,22.000000) (22.000000,28.000000) (28.000000,34.000000)
   (34.000000,40.000000) (17.000000,18.000000) (17.000000,23.000000) (23.000000,29.000000) (29.000000,35.000000) (35.000000,41.000000)
   (18.000000,19.000000) (18.000000,24.000000) (24.000000,30.000000) (30.000000,36.000000) (36.000000,42.000000) (19.000000,20.000000)
   (19.000000,25.000000) (25.000000,31.000000) (31.000000,37.000000) (37.000000,43.000000) (20.000000,21.000000) (20.000000,26.000000)
   (26.000000,32.000000) (32.000000,38.000000) (38.000000,44.000000) (21.000000,22.000000) (21.000000,27.000000) (27.000000,33.000000)
   (33.000000,39.000000) (39.000000,45.000000) (22.000000,23.000000) (22.000000,28.000000) (28.000000,34.000000) (34.000000,40.000000)
   (40.000000,46.000000) (23.000000,24.000000) (23.000000,29.000000) (29.000000,35.000000) (35.000000,41.000000) (41.000000,47.000000)
    (24, 30) repeated twice.
    Problem is the actual coords are unique (so made into a MRUTC) but we have:
      X----
      X----
      X----
      X----
      X----
      X----
      X----
      X----
      X----
      X----
      X----
      X----
      XXXXX
      X----
      X----
      X----
      X----
      X----
      X----
      X----
      X----
      X----
      X----
      X----

      should remove the missing records.
      https://github.com/Unidata/thredds/issues/584
   */
  @Ignore("Overlapping time interval")
  public void testNonUniqueTimeCoordsProblem() throws IOException {
    ucar.nc2.util.Counters counters = GribCoordsMatchGbx.getCounters();
    String filename = TestDir.cdmUnitTestDir + "formats/grib1/problem/QPE.20101005.009.157";
    GribCoordsMatchGbx helper = new GribCoordsMatchGbx(filename, counters);
    helper.readGridDataset();
    helper.readCoverageDataset();
    logger.debug("counters= {}", counters);
  }

  public void testProblem2() throws IOException {
    ucar.nc2.util.Counters counters = GribCoordsMatchGbx.getCounters();
    String filename = "D:/work/rdavm/ds084.3/2015/20150201/ds084.3-20150201.ncx4";
    GribCoordsMatchGbx helper = new GribCoordsMatchGbx(filename, counters);
    // helper.readGridDataset();
    helper.readCoverageDataset();
    logger.debug("counters= {}", counters);
  }

  @Test
  public void testRdaPofP() throws IOException {
    ucar.nc2.util.Counters counters = GribCoordsMatchGbx.getCounters();
    String filename = TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2/PofP/ds083.2-pofp.ncx4";
    GribCoordsMatchGbx helper = new GribCoordsMatchGbx(filename, counters);
    helper.readGridDataset();
    helper.readCoverageDataset();
    logger.debug("counters= {}", counters);
  }

  int readAllDir(String dirName, String suffix, boolean recurse) throws Exception {
    return TestDir.actOnAll(dirName, new GribFilter(), new GribAct(), recurse);
  }

  class GribFilter implements FileFilter {

    @Override
    public boolean accept(File file) {
      if (file.isDirectory()) return false;
      String name = file.getName();
      if (name.contains(".gbx")) return false;
      if (name.contains(".ncx")) return false;
      if (name.contains(".ncml")) return false;
      return true;
    }
  }

  class GribAct implements TestDir.Act {

    private final List<String> skipFileGrib1 = Arrays.asList("QPE.20101005.009.157",
            "testproj.grb", "testproj2.grb", "wrf_d03_201308080200.grb", "test.nc");

    private final List<String> skipFileGrib2 = Arrays.asList("ds.pop12.bin",
            "shtfl.grib2", "shtfl.grib2.ncx4", "ds.mint.nc");

    @Override
    public int doAct(String filename) throws IOException {
      Path filePath = Paths.get(filename);
      String fileName = filePath.getFileName().toString();
      int fail =0;
      int fail2 = 0;

      if (!(skipFileGrib1.contains(fileName) || skipFileGrib2.contains(fileName))) {
        ucar.nc2.util.Counters fileCounters = counterCurrent.makeSubCounters();
        GribCoordsMatchGbx helper = new GribCoordsMatchGbx(filename, fileCounters);
        fail = helper.readGridDataset();
        fail2 = helper.readCoverageDataset();
        if (showFileCounters) logger.debug("fileCounters= {}", fileCounters);
        counterCurrent.addTo(fileCounters);
      } else {
        logger.warn("Skipping file {}", filename);
      }

      return fail + fail2;
    }

  }
}

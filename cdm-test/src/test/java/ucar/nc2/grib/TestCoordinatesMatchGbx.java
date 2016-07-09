/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */
package ucar.nc2.grib;

import org.junit.*;
import org.junit.experimental.categories.Category;
import ucar.nc2.Variable;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

/**
 * Test reading grib coordinates match gbx
 */
@Category(NeedsCdmUnitTest.class)
public class TestCoordinatesMatchGbx {
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
    System.out.printf("countersAll = %s%n", countersAll);
    Variable.permitCaching = true;
  }

  static ucar.nc2.util.Counters countersAll;
  ucar.nc2.util.Counters counterCurrent;

  @Test
  public void readGrib1Files() throws Exception {
    counterCurrent = countersAll.makeSubCounters();
    int fail = readAllDir(TestDir.cdmUnitTestDir + "formats/grib1", null, false);
    System.out.printf("readGrib1Files = %s%n", counterCurrent);
    countersAll.addTo(counterCurrent);
    Assert.assertEquals(0, fail);
  }

  @Test
  public void readGrib2Files() throws Exception {
    counterCurrent = countersAll.makeSubCounters();
    int fail = readAllDir(TestDir.cdmUnitTestDir + "formats/grib2", null, false);
    System.out.printf("readGrib2Files = %s%n", counterCurrent);
    countersAll.addTo(counterCurrent);
    Assert.assertEquals(0, fail);
  }

  @Test
  public void readNcepFiles() throws Exception {
    counterCurrent = countersAll.makeSubCounters();
    int fail = readAllDir(TestDir.cdmUnitTestDir + "tds/ncep", null, true);
    System.out.printf("readNcepFiles = %s%n", counterCurrent);
    countersAll.addTo(counterCurrent);
    Assert.assertEquals(0, fail);
  }

  @Test
  public void readFnmocFiles() throws Exception {
    counterCurrent = countersAll.makeSubCounters();
    int fail = readAllDir(TestDir.cdmUnitTestDir + "tds/fnmoc", null, true);
    System.out.printf("readFnmocFiles = %s%n", counterCurrent);
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
    System.out.printf("counters= %s%n", counters);
  }

  public void testProblem2() throws IOException {
    ucar.nc2.util.Counters counters = GribCoordsMatchGbx.getCounters();
    String filename = "D:/work/rdavm/ds084.3/2015/20150201/ds084.3-20150201.ncx4";
    GribCoordsMatchGbx helper = new GribCoordsMatchGbx(filename, counters);
    // helper.readGridDataset();
    helper.readCoverageDataset();
    System.out.printf("counters= %s%n", counters);
  }

  @Test
  public void testRdaPofP() throws IOException {
    ucar.nc2.util.Counters counters = GribCoordsMatchGbx.getCounters();
    String filename = TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds083.2/PofP/ds083.2-pofp.ncx4";
    GribCoordsMatchGbx helper = new GribCoordsMatchGbx(filename, counters);
    helper.readGridDataset();
    helper.readCoverageDataset();
    System.out.printf("counters= %s%n", counters);
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

    @Override
    public int doAct(String filename) throws IOException {
      int fail =0;
      int fail2 = 0;
      ucar.nc2.util.Counters fileCounters = counterCurrent.makeSubCounters();
      GribCoordsMatchGbx helper = new GribCoordsMatchGbx(filename, fileCounters);
      fail = helper.readGridDataset();
      fail2 = helper.readCoverageDataset();
      if (showFileCounters) System.out.printf("fileCounters= %s%n", fileCounters);
      counterCurrent.addTo(fileCounters);
      return fail + fail2;
    }

  }
}

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
package ucar.nc2.ft.coverage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.grib.GribCoordsMatchGbx;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.util.test.category.NeedsRdaData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Read one file from each directory
 *
 * @author caron
 * @since 6/30/2016.
 */
@RunWith(Parameterized.class)
@Category(NeedsRdaData.class)
public class TestRdaCoordsMatchGbx {
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

  static String topdir = "D:/work/rdavm/"; // LOOK how can we handle this better?
  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    result.add( new Object[] {topdir+"ds628.0/fcst_surf/2012/ds628.0_fcst_surf-2012.ncx4"});
    result.add( new Object[] {topdir+"ds084.3/2015/20150201/ds084.3-20150201.ncx4"});
    result.add( new Object[] {topdir+"ds084.4/2016/201602/ds084.4-201602.ncx4"});
    result.add( new Object[] {topdir+"ds094.1/2013/ds094.1_Test2013.ncx4"});
    result.add( new Object[] {topdir+"ds094.2/timeseries/ds094.2_t.ncx4"});
    result.add( new Object[] {topdir+"ds277.6/monthly/ds277.6.ncx4"});
    result.add( new Object[] {topdir+"ds628.0/fcst_surf/2012/ds628.0_fcst_surf_local.ncx4"});
    result.add( new Object[] {topdir+"ds628.0/ll125/ds628.0_ll125.ncx4"});
    result.add( new Object[] {topdir+"ds628.2/fcst_column125/1986/ds628.2-1986.ncx4"});
    result.add( new Object[] {topdir+"ds628.5/fcst_surf125_var_diurnal/1972/ds628.5.MRUTC-1972.ncx4"});
    return result;
  }

  String endpoint;

  public TestRdaCoordsMatchGbx(String endpoint) {
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

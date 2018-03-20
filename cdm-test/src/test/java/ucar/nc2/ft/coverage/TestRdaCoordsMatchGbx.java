/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.coverage;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grib.GribCoordsMatchGbx;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.util.test.category.NeedsRdaData;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
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

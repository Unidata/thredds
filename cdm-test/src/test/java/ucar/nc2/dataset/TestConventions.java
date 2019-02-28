/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import ucar.ma2.DataType;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.HorizCoordSys;
import ucar.nc2.time.Calendar;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.nc2.util.Optional;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * Test specific files for CoordSys Conventions
 *
 * @author caron
 */
@Category(NeedsCdmUnitTest.class)
public class TestConventions  {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testCF() throws IOException {
    try (GridDataset ds = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/cf/twoGridMaps.nc")) {
      GeoGrid grid = ds.findGridByName("altitude");
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert 1 == gcs.getCoordinateTransforms().size();
      CoordinateTransform ct = gcs.getCoordinateTransforms().get(0);
      assert ct.getTransformType() == TransformType.Projection;
      assert ct.getName().equals("projection_stere");
    }
  }

  @Test
  public void testCOARDSdefaultCalendar() throws IOException {
    try (GridDataset ds = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/coards/olr.day.mean.nc")) {
      GeoGrid grid = ds.findGridByName("olr");
      assert grid != null;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      CoordinateAxis1DTime time = gcs.getTimeAxis1D();
      assert time != null;

      CalendarDate first = time.getCalendarDate(0);
      CalendarDate cd = CalendarDateFormatter.isoStringToCalendarDate(Calendar.gregorian, "2002-01-01T00:00:00Z");
      assert first.equals(cd) : first + " != " + cd;
      CalendarDate last = time.getCalendarDate((int) time.getSize() - 1);
      CalendarDate cd2 = CalendarDateFormatter.isoStringToCalendarDate(Calendar.gregorian, "2012-12-02T00:00:00Z");
      assert last.equals(cd2) : last + " != " + cd2;
    }
  }

  @Test
  public void testAWIPSsatLatlon() throws IOException {
    try (GridDataset ds = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/awips/20150602_0830_sport_imerg_noHemis_rr.nc")) {
      GeoGrid grid = ds.findGridByName("image");
      assert grid != null;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert gcs.isLatLon();
      Assert.assertEquals(DataType.BYTE, grid.getDataType());
    }
  }

  @Test
  public void testIfps() throws IOException {
    Optional<FeatureDatasetCoverage> ds =
            CoverageDatasetFactory.openCoverageDataset(TestDir.cdmUnitTestDir + "conventions/ifps/HUNGrids.netcdf");
    assert ds.isPresent();
    CoverageCollection cc = ds.get().getSingleCoverageCollection();
    assert cc != null;
    Coverage coverage = cc.findCoverage("T_SFC");
    assert coverage != null;
    CoverageCoordSys cs = coverage.getCoordSys();
    HorizCoordSys hcs = cs.getHorizCoordSys();
    assert hcs.isProjection();
    Assert.assertEquals(DataType.FLOAT, coverage.getDataType());
  }

}

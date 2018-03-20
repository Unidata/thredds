/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test specific projections
 *
 * @author caron
 * @since 8/29/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestProjectionCoordinates {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static String testDir= TestDir.cdmUnitTestDir + "transforms/";

  @Test
  public void testPSscaleFactor() throws IOException, InvalidRangeException {
    testCoordinates(testDir + "stereographic/foster.grib2", 26.023346, 251.023136 - 360.0, 41.527360, 270.784605 - 360.0);
  }

  @Test
  public void testMercatorScaleFactor() throws IOException, InvalidRangeException {
    testCoordinates(testDir + "melb-small_M-1SP.nc", -36.940012, 145.845218, -36.918406, 145.909746);
  }


  @Test
  public void testRotatedPole() throws IOException, InvalidRangeException {
    testCoordinates(testDir + "rotatedPole/snow.DMI.ecctrl.v5.ncml", 28.690059, -3.831161, 68.988028, 57.076276);
  }

  @Test
  public void testRotatedPole2() throws IOException, InvalidRangeException {
    testCoordinates(testDir+ "rotatedPole/DMI-HIRHAM5_ERAIN_DM_AMMA-50km_1989-1990_as.nc", -19.8, -35.64, 35.2, 35.2);
  }

  private void testCoordinates(String filename, double startLat, double startLon, double endLat, double endLon) throws IOException, InvalidRangeException {
    System.out.printf("%n***Open %s%n", filename);
    NetcdfDataset ncd = NetcdfDataset.openDataset(filename);
    GridDataset gds = new GridDataset(ncd);
    GridCoordSystem gsys = null;
    ProjectionImpl p = null;

    for (ucar.nc2.dt.GridDataset.Gridset g : gds.getGridsets()) {
      gsys = g.getGeoCoordSystem();
      for (CoordinateTransform t : gsys.getCoordinateTransforms()) {
        if (t instanceof ProjectionCT) {
          p = ((ProjectionCT)t).getProjection();
          break;
        }
      }
    }
    assert p != null;

    CoordinateAxis1D xaxis = (CoordinateAxis1D) gsys.getXHorizAxis();
    CoordinateAxis1D yaxis =  (CoordinateAxis1D) gsys.getYHorizAxis();
    p.projToLatLon(xaxis.getCoordValue(0), yaxis.getCoordValue(0)  );
    LatLonPoint start1 =  p.projToLatLon(xaxis.getCoordValue(0), yaxis.getCoordValue(0));
    LatLonPoint start2 =  p.projToLatLon(xaxis.getCoordValue((int)xaxis.getSize()-1), yaxis.getCoordValue((int)yaxis.getSize()-1));
    System.out.printf( "start = %f %f%n", start1.getLatitude(), start1.getLongitude());
    System.out.printf( "end = %f %f%n", start2.getLatitude(), start2.getLongitude());

    assert start1.nearlyEquals(new LatLonPointImpl(startLat, startLon), 2.0E-4);
    assert start2.nearlyEquals(new LatLonPointImpl(endLat, endLon), 2.0E-4);

    ncd.close();
  }

}

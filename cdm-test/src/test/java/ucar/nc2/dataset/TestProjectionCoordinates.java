/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.dataset;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Test specific projections
 *
 * @author caron
 * @since 8/29/2014
 */
@Category(NeedsCdmUnitTest.class)
public class TestProjectionCoordinates {

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
    testCoordinates(testDir + "rotatedPole/snow.DMI.ecctrl.ncml", 28.690059, -3.831161, 68.988028, 57.076276);
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

    assert Misc.closeEnough(start1.getLatitude(), startLat) : Misc.howClose(start1.getLatitude(), startLat);
    assert Misc.closeEnough(start1.getLongitude(), startLon) : Misc.howClose(start1.getLongitude(), startLon);

    assert Misc.closeEnough(start2.getLatitude(), endLat,  2.0E-4) :  Misc.howClose(start2.getLatitude(), endLat);
    assert Misc.closeEnough(start2.getLongitude(),endLon, 2.0E-4) : Misc.howClose(start2.getLongitude(), endLon);

    ncd.close();
  }

}

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
 */

package ucar.nc2.ft.coverage;

import java.io.IOException;
import java.util.Formatter;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.ft2.coverage.adapter.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

/**
 * Description
 *
 * @author John
 * @since 8/17/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCoverageBuilding {

  @Test
  public void testScalarRuntimeCoordinate() throws IOException {

    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1.ncx4";
    String gridName = "Pressure_surface";

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection cd = cc.getCoverageCollections().get(0);
      Coverage cov = cd.findCoverage(gridName);
      Assert.assertNotNull(gridName, cov);
      CoverageCoordSys csys = cov.getCoordSys();
      Assert.assertNotNull("CoverageCoordSys", csys);

      CoverageCoordAxis1D runtime = (CoverageCoordAxis1D) csys.getAxis(AxisType.RunTime);
      Assert.assertNotNull(AxisType.RunTime.toString(), runtime);
      Assert.assertTrue(runtime.getClass().getName(), runtime instanceof CoverageCoordAxis1D);
      Assert.assertEquals(CoverageCoordAxis.Spacing.regularPoint, runtime.getSpacing());
      Assert.assertEquals(CoverageCoordAxis.DependenceType.scalar, runtime.getDependenceType());
      CalendarDate startDate = runtime.makeDate(runtime.getCoordMidpoint(0));
      Assert.assertEquals(CalendarDate.parseISOformat(null, "2012-02-27T00:00:00Z"), startDate);
    }
  }

  @Test
    public void test2DTimeCoordinates() throws IOException {
      String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km.ncx4";
      String gridName = "Pressure_surface";

      try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
        Assert.assertNotNull(filename, cc);
        Assert.assertEquals(2, cc.getCoverageCollections().size());
        CoverageCollection cd = cc.findCoverageDataset(FeatureType.FMRC);
        Assert.assertNotNull(FeatureType.FMRC.toString(), cd);

        Coverage cov = cd.findCoverage(gridName);
        Assert.assertNotNull(gridName, cov);
        CoverageCoordSys csys = cov.getCoordSys();
        Assert.assertNotNull("CoverageCoordSys", csys);

        CoverageCoordAxis runtime = csys.getAxis(AxisType.RunTime);
        Assert.assertNotNull(AxisType.RunTime.toString(), runtime);
        Assert.assertTrue(runtime.getClass().getName(), runtime instanceof CoverageCoordAxis1D);
        Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, runtime.getSpacing());
        Assert.assertEquals(CoverageCoordAxis.DependenceType.independent, runtime.getDependenceType());
        Assert.assertEquals(CalendarDate.parseISOformat(null, "2012-02-27T00:00:00Z"), runtime.makeDate(0));
        Assert.assertEquals(6.0, runtime.getResolution(), Misc.maxReletiveError);

        CoverageCoordAxis time = csys.getAxis(AxisType.TimeOffset);
        Assert.assertNotNull(AxisType.TimeOffset.toString(), time);
        Assert.assertTrue(time.getClass().getName(), time instanceof TimeOffsetAxis);
        Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, time.getSpacing());
        Assert.assertEquals(CoverageCoordAxis.DependenceType.independent, time.getDependenceType());
        Assert.assertEquals(CalendarDate.parseISOformat(null, "2012-02-27T00:00:00Z"), time.makeDate(0));
        Assert.assertEquals(6.0, time.getResolution(), Misc.maxReletiveError);
        Assert.assertEquals(true, csys.isTime2D(time));
      }
    }

  @Test
  public void testBestTimeCoordinates() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km.ncx4";
    String gridName = "Pressure_surface";

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      Assert.assertEquals(2, cc.getCoverageCollections().size());
      CoverageCollection cd = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull(FeatureType.GRID.toString(), cd);

      Coverage cov = cd.findCoverage(gridName);
      Assert.assertNotNull(gridName, cov);
      CoverageCoordSys csys = cov.getCoordSys();
      Assert.assertNotNull("CoverageCoordSys", csys);

        CoverageCoordAxis time = csys.getAxis(AxisType.Time);
        Assert.assertNotNull(AxisType.Time.toString(), time);
        Assert.assertTrue(time.getClass().getName(), time instanceof CoverageCoordAxis1D);
      Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, time.getSpacing());
        Assert.assertEquals(CoverageCoordAxis.DependenceType.independent, time.getDependenceType());
        Assert.assertEquals(CalendarDate.parseISOformat(null, "2012-02-27T00:00:00Z"), time.makeDate(0));
        Assert.assertEquals(6.0, time.getResolution(), Misc.maxReletiveError);
        Assert.assertEquals(false, csys.isTime2D(time));

      CoverageCoordAxis runtime = csys.getAxis(AxisType.RunTime);
      Assert.assertNotNull(AxisType.RunTime.toString(), runtime);
      Assert.assertTrue(runtime.getClass().getName(), runtime instanceof CoverageCoordAxis1D);
      Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, runtime.getSpacing());
      Assert.assertEquals(CoverageCoordAxis.DependenceType.dependent, runtime.getDependenceType());
      Assert.assertEquals(CalendarDate.parseISOformat(null, "2012-02-27T00:00:00Z"), runtime.makeDate(0));
      }
    }

  @Test
  public void testTimeOffsetSubsetWhenTimePresent() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1";
    String gridName = "Temperature_isobaric";
    System.out.printf("file %s coverage %s%n", filename, gridName);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection cd = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull(FeatureType.GRID.toString(), cd);

      Coverage cov = cd.findCoverage(gridName);
      Assert.assertNotNull(gridName, cov);

      CoverageCoordSys csys = cov.getCoordSys();
      Assert.assertNotNull("CoverageCoordSys", csys);

      CoverageCoordAxis time = csys.getAxis(AxisType.Time);
      Assert.assertNotNull(AxisType.Time.toString(), time);
      Assert.assertTrue(time.getClass().getName(), time instanceof CoverageCoordAxis1D);
      Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, time.getSpacing());
      Assert.assertEquals(CoverageCoordAxis.DependenceType.independent, time.getDependenceType());
      Assert.assertEquals(CalendarDate.parseISOformat(null, "2012-02-27T00:00:00Z"), time.makeDate(0));
      Assert.assertEquals(6.0, time.getResolution(), Misc.maxReletiveError);

      CoverageCoordAxis runtime = csys.getAxis(AxisType.RunTime);
      Assert.assertNotNull(AxisType.RunTime.toString(), runtime);
      Assert.assertTrue(runtime.getClass().getName(), runtime instanceof CoverageCoordAxis1D);
      Assert.assertEquals(CoverageCoordAxis.Spacing.regularPoint, runtime.getSpacing());
      Assert.assertEquals(CoverageCoordAxis.DependenceType.scalar, runtime.getDependenceType());
      Assert.assertEquals(CalendarDate.parseISOformat(null, "2012-02-27T00:00:00Z"), runtime.makeDate(0));
    }
  }

  @Test
  public void testGaussianLats() throws IOException {

    String filename = TestDir.cdmUnitTestDir + "formats/grib1/cfs.wmo";
    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      String gridName =  "Albedo_surface_Average";

      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection cd = cc.getCoverageCollections().get(0);
      Coverage cov = cd.findCoverage(gridName);
      Assert.assertNotNull(gridName, cov);
      CoverageCoordSys csys = cov.getCoordSys();
      Assert.assertNotNull("CoverageCoordSys", csys);

      CoverageCoordAxis latAxis = csys.getAxis(AxisType.Lat);
      Assert.assertNotNull(AxisType.RunTime.toString(), latAxis);
      Assert.assertTrue(latAxis.getClass().getName(), latAxis instanceof CoverageCoordAxis1D);
      Assert.assertEquals(CoverageCoordAxis.Spacing.irregularPoint, latAxis.getSpacing());
      Assert.assertEquals(CoverageCoordAxis.DependenceType.independent, latAxis.getDependenceType());

      Attribute att = latAxis.findAttribute(CDM.GAUSSIAN);
      Assert.assertNotNull(att);
      Assert.assertEquals("true", att.getStringValue());
    }
  }

}

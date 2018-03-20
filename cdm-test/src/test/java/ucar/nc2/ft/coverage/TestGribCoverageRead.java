/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test Grib Collection reading
 *
 * @author caron
 * @since 2/9/2016.
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCoverageRead {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  CalendarDate useDate = CalendarDate.parseISOformat(null, "2014-10-27T06:00:00Z");

  @Test
  public void TestTwoDRead() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/gfsConus80_file.ncx4";
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(2, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertEquals(FeatureType.FMRC, gds.getCoverageType());

      String covName = "Temperature_isobaric";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      long size = cover.getSizeInBytes();
      Assert.assertEquals(6*36*29*65*93*4, size);

      // LOOK if we dont set the runtime, assume latest. driven by Cdmrf spec. could be different.
      SubsetParams subset = new SubsetParams().setVertCoord(300.0).setTime(useDate);
      GeoReferencedArray geo = cover.readData(subset);
      Array data = geo.getData();
      System.out.printf("%s%n", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(new int[] {1,1,1,65,93}, data.getShape());

      float first = data.getFloat(0);
      float last = data.getFloat((int)data.getSize()-1);
      System.out.printf("data first = %f last=%f%n", first, last);
      Assert.assertTrue(Misc.nearlyEquals(241.699997, first));
      Assert.assertTrue(Misc.nearlyEquals(225.099991, last));
    }
  }

  @Test
  public void TestBestRead() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/gfsConus80_file.ncx4";
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(2, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(1);
      Assert.assertEquals(FeatureType.GRID, gds.getCoverageType());

      String covName = "Temperature_isobaric";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      long size = cover.getSizeInBytes();
      Assert.assertEquals(41*29*65*93*4, size);

      SubsetParams subset = new SubsetParams().setVertCoord(300.0).setTime(useDate);
      GeoReferencedArray geo = cover.readData(subset);
      Array data = geo.getData();
      System.out.printf("%s%n", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(new int[] {1,1,65,93}, data.getShape());

      float first = data.getFloat(0);
      float last = data.getFloat((int)data.getSize()-1);
      System.out.printf("data first = %f last=%f%n", first, last);
      Assert.assertTrue(Misc.nearlyEquals(241.699997, first));
      Assert.assertTrue(Misc.nearlyEquals(225.099991, last));
    }
  }

  @Test
  public void TestSRCRead() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/20141025/GFS_CONUS_80km_20141025_0000.grib1.ncx4";
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertEquals(FeatureType.GRID, gds.getCoverageType());

      String covName = "Temperature_isobaric";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      long size = cover.getSizeInBytes();
      Assert.assertEquals(25243920, size);

      SubsetParams subset = new SubsetParams().setVertCoord(200.0).setTimeOffset(42.0);
      GeoReferencedArray geo = cover.readData(subset);
      Array data = geo.getData();
      System.out.printf("%s%n", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(new int[] {1,1,65,93}, data.getShape());

      float first = data.getFloat(0);
      float last = data.getFloat((int)data.getSize()-1);
      System.out.printf("data first = %f last=%f%n", first, last);
      Assert.assertTrue(Misc.nearlyEquals(219.5f, first));
      Assert.assertTrue(Misc.nearlyEquals(218.6f, last));
    }
  }

  @Test
  public void TestMRUTCRead() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/anal/HRRRanalysis.ncx4";
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertEquals(FeatureType.GRID, gds.getCoverageType());

      String covName = "Temperature_isobaric";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      long size = cover.getSizeInBytes();
      Assert.assertEquals(4*5*1377*2145*4, size);

      SubsetParams subset = new SubsetParams().setVertCoord(70000).setTimeOffset(2);
      GeoReferencedArray geo = cover.readData(subset);
      Array data = geo.getData();
      System.out.printf("%s%n", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(new int[]{1, 1, 1377, 2145}, data.getShape());

      float val = data.getFloat(40600);
      System.out.printf("data val at %d = %f%n", 40600, val);
      Assert.assertTrue(Misc.nearlyEquals(281.627563, val));

      val = data.getFloat(55583);
      System.out.printf("data val at %d = %f%n", 55583, val);
      Assert.assertTrue(Misc.nearlyEquals(281.690063, val));

    }
  }

  @Test
  public void TestMRUTPRead() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx4";
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertEquals(FeatureType.GRID, gds.getCoverageType());

      String covName = "Relative_humidity_sigma";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      long size = cover.getSizeInBytes();
      Assert.assertEquals(2*181*360*4, size);

      SubsetParams subset = new SubsetParams().setTimeOffset(6);
      GeoReferencedArray geo = cover.readData(subset);
      Array data = geo.getData();
      System.out.printf("%s%n", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(new int[]{1, 1, 181, 360}, data.getShape());

      float val = data.getFloat(3179);
      System.out.printf("data val at %d = %f%n", 3179, val);
      Assert.assertTrue(Misc.nearlyEquals(98.0, val));

      val = data.getFloat(5020);
      System.out.printf("data val at %d = %f%n", 5020, val);
      Assert.assertTrue(Misc.nearlyEquals(60.0, val));

    }
  }

  @Test
  public void TestPofPRead() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/gfsConus80_file.ncx4";
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(2, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertEquals(FeatureType.FMRC, gds.getCoverageType());

      String covName = "Vertical_velocity_pressure_isobaric";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      long size = cover.getSizeInBytes();
      Assert.assertEquals(6*35*9*65*93*4, size);

      SubsetParams subset = new SubsetParams().setRunTime(CalendarDate.parseISOformat(null,"2014-10-24T12:00:00Z"))
              .setTimeOffset(42).setVertCoord(500);
      GeoReferencedArray geo = cover.readData(subset);
      Array data = geo.getData();
      System.out.printf("%s%n", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(new int[]{1, 1, 1, 65, 93}, data.getShape());

      float val = data.getFloat(0);
      System.out.printf("data val first = %f%n", val);
      Assert.assertTrue(Misc.nearlyEquals(-0.10470009f, val));

      val = data.getFloat( (int)data.getSize()-1);
      System.out.printf("data val last = %f%n", val);
      Assert.assertTrue(Misc.nearlyEquals(0.18079996f, val));
    }
  }


}

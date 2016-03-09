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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Misc;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;

/**
 * Test Grib Collection reading
 *
 * @author caron
 * @since 2/9/2016.
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCoverageRead {
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
      SubsetParams subset = new SubsetParams().set(SubsetParams.vertCoord, 300.0).setTime(useDate);
      GeoReferencedArray geo = cover.readData(subset);
      Array data = geo.getData();
      System.out.printf("%s%n", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(new int[] {1,1,1,65,93}, data.getShape());

      float first = data.getFloat(0);
      float last = data.getFloat((int)data.getSize()-1);
      System.out.printf("data first = %f last=%f%n", first, last);
      Assert.assertEquals(241.699997, first, first*Misc.maxReletiveError);
      Assert.assertEquals(225.099991, last, last*Misc.maxReletiveError);
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

      SubsetParams subset = new SubsetParams().set(SubsetParams.vertCoord, 300.0).setTime(useDate);
      GeoReferencedArray geo = cover.readData(subset);
      Array data = geo.getData();
      System.out.printf("%s%n", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(new int[] {1,1,65,93}, data.getShape());

      float first = data.getFloat(0);
      float last = data.getFloat((int)data.getSize()-1);
      System.out.printf("data first = %f last=%f%n", first, last);
      Assert.assertEquals(241.699997, first, first*Misc.maxReletiveError);
      Assert.assertEquals(225.099991, last, last*Misc.maxReletiveError);
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

      SubsetParams subset = new SubsetParams().set(SubsetParams.vertCoord, 200.0).set(SubsetParams.timeOffset, 42.0);
      GeoReferencedArray geo = cover.readData(subset);
      Array data = geo.getData();
      System.out.printf("%s%n", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(new int[] {1,1,65,93}, data.getShape());

      float first = data.getFloat(0);
      float last = data.getFloat((int)data.getSize()-1);
      System.out.printf("data first = %f last=%f%n", first, last);
      Assert.assertEquals(219.5, first, first * Misc.maxReletiveError);
      Assert.assertEquals(218.6, last, last*Misc.maxReletiveError);
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

      SubsetParams subset = new SubsetParams().set(SubsetParams.vertCoord, 70000).set(SubsetParams.timeOffset, 2);
      GeoReferencedArray geo = cover.readData(subset);
      Array data = geo.getData();
      System.out.printf("%s%n", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(new int[]{1, 1, 1377, 2145}, data.getShape());

      float val = data.getFloat(40600);
      System.out.printf("data val at %d = %f%n", 40600, val);
      Assert.assertEquals(281.627563, val, val * Misc.maxReletiveError);

      val = data.getFloat(55583);
      System.out.printf("data val at %d = %f%n", 55583, val);
      Assert.assertEquals(281.690063, val, val*Misc.maxReletiveError);

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
      Assert.assertEquals(98.0, val, val * Misc.maxReletiveError);

      val = data.getFloat(5020);
      System.out.printf("data val at %d = %f%n", 5020, val);
      Assert.assertEquals(60.0, val, val * Misc.maxReletiveError);

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
      Assert.assertEquals(-0.10470009, val, Misc.maxReletiveError);

      val = data.getFloat( (int)data.getSize()-1);
      System.out.printf("data val last = %f%n", val);
      Assert.assertEquals(0.18079996, val, Misc.maxReletiveError);
    }
  }


}

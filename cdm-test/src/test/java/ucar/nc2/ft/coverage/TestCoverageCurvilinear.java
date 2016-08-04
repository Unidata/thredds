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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NCdumpW;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.GeoReferencedArray;
import ucar.nc2.ft2.coverage.HorizCoordSys;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

/**
 * Description
 *
 * @author John
 * @since 8/24/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestCoverageCurvilinear {

  @Test
  public void TestGribCurvilinear() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";  // GRIB Curvilinear
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.CURVILINEAR, gds.getCoverageType());
      Assert.assertEquals(7, gds.getCoverageCount());

      HorizCoordSys hcs = gds.getHorizCoordSys();
      Assert.assertNotNull(endpoint, hcs);
      Assert.assertTrue(endpoint, !hcs.getIsProjection());
      Assert.assertNull(endpoint, hcs.getTransform());

      String covName = "Mixed_layer_depth_surface";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      GeoReferencedArray geo = cover.readData(new SubsetParams());
      TestCoverageSubsetTime.testGeoArray(geo, null, null, null);
    }
  }

  @Test
  // @Ignore("takes too long - problem HorizCoordSys2D.computeBoundsExhaustive")
  public void TestGribCurvilinearSubset() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";  // GRIB Curvilinear
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.CURVILINEAR, gds.getCoverageType());
      Assert.assertEquals(7, gds.getCoverageCount());

      HorizCoordSys hcs = gds.getHorizCoordSys();
      Assert.assertNotNull(endpoint, hcs);
      Assert.assertTrue(endpoint, !hcs.getIsProjection());
      Assert.assertNull(endpoint, hcs.getTransform());

      String covName = "Mixed_layer_depth_surface";
      Coverage coverage = gds.findCoverage(covName);
      Assert.assertNotNull(covName, coverage);

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(64.0, -61.), new LatLonPointImpl(59.0, -52.));

      SubsetParams params = new SubsetParams().set(SubsetParams.timePresent, true).set(SubsetParams.latlonBB, bbox);
      GeoReferencedArray geo = coverage.readData(params);
      System.out.printf("csys shape=%s%n", Misc.showInts(geo.getCoordSysForData().getShape()));

      Array data = geo.getData();
      System.out.printf("data shape=%s%n", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(geo.getCoordSysForData().getShape(), data.getShape());

      int[] expectedShape = new int[] {1,166,160};
      Assert.assertArrayEquals(expectedShape, data.getShape());
      //NCdumpW.printArray(data);
    }
  }

  @Test
  public void TestNetcdfCurvilinear() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc";  // NetCDF has 2D and 1D
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.CURVILINEAR, gds.getCoverageType());
      Assert.assertEquals(20, gds.getCoverageCount());

      String covName = "u";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams().setVertCoord(-.05).set(SubsetParams.timePresent, true);
      GeoReferencedArray geo = cover.readData(params);

      Array data = geo.getData();
      Index ima = data.getIndex();
      int[] expectedShape = new int[] {1,1,22,12};
      Assert.assertArrayEquals(expectedShape, data.getShape());
      // NCdumpW.printArray(data);
      Assert.assertEquals(0.0036624447, data.getDouble(ima.set(0, 0, 0, 0)), Misc.maxReletiveError);
      Assert.assertEquals(0.20564626, data.getDouble(ima.set(0, 0, 21, 11)), Misc.maxReletiveError);
    }
  }

  @Test
  public void TestNetcdfCurvilinear2D() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc";  // NetCDF Curvilinear 2D only
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.CURVILINEAR, gds.getCoverageType());
      Assert.assertEquals(10, gds.getCoverageCount());

      String covName = "hs";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      SubsetParams params = new SubsetParams().set(SubsetParams.timePresent, true);
      GeoReferencedArray geo = cover.readData(params);

      Array data = geo.getData();
      Index ima = data.getIndex();
      int[] expectedShape = new int[] {1,151,171};
      Assert.assertArrayEquals(expectedShape, data.getShape());
      // NCdumpW.printArray(data);
      Assert.assertEquals(1.782, data.getDouble(ima.set(0, 0, 0)), Misc.maxReletiveError);
      Assert.assertEquals(1.769, data.getDouble(ima.set(0, 11, 0)), Misc.maxReletiveError);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void TestNetcdfCurvilinear2Dsubset() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc";  // NetCDF Curvilinear 2D only
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.CURVILINEAR, gds.getCoverageType());
      Assert.assertEquals(10, gds.getCoverageCount());

      String covName = "hs";
      Coverage coverage = gds.findCoverage(covName);
      Assert.assertNotNull(covName, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);
      Assert.assertEquals("coordSys", 3, cs.getShape().length);
      System.out.printf("org shape=%s%n", Misc.showInts(cs.getShape()));
      int[] expectedOrgShape = new int[] {85,151,171};
      Assert.assertArrayEquals(expectedOrgShape, cs.getShape());

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(43.489, -8.5353), new LatLonPointImpl(43.371, -8.2420));

      SubsetParams params = new SubsetParams().set(SubsetParams.timePresent, true).setLatLonBoundingBox(bbox);
      GeoReferencedArray geo = coverage.readData(params);
      System.out.printf("geoCs shape=%s%n", Misc.showInts(geo.getCoordSysForData().getShape()));

      Array data = geo.getData();
      System.out.printf("data shape=%s%n", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(geo.getCoordSysForData().getShape(), data.getShape());

      int[] expectedShape = new int[] {1,99,105};
      Assert.assertArrayEquals(expectedShape, data.getShape());
      //NCdumpW.printArray(data);
      /*Index ima = data.getIndex();
      Assert.assertEquals(1.782, data.getDouble(ima.set(0,0,0)), Misc.maxReletiveError);
      Assert.assertEquals(1.769, data.getDouble(ima.set(0,11,0)), Misc.maxReletiveError); */
    }
  }


  @Test
  public void testNetcdf2D() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/mississippi.nc";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.CURVILINEAR);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "salt";
      Coverage coverage = gcs.findCoverage(gribId);
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);

      int[] expectedOrgShape = new int[] {1,20,64,128};
      Assert.assertArrayEquals(expectedOrgShape, cs.getShape());
      System.out.printf("org shape=%s%n", Misc.showInts(cs.getShape()));

      // just try to bisect ot along the width
      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(90, -180), new LatLonPointImpl(-90, -90));

      SubsetParams params = new SubsetParams().set(SubsetParams.timePresent, true).set(SubsetParams.latlonBB, bbox);
      GeoReferencedArray geo = coverage.readData(params);
      System.out.printf("geoCs shape=%s%n", Misc.showInts(geo.getCoordSysForData().getShape()));

      Array data = geo.getData();
      System.out.printf("data shape=%s%n", Misc.showInts(data.getShape()));
      Assert.assertArrayEquals(geo.getCoordSysForData().getShape(), data.getShape());

      int[] expectedShape = new int[] {1,20, 64, 75};
      Assert.assertArrayEquals(expectedShape, data.getShape());
      //NCdumpW.printArray(data);

    }
  }

}

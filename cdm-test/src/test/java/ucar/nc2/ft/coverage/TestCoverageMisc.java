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

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.ft2.coverage.writer.CFGridCoverageWriter2;
import ucar.nc2.util.Misc;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;

/**
 * Test coverage.getSizeInBytes()
 *
 * @author caron
 * @since 10/6/2015.
 */
@Category(NeedsCdmUnitTest.class)
public class TestCoverageMisc {

  @Test
  public void TestCoverageSize() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1";
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.GRID, gds.getCoverageType());

      String covName = "Temperature_isobaric";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      long size = cover.getSizeInBytes();
      Assert.assertEquals(25243920, size);

      covName = "Relative_humidity_layer_between_two_sigmas_layer";
      cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      size = cover.getSizeInBytes();
      Assert.assertEquals(870480, size);
    }
  }

  @Test
  public void TestCFWriterCoverageSize() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1";
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.GRID, gds.getCoverageType());

      // CFGridCoverageWriter2 adds another (dependent) time coordinate, so we need to test this case
      ucar.nc2.util.Optional<Long> opt = CFGridCoverageWriter2.writeOrTestSize(gds, Lists.newArrayList("Temperature_isobaric"),
              new SubsetParams(), false, true, null);
      Assert.assertTrue(opt.isPresent());

      long size = opt.get();
      Assert.assertEquals(25243920, size);
    }
  }

  @Test
  public void TestCoverageSubsetWithFullLatlonBounds() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1";
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.GRID, gds.getCoverageType());

      String covName = "Temperature_isobaric";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      long size = cover.getSizeInBytes();
      Assert.assertEquals(25243920, size);

      CoverageCoordSys csys = cover.getCoordSys();
      LatLonRect llbb = gds.getLatlonBoundingBox();
      ProjectionRect projBB = gds.getProjBoundingBox();
      ProjectionImpl proj = csys.getProjection();
      ProjectionRect projBB2 = proj.latLonToProjBB(llbb);
      System.out.printf("ProjRect =%s%n", projBB);
      System.out.printf("LatLonBB =%s%n", llbb);
      System.out.printf("ProjRect2=%s%n", projBB2);

      SubsetParams subset = new SubsetParams().setLatLonBoundingBox(gds.getLatlonBoundingBox()); // should be the same!
      Optional<CoverageCoordSys> opt = csys.subset(subset);
      Assert.assertTrue(opt.isPresent());

      CoverageCoordSys csyss = opt.get();
      Assert.assertEquals(csys.getXAxis().getNcoords(), csyss.getXAxis().getNcoords());
      Assert.assertEquals(csys.getYAxis().getNcoords(), csyss.getYAxis().getNcoords());
    }
  }

  @Test
  public void TestCoverageSubsetWithFullLatlonBoundsPS() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "tds/ncep/DGEX_Alaska_12km_20100524_0000.grib2"; // Polar stereographic
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.GRID, gds.getCoverageType());

      String covName = "Pressure_surface";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      long size = cover.getSizeInBytes();
      Assert.assertEquals(6433128, size);

      CoverageCoordSys csys = cover.getCoordSys();
      LatLonRect llbb = gds.getLatlonBoundingBox();
      ProjectionRect projBB = gds.getProjBoundingBox();
      ProjectionImpl proj = csys.getProjection();
      ProjectionRect projBB2 = proj.latLonToProjBB(llbb);
      System.out.printf("ProjRect =%s%n", projBB);
      System.out.printf("LatLonBB =%s%n", llbb);
      System.out.printf("ProjRect2=%s%n", projBB2);

      SubsetParams subset = new SubsetParams().setLatLonBoundingBox(gds.getLatlonBoundingBox()); // should be the same!
      Optional<CoverageCoordSys> opt = csys.subset(subset);
      Assert.assertTrue(opt.isPresent());

      CoverageCoordSys csyss = opt.get();
      Assert.assertEquals(csys.getXAxis().getNcoords(), csyss.getXAxis().getNcoords());
      Assert.assertEquals(csys.getYAxis().getNcoords(), csyss.getYAxis().getNcoords());
    }
  }

  // CFGridCoverageWriter2 adds another (dependent) time coordinate, so test that file

  @Test
  public void TestCFWriterCoverageRead() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "ncss/test/GFS_CONUS_80km_20120227_0000.grib1.nc4";
    System.out.printf("open %s%n", endpoint);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals(FeatureType.FMRC, gds.getCoverageType());

      String covName = "Temperature_isobaric";
      Coverage cover = gds.findCoverage(covName);
      Assert.assertNotNull(covName, cover);
      long size = cover.getSizeInBytes();
      Assert.assertEquals(25243920, size);

      SubsetParams subset = new SubsetParams().setVertCoord(300.0).setTimeOffset(42.0);
      GeoReferencedArray geo = cover.readData(subset);
      Array data = geo.getData();
      System.out.printf("%s%n", Misc.showInts(data.getShape()));
    }
  }

}

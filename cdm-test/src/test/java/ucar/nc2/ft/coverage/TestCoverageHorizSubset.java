/* Copyright Unidata */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.*;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.NeedsExternalResource;
import ucar.unidata.test.util.TestDir;

public class TestCoverageHorizSubset {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testMSG() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "transforms/Eumetsat.VerticalPerspective.grb";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "VAR_3-0-8";
      Coverage coverage = gcs.findCoverageByAttribute(Grib.VARIABLE_ID_ATTNAME, gribId); // "Pixel_scene_type");
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);

      Assert.assertEquals("coordSys", 3, cs.getShape().length);

      // bbox =  ll: 16.79S 20.5W+ ur: 14.1N 20.09E
      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(-16.79, -20.5), new LatLonPointImpl(14.1, 20.9));

      ProjectionImpl p = hcs.getTransform().getProjection();
      ProjectionRect prect = p.latLonToProjBB(bbox); // must override default implementation
      System.out.printf("%s -> %s %n", bbox, prect);
      assert Misc.closeEnough(prect.getMinX(), -2129.5688);
      assert Misc.closeEnough(prect.getWidth(), 4297.8453);
      assert Misc.closeEnough(prect.getMinY(), -1793.0041);
      assert Misc.closeEnough(prect.getHeight(), 3308.3885);

      LatLonRect bb2 = p.projToLatLonBB(prect);
      System.out.printf("%s -> %s %n", prect, bb2);

      SubsetParams params = new SubsetParams().set(SubsetParams.latlonBB, bbox);
      GeoReferencedArray geo = coverage.readData(params);

      int[] expectedShape = new int[] {1, 363, 479};
      Assert.assertArrayEquals(expectedShape, geo.getData().getShape());
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testLatLonSubset() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/SUPER-NATIONAL_latlon_IR_20070222_1600.nc";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "micron11";
      Coverage coverage = gcs.findCoverage(gribId);
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);

      Assert.assertEquals("rank", 2, cs.getShape().length);

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(40.0, -100.0), 10.0, 20.0);
      checkLatLonSubset(gcs, coverage, bbox, new int[]{141, 281});

      bbox = new LatLonRect(new LatLonPointImpl(-40.0, -180.0), 120.0, 300.0);
      checkLatLonSubset(gcs, coverage, bbox, new int[]{800, 1300});
    }
  }

  // longitude subsetting (CoordAxis1D regular)
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testLongitudeSubset() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_onedeg_20100913_0000.grib2";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "VAR_0-3-0_L1";
      Coverage coverage = gcs.findCoverageByAttribute(Grib.VARIABLE_ID_ATTNAME, gribId); // "Pressure_Surface");
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);
      Assert.assertEquals("rank", 3, cs.getShape().length);

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(40.0, -100.0), 10.0, 20.0);
      checkLatLonSubset(gcs, coverage, bbox, new int[]{1, 11, 21});
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testDodsSubset() throws Exception {
    String filename = "dods://thredds.ucar.edu/thredds/dodsC/grib/NCEP/GFS/CONUS_80km/best";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "Pressure_surface";
      Coverage coverage = gcs.findCoverage(gribId);
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);
      // Assert.assertArrayEquals(new int[]{65, 361, 720}, cs.getShape());

      LatLonRect llbb = gcs.getLatlonBoundingBox();
      LatLonRect llbb_subset = new LatLonRect(llbb.getLowerLeftPoint(), 20.0, llbb.getWidth() / 2);

      checkLatLonSubset(gcs, coverage, llbb_subset, new int[]{1, 35, 46});
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testCdmRemoteSubset() throws Exception {
    String filename = "cdmremote:http://thredds-dev.unidata.ucar.edu/thredds/cdmremote/grib/NCEP/NAM/CONUS_40km/conduit/best";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "Pressure_hybrid";
      Coverage coverage = gcs.findCoverage(gribId);
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);
      Assert.assertEquals("rank", 4, cs.getShape().length);

      LatLonRect llbb_subset = new LatLonRect(new LatLonPointImpl(-15, -140), new LatLonPointImpl(55, 30));

      System.out.println("subset lat/lon bbox= " + llbb_subset);

      checkLatLonSubset(gcs, coverage, llbb_subset, new int[]{1, 1, 129, 185});
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCrossLongitudeSeam() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_0p5deg_20100913_0000.grib2";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "VAR_2-0-0_L1";
      Coverage coverage = gcs.findCoverageByAttribute(Grib.VARIABLE_ID_ATTNAME, gribId); // Land_cover_0__sea_1__land_surface
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      System.out.printf(" org coverage shape=%s%n", Misc.showInts(cs.getShape()));

      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);
      Assert.assertEquals("rank", 3, cs.getShape().length);

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(40.0, -100.0), 10.0, 120.0);
      checkLatLonSubset(gcs, coverage, bbox, new int[]{1, 21, 241});
    }
  }

  private void checkLatLonSubset(CoverageCollection gcs, Coverage coverage, LatLonRect bbox, int[] expectedShape) throws Exception {
    System.out.printf(" coverage llbb = %s width=%f%n", gcs.getLatlonBoundingBox().toString2(), gcs.getLatlonBoundingBox().getWidth());
    System.out.printf(" constrain bbox= %s width=%f%n", bbox.toString2(), bbox.getWidth());

    SubsetParams params = new SubsetParams().setLatLonBoundingBox(bbox).setTimePresent();
    GeoReferencedArray geo = coverage.readData(params);
    CoverageCoordSys gcs2 = geo.getCoordSysForData();
    Assert.assertNotNull("CoordSysForData", gcs2);
    System.out.printf(" data cs shape=%s%n", Misc.showInts(gcs2.getShape()));
    System.out.printf(" data shape=%s%n", Misc.showInts(geo.getData().getShape()));

    Assert.assertArrayEquals("CoordSys=Data shape", gcs2.getShape(), geo.getData().getShape());
    Assert.assertArrayEquals("expected data shape", expectedShape, geo.getData().getShape());
  }

}

/* Copyright Unidata */
package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.*;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;
import ucar.unidata.test.util.ThreddsServer;

public class TestCoverageHorizSubset {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testMSG() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "transforms/Eumetsat.VerticalPerspective.grb";
    System.out.printf("open %s%n", filename);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageDataset gcs = cc.findCoverageDataset(CoverageCoordSys.Type.Grid);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "VAR_3-0-8";
      Coverage coverage = gcs.findCoverageByAttribute(GribIosp.VARIABLE_ID_ATTNAME, gribId); // "Pixel_scene_type");
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

  /*
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void test2D() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/mississippi.nc";
    System.out.printf("open %s%n", filename);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageDataset gcs = cc.findCoverageDataset(CoverageCoordSys.Type.Grid);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "salt";
      Coverage coverage = gcs.findCoverage(gribId);
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);

      Assert.assertEquals("coordSys", 4, cs.getShape().length);
      Assert.assertArrayEquals();


      GeoGrid grid_section = grid.subset(null, null, null, 5, 5, 5);

      Array data = grid_section.readDataSlice(-1, -1, -1, -1);
      assert data.getShape()[0] == 1 : data.getShape()[0];
      assert data.getShape()[1] == 4 : data.getShape()[1];
      assert data.getShape()[2] == 13 : data.getShape()[2];
      assert data.getShape()[3] == 26 : data.getShape()[3];

      grid_section = grid.subset(null, new Range(0, 0), null, 0, 2, 2);
      data = grid_section.readDataSlice(-1, -1, -1, -1);
      assert data.getShape()[0] == 1 : data.getShape()[0];
      assert data.getShape()[1] == 1 : data.getShape()[1];
      assert data.getShape()[2] == 32 : data.getShape()[2];
      assert data.getShape()[3] == 64 : data.getShape()[3];

      NCdumpW.printArray(data, "grid_section", System.out, null);

      LatLonPoint p0 = new LatLonPointImpl(29.0, -90.0);
      LatLonRect bbox = new LatLonRect(p0, 1.0, 2.0);
      grid_section = grid.subset(null, null, bbox, 1, 1, 1);
      data = grid_section.readDataSlice(-1, -1, -1, -1);

      assert data.getShape()[0] == 1 : data.getShape()[0];
      assert data.getShape()[1] == 20 : data.getShape()[1];
      assert data.getShape()[2] == 63 : data.getShape()[2];
      assert data.getShape()[3] == 53 : data.getShape()[3];

      gcs = grid_section.getCoordinateSystem();
      ProjectionRect rect = gcs.getBoundingBox();
      System.out.println(" rect= " + rect);

      p0 = new LatLonPointImpl(30.0, -90.0);
      bbox = new LatLonRect(p0, 1.0, 2.0);
      grid_section = grid.subset(null, null, bbox, 1, 1, 1);
      data = grid_section.readDataSlice(-1, -1, -1, -1);

      assert data.getShape()[0] == 1 : data.getShape()[0];
      assert data.getShape()[1] == 20 : data.getShape()[1];
      assert data.getShape()[2] == 18 : data.getShape()[2];
      assert data.getShape()[3] == 17 : data.getShape()[3];

      gcs = grid_section.getCoordinateSystem();
      System.out.println(" rect= " + gcs.getBoundingBox());
    }
  } */

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testLatLonSubset() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/SUPER-NATIONAL_latlon_IR_20070222_1600.nc";
    System.out.printf("open %s%n", filename);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageDataset gcs = cc.findCoverageDataset(CoverageCoordSys.Type.Grid);
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
      testLatLonSubset(gcs, coverage, bbox, new int[]{141, 281});

      bbox = new LatLonRect(new LatLonPointImpl(-40.0, -180.0), 120.0, 300.0);
      testLatLonSubset(gcs, coverage, bbox, new int[]{800, 1300});
    }
  }

  // longitude subsetting (CoordAxis1D regular)
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testLongitudeSubset() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_onedeg_20100913_0000.grib2";
    System.out.printf("open %s%n", filename);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageDataset gcs = cc.findCoverageDataset(CoverageCoordSys.Type.Grid);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "VAR_0-3-0_L1";
      Coverage coverage = gcs.findCoverageByAttribute(GribIosp.VARIABLE_ID_ATTNAME, gribId); // "Pressure_Surface");
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);
      Assert.assertEquals("rank", 3, cs.getShape().length);

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(40.0, -100.0), 10.0, 20.0);
      testLatLonSubset(gcs, coverage, bbox, new int[]{1, 11, 21});
    }
  }

  @Test
  public void testDodsSubset() throws Exception {
    ThreddsServer.LIVE.assumeIsAvailable();
    String filename = "dods://thredds.ucar.edu/thredds/dodsC/grib/NCEP/GFS/CONUS_80km/best";
    System.out.printf("open %s%n", filename);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageDataset gcs = cc.findCoverageDataset(CoverageCoordSys.Type.Grid);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "Pressure_surface";
      Coverage coverage = gcs.findCoverage(gribId);
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);
      Assert.assertEquals("rank", 3, cs.getShape().length);

      LatLonRect llbb = gcs.getLatLonBoundingBox();
      LatLonRect llbb_subset = new LatLonRect(llbb.getLowerLeftPoint(), 20.0, llbb.getWidth() / 2);
      System.out.println("subset lat/lon bbox= " + llbb_subset);

      testLatLonSubset(gcs, coverage, llbb_subset, new int[]{1, 35, 46});
    }
  }

  @Test
  public void testSubset2() throws Exception {
    ThreddsServer.LIVE.assumeIsAvailable();
    String filename = "http://thredds.ucar.edu/thredds/dodsC/grib/NCEP/NAM/CONUS_40km/conduit/best";
    System.out.printf("open %s%n", filename);

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageDataset gcs = cc.findCoverageDataset(CoverageCoordSys.Type.Grid);
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

      testLatLonSubset(gcs, coverage, llbb_subset, new int[]{1, 1, 129, 185});
    }
  }

  private void testLatLonSubset(CoverageDataset gcs, Coverage coverage, LatLonRect bbox, int[] expectedShape) throws Exception {
    System.out.printf(" coverage bbox= %s%n", gcs.getProjBoundingBox().toString2());
    System.out.printf(" coverage llbb= %s%n", gcs.getLatLonBoundingBox().toString2());


    //LatLonProjection llproj = new LatLonProjection();
    //ucar.unidata.geoloc.ProjectionRect[] prect = llproj.latLonToProjRect(bbox);
    // System.out.println("\n     grid bbox= " + coverage.getCoordSys().getHorizCoordSys().getBB.toString2());
    System.out.println(" constrain bbox= " + bbox.toString2());

    SubsetParams params = new SubsetParams().set(SubsetParams.latlonBB, bbox).set(SubsetParams.timePresent, true);
    GeoReferencedArray geo = coverage.readData(params);
    CoverageCoordSys gcs2 = geo.getCoordSysForData();
    Assert.assertNotNull("CoordSysForData", gcs2);
    Assert.assertEquals("CoordSysForData", expectedShape.length, gcs2.getShape().length);

    //ucar.unidata.geoloc.ProjectionRect subset_prect = gcs2.getBoundingBox();
    //System.out.println(" resulting bbox= " + gcs2.getLatLonBoundingBox().toString2());

    //Assert.assertTrue("CoordSysForData", bbox.containedIn());

    Assert.assertArrayEquals(expectedShape, geo.getData().getShape());
  }

}

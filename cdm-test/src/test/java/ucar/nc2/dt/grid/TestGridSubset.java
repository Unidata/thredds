/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.grid;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.Range;
import ucar.nc2.NCdumpW;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.vertical.VerticalTransform;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestGridSubset {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testRegular() throws Exception {
    try(GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/nuwg/03061219_ruc.nc")) {

      GeoGrid grid = dataset.findGridByName("T");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 4;

      CoordinateAxis zaxis = gcs.getVerticalAxis();
      assert zaxis.getUnitsString().equals("hectopascals");

      GeoGrid grid_section = grid.subset(null, null, null, 3, 3, 3);

      GridCoordSystem gcs_section = grid_section.getCoordinateSystem();
      CoordinateAxis zaxis2 = gcs_section.getVerticalAxis();
      assert zaxis2.getSize() == 7;
      assert zaxis2.getUnitsString().equals("hectopascals");
      assert gcs_section.getTimeAxis().equals(gcs.getTimeAxis());

      Array data = grid_section.readDataSlice(-1, -1, -1, -1);
      assert data.getShape()[0] == 2 : data.getShape()[0];
      assert data.getShape()[1] == 7 : data.getShape()[1];
      assert data.getShape()[2] == 22 : data.getShape()[2];
      assert data.getShape()[3] == 31 : data.getShape()[3];

      // check axes
      for (CoordinateAxis axis : gcs_section.getCoordinateAxes()) {
        assert axis.getAxisType() != null;
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGrib() throws Exception {
    try(GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "formats/grib1/AVN.wmo")) {

      GeoGrid grid = dataset.findGridDatatypeByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_7-0-2-11_L100"); // "Temperature_isobaric");
      assert null != grid : dataset.getLocation();
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 4;

      GeoGrid grid_section = grid.subset(null, null, null, 3, 3, 3);

      Array data = grid_section.readDataSlice(-1, -1, -1, -1);
      assert data.getShape()[0] == 3 : data.getShape()[0];
      assert data.getShape()[1] == 3 : data.getShape()[1];
      assert data.getShape()[2] == 13 : data.getShape()[2];
      assert data.getShape()[3] == 15 : data.getShape()[3];
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testWRF() throws Exception {
    try(GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc")) {

      GeoGrid grid = dataset.findGridByName("T");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 4;

      CoordinateAxis zaxis = gcs.getVerticalAxis();
      assert zaxis.getSize() == 27;

      VerticalTransform vt = gcs.getVerticalTransform();
      assert vt != null;
      assert vt.getUnitString().equals("Pa");

      GeoGrid grid_section = grid.subset(null, null, null, 3, 3, 3);

      Array data = grid_section.readDataSlice(-1, -1, -1, -1);
      assert data.getShape()[0] == 13 : data.getShape()[0];
      assert data.getShape()[1] == 9 : data.getShape()[1];
      assert data.getShape()[2] == 20 : data.getShape()[2];
      assert data.getShape()[3] == 25 : data.getShape()[3];

      GridCoordSystem gcs_section = grid_section.getCoordinateSystem();
      CoordinateAxis zaxis2 = gcs_section.getVerticalAxis();
      assert zaxis2.getSize() == 9 : zaxis2.getSize();

      assert zaxis2.getUnitsString().equals(zaxis.getUnitsString());
      assert gcs_section.getTimeAxis().equals(gcs.getTimeAxis());

      VerticalTransform vt_section = gcs_section.getVerticalTransform();
      assert vt_section != null;
      assert vt_section.getUnitString().equals(vt.getUnitString());
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testMSG() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "transforms/Eumetsat.VerticalPerspective.grb";
    try(GridDataset dataset = GridDataset.open(filename)) {
      logger.debug("open {}", filename);
      GeoGrid grid = dataset.findGridDatatypeByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_3-0-8"); // "Pixel_scene_type");
      assert null != grid : dataset.getLocation();
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 3;

      // bbox =  ll: 16.79S 20.5W+ ur: 14.1N 20.09E
      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(-16.79, -20.5), new LatLonPointImpl(14.1, 20.9));

      ProjectionImpl p = gcs.getProjection();
      ProjectionRect prect = p.latLonToProjBB(bbox); // must override default implementation
      logger.debug("{} -> {}", bbox, prect);

      ProjectionRect expected = new ProjectionRect(
              new ProjectionPointImpl(-2129.5688, -1793.0041), 4297.8453, 3308.3885);
      assert prect.nearlyEquals(expected);

      LatLonRect bb2 = p.projToLatLonBB(prect);
      logger.debug("{} -> {}", prect, bb2);

      GeoGrid grid_section = grid.subset(null, null, bbox, 1, 1, 1);

      Array data = grid_section.readDataSlice(-1, -1, -1, -1);
      assert data.getRank() == 3;
      int[] shape = data.getShape();
      assert shape[0] == 1 : shape[0] + " should be 1";
      assert shape[1] == 363 : shape[1] + " should be 363";
      assert shape[2] == 479 : shape[2] + " should be 479";
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void test2D() throws Exception {
    try(GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/cf/mississippi.nc")) {

      GeoGrid grid = dataset.findGridByName("salt");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 4;

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

      logger.debug(NCdumpW.toString(data, "grid_section", null));

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
      logger.debug(" rect = {}", rect);

      p0 = new LatLonPointImpl(30.0, -90.0);
      bbox = new LatLonRect(p0, 1.0, 2.0);
      grid_section = grid.subset(null, null, bbox, 1, 1, 1);
      data = grid_section.readDataSlice(-1, -1, -1, -1);

      assert data.getShape()[0] == 1 : data.getShape()[0];
      assert data.getShape()[1] == 20 : data.getShape()[1];
      assert data.getShape()[2] == 18 : data.getShape()[2];
      assert data.getShape()[3] == 17 : data.getShape()[3];

      gcs = grid_section.getCoordinateSystem();
      logger.debug(" rect = {}", gcs.getBoundingBox());
    }
  }


  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testLatLonSubset() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/cf/SUPER-NATIONAL_latlon_IR_20070222_1600.nc")) {
      GeoGrid grid = dataset.findGridByName("micron11");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 2;

      logger.debug("original bbox = {}", gcs.getBoundingBox());

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(40.0, -100.0), 10.0, 20.0);
      testLatLonSubset(grid, bbox, new int[]{141, 281});

      bbox = new LatLonRect(new LatLonPointImpl(-40.0, -180.0), 120.0, 300.0);
      testLatLonSubset(grid, bbox, new int[]{800, 1300});
    }
  }

  private void testLatLonSubset(GeoGrid grid, LatLonRect bbox, int[] shape) throws Exception {
    logger.debug("grid bbox = {}", grid.getCoordinateSystem().getLatLonBoundingBox().toString2());
    logger.debug("constrain bbox = {}", bbox.toString2());

    GeoGrid grid_section = grid.subset(null, null, bbox, 1, 1, 1);
    GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
    assert null != gcs2;
    assert grid_section.getRank() == 2;

    logger.debug("resulting bbox = {}", gcs2.getLatLonBoundingBox().toString2());

    Array data = grid_section.readDataSlice(0, 0, -1, -1);
    assert data != null;
    assert data.getRank() == 2;
    assert data.getShape()[0] == shape[0] : data.getShape()[0];
    assert data.getShape()[1] == shape[1] : data.getShape()[1];
  }

  // longitude subsetting (CoordAxis1D regular)
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testLatLonSubset2() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_onedeg_20100913_0000.grib2")) {
      GeoGrid grid = dataset.findGridDatatypeByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_0-3-0_L1"); // "Pressure_Surface");
      assert null != grid : dataset.getLocation();
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 3 : grid.getRank();

      logger.debug("original bbox = {}", gcs.getBoundingBox());
      logger.debug("lat/lon bbox = {}", gcs.getLatLonBoundingBox().toString2());

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(40.0, -100.0), 10.0, 20.0);
      logger.debug("constrain bbox = {}", bbox.toString2());

      GeoGrid grid_section = grid.subset(null, null, bbox, 1, 1, 1);
      GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
      assert null != gcs2;
      assert grid_section.getRank() == grid.getRank();

      logger.debug("resulting bbox = {}", gcs2.getLatLonBoundingBox().toString2());

      Array data = grid_section.readDataSlice(0, 0, -1, -1);
      assert data != null;
      assert data.getRank() == 2;

      int[] dataShape = data.getShape();
      assert dataShape.length == 2;
      assert dataShape[0] == 11 : data.getShape()[0];
      assert dataShape[1] == 21 : data.getShape()[1];
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGiniSubsetStride() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "formats/gini/WEST-CONUS_4km_IR_20070216_1500.gini")) {
      GeoGrid grid = dataset.findGridByName("IR");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 3;
      int[] org_shape = grid.getShape();

      Array data_org = grid.readDataSlice(0, 0, -1, -1);
      assert data_org != null;
      assert data_org.getRank() == 2;
      int[] data_shape = data_org.getShape();
      assert org_shape[1] == data_shape[0];
      assert org_shape[2] == data_shape[1];

      logger.debug("original bbox = {}" + gcs.getBoundingBox());

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(40.0, -100.0), 10.0, 20.0);

      LatLonProjection llproj = new LatLonProjection();
      ucar.unidata.geoloc.ProjectionRect[] prect = llproj.latLonToProjRect(bbox);
      logger.debug("constrain bbox = {}", prect[0]);

      GeoGrid grid_section = grid.subset(null, null, bbox, 1, 2, 3);
      GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
      assert null != gcs2;
      assert grid_section.getRank() == 3;

      ucar.unidata.geoloc.ProjectionRect subset_prect = gcs2.getBoundingBox();
      logger.debug("resulting bbox = {}", subset_prect);

      // test stride
      grid_section = grid.subset(null, null, null, 1, 2, 3);
      Array data = grid_section.readDataSlice(0, 0, -1, -1);
      assert data != null;
      assert data.getRank() == 2;

      int[] shape = data.getShape();
      assert Math.abs(org_shape[1] - 2 * shape[0]) < 2 : org_shape[2] + " != " + (2 * shape[0]);
      assert Math.abs(org_shape[2] - 3 * shape[1]) < 3 : org_shape[2] + " != " + (3 * shape[1]);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testVerticalAxis() throws Exception {
    String uri = TestDir.cdmUnitTestDir + "ncml/nc/cg/CG2006158_120000h_usfc.nc";
    String varName = "CGusfc";

    try (GridDataset dataset = GridDataset.open(uri)) {
      GeoGrid grid = dataset.findGridByName(varName);
      assert null != grid;

      GridCoordSystem gcsi = grid.getCoordinateSystem();
      assert null != gcsi;
      assert (gcsi.getVerticalAxis() != null);

      GridCoordSys gcs = (GridCoordSys) grid.getCoordinateSystem();
      assert null != gcs;
      assert gcs.hasVerticalAxis();          // returns true.

      // subset geogrid
      GeoGrid subg = grid.subset(null, null, null, 1, 1, 1);
      assert null != subg;

      GridCoordSystem gcsi2 = subg.getCoordinateSystem();
      assert null != gcsi2;
      assert (gcsi2.getVerticalAxis() != null);

      GridCoordSys gcs2 = (GridCoordSys) subg.getCoordinateSystem();
      assert null != gcs2;
      assert !gcs2.hasVerticalAxis();          // fails
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testBBSubsetVP() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "transforms/Eumetsat.VerticalPerspective.grb";
    try (GridDataset dataset = GridDataset.open(filename)) {
      GeoGrid grid = dataset.findGridDatatypeByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_3-0-8"); // "Pixel_scene_type");
      assert null != grid : dataset.getLocation();
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;

      logger.debug("original bbox = {}", gcs.getBoundingBox());
      logger.debug("lat/lon bbox = {}", gcs.getLatLonBoundingBox());

      ucar.unidata.geoloc.LatLonRect llbb_subset = new LatLonRect(new LatLonPointImpl(), 20.0, 40.0);
      logger.debug("subset lat/lon bbox = {}", llbb_subset);

      GeoGrid grid_section = grid.subset(null, null, llbb_subset, 1, 1, 1);
      GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
      assert null != gcs2;

      logger.debug("result lat/lon bbox = {}", gcs2.getLatLonBoundingBox());
      logger.debug("result bbox = {}", gcs2.getBoundingBox());

      ProjectionRect pr = gcs2.getProjection().getDefaultMapArea();
      logger.debug("projection mapArea = {}", pr);
      assert (pr.nearlyEquals(gcs2.getBoundingBox()));
    }
  }

  // x,y in meters
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testBBSubsetUnits() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "ncml/testBBSubsetUnits.ncml")) {
      logger.debug("file = {}", dataset.getLocation());

      GeoGrid grid = dataset.findGridByName("pr");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;

      logger.debug("original bbox = {}", gcs.getBoundingBox());
      logger.debug("lat/lon bbox = {}", gcs.getLatLonBoundingBox());

      ucar.unidata.geoloc.LatLonRect llbb_subset = new LatLonRect(new LatLonPointImpl(38, -110), new LatLonPointImpl(42, -90));
      logger.debug("subset lat/lon bbox = {}", llbb_subset);

      GeoGrid grid_section = grid.subset(null, null, llbb_subset, 1, 1, 1);
      GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
      assert null != gcs2;

      logger.debug("result lat/lon bbox = {}", gcs2.getLatLonBoundingBox());
      logger.debug("result bbox = {}", gcs2.getBoundingBox());

      ProjectionRect pr = gcs2.getProjection().getDefaultMapArea();
      logger.debug("projection mapArea = {}", pr);
      assert (pr.nearlyEquals(gcs2.getBoundingBox()));

      CoordinateAxis xaxis = gcs.getXHorizAxis();
      CoordinateAxis yaxis = gcs.getYHorizAxis();
      logger.debug("(nx,ny)= {}, {}", xaxis.getSize(), yaxis.getSize());
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testAggByteGiniSubsetStride() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "formats/gini/giniAggByte.ncml")) {
      logger.debug("Test {}", dataset.getLocation());
      GeoGrid grid = dataset.findGridByName("IR");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 3;
      int[] org_shape = grid.getShape();
      assert grid.getDataType() == DataType.UINT;

      Array data_org = grid.readDataSlice(0, 0, -1, -1);
      assert data_org != null;
      assert data_org.getRank() == 2;
      int[] data_shape = data_org.getShape();
      assert org_shape[1] == data_shape[0];
      assert org_shape[2] == data_shape[1];
      assert data_org.getElementType() == int.class : data_org.getElementType();

      logger.debug("original bbox = {}", gcs.getBoundingBox());

      LatLonRect bbox = new LatLonRect(new LatLonPointImpl(40.0, -100.0), 10.0, 20.0);

      LatLonProjection llproj = new LatLonProjection();
      ucar.unidata.geoloc.ProjectionRect[] prect = llproj.latLonToProjRect(bbox);
      logger.debug("constrain bbox = {}", prect[0]);

      GeoGrid grid_section = grid.subset(null, null, bbox, 1, 2, 3);
      GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
      assert null != gcs2;
      assert grid_section.getRank() == 3;
      assert grid_section.getDataType() == DataType.UINT;

      ucar.unidata.geoloc.ProjectionRect subset_prect = gcs2.getBoundingBox();
      logger.debug("resulting bbox = {}", subset_prect);

      // test stride
      grid_section = grid.subset(null, null, null, 2, 2, 3);
      Array data = grid_section.readVolumeData(1);
      assert data != null;
      assert data.getRank() == 2;
      assert data.getElementType() == int.class;

      int[] shape = data.getShape();
      assert Math.abs(org_shape[1] - 2 * shape[0]) < 2 : org_shape[2] + " != " + (2 * shape[0]);
      assert Math.abs(org_shape[2] - 3 * shape[1]) < 3 : org_shape[2] + " != " + (3 * shape[1]);
    }
  }

  @Test
  @Ignore("Does this file exist in a shared location?")
  public void testBBSubsetVP2() throws Exception {
    String filename = "C:/Documents and Settings/caron/My Documents/downloads/MSG2-SEVI-MSGCLAI-0000-0000-20070522114500.000000000Z-582760.grb";
    try (GridDataset dataset = GridDataset.open(filename)) {
      GeoGrid grid = dataset.findGridByName("Pixel_scene_type");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;

      logger.debug("original bbox = {}", gcs.getBoundingBox());
      logger.debug("lat/lon bbox = {}", gcs.getLatLonBoundingBox());

      ucar.unidata.geoloc.LatLonRect llbb_subset = new LatLonRect(new LatLonPointImpl(), 20.0, 40.0);
      logger.debug("subset lat/lon bbox = {}", llbb_subset);

      GeoGrid grid_section = grid.subset(null, null, llbb_subset, 1, 1, 1);
      GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
      assert null != gcs2;

      logger.debug("result lat/lon bbox = {}", gcs2.getLatLonBoundingBox());
      logger.debug("result bbox = {}", gcs2.getBoundingBox());

      ProjectionRect pr = gcs2.getProjection().getDefaultMapArea();
      logger.debug("projection mapArea = {}", pr);
      assert (pr.nearlyEquals(gcs2.getBoundingBox()));
    }
  }

  // this one has the coordinate bounds set in the file
  @Test
  public void testSubsetCoordEdges() throws Exception {
    try (NetcdfDataset fooDataset = NetcdfDataset.openDataset(TestDir.cdmLocalTestDataDir + "ncml/subsetCoordEdges.ncml")) {
      logger.debug("Open {}", fooDataset.getLocation());
      CompareNetcdf2 compare = new CompareNetcdf2();
      boolean ok = true;

      GridDataset fooGridDataset = new GridDataset(fooDataset);

      GridDatatype fooGrid = fooGridDataset.findGridDatatype("foo");
      assert fooGrid != null;

      CoordinateAxis1D fooTimeAxis = fooGrid.getCoordinateSystem().getTimeAxis1D();
      CoordinateAxis1D fooLatAxis = (CoordinateAxis1D) fooGrid.getCoordinateSystem().getYHorizAxis();
      CoordinateAxis1D fooLonAxis = (CoordinateAxis1D) fooGrid.getCoordinateSystem().getXHorizAxis();

      logger.debug("mid time = {}", Arrays.toString(fooTimeAxis.getCoordValues()));
      logger.debug("edge time = {}", Arrays.toString(fooTimeAxis.getCoordEdges()));

      ok &= compare.compareData("time getCoordValues", fooTimeAxis.getCoordValues(), new double[] {15.5, 45.0, 74.5, 105.0});
      ok &= compare.compareData("time getCoordEdges", fooTimeAxis.getCoordEdges(), new double[] {0.0, 31.0, 59.0, 90.0, 120.0});

      // Expected: [-90.0, -18.0, 36.0, 72.0, 90.0]
      // Actual:   [-90.0, -18.0, 36.0, 72.0, 90.0]
      logger.debug("mid lat = {}", Arrays.toString(fooLatAxis.getCoordValues()));
      logger.debug("edge lat = {}", Arrays.toString(fooLatAxis.getCoordEdges()));

      ok &= compare.compareData("lat getCoordValues", fooLatAxis.getCoordValues(), new double[] {-54.0, 9.0, 54.0, 81.0});
      ok &= compare.compareData("lat getCoordEdges", fooLatAxis.getCoordEdges(), new double[] {-90.0, -18.0, 36.0, 72.0, 90.0});

      // Expected: [0.0, 36.0, 108.0, 216.0, 360.0]
      // Actual:   [0.0, 36.0, 108.0, 216.0, 360.0]
      logger.debug("mid lon= " + Arrays.toString(fooLonAxis.getCoordValues()));
      logger.debug("edge lon= " + Arrays.toString(fooLonAxis.getCoordEdges()));

      ok &= compare.compareData("lon getCoordValues", fooLonAxis.getCoordValues(), new double[] {18.0, 72.0, 162.0, 288.0});
      ok &= compare.compareData("lon getCoordEdges", fooLonAxis.getCoordEdges(), new double[] {0.0, 36.0, 108.0, 216.0, 360.0});

      // take mid range for all of the 3 coordinates
      Range middleRange = new Range(1, 2);
      GridDatatype fooSubGrid = fooGrid.makeSubset(null, null, middleRange, null, middleRange, middleRange);

      CoordinateAxis1D fooSubTimeAxis = fooSubGrid.getCoordinateSystem().getTimeAxis1D();
      CoordinateAxis1D fooSubLatAxis = (CoordinateAxis1D) fooSubGrid.getCoordinateSystem().getYHorizAxis();
      CoordinateAxis1D fooSubLonAxis = (CoordinateAxis1D) fooSubGrid.getCoordinateSystem().getXHorizAxis();

      // Expected: [31.0, 59.0, 90.0]
      // Actual:   [30.25, 59.75, 89.25]
      logger.debug("subset mid time = {}", Arrays.toString(fooSubTimeAxis.getCoordValues()));
      logger.debug("subset edge time = {}", Arrays.toString(fooSubTimeAxis.getCoordEdges()));
      ok &= compare.compareData("subset time getCoordValues", fooSubTimeAxis.getCoordValues(), new double[] {45.0, 74.5});
      ok &= compare.compareData("subset time getCoordEdges", fooSubTimeAxis.getCoordEdges(), new double[] {31.0, 59.0, 90.0});

      // Expected: [-18.0, 36.0, 72.0]
      // Actual:   [-13.5, 31.5, 76.5]
      logger.debug("subset mid lat = {}", Arrays.toString(fooSubLatAxis.getCoordValues()));
      logger.debug("subset edge lat = {}", Arrays.toString(fooSubLatAxis.getCoordEdges()));
      ok &= compare.compareData("subset lat getCoordValues", fooSubLatAxis.getCoordValues(), new double[] {9.0, 54.0} );
      ok &= compare.compareData("subset lat getCoordEdges", fooSubLatAxis.getCoordEdges(), new double[] {-18.0, 36.0, 72.0} );

      // Expected: [36.0, 108.0, 216.0]
      // Actual:   [27.0, 117.0, 207.0]
      logger.debug("subset mid lon = {}", Arrays.toString(fooSubLonAxis.getCoordValues()));
      logger.debug("subset edge lon = {}", Arrays.toString(fooSubLonAxis.getCoordEdges()));
      ok &= compare.compareData("subset lon getCoordValues", fooSubLonAxis.getCoordValues(), new double[] {72.0, 162.0, } );
      ok &= compare.compareData("subset lon getCoordEdges", fooSubLonAxis.getCoordEdges(), new double[] {36.0, 108.0, 216.0} );

      assert ok : "not ok";
    }
  }

  @Test
  @Ignore("Does this file exist in a shared location?")
  public void testAaron() throws Exception{
     // different scale/offset in aggregation
     try (GridDataset dataset = GridDataset.open("G:/work/braekel/dataset.ncml" )) {
       GridDatatype grid = null;
       for (GridDatatype thisGrid : dataset.getGrids()) {
         if (thisGrid.getName().equals("cref")) {
           grid = thisGrid;
         }
       }
       List<Range> ranges = new ArrayList<Range>();
       ranges.add(new Range(0, 0));
       ranges.add(new Range(0, 0));
       ranges.add(new Range(638, 638));
       ranges.add(new Range(3750, 4622));

       Array arr = grid.getVariable().read(ranges);
       Index index = arr.getIndex();
       index.set(new int[]{0, 0, 0, 834});
       logger.debug("index {} value {}", index.currentElement(), arr.getDouble(index));
     }
   }

  // has runtime(time), time(time)
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testTPgribCollection() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx4")) {
      GeoGrid grid = dataset.findGridByName("Pressure_surface");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      logger.debug("{}", gcs);
      CoordinateAxis runtime = gcs.getRunTimeAxis();
      CoordinateAxis time = gcs.getTimeAxis();
      assert runtime != null;
      assert time != null;
      assert runtime.getSize() == time.getSize();
      assert runtime.getSize() == 2;
      assert runtime.getDimension(0) == time.getDimension(0);

      GeoGrid grid2 = grid.subset(new Range(1,1), null, null, 1, 1, 1);
      GridCoordSystem gcs2 = grid2.getCoordinateSystem();
      assert null != gcs2;
      logger.debug("{}", gcs2);
      runtime = gcs2.getRunTimeAxis();
      time = gcs2.getTimeAxis();
      assert runtime != null;
      assert time != null;
      assert runtime.getSize() == time.getSize();
      assert runtime.getSize() == 1;
      Assert.assertEquals(runtime.getDimension(0), time.getDimension(0));

      // read a random point
      Array data = grid.readDataSlice(1, 0, 10, 20);
      Array data2 = grid2.readDataSlice(0, 0, 10, 20);

      logger.debug(NCdumpW.toString(data, "org", null));
      logger.debug(NCdumpW.toString(data2, "subset", null));

      ucar.unidata.util.test.CompareNetcdf.compareData(data, data2);
    }
  }
}

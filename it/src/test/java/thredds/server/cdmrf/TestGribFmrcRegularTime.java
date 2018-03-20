/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.cdmrf;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.tools.DataFactory;
import thredds.server.catalog.TdsLocalCatalog;
import ucar.ma2.Array;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * This dataset has regular, not orthogonal times.
 * Read through cdmrf (FeatureDatasetCoverage) and cdmremote (GridDataset)
 *
 * @author caron
 * @since 3/1/2016.
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribFmrcRegularTime {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /* Relies on:
  <featureCollection name="NDFD-CONUS-5km.v5" featureType="GRIB2" harvest="true" path="grib.v5/NDFD/CONUS_5km">
    <collection spec="${cdmUnitTest}/datasets/NDFD-CONUS-5km/.*grib2$" timePartition="file" />
    ..
  </featureCollection>
  in tds/src/test/content/thredds/catalogs5/catalogGrib5.xml
   */
  @Test
  public void testCoverageDataset2D() throws IOException {
    String catalog = "/catalog/grib.v5/NDFD/CONUS_5km/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);

    String id = "grib.v5/NDFD/CONUS_5km/TwoD";
    Dataset ds = cat.findDatasetByID(id);
    assert (ds != null) : "cant find dataset id="+id;
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset(ds, null)) {
      assert !dataResult.fatalError : dataResult.errLog;
      assert dataResult.featureDataset != null;
      Assert.assertEquals(FeatureDatasetCoverage.class, dataResult.featureDataset.getClass());

      FeatureDatasetCoverage gds = (FeatureDatasetCoverage) dataResult.featureDataset;
      String gridName = "Maximum_temperature_Forecast_height_above_ground_12_Hour_Maximum";
      VariableSimpleIF vs = gds.getDataVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      Assert.assertEquals(1, gds.getCoverageCollections().size());
      CoverageCollection cc = gds.getCoverageCollections().get(0);
      Coverage grid = cc.findCoverage(gridName);
      Assert.assertNotNull(gridName, grid);

      int[] expectShape = new int[] {4,4,1,689,1073};
      Assert.assertArrayEquals(expectShape, grid.getShape());

      CoverageCoordSys gcs = grid.getCoordSys();
      Assert.assertNotNull(gcs);

      CoverageCoordAxis reftime = gcs.getAxis(AxisType.RunTime);
      Assert.assertNotNull(reftime);
      Assert.assertEquals(4, reftime.getNcoords());
      double[] want = new double[]{0., 12., 24., 36.};
      CompareNetcdf2 cn = new CompareNetcdf2();
      assert cn.compareData("time", reftime.getCoordsAsArray(), Array.makeFromJavaArray(want), false);

      CoverageCoordAxis time = gcs.getTimeAxis();
      Assert.assertNotNull(time);
      Assert.assertTrue(time instanceof TimeAxis2DFmrc);
      Assert.assertEquals(16, time.getNcoords());

      //double[] want = new double[]{108.000000, 132.000000, 156.000000, 180.000000};
      //assert cn.compareData("time", time.getCoordsAsArray(), Array.makeFromJavaArray(want), false);
    }
  }

  /* Relies on:
  <featureCollection name="NDFD-CONUS-5km.v5" featureType="GRIB2" harvest="true" path="grib.v5/NDFD/CONUS_5km">
    <collection spec="${cdmUnitTest}/datasets/NDFD-CONUS-5km/.*grib2$" timePartition="file" />
    ..
  </featureCollection>
  in tds/src/test/content/thredds/catalogs5/catalogGrib5.xml
   */
  @Test
  public void testCoverageLatest() throws IOException {
    String catalog = "/catalog/grib.v5/NDFD/CONUS_5km/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);

    Dataset ds = cat.findDatasetByID("latest.xml");
    assert (ds != null) : "cant find dataset 'dataset=latest.xml'";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset(ds, null)) {
      Assert.assertFalse(dataResult.errLog.toString(), dataResult.fatalError);
      Assert.assertNotNull(dataResult.featureDataset);
      Assert.assertEquals(FeatureDatasetCoverage.class, dataResult.featureDataset.getClass());

      FeatureDatasetCoverage gds = (FeatureDatasetCoverage) dataResult.featureDataset;
      String gridName = "Maximum_temperature_Forecast_height_above_ground_12_Hour_Maximum";
      VariableSimpleIF vs = gds.getDataVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      Assert.assertEquals(1, gds.getCoverageCollections().size());
      CoverageCollection cc = gds.getCoverageCollections().get(0);
      Coverage grid = cc.findCoverage(gridName);
      Assert.assertNotNull(gridName, grid);

      int[] expectShape = new int[] {4,1,689,1073};
      Assert.assertArrayEquals(expectShape, grid.getShape());

      CoverageCoordSys gcs = grid.getCoordSys();
      Assert.assertNotNull(gcs);

      CoverageCoordAxis reftime = gcs.getAxis(AxisType.RunTime);
      Assert.assertNotNull(reftime);
      Assert.assertEquals(1, reftime.getNcoords());
      double[] runtimeValues = new double[]{0.};
      CompareNetcdf2 cn = new CompareNetcdf2();
      assert cn.compareData("time", reftime.getCoordsAsArray(), Array.makeFromJavaArray(runtimeValues), false);

      CoverageCoordAxis time = gcs.getTimeAxis();
      Assert.assertNotNull(time);
      Assert.assertEquals(4, time.getNcoords());
      double[] timeValues = new double[]{102.,126.,150,174};  // midpoints
      assert cn.compareData("time", time.getCoordsAsArray(), Array.makeFromJavaArray(timeValues), false);
    }
  }

  // this is the same dataset, seem through cdmremote / gridDataset

  /* Relies on:
  <featureCollection name="NDFD-CONUS-5km" featureType="GRIB2" harvest="true" path="grib/NDFD/CONUS_5km">
    ...
    <collection spec="${cdmUnitTest}/datasets/NDFD-CONUS-5km/.*grib2$"
    ...
  </featureCollection>
  in tds/src/test/content/thredds/catalogGrib.xml
   */
  @Test
  public void testGribDataset2D() throws IOException {
    String catalog = "/catalog/grib/NDFD/CONUS_5km/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);

    Dataset ds = cat.findDatasetByID("grib/NDFD/CONUS_5km/TwoD");
    assert (ds != null) : "cant find dataset 'dataset=grib/NDFD/CONUS_5km/TwoD'";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset(ds, null)) {
      assert !dataResult.fatalError : dataResult.errLog;
      assert dataResult.featureDataset != null;
      Assert.assertEquals(GridDataset.class, dataResult.featureDataset.getClass());

      GridDataset gds = (GridDataset) dataResult.featureDataset;
      String gridName = "Maximum_temperature_Forecast_height_above_ground_12_Hour_Maximum";
      VariableSimpleIF vs = gds.getDataVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      GridDatatype grid = gds.findGridByShortName(gridName);
      Assert.assertNotNull(gridName, grid);

      int[] expectShape = new int[] {4,4,1,689,1073};
      Assert.assertArrayEquals(expectShape, grid.getShape());

      GridCoordSystem gcs = grid.getCoordinateSystem();
      Assert.assertNotNull(gcs);

      CoordinateAxis1DTime reftime = gcs.getRunTimeAxis();
      Assert.assertNotNull(reftime);
      Assert.assertEquals(4, reftime.getSize());
      double[] want = new double[]{0., 12., 24., 36.};
      CompareNetcdf2 cn = new CompareNetcdf2();
      assert cn.compareData("reftime", reftime.read(), Array.makeFromJavaArray(want), false);

      CoordinateAxis time = gcs.getTimeAxis();
      Assert.assertNotNull(time);
      Assert.assertEquals(CoordinateAxis2D.class, time.getClass());
      Assert.assertEquals(16, time.getSize());

      //double[] want = new double[]{108.000000, 132.000000, 156.000000, 180.000000};
      //assert cn.compareData("time", time.getCoordsAsArray(), Array.makeFromJavaArray(want), false);
    }
  }

  /* Relies on:
  <featureCollection name="NDFD-CONUS-5km" featureType="GRIB2" harvest="true" path="grib/NDFD/CONUS_5km">
    ...
    <collection spec="${cdmUnitTest}/datasets/NDFD-CONUS-5km/.*grib2$"
    ...
  </featureCollection>
  in tds/src/test/content/thredds/catalogGrib.xml
   */
  @Test
  public void testGribLatest() throws IOException {
    String catalog = "/catalog/grib/NDFD/CONUS_5km/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);

    Dataset ds = cat.findDatasetByID("latest.xml");
    assert (ds != null) : "cant find dataset 'dataset=latest.xml'";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset(ds, null)) {
      Assert.assertFalse(dataResult.errLog.toString(), dataResult.fatalError);
      Assert.assertNotNull(dataResult.featureDataset);
      Assert.assertEquals(GridDataset.class, dataResult.featureDataset.getClass());

      GridDataset gds = (GridDataset) dataResult.featureDataset;
      String gridName = "Maximum_temperature_Forecast_height_above_ground_12_Hour_Maximum";
      VariableSimpleIF vs = gds.getDataVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      GridDatatype grid = gds.findGridByShortName(gridName);
      Assert.assertNotNull(gridName, grid);

      int[] expectShape = new int[] {4,1,689,1073};
      Assert.assertArrayEquals(expectShape, grid.getShape());

      GridCoordSystem gcs = grid.getCoordinateSystem();
      Assert.assertNotNull(gcs);

      CoordinateAxis1DTime reftime = gcs.getRunTimeAxis();
      Assert.assertNotNull(reftime);
      Assert.assertEquals(1, reftime.getSize());
      double[] runtimeValues = new double[]{0.};
      CompareNetcdf2 cn = new CompareNetcdf2();
      assert cn.compareData("reftime", reftime.read(), Array.makeFromJavaArray(runtimeValues), false);

      CoordinateAxis time = gcs.getTimeAxis();
      Assert.assertNotNull(time);
      Assert.assertTrue(time instanceof CoordinateAxis1DTime);
      Assert.assertEquals(4, time.getSize());
      double[] timeValues = new double[]{108.,132.,156,180};  // endpoints
      assert cn.compareData("time", time.read(), Array.makeFromJavaArray(timeValues), false);
    }
  }
}

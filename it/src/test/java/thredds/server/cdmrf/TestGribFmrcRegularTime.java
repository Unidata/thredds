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
package thredds.server.cdmrf;

import org.junit.Assert;
import org.junit.Test;
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

import java.io.IOException;

/**
 * This dataset has regular, not orthogonal times.
 * Read through cdmrf (FeatureDatasetCoverage) and cdmremote (GridDataset)
 *
 * @author caron
 * @since 3/1/2016.
 */
public class TestGribFmrcRegularTime {

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
      Assert.assertTrue(time instanceof FmrcTimeAxis2D);
      Assert.assertEquals(16, time.getNcoords());

      //double[] want = new double[]{108.000000, 132.000000, 156.000000, 180.000000};
      //assert cn.compareData("time", time.getCoordsAsArray(), Array.makeFromJavaArray(want), false);
    }
  }

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

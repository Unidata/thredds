package thredds.server.catalog;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.tools.DataFactory;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;

/**
 * Sanity check on FMRC catalog
 *
 * @author caron
 * @since Sep 24, 2010
 */
@Category(NeedsCdmUnitTest.class)
public class TestTdsFmrc {

  @Test
  public void testFmrcBest() throws IOException {
    String catalog = "/catalog/testNAMfmrc/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);

    Dataset ds = cat.findDatasetByID("testNAMfmrc/NAM_FMRC_best.ncd");
    assert (ds != null) : "cant find dataset 'testNAMfmrc/NAM_FMRC_best.ncd'";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset(ds, null)) {
      assert !dataResult.fatalError;
      assert dataResult.featureDataset != null;

      FeatureDatasetCoverage gds = (FeatureDatasetCoverage) dataResult.featureDataset;
      String gridName = "Total_cloud_cover";
      VariableSimpleIF vs = gds.getDataVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      Assert.assertEquals(1, gds.getCoverageCollections().size());
      CoverageCollection cc = gds.getCoverageCollections().get(0);
      Coverage grid = cc.findCoverage(gridName);
      Assert.assertNotNull(gridName, grid);
      Assert.assertArrayEquals(new int[]{8, 103, 108}, grid.getShape());

      CoverageCoordSys gcs = grid.getCoordSys();
      Assert.assertNotNull(gcs);
      assert null == gcs.getZAxis();

      CoverageCoordAxis time = gcs.getTimeAxis();
      assert time != null;
      Assert.assertNotNull("time axis", time);
      Assert.assertEquals(8, time.getNcoords());
      double[] want = new double[]{3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0};
      CompareNetcdf2 cn = new CompareNetcdf2();
      assert cn.compareData("time", time.getCoordsAsArray(), Array.makeFromJavaArray(want), false);

      Attribute att = gds.findGlobalAttributeIgnoreCase("ncmlAdded");
      assert att != null;
      assert att.isString();
      assert att.getStringValue().equals("goodStuff");

      grid = cc.findCoverage("Visibility");
      att = grid.findAttributeIgnoreCase("ncmlAdded");
      assert att != null;
      assert att.isString();
      assert att.getStringValue().equals("reallyGoodStuff");

      att = grid.findAttributeIgnoreCase("ncmlInnerAdded");
      assert att != null;
      assert att.isString();
      assert att.getStringValue().equals("innerTruth");
    }
  }

  @Test
  @Ignore("FMRC regular, not orthogonal")
  public void testFmrcTwoD() throws IOException {
    String catalog = "/catalog/testNAMfmrc/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);

    Dataset ds = cat.findDatasetByID("testNAMfmrc/NAM_FMRC_fmrc.ncd");
    assert (ds != null) : "cant find dataset 'testNAMfmrc/NAM_FMRC_fmrc.ncd'";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset(ds, null)) {
      Assert.assertTrue(dataResult.errLog.toString(), !dataResult.fatalError);
      Assert.assertNotNull(dataResult.featureDataset);

      FeatureDatasetCoverage gds = (FeatureDatasetCoverage) dataResult.featureDataset;
      String gridName = "Total_cloud_cover";
      VariableSimpleIF vs = gds.getDataVariable(gridName);
      Assert.assertNotNull(gridName, vs);

      Assert.assertEquals(1, gds.getCoverageCollections().size());
      CoverageCollection cc = gds.getCoverageCollections().get(0);
      Coverage grid = cc.findCoverage(gridName);
      Assert.assertNotNull(gridName, grid);

      CoverageCoordSys gcs = grid.getCoordSys();
      Assert.assertNotNull(gcs);
      Assert.assertNull(gcs.getZAxis());

      CoverageCoordAxis time = gcs.getTimeAxis();
      Assert.assertNotNull(time);
      Assert.assertNotNull("time axis", time);
      Assert.assertEquals(8, time.getNcoords());
      double[] want = new double[]{3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0};
      CompareNetcdf2 cn = new CompareNetcdf2();
      assert cn.compareData("time", time.getCoordsAsArray(), Array.makeFromJavaArray(want), false);

      Assert.assertArrayEquals(new int[]{4, 2, 103, 108}, grid.getShape());
    }
  }

  @Test
  public void testFmrcCatRefs() throws IOException {
    String catalog = "/catalog/testNAMfmrc/catalog.xml";
    Catalog cat = TdsLocalCatalog.open(catalog);
    Dataset top = cat.getDatasetsLocal().get(0);

    for (Dataset ds : top.getDatasetsLocal()) {
      if (ds instanceof CatalogRef) {
        CatalogRef catref = (CatalogRef) ds;
        String name =  catref.getName();
        assert name != null : "name is null";
        assert name.length() > 0 : "name is empty";
      }
    }
  }


}

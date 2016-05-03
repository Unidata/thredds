package thredds.server.catalog;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.writer.DataFactory;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
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

  String catalog = "/catalog/testNAMfmrc/catalog.xml";

  @Test
  public void testFmrc() throws IOException {
    Catalog cat = TestTdsLocal.open(catalog);

    Dataset ds = cat.findDatasetByID("testNAMfmrc/NAM_FMRC_best.ncd");
    assert (ds != null) : "cant find dataset 'testNAMfmrc/NAM_FMRC_best.ncd'";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    DataFactory.Result dataResult = fac.openFeatureDataset( ds, null);

    assert dataResult != null;
    assert !dataResult.fatalError;
    assert dataResult.featureDataset != null;

    GridDataset gds = (GridDataset) dataResult.featureDataset;
    GridDatatype grid = gds.findGridDatatype("Total_cloud_cover");
    assert grid != null;
    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert gcs != null;
    assert null == gcs.getVerticalAxis();

    CoordinateAxis1D time = gcs.getTimeAxis1D();
    assert time != null;
    assert time.getSize() == 8;
    double[] want = new double[] {3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0, 24.0};
    CompareNetcdf2 cn = new CompareNetcdf2();
    assert cn.compareData("time", time.read(), Array.factory(want), false);

    Attribute att = gds.findGlobalAttributeIgnoreCase("ncmlAdded");
    assert att != null;
    assert att.isString();
    assert att.getStringValue().equals("goodStuff");

    grid = gds.findGridDatatype("Visibility");
    att = grid.findAttributeIgnoreCase("ncmlAdded");
    assert att != null;
    assert att.isString();
    assert att.getStringValue().equals("reallyGoodStuff");

    att = grid.findAttributeIgnoreCase("ncmlInnerAdded");
    assert att != null;
    assert att.isString();
    assert att.getStringValue().equals("innerTruth");

    gds.close();
  }
}

package thredds.tds;

import junit.framework.TestCase;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvDataset;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.util.CompareNetcdf;
import ucar.nc2.util.CompareNetcdf2;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since Sep 24, 2010
 */
public class TestTdsFmrc extends TestCase {

  public TestTdsFmrc( String name) {
    super(name);
  }

  String catalog = "/catalog/aggorama/catalog.xml";

  public void testFmrc() throws IOException {
    InvCatalogImpl cat = TestTdsLocal.open(catalog);

    InvDataset ds = cat.findDatasetByID("aggorama/NAM_FMRC_best.ncd");
    assert (ds != null) : "cant find dataset 'aggorama/NAM_FMRC_best.ncd'";
    assert ds.getDataType() == FeatureType.GRID;

    ThreddsDataFactory fac = new ThreddsDataFactory();

    ThreddsDataFactory.Result dataResult = fac.openFeatureDataset( ds, null);

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

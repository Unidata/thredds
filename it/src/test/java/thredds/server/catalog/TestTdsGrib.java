/* Copyright */
package thredds.server.catalog;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.Service;
import thredds.client.catalog.tools.DataFactory;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.test.util.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Describe
 *
 * @author caron
 * @since 4/20/2015
 */
@Category(NeedsCdmUnitTest.class)

public class TestTdsGrib {

  @Test
  public void testGribLatest() throws IOException {
    String catalog = "/catalog/grib/NDFD/CONUS_5km/catalog.xml";
    Catalog cat = TestTdsLocal.open(catalog);

    Dataset ds = cat.findDatasetByID("latest.xml");
    assert (ds != null) : "cant find dataset 'dataset=latest.xml'";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset(ds, null)) {

      assert dataResult != null;
      assert !dataResult.fatalError : dataResult.errLog;
      assert dataResult.featureDataset != null;

      GridDataset gds = (GridDataset) dataResult.featureDataset;
      GridDatatype grid = gds.findGridDatatype("Maximum_temperature_Forecast_height_above_ground_12_Hour_Maximum");
      assert grid != null;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert gcs != null;

      CoordinateAxis1D time = gcs.getTimeAxis1D();
      assert time != null;
      assert time.getSize() == 4;
      double[] want = new double[]{108.000000, 132.000000, 156.000000, 180.000000};
      CompareNetcdf2 cn = new CompareNetcdf2();
      assert cn.compareData("time", time.read(), Array.factory(want), false);
    }
  }

  @Test
  public void testGribCatRefs() throws IOException {
    String catalog = "/catalog/grib/NDFD/CONUS_5km/catalog.xml";
    Catalog cat = TestTdsLocal.open(catalog);

    Set<String> ss = new HashSet<>();
    for (Service s : cat.getServices()) {
      assert !ss.contains(s.getName()) : "already has "+s;
      ss.add(s.getName());
    }

    Dataset top = cat.getDatasets().get(0);
    for (Dataset ds : top.getDatasets()) {
      if (ds instanceof CatalogRef) {
        CatalogRef catref = (CatalogRef) ds;
        String name =  catref.getName();
        assert name != null : "name is null";
        assert name.length() > 0 : "name is empty";
      }
    }
  }

}


/* Copyright Unidata */
package thredds.server.dap4;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.client.catalog.*;
import thredds.client.catalog.tools.DataFactory;
import thredds.server.catalog.TdsLocalCatalog;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;

@Category(NeedsCdmUnitTest.class)
public class TestDap4 {
  @Test
  public void testSimpleDap4GridDataset() throws IOException {
    Catalog cat = TdsLocalCatalog.open(null);  // default catalog

    Dataset ds = cat.findDatasetByID("testDap4Dataset");
    assert (ds != null) : "cant find dataset 'testDap4Dataset'";
    assert ds.getFeatureType() == FeatureType.GRID;

    DataFactory fac = new DataFactory();
    try (DataFactory.Result dataResult = fac.openFeatureDataset( ds, null)) {
      Assert.assertNotNull("dataResult", dataResult);
      if (dataResult.fatalError) {
        System.out.printf("fatalError= %s%n", dataResult.errLog);
        assert false;
      }
      Assert.assertNotNull("GridDataset", dataResult.featureDataset);
      Assert.assertEquals("dap4 service", ServiceType.DAP4, dataResult.accessUsed.getService().getType());

      GridDataset gds = (GridDataset) dataResult.featureDataset;
      NetcdfFile nc = gds.getNetcdfFile();
      if (nc != null)
        System.out.printf(" NetcdfFile location = %s%n", nc.getLocation());

      GridDatatype grid = gds.findGridDatatype("Pressure_reduced_to_MSL");
      assert grid != null;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert gcs != null;
      assert null == gcs.getVerticalAxis();

      CoordinateAxis1D time = gcs.getTimeAxis1D();
      Assert.assertNotNull("time axis", time);
      double[] expect = new double[]{0., 6.0, 12.0, 18.0};
      double[] have = time.getCoordValues();
      Assert.assertArrayEquals(expect, have, Misc.maxReletiveError);
    }
  }
}


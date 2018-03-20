/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.grid;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.Range;
import ucar.nc2.Dimension;
import ucar.nc2.NCdumpW;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test grids with 1 dimensional z and/or t dimension */

public class TestGeoGrid extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void testSubset() throws Exception {
    ucar.nc2.dt.grid.GridDataset dataset = GridDataset.open(TestDir.cdmLocalTestDataDir+"rankTest.nc");

    GeoGrid grid = dataset.findGridByName("full4");
    assert null != grid;
    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert null != gcs;
    assert grid.getRank() == 4;

    doRead4(grid);

    GeoGrid grid_section = grid.subset(null, new Range(0,3,2), null, null);
    GridCoordSystem gcs_section = grid_section.getCoordinateSystem();
    CoordinateAxis zaxis = gcs_section.getVerticalAxis();
    assert zaxis.getSize() == 2;

    assert gcs_section.getXHorizAxis().equals( gcs.getXHorizAxis());
    assert gcs_section.getYHorizAxis().equals( gcs.getYHorizAxis());
    assert gcs_section.getTimeAxis().equals( gcs.getTimeAxis());

    Array data = grid_section.readDataSlice(-1, -1, -1, -1);
    logger.debug(NCdumpW.toString( data, "grid_section", null));

    dataset.close();
  }

  private void doRead4( GeoGrid gg) throws IOException {
    Array aa = gg.readDataSlice(-1,-1,-1,-1);
    int[] shape = aa.getShape();
    Index ima = aa.getIndex();
    int[] w = getWeights( gg);

    for (int i=0; i<shape[0]; i++) {
      for (int j=0; j<shape[1]; j++) {
        for (int k=0; k<shape[2]; k++) {
          for (int m=0; m<shape[3]; m++) {
            double got = aa.getDouble( ima.set(i,j,k,m));
            double want = ((double) (i*w[0] + j*w[1] + k*w[2] + m*w[3]));

            assert (got == want)  : "got "+got+ " want "+want;
            // System.out.println("got "+got+ " want "+want);
          }
        }
      }
    }

    System.out.println("ok reading "+gg.getFullName());
  }

  private int[] getWeights( GeoGrid gg) {
    int rank = gg.getRank();
    int[] w = new int[rank];

    for (int n=0; n<rank; n++) {
      Dimension dim = gg.getDimension(n);
      String dimName = dim.getShortName();
      if (dimName.equals("time")) w[n]  = 1000;
      if (dimName.equals("z")) w[n]  = 100;
      if (dimName.equals("y")) w[n]  = 10;
      if (dimName.equals("x")) w[n]  = 1;
    }

    return w;
  }

  public void utestAxisId() throws IOException {
    ucar.nc2.dt.grid.GridDataset dataset = GridDataset.open("C:/data/20100314_v_000000_l_0118800.nc");

    GeoGrid grid = dataset.findGridByName("wind_speed");
    assert null != grid;
    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert null != gcs;
    assert grid.getRank() == 3;

    CoordinateAxis axis = gcs.getXHorizAxis();
    assert axis != null;
    assert axis.getShortName().equals("x") : axis.getShortName();

    axis = gcs.getYHorizAxis();
    assert axis != null;
    assert axis.getShortName().equals("y") : axis.getShortName();

    dataset.close();
  }
}

package ucar.nc2.dataset.grid;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.dataset.*;

import java.io.*;

/** Test grids with 1 dimensional z and/or t dimension */

public class TestGeoGrid extends TestCase {
  private boolean show = false;

  public TestGeoGrid( String name) {
    super(name);
  }

  public void testSubset() throws Exception {
    GridDataset dataset = GridDataset.open(TestGrid.topDir+"rankTest.nc");

    GeoGrid grid = dataset.findGridByName("full4");
    assert null != grid;
    GridCoordSys gcs = grid.getCoordinateSystem();
    assert null != gcs;
    assert grid.getRank() == 4;

    doRead4(grid);

    GeoGrid grid_section = grid.subset(null, new Range(0,3,2), null, null);
    GridCoordSys gcs_section = grid_section.getCoordinateSystem();
    CoordinateAxis zaxis = gcs_section.getVerticalAxis();
    assert zaxis.getSize() == 2;

    assert gcs_section.getXHorizAxis().equals( gcs.getXHorizAxis());
    assert gcs_section.getYHorizAxis().equals( gcs.getYHorizAxis());
    assert gcs_section.getTimeAxis().equals( gcs.getTimeAxis());

    Array data = grid_section.readDataSlice(-1, -1, -1, -1);
    NCdump.printArray( data, "grid_section", System.out,  null);

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

    System.out.println("ok reading "+gg.getName());
  }

  private int[] getWeights( GeoGrid gg) {
    int rank = gg.getRank();
    int[] w = new int[rank];

    for (int n=0; n<rank; n++) {
      Dimension dim = gg.getDimension(n);
      String dimName = dim.getName();
      if (dimName.equals("time")) w[n]  = 1000;
      if (dimName.equals("z")) w[n]  = 100;
      if (dimName.equals("y")) w[n]  = 10;
      if (dimName.equals("x")) w[n]  = 1;
    }

    return w;
  }

}

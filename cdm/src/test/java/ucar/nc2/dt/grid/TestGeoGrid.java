/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.dt.grid;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.util.test.TestDir;

import java.io.*;

/** Test grids with 1 dimensional z and/or t dimension */

public class TestGeoGrid extends TestCase {

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
    NCdumpW.printArray( data, "grid_section", System.out,  null);

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

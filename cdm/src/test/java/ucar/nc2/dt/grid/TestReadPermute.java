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
import ucar.unidata.util.test.TestDir;

import java.io.*;

/** Test in JUnit framework. */

public class TestReadPermute extends TestCase {

  public void testReadPermute() {
    try {

      ucar.nc2.dt.grid.GridDataset dataset = GridDataset.open( TestDir.cdmLocalTestDataDir+"permuteTest.nc");

      doRead4(dataset, "tzyx");
      doRead4(dataset, "tzxy");
      doRead4(dataset, "txyz");
      doRead4(dataset, "tyxz");
      doRead4(dataset, "zyxt");
      doRead4(dataset, "zxyt");
      doRead4(dataset, "xyzt");
      doRead4(dataset, "yxzt");

      doRead3(dataset, "zyx");
      doRead3(dataset, "txy");
      doRead3(dataset, "yxz");
      doRead3(dataset, "xzy");
      doRead3(dataset, "yxt");
      doRead3(dataset, "xyt");
      doRead3(dataset, "yxt");
      doRead3(dataset, "xyz");

      doRead2(dataset, "yx");
      doRead2(dataset, "xy");
      doRead2(dataset, "yz");
      doRead2(dataset, "xz");
      doRead2(dataset, "yt");
      doRead2(dataset, "xt");
      doRead2(dataset, "ty");
      doRead2(dataset, "tx");

            // read 4D volume data
      doRead4Volume(dataset, "tzyx");
      doRead4Volume(dataset, "tzxy");
      doRead4Volume(dataset, "txyz");
      doRead4Volume(dataset, "tyxz");
      doRead4Volume(dataset, "zyxt");
      doRead4Volume(dataset, "zxyt");
      doRead4Volume(dataset, "xyzt");
      doRead4Volume(dataset, "yxzt");

            // read 3D volume data with time
      doRead3Volume(dataset, "txy");
      doRead3Volume(dataset, "yxt");
      doRead3Volume(dataset, "xyt");
      doRead3Volume(dataset, "yxt");

            // read 3D volume data without time
      doRead3XY(dataset, "zyx");
      doRead3XY(dataset, "yxz");
      doRead3XY(dataset, "xzy");
      doRead3XY(dataset, "xyz");

      // all done
      dataset.close();
    } catch (IOException e) {
      assert false : e.getMessage();
    }

    System.out.println( "*****************Test Read done");
  }

  private void doRead4( GridDataset ds, String varName) throws IOException {

    GeoGrid gg = ds.findGridByName( varName);
    testOrder( gg);

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

    System.out.println("ok reading "+varName);
  }

  private void doRead3( GridDataset ds, String varName) throws IOException {
    GeoGrid gg = ds.findGridByName( varName);
    testOrder( gg);

    Array aa = gg.readDataSlice(-1,-1,-1,-1);
    int[] shape = aa.getShape();

    int[] w = getWeights( gg);
    Index ima = aa.getIndex();
    for (int i=0; i<shape[0]; i++) {
      for (int j=0; j<shape[1]; j++) {
        for (int k=0; k<shape[2]; k++) {
            double got = aa.getDouble( ima.set(i,j,k));
            double want = ((double) (i*w[0] + j*w[1] + k*w[2]));
            assert (got == want)  : "got "+got+ " want "+want;
        }
      }
    }

    System.out.println("ok reading "+varName);
  }

  private void doRead2( GridDataset ds, String varName) throws IOException {
    GeoGrid gg = ds.findGridByName( varName);
    if (gg == null) return;
    testOrder( gg);

    Array aa = gg.readDataSlice(-1,-1,-1,-1);
    int[] shape = aa.getShape();

    int[] w = getWeights( gg);
    Index ima = aa.getIndex();
    for (int i=0; i<shape[0]; i++) {
      for (int j=0; j<shape[1]; j++) {
            double got = aa.getDouble( ima.set(i,j));
            double want = ((double) (i*w[0] + j*w[1]));
            assert (got == want)  : "got "+got+ " want "+want;
      }
    }

    System.out.println("ok reading "+varName);
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

  private void testOrder( GeoGrid gg) {
    int current = -1;

    int idx = gg.getTimeDimensionIndex();
    if (idx >= 0) {
      assert idx > current;
      current = idx;
    }
    idx = gg.getZDimensionIndex();
    if (idx >= 0) {
      assert idx > current;
      current = idx;
    }
    idx = gg.getYDimensionIndex();
    if (idx >= 0) {
      assert idx > current;
      current = idx;
    }
    idx = gg.getXDimensionIndex();
    if (idx >= 0) {
      assert idx > current;
      current = idx;
    }

  }

  private void doRead4Volume( GridDataset ds, String varName) throws IOException {

    GeoGrid gg = ds.findGridByName( varName);
    CoordinateAxis1D timeAxis = gg.getCoordinateSystem().getTimeAxis1D();
    for (int t=0; t<timeAxis.getSize(); t++) {
      Array aa = gg.readVolumeData(t);

      int[] shape = aa.getShape();
      Index ima = aa.getIndex();

      for (int i=0; i<shape[0]; i++) {
        for (int j=0; j<shape[1]; j++) {
          for (int k=0; k<shape[2]; k++) {
              double got = aa.getDouble( ima.set(i,j,k));
              double want = ((double) (t*1000 + i*100 + j*10 + k));
              assert (got == want)  : "got "+got+ " want "+want;
          }
        }
      }
    }

    System.out.println("*** ok reading doRead4Volume for "+varName);
  }

  private void doRead3Volume( GridDataset ds, String varName) throws IOException {

    GeoGrid gg = ds.findGridByName( varName);
    CoordinateAxis1D timeAxis = gg.getCoordinateSystem().getTimeAxis1D();
    int[] w = getWeights( gg);

    for (int t=0; t<timeAxis.getSize(); t++) {
      Array aa = gg.readVolumeData(t);

      int[] shape = aa.getShape();
      Index ima = aa.getIndex();

      for (int i=0; i<shape[0]; i++) {
        for (int j=0; j<shape[1]; j++) {
              double got = aa.getDouble( ima.set(i,j));
              double want = ((double) (t*1000 + i*w[1] + j*w[2]));

              assert (got == want)  : "got "+got+ " want "+want;
        }
      }
    }

    System.out.println("*** ok reading doRead3Volume for "+varName);
  }

  private void doRead3XY( GridDataset ds, String varName) throws IOException {

    GeoGrid gg = ds.findGridByName( varName);
    CoordinateAxis1D zAxis = gg.getCoordinateSystem().getVerticalAxis();
    int[] w = getWeights( gg);

    for (int z=0; z<zAxis.getSize(); z++) {
      Array aa = gg.readYXData(0, z);

      int[] shape = aa.getShape();
      Index ima = aa.getIndex();

      for (int i=0; i<shape[0]; i++) {
        for (int j=0; j<shape[1]; j++) {
              double got = aa.getDouble( ima.set(i,j));
              double want = ((double) (z*100 + i*w[1] + j*w[2]));

              assert (got == want)  : "got "+got+ " want "+want;
        }
      }
    }

    System.out.println("*** ok reading doRead3XY for "+varName);
  }


}

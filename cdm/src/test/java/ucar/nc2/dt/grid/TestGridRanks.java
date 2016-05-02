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

/** Test grids with 1 dimensional z and/or t dimension */

public class TestGridRanks extends TestCase {
  private boolean show = false;

  public void testWrite() throws Exception {
    NetcdfFileWriteable ncfile = new NetcdfFileWriteable();
    ncfile.setName(TestDir.cdmLocalTestDataDir+"rankTest.nc");

    // define dimensions
    Dimension xDim = ncfile.addDimension("x", 3);
    Dimension yDim = ncfile.addDimension("y", 5);
    Dimension zDim = ncfile.addDimension("z", 4);
    Dimension tDim = ncfile.addDimension("time", 2);

    Dimension z1Dim = ncfile.addDimension("z1", 1);
    Dimension t1Dim = ncfile.addDimension("time1", 1);

    // define Variables
    ncfile.addVariable("time", double.class, new Dimension[] { tDim } );
    ncfile.addVariableAttribute("time", "units", "secs since 1-1-1 00:00");

    ncfile.addVariable("z", double.class, new Dimension[] { zDim } );
    ncfile.addVariableAttribute("z", "units", "meters");
    ncfile.addVariableAttribute("z", "positive", "up");

    ncfile.addVariable("y", double.class, new Dimension[] { yDim } );
    ncfile.addVariableAttribute("y", "units", "degrees_north");

    ncfile.addVariable("x", double.class, new Dimension[] { xDim } );
    ncfile.addVariableAttribute("x", "units", "degrees_east");

    ncfile.addVariable("time1", double.class, new Dimension[] { t1Dim } );
    ncfile.addVariableAttribute("time1", "units", "secs since 1-1-1 00:00");

    ncfile.addVariable("z1", double.class, new Dimension[] { z1Dim } );
    ncfile.addVariableAttribute("z1", "units", "meters");
    ncfile.addVariableAttribute("z1", "positive", "up");

    // 4 d
    ncfile.addVariable("full4", double.class, new Dimension[] { tDim, zDim, yDim, xDim });
    ncfile.addVariable("withZ1", double.class, new Dimension[] { tDim, z1Dim, xDim, yDim });
    ncfile.addVariable("withT1", double.class, new Dimension[] { t1Dim, zDim, xDim, yDim });
    ncfile.addVariable("withT1Z1", double.class, new Dimension[] { t1Dim, z1Dim, xDim, yDim });

    // 3 d
    ncfile.addVariable("full3", double.class, new Dimension[] { zDim, yDim, xDim });
    ncfile.addVariable("Z1noT", double.class, new Dimension[] { z1Dim, xDim, yDim });
    ncfile.addVariable("T1noZ", double.class, new Dimension[] { t1Dim, xDim, yDim });

    // add global attributes
    ncfile.addGlobalAttribute("Convention", "COARDS");

    // create the file
    ncfile.create();

    // write time data
    int len = tDim.getLength();
    ArrayDouble A = new ArrayDouble.D1(len);
    Index ima = A.getIndex();
    for (int i=0; i<len; i++)
      A.setDouble(ima.set(i), (double) (i*3600));
    int[] origin = new int[1];
    ncfile.write("time", origin, A);

    // write time1 data
    len = t1Dim.getLength();
    A = new ArrayDouble.D1(len);
    ima = A.getIndex();
    for (int i=0; i<len; i++)
      A.setDouble(ima.set(i), (double) (i*3600));
    origin = new int[1];
    ncfile.write("time1", origin, A);

    // write z data
    len = zDim.getLength();
    A = new ArrayDouble.D1(len);
    ima = A.getIndex();
    for (int i=0; i<len; i++)
      A.setDouble(ima.set(i), (double) (i*10));
    ncfile.write("z", origin, A);

    // write z1 data
    len = z1Dim.getLength();
    A = new ArrayDouble.D1(len);
    ima = A.getIndex();
    for (int i=0; i<len; i++)
      A.setDouble(ima.set(i), (double) (i*10));
    ncfile.write("z1", origin, A);

    // write y data
    len = yDim.getLength();
    A = new ArrayDouble.D1(len);
    ima = A.getIndex();
    for (int i=0; i<len; i++)
      A.setDouble(ima.set(i), (double) (i*3));
    ncfile.write("y", origin, A);

    // write x data
    len = xDim.getLength();
    A = new ArrayDouble.D1(len);
    ima = A.getIndex();
    for (int i=0; i<len; i++)
      A.setDouble(ima.set(i), (double) (i*5));
    ncfile.write("x", origin, A);

    // write tzyx data
    doWrite4(ncfile, "full4");
    doWrite4(ncfile, "withZ1");
    doWrite4(ncfile, "withT1");
    doWrite4(ncfile, "withT1Z1");

    doWrite3(ncfile, "full3");
    doWrite3(ncfile, "Z1noT");
    doWrite3(ncfile, "T1noZ");

    if (show) System.out.println( "ncfile = "+ ncfile);

    // all done
    ncfile.close();

    System.out.println( "*****************Test Write done");
  }

  private void doWrite4( NetcdfFileWriteable ncfile, String varName) throws Exception {
    Variable v = ncfile.findVariable( varName);
    int[] w = getWeights( v);

    int[] shape = v.getShape();
    Array aa = Array.factory(v.getDataType().getPrimitiveClassType(), shape);
    Index ima = aa.getIndex();
    for (int i=0; i<shape[0]; i++) {
      for (int j=0; j<shape[1]; j++) {
        for (int k=0; k<shape[2]; k++) {
          for (int m=0; m<shape[3]; m++) {
            aa.setDouble( ima.set(i,j,k,m), (double) (i*w[0] + j*w[1] + k*w[2] + m*w[3]));
          }
        }
      }
    }

    ncfile.write(varName, aa);
  }

  private void doWrite3( NetcdfFileWriteable ncfile, String varName) throws Exception {
    Variable v = ncfile.findVariable( varName);
    int[] w = getWeights( v);

    int[] shape = v.getShape();
    Array aa = Array.factory(v.getDataType().getPrimitiveClassType(), shape);
    Index ima = aa.getIndex();
    for (int i=0; i<shape[0]; i++) {
      for (int j=0; j<shape[1]; j++) {
        for (int k=0; k<shape[2]; k++) {
            aa.setDouble( ima.set(i,j,k), (double) (i*w[0] + j*w[1] + k*w[2]));
        }
      }
    }

    ncfile.write(varName, aa);
  }

  private int[] getWeights( Variable v) {
    int rank = v.getRank();
    int[] w = new int[rank];

    for (int n=0; n<rank; n++) {
      Dimension dim = v.getDimension(n);
      String dimName = dim.getShortName();
      if (dimName.equals("time")) w[n]  = 1000;
      if (dimName.equals("z")) w[n]  = 100;
      if (dimName.equals("y")) w[n]  = 10;
      if (dimName.equals("x")) w[n]  = 1;
    }

    return w;
  }

  //////////////////////////////////////////////
  public void testRead() throws Exception {
      ucar.nc2.dt.grid.GridDataset dataset = GridDataset.open( TestDir.cdmLocalTestDataDir+"rankTest.nc");

      doRead4(dataset, "full4");
      doRead4(dataset, "withZ1");
      doRead4(dataset, "withT1");
      doRead4(dataset, "withT1Z1");

      doRead3(dataset, "full3");
      doRead3(dataset, "Z1noT");
      doRead3(dataset, "T1noZ");

              // read 4D volume data
      doRead4Volume(dataset, "full4");
      doRead4Volume(dataset, "withZ1");
      doRead4Volume(dataset, "withT1");

            // read 3D volume data with time
      doRead3Volume(dataset, "T1noZ");

            // read 3D volume data without time
      doRead3XY(dataset, "full3");
      doRead3XY(dataset, "Z1noT");

      // all done
      dataset.close();

    System.out.println( "*****************Test Read done");
  }

  private void doRead4( GridDataset ds, String varName) throws IOException {
    GeoGrid gg = ds.findGridByName( varName);

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



}

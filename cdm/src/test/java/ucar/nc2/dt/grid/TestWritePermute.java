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
import ucar.unidata.util.test.TestDir;

import java.io.*;

/** Test  write JUnit framework. */

public class TestWritePermute extends TestCase {
  private boolean show = false;

  public void testWritePermute() throws Exception {
    NetcdfFileWriteable ncfile = new NetcdfFileWriteable();
    ncfile.setName(TestDir.cdmLocalTestDataDir+"permuteTest.nc");

    // define dimensions
    Dimension xDim = ncfile.addDimension("x", 3);
    Dimension yDim = ncfile.addDimension("y", 5);
    Dimension zDim = ncfile.addDimension("z", 4);
    Dimension tDim = ncfile.addDimension("time", 2);

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

    ncfile.addVariable("tzyx", double.class, new Dimension[] { tDim, zDim, yDim, xDim });
    ncfile.addVariableAttribute("tzyx", "units", "K");

    ncfile.addVariable("tzxy", double.class, new Dimension[] { tDim, zDim, xDim, yDim });
    ncfile.addVariableAttribute("tzxy", "units", "K");

    ncfile.addVariable("tyxz", double.class, new Dimension[] { tDim, yDim, xDim, zDim });
    ncfile.addVariableAttribute("tyxz", "units", "K");

    ncfile.addVariable("txyz", double.class, new Dimension[] { tDim, xDim, yDim, zDim });
    ncfile.addVariableAttribute("txyz", "units", "K");

    ncfile.addVariable("zyxt", double.class, new Dimension[] { zDim, yDim, xDim, tDim });
    ncfile.addVariableAttribute("zyxt", "units", "K");

    ncfile.addVariable("zxyt", double.class, new Dimension[] { zDim, xDim, yDim, tDim });
    ncfile.addVariableAttribute("zxyt", "units", "K");

    ncfile.addVariable("yxzt", double.class, new Dimension[] { yDim, xDim, zDim, tDim });
    ncfile.addVariableAttribute("yxzt", "units", "K");

    ncfile.addVariable("xyzt", double.class, new Dimension[] { xDim, yDim, zDim, tDim });
    ncfile.addVariableAttribute("xyzt", "units", "K");

    // missing one dimension
    ncfile.addVariable("zyx", double.class, new Dimension[] { zDim, yDim, xDim });
    ncfile.addVariable("txy", double.class, new Dimension[] { tDim, xDim, yDim });
    ncfile.addVariable("yxz", double.class, new Dimension[] { yDim, xDim, zDim });
    ncfile.addVariable("xzy", double.class, new Dimension[] { xDim, zDim, yDim });
    ncfile.addVariable("yxt", double.class, new Dimension[] { yDim, xDim, tDim });
    ncfile.addVariable("xyt", double.class, new Dimension[] { xDim, yDim, tDim });
    ncfile.addVariable("xyz", double.class, new Dimension[] { xDim, yDim, zDim });

    // missing two dimension
    ncfile.addVariable("yx", double.class, new Dimension[] { yDim, xDim });
    ncfile.addVariable("xy", double.class, new Dimension[] { xDim, yDim });
    ncfile.addVariable("yz", double.class, new Dimension[] { yDim, zDim });
    ncfile.addVariable("xz", double.class, new Dimension[] { xDim, zDim });
    ncfile.addVariable("yt", double.class, new Dimension[] { yDim, tDim });
    ncfile.addVariable("xt", double.class, new Dimension[] { xDim, tDim });
    ncfile.addVariable("ty", double.class, new Dimension[] { tDim, yDim });
    ncfile.addVariable("tx", double.class, new Dimension[] { tDim, xDim });

    // add global attributes
    ncfile.addGlobalAttribute("Convention", "COARDS");

    // create the file
    try {
      ncfile.create();
    }  catch (IOException e) {
      System.err.println("ERROR creating file");
      assert(false);
    }

    // write time data
    int len = tDim.getLength();
    ArrayDouble A = new ArrayDouble.D1(len);
    Index ima = A.getIndex();
    for (int i=0; i<len; i++)
      A.setDouble(ima.set(i), (double) (i*3600));
    int[] origin = new int[1];
    try {
      ncfile.write("time", origin, A);
    } catch (IOException e) {
      System.err.println("ERROR writing time");
      assert(false);
    }

    // write z data
    len = zDim.getLength();
    A = new ArrayDouble.D1(len);
    ima = A.getIndex();
    for (int i=0; i<len; i++)
      A.setDouble(ima.set(i), (double) (i*10));
    try {
      ncfile.write("z", origin, A);
    } catch (IOException e) {
      System.err.println("ERROR writing z");
      assert(false);
    }

    // write y data
    len = yDim.getLength();
    A = new ArrayDouble.D1(len);
    ima = A.getIndex();
    for (int i=0; i<len; i++)
      A.setDouble(ima.set(i), (double) (i*3));
    try {
      ncfile.write("y", origin, A);
    } catch (IOException e) {
      System.err.println("ERROR writing y");
      assert(false);
    }

    // write x data
    len = xDim.getLength();
    A = new ArrayDouble.D1(len);
    ima = A.getIndex();
    for (int i=0; i<len; i++)
      A.setDouble(ima.set(i), (double) (i*5));
    try {
      ncfile.write("x", origin, A);
    } catch (IOException e) {
      System.err.println("ERROR writing x");
      assert(false);
    }

    // write tzyx data
    doWrite4(ncfile, "tzyx");
    doWrite4(ncfile, "tzxy");
    doWrite4(ncfile, "txyz");
    doWrite4(ncfile, "tyxz");
    doWrite4(ncfile, "zyxt");
    doWrite4(ncfile, "zxyt");
    doWrite4(ncfile, "xyzt");
    doWrite4(ncfile, "yxzt");

    doWrite3(ncfile, "zyx");
    doWrite3(ncfile, "txy");
    doWrite3(ncfile, "yxz");
    doWrite3(ncfile, "xzy");
    doWrite3(ncfile, "yxt");
    doWrite3(ncfile, "xyt");
    doWrite3(ncfile, "yxt");
    doWrite3(ncfile, "xyz");

    doWrite2(ncfile, "yx");
    doWrite2(ncfile, "xy");
    doWrite2(ncfile, "yz");
    doWrite2(ncfile, "xz");
    doWrite2(ncfile, "yt");
    doWrite2(ncfile, "xt");
    doWrite2(ncfile, "ty");
    doWrite2(ncfile, "tx");

    if (show) System.out.println( "ncfile = "+ ncfile);

    // all done
    try {
      ncfile.close();
    } catch (IOException e) {
      System.err.println("ERROR writing file");
      assert(false);
    }

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


  private void doWrite2( NetcdfFileWriteable ncfile, String varName) throws Exception {
    Variable v = ncfile.findVariable( varName);
    int[] w = getWeights( v);

    int[] shape = v.getShape();
    Array aa = Array.factory(v.getDataType().getPrimitiveClassType(), shape);
    Index ima = aa.getIndex();
    for (int i=0; i<shape[0]; i++) {
      for (int j=0; j<shape[1]; j++) {
            aa.setDouble( ima.set(i,j), (double) (i*w[0] + j*w[1]));
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
}

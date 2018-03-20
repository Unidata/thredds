/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.grid;

import com.google.common.collect.Lists;
import junit.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.unidata.util.test.TestDir;

import java.io.*;
import java.lang.invoke.MethodHandles;

/** Test  write JUnit framework. */

public class TestWritePermute extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private boolean show = false;

  public void testWritePermute() throws Exception {
    NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(TestDir.cdmLocalTestDataDir+"permuteTest.nc", true);

    // define dimensions
    Dimension xDim = ncfile.addDimension("x", 3);
    Dimension yDim = ncfile.addDimension("y", 5);
    Dimension zDim = ncfile.addDimension("z", 4);
    Dimension tDim = ncfile.addDimension("time", 2);

    // define Variables
    ncfile.addVariable("time", DataType.DOUBLE, "time" );
    ncfile.addVariableAttribute("time", "units", "secs since 1-1-1 00:00");

    ncfile.addVariable("z", DataType.DOUBLE, "z" );
    ncfile.addVariableAttribute("z", "units", "meters");
    ncfile.addVariableAttribute("z", "positive", "up");

    ncfile.addVariable("y", DataType.DOUBLE, "y" );
    ncfile.addVariableAttribute("y", "units", "degrees_north");

    ncfile.addVariable("x", DataType.DOUBLE, "x" );
    ncfile.addVariableAttribute("x", "units", "degrees_east");

    ncfile.addVariable("tzyx", DataType.DOUBLE, Lists.newArrayList(tDim, zDim, yDim, xDim));
    ncfile.addVariableAttribute("tzyx", "units", "K");

    ncfile.addVariable("tzxy", DataType.DOUBLE, Lists.newArrayList( tDim, zDim, xDim, yDim));
    ncfile.addVariableAttribute("tzxy", "units", "K");

    ncfile.addVariable("tyxz", DataType.DOUBLE, Lists.newArrayList( tDim, yDim, xDim, zDim));
    ncfile.addVariableAttribute("tyxz", "units", "K");

    ncfile.addVariable("txyz", DataType.DOUBLE, Lists.newArrayList( tDim, xDim, yDim, zDim));
    ncfile.addVariableAttribute("txyz", "units", "K");

    ncfile.addVariable("zyxt", DataType.DOUBLE, Lists.newArrayList( zDim, yDim, xDim, tDim));
    ncfile.addVariableAttribute("zyxt", "units", "K");

    ncfile.addVariable("zxyt", DataType.DOUBLE, Lists.newArrayList( zDim, xDim, yDim, tDim));
    ncfile.addVariableAttribute("zxyt", "units", "K");

    ncfile.addVariable("yxzt", DataType.DOUBLE, Lists.newArrayList( yDim, xDim, zDim, tDim));
    ncfile.addVariableAttribute("yxzt", "units", "K");

    ncfile.addVariable("xyzt", DataType.DOUBLE, Lists.newArrayList( xDim, yDim, zDim, tDim));
    ncfile.addVariableAttribute("xyzt", "units", "K");

    // missing one dimension
    ncfile.addVariable("zyx", DataType.DOUBLE, Lists.newArrayList(zDim, yDim, xDim));
    ncfile.addVariable("txy", DataType.DOUBLE, Lists.newArrayList(tDim, xDim, yDim));
    ncfile.addVariable("yxz", DataType.DOUBLE, Lists.newArrayList(yDim, xDim, zDim));
    ncfile.addVariable("xzy", DataType.DOUBLE, Lists.newArrayList(xDim, zDim, yDim));
    ncfile.addVariable("yxt", DataType.DOUBLE, Lists.newArrayList(yDim, xDim, tDim));
    ncfile.addVariable("xyt", DataType.DOUBLE, Lists.newArrayList(xDim, yDim, tDim));
    ncfile.addVariable("xyz", DataType.DOUBLE, Lists.newArrayList(xDim, yDim, zDim));

    // missing two dimension
    ncfile.addVariable("yx", DataType.DOUBLE, Lists.newArrayList(yDim, xDim));
    ncfile.addVariable("xy", DataType.DOUBLE, Lists.newArrayList(xDim, yDim));
    ncfile.addVariable("yz", DataType.DOUBLE, Lists.newArrayList(yDim, zDim));
    ncfile.addVariable("xz", DataType.DOUBLE, Lists.newArrayList(xDim, zDim));
    ncfile.addVariable("yt", DataType.DOUBLE, Lists.newArrayList(yDim, tDim));
    ncfile.addVariable("xt", DataType.DOUBLE, Lists.newArrayList(xDim, tDim));
    ncfile.addVariable("ty", DataType.DOUBLE, Lists.newArrayList(tDim, yDim));
    ncfile.addVariable("tx", DataType.DOUBLE, Lists.newArrayList(tDim, xDim));

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

  private void doWrite4( NetcdfFileWriter ncfile, String varName) throws Exception {
    Variable v = ncfile.findVariable( varName);
    int[] w = getWeights( v);

    int[] shape = v.getShape();
    Array aa = Array.factory(v.getDataType(), shape);
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

  private void doWrite3( NetcdfFileWriter ncfile, String varName) throws Exception {
    Variable v = ncfile.findVariable( varName);
    int[] w = getWeights( v);

    int[] shape = v.getShape();
    Array aa = Array.factory(v.getDataType(), shape);
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


  private void doWrite2( NetcdfFileWriter ncfile, String varName) throws Exception {
    Variable v = ncfile.findVariable( varName);
    int[] w = getWeights( v);

    int[] shape = v.getShape();
    Array aa = Array.factory(v.getDataType(), shape);
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

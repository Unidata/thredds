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
package ucar.nc2;

import junit.framework.*;
import ucar.ma2.*;
import ucar.nc2.util.CompareNetcdf;

import java.io.*;
import java.util.*;

/**
 * Test nc2 write JUnit framework.
 */

public class TestWrite extends TestCase {
  private boolean show = false;

  public TestWrite(String name) {
    super(name);
  }

  public void testNC3Write() throws IOException {
    String filename = TestLocal.temporaryDataDir + "testWrite.nc";
    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(filename, false);

    // define dimensions
    Dimension latDim = ncfile.addDimension("lat", 64);
    Dimension lonDim = ncfile.addDimension("lon", 128);

    // define Variables
    ArrayList dims = new ArrayList();
    dims.add(latDim);
    dims.add(lonDim);

    ncfile.addVariable("temperature", DataType.DOUBLE, dims);
    ncfile.addVariableAttribute("temperature", "units", "K");

    Array data = Array.factory(int.class, new int[]{3}, new int[]{1, 2, 3});
    ncfile.addVariableAttribute("temperature", "scale", data);
    ncfile.addVariableAttribute("temperature", "versionD", new Double(1.2));
    ncfile.addVariableAttribute("temperature", "versionF", new Float(1.2));
    ncfile.addVariableAttribute("temperature", "versionI", new Integer(1));
    ncfile.addVariableAttribute("temperature", "versionS", new Short((short) 2));
    ncfile.addVariableAttribute("temperature", "versionB", new Byte((byte) 3));

    ncfile.addVariableAttribute("temperature", "versionString", "1.2");

    // add string-valued variables
    Dimension svar_len = ncfile.addDimension("svar_len", 80);
    dims = new ArrayList();
    dims.add(svar_len);
    ncfile.addVariable("svar", DataType.CHAR, dims);
    ncfile.addVariable("svar2", DataType.CHAR, dims);

    // string array
    Dimension names = ncfile.addDimension("names", 3);
    ArrayList dima = new ArrayList();
    dima.add(names);
    dima.add(svar_len);

    ncfile.addVariable("names", DataType.CHAR, dima);
    ncfile.addVariable("names2", DataType.CHAR, dima);

    // how about a scalar variable?
    ncfile.addVariable("scalar", DataType.DOUBLE, new ArrayList());

    // signed byte
    ncfile.addVariable("bvar", DataType.BYTE, "lat");

    // add global attributes
    ncfile.addGlobalAttribute("yo", "face");
    ncfile.addGlobalAttribute("versionD", new Double(1.2));
    ncfile.addGlobalAttribute("versionF", new Float(1.2));
    ncfile.addGlobalAttribute("versionI", new Integer(1));
    ncfile.addGlobalAttribute("versionS", new Short((short) 2));
    ncfile.addGlobalAttribute("versionB", new Byte((byte) 3));

    // test some errors
    try {
      Array bad = Array.factory(ArrayList.class, new int[]{1});
      ncfile.addGlobalAttribute("versionC", bad);
      assert (false);
    } catch (IllegalArgumentException e) {
      assert (true);
    }

    // create the file
    try {
      ncfile.create();
    } catch (IOException e) {
      System.err.println("ERROR creating file " + ncfile.getLocation() + "\n" + e);
      assert (false);
    }

    // write some data
    ArrayDouble A = new ArrayDouble.D2(latDim.getLength(), lonDim.getLength());
    int i, j;
    Index ima = A.getIndex();
    // write
    for (i = 0; i < latDim.getLength(); i++) {
      for (j = 0; j < lonDim.getLength(); j++) {
        A.setDouble(ima.set(i, j), (double) (i * 1000000 + j * 1000));
      }
    }

    int[] origin = new int[2];
    try {
      ncfile.write("temperature", origin, A);
    } catch (IOException e) {
      System.err.println("ERROR writing file");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }


    // write char variable
    int[] origin1 = new int[1];
    ArrayChar ac = new ArrayChar.D1(svar_len.getLength());
    ima = ac.getIndex();
    String val = "Testing 1-2-3";
    for (j = 0; j < val.length(); j++)
      ac.setChar(ima.set(j), val.charAt(j));

    try {
      ncfile.write("svar", origin1, ac);
    } catch (IOException e) {
      System.err.println("ERROR writing Achar");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }

    // write char variable
    ArrayByte.D1 barray = new ArrayByte.D1(latDim.getLength());
    int start = -latDim.getLength() / 2;
    for (j = 0; j < latDim.getLength(); j++)
      barray.setByte(j, (byte) (start + j));

    try {
      ncfile.write("bvar", barray);
    } catch (IOException e) {
      System.err.println("ERROR writing bvar");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }

    // write char variable as String
    try {
      ArrayChar ac2 = new ArrayChar.D1(svar_len.getLength());
      ac2.setString("Two pairs of ladies stockings!");
      ncfile.write("svar2", origin1, ac2);
    } catch (IOException e) {
      System.err.println("ERROR writing Achar2");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }


    // write String array
    try {
      ArrayChar ac2 = new ArrayChar.D2(names.getLength(), svar_len.getLength());
      ima = ac2.getIndex();
      ac2.setString(ima.set(0), "No pairs of ladies stockings!");
      ac2.setString(ima.set(1), "One pair of ladies stockings!");
      ac2.setString(ima.set(2), "Two pairs of ladies stockings!");
      ncfile.write("names", origin, ac2);
    } catch (IOException e) {
      System.err.println("ERROR writing Achar3");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }


    // write String array
    try {
      ArrayChar ac2 = new ArrayChar.D2(names.getLength(), svar_len.getLength());
      ac2.setString(0, "0 pairs of ladies stockings!");
      ac2.setString(1, "1 pair of ladies stockings!");
      ac2.setString(2, "2 pairs of ladies stockings!");
      ncfile.write("names2", origin, ac2);
    } catch (IOException e) {
      System.err.println("ERROR writing Achar4");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }

    // write scalar data
    // write String array
    try {
      ArrayDouble.D0 datas = new ArrayDouble.D0();
      datas.set(222.333);
      ncfile.write("scalar", datas);
    } catch (IOException e) {
      System.err.println("ERROR writing scalar");
      assert (false);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      assert (false);
    }

    try {
      ncfile.flush();
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }

    if (show) System.out.println("ncfile = " + ncfile);


    //////////////////////////////////////////////////////////////////////
    // test reading without closing the file

    // read entire array
    Variable temp = ncfile.findVariable("temperature");
    assert (null != temp);

    Array tA = temp.read();
    assert (tA.getRank() == 2);

    ima = tA.getIndex();
    int[] shape = tA.getShape();

    for (i = 0; i < shape[0]; i++) {
      for (j = 0; j < shape[1]; j++) {
        assert (tA.getDouble(ima.set(i, j)) == (double) (i * 1000000 + j * 1000));
      }
    }

    // read part of array
    int[] origin2 = new int[2];
    int[] shape2 = new int[2];
    shape2[0] = 1;
    shape2[1] = temp.getShape()[1];
    try {
      tA = temp.read(origin2, shape2);
    } catch (InvalidRangeException e) {
      System.err.println("ERROR reading file " + e);
      assert (false);
      return;
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      assert (false);
      return;
    }
    assert (tA.getRank() == 2);

    for (j = 0; j < shape2[1]; j++) {
      assert (tA.getDouble(ima.set(0, j)) == (double) (j * 1000));
    }

    // rank reduction
    Array Areduce = tA.reduce();
    Index ima2 = Areduce.getIndex();
    assert (Areduce.getRank() == 1);

    for (j = 0; j < shape2[1]; j++) {
      assert (Areduce.getDouble(ima2.set(j)) == (double) (j * 1000));
    }

    // read char variable
    Variable c = null;
    assert (null != (c = ncfile.findVariable("svar")));
    try {
      tA = c.read();
    } catch (IOException e) {
      assert (false);
    }
    assert (tA instanceof ArrayChar);
    ArrayChar achar = (ArrayChar) tA;
    String sval = achar.getString(ac.getIndex());
    assert sval.equals("Testing 1-2-3") : sval;
    //System.out.println( "val = "+ val);

    // read char variable 2
    Variable c2 = null;
    assert (null != (c2 = ncfile.findVariable("svar2")));
    try {
      tA = c2.read();
    } catch (IOException e) {
      assert (false);
    }
    assert (tA instanceof ArrayChar);
    ArrayChar ac2 = (ArrayChar) tA;
    assert (ac2.getString().equals("Two pairs of ladies stockings!"));

    // read String Array
    Variable c3 = null;
    assert (null != (c3 = ncfile.findVariable("names")));
    try {
      tA = c3.read();
    } catch (IOException e) {
      assert (false);
    }
    assert (tA instanceof ArrayChar);
    ArrayChar ac3 = (ArrayChar) tA;
    ima = ac3.getIndex();

    assert (ac3.getString(ima.set(0)).equals("No pairs of ladies stockings!"));
    assert (ac3.getString(ima.set(1)).equals("One pair of ladies stockings!"));
    assert (ac3.getString(ima.set(2)).equals("Two pairs of ladies stockings!"));

    // read String Array - 2
    Variable c4 = null;
    assert (null != (c4 = ncfile.findVariable("names2")));
    try {
      tA = c4.read();
    } catch (IOException e) {
      assert (false);
    }
    assert (tA instanceof ArrayChar);
    ArrayChar ac4 = (ArrayChar) tA;
    ima = ac4.getIndex();

    assert (ac4.getString(0).equals("0 pairs of ladies stockings!"));
    assert (ac4.getString(1).equals("1 pair of ladies stockings!"));
    assert (ac4.getString(2).equals("2 pairs of ladies stockings!"));

    /////////////////////////////////////////////////////////////////////
    // all done
    try {
      ncfile.close();
    } catch (IOException e) {
      e.printStackTrace();
      assert (false);
    }

    System.out.println("*****************Test Write done on " + filename);
  }

  public void utestDennisCode() throws IOException, InvalidRangeException {
    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew("location");
    ArrayInt data = new ArrayInt(new int[]{4});
    Index ima = data.getIndex();
    data.setInt(ima.set(0), (int) 1);
    data.setInt(ima.set(1), (int) 2);
    data.setInt(ima.set(2), (int) 3);
    data.setInt(ima.set(3), (int) 4);
    int[] origin = new int[]{0};
    ncfile.write("v", origin, data);
  }

  public void utestDennisCode2() throws IOException, InvalidRangeException {
    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew("location");
    ArrayInt.D1 data = new ArrayInt.D1(4);
    data.setInt(0, 1);
    data.setInt(1, 2);
    data.setInt(2, 3);
    data.setInt(3, 4);
    ncfile.write("v", data);
  }

  // fix for bug introduced 2/9/10, reported by Christian Ward-Garrison cwardgar@usgs.gov
  public void testRecordSizeBug() throws IOException, InvalidRangeException {
    String filename = TestLocal.temporaryDataDir + "foo.nc";
    //File tempFile = File.createTempFile("foo", "nc");
    //tempFile.deleteOnExit();
    NetcdfFileWriteable ncWriteable = NetcdfFileWriteable.createNew(filename, false);
    Array result1;

    try {
      Dimension timeDim = ncWriteable.addUnlimitedDimension("time");
      ncWriteable.addVariable("time", DataType.INT, new Dimension[]{timeDim});
      ncWriteable.addVariableAttribute("time", "units", "hours since 1990-01-01");
      ncWriteable.create();

      Array timeData = Array.factory(DataType.INT, new int[]{1});
      int[] time_origin = new int[]{0};

      for (int time = 0; time < 10; time++) {
        timeData.setInt(timeData.getIndex(), time * 12);
        time_origin[0] = time;
        ncWriteable.write("time", time_origin, timeData);
      }

      // Prints "0 12 24 36 48 60 72 84 96 108", the expected result.
      result1 = ncWriteable.readSection("time");
      System.out.println(result1);
    } finally {
      ncWriteable.close();
    }

    NetcdfFile ncFile = NetcdfFile.open(filename);
    try {
      // Prints "0 0 12 0 24 0 36 0 48 0".
      Array result2 = ncFile.readSection("time");
      System.out.println(result2);
      CompareNetcdf.compareData(result1, result2);
    } finally {
      ncFile.close();
    }
  }

}

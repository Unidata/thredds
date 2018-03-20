/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import junit.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.unidata.util.test.UtilsMa2Test;
import ucar.unidata.util.test.TestDir;

import java.io.*;
import java.lang.invoke.MethodHandles;

/** Test reading variable data */

public class TestReadSlice extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void testReadSlice1() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc");

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("temperature")));
    int[] shape = temp.getShape();

    Variable tempSlice = temp.slice(0, 12);

    // read array section
    Array Asection;
    try {
      Asection = tempSlice.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      e.printStackTrace();
      assert(false);
      return;
    }
    assert Asection.getRank() == 1;
    assert shape[1] == Asection.getShape()[0];

    // read entire array
    Array A;
    try {
      A = temp.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      assert(false);
      return;
    }
    assert (A.getRank() == 2);

    // compare
    Array Asection2 = A.slice( 0, 12);
    assert (Asection2.getRank() == 1);

    UtilsMa2Test.testEquals(Asection, Asection2);

    ncfile.close();
    System.out.println( "*** testReadSlice1 done");
  }

  public void testReadSlice2() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc");

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("temperature")));
    int[] shape = temp.getShape();

    Variable tempSlice = temp.slice(1, 55);

    // read array section
    Array Asection;
    try {
      Asection = tempSlice.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      e.printStackTrace();
      assert(false);
      return;
    }
    assert Asection.getRank() == 1;
    assert shape[0] == Asection.getShape()[0];

    // read entire array
    Array A;
    try {
      A = temp.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      assert(false);
      return;
    }
    assert (A.getRank() == 2);

    // compare
    Array Asection2 = A.slice( 1, 55);
    assert (Asection2.getRank() == 1);

    UtilsMa2Test.testEquals(Asection, Asection2);

    ncfile.close();
    System.out.println( "*** testReadSlice2 done");
  }

  public void testReadSliceCompose() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc");
    System.out.printf("Open %s%n", ncfile.location);

    Variable temp = ncfile.findVariable("temperature");
    assert temp != null;
    int[] shape = temp.getShape();
    assert shape[0] == 64;
    assert shape[1] == 128;

    Variable tempSlice = temp.slice(1, 55); // fix dimension 1, eg temp(*,55)
    Variable slice2 = tempSlice.slice(0, 12); // fix dimension 0, eg temp(12,55)
    assert slice2.getRank() == 0; // contract is that rank is reduced by one for each slice

    // read array section
    Array Asection;
    try {
      Asection = slice2.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      e.printStackTrace();
      assert(false);
      return;
    }
    //assert Asection.getRank() == 0;  // this is returning a rank1 (length 1)

    // read entire array
    Array A;
    try {
      A = temp.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      assert(false);
      return;
    }
    assert (A.getRank() == 2);

    // compare
    Array data = A.slice( 1, 55);
    data = data.slice( 0, 12);
    assert (data.getRank() == 0);

    UtilsMa2Test.testEquals(Asection, data);

    ncfile.close();
    System.out.println( "*** testReadSliceCompose done");
  }

}

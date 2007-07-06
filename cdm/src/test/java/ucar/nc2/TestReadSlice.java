package ucar.nc2;

import junit.framework.*;
import ucar.ma2.*;

import java.io.*;
import java.util.ArrayList;

/** Test reading variable data */

public class TestReadSlice extends TestCase {

  public TestReadSlice( String name) {
    super(name);
  }

  public void testReadSlice1() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestLocalNC2.openFile("testWrite.nc");

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

    TestMA2.testEquals(Asection, Asection2 );

    ncfile.close();
    System.out.println( "*** testReadSlice1 done");
  }

  public void testReadSlice2() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestLocalNC2.openFile("testWrite.nc");

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

    TestMA2.testEquals(Asection, Asection2 );

    ncfile.close();
    System.out.println( "*** testReadSlice2 done");
  }

  public void testReadSliceCompose() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestLocalNC2.openFile("testWrite.nc");

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("temperature")));
    int[] shape = temp.getShape();

    Variable tempSlice = temp.slice(1, 55);
    Variable slice2 = tempSlice.slice(0, 12);

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
    assert Asection.getRank() == 0;

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

    TestMA2.testEquals(Asection, data );

    ncfile.close();
    System.out.println( "*** testReadSliceCompose done");
  }

}

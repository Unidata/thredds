package ucar.nc2;

import junit.framework.*;
import ucar.ma2.*;

import java.io.*;
import java.util.ArrayList;

/** Test reading variable data */

public class TestReadSection extends TestCase {

  public TestReadSection( String name) {
    super(name);
  }

  public void testReadVariableSection() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestNC2.openFile("testWrite.nc");

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("temperature")));

    int[] origin = {3,6};
    int[] shape = {12,17};

    Variable tempSection = temp.section(Range.factory(origin, shape));

    // read array section
    Array Asection;
    try {
      Asection = tempSection.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      e.printStackTrace();
      assert(false);
      return;
    }
    assert Asection.getRank() == 2;
    assert shape[0] == Asection.getShape()[0];
    assert shape[1] == Asection.getShape()[1];

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
    Array Asection2 = A.section( origin, shape, null);
    assert (Asection2.getRank() == 2);
    assert (shape[0] == Asection2.getShape()[0]);
    assert (shape[1] == Asection2.getShape()[1]);

    IndexIterator s1 = Asection.getIndexIterator();
    IndexIterator s2 = Asection2.getIndexIterator();
    int count = 0;
    while (s1.hasNext()) {
      double d1 = s1.getDoubleNext();
      double d2 = s2.getDoubleNext();
      assert TestAll.closeEnough( d1, d2) : count+" "+d1 +" != "+d2;
      count++;
    }

    ncfile.close();
    System.out.println( "*** testReadVariableSection done");
  }


  public void testReadVariableSection2() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestNC2.openFile("testWrite.nc");

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("temperature")));

    ArrayList ranges = new ArrayList();
    Range r0 = new Range(3,14);
    Range r1 = new Range(6,22);
    ranges.add( r0);
    ranges.add( r1);

    Variable tempSection = temp.section(ranges);
    assert tempSection.getRank() == 2;
    int[] vshape = tempSection.getShape();
    assert r0.length() == vshape[0];
    assert r1.length() == vshape[1];

    // read array section
    Array Asection;
    try {
      Asection = tempSection.read();
    } catch (IOException e) {
      System.err.println("ERROR reading file");
      e.printStackTrace();
      assert(false);
      return;
    }
    assert Asection.getRank() == 2;
    assert r0.length() == Asection.getShape()[0];
    assert r1.length() == Asection.getShape()[1];

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
    Array Asection2 = A.section( ranges);
    assert (Asection2.getRank() == 2);
    assert (r0.length() == Asection2.getShape()[0]);
    assert (r1.length() == Asection2.getShape()[1]);

    IndexIterator s1 = Asection.getIndexIterator();
    IndexIterator s2 = Asection2.getIndexIterator();
    int count = 0;
    while (s1.hasNext()) {
      double d1 = s1.getDoubleNext();
      double d2 = s2.getDoubleNext();
      assert TestAll.closeEnough( d1, d2) : count+" "+d1 +" != "+d2;
      count++;
    }

    ncfile.close();
    System.out.println( "*** testReadVariableSection2 done");
  }

}

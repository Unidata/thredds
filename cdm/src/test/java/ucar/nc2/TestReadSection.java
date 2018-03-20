/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import junit.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

/** Test reading variable data */

public class TestReadSection extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestReadSection( String name) {
    super(name);
  }

  public void testReadVariableSection() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc");

    Variable temp = ncfile.findVariable("temperature");
    assert(null != temp);

    int[] origin = {3,6};
    int[] shape = {12,17};

    Variable tempSection = temp.section(new Section(origin, shape));

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
      assert Misc.nearlyEquals(d1, d2) : count+" "+d1 +" != "+d2;
      count++;
    }

    ncfile.close();
    System.out.println( "*** testReadVariableSection done");
  }


  public void testReadVariableSection2() throws InvalidRangeException, IOException {
    NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc");

    Variable temp = null;
    assert(null != (temp = ncfile.findVariable("temperature")));

    ArrayList<Range> ranges = new ArrayList<Range>();
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
      assert Misc.nearlyEquals( d1, d2) : count+" "+d1 +" != "+d2;
      count++;
    }

    ncfile.close();
    System.out.println( "*** testReadVariableSection2 done");
  }

}

/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.TestDir;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

/** Test reading variable data */
public class TestReadSection {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testReadVariableSection() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc")) {
      Variable temp = ncfile.findVariable("temperature");
      assert(null != temp);
    
      int[] origin = {3,6};
      int[] shape = {12,17};
    
      Variable tempSection = temp.section(new Section(origin, shape));
    
      // read array section
      Array Asection = tempSection.read();
      assert Asection.getRank() == 2;
      assert shape[0] == Asection.getShape()[0];
      assert shape[1] == Asection.getShape()[1];
    
      // read entire array
      Array A = temp.read();
      assert (A.getRank() == 2);
    
      // compare
      Array Asection2 = A.section( origin, shape, null);
      assert (Asection2.getRank() == 2);
      assert (shape[0] == Asection2.getShape()[0]);
      assert (shape[1] == Asection2.getShape()[1]);
      
      Assert2.assertArrayNearlyEquals((double[]) Asection.copyTo1DJavaArray(),
              (double[]) Asection2.copyTo1DJavaArray());
    }
  }

  @Test
  public void testReadVariableSection2() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc")) {
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
      Array Asection = tempSection.read();
      assert Asection.getRank() == 2;
      assert r0.length() == Asection.getShape()[0];
      assert r1.length() == Asection.getShape()[1];
    
      // read entire array
      Array A = temp.read();
      assert (A.getRank() == 2);
    
      // compare
      Array Asection2 = A.section( ranges);
      assert (Asection2.getRank() == 2);
      assert (r0.length() == Asection2.getShape()[0]);
      assert (r1.length() == Asection2.getShape()[1]);
      
      Assert2.assertArrayNearlyEquals((double[]) Asection.copyTo1DJavaArray(),
              (double[]) Asection2.copyTo1DJavaArray());
    }
  }
}

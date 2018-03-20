/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import java.io.*;
import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;

/**
 * Static utilities for testing
 *
 * @author Russ Rew
 */

public class TestUtils  {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** read all data, make sure variable metadata matches the array */
  static public void testReadData( NetcdfFile ncfile, boolean showStatus) {
    try {
      for (Variable v  : ncfile.getVariables()) {
        testVarMatchesData(v, showStatus);
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
      assert (false);
    }

    if (showStatus) System.out.println( "**** testReadData done on "+ncfile.getLocation());
  }

  static public void testVarMatchesData( Variable v, boolean showStatus) throws IOException {
    Array data = v.read();
    assert data.getSize() == v.getSize();
    assert data.getElementType() == v.getDataType().getPrimitiveClassType();

    assert data.getRank() == v.getRank();
    int[] dataShape = data.getShape();
    int[] varShape = v.getShape();
    for (int i=0; i<data.getRank(); i++)
      assert dataShape[i] == varShape[i];

    if (showStatus) System.out.println( "**** testReadData done on "+v.getFullName());
  }

  static public boolean close( double d1, double d2) {
    if (Double.isNaN(d1))
      return Double.isNaN(d2);

    if (d1 != 0.0)
      return Math.abs((d1-d2)/d1) < 1.0e-9;
    else
      return Math.abs(d1-d2) < 1.0e-9;
  }

  static public boolean close( float d1, float d2) {
    if (Float.isNaN(d1))
      return Float.isNaN(d2);

    if (d1 != 0.0)
      return Math.abs((d1-d2)/d1) < 1.0e-5;
    else
      return Math.abs(d1-d2) < 1.0e-5;
  }
}

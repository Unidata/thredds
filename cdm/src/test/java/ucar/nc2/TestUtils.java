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

import java.io.*;
import java.util.*;

import ucar.ma2.Array;

/**
 * Static utililities for testing
 *
 * @author Russ Rew
 */

public class TestUtils  {


  static public void NCdump( String filename) {
    try {
      NCdump.print(filename, System.out, false, true, false, false, null, null);
      NCdump.printNcML(filename, System.out);
    } catch (IOException ioe) {
      ioe.printStackTrace();
      assert (false);
    }

    System.out.println( "**** NCdump done");
  }

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

    if (showStatus) System.out.println( "**** testReadData done on "+v.getName());
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

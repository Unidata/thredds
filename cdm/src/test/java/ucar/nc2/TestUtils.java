package ucar.nc2;

import java.io.*;
import java.util.*;

import ucar.ma2.Array;

/**
 * Static utililities for testing
 *
 * @author Russ Rew
 * @version $Id$ */

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

  static public void testReadData( NetcdfFile ncfile, boolean showStatus) {
    try {
      List list = ncfile.getVariables();
      for (int i = 0; i < list.size(); i++) {
        Variable v = (Variable) list.get(i);
        testVarMatchesData( v, showStatus);
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

package ucar.nc2.ncml;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.TestNcML;

import java.io.IOException;

/** Test netcdf dataset in the JUnit framework. */

public class TestNcMLDoubleNested extends TestCase {

  public TestNcMLDoubleNested( String name) {
    super(name);
  }

  static NetcdfFile ncfile = null;
  static String filename = "file:./"+TestNcML.topDir + "doubleNested2.xml";

  public void setUp() {
    if (ncfile != null) return;

    try {
      ncfile = new NcMLReader().readNcML(filename, null);
    } catch (java.net.MalformedURLException e) {
      System.out.println("bad URL error = "+e);
    } catch (IOException e) {
      System.out.println("IO error = "+e);
      e.printStackTrace();
    }
  }

  public void testCoords() {
    System.out.println("DoubleNested = \n"+ncfile);

    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getName().equals("lat");
    assert latDim.getLength() == 3;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("lon");
    assert null != lonDim;
    assert lonDim.getName().equals("lon");
    assert lonDim.getLength() == 4;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getName().equals("time");
    assert timeDim.getLength() == 59;
    assert !timeDim.isUnlimited();
  }

  public void testAggCoordVar() {

    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 59;
    assert time.getShape()[0] == 59;
    assert time.getDataType() == DataType.INT;

    assert time.getDimension(0).equals(ncfile.findDimension("time"));

    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == 59;
      assert data.getShape()[0] == 59;
      assert data.getElementType() == int.class;

      int count = 0;
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext())
        assert dataI.getIntNext() == count++;

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

  }

  public void testReadT() {

    Variable v = ncfile.findVariable("T");
    assert null != v;
    assert v.getName().equals("T");
    assert v.getRank() == 3;
    assert v.getSize() == 708 : v.getSize();
    assert v.getShape()[0] == 59;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getDataType() == DataType.DOUBLE;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0).equals(ncfile.findDimension("time"));
    assert v.getDimension(1).equals(ncfile.findDimension("lat"));
    assert v.getDimension(2).equals(ncfile.findDimension("lon"));

    try {
      Array data = v.read();
      assert data.getRank() == 3;
      assert data.getSize() == 708;
      assert data.getShape()[0] == 59;
      assert data.getShape()[1] == 3;
      assert data.getShape()[2] == 4;
      assert data.getElementType() == double.class;

      int [] shape = data.getShape();
      Index tIndex = data.getIndex();
      for (int i=0; i<shape[0]; i++)
       for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++) {
          double val = data.getDouble( tIndex.set(i, j, k));
          // System.out.println(" "+val);
          assert close(val, 100*i + 10*j + k) : val;
        }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  public void testReadP() {

    Variable v = ncfile.findVariable("P");
    assert null != v;
    assert v.getName().equals("P");
    assert v.getRank() == 3;
    assert v.getSize() == 708 : v.getSize();
    assert v.getShape()[0] == 59;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getDataType() == DataType.DOUBLE;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0).equals(ncfile.findDimension("time"));
    assert v.getDimension(1).equals(ncfile.findDimension("lat"));
    assert v.getDimension(2).equals(ncfile.findDimension("lon"));

    try {
      Array data = v.read();
      assert data.getRank() == 3;
      assert data.getSize() == 708;
      assert data.getShape()[0] == 59;
      assert data.getShape()[1] == 3;
      assert data.getShape()[2] == 4;
      assert data.getElementType() == double.class;

      int [] shape = data.getShape();
      Index tIndex = data.getIndex();
      for (int i=0; i<shape[0]; i++)
       for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++) {
          double val = data.getDouble( tIndex.set(i, j, k));
          // System.out.println(" "+val);
          assert close(val, 200*i + 20*j + 2*k) : val;
        }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  public void readSliceT(int[] origin, int[] shape) {

    Variable v = ncfile.findVariable("T");

    try {
      Array data = v.read(origin, shape);
      assert data.getRank() == 3;
      assert data.getSize() == shape[0] * shape[1] * shape[2];
      assert data.getShape()[0] == shape[0] : data.getShape()[0] +" "+shape[0];
      assert data.getShape()[1] == shape[1];
      assert data.getShape()[2] == shape[2];
      assert data.getElementType() == double.class;

      Index tIndex = data.getIndex();
      for (int i=0; i<shape[0]; i++)
       for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++) {
          double val = data.getDouble( tIndex.set(i, j, k));
          //System.out.println(" "+val);
          assert close(val, 100*(i+origin[0]) + 10*j + k) : val;
        }

    } catch (InvalidRangeException io) {
      assert false;
    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  public void readSliceP(int[] origin, int[] shape) {

    Variable v = ncfile.findVariable("P");

    try {
      Array data = v.read(origin, shape);
      assert data.getRank() == 3;
      assert data.getSize() == shape[0] * shape[1] * shape[2];
      assert data.getShape()[0] == shape[0] : data.getShape()[0] +" "+shape[0];
      assert data.getShape()[1] == shape[1];
      assert data.getShape()[2] == shape[2];
      assert data.getElementType() == double.class;

      Index tIndex = data.getIndex();
      for (int i=0; i<shape[0]; i++)
       for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++) {
          double val = data.getDouble( tIndex.set(i, j, k));
          //System.out.println(" "+val);
          assert close(val, 200*(i+origin[0]) + 20*j + 2*k) : val;
        }

    } catch (InvalidRangeException io) {
      assert false;
    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  public void testReadSlice() {

    readSliceT( new int[] {0, 0, 0}, new int[] {59, 3, 4} );
    readSliceT( new int[] {0, 0, 0}, new int[] {2, 3, 2} );
    readSliceT( new int[] {25, 0, 0}, new int[] {10, 3, 4} );
    readSliceT( new int[] {44, 0, 0}, new int[] {10, 2, 3} );

    readSliceP( new int[] {0, 0, 0}, new int[] {59, 3, 4} );
    readSliceP( new int[] {0, 0, 0}, new int[] {2, 3, 2} );
    readSliceP( new int[] {25, 0, 0}, new int[] {10, 3, 4} );
    readSliceP( new int[] {44, 0, 0}, new int[] {10, 2, 3} );
   }

  boolean close( double d1, double d2) {
    //System.out.println(d1+" "+d2);
    if (d1 != 0.0)
      return Math.abs((d1-d2)/d1) < 1.0e-5;
    else
      return Math.abs(d1-d2) < 1.0e-5;
  }

}

package ucar.nc2.ncml_old;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.ncml_old.NcMLReader;
import ucar.nc2.ncml_old.TestNcML;

import java.io.IOException;

/** Test netcdf dataset in the JUnit framework. */

public class TestNcmlUnionSimple extends TestCase {

  public TestNcmlUnionSimple( String name) {
    super(name);
  }

  static NetcdfFile ncfile = null;

  public void setUp() {
    if (ncfile != null) return;
    String filename = "file:./"+TestNcML.topDir + "aggUnionSimple.xml";

    try {
      ncfile = new NcMLReader().readNcML(filename, null);
    } catch (java.net.MalformedURLException e) {
      System.out.println("bad URL error = "+e);
    } catch (IOException e) {
      System.out.println("IO error = "+e);
      e.printStackTrace();
      assert false;
    }
  }
 

   public void tearDown() throws IOException {
      if (ncfile != null) ncfile.close();
      ncfile = null;
    }

  public void testRead() {
    System.out.println("ncfile = "+ncfile);
    ucar.nc2.TestUtils.testReadData( ncfile, true);
  }

  public void testStructure() {
    System.out.println("TestNested = \n"+ncfile);

    Attribute att = ncfile.findGlobalAttribute("title");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("Union cldc and lflx");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getName().equals("lat");
    assert latDim.getLength() == 21;
    assert !latDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getName().equals("time");
    assert timeDim.getLength() == 456;
    assert !timeDim.isUnlimited();
  }

  public void testReadCoordvar() {

    Variable lat = ncfile.findVariable("lat");
    assert null != lat;
    assert lat.getName().equals("lat");
    assert lat.getRank() == 1;
    assert lat.getSize() == 21;
    assert lat.getShape()[0] == 21;
    assert lat.getDataType() == DataType.FLOAT;

    assert lat.isCoordinateVariable();
    assert !lat.isUnlimited();

    assert lat.getDimension(0).equals(ncfile.findDimension("lat"));

    Attribute att = lat.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("degrees_north");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    try {
      Array data = lat.read();
      assert data.getRank() == 1;
      assert data.getSize() == 21;
      assert data.getShape()[0] == 21;
      assert data.getElementType() == float.class;

      IndexIterator dataI = data.getIndexIterator();
      assert close(dataI.getDoubleNext(), 10.0);
      assert close(dataI.getDoubleNext(), 9.0);
      assert close(dataI.getDoubleNext(), 8.0);
    } catch (IOException io) {}

  }

  public void testReadData() {

    Variable v = ncfile.findVariable("lflx");
    assert null != v;
    assert v.getName().equals("lflx");
    assert v.getRank() == 3;
    assert v.getSize() == 360 * 21 * 456;
    assert v.getShape()[0] == 456;
    assert v.getShape()[1] == 21;
    assert v.getShape()[2] == 360;
    assert v.getDataType() == DataType.SHORT : v.getDataType();

    assert !v.isCoordinateVariable();
    assert !v.isUnlimited();

    assert v.getDimension(0).equals(ncfile.findDimension("time"));
    assert v.getDimension(1).equals(ncfile.findDimension("lat"));
    assert v.getDimension(2).equals(ncfile.findDimension("lon"));

    Attribute att = v.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("grams/kg m/s");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    try {
      Array data = v.read();
      assert data.getRank() == 3;
      assert data.getSize() == 360 * 21 * 456;
      assert data.getShape()[0] == 456;
      assert data.getShape()[1] == 21;
      assert data.getShape()[2] == 360;
      assert data.getElementType() == short.class;

      IndexIterator dataI = data.getIndexIterator();
      assert 32766 == dataI.getShortNext();
      assert 32766 == dataI.getShortNext();
      assert 32766 == dataI.getShortNext();
      assert 32766 == dataI.getShortNext();
    } catch (IOException io) {}
  }

  public void testReadSlice() {

    Variable v = ncfile.findVariable("lflx");
    int[] origin = {0, 6, 5};
    int[] shape = {1, 2, 3};
    try {
      Array data = v.read(origin, shape).reduce();
      assert data.getRank() == 2;
      assert data.getSize() == 6;
      assert data.getShape()[0] == 2;
      assert data.getShape()[1] == 3;
      assert data.getElementType() == short.class;

      IndexIterator dataI = data.getIndexIterator();
      assert dataI.getShortNext() == -22711;
      assert dataI.getShortNext() == -22239;
      assert dataI.getShortNext() == -22585;
      assert dataI.getShortNext() == -22670;
      assert dataI.getShortNext() == 32766;
      assert dataI.getShortNext() == 32766;
    } catch (InvalidRangeException io) {
      assert false;
    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  boolean close( double d1, double d2) {
    //System.out.println(d1+" "+d2);
    if (d1 != 0.0)
      return Math.abs((d1-d2)/d1) < 1.0e-5;
    else
      return Math.abs(d1-d2) < 1.0e-5;
  }
}

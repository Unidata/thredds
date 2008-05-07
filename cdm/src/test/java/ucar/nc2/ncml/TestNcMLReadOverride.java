package ucar.nc2.ncml;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.TestNcML;

import java.io.IOException;

/** Test netcdf dataset in the JUnit framework. */

public class TestNcMLReadOverride extends TestCase {

  public TestNcMLReadOverride( String name) {
    super(name);
  }

  static NetcdfFile ncfile = null;

  public void setUp() {
    if (ncfile != null) return;
    String filename = "file:./"+TestNcML.topDir + "testReadOverride.xml";

    try {
      ncfile = NcMLReader.readNcML(filename, null);
      //System.out.println("ncfile = "+ncfile);
    } catch (java.net.MalformedURLException e) {
      System.out.println("bad URL error = "+e);
    } catch (IOException e) {
      System.out.println("IO error = "+e);
      e.printStackTrace();
    }
  }

  protected void tearDown() throws IOException {
    ncfile.close();
  }

  public void testRemoved() {

    // rh was removed
    Variable v = ncfile.findVariable("rh");
    assert null == v;
   }

  public void testReadReplaced() {

    Variable v = ncfile.findVariable("time");
    assert null != v;
    assert v.getName().equals("time");
    assert v.getRank() == 1;
    assert v.getSize() == 2;
    assert v.getShape()[0] == 2;
    assert v.getDataType() == DataType.DOUBLE;

    assert v.isUnlimited();
    assert v.getDimension(0) == ncfile.findDimension("time");

    Attribute att = v.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("days");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    try {
      Array data = v.read();
      assert data.getRank() == 1;
      assert data.getSize() == 2;
      assert data.getShape()[0] == 2;
      assert data.getElementType() == double.class;

      IndexIterator dataI = data.getIndexIterator();
      assert close( dataI.getDoubleNext(),0.5);
      assert close( dataI.getDoubleNext(),1.5);
      try {
        dataI.getDoubleNext();
        assert (false);
      } catch (Exception e) {
      }
    } catch (IOException io) {}
  }


  public void testReadData() {

    Variable v = ncfile.findVariable("T");
    assert null != v;
    assert v.getName().equals("T");
    assert v.getRank() == 3;
    assert v.getSize() == 24;
    assert v.getShape()[0] == 2;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getDataType() == DataType.DOUBLE;

    assert !v.isCoordinateVariable();
    assert v.isUnlimited();

    assert v.getDimension(0) == ncfile.findDimension("time");
    assert v.getDimension(1) == ncfile.findDimension("lat");
    assert v.getDimension(2) == ncfile.findDimension("lon");

    Attribute att = v.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("degC");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    try {
      Array data = v.read();
      assert data.getRank() == 3;
      assert data.getSize() == 24;
      assert data.getShape()[0] == 2;
      assert data.getShape()[1] == 3;
      assert data.getShape()[2] == 4;
      assert data.getElementType() == double.class;

      IndexIterator dataI = data.getIndexIterator();
      assert close( dataI.getDoubleNext(),1.0);
      assert close( dataI.getDoubleNext(),2.0);
      assert close( dataI.getDoubleNext(),3.0);
      assert close( dataI.getDoubleNext(),4.0);
      assert close( dataI.getDoubleNext(),2.0);
    } catch (IOException io) {}
  }

  boolean close( double d1, double d2) {
    //System.out.println(d1+" "+d2);
    return Math.abs((d1-d2)/d1) < 1.0e-5;
  }
}

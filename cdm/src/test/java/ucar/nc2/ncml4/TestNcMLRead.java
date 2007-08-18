package ucar.nc2.ncml4;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.TestNcML;

import java.io.IOException;

/** Test netcdf dataset in the JUnit framework. */

public class TestNcMLRead extends TestCase {

  public TestNcMLRead( String name) {
    super(name);
  }

  NetcdfFile ncfile = null;
  String location = "file:"+TestNcML.topDir + "testRead.xml";

  public void setUp() {
    try {
      ncfile = NcMLReader.readNcML(location, null);
      //System.out.println("ncfile opened = "+location);
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

  public void testStructure() {
    System.out.println("ncfile opened = "+location+"\n"+ncfile);

    Attribute att = ncfile.findGlobalAttribute("title");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("Example Data");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    att = ncfile.findGlobalAttribute("testFloat");
    assert null != att;
    assert att.isArray();
    assert !att.isString();
    assert att.getDataType() == DataType.FLOAT;
    assert att.getStringValue() == null;
    assert att.getNumericValue().equals(1.0f);
    assert att.getNumericValue(3).equals(4.0f);

    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getName().equals("lat");
    assert latDim.getLength() == 3;
    assert !latDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getName().equals("time");
    assert timeDim.getLength() == 2;
    assert timeDim.isUnlimited();
  }

  public void testReadCoordvar() {

    Variable lat = ncfile.findVariable("lat");
    assert null != lat;
    assert lat.getName().equals("lat");
    assert lat.getRank() == 1;
    assert lat.getSize() == 3;
    assert lat.getShape()[0] == 3;
    assert lat.getDataType() == DataType.FLOAT;

    assert !lat.isUnlimited();

    assert lat.getDimension(0) == ncfile.findDimension("lat");

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
      assert data.getSize() == 3;
      assert data.getShape()[0] == 3;
      assert data.getElementType() == float.class;

      IndexIterator dataI = data.getIndexIterator();
      assert close(dataI.getDoubleNext(), 41.0);
      assert close(dataI.getDoubleNext(), 40.0);
      assert close(dataI.getDoubleNext(), 39.0);
    } catch (IOException io) {}

  }

  public void testReadData() throws Exception {

    Variable v = ncfile.findVariable("rh");
    assert null != v;
    assert v.getName().equals("rh");
    assert v.getRank() == 3;
    assert v.getSize() == 24;
    assert v.getShape()[0] == 2;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getDataType() == DataType.INT;

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
    assert att.getStringValue().equals("percent");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    Array data = v.read();
    assert data.getRank() == 3;
    assert data.getSize() == 24;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 4;
    assert data.getElementType() == int.class;

    IndexIterator dataI = data.getIndexIterator();
    assert dataI.getIntNext() == 1;
    assert dataI.getIntNext() == 2;
    assert dataI.getIntNext() == 3;
    assert dataI.getIntNext() == 4;
    assert dataI.getIntNext() == 5;
  }

  public void testReadSlice() throws Exception {

    Variable v = ncfile.findVariable("rh");
    int[] origin = new int[3];
    int[] shape = {2, 3, 1};

      Array data = v.read(origin, shape);
      assert data.getRank() == 3;
      assert data.getSize() == 6;
      assert data.getShape()[0] == 2;
      assert data.getShape()[1] == 3;
      assert data.getShape()[2] == 1;
      assert data.getElementType() == int.class;

      IndexIterator dataI = data.getIndexIterator();
      assert dataI.getIntNext() == 1;
      assert dataI.getIntNext() == 5;
      assert dataI.getIntNext() == 9;
      assert dataI.getIntNext() == 21;
      assert dataI.getIntNext() == 25;
      assert dataI.getIntNext() == 29;
  }

  public void testReadSlice2() throws Exception {

    Variable v = ncfile.findVariable("rh");
    int[] origin = new int[3];
    int[] shape = {2, 1, 3};

      Array data = v.read(origin, shape).reduce();
      assert data.getRank() == 2;
      assert data.getSize() == 6;
      assert data.getShape()[0] == 2;
      assert data.getShape()[1] == 3;
      assert data.getElementType() == int.class;

      IndexIterator dataI = data.getIndexIterator();
      assert dataI.getIntNext() == 1;
      assert dataI.getIntNext() == 2;
      assert dataI.getIntNext() == 3;
      assert dataI.getIntNext() == 21;
      assert dataI.getIntNext() == 22;
      assert dataI.getIntNext() == 23;

  }

  public void testReadDataAlias() throws Exception {

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
    assert att.getDataType() == DataType.STRING : att.getDataType();
    assert att.getStringValue().equals("degC");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

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
  }

  boolean close( double d1, double d2) {
    //System.out.println(d1+" "+d2);
    return Math.abs((d1-d2)/d1) < 1.0e-5;
  }

  static public class TestRead2 extends TestNcMLRead {

    // equivalent dataset using "readMetadata"
    public TestRead2( String name) {
      super(name);
      ncfile = null;
      location = "file:"+TestNcML.topDir + "readMetadata.xml";
    }
  }
}

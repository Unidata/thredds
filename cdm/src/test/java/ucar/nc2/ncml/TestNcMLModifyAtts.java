/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import junit.framework.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.Assert2;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test netcdf dataset in the JUnit framework. */

public class TestNcMLModifyAtts extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestNcMLModifyAtts( String name) {
    super(name);
  }

  NetcdfFile ncfile = null;

  public void setUp() throws IOException {
    String filename = "file:"+TestNcML.topDir + "modifyAtts.xml";
    ncfile = NcMLReader.readNcML(filename, null);
  }

  protected void tearDown() throws IOException {
    ncfile.close();
  }

  public void testGlobalAtt() {
    Attribute att = ncfile.findGlobalAttribute("Conventions");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("Metapps");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;
  }

  public void testVarAtt() {
    Variable v = ncfile.findVariable("rh");
    assert null != v;

    Attribute att = v.findAttribute(CDM.LONG_NAME);
    assert null == att;

    att = v.findAttribute("units");
    assert null == att;

    att = v.findAttribute("UNITS");
    assert null != att;
    assert att.getStringValue().equals("percent");

    att = v.findAttribute("longer_name");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("Abe said what?");
  }

  public void testStructure() {

    Attribute att = ncfile.findGlobalAttribute("title");
    assert null == att;

    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getShortName().equals("lat");
    assert latDim.getLength() == 3;
    assert !latDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
    assert timeDim.getLength() == 2;
    assert timeDim.isUnlimited();
  }

  public void testReadCoordvar() throws IOException {
    Variable lat = ncfile.findVariable("lat");
    assert null != lat;
    assert lat.getShortName().equals("lat");
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
  
    Array data = lat.read();
    assert data.getRank() == 1;
    assert data.getSize() == 3;
    assert data.getShape()[0] == 3;
    assert data.getElementType() == float.class;
  
    IndexIterator dataI = data.getIndexIterator();
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 41.0);
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 40.0);
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 39.0);
  }

  public void testReadData() throws IOException {
    Variable v = ncfile.findVariable("rh");
    assert null != v;
    assert v.getShortName().equals("rh");
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

  public void testReadSlice() throws IOException, InvalidRangeException {
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

  public void testReadSlice2() throws IOException, InvalidRangeException {
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

  public void testReadData2() throws IOException {
    Variable v = ncfile.findVariable("Temperature");
    assert null == v;

    v = ncfile.findVariable("T");
    assert null != v;
    assert v.getShortName().equals("T");
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
  
    Array data = v.read();
    assert data.getRank() == 3;
    assert data.getSize() == 24;
    assert data.getShape()[0] == 2;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 4;
    assert data.getElementType() == double.class;
  
    IndexIterator dataI = data.getIndexIterator();
    Assert2.assertNearlyEquals( dataI.getDoubleNext(),1.0);
    Assert2.assertNearlyEquals( dataI.getDoubleNext(),2.0);
    Assert2.assertNearlyEquals( dataI.getDoubleNext(),3.0);
    Assert2.assertNearlyEquals( dataI.getDoubleNext(),4.0);
    Assert2.assertNearlyEquals( dataI.getDoubleNext(),2.0);
  }
}

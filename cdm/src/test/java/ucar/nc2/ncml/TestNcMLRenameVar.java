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
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.TestNcML;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test netcdf dataset in the JUnit framework. */

public class TestNcMLRenameVar extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestNcMLRenameVar( String name) {
    super(name);
  }

  NetcdfFile ncfile = null;
  String filename = "file:./"+TestNcML.topDir + "renameVar.xml";

  public void setUp() {
    try {
      ncfile = NcMLReader.readNcML(filename, null);
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
    // System.out.println("ncfile opened = "+filename+"\n"+ncfile);

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
    assert latDim.getShortName().equals("lat");
    assert latDim.getLength() == 3;
    assert !latDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
    assert timeDim.getLength() == 4;
    assert timeDim.isUnlimited();
  }

  public void testReadCoordvar() {

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

  public void testReadData() {

    Variable v = ncfile.findVariable("ReletiveHumidity");
    assert null != v;
    assert v.getShortName().equals("ReletiveHumidity");
    assert v.getRank() == 3;
    assert v.getSize() == 48;
    assert v.getShape()[0] == 4;
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

    try {
      Array data = v.read();
      assert data.getRank() == 3;
      assert data.getSize() == 48;
      assert data.getShape()[0] == 4;
      assert data.getShape()[1] == 3;
      assert data.getShape()[2] == 4;
      assert data.getElementType() == int.class;

      IndexIterator dataI = data.getIndexIterator();
      assert dataI.getIntNext() == 1;
      assert dataI.getIntNext() == 2;
      assert dataI.getIntNext() == 3;
      assert dataI.getIntNext() == 4;
      assert dataI.getIntNext() == 5;
    } catch (IOException io) {}
  }

  public void testReadSlice() {

    Variable v = ncfile.findVariable("ReletiveHumidity");
    int[] origin = new int[3];
    int[] shape = {2, 3, 1};

    try {
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
    } catch (InvalidRangeException io) {
      assert false;
    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  public void testReadSlice2() {

    Variable v = ncfile.findVariable("ReletiveHumidity");
    int[] origin = new int[3];
    int[] shape = {2, 1, 3};

    try {
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
    } catch (InvalidRangeException io) {
      assert false;
    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  boolean close( double d1, double d2) {
    //System.out.println(d1+" "+d2);
    return Math.abs((d1-d2)/d1) < 1.0e-5;
  }

}

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
import ucar.unidata.util.test.Assert2;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;

/**
 * Test netcdf dataset in the JUnit framework.
 */

public class TestAggSynthetic extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestAggSynthetic(String name) {
    super(name);
  }

  public void test1() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcML.topDir + "aggSynthetic.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

    Variable v = ncfile.findVariable("time");
    assert v != null;
    String testAtt = ncfile.findAttValueIgnoreCase(v, "units", null);
    assert testAtt != null;
    assert testAtt.equals("months since 2000-6-16 6:00");

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile, "T");
    testReadSlice(ncfile, "T");

    ncfile.close();
  }

  public void test2() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcML.topDir + "aggSynthetic2.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar2(ncfile);
    testReadData(ncfile, "T");
    testReadSlice(ncfile, "T");

    ncfile.close();
  }

  public void test3() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcML.topDir + "aggSynthetic3.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar3(ncfile);
    testReadData(ncfile, "T");
    testReadSlice(ncfile, "T");

    ncfile.close();
  }

  public void testNoCoord() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcML.topDir + "aggSynNoCoord.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVarNoCoord(ncfile);
    testReadData(ncfile, "T");
    testReadSlice(ncfile, "T");

    ncfile.close();
  }

  public void testNoCoordDir() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcML.topDir + "aggSynNoCoordsDir.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVarNoCoordsDir(ncfile);
    testReadData(ncfile, "T");
    testReadSlice(ncfile, "T");

    ncfile.close();
  }

  public void testJoinNewScalarCoord() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcML.topDir + "aggJoinNewScalarCoord.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

    Variable v = ncfile.findVariable("time");
    assert v != null;
    String testAtt = ncfile.findAttValueIgnoreCase(v, "units", null);
    assert testAtt != null;
    assert testAtt.equals("seconds since 2017-01-01");

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVarJoinedScalar(ncfile);
    testReadData(ncfile, "T");
    testReadSlice(ncfile, "T");

    ncfile.close();
  }

  public void testRename() throws IOException, InvalidRangeException {
    String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
        "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
        "  <variable name='Temperature' orgName='T' />\n" +
        "  <aggregation  dimName='time' type='joinNew'>\n" +
        "    <variableAgg name='T'/>\n" +
        "    <scan location='src/test/data/ncml/nc/' suffix='Dir.nc' subdirs='false'/>\n" +
        "  </aggregation>\n" +
        "</netcdf>";

    String filename = "file:./" + TestNcML.topDir + "aggSynRename.xml";
    NetcdfFile ncfile = NcMLReader.readNcML( new StringReader(xml), null);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVarNoCoordsDir(ncfile);
    testReadData(ncfile, "Temperature");
    testReadSlice(ncfile, "Temperature");
    ncfile.close();
  }

  public void testScan() throws IOException, InvalidRangeException {
    String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
        "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
        "  <variable name='time' type='int' shape='time'>\n" +
        "    <attribute name='long_name' type='string' value='time coordinate' />\n" +
        "    <attribute name='units' type='string' value='days since 2001-8-31 00:00:00 UTC' />\n" +
        "    <values start='0' increment='10' />\n" +
        "  </variable>\n" +
        "  <aggregation dimName='time' type='joinNew'>\n" +
        "    <variableAgg name='T'/>\n" +
        "    <scan location='src/test/data/ncml/nc/' suffix='Dir.nc' subdirs='false'/>\n" +
        "  </aggregation>\n" +
        "</netcdf>";

    String filename = "file:./" + TestNcML.topDir + "aggSynScan.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(xml), null);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVarScan(ncfile);
    testReadData(ncfile, "T");
    testReadSlice(ncfile, "T");
    ncfile.close();
  }

  public void testDimensions(NetcdfFile ncfile) {
    logger.debug("ncfile = {}", ncfile);

    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getShortName().equals("lat");
    assert latDim.getLength() == 3;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("lon");
    assert null != lonDim;
    assert lonDim.getShortName().equals("lon");
    assert lonDim.getLength() == 4;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
    assert timeDim.getLength() == 3 : timeDim.getLength();
  }

  public void testCoordVar(NetcdfFile ncfile) throws IOException {
    Variable lat = ncfile.findVariable("lat");
    assert null != lat;
    assert lat.getShortName().equals("lat");
    assert lat.getRank() == 1;
    assert lat.getSize() == 3;
    assert lat.getShape()[0] == 3;
    assert lat.getDataType() == DataType.FLOAT;

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

  public void testAggCoordVar(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");
  
    Array data = time.read();
  
    assert (data instanceof ArrayDouble.D1) : data.getClass().getName();
    ArrayDouble.D1 dataD = (ArrayDouble.D1) data;
    assert dataD.get(0) == 0;
    assert dataD.get(1) == 10;
    assert dataD.get(2) == 99;
  }

  public void testAggCoordVar2(NetcdfFile ncfile) throws IOException {

    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");
  
    Array data = time.read();
  
    assert (data instanceof ArrayInt);
    IndexIterator dataI = data.getIndexIterator();
    assert dataI.getIntNext() == 0 : dataI.getIntCurrent();
    assert dataI.getIntNext() == 1 : dataI.getIntCurrent();
    assert dataI.getIntNext() == 2 : dataI.getIntCurrent();
  }

  public void testAggCoordVar3(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.DOUBLE : time.getDataType();

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();

    assert (data instanceof ArrayDouble);
    IndexIterator dataI = data.getIndexIterator();
    double val = dataI.getDoubleNext();
    Assert2.assertNearlyEquals(val, 0.0);
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 10.0);
    Assert2.assertNearlyEquals(dataI.getDoubleNext(), 99.0);
  }

  public void testAggCoordVarScan(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.INT : time.getDataType();

    assert time.getDimension(0) == ncfile.findDimension("time");

    int count = 0;
    Array data = time.read();
    assert (data instanceof ArrayInt);
    while (data.hasNext()) {
      int val = data.nextInt();
      assert val == count * 10 : val + "!="+ count * 10;
      count++;
    }
  }

  public void testAggCoordVarJoinedScalar(NetcdfFile ncfile) throws IOException {

    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");
  
    Array data = time.read();
  
    assert (data instanceof ArrayInt.D1) : data.getClass().getName();
    ArrayInt.D1 dataI = (ArrayInt.D1) data;
    assert dataI.get(0) == 82932;
    assert dataI.get(1) == 83232;
    assert dataI.get(2) == 83532;
  }

  public void testAggCoordVarNoCoord(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.STRING : time.getDataType();

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();

    assert (data instanceof ArrayObject);
    IndexIterator dataI = data.getIndexIterator();
    String coordName = (String) dataI.getObjectNext();
    assert coordName.equals("time0.nc") : coordName;
    coordName = (String) dataI.getObjectNext();
    assert coordName.equals("time1.nc") : coordName;
    coordName = (String) dataI.getObjectNext();
    assert coordName.equals("time2.nc") : coordName;
  }

  public void testAggCoordVarNoCoordsDir(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.STRING : time.getDataType();

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();

    assert (data instanceof ArrayObject);
    IndexIterator dataI = data.getIndexIterator();
    String coordName = (String) dataI.getObjectNext();
    assert coordName.equals("time0Dir.nc") : coordName;
    coordName = (String) dataI.getObjectNext();
    assert coordName.equals("time1Dir.nc") : coordName;
    coordName = (String) dataI.getObjectNext();
    assert coordName.equals("time2Dir.nc") : coordName;
  }

  public void testReadData(NetcdfFile ncfile, String name) throws IOException {

    Variable v = ncfile.findVariable(name);
    assert null != v;
    assert v.getShortName().equals(name);
    assert v.getRank() == 3;
    assert v.getSize() == 36 : v.getSize();
    assert v.getShape()[0] == 3;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getDataType() == DataType.DOUBLE;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("time");
    assert v.getDimension(1) == ncfile.findDimension("lat");
    assert v.getDimension(2) == ncfile.findDimension("lon");

    Array data = v.read();
    assert data.getRank() == 3;
    assert data.getSize() == 36;
    assert data.getShape()[0] == 3;
    assert data.getShape()[1] == 3;
    assert data.getShape()[2] == 4;
    assert data.getElementType() == double.class;

    int[] shape = data.getShape();
    Index tIndex = data.getIndex();
    for (int i = 0; i < shape[0]; i++)
      for (int j = 0; j < shape[1]; j++)
        for (int k = 0; k < shape[2]; k++) {
          double val = data.getDouble(tIndex.set(i, j, k));
          Assert2.assertNearlyEquals(val, 100 * i + 10 * j + k);
        }

  }

  public void readSlice(NetcdfFile ncfile, int[] origin, int[] shape, String name) throws IOException, InvalidRangeException {

    Variable v = ncfile.findVariable(name);

    Array data = v.read(origin, shape);
    assert data.getRank() == 3;
    assert data.getSize() == shape[0] * shape[1] * shape[2];
    assert data.getShape()[0] == shape[0] : data.getShape()[0] + " " + shape[0];
    assert data.getShape()[1] == shape[1];
    assert data.getShape()[2] == shape[2];
    assert data.getElementType() == double.class;

    Index tIndex = data.getIndex();
    for (int i = 0; i < shape[0]; i++)
      for (int j = 0; j < shape[1]; j++)
        for (int k = 0; k < shape[2]; k++) {
          double val = data.getDouble(tIndex.set(i, j, k));
          Assert2.assertNearlyEquals(val, 100 * (i + origin[0]) + 10 * j + k);
        }
  }

  public void testReadSlice(NetcdfFile ncfile, String name) throws IOException, InvalidRangeException {
    readSlice(ncfile, new int[]{0, 0, 0}, new int[]{3, 3, 4}, name);
    readSlice(ncfile, new int[]{0, 0, 0}, new int[]{2, 3, 2}, name);
    readSlice(ncfile, new int[]{2, 0, 0}, new int[]{1, 3, 4}, name);
    readSlice(ncfile, new int[]{1, 0, 0}, new int[]{2, 2, 3}, name);
  }
}

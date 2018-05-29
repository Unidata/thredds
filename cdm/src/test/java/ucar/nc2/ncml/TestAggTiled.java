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
import java.lang.invoke.MethodHandles;

/**
 * Test tiled aggregation
 */

public class TestAggTiled extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  int nlon = 24;
  int nlat = 12;

  public TestAggTiled(String name) {
    super(name);
  }

  public void testTiled4() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcML.topDir + "tiled/testAggTiled.ncml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    logger.debug(" TestNcmlAggExisting.open {}", ncfile);

    testDimensions(ncfile);
    testCoordVar(ncfile, "lat", nlat, DataType.DOUBLE);
    testCoordVar(ncfile, "lon", nlon, DataType.FLOAT);

    Variable v = ncfile.findVariable("temperature");
    v.setCaching(false);

    testReadData(ncfile, v);
    testReadDataSection(v, v.getShapeAsSection());

    testReadDataSection(v, new Section("0:5,6:18"));
    testReadDataSection(v, new Section("3:9,6:18"));
    testReadDataSection(v, new Section("6:11,6:18"));

    testReadDataSection(v, new Section("2:4,3:9"));
    testReadDataSection(v, new Section("2:4,14:20"));
    testReadDataSection(v, new Section("8:10,3:9"));
    testReadDataSection(v, new Section("8:10,14:20"));

    testReadDataSection(v, new Section("8:10,22"));
    testReadDataSection(v, new Section("11,22"));
    testReadDataSection(v, new Section("9,14:20"));

    testReadDataSection(v, new Section("0:5:2,6:18:2"));
    testReadDataSection(v, new Section("2:4:2,3:9:2"));
    testReadDataSection(v, new Section("9,14:20:3"));
    testReadDataSection(v, new Section("1:11:2,22"));
    testReadDataSection(v, new Section("1:11:22,22"));
    testReadDataSection(v, new Section("1:9:4,3:19:3"));

    ncfile.close();
  }

  public void testStride() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcML.topDir + "tiled/testAggTiled.ncml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    logger.debug(" TestNcmlAggExisting.open {}", ncfile);

    Variable v = ncfile.findVariable("temperature");
    v.setCaching(false);

    testReadDataSection(v, new Section("1:9:4,3:19:3"));

    ncfile.close();
  }


  public void testDimensions(NetcdfFile ncfile) {
    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getShortName().equals("lat");
    assert latDim.getLength() == nlat;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("lon");
    assert null != lonDim;
    assert lonDim.getShortName().equals("lon");
    assert lonDim.getLength() == nlon;
    assert !lonDim.isUnlimited();
  }

  public void testCoordVar(NetcdfFile ncfile, String name, int n, DataType type) throws IOException {

    Variable lat = ncfile.findVariable(name);
    assert null != lat;
    assert lat.getShortName().equals(name);
    assert lat.getRank() == 1;
    assert lat.getSize() == n;
    assert lat.getShape()[0] == n;
    assert lat.getDataType() == type;

    assert !lat.isUnlimited();
    assert lat.getDimension(0).equals(ncfile.findDimension(name));

    Array data = lat.read();
    assert data.getRank() == 1;
    assert data.getSize() == n;
    assert data.getShape()[0] == n;
    assert data.getElementType() == type.getPrimitiveClassType();

    int count = 0;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext())
      Assert2.assertNearlyEquals(dataI.getDoubleNext(), (double) count++);

  }

  public void testReadData(NetcdfFile ncfile, Variable v) throws IOException {
    assert v.getShortName().equals("temperature");
    assert v.getRank() == 2;
    assert v.getSize() == nlon * nlat : v.getSize();
    assert v.getShape()[0] == nlat;
    assert v.getShape()[1] == nlon;
    assert v.getDataType() == DataType.DOUBLE;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("lat");
    assert v.getDimension(1) == ncfile.findDimension("lon");
  
    Array data = v.read();
    assert data.getRank() == 2;
    assert data.getSize() == nlon * nlat;
    assert data.getShape()[0] == nlat;
    assert data.getShape()[1] == nlon;
    assert data.getElementType() == double.class;
  
    int[] shape = data.getShape();
    Index tIndex = data.getIndex();
    for (int row = 0; row < shape[0]; row++)
      for (int col = 0; col < shape[1]; col++) {
        double val = data.getDouble( tIndex.set(row, col));
        double truth = getVal(row, col);
        Assert2.assertNearlyEquals(val, truth);
      }
  }

  private double getVal(int row, int col) {
    if (row < 6)
      return (col < 12 ) ? row * 12 + col : 72 + row * 12 + (col-12);
    else
      return (col < 12 ) ? 144 + (row-6) * 12 + col : 216 + (row-6) * 12 + (col-12);
  }

  public void testReadDataSection(Variable v, Section s) throws InvalidRangeException, IOException {
    logger.debug("Read Section {}", s);
  
    Array data = v.read(s);
    assert data.getRank() == 2;
    assert data.getSize() == s.computeSize();
    assert data.getShape()[0] == s.getShape(0);
    assert data.getShape()[1] == s.getShape(1);
    assert data.getElementType() == double.class;
  
    int startRow = s.getOrigin(0);
    int startCol = s.getOrigin(1);
    int strideRow = s.getStride(0);
    int strideCol = s.getStride(1);
  
    int[] shape = data.getShape();
    Index tIndex = data.getIndex();
    for (int row = 0; row < shape[0]; row++)
      for (int col = 0; col < shape[1]; col++) {
        double val = data.getDouble( tIndex.set(row, col));
        double truth = getVal(startRow + row*strideRow, startCol + col*strideCol);
        Assert2.assertNearlyEquals(val, truth);
      }
  }
}

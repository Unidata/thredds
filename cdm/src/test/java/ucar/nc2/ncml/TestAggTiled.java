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
package ucar.nc2.ncml;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.ncml.TestNcML;

import java.io.IOException;

/**
 * Test tiled aggregation
 */

public class TestAggTiled extends TestCase {
  int nlon = 24;
  int nlat = 12;

  public TestAggTiled(String name) {
    super(name);
  }

  public void testTiled4() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcML.topDir + "tiled/testAggTiled.ncml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    System.out.println(" TestNcmlAggExisting.open " + ncfile);

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
    System.out.println(" TestNcmlAggExisting.open " + ncfile);

    Variable v = ncfile.findVariable("temperature");
    v.setCaching(false);

    testReadDataSection(v, new Section("1:9:4,3:19:3"));

    ncfile.close();
  }


  public void testDimensions(NetcdfFile ncfile) {
    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getName().equals("lat");
    assert latDim.getLength() == nlat;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("lon");
    assert null != lonDim;
    assert lonDim.getName().equals("lon");
    assert lonDim.getLength() == nlon;
    assert !lonDim.isUnlimited();
  }

  public void testCoordVar(NetcdfFile ncfile, String name, int n, DataType type) throws IOException {

    Variable lat = ncfile.findVariable(name);
    assert null != lat;
    assert lat.getName().equals(name);
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
      assert TestUtils.close(dataI.getDoubleNext(), (double) count++);

  }

  public void testReadData(NetcdfFile ncfile, Variable v) {
    assert v.getName().equals("temperature");
    assert v.getRank() == 2;
    assert v.getSize() == nlon * nlat : v.getSize();
    assert v.getShape()[0] == nlat;
    assert v.getShape()[1] == nlon;
    assert v.getDataType() == DataType.DOUBLE;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("lat");
    assert v.getDimension(1) == ncfile.findDimension("lon");

    try {
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
          assert TestUtils.close(val, truth) : val + "!=" + truth+"("+row+","+col+")";
        }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  private double getVal(int row, int col) {
    if (row < 6)
      return (col < 12 ) ? row * 12 + col : 72 + row * 12 + (col-12);
    else
      return (col < 12 ) ? 144 + (row-6) * 12 + col : 216 + (row-6) * 12 + (col-12);
  }

  public void testReadDataSection(Variable v, Section s) throws InvalidRangeException {
    System.out.println("Read Section "+s);

    try {
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
          assert TestUtils.close(val, truth) : val + "!=" + truth+"("+row+","+col+")";
        }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

}

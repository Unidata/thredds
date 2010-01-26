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

import junit.framework.TestCase;

import java.io.IOException;
import java.util.List;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.ma2.*;

public class TestOffAggDirectory extends TestCase {

  public TestOffAggDirectory( String name) {
    super(name);
  }

  public void testNcmlDirect() throws IOException {
    String filename = "file:" + TestAll.cdmUnitTestDir + "ncml/nc/seawifs/aggDirectory.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    System.out.println(" TestNcmlAggDirectory.open "+ filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);

    ncfile.close();
  }

  public void testNcmlDataset() throws IOException {
    String filename = "file:" + TestAll.cdmUnitTestDir + "ncml/nc/seawifs/aggDirectory.xml";

    NetcdfFile ncfile = NetcdfDataset.openDataset( filename, true, null);
    System.out.println(" TestNcmlAggExisting.openDataset "+ filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData2(ncfile);

    ncfile.close();
  }

  public void testNcmlGrid() throws IOException {
    String filename = "file:" + TestAll.cdmUnitTestDir + "ncml/nc/seawifs/aggDirectory.xml";

    GridDataset gds = GridDataset.open( filename);
    System.out.println(" TestNcmlAggExisting.openGrid "+ filename);

    List grids = gds.getGrids();
    assert grids.size() == 2;

    gds.close();
  }

  public void testDimensions(NetcdfFile ncfile) {
    Dimension latDim = ncfile.findDimension("latitude");
    assert null != latDim;
    assert latDim.getName().equals("latitude");
    assert latDim.getLength() == 630;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("longitude");
    assert null != lonDim;
    assert lonDim.getName().equals("longitude");
    assert lonDim.getLength() == 630;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getName().equals("time");
    assert timeDim.getLength() == 6;
  }

 public void testCoordVar(NetcdfFile ncfile) throws IOException {

    Variable lat = ncfile.findVariable("latitude");
    assert lat.getDataType() == DataType.FLOAT;
    assert lat.getDimension(0).equals(ncfile.findDimension("latitude"));

    Attribute att = lat.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("degree_N");

    Array data = lat.read();
    assert data.getRank() == 1;
    assert data.getSize() == 630;
    assert data.getShape()[0] == 630;
    assert data.getElementType() == float.class;

    IndexIterator dataI = data.getIndexIterator();
    assert TestUtils.close(dataI.getFloatNext(), 43.0f) : dataI.getFloatCurrent();
    assert TestUtils.close(dataI.getFloatNext(), 43.01045f) : dataI.getFloatCurrent();
    assert TestUtils.close(dataI.getFloatNext(), 43.020893f) : dataI.getFloatCurrent();

  }

  public void testAggCoordVar(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 6;
    assert time.getShape()[0] == 6;
    assert time.getDataType() == DataType.FLOAT;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == 6;
    assert data.getShape()[0] == 6;
    assert data.getElementType() == float.class;

    float vals[] = {890184.0f, 890232.0f, 890256.0f, 890304.0f, 890352.0f, 890376.0f};
    int count = 0;
    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext())
      assert TestUtils.close( dataI.getFloatNext(), vals[count++]);
  }

  public void testReadData(NetcdfFile ncfile) throws IOException {
    Variable v = ncfile.findVariable("chlorophylle_a");
    assert null != v;
    assert v.getName().equals("chlorophylle_a");
    assert v.getRank() == 3;
    assert v.getShape()[0] == 6;
    assert v.getShape()[1] == 630;
    assert v.getShape()[2] == 630;
    assert v.getDataType() == DataType.SHORT;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("time");
    assert v.getDimension(1) == ncfile.findDimension("latitude");
    assert v.getDimension(2) == ncfile.findDimension("longitude");

    Array data = v.read();
    assert data.getRank() == 3;
    assert data.getShape()[0] == 6;
    assert data.getShape()[1] == 630;
    assert data.getShape()[2] == 630;
    assert data.getElementType() == short.class;

    short[] vals = {32767, 32767, 20, 32767, 20, 20};
    int [] shape = data.getShape();
    Index tIndex = data.getIndex();
    for (int i=0; i<shape[0]; i++) {
        double val = data.getDouble( tIndex.set(i, 133, 133));
        // System.out.println(" "+val);
        assert TestUtils.close(vals[i], val) : val;
      }
  }

  public void testReadData2(NetcdfFile ncfile) throws IOException {
    Variable v = ncfile.findVariable("chlorophylle_a");
    assert null != v;
    assert v.getName().equals("chlorophylle_a");
    assert v.getRank() == 3;
    assert v.getShape()[0] == 6;
    assert v.getShape()[1] == 630;
    assert v.getShape()[2] == 630;
    assert v.getDataType() == DataType.DOUBLE;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("time");
    assert v.getDimension(1) == ncfile.findDimension("latitude");
    assert v.getDimension(2) == ncfile.findDimension("longitude");

    Array data = v.read();
    assert data.getRank() == 3;
    assert data.getShape()[0] == 6;
    assert data.getShape()[1] == 630;
    assert data.getShape()[2] == 630;
    assert data.getElementType() == double.class;

    double[] vals = {Double.NaN, Double.NaN, .20, Double.NaN, .20, .20};
    int [] shape = data.getShape();
    Index tIndex = data.getIndex();
    for (int i=0; i<shape[0]; i++) {
        double val = data.getDouble( tIndex.set(i, 133, 133));
        // System.out.println(" "+val);
        assert TestUtils.close(vals[i], val) : val;
      }
  }


}


/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import junit.framework.TestCase;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.List;

@Category(NeedsCdmUnitTest.class)
public class TestOffAggDirectory extends TestCase {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void testNcmlDirect() throws IOException {
    String filename = "file:" + TestDir.cdmUnitTestDir + "ncml/nc/seawifs/aggDirectory.ncml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    logger.debug(" TestNcmlAggDirectory.open {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData(ncfile);

    ncfile.close();
  }

  public void testNcmlDataset() throws IOException {
    String filename = "file:" + TestDir.cdmUnitTestDir + "ncml/nc/seawifs/aggDirectory.ncml";

    NetcdfFile ncfile = NetcdfDataset.openDataset( filename, true, null);
    logger.debug(" TestNcmlAggExisting.openDataset {}", filename);

    testDimensions(ncfile);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile);
    testReadData2(ncfile);

    ncfile.close();
  }

  public void testNcmlGrid() throws IOException {
    String filename = "file:" + TestDir.cdmUnitTestDir + "ncml/nc/seawifs/aggDirectory.ncml";

    GridDataset gds = GridDataset.open( filename);
    logger.debug(" TestNcmlAggExisting.openGrid {}", filename);

    List grids = gds.getGrids();
    assert grids.size() == 2;

    gds.close();
  }

  public void testDimensions(NetcdfFile ncfile) {
    Dimension latDim = ncfile.findDimension("latitude");
    assert null != latDim;
    assert latDim.getShortName().equals("latitude");
    assert latDim.getLength() == 630;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("longitude");
    assert null != lonDim;
    assert lonDim.getShortName().equals("longitude");
    assert lonDim.getLength() == 630;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getShortName().equals("time");
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
    Assert2.assertNearlyEquals(dataI.getFloatNext(), 43.0f);
    Assert2.assertNearlyEquals(dataI.getFloatNext(), 43.01045f);
    Assert2.assertNearlyEquals(dataI.getFloatNext(), 43.020893f);
  }

  public void testAggCoordVar(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
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
      Assert2.assertNearlyEquals(dataI.getFloatNext(), vals[count++]);
  }

  public void testReadData(NetcdfFile ncfile) throws IOException {
    Variable v = ncfile.findVariable("chlorophylle_a");
    assert null != v;
    assert v.getShortName().equals("chlorophylle_a");
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
        Assert2.assertNearlyEquals(vals[i], val);
      }
  }

  public void testReadData2(NetcdfFile ncfile) throws IOException {
    Variable v = ncfile.findVariable("chlorophylle_a");
    assert null != v;
    assert v.getShortName().equals("chlorophylle_a");
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
        if (Double.isNaN(val))
          assert Double.isNaN(vals[i]);
        else
          Assert2.assertNearlyEquals(vals[i], val);
      }
  }

  public void testBlanksInDirectory() throws IOException {
    String dir = TestDir.cdmUnitTestDir +"encoding/";
    String ncml =
      "<?xml version='1.0' encoding='UTF-8'?>\n" +
      "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
      " <aggregation type='joinNew' dimName='fake'>\n" +
      "  <netcdf location='"+dir+"dir mit blank/20070101.nc' coord='1'/>\n" +
      "  <netcdf location='"+dir+"dir mit blank/20070301.nc' coord='2'/>\n" +
      " </aggregation>\n" +
      "</netcdf> ";
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), null);
    logger.debug("result={}", ncfile);
    ncfile.close();
  }
}

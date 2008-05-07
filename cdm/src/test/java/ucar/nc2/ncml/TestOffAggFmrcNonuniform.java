package ucar.nc2.ncml;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.Date;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;

public class TestOffAggFmrcNonuniform extends TestCase {

  public TestOffAggFmrcNonuniform( String name) {
    super(name);
  }

  public void testGribNonuniform() throws Exception, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "offsite/aggFmrcNonuniform.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);
    System.out.println("file="+ncfile);

    testDimensions(ncfile, 3, 113, 151);
    testCoordVar(ncfile, 113);
    testAggCoordVar(ncfile, 3);
    testTimeCoordVar(ncfile, "time", 3, 11);

    ncfile.close();
  }

  private void testDimensions(NetcdfFile ncfile, int nagg, int y, int x) {
    Dimension latDim = ncfile.findDimension("x");
    assert null != latDim;
    assert latDim.getName().equals("x");
    assert latDim.getLength() == x;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("y");
    assert null != lonDim;
    assert lonDim.getName().equals("y");
    assert lonDim.getLength() == y;
    assert !lonDim.isUnlimited();

    Dimension runDim = ncfile.findDimension("run");
    assert null != runDim;
    assert runDim.getName().equals("run");
    assert runDim.getLength() == nagg : runDim.getLength();
  }

 private void testCoordVar(NetcdfFile ncfile, int n) {

    Variable lat = ncfile.findVariable("y");
    assert null != lat;
    assert lat.getName().equals("y");
    assert lat.getRank() == 1;
    assert lat.getSize() == n;
    assert lat.getShape()[0] == n;
    assert lat.getDataType() == DataType.DOUBLE;

    assert !lat.isUnlimited();
    assert lat.getDimension(0).equals(ncfile.findDimension("y"));

    Attribute att = lat.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("km");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    try {
      Array data = lat.read();
      assert data.getRank() == 1;
      assert data.getSize() == n;
      assert data.getShape()[0] == n;
      assert data.getElementType() == double.class;
    } catch (IOException io) {}

  }

  private void testAggCoordVar(NetcdfFile ncfile, int nagg) {
    Variable time = ncfile.findVariable("run");
    assert null != time;
    assert time.getName().equals("run");
    assert time.getRank() == 1;
    assert time.getSize() == nagg;
    assert time.getShape()[0] == nagg;
    assert time.getDataType() == DataType.STRING;

    DateFormatter formatter = new DateFormatter();
    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == nagg;
      assert data.getShape()[0] == nagg;
      assert data.getElementType() == String.class;

      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        String text = (String) dataI.getObjectNext();
        Date date = formatter.getISODate(text);
        assert date != null;
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

  }

  private void testTimeCoordVar(NetcdfFile ncfile, String varName, int nagg, int ntimes) throws Exception {
    Variable time = ncfile.findVariable(varName);
    assert null != time;
    assert time.getName().equals(varName);
    assert time.getRank() == 2;
    assert time.getSize() == nagg * ntimes;
    assert time.getShape()[0] == nagg;
    assert time.getShape()[1] == ntimes;
    assert time.getDataType() == DataType.INT;

    String units = time.getUnitsString();
    DateUnit du = new DateUnit( units);

    DateFormatter formatter = new DateFormatter();
    try {
      Array data = time.read();
      assert data.getSize() == nagg * ntimes;
      assert data.getShape()[0] == nagg;
      assert data.getShape()[1] == ntimes;
      assert data.getElementType() == int.class;

      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        double val = dataI.getDoubleNext();
        Date date = du.makeDate(val);
        System.out.println(" date= "+ formatter.toDateTimeStringISO(date));
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

  }

}


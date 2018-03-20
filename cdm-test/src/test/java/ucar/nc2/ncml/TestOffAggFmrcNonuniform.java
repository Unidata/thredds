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
import ucar.ma2.IndexIterator;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.util.Date;

@Category(NeedsCdmUnitTest.class)
public class TestOffAggFmrcNonuniform extends TestCase {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestOffAggFmrcNonuniform( String name) {
    super(name);
  }

  public void testGribNonuniform() throws Exception {
    String xml = "<?xml version='1.0' encoding='UTF-8'?>\n" +
      "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
      "  <aggregation dimName='run' type='forecastModelRunCollection' timeUnitsChange='true'>\n" +
      "    <scan location='" + TestDir.cdmUnitTestDir + "ncml/nc/ruc_conus40/' suffix='.grib1' enhance='true' dateFormatMark='RUC_CONUS_40km_#yyyyMMdd_HHmm'/>\n" +
      "  </aggregation>\n" +
      "</netcdf>";
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(xml), "aggFmrcNonuniform", null);

    testDimensions(ncfile, 3, 113, 151);
    testCoordVar(ncfile, 113);
    testAggCoordVar(ncfile, 3);
    testTimeCoordVar(ncfile, "time5", 3, 10);

    ncfile.close();
  }

  private void testDimensions(NetcdfFile ncfile, int nagg, int y, int x) {
    Dimension latDim = ncfile.findDimension("x");
    assert null != latDim;
    assert latDim.getShortName().equals("x");
    assert latDim.getLength() == x;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("y");
    assert null != lonDim;
    assert lonDim.getShortName().equals("y");
    assert lonDim.getLength() == y;
    assert !lonDim.isUnlimited();

    Dimension runDim = ncfile.findDimension("run");
    assert null != runDim;
    assert runDim.getShortName().equals("run");
    assert runDim.getLength() == nagg : runDim.getLength();
  }

 private void testCoordVar(NetcdfFile ncfile, int n) {

    Variable lat = ncfile.findVariable("y");
    assert null != lat;
    assert lat.getShortName().equals("y");
    assert lat.getRank() == 1;
    assert lat.getSize() == n;
    assert lat.getShape()[0] == n;
    assert lat.getDataType() == DataType.DOUBLE || lat.getDataType() == DataType.FLOAT;

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
    } catch (IOException io) {}

  }

  private void testAggCoordVar(NetcdfFile ncfile, int nagg) {
    Variable time = ncfile.findVariable("run");
    assert null != time;
    assert time.getShortName().equals("run");
    assert time.getRank() == 1;
    assert time.getSize() == nagg;
    assert time.getShape()[0] == nagg;
    assert time.getDataType() == DataType.DOUBLE || time.getDataType() == DataType.FLOAT;


    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == nagg;
      assert data.getShape()[0] == nagg;
      assert data.getElementType() == double.class;

      logger.debug(NCdumpW.toString(data));

      int count = 0;
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        double val = dataI.getDoubleNext();
        assert val == count;
        count++;
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

  }

  private void testTimeCoordVar(NetcdfFile ncfile, String varName, int nagg, int ntimes) throws Exception {
    Variable time = ncfile.findVariable(varName);
    assert null != time;
    assert time.getShortName().equals(varName);
    assert time.getRank() == 2;
    assert time.getSize() == nagg * ntimes : time.getSize() +" != "+  nagg * ntimes;
    assert time.getShape()[0] == nagg;
    assert time.getShape()[1] == ntimes;
    assert time.getDataType() == DataType.DOUBLE;

    String units = time.getUnitsString();
    DateUnit du = new DateUnit( units);

    DateFormatter formatter = new DateFormatter();
    try {
      Array data = time.read();
      assert data.getSize() == nagg * ntimes;
      assert data.getShape()[0] == nagg;
      assert data.getShape()[1] == ntimes;
      assert data.getElementType() == double.class;

      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        double val = dataI.getDoubleNext();
        Date date = du.makeDate(val);
        if (date != null)
          System.out.println(" date= "+ formatter.toDateTimeStringISO(date));
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

  }

}


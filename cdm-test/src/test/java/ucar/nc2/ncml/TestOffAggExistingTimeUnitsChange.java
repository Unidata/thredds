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
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;

/**
 * Test aggregation where timeUnitsChange='true'
 *
 * @author caron
 */
@Category(NeedsCdmUnitTest.class)
public class TestOffAggExistingTimeUnitsChange extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestOffAggExistingTimeUnitsChange( String name) {
    super(name);
  }
  //String location = "file:R:/testdata2/ncml/nc/narr/narr.ncml";

  public void testNamExtract() throws IOException, InvalidRangeException {
    String location = TestDir.cdmUnitTestDir + "ncml/nc/namExtract/test_agg.ncml";
    logger.debug(" TestOffAggExistingTimeUnitsChange.open {}", location);

    NetcdfFile ncfile = NetcdfDataset.openFile(location, null);

    Variable v = ncfile.findVariable("time");
    assert v != null;
    assert v.getDataType() == DataType.DOUBLE;

    String units = v.getUnitsString();
    assert units != null;
    assert units.equals("hours since 2006-09-25T06:00:00Z");

    int count = 0;
    Array data = v.read();
    logger.debug(NCdumpW.toString(data, "time", null));

    while (data.hasNext()) {
      Assert2.assertNearlyEquals(data.nextInt(), (count + 1) * 3);
      count++;
    }

    ncfile.close();
  }

  public void testNarrGrib() throws IOException, InvalidRangeException {
    String ncml =
            "<?xml version='1.0' encoding='UTF-8'?>\n" +
                    "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
                    " <aggregation type='joinExisting' dimName='time' timeUnitsChange='true' >\n" +
                    "  <netcdf location='narr-a_221_20070411_0000_000.grb'/>\n" +
                    "  <netcdf location='narr-a_221_20070411_0300_000.grb'/>\n" +
                    "  <netcdf location='narr-a_221_20070411_0600_000.grb'/>\n" +
                    " </aggregation>\n" +
                    "</netcdf>";

    String location = "file:"+ TestDir.cdmUnitTestDir + "ncml/nc/narr/";
    logger.debug(" TestOffAggExistingTimeUnitsChange.testNarrGrib={}\n{}", location, ncml);
    NetcdfFile ncfile = NcMLReader.readNcML(new StringReader(ncml), location, null);

    Variable v = ncfile.findVariable("time");
    assert v != null;
    assert v.getDataType() == DataType.DOUBLE;

    String units = v.getUnitsString();
    assert units != null;
    assert units.equalsIgnoreCase("Hour since 2007-04-11T00:00:00Z") : units;   // Hour since 2007-04-11T00:00:00.000Z

    int count = 0;
    Array data = v.read();
    logger.debug(NCdumpW.toString(data, "time", null));

    while (data.hasNext()) {
      assert data.nextInt() == count * 3;
      count++;
    }

    ncfile.close();
  }
}

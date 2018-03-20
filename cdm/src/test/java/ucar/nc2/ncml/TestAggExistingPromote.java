/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NCdumpW;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;

/**
 * Test promoting an attribute to a variable.
 */

public class TestAggExistingPromote extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public void testWithDateFormatMark() throws Exception {
    String filename = "file:" + TestNcML.topDir + "aggExistingPromote.ncml";

    String aggExistingPromote =
          "<?xml version='1.0' encoding='UTF-8'?>\n"+
          "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n"+
          "  <aggregation dimName='time' type='joinExisting' recheckEvery='4 sec'>\n"+
          "    <promoteGlobalAttribute name='times' orgName='time_coverage_end' />\n"+
          "    <scan dateFormatMark='CG#yyyyDDD_HHmmss' location='nc/cg/' suffix='.nc' subdirs='false' />\n"+
          "  </aggregation>\n"+
          "</netcdf>";

    NetcdfFile ncfile = NcMLReader.readNcML( new StringReader(aggExistingPromote), filename, null);
    System.out.println(" TestNcmlAggExisting.open "+ filename+"\n"+ncfile);

    // the promoted var
    Variable pv = ncfile.findVariable("times");
    assert null != pv;

    assert pv.getShortName().equals("times");
    assert pv.getRank() == 1;
    assert pv.getSize() == 3;
    assert pv.getShape()[0] == 3;
    assert pv.getDataType() == DataType.STRING;
    Dimension d = pv.getDimension(0);
    assert d.getShortName().equals("time");

    Array datap = pv.read();
    assert datap.getRank() == 1;
    assert datap.getSize() == 3;
    assert datap.getShape()[0] == 3;
    assert datap.getElementType() == String.class;

    logger.debug(NCdumpW.toString(datap, "time_coverage_end", null));

    String[] resultp = new String[]{"2006-06-07T12:00:00Z", "2006-06-07T13:00:00Z", "2006-06-07T14:00:00Z"};
    int count = 0;
    IndexIterator dataI = datap.getIndexIterator();
    while (dataI.hasNext()) {
      String s = (String) dataI.getObjectNext();
      assert s.equals(resultp[count]) : s;
      count++;
    }

    // the coordinate var
    Variable time = ncfile.findVariable("time");
    assert null != time;

    assert time.getShortName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 3;
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.STRING;

    assert time.getDimension(0) == ncfile.findDimension("time");

    //String units = time.getUnitsString();
    //DateUnit du = new DateUnit(units);
    //DateFormatter df = new DateFormatter();

    String[] result = new String[] {"2006-06-07T12:00:00Z",   "2006-06-07T13:00:00Z",   "2006-06-07T14:00:00Z"};
    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == 3;
      assert data.getShape()[0] == 3;
      assert data.getElementType() == String.class;

      logger.debug(NCdumpW.toString(data, "time coord", null));

      count = 0;
      dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        String val = (String) dataI.getObjectNext();
        //Date dateVal = du.makeDate(val);
        //String dateS = df.toDateTimeStringISO(dateVal);
        assert val.equals( result[count]) : val+" != "+ result[count];
        count++;
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

    /* String[] result = new String[]{"2006-06-07T12:00:00Z", "2006-06-07T13:00:00Z", "2006-06-07T14:00:00Z"};
    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == 3;
    assert data.getShape()[0] == 3;
    assert data.getElementType() == String.class;

    NCdumpW.printArray(data, "time coord", System.out, null);

    count = 0;
    dataI = data.getIndexIterator();
    while (dataI.hasNext()) {
      String s = (String) dataI.getObjectNext();
      assert s.equals(result[count]) : s;
      count++;
    } */

    ncfile.close();
  }

  /*
  <netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">

  <aggregation dimName="time" type="joinExisting">
    <promoteGlobalAttribute name="title" />
    <netcdf location="file:src/test/data/ncml/nc/jan.nc"/>
    <netcdf location="file:src/test/data/ncml/nc/feb.nc"/>
  </aggregation>

</netcdf>
   */
  public void testNotOne() throws IOException, InvalidRangeException {
    String filename = "file:" + TestNcML.topDir + "aggExistingPromote2.ncml";

    String aggExistingPromote2 =
        "<?xml version='1.0' encoding='UTF-8'?>\n" +
        "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
        "  <aggregation dimName='time' type='joinExisting'>\n" +
        "    <promoteGlobalAttribute name='title' />\n" +
        "    <promoteGlobalAttribute name='month' />\n" +
        "    <promoteGlobalAttribute name='vector' />\n" +
        "    <netcdf location='file:src/test/data/ncml/nc/jan.nc'>\n" +
        "      <attribute name='month' value='jan'/>\n" +
        "      <attribute name='vector' value='1 2 3' type='int'/>\n" +
        "    </netcdf>\n" +
        "    <netcdf location='file:src/test/data/ncml/nc/feb.nc'>\n" +
        "      <attribute name='month' value='feb'/>\n" +
        "      <attribute name='vector' value='4 5 6' type='int'/>\n" +
        "    </netcdf>\n" +
        "  </aggregation>\n" +
        "</netcdf>";


    NetcdfFile ncfile = NcMLReader.readNcML( new StringReader(aggExistingPromote2), filename, null);
    Dimension dim = ncfile.findDimension("time");

    // the promoted var
    Variable pv = ncfile.findVariable("title");
    assert null != pv;

    assert pv.getShortName().equals("title");
    assert pv.getRank() == 1;
    assert pv.getSize() == dim.getLength();
    assert pv.getDataType() == DataType.STRING;
    Dimension d = pv.getDimension(0);
    assert d.getShortName().equals("time");

    Array datap = pv.read();
    assert datap.getRank() == 1;
    assert datap.getSize() == dim.getLength();
    assert datap.getElementType() == String.class;

    logger.debug(NCdumpW.toString(datap, "title", null));

    while (datap.hasNext())
      assert datap.next().equals("Example Data");

    // the promoted var
    pv = ncfile.findVariable("month");
    assert null != pv;

    assert pv.getShortName().equals("month");
    assert pv.getRank() == 1;
    assert pv.getSize() == dim.getLength();
    assert pv.getDataType() == DataType.STRING;
    d = pv.getDimension(0);
    assert d.getShortName().equals("time");

    datap = pv.read();
    assert datap.getRank() == 1;
    assert datap.getSize() == dim.getLength();
    assert datap.getElementType() == String.class;

    logger.debug(NCdumpW.toString(datap, "title", null));

    int count = 0;
    while (datap.hasNext()) {
      assert datap.next().equals((count < 31) ? "jan" : "feb") : count;
      count++;
    }

    ncfile.close();
  }


}

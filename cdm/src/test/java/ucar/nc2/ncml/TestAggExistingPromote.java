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
import ucar.nc2.units.DateUnit;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.ncml.TestNcML;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

/**
 * Test promoting an attribute to a variable.
 */

public class TestAggExistingPromote extends TestCase {

  public TestAggExistingPromote(String name) {
    super(name);
  }

  public void testWithDateFormatMark() throws Exception, InvalidRangeException {
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

    assert pv.getName().equals("times");
    assert pv.getRank() == 1;
    assert pv.getSize() == 3;
    assert pv.getShape()[0] == 3;
    assert pv.getDataType() == DataType.STRING;
    Dimension d = pv.getDimension(0);
    assert d.getName().equals("time");

    Array datap = pv.read();
    assert datap.getRank() == 1;
    assert datap.getSize() == 3;
    assert datap.getShape()[0] == 3;
    assert datap.getElementType() == String.class;

    NCdump.printArray(datap, "time_coverage_end", System.out, null);

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

    assert time.getName().equals("time");
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

      NCdump.printArray(data, "time coord", System.out, null);

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

    NCdump.printArray(data, "time coord", System.out, null);

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

    assert pv.getName().equals("title");
    assert pv.getRank() == 1;
    assert pv.getSize() == dim.getLength();
    assert pv.getDataType() == DataType.STRING;
    Dimension d = pv.getDimension(0);
    assert d.getName().equals("time");

    Array datap = pv.read();
    assert datap.getRank() == 1;
    assert datap.getSize() == dim.getLength();
    assert datap.getElementType() == String.class;

    NCdump.printArray(datap, "title", System.out, null);

    while (datap.hasNext())
      assert datap.next().equals("Example Data");

    // the promoted var
    pv = ncfile.findVariable("month");
    assert null != pv;

    assert pv.getName().equals("month");
    assert pv.getRank() == 1;
    assert pv.getSize() == dim.getLength();
    assert pv.getDataType() == DataType.STRING;
    d = pv.getDimension(0);
    assert d.getName().equals("time");

    datap = pv.read();
    assert datap.getRank() == 1;
    assert datap.getSize() == dim.getLength();
    assert datap.getElementType() == String.class;

    NCdump.printArray(datap, "title", System.out, null);

    int count = 0;
    while (datap.hasNext()) {
      assert datap.next().equals((count < 31) ? "jan" : "feb") : count;
      count++;
    }

    ncfile.close();
  }


}

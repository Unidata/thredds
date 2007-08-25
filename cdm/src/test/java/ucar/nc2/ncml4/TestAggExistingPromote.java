package ucar.nc2.ncml4;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.ncml.TestNcML;

import java.io.IOException;

/**
 * Test NcML AggExisting ways to define coordinate variable calues
 *
 * @see "http://www.unidata.ucar.edu/software/netcdf/ncml/v2.2/Aggregation.html#JoinExistingTypes"
 */

public class TestAggExistingPromote extends TestCase {

  public TestAggExistingPromote(String name) {
    super(name);
  }

  public void testWithDateFormatMark() throws IOException, InvalidRangeException {
    String filename = "file:" + TestNcML.topDir + "aggExistingPromote.ncml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

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

    String[] result = new String[]{"2006-06-07T12:00:00Z", "2006-06-07T13:00:00Z", "2006-06-07T14:00:00Z"};
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
    }

    ncfile.close();
  }


}

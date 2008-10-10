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

/** Test NcML AggExisting ways to define coordinate variable calues
 * @see "http://www.unidata.ucar.edu/software/netcdf/ncml/v2.2/Aggregation.html#JoinExistingTypes"
 **/

public class TestAggExistingCoordVars extends TestCase {

  public TestAggExistingCoordVars( String name) {
    super(name);
  }

  public void testType1() throws IOException, InvalidRangeException {
    String filename = "file:./"+ TestNcML.topDir + "aggExisting1.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    System.out.println(" TestNcmlAggExisting.open "+ filename);

    Variable time = ncfile.findVariable("time");
    assert null != time;

    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 59;
    assert time.getShape()[0] == 59;
    assert time.getDataType() == DataType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");

    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == 59;
      assert data.getShape()[0] == 59;
      assert data.getElementType() == int.class;

      int count = 0;
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        assert dataI.getIntNext() == 7 + 2 * count;
        count++;
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

    ncfile.close();
  }


  String aggExisting2 =
    "<?xml version='1.0' encoding='UTF-8'?>\n" +
    "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n" +
    "   <variable name='time'>\n" +
    "     <attribute name='units' value='hours since 2006-06-16 00:00'/>\n" +
    "     <attribute name='_CoordinateAxisType' value='Time' />\n" +
    "   </variable>\n" +
    "  <aggregation type='joinExisting' dimName='time' >\n" +
    "    <netcdf location='nc/cg/CG2006158_120000h_usfc.nc' ncoords='1' coordValue='12' />\n" +
    "    <netcdf location='nc/cg/CG2006158_130000h_usfc.nc' ncoords='1' coordValue='13' />\n" +
    "    <netcdf location='nc/cg/CG2006158_140000h_usfc.nc' ncoords='1' coordValue='14' />\n" +
    "  </aggregation>\n" +
    "</netcdf>";

  public void testType2() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggExisting2.xml";

    NetcdfFile ncfile = NcMLReader.readNcML( new StringReader(aggExisting2), filename, null);
    System.out.println(" TestNcmlAggExisting.open "+ filename+"\n"+ncfile);

    Variable time = ncfile.findVariable("time");
    assert null != time;

    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 3;
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.getDimension(0) == ncfile.findDimension("time");

    double[] result = new double[] {12, 13, 14};
    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == 3;
      assert data.getShape()[0] == 3;
      assert data.getElementType() == double.class;

      int count = 0;
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        double val = dataI.getDoubleNext();
        assert TestAll.closeEnough( val, result[count]) : val +" != "+ result[count];
        count++;
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

    ncfile.close();
  }

  public void testType3() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggExisting.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    System.out.println(" TestNcmlAggExisting.open "+ filename+"\n"+ncfile);

    Variable time = ncfile.findVariable("time");
    assert null != time;

    String testAtt = ncfile.findAttValueIgnoreCase(time, "ncmlAdded", null);
    assert testAtt != null;
    assert testAtt.equals("timeAtt");

    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 59;
    assert time.getShape()[0] == 59;
    assert time.getDataType() == DataType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");

    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == 59;
      assert data.getShape()[0] == 59;
      assert data.getElementType() == int.class;

      int count = 0;
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext())
        assert dataI.getIntNext() == count++;

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

    ncfile.close();
  }

  public void testType4() throws IOException, InvalidRangeException {
    String filename = "file:"+TestNcML.topDir + "aggExisting4.ncml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

    Variable time = ncfile.findVariable("time");
    assert null != time;

    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 3;
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.getDimension(0) == ncfile.findDimension("time");

    double[] result = new double[] {1.1496816E9, 1.1496852E9, 1.1496888E9  };
    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == 3;
      assert data.getShape()[0] == 3;
      assert data.getElementType() == double.class;

      int count = 0;
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        assert TestAll.closeEnough(dataI.getDoubleNext(), result[count]);
        count++;
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

    ncfile.close();
  }

  public void testWithDateFormatMark() throws Exception, InvalidRangeException {
    String filename = "file:"+TestNcML.topDir + "aggExistingOne.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

    Variable time = ncfile.findVariable("time");
    assert null != time;

    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 3;
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.getDimension(0) == ncfile.findDimension("time");

    String units = time.getUnitsString();
    DateUnit du = new DateUnit(units);
    DateFormatter df = new DateFormatter();

    String[] result = new String[] {"2006-06-07T12:00:00Z",   "2006-06-07T13:00:00Z",   "2006-06-07T14:00:00Z"};
    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == 3;
      assert data.getShape()[0] == 3;
      assert data.getElementType() == double.class;

      NCdump.printArray(data, "time coord", System.out, null);

      int count = 0;
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        double val = dataI.getDoubleNext();
        Date dateVal = du.makeDate(val);
        String dateS = df.toDateTimeStringISO(dateVal);
        assert dateS.equals( result[count]) : dateS+" != "+ result[count];
        count++;
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

    ncfile.close();
  }

  public void testClimatologicalDate() throws IOException, InvalidRangeException {
    String filename = "file:"+TestNcML.topDir + "aggExisting5.ncml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

    Variable time = ncfile.findVariable("time");
    assert null != time;

    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 59;
    assert time.getShape()[0] == 59;
    assert time.getDataType() == DataType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");

      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == 59;
      assert data.getShape()[0] == 59;
      assert data.getElementType() == int.class;

      int count = 0;
      while (data.hasNext()) {
        assert data.nextInt() == count : data.nextInt() +"!="+ count;
        count++;
      }

    ncfile.close();
  }


}

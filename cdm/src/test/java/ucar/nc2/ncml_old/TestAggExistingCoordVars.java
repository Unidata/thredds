package ucar.nc2.ncml_old;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.*;

import java.io.IOException;

/** Test NcML AggExisting ways to define coordinate variable calues
 * @see "http://www.unidata.ucar.edu/software/netcdf/ncml/v2.2/Aggregation.html#JoinExistingTypes"
 **/

public class TestAggExistingCoordVars extends TestCase {

  public TestAggExistingCoordVars( String name) {
    super(name);
  }

  public void testType1() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggExisting1.xml";

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


  public void testType2() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggExisting2.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
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
    System.out.println(" TestNcmlAggExisting.open "+ filename);

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

}

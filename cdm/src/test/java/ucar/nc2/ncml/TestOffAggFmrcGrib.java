package ucar.nc2.ncml;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.Date;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;

public class TestOffAggFmrcGrib extends TestCase {
  private boolean showValues = false;

  public TestOffAggFmrcGrib( String name) {
    super(name);
  }

  public void testSimple() throws Exception {
    String filename = "file:./"+TestNcML.topDir + "offsite/aggFmrcGrib.xml";
    System.out.println("TestAggForecastModel.open "+ filename);
    TestAll.showMem("TestAggFmrcGrib start ");

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    //System.out.println("file="+ncfile);

    String timeDimName = "time";
    int naggs = 8;

    testDimensions(ncfile, naggs, timeDimName);
    testCoordVar(ncfile, 257);
    testAggCoordVar(ncfile, naggs, 122100, 12);
    testTimeCoordVar(ncfile, naggs, 29, timeDimName);

    System.out.println("TestAggForecastModel.testReadData ");    
    testReadData(ncfile, naggs);
 //   testReadSlice(ncfile);

    TestAll.showMem("TestAggFmrcGrib end ");
    ncfile.close();    
  }

  // the fmrc definition has changed
  public void utestRunseq() throws Exception {
    String filename = "file:./"+TestNcML.topDir + "offsite/aggFmrcGribRunseq.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);
    //System.out.println("file="+ncfile);

    int naggs = 4;
    String timeDimName = "time";
    testDimensions(ncfile, naggs, timeDimName);
    testCoordVar(ncfile, 257);
    testAggCoordVar(ncfile, naggs, 122100, 12);
    testTimeCoordVar(ncfile, naggs, 29, timeDimName);

    ncfile.close();
  }

  private void testDimensions(NetcdfFile ncfile, int nagg, String timeDimName) {
    Dimension latDim = ncfile.findDimension("x");
    assert null != latDim;
    assert latDim.getName().equals("x");
    assert latDim.getLength() == 369;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("y");
    assert null != lonDim;
    assert lonDim.getName().equals("y");
    assert lonDim.getLength() == 257;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension(timeDimName);
    assert null != timeDim;
    assert timeDim.getName().equals(timeDimName);
    assert timeDim.getLength() == 29;

    Dimension runDim = ncfile.findDimension("run");
    assert null != runDim;
    assert runDim.getName().equals("run");
    assert runDim.getLength() == nagg : nagg +" != "+ runDim.getLength();
  }

 private void testCoordVar(NetcdfFile ncfile, int n) throws IOException {

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

      Array data = lat.read();
      assert data.getRank() == 1;
      assert data.getSize() == n;
      assert data.getShape()[0] == n;
      assert data.getElementType() == double.class;

      IndexIterator dataI = data.getIndexIterator();
      assert TestUtils.close(dataI.getDoubleNext(), -832.6982610175637);
      assert TestUtils.close(dataI.getDoubleNext(), -812.3802610175637);
      assert TestUtils.close(dataI.getDoubleNext(), -792.0622610175637);

  }

  private void testAggCoordVar(NetcdfFile ncfile, int nagg, int start, int incr) {
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

  private void testTimeCoordVar(NetcdfFile ncfile, int nagg, int ntimes, String timeDimName) throws Exception {
    Variable time = ncfile.findVariable(timeDimName);
    assert null != time;
    assert time.getName().equals(timeDimName);
    assert time.getRank() == 2;
    assert time.getSize() == nagg * ntimes;
    assert time.getShape()[0] == nagg;
    assert time.getShape()[1] == ntimes;
    assert time.getDataType() == DataType.INT;

    String units = time.getUnitsString();
    DateUnit du = new DateUnit( units);

    DateFormatter formatter = new DateFormatter();
      Array data = time.read();
      assert data.getSize() == nagg * ntimes;
      assert data.getShape()[0] == nagg;
      assert data.getShape()[1] == ntimes;
      assert data.getElementType() == int.class;

      while (data.hasNext()) {
        double val = data.nextDouble();
        Date date = du.makeDate(val);
        if (showValues) System.out.println(" date= "+ formatter.toDateTimeStringISO(date));
      }

  }

  private void testReadData(NetcdfFile ncfile, int nagg) throws IOException {
    Variable v = ncfile.findVariable("Pressure_surface");
    assert null != v;
    assert v.getName().equals("Pressure_surface");
    assert v.getRank() == 4;
    int[] shape = v.getShape();
    assert shape[0] == nagg;
    assert shape[1] == 29 : new Section(shape).toString();
    assert shape[2] == 257 : new Section(shape).toString();
    assert shape[3] == 369 : new Section(shape).toString();
    assert v.getDataType() == DataType.FLOAT;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("run");
    assert v.getDimension(1) == ncfile.findDimension("time");
    assert v.getDimension(2) == ncfile.findDimension("y");
    assert v.getDimension(3) == ncfile.findDimension("x");

    Array data = v.read();
    assert data.getRank() == 4;
    assert data.getShape()[0] == nagg;
    assert data.getShape()[1] == 29;
    assert data.getShape()[2] == 257;
    assert data.getShape()[3] == 369;

    double sum = MAMath.sumDoubleSkipMissingData(data, 0.0);

    /* float sum = 0.0f;
    IndexIterator ii = data.getIndexIterator();
    while (ii.hasNext()) {
      sum += ii.getFloatNext();
    } */
    System.out.println(" sum= "+sum);

  }

  private void testReadSlice(NetcdfFile ncfile, int[] origin, int[] shape) throws IOException, InvalidRangeException {

    Variable v = ncfile.findVariable("P_sfc");

      Array data = v.read(origin, shape);
      assert data.getRank() == 4;
      assert data.getSize() == shape[0] * shape[1] * shape[2] * shape[3];
      assert data.getShape()[0] == shape[0] : data.getShape()[0] +" "+shape[0];
      assert data.getShape()[1] == shape[1];
      assert data.getShape()[2] == shape[2];
      assert data.getShape()[3] == shape[3];
      assert data.getElementType() == float.class;

      /* Index tIndex = data.getIndex();
      for (int i=0; i<shape[0]; i++)
       for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++) {
          double val = data.getDouble( tIndex.set(i, j, k));
          //System.out.println(" "+val);
          assert TestUtils.close(val, 100*(i+origin[0]) + 10*j + k) : val;
        } */

  }

  private void testReadSlice(NetcdfFile ncfile) throws IOException, InvalidRangeException {
    testReadSlice( ncfile, new int[] {0, 0, 0, 0}, new int[] {1, 11, 3, 4} );
    testReadSlice( ncfile, new int[] {0, 0, 0, 0}, new int[] {3, 2, 3, 2} );
    testReadSlice( ncfile, new int[] {3, 5, 0, 0}, new int[] {1, 5, 3, 4} );
    testReadSlice( ncfile, new int[] {3, 9, 0, 0}, new int[] {5, 2, 2, 3} );
   }
}


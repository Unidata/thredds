package ucar.nc2.ncml;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;
import java.util.Date;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;

public class TestAggFmrcGrib extends TestCase {

  public TestAggFmrcGrib( String name) {
    super(name);
  }

  public void testGribdatasets() throws Exception, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggFmrcGrib.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);
    //System.out.println("file="+ncfile);

    testDimensions(ncfile, 7);
    testCoordVar(ncfile, 257);
    testAggCoordVar(ncfile, 7, 122100, 12);
    testTimeCoordVar(ncfile, 7, 29);

//    testReadData(ncfile, 15);
 //   testReadSlice(ncfile);

    ncfile.close();
  }

  private void testDimensions(NetcdfFile ncfile, int nagg) {
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

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getName().equals("time");
    assert timeDim.getLength() == 29;

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
    assert lat.getCoordinateDimension().equals(ncfile.findDimension("y"));

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

      IndexIterator dataI = data.getIndexIterator();
      assert TestUtils.close(dataI.getDoubleNext(), -832.6982610175637);
      assert TestUtils.close(dataI.getDoubleNext(), -812.3802610175637);
      assert TestUtils.close(dataI.getDoubleNext(), -792.0622610175637);
    } catch (IOException io) {}

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

  private void testTimeCoordVar(NetcdfFile ncfile, int nagg, int ntimes) throws Exception {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getName().equals("time");
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

  private void testReadData(NetcdfFile ncfile, int nagg) throws IOException {
    Variable v = ncfile.findVariable("P_sfc");
    assert null != v;
    assert v.getName().equals("P_sfc");
    assert v.getRank() == 4;
    assert v.getShape()[0] == nagg;
    assert v.getShape()[1] == 11;
    assert v.getShape()[2] == 65;
    assert v.getShape()[3] == 93;
    assert v.getDataType() == DataType.FLOAT;

    assert v.getCoordinateDimension() == null;

    assert v.getDimension(0) == ncfile.findDimension("run");
    assert v.getDimension(1) == ncfile.findDimension("record");
    assert v.getDimension(2) == ncfile.findDimension("y");
    assert v.getDimension(3) == ncfile.findDimension("x");

    Array data = v.read();
    assert data.getRank() == 4;
    assert data.getShape()[0] == nagg;
    assert data.getShape()[1] == 11;
    assert data.getShape()[2] == 65;
    assert data.getShape()[3] == 93;

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


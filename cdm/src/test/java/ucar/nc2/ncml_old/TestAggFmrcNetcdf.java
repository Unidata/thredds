package ucar.nc2.ncml_old;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.Date;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;

public class TestAggFmrcNetcdf extends TestCase {

  public TestAggFmrcNetcdf( String name) {
    super(name);
  }

  public void testNUWGdatasets() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggFmrcNetcdf.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);

    int nagg = 14;

    testDimensions(ncfile, nagg);
    testYCoordVar(ncfile);
    testRunCoordVar(ncfile, nagg);
    testTimeCoordVar(ncfile, nagg, 11);
    testReadData(ncfile, nagg);
    testReadSlice(ncfile);

    ncfile.close();
  }

  private void testDimensions(NetcdfFile ncfile, int nagg) {
    Dimension latDim = ncfile.findDimension("x");
    assert null != latDim;
    assert latDim.getName().equals("x");
    assert latDim.getLength() == 93;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("y");
    assert null != lonDim;
    assert lonDim.getName().equals("y");
    assert lonDim.getLength() == 65;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("run");
    assert null != timeDim;
    assert timeDim.getName().equals("run");
    assert timeDim.getLength() == nagg : timeDim.getLength();
  }

 private void testYCoordVar(NetcdfFile ncfile) {

    Variable lat = ncfile.findVariable("y");
    assert null != lat;
    assert lat.getName().equals("y");
    assert lat.getRank() == 1;
    assert lat.getSize() == 65;
    assert lat.getShape()[0] == 65;
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
      assert data.getSize() == 65;
      assert data.getShape()[0] == 65;
      assert data.getElementType() == double.class;

      IndexIterator dataI = data.getIndexIterator();
      assert TestUtils.close(dataI.getDoubleNext(), -832.6983183345455);
      assert TestUtils.close(dataI.getDoubleNext(), -751.4273183345456);
      assert TestUtils.close(dataI.getDoubleNext(), -670.1563183345455);
    } catch (IOException io) {}

  }


  private void testRunCoordVar(NetcdfFile ncfile, int nagg) {
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

  private void testTimeCoordVar(NetcdfFile ncfile, int nagg, int noff) throws IOException {
    Variable time = ncfile.findVariable("valtime");
    assert null != time;
    assert time.getName().equals("valtime");
    assert time.getRank() == 2;
    assert time.getSize() == nagg * noff;
    assert time.getShape()[0] == nagg;
    assert time.getShape()[1] == noff;
    assert time.getDataType() == DataType.DOUBLE;

    Array data = time.read();
    assert data.getRank() == 2;
    assert data.getSize() == nagg * noff;
    assert data.getShape()[0] == nagg;
    assert data.getShape()[1] == noff;
    assert data.getElementType() == double.class;

    double[][] result =  new double[][]
       {{122100.0, 122106.0, 122112.0, 122118.0, 122124.0, 122130.0, 122136.0, 122142.0, 122148.0, 122154.0, 122160.0},
        {122112.0, 122118.0, 122124.0, 122130.0, 122136.0, 122142.0, 122148.0, 122154.0, 122160.0, 122166.0, 122172.0},
        {122124.0, 122130.0, 122136.0, 122142.0, 122148.0, 122154.0, 122160.0, 122166.0, 122172.0, 122178.0, 122184.0},
        {122136.0, 122142.0, 122148.0, 122154.0, 122160.0, 122166.0, 122172.0, 122178.0, 122184.0, 122190.0, 122196.0},
        {122148.0, 122154.0, 122160.0, 122166.0, 122172.0, 122178.0, 122184.0, 122190.0, 122196.0, 122202.0, 122208.0},
        {122160.0, 122166.0, 122172.0, 122178.0, 122184.0, 122190.0, 122196.0, 122202.0, 122208.0, 122214.0, 122220.0},
        {122172.0, 122178.0, 122184.0, 122190.0, 122196.0, 122202.0, 122208.0, 122214.0, 122220.0, 122226.0, 122232.0},
        {122184.0, 122190.0, 122196.0, 122202.0, 122208.0, 122214.0, 122220.0, 122226.0, 122232.0, 122238.0, 122244.0},
        {122196.0, 122202.0, 122208.0, 122214.0, 122220.0, 122226.0, 122232.0, 122238.0, 122244.0, 122250.0, 122256.0},
        {122208.0, 122214.0, 122220.0, 122226.0, 122232.0, 122238.0, 122244.0, 122250.0, 122256.0, 122262.0, 122268.0},
        {122220.0, 122226.0, 122232.0, 122238.0, 122244.0, 122250.0, 122256.0, 122262.0, 122268.0, 122274.0, 122280.0},
        {122232.0, 122238.0, 122244.0, 122250.0, 122256.0, 122262.0, 122268.0, 122274.0, 122280.0, 122286.0, 122292.0},
        {122244.0, 122250.0, 122256.0, 122262.0, 122268.0, 122274.0, 122280.0, 122286.0, 122292.0, 122298.0, 122304.0},
        {122256.0, 122262.0, 122268.0, 122274.0, 122280.0, 122286.0, 122292.0, 122298.0, 122304.0, 122310.0, 122316.0}};

    Index ima = data.getIndex();
    for (int i=0; i < nagg; i++)
      for (int j=0; j < noff; j++) {
        double val = data.getDouble(ima.set(i,j));
        assert TestAll.closeEnough(val, result[i][j]);
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

    assert !v.isCoordinateVariable();

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


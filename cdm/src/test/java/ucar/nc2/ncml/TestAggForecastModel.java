package ucar.nc2.ncml;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;

import ucar.ma2.*;
import ucar.nc2.*;

public class TestAggForecastModel extends TestCase {

  public TestAggForecastModel( String name) {
    super(name);
  }

  public void testForecastModel() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggForecastModel.xml";

    NetcdfFile ncfile = new NcMLReader().readNcML(filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);

    testDimensions(ncfile, 15);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile, 15, 122100, 12);
    testReadData(ncfile, 15);
    testReadSlice(ncfile);

    ncfile.close();
  }

  public void testForecastModelExtend() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggForecastModel.xml";
    String newModel = "C:/data/ncmodels/NAM_CONUS_80km_20051212_1200.nc";
    String newModelsave = "C:/data/ncmodels/NAM_CONUS_80km_20051212_1200.nc.save";
    File newModelFile = new File(newModel);
    File newModelFileSave = new File(newModelsave);
    boolean ok = newModelFile.renameTo(newModelFileSave);
    if (!ok) throw new IOException("cant rename file");

    NetcdfFile ncfile = new NcMLReader().readNcML(filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);

    testDimensions(ncfile, 14);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile, 14, 122100, 12);
    testReadData(ncfile, 14);
    testReadSlice(ncfile);

    // new file arrives
    ok = newModelFileSave.renameTo(newModelFile);
    if (!ok) throw new IOException("cant rename file");

    ncfile.sync();

    testDimensions(ncfile, 15);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile, 15, 122100, 12);;
    testReadData(ncfile, 15);
    testReadSlice(ncfile);

    ncfile.close();
  }

  public void testForecastConstant() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggForecastConstant.xml";

    NetcdfFile ncfile = new NcMLReader().readNcML(filename, null);
    System.out.println(" aggForecastConstant.open "+ filename);

    testDimensions(ncfile, 6);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile, 6, 122160, 0);
    testReadData(ncfile, 6);
    testReadSlice(ncfile);

    ncfile.close();
  }

  public void testForecastOffset() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "aggForecastOffset.xml";

    NetcdfFile ncfile = new NcMLReader().readNcML(filename, null);
    System.out.println(" aggForecastOffset.open "+ filename);

    testDimensions(ncfile, 15);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile, 15, 122106, 12);
    testReadData(ncfile, 15);
    testReadSlice(ncfile);

    ncfile.close();
  }

  public void testDimensions(NetcdfFile ncfile, int nagg) {
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

    Dimension timeDim = ncfile.findDimension("record");
    assert null != timeDim;
    assert timeDim.getName().equals("record");
    assert timeDim.getLength() == nagg : timeDim.getLength();
  }

 public void testCoordVar(NetcdfFile ncfile) {

    Variable lat = ncfile.findVariable("y");
    assert null != lat;
    assert lat.getName().equals("y");
    assert lat.getRank() == 1;
    assert lat.getSize() == 65;
    assert lat.getShape()[0] == 65;
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
      assert data.getSize() == 65;
      assert data.getShape()[0] == 65;
      assert data.getElementType() == double.class;

      IndexIterator dataI = data.getIndexIterator();
      assert TestUtils.close(dataI.getDoubleNext(), -832.6983183345455);
      assert TestUtils.close(dataI.getDoubleNext(), -751.4273183345456);
      assert TestUtils.close(dataI.getDoubleNext(), -670.1563183345455);
    } catch (IOException io) {}

  }

  public void testAggCoordVar(NetcdfFile ncfile, int nagg, int start, int incr) {
    Variable time = ncfile.findVariable("valtime");
    assert null != time;
    assert time.getName().equals("valtime");
    assert time.getRank() == 1;
    assert time.getSize() == nagg;
    assert time.getShape()[0] == nagg;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.getCoordinateDimension() == null; // LOOK maybe should be ??

    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == nagg;
      assert data.getShape()[0] == nagg;
      assert data.getElementType() == double.class;

      int count = 0;
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        assert dataI.getIntNext() == start + count*incr : dataI.getIntNext();
        count++;
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

  }

  public void testReadData(NetcdfFile ncfile, int nagg) throws IOException {
    Variable v = ncfile.findVariable("P_sfc");
    assert null != v;
    assert v.getName().equals("P_sfc");
    assert v.getRank() == 3;
    assert v.getShape()[0] == nagg;
    assert v.getShape()[1] == 65;
    assert v.getShape()[2] == 93;
    assert v.getDataType() == DataType.FLOAT;

    assert v.getCoordinateDimension() == null;

    assert v.getDimension(0) == ncfile.findDimension("record");
    assert v.getDimension(1) == ncfile.findDimension("y");
    assert v.getDimension(2) == ncfile.findDimension("x");

      Array data = v.read();
      assert data.getRank() == 3;
      assert data.getShape()[0] == nagg;
      assert data.getShape()[1] == 65;
      assert data.getShape()[2] == 93;
      assert data.getElementType() == float.class;

      /* int [] shape = data.getShape();
      Index tIndex = data.getIndex();
      for (int i=0; i<shape[0]; i++)
       for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++) {
          double val = data.getDouble( tIndex.set(i, j, k));
          // System.out.println(" "+val);
          assert TestUtils.close(val, 100*i + 10*j + k) : val;
        }  */

  }

  public void testReadSlice(NetcdfFile ncfile, int[] origin, int[] shape) throws IOException, InvalidRangeException {

    Variable v = ncfile.findVariable("P_sfc");

      Array data = v.read(origin, shape);
      assert data.getRank() == 3;
      assert data.getSize() == shape[0] * shape[1] * shape[2];
      assert data.getShape()[0] == shape[0] : data.getShape()[0] +" "+shape[0];
      assert data.getShape()[1] == shape[1];
      assert data.getShape()[2] == shape[2];
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

  public void testReadSlice(NetcdfFile ncfile) throws IOException, InvalidRangeException {
    testReadSlice( ncfile, new int[] {0, 0, 0}, new int[] {11, 3, 4} );
    testReadSlice( ncfile, new int[] {0, 0, 0}, new int[] {2, 3, 2} );
    testReadSlice( ncfile, new int[] {5, 0, 0}, new int[] {10, 3, 4} );
    testReadSlice( ncfile, new int[] {10, 0, 0}, new int[] {10, 2, 3} );
   }
}


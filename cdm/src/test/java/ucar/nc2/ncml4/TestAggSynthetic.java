package ucar.nc2.ncml4;

import junit.framework.*;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.ncml4.NcMLReader;
import ucar.nc2.ncml4.TestNcML;

import java.io.IOException;

/** Test netcdf dataset in the JUnit framework. */

public class TestAggSynthetic extends TestCase {

  public TestAggSynthetic( String name) {
    super(name);
  }


  public void test1() throws IOException {
    String filename = "file:./"+TestNcML.topDir + "aggSynthetic.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

    Variable v = ncfile.findVariable("time");
    assert v != null;
    String testAtt = ncfile.findAttValueIgnoreCase(v, "units", null);
    assert testAtt != null;
    assert testAtt.equals("months since 2000-6-16 6:00");

    testDimensions( ncfile);
    testCoordVar( ncfile);
    testAggCoordVar( ncfile);
    testReadData( ncfile);
    testReadSlice( ncfile);

    ncfile.close();
  }

  public void test2() throws IOException {
    String filename = "file:./"+TestNcML.topDir + "aggSynthetic2.xml";
    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

    testDimensions( ncfile);
    testCoordVar( ncfile);
    testAggCoordVar2( ncfile);
    testReadData( ncfile);
    testReadSlice( ncfile);

    ncfile.close();
  }

  public void test3() throws IOException {
     String filename = "file:./"+TestNcML.topDir + "aggSynthetic3.xml";
     NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

     testDimensions( ncfile);
     testCoordVar( ncfile);
     testAggCoordVar3( ncfile);
     testReadData( ncfile);
     testReadSlice( ncfile);

     ncfile.close();
   }

  public void testNoCoord() throws IOException {
     String filename = "file:./"+TestNcML.topDir + "aggSynNoCoord.xml";
     NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

     testDimensions( ncfile);
     testCoordVar( ncfile);
     testAggCoordVarNoCoord( ncfile);
     testReadData( ncfile);
     testReadSlice( ncfile);

    ncfile.close();
   }

  public void testNoCoordDir() throws IOException {
     String filename = "file:./"+TestNcML.topDir + "aggSynNoCoordsDir.xml";
     NetcdfFile ncfile = NcMLReader.readNcML(filename, null);

     testDimensions( ncfile);
     testCoordVar( ncfile);
     testAggCoordVarNoCoordsDir( ncfile);
     testReadData( ncfile);
     testReadSlice( ncfile);

    ncfile.close();
   }

   public void testDimensions(NetcdfFile ncfile) {
    System.out.println("ncfile = \n"+ncfile);

    Dimension latDim = ncfile.findDimension("lat");
    assert null != latDim;
    assert latDim.getName().equals("lat");
    assert latDim.getLength() == 3;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("lon");
    assert null != lonDim;
    assert lonDim.getName().equals("lon");
    assert lonDim.getLength() == 4;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getName().equals("time");
    assert timeDim.getLength() == 3 :  timeDim.getLength();
  }

 public void testCoordVar(NetcdfFile ncfile) {

    Variable lat = ncfile.findVariable("lat");
    assert null != lat;
    assert lat.getName().equals("lat");
    assert lat.getRank() == 1;
    assert lat.getSize() == 3;
    assert lat.getShape()[0] == 3;
    assert lat.getDataType() == DataType.FLOAT;

    assert !lat.isUnlimited();
    assert lat.getDimension(0).equals(ncfile.findDimension("lat"));

    Attribute att = lat.findAttribute("units");
    assert null != att;
    assert !att.isArray();
    assert att.isString();
    assert att.getDataType() == DataType.STRING;
    assert att.getStringValue().equals("degrees_north");
    assert att.getNumericValue() == null;
    assert att.getNumericValue(3) == null;

    try {
      Array data = lat.read();
      assert data.getRank() == 1;
      assert data.getSize() == 3;
      assert data.getShape()[0] == 3;
      assert data.getElementType() == float.class;

      IndexIterator dataI = data.getIndexIterator();
      assert TestUtils.close(dataI.getDoubleNext(), 41.0);
      assert TestUtils.close(dataI.getDoubleNext(), 40.0);
      assert TestUtils.close(dataI.getDoubleNext(), 39.0);
    } catch (IOException io) {}

  }

  public void testAggCoordVar(NetcdfFile ncfile) {

    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");

    try {
      Array data = time.read();

      assert (data instanceof ArrayInt.D1) : data.getClass().getName();
      ArrayInt.D1 dataI = (ArrayInt.D1) data;
      assert dataI.get(0) == 0;
      assert dataI.get(1) == 10;
      assert dataI.get(2) == 99;

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }

  }

  public void testAggCoordVar2(NetcdfFile ncfile) {

    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getName().equals("time");
    assert time.getRank() == 1 : time.getRank();
    assert time.getShape()[0] == 3;
    assert time.getDataType() == DataType.INT;

    assert time.getDimension(0) == ncfile.findDimension("time");

    try {
      Array data = time.read();

      assert (data instanceof ArrayInt);
      IndexIterator dataI = data.getIndexIterator();
      assert dataI.getIntNext() == 0 : dataI.getIntCurrent();
      assert dataI.getIntNext() == 1 : dataI.getIntCurrent();
      assert dataI.getIntNext() == 2 : dataI.getIntCurrent();

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  public void testAggCoordVar3(NetcdfFile ncfile) throws IOException {
      Variable time = ncfile.findVariable("time");
      assert null != time;
      assert time.getName().equals("time");
      assert time.getRank() == 1 : time.getRank();
      assert time.getShape()[0] == 3;
      assert time.getDataType() == DataType.DOUBLE : time.getDataType();

      assert time.getDimension(0) == ncfile.findDimension("time");

      Array data = time.read();

      assert (data instanceof ArrayDouble);
      IndexIterator dataI = data.getIndexIterator();
      double val = dataI.getDoubleNext();
      assert TestAll.closeEnough(val, 0.0) : val;
      assert TestAll.closeEnough(dataI.getDoubleNext(), 10.0) : dataI.getDoubleCurrent();
      assert TestAll.closeEnough(dataI.getDoubleNext(), 99.0) : dataI.getDoubleCurrent();
    }

  public void testAggCoordVarNoCoord(NetcdfFile ncfile) throws IOException {
      Variable time = ncfile.findVariable("time");
      assert null != time;
      assert time.getName().equals("time");
      assert time.getRank() == 1 : time.getRank();
      assert time.getShape()[0] == 3;
      assert time.getDataType() == DataType.STRING : time.getDataType();

      assert time.getDimension(0) == ncfile.findDimension("time");

      Array data = time.read();

      assert (data instanceof ArrayObject);
      IndexIterator dataI = data.getIndexIterator();
      String coordName = (String) dataI.getObjectNext();
      assert coordName.equals("time0.nc") : coordName;
      coordName = (String) dataI.getObjectNext();
      assert coordName.equals("time1.nc") : coordName;
      coordName = (String) dataI.getObjectNext();
      assert coordName.equals("time2.nc") : coordName;
      }

  public void testAggCoordVarNoCoordsDir(NetcdfFile ncfile) throws IOException {
      Variable time = ncfile.findVariable("time");
      assert null != time;
      assert time.getName().equals("time");
      assert time.getRank() == 1 : time.getRank();
      assert time.getShape()[0] == 3;
      assert time.getDataType() == DataType.STRING : time.getDataType();

      assert time.getDimension(0) == ncfile.findDimension("time");

      Array data = time.read();

      assert (data instanceof ArrayObject);
      IndexIterator dataI = data.getIndexIterator();
      String coordName = (String) dataI.getObjectNext();
      assert coordName.equals("time0Dir.nc") : coordName;
      coordName = (String) dataI.getObjectNext();
      assert coordName.equals("time1Dir.nc") : coordName;
      coordName = (String) dataI.getObjectNext();
      assert coordName.equals("time2Dir.nc") : coordName;
      }

  public void testReadData(NetcdfFile ncfile) {

    Variable v = ncfile.findVariable("T");
    assert null != v;
    assert v.getName().equals("T");
    assert v.getRank() == 3;
    assert v.getSize() == 36 : v.getSize();
    assert v.getShape()[0] == 3;
    assert v.getShape()[1] == 3;
    assert v.getShape()[2] == 4;
    assert v.getDataType() == DataType.DOUBLE;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("time");
    assert v.getDimension(1) == ncfile.findDimension("lat");
    assert v.getDimension(2) == ncfile.findDimension("lon");

    try {
      Array data = v.read();
      assert data.getRank() == 3;
      assert data.getSize() == 36;
      assert data.getShape()[0] == 3;
      assert data.getShape()[1] == 3;
      assert data.getShape()[2] == 4;
      assert data.getElementType() == double.class;

      int [] shape = data.getShape();
      Index tIndex = data.getIndex();
      for (int i=0; i<shape[0]; i++)
       for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++) {
          double val = data.getDouble( tIndex.set(i, j, k));
          // System.out.println(" "+val);
          assert TestUtils.close(val, 100*i + 10*j + k) : val;
        }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  public void readSlice(NetcdfFile ncfile, int[] origin, int[] shape) {

    Variable v = ncfile.findVariable("T");

    try {
      Array data = v.read(origin, shape);
      assert data.getRank() == 3;
      assert data.getSize() == shape[0] * shape[1] * shape[2];
      assert data.getShape()[0] == shape[0] : data.getShape()[0] +" "+shape[0];
      assert data.getShape()[1] == shape[1];
      assert data.getShape()[2] == shape[2];
      assert data.getElementType() == double.class;

      Index tIndex = data.getIndex();
      for (int i=0; i<shape[0]; i++)
       for (int j=0; j<shape[1]; j++)
        for (int k=0; k<shape[2]; k++) {
          double val = data.getDouble( tIndex.set(i, j, k));
          //System.out.println(" "+val);
          assert TestUtils.close(val, 100*(i+origin[0]) + 10*j + k) : val;
        }

    } catch (InvalidRangeException io) {
      assert false;
    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    }
  }

  public void testReadSlice(NetcdfFile ncfile) {
    readSlice( ncfile, new int[] {0, 0, 0}, new int[] {3, 3, 4} );
    readSlice( ncfile,new int[] {0, 0, 0}, new int[] {2, 3, 2} );
    readSlice( ncfile,new int[] {2, 0, 0}, new int[] {1, 3, 4} );
    readSlice( ncfile,new int[] {1, 0, 0}, new int[] {2, 2, 3} );
   }

}

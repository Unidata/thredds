package ucar.nc2.ncml;

import junit.framework.TestCase;
import ucar.nc2.*;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.ma2.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class TestNcmlAggSynGrid extends TestCase {

    public TestNcmlAggSynGrid( String name) {
      super(name);
    }

    static GridDataset gds = null;
    String filename = "file:./"+TestNcML.topDir + "aggSynGrid.xml";

    public void setUp() throws IOException {
      if (gds != null) return;
      gds = ucar.nc2.dataset.grid.GridDataset.open(filename);
    }

    public void tearDown() throws IOException {
      if (gds != null) gds.close();
      gds = null;
    }

    public void testGrid() {
      GridDatatype grid = gds.findGridDatatype("T");
      assert null != grid;
      assert grid.getName().equals("T");
      assert grid.getRank() == 3;
      assert grid.getDataType() == DataType.DOUBLE;

      GridCoordSystem gcsys = grid.getGridCoordSystem();
      assert gcsys.getYHorizAxis() != null;
      assert gcsys.getXHorizAxis() != null;
      assert gcsys.getTimeAxis() != null;

      CoordinateAxis1DTime taxis = gcsys.getTimeAxis();
      assert taxis.getDataType() == DataType.STRING : taxis.getDataType();

      List names = taxis.getNames();
      java.util.Date[] dates = taxis.getTimeDates();
      assert dates != null;
      for (int i = 0; i < dates.length; i++) {
        Date d = dates[i];
        ucar.nc2.util.NamedObject name = (ucar.nc2.util.NamedObject) names.get(i);
        System.out.println(name.getName()+" == "+d);
      }
    }

    public void testDimensions() {
      NetcdfFile ncfile = gds.getNetcdfFile();

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
      assert timeDim.getLength() == 3;
    }

   public void testCoordVar() {
      NetcdfFile ncfile = gds.getNetcdfFile();
      Variable lat = ncfile.findVariable("lat");
      assert null != lat;
      assert lat.getName().equals("lat");
      assert lat.getRank() == 1;
      assert lat.getSize() == 3;
      assert lat.getShape()[0] == 3;
      assert lat.getDataType() == DataType.FLOAT;

      assert !lat.isUnlimited();
      assert lat.getCoordinateDimension().equals(ncfile.findDimension("lat"));

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
        assert TestAll.closeEnough(dataI.getDoubleNext(), 41.0);
        assert TestAll.closeEnough(dataI.getDoubleNext(), 40.0);
        assert TestAll.closeEnough(dataI.getDoubleNext(), 39.0);
      } catch (IOException io) {}

    }

    public void utestAggCoordVar() throws IOException {
      NetcdfFile ncfile = gds.getNetcdfFile();
      Variable time = ncfile.findVariable("time");
      assert null != time;
      assert time.getName().equals("time");
      assert time.getRank() == 1 : time.getRank();
      assert time.getShape()[0] == 3;
      assert time.getDataType() == DataType.DOUBLE : time.getDataType();

      assert time.getCoordinateDimension() == ncfile.findDimension("time");

      Array data = time.read();

      assert (data instanceof ArrayDouble);
      IndexIterator dataI = data.getIndexIterator();
      double val = dataI.getDoubleNext();
      assert TestAll.closeEnough(val, 0.0) : val;
      assert TestAll.closeEnough(dataI.getDoubleNext(), 10.0) : dataI.getDoubleCurrent();
      assert TestAll.closeEnough(dataI.getDoubleNext(), 99.0) : dataI.getDoubleCurrent();

    }

    public void testReadData() {
      NetcdfFile ncfile = gds.getNetcdfFile();
      Variable v = ncfile.findVariable("T");
      assert null != v;
      assert v.getName().equals("T");
      assert v.getRank() == 3;
      assert v.getSize() == 36 : v.getSize();
      assert v.getShape()[0] == 3;
      assert v.getShape()[1] == 3;
      assert v.getShape()[2] == 4;
      assert v.getDataType() == DataType.DOUBLE;

      assert v.getCoordinateDimension() == null;

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
            assert TestAll.closeEnough(val, 100*i + 10*j + k) : val;
          }

      } catch (IOException io) {
        io.printStackTrace();
        assert false;
      }
    }

    public void readSlice(int[] origin, int[] shape) {
      NetcdfFile ncfile = gds.getNetcdfFile();
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
            assert TestAll.closeEnough(val, 100*(i+origin[0]) + 10*j + k) : val;
          }

      } catch (InvalidRangeException io) {
        assert false;
      } catch (IOException io) {
        io.printStackTrace();
        assert false;
      }
    }

    public void testReadSlice() {

      readSlice( new int[] {0, 0, 0}, new int[] {3, 3, 4} );
      readSlice( new int[] {0, 0, 0}, new int[] {2, 3, 2} );
      readSlice( new int[] {2, 0, 0}, new int[] {1, 3, 4} );
      readSlice( new int[] {1, 0, 0}, new int[] {2, 2, 3} );
     }

}


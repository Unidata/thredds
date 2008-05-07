package ucar.nc2.ncml_old;

import junit.framework.TestCase;

import java.io.IOException;

import ucar.nc2.*;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.ma2.DataType;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;

public class TestNcmlAggDirDateFormat extends TestCase {

  public TestNcmlAggDirDateFormat( String name) {
    super(name);
  }

  public void testNcmlGrid() throws IOException {
    String filename = "file:./"+TestNcML.topDir + "aggDateFormat.xml";

    GridDataset gds = ucar.nc2.dt.grid.GridDataset.open( filename);
    System.out.println(" TestNcmlAggDirDateFormat.openGrid "+ filename);

    NetcdfFile ncfile = gds.getNetcdfFile();
    testDimensions( ncfile);
    testAggCoordVar( ncfile);
    testReadData( gds);

    gds.close();
  }

  public void testDimensions(NetcdfFile ncfile) {
    Dimension latDim = ncfile.findDimension("y");
    assert null != latDim;
    assert latDim.getName().equals("y");
    assert latDim.getLength() == 1008;
    assert !latDim.isUnlimited();

    Dimension lonDim = ncfile.findDimension("x");
    assert null != lonDim;
    assert lonDim.getName().equals("x");
    assert lonDim.getLength() == 1536;
    assert !lonDim.isUnlimited();

    Dimension timeDim = ncfile.findDimension("time");
    assert null != timeDim;
    assert timeDim.getName().equals("time");
    assert timeDim.getLength() == 12;
  }


  public void testAggCoordVar(NetcdfFile ncfile) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == 12;
    assert time.getShape()[0] == 12;
    assert time.getDataType() == DataType.DOUBLE;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == 12;
    assert data.getShape()[0] == 12;
    assert data.getElementType() == double.class;

    IndexIterator dataI = data.getIndexIterator();
    while (dataI.hasNext())
      System.out.println(" coord="+dataI.getObjectNext());
  }

  public void testReadData(GridDataset gds) throws IOException {
    GridDatatype g = gds.findGridDatatype("IR_WV");
    assert null != g;
    assert g.getName().equals("IR_WV");
    assert g.getRank() == 3;
    assert g.getShape()[0] == 12;
    assert g.getShape()[1] == 1008;
    assert g.getShape()[2] == 1536;
    assert g.getDataType() == DataType.SHORT;

    GridCoordSystem gsys = g.getCoordinateSystem();
    assert gsys.getXHorizAxis() != null;
    assert gsys.getYHorizAxis() != null;
    assert gsys.getTimeAxis() != null;
    assert gsys.getVerticalAxis() == null;
    assert gsys.getProjection() != null;

    Array data = g.readVolumeData(0);
    assert data.getRank() == 2;
    assert data.getShape()[0] == 1008;
    assert data.getShape()[1] == 1536;
    assert data.getElementType() == short.class;
  }


}



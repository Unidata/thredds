package ucar.nc2.ncml4;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.File;
import java.text.ParseException;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;

public class TestOffAggForecastModel extends TestCase {
  private int nruns = 15;
  private int nfore = 11;
  public TestOffAggForecastModel( String name) {
    super(name);
  }

  public void testForecastModel() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "offsite/aggForecastModel.xml";

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);

    testDimensions(ncfile, nruns);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile, nruns);
    testReadData(ncfile, nruns, nfore);
    testReadSlice(ncfile);

    ncfile.close();
  }

  public void testForecastModelExtend() throws IOException, InvalidRangeException {
    String filename = "file:./"+TestNcML.topDir + "offsite/aggForecastModel.xml";
    String newModel = "C:/data/ncmodels/NAM_CONUS_80km_20051212_1200.nc";
    String newModelsave = "C:/data/ncmodels/NAM_CONUS_80km_20051212_1200.nc.save";
    File newModelFile = new File(newModel);
    File newModelFileSave = new File(newModelsave);

    if (newModelFile.exists() && !newModelFileSave.exists()) {
      boolean ok = newModelFile.renameTo(newModelFileSave);
      if (!ok) throw new IOException("cant rename file");
    } else if (!newModelFile.exists() && newModelFileSave.exists()) {
      System.out.println("already renamed");
    } else if (!newModelFile.exists() && !newModelFileSave.exists()) {
      throw new IOException("missing "+newModelFile.getPath());
    } else if (newModelFile.exists() && newModelFileSave.exists()) {
      boolean ok = newModelFile.delete();
      if (!ok) throw new IOException("cant delete file "+newModelFile.getPath());
    }

    NetcdfFile ncfile = NcMLReader.readNcML(filename, null);
    System.out.println(" TestAggForecastModel.open "+ filename);

    testDimensions(ncfile, nruns-1);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile, nruns-1);
    testReadData(ncfile, nruns-1, nfore);
    testReadSlice(ncfile);

    // new file arrives
    boolean ok = newModelFileSave.renameTo(newModelFile);
    if (!ok) throw new IOException("cant rename file");

    ncfile.sync();

    testDimensions(ncfile, nruns);
    testCoordVar(ncfile);
    testAggCoordVar(ncfile, nruns);
    testReadData(ncfile, nruns, nfore);
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

    Dimension timeDim = ncfile.findDimension("runtime");
    assert null != timeDim;
    assert timeDim.getName().equals("runtime");
    assert timeDim.getLength() == nagg : timeDim.getLength();
  }

 public void testCoordVar(NetcdfFile ncfile) throws IOException {

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

      Array data = lat.read();
      assert data.getRank() == 1;
      assert data.getSize() == 65;
      assert data.getShape()[0] == 65;
      assert data.getElementType() == double.class;

      IndexIterator dataI = data.getIndexIterator();
      assert TestUtils.close(dataI.getDoubleNext(), -832.6983183345455);
      assert TestUtils.close(dataI.getDoubleNext(), -751.4273183345456);
      assert TestUtils.close(dataI.getDoubleNext(), -670.1563183345455);

  }

  public void testAggCoordVar(NetcdfFile ncfile, int nagg) {
    Variable time = ncfile.findVariable("runtime");
    assert null != time;
    assert time.getName().equals("runtime");
    assert time.getRank() == 1;
    assert time.getSize() == nagg;
    assert time.getShape()[0] == nagg;
    assert time.getDataType() == DataType.STRING;

    assert time.isCoordinateVariable();

    try {
      Array data = time.read();
      assert data.getRank() == 1;
      assert data.getSize() == nagg;
      assert data.getShape()[0] == nagg;
      assert data.getElementType() == String.class;

      DateFormatter df = new DateFormatter();
      IndexIterator dataI = data.getIndexIterator();
      while (dataI.hasNext()) {
        String d = (String) dataI.getObjectNext();
        assert df.isoDateTimeFormat(d) != null : d;
      }

    } catch (IOException io) {
      io.printStackTrace();
      assert false;
    } catch (ParseException e) {
      e.printStackTrace();
      assert false;
    }

  }

  public void testReadData(NetcdfFile ncfile, int nagg, int nfore) throws IOException {
    Variable v = ncfile.findVariable("P_sfc");
    assert null != v;
    assert v.getName().equals("P_sfc");
    assert v.getRank() == 4;
    assert v.getShape()[0] == nagg;
    assert v.getShape()[1] == nfore : v.getShape()[1];
    assert v.getShape()[2] == 65;
    assert v.getShape()[3] == 93;
    assert v.getDataType() == DataType.FLOAT;

    assert !v.isCoordinateVariable();

    assert v.getDimension(0) == ncfile.findDimension("runtime");
    assert v.getDimension(1) == ncfile.findDimension("record");
    assert v.getDimension(2) == ncfile.findDimension("y");
    assert v.getDimension(3) == ncfile.findDimension("x");

      Array data = v.read();
      assert data.getRank() == 4;
      assert data.getShape()[0] == nagg;
      assert data.getShape()[1] == nfore;
      assert data.getShape()[2] == 65;
      assert data.getShape()[3] == 93;
      assert data.getElementType() == float.class;

  }

  public void testReadSlice(NetcdfFile ncfile, int[] origin, int[] shape) throws IOException, InvalidRangeException {

    Variable v = ncfile.findVariable("P_sfc");

      Array data = v.read(origin, shape);
      assert data.getRank() == 4;
      assert data.getSize() == shape[0] * shape[1] * shape[2] * shape[3];
      assert data.getShape()[0] == shape[0];
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

  public void testReadSlice(NetcdfFile ncfile) throws IOException, InvalidRangeException {
    testReadSlice( ncfile, new int[] {0, 0, 0, 0}, new int[] {14, 11, 3, 4} );
    testReadSlice( ncfile, new int[] {0, 0, 0, 0}, new int[] {4, 2, 3, 2} );
    testReadSlice( ncfile, new int[] {5, 0, 0, 0}, new int[] {3, 10, 3, 4} );
    testReadSlice( ncfile, new int[] {10, 0, 0, 0}, new int[] {4, 10, 2, 3} );
   }
}


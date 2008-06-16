package ucar.nc2;

import java.io.IOException;

import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.Variable;
import junit.framework.TestCase;


public class TestSlice extends TestCase {

  public TestSlice(String name) {
    super(name);
  }

  private static final String NETCDF_FILE = TestLocal.cdmTestDataDir +"testSlice.nc";
  private static final String DATA_VARIABLE = "data";
  private static final int DIM_T = 10;
  private static final int DIM_ALT = 5;
  private static final int DIM_LAT = 123;
  private static final int DIM_LON = 234;

  public void setUp() throws IOException {
    NetcdfFileWriteable file = NetcdfFileWriteable.createNew(NETCDF_FILE);

    Dimension t = new Dimension("t", DIM_T, true);
    Dimension alt = new Dimension("alt", DIM_ALT, true);
    Dimension lat = new Dimension("lat", DIM_LAT, true);
    Dimension lon = new Dimension("lon", DIM_LON, true);
    file.addDimension(null, t);
    file.addDimension(null, alt);
    file.addDimension(null, lat);
    file.addDimension(null, lon);

    Dimension[] dims = {t, alt, lat, lon};
    file.addVariable(DATA_VARIABLE, DataType.FLOAT, dims);
    file.create();
  }

  public void tearDown() {
  }

  private Array createData() {
    ArrayFloat.D4 values = new ArrayFloat.D4(DIM_T, DIM_ALT, DIM_LAT, DIM_LON);
    for (int i = 0; i < DIM_T; i++) {
      for (int j = 0; j < DIM_ALT; j++) {
        for (int k = 0; k < DIM_LAT; k++) {
          for (int l = 0; l < DIM_LON; l++) {
            values.set(i, j, k, l, i + j);
          }
        }
      }
    }
    return values;
  }

  public void testFill() throws IOException, InvalidRangeException {
    NetcdfFileWriteable file = NetcdfFileWriteable.openExisting(NETCDF_FILE);
    file.write(DATA_VARIABLE, createData());
  }

  public void testSlice1() throws IOException, InvalidRangeException {
    NetcdfFile file = NetcdfFile.open(NETCDF_FILE);
    Variable var = file.findVariable(DATA_VARIABLE);
    Variable sliced = var.slice(0, 3);
    sliced.read();

    int[] shape = sliced.getShape();
    assertEquals(3, shape.length);
    assertEquals(DIM_ALT, shape[0]);
    assertEquals(DIM_LAT, shape[1]);
    assertEquals(DIM_LON, shape[2]);

    assertEquals("alt lat lon", sliced.getDimensionsString());
  }

  public void testSlice2() throws IOException, InvalidRangeException {
    NetcdfFile file = NetcdfFile.open(NETCDF_FILE);
    Variable var = file.findVariable(DATA_VARIABLE);
    Variable sliced = var.slice(1, 3);
    sliced.read();

    int[] shape = sliced.getShape();
    assertEquals(3, shape.length);
    assertEquals(DIM_T, shape[0]);
    assertEquals(DIM_LAT, shape[1]);
    assertEquals(DIM_LON, shape[2]);

    assertEquals("t lat lon", sliced.getDimensionsString());
  }

  public void testSlice3() throws IOException, InvalidRangeException {
    NetcdfFile file = NetcdfFile.open(NETCDF_FILE);
    Variable var = file.findVariable(DATA_VARIABLE);
    Variable sliced1 = var.slice(0, 3);
    Variable sliced2 = sliced1.slice(0, 3);

    int[] shape = sliced2.getShape();
    assertEquals(2, shape.length);
    assertEquals(DIM_LAT, shape[0]);
    assertEquals(DIM_LON, shape[1]);

    assertEquals("lat lon", sliced2.getDimensionsString());

    Array org = var.read("3,3,:,:");
    Array data = sliced2.read();
    TestCompare.compareData(org, data);
  }
}

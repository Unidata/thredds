package ucar.nc2.iosp.grib;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NCdumpW;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 1/7/14
 */
@Category(NeedsCdmUnitTest.class)
public class TestScanMode {

  @Test
  // scanMode = 0
  public void testScanMode0() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/S-HSAF-h03_20131214_1312_rom.grb";
    System.out.println("\n\nReading File " + filename);
    GridDataset gds = GridDataset.open(filename);
    GeoGrid grid = gds.findGridByName("Instantaneous_rain_rate");
    assert grid != null;
    assert (grid.getDimensions().size() == 3);

    GridCoordSystem gcs = grid.getCoordinateSystem();
    int[] result = gcs.findXYindexFromCoord(-7, 60.0, null);
    System.out.printf("x,y=%d,%d%n", result[0], result[1]);

    // should be non NAN
    Array data = grid.readDataSlice(0, 0, 714, 1779);
    NCdumpW.printArray(data);

    Index ima = data.getIndex();
    float val = data.getFloat(ima);
    assert Misc.closeEnough(val, 5.0192626E-5);

    gds.close();
  }

  @Test
  public void testEcmwf() throws IOException {  // scanMode 192
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/MSG1-SEVI-MSGCLMK-0100-0100-20060102111500.000000000Z-12774.grb.grb";
    System.out.println("\n\nReading File " + filename);
    GridDataset gds = GridDataset.open(filename);
    GeoGrid grid = gds.findGridByName("Cloud_mask");
    assert grid != null;
    assert (grid.getDimensions().size() == 3);

    GridCoordSystem gcs = grid.getCoordinateSystem();
    int[] result = gcs.findXYindexFromCoord(0, 0, null);
    System.out.printf("x,y=%d,%d%n", result[0], result[1]);

    // should be non NAN
    Array data = grid.readDataSlice(0, 0, result[1], result[0]);
    NCdumpW.printArray(data);

    Index ima = data.getIndex();
    float val = data.getFloat(ima);
    assert Misc.closeEnough(val, 0.0);

    gds.close();
  }

}

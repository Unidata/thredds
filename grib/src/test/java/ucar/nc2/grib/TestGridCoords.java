package ucar.nc2.grib;

import org.junit.Test;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;

/**
 * Test coordinate extraction on grib file.
 *
 * @author caron
 * @since 9/10/12
 */
public class TestGridCoords {

  @Test
  public void testCoordExtract() throws IOException {
    ucar.nc2.dt.grid.GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir+"ft/grid/grib2/TestCoordExtract.grib2");

    GeoGrid grid = dataset.findGridByName("Convective_inhibition_surface");
    assert null != grid;
    GridCoordSystem gcs = grid.getCoordinateSystem();
    assert null != gcs;
    assert grid.getRank() == 3;


    int result[] = gcs.findXYindexFromLatLon(41.3669944444, -91.140575, null);

    System.out.printf("%d %d %n", result[0], result[1]);
    assert result[0] == 538;
    assert result[1] == 97;

    dataset.close();
  }


}

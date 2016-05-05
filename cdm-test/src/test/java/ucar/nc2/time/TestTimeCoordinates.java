package ucar.nc2.time;

import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.List;

/**
 * Describe
 *
 * @author caron
 * @since 7/15/13
 */
public class TestTimeCoordinates {

  //@Test
  // Conventions = "CF-1.0, CW HDF, COARDS"
  public void testConventionsMultiple() throws IOException {
    //String filename = TestDir.cdmUnitTestDir + "ft/grid/ensemble/demeter/MM_cnrm_129_red.ncml";
    String filename = "G:/work/lmoxey/PF5_SST_Climatology_Monthly_1982_2008.ncml";
    GridDataset ncd = GridDataset.open(filename);
    Attribute convAtt = ncd.findGlobalAttributeIgnoreCase("Conventions");
    assert convAtt != null;
    System.out.printf("%s%n", convAtt);

    GeoGrid grid = ncd.findGridByName("sst");
    GridCoordSystem gcs = grid.getCoordinateSystem();
    CoordinateAxis1DTime time = gcs.getTimeAxis1D();
    assert time != null;

    /* Variable v = ncd.getDetailInfo("TIME");
    assert v != null;
    //assert v.isCoordinateVariable();
    assert v instanceof CoordinateAxis1DTime;
    CoordinateAxis1DTime axis = (CoordinateAxis1DTime) v;
    List<CalendarDate> dates = axis.getCalendarDates();
    for (CalendarDate d : dates)  System.out.printf("%s%n", d);  */


    ncd.close();
  }

}

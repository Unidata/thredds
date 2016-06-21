package ucar.nc2.dt.grid;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * Test index to coordinate space mapping for curvilinear grids, e.g., lat(i,j), lon(i,j).
 *
 * Initially written in response to JIRA issue TDS-173 (https://www.unidata.ucar.edu/jira/browse/TDS-173).
 *
 * @author edavis
 * @since 4.1
 */
@Category(NeedsCdmUnitTest.class)
public class CurvilinearGridPointMappingTest
{
  private String datasetLocation = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc";
  private int i = 170;
  private int j = 62;
  private double lat = 43.58750915527344;
  private double lon = -8.184059143066406;

  /**
   * Test GridCoordSystem.getLatLon()
   *
   * @throws IOException  if ...
   * @throws InvalidRangeException  if ...
   */
  @Test
  public void checkGridCoordSystem_getLatLon()
          throws IOException, InvalidRangeException
  {
    int[] origin = new int[] { j, i};
    int[] shape = new int[] {1,1};

    NetcdfFile ncf = NetcdfFile.open( datasetLocation);
    Variable latVar = ncf.findVariable( "lat" );
    Array latArray = latVar.read( origin, shape );
    Variable lonVar = ncf.findVariable( "lon" );
    Array lonArray = lonVar.read( origin, shape );

    double latVal = latArray.getDouble( latArray.getIndex());
    double lonVal = lonArray.getDouble( lonArray.getIndex());

    GridDataset gd = GridDataset.open( datasetLocation );
    GeoGrid gg = gd.findGridByName( "hs" );
    GridCoordSystem gridCoordSys = gg.getCoordinateSystem();
    //gridCoordSys.getXHorizAxis().;
    //gridCoordSys.getYHorizAxis();
    LatLonPoint llPnt = gridCoordSys.getLatLon( 170, 62 );

    Assert.assertEquals( lat, llPnt.getLatitude(), 0.001 );
    Assert.assertEquals( lon, llPnt.getLongitude(), 0.001 );
  }

  /**
   * Test GridCoordSystem.findXYindexFromLatLonBounded()
   * @throws IOException
   */
  @Test
  public void checkGridCoordSystem_findXYindexFromLatLonBounded()
          throws IOException
  {
    GridDataset gd = GridDataset.open( datasetLocation );

    GridDatatype hsGrid = gd.findGridDatatype( "hs" );
    GridCoordSystem coordSys = hsGrid.getCoordinateSystem();
    CalendarDate date = coordSys.getTimeAxis1D().getCalendarDate(0);

    int[] xy = coordSys.findXYindexFromLatLonBounded( lat, lon, null );
    assertEquals( i, xy[0] );
    assertEquals( j, xy[1] );

    GridAsPointDataset hsGridAsPoint = new GridAsPointDataset( Collections.singletonList( hsGrid ));
    GridAsPointDataset.Point point = hsGridAsPoint.readData( hsGrid, date, lat, lon );

    assertEquals( lat, point.lat, 0.001 );
    assertEquals( lon, point.lon, 0.001 );

  }
}

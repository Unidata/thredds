package ucar.nc2.dt.grid;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.geoloc.LatLonPoint;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class CurvilinearGridPointMappingTest
{
  private String datasetLocation = "S:/support/2011-04-21-ncssPointProblem/artabro_20110419.nc";
  private int i = 170;
  private int j = 62;
  private double lat = 43.58750915527344;
  private double lon = -8.184059143066406;

  @Test
  public void checkLatLon()
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

  @Test
  public void stuff()
          throws IOException
  {
    GridDataset gd = GridDataset.open( datasetLocation );

    GridDatatype hsGrid = gd.findGridDatatype( "hs" );
    GridCoordSystem coordSys = hsGrid.getCoordinateSystem();
    Date date = coordSys.getTimeAxis1D().getTimeDate( 0 );

    int[] xy = coordSys.findXYindexFromLatLonBounded( lat, lon, null );
    assertEquals( i, xy[0] );
    assertEquals( j, xy[1] );

    GridAsPointDataset hsGridAsPoint = new GridAsPointDataset( Collections.singletonList( hsGrid ));
    GridAsPointDataset.Point point = hsGridAsPoint.readData( hsGrid, date, lat, lon );

    assertEquals( lat, point.lat, 0.001 );
    assertEquals( lon, point.lon, 0.001 );

  }
}

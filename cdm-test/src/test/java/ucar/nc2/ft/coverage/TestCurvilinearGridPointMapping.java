package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Test index to coordinate space mapping for curvilinear grids, e.g., lat(i,j), lon(i,j).
 * Modified to test Coverage
 */
@Category(NeedsCdmUnitTest.class)
public class TestCurvilinearGridPointMapping {
  private String datasetLocation = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc";
  private String covName = "hs";

  private int i = 170;
  private int j = 62;
  private double lat = 43.58750915527344;
  private double lon = -8.184059143066406;

  /**
   * Test CoverageCoordSys.HorizCoordSys.getLatLon()
   *
   * @throws IOException           if ...
   * @throws InvalidRangeException if ...
   */
  @Test
  public void checkGridCoordSystem_getLatLon() throws IOException, InvalidRangeException {
    int[] origin = new int[]{j, i};
    int[] shape = new int[]{1, 1};

    NetcdfFile ncf = NetcdfFile.open(datasetLocation);
    Variable latVar = ncf.findVariable("lat");
    Array latArray = latVar.read(origin, shape);
    Variable lonVar = ncf.findVariable("lon");
    Array lonArray = lonVar.read(origin, shape);

    double latVal = latArray.getDouble(0);
    double lonVal = lonArray.getDouble(0);
    System.out.printf("%f,%f%n", latVal, lonVal);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(datasetLocation)) {
      Assert.assertNotNull(datasetLocation, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.CURVILINEAR);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      CoverageCoordSys gridCoordSys = cover.getCoordSys();
      Assert.assertNotNull("CoverageCoordSys", gridCoordSys);
      HorizCoordSys hcs = gridCoordSys.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);

      LatLonPoint llPnt = hcs.getLatLon(j, i);
      Assert.assertEquals(lat, llPnt.getLatitude(), lat*Misc.maxReletiveError);
      Assert.assertEquals(lon, llPnt.getLongitude(), lon*Misc.maxReletiveError);
    }
  }

  /**
   * Test GridCoordSystem.findXYindexFromLatLonBounded()
   *
   * @throws IOException
   *
  @Test
  public void checkGridCoordSystem_findXYindexFromLatLonBounded() throws IOException {

    try (CoverageDatasetCollection cc = CoverageDatasetFactory.open(datasetLocation)) {
      Assert.assertNotNull(datasetLocation, cc);
      CoverageDataset gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      Coverage cover = gcs.findCoverage(covName);
      Assert.assertNotNull(covName, cover);

      GridDataset gd = GridDataset.open(datasetLocation);

      GridDatatype hsGrid = gd.findGridDatatype("hs");
      GridCoordSystem coordSys = hsGrid.getCoordinateSystem();
      CalendarDate date = coordSys.getTimeAxis1D().getCalendarDate(0);

      int[] xy = coordSys.findXYindexFromLatLonBounded(lat, lon, null);
      assertEquals(i, xy[0]);
      assertEquals(j, xy[1]);

      GridAsPointDataset hsGridAsPoint = new GridAsPointDataset(Collections.singletonList(hsGrid));
      GridAsPointDataset.Point point = hsGridAsPoint.readData(hsGrid, date, lat, lon);

      assertEquals(lat, point.lat, 0.001);
      assertEquals(lon, point.lon, 0.001);
    }
  } */
}

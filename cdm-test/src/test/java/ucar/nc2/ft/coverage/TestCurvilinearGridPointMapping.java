package ucar.nc2.ft.coverage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test index to coordinate space mapping for curvilinear grids, e.g., lat(i,j), lon(i,j).
 * Modified to test Coverage
 */
@Category(NeedsCdmUnitTest.class)
public class TestCurvilinearGridPointMapping {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
    logger.debug("{}, {}", latVal, lonVal);

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
      Assert2.assertNearlyEquals(lat, llPnt.getLatitude());
      Assert2.assertNearlyEquals(lon, llPnt.getLongitude());
    }
  }
}

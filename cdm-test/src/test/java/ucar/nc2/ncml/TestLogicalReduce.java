package ucar.nc2.ncml;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;

import org.slf4j.LoggerFactory;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

/**
 * Use Ncml logicalReduce to fix datasets with extraneous length 1 dimensions
 *
 * @author caron
 * @since 12/18/13
 * Note: the referenced geoport urls in
 * e.g. 4151-a1h.ncml
 * are currently broken
 */
public class TestLogicalReduce {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  // NcML references "dods://stellwagen.er.usgs.gov/thredds/dodsC/TSdata/ECOHAB_I/4151-a1h.cdf".
  @Category(NeedsExternalResource.class)
  public void testStation() throws IOException {
    String location = TestDir.cdmLocalTestDataDir + "ncml/logicalReduce/4151-a1h.ncml";
    Formatter errlog = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, location, null, errlog);
    assert fdataset != null;
    assert fdataset.getFeatureType() == FeatureType.STATION;

    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;
    for (DsgFeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      assert (fc instanceof PointFeatureCC) : fc.getClass().getName();
      assert fc.getCollectionFeatureType() == FeatureType.STATION;
    }
  }

  @Test
  // NcML references "dods://stellwagen.er.usgs.gov/thredds/dodsC/TSdata/WFAL/8602wh-a.nc"
  @Category(NeedsExternalResource.class)
  public void testStationProfile() throws IOException {
    String location = TestDir.cdmLocalTestDataDir + "ncml/logicalReduce/8602wh-a.ncml";
    Formatter errlog = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, location, null, errlog);
    assert fdataset != null;
    assert fdataset.getFeatureType() == FeatureType.STATION_PROFILE;

    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;
    for (DsgFeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      assert (fc instanceof PointFeatureCCC) : fc.getClass().getName();
      assert fc.getCollectionFeatureType() == FeatureType.STATION_PROFILE;
      assert (fc instanceof StationProfileFeatureCollection);

      int count = checkStationProfileFeatureCollection((StationProfileFeatureCollection) fc);
    }
  }

  int checkStationProfileFeatureCollection(StationProfileFeatureCollection stationProfileFeatureCollection) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;
    stationProfileFeatureCollection.resetIteration();
    while (stationProfileFeatureCollection.hasNext()) {
      ucar.nc2.ft.StationProfileFeature spf = stationProfileFeatureCollection.next();

      CalendarDate last = null;
      spf.resetIteration();
      while (spf.hasNext()) {
        ucar.nc2.ft.ProfileFeature pf = spf.next();
        assert pf.getName() != null;
        assert pf.getTime() != null;

        logger.debug("ProfileFeature = {}", pf.getName());
        if (last != null) assert !last.equals(pf.getTime());
        last = pf.getTime();
        count++;
      }
    }
    long took = System.currentTimeMillis() - start;
    logger.debug("testStationProfileFeatureCollection complete count = {}  full iter took = {} msec", count , took);
    return count;
  }

}

package ucar.nc2.ncml;

import java.io.IOException;
import java.util.Date;
import java.util.Formatter;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NestedPointFeatureCollection;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.StationProfileFeatureCollection;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;

/**
 * Use Ncml logicalReduce to fix datasets with extraneous length 1 dimensions
 *
 * @author caron
 * @since 12/18/13
 */
public class TestLogicalReduce {

  @Test
  // NcML references "dods://geoport.whoi.edu/thredds/dodsC/ECOHAB_I/4151-a1h.cdf".
  @Category(NeedsExternalResource.class)
  public void testStation() throws IOException {
    String location = TestDir.cdmLocalTestDataDir + "ncml/logicalReduce/4151-a1h.ncml";
    Formatter errlog = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, location, null, errlog);
    assert fdataset != null;
    assert fdataset.getFeatureType() == FeatureType.STATION;

    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;
    for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      assert (fc instanceof PointFeatureCollection) || (fc instanceof NestedPointFeatureCollection) : fc.getClass().getName();
      assert fc.getCollectionFeatureType() == FeatureType.STATION;
    }
  }

  @Test
  // NcML references "dods://geoport.whoi.edu/thredds/dodsC/WFAL/8602wh-a.nc"
  @Category(NeedsExternalResource.class)
  public void testStationProfile() throws IOException {
    String location = TestDir.cdmLocalTestDataDir + "ncml/logicalReduce/8602wh-a.ncml";
    Formatter errlog = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, location, null, errlog);
    assert fdataset != null;
    assert fdataset.getFeatureType() == FeatureType.STATION_PROFILE;

    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;
    for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      assert (fc instanceof PointFeatureCollection) || (fc instanceof NestedPointFeatureCollection) : fc.getClass().getName();
      assert fc.getCollectionFeatureType() == FeatureType.STATION_PROFILE;
      assert (fc instanceof StationProfileFeatureCollection);

      int count = checkStationProfileFeatureCollection((StationProfileFeatureCollection) fc, false);
    }
  }

  int checkStationProfileFeatureCollection(StationProfileFeatureCollection stationProfileFeatureCollection, boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;
    stationProfileFeatureCollection.resetIteration();
    while (stationProfileFeatureCollection.hasNext()) {
      ucar.nc2.ft.StationProfileFeature spf = stationProfileFeatureCollection.next();

      Date last = null;
      spf.resetIteration();
      while (spf.hasNext()) {
        ucar.nc2.ft.ProfileFeature pf = spf.next();
        assert pf.getName() != null;
        assert pf.getTime() != null;

        if (show)
          System.out.printf(" ProfileFeature=%s %n", pf.getName());
        if (last != null) assert !last.equals(pf.getTime());
        last = pf.getTime();
        count++;
      }
    }
    long took = System.currentTimeMillis() - start;
    if (show)
      System.out.println(" testStationProfileFeatureCollection complete count= " + count + " full iter took= " + took + " msec");
    return count;
  }

}

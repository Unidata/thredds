package ucar.nc2.ncml;

import org.junit.Test;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.unidata.test.util.TestDir;

import java.io.IOException;
import java.util.Formatter;

/**
 * Use Ncml logialReduce to fix datasets with extranwous length 1 dimensions
 *
 * @author caron
 * @since 12/18/13
 */
public class TestLogicalReduce {

  @Test
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
    }
  }

}

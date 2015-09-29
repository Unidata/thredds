package ucar.nc2.ogc.waterml;

import org.junit.Test;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ogc.MarshallingUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Created by cwardgar on 2014/03/13.
 */
public class NcCollectionTypeTest {
    @Test public void testCreateCollection() throws Exception {
        File pointFile = new File(getClass().getResource("multiStationMultiVar.ncml").toURI());
        try (FeatureDatasetPoint fdPoint = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                FeatureType.STATION, pointFile.getAbsolutePath(), null)) {
            MarshallingUtil.marshalPointDataset(fdPoint, new ByteArrayOutputStream());
        }
    }
}

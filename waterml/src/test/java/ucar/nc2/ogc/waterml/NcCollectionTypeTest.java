package ucar.nc2.ogc.waterml;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ogc.MarshallingUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

/**
 * Created by cwardgar on 2014/03/13.
 */
public class NcCollectionTypeTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test public void testCreateCollection() throws Exception {
        File pointFile = new File(getClass().getResource("multiStationMultiVar.ncml").toURI());
        try (FeatureDatasetPoint fdPoint = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                FeatureType.STATION, pointFile.getAbsolutePath(), null, new Formatter())) {
            MarshallingUtil.marshalPointDataset(fdPoint, new ByteArrayOutputStream());
        }
    }
}

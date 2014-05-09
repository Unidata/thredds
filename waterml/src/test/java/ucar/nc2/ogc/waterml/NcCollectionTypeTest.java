package ucar.nc2.ogc.waterml;

import org.junit.Test;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ogc.MarshallingUtil;
import ucar.nc2.ogc.PointUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Created by cwardgar on 2014/03/13.
 */
public class NcCollectionTypeTest {
    @Test public void testCreateCollection() throws Exception {
        File pointFile = new File(getClass().getResource("multiStationSingleVar.ncml").toURI());
        FeatureDatasetPoint fdPoint = PointUtil.openPointDataset(FeatureType.STATION, pointFile.getAbsolutePath());
        try {
            MarshallingUtil.marshalPointDataset(fdPoint, new ByteArrayOutputStream());
        } finally {
            fdPoint.close();
        }
    }
}

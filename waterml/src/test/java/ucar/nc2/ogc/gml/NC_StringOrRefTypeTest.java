package ucar.nc2.ogc.gml;

import net.opengis.gml.v_3_2_1.StringOrRefType;
import net.opengis.waterml.v_2_0_1.ObjectFactory;
import org.junit.Test;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.PointUtil;
import ucar.nc2.ogc.TestUtil;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import java.io.File;

/**
 * Created by cwardgar on 2014/02/26.
 */
public class NC_StringOrRefTypeTest {
    @Test public void createSamplingFeatureTypeDescription() throws Exception {
        File pointFile = new File(getClass().getResource("/ucar/nc2/ogc/singleTimeSeries.ncml").toURI());
        FeatureDatasetPoint fdPoint = PointUtil.openPointDataset(FeatureType.STATION, pointFile.getAbsolutePath());
        try {
            StationTimeSeriesFeature stationFeat = PointUtil.getSingleStationFeatureFromDataset(fdPoint);

            StringOrRefType description = NC_StringOrRefType.createSamplingFeatureTypeDescription(stationFeat);
            JAXBElement<?> jaxbElement = Factories.GML.createDescription(description);

            Marshaller marshaller = TestUtil.createMarshaller(ObjectFactory.class);
            marshaller.marshal(jaxbElement, System.out);
        } finally {
            fdPoint.close();
        }
    }
}

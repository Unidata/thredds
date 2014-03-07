package ucar.nc2.ogc.waterml;

import net.opengis.waterml.v_2_0_1.MeasureTVPType;
import net.opengis.waterml.v_2_0_1.ObjectFactory;
import org.junit.Test;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.PointUtil;
import ucar.nc2.ogc.TestUtil;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import java.io.File;

/**
 * Created by cwardgar on 2014/03/05.
 */
public class NC_MeasureTVPTypeTest {
    @Test public void testCreateMeasurementTVP() throws Exception {
        File pointFile = new File(getClass().getResource("/ucar/nc2/ogc/singleTimeSeries.ncml").toURI());
        FeatureDatasetPoint fdPoint = PointUtil.openPointDataset(FeatureType.STATION, pointFile.getAbsolutePath());
        try {
            StationTimeSeriesFeature stationFeat = PointUtil.getSingleStationFeatureFromDataset(fdPoint);
            assert stationFeat.hasNext();
            PointFeature pointFeat = stationFeat.next();

            MeasureTVPType measurementTVP = NC_MeasureTVPType.createMeasurementTVP(fdPoint, pointFeat);
            JAXBElement<?> jaxbElement = Factories.WATERML.createMeasurementTVP(measurementTVP);

            Marshaller marshaller = TestUtil.createMarshaller(ObjectFactory.class);
            marshaller.marshal(jaxbElement, System.out);
        } finally {
            fdPoint.close();
        }
    }
}

package ucar.nc2.ogc.waterml;

import net.opengis.waterml.v_2_0_1.MonitoringPointType;
import net.opengis.waterml.v_2_0_1.ObjectFactory;
import org.junit.Test;
import org.xml.sax.SAXException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ogc.NoFactoryFoundException;
import ucar.nc2.ogc.PointUtil;
import ucar.nc2.ogc.TestUtil;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by cwardgar on 2014/02/26.
 */
public class NC_MonitoringPointTypeTest {
    @Test public void init()
            throws URISyntaxException, IOException, NoFactoryFoundException, JAXBException, SAXException {
        File pointFile = new File(getClass().getResource("/ucar/nc2/ogc/singleTimeSeries.ncml").toURI());
        FeatureDatasetPoint fdPoint = PointUtil.openPointDataset(FeatureType.STATION, pointFile.getAbsolutePath());
        try {
            StationTimeSeriesFeature stationFeat = TestUtil.getSingleStationFeatureFromDataset(fdPoint);

            MonitoringPointType monitoringPointType = new NC_MonitoringPointType(stationFeat);
            Object jaxbElement = new ObjectFactory().createMonitoringPoint(monitoringPointType);

            Marshaller marshaller = TestUtil.createMarshaller(ObjectFactory.class);
            marshaller.marshal(jaxbElement, System.out);
        } finally {
            fdPoint.close();
        }
    }
}

package ucar.nc2.ogc.om;

import net.opengis.om.v_2_0_0.OMObservationType;
import net.opengis.waterml.v_2_0_1.ObjectFactory;
import org.junit.Test;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.PointUtil;
import ucar.nc2.ogc.TestUtil;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import java.io.File;

/**
 * Created by cwardgar on 2014/03/05.
 */
public class NC_OMObservationTypeTest {
    @Test public void testCreateObservationType() throws Exception {
        File pointFile = new File(getClass().getResource("/ucar/nc2/ogc/singleTimeSeries.ncml").toURI());
        FeatureDatasetPoint fdPoint = PointUtil.openPointDataset(FeatureType.STATION, pointFile.getAbsolutePath());
        try {
            OMObservationType obsType = NC_OMObservationType.createObservationType(fdPoint);
            JAXBElement<?> jaxbElement = Factories.OM.createOMObservation(obsType);

            Marshaller marshaller = TestUtil.createMarshaller(ObjectFactory.class);
            marshaller.marshal(jaxbElement, System.out);
        } finally {
            fdPoint.close();
        }
    }
}

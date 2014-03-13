package ucar.nc2.ogc.waterml;

import net.opengis.waterml.v_2_0_1.CollectionType;
import net.opengis.waterml.v_2_0_1.ObjectFactory;
import org.junit.Test;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ogc.Factories;
import ucar.nc2.ogc.PointUtil;
import ucar.nc2.ogc.TestUtil;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * Created by cwardgar on 2014/03/13.
 */
public class NC_CollectionTest {
    @Test public void testCreateCollection() throws Exception {
        File pointFile = new File(getClass().getResource("/ucar/nc2/ogc/multiStationSingleVar.ncml").toURI());
        FeatureDatasetPoint fdPoint = PointUtil.openPointDataset(FeatureType.STATION, pointFile.getAbsolutePath());
        try {
            CollectionType collection = NC_Collection.createCollection(fdPoint);
            JAXBElement<?> jaxbElement = Factories.WATERML.createCollection(collection);

            Marshaller marshaller = TestUtil.createMarshaller(ObjectFactory.class);

//            marshaller.marshal(jaxbElement, System.out);

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            marshaller.marshal(jaxbElement, outStream);
        } finally {
            fdPoint.close();
        }
    }
}

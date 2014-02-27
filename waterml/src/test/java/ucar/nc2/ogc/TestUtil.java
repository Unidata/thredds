package ucar.nc2.ogc;

import org.xml.sax.SAXException;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by cwardgar on 2014/02/26.
 */
public abstract class TestUtil {
    public static StationTimeSeriesFeature getSingleStationFeatureFromDataset(FeatureDatasetPoint fdPoint)
            throws IOException {
        String datasetFileName = new File(fdPoint.getNetcdfFile().getLocation()).getName();
        List<FeatureCollection> featCollList = fdPoint.getPointFeatureCollectionList();

        if (featCollList.size() != 1) {
            throw new IllegalArgumentException(String.format("Expected %s to contain 1 FeatureCollection, not %s.",
                    datasetFileName, featCollList.size()));
        } else if (!(featCollList.get(0) instanceof StationTimeSeriesFeatureCollection)) {
            String expectedClassName = StationTimeSeriesFeatureCollection.class.getName();
            String actualClassName = featCollList.get(0).getClass().getName();

            throw new IllegalArgumentException(String.format("Expected %s's FeatureCollection to be a %s, not a %s.",
                    datasetFileName, expectedClassName, actualClassName));
        }

        StationTimeSeriesFeatureCollection stationFeatColl = (StationTimeSeriesFeatureCollection) featCollList.get(0);

        if (!stationFeatColl.hasNext()) {
            throw new IllegalArgumentException(String.format("%s's FeatureCollection is empty.",
                    datasetFileName));
        }

        StationTimeSeriesFeature stationFeat = stationFeatColl.next();

        if (stationFeatColl.hasNext()) {
            throw new IllegalArgumentException(String.format("%s's FeatureCollection contains more than 1 feature.",
                    datasetFileName));
        }

        return stationFeat;
    }

    public static Marshaller createMarshaller(Class<?> objectFactoryClass) throws JAXBException, SAXException {
        Package thePackage = objectFactoryClass.getPackage();
        JAXBContext jaxbContext = JAXBContext.newInstance(thePackage.getName());

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(TestUtil.class.getResource("/waterml/2.0.1/waterml2.xsd"));

        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setEventHandler(new DefaultValidationEventHandler());
        marshaller.setSchema(schema);
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        XmlSchema xmlSchema = thePackage.getAnnotation(XmlSchema.class);
        if (xmlSchema != null && xmlSchema.location() != null && !xmlSchema.location().equals(XmlSchema.NO_LOCATION)) {
            // By default, JAXB seems to ignore XmlSchema.location(). We must manually associate its value with the
            // JAXB_SCHEMA_LOCATION key.
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, xmlSchema.location());
        }

        return marshaller;
    }
}

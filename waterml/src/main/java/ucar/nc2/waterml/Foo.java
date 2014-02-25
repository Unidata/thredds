package ucar.nc2.waterml;

import org.xml.sax.SAXException;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;

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
import java.net.URISyntaxException;

/**
 * Created by cwardgar on 2014/02/21.
 */
public class Foo {
    public static void main(String[] args) throws URISyntaxException, IOException, NoFactoryFoundException {
        File pointFile = new File(Foo.class.getResource("singleTimeSeries.ncml").toURI());
        FeatureDatasetPoint fdPoint = PointUtil.openPointDataset(FeatureType.STATION, pointFile.getAbsolutePath());

        for (FeatureCollection featCol : fdPoint.getPointFeatureCollectionList()) {
            StationTimeSeriesFeatureCollection stationCol = (StationTimeSeriesFeatureCollection) featCol;
            PointFeatureCollectionIterator pointFeatColIter = stationCol.getPointFeatureCollectionIterator(-1);

            while (pointFeatColIter.hasNext()) {
                StationTimeSeriesFeature stationFeat = (StationTimeSeriesFeature) pointFeatColIter.next();
                PointFeatureIterator pointFeatIter = stationFeat.getPointFeatureIterator(-1);

                while (pointFeatIter.hasNext()) {
                    PointFeature pointFeature = pointFeatIter.next();
                    StructureData data = pointFeature.getData();

                    for (StructureMembers.Member member : data.getMembers()) {
                        Array memberData = data.getArray(member);
                        System.out.printf("%s: %s    ", member.getName(), memberData);
                    }

                    System.out.println();
                }
            }
        }


//        ObjectFactory watermlObjectFactory = new ObjectFactory();
//        MeasurementTimeseriesType measurementTimeseriesType = watermlObjectFactory.createMeasurementTimeseriesType();
//        measurementTimeseriesType.setId("test");
//
//        JAXBElement<MeasurementTimeseriesType> measurementTimeseriesElem =
//                watermlObjectFactory.createMeasurementTimeseries(measurementTimeseriesType);
//
//        Marshaller marshaller = createMarshaller(ObjectFactory.class);
//        marshaller.marshal(measurementTimeseriesElem, System.out);
    }

    private static Marshaller createMarshaller(Class<?> objectFactoryClass) throws JAXBException, SAXException {
        Package thePackage = objectFactoryClass.getPackage();
        JAXBContext jaxbContext = JAXBContext.newInstance(thePackage.getName());

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(Foo.class.getResource("/waterml/2.0.1/waterml2.xsd"));

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

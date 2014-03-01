package ucar.nc2.ogc;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

/**
 * Created by cwardgar on 2014/02/26.
 */
public abstract class TestUtil {
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

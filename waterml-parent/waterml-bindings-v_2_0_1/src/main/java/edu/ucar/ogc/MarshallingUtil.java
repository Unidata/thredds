package edu.ucar.ogc;

import net.opengis.waterml_dr.v_2_0_1.ObjectFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

/**
 * Created by cwardgar on 2014/03/17.
 */
public abstract class MarshallingUtil {
    public static final Unmarshaller WATERML_UNMARSHALLER;
    public static final Marshaller   WATERML_MARSHALLER;

    static {
        try {
            Package thePackage = ObjectFactory.class.getPackage();
            JAXBContext jaxbContext = JAXBContext.newInstance(thePackage.getName());

            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(MarshallingUtil.class.getResource(
                    // This will pull in every dependent schema that we'll need for validation, i.e. OM, GML, etc.
                    "/waterml/2.0.1/domain-range-informative/timeseries-domain-range.xsd"
            ));

            WATERML_UNMARSHALLER = jaxbContext.createUnmarshaller();
            WATERML_UNMARSHALLER.setEventHandler(new DefaultValidationEventHandler());
            WATERML_UNMARSHALLER.setSchema(schema);

            WATERML_MARSHALLER = jaxbContext.createMarshaller();
            WATERML_MARSHALLER.setEventHandler(new DefaultValidationEventHandler());
            WATERML_MARSHALLER.setSchema(schema);
            WATERML_MARSHALLER.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            XmlSchema xmlSchema = thePackage.getAnnotation(XmlSchema.class);
            if (xmlSchema != null && xmlSchema.location() != null && !xmlSchema.location().equals(XmlSchema.NO_LOCATION)) {
                // By default, JAXB seems to ignore XmlSchema.location(). We must manually associate its value with the
                // JAXB_SCHEMA_LOCATION key.
                WATERML_MARSHALLER.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, xmlSchema.location());
            }
        } catch (JAXBException | SAXException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private MarshallingUtil() { }
}

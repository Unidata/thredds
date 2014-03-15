package ucar.nc2.ogc.waterml;

import net.opengis.waterml.v_2_0_1.DocumentMetadataType;
import ucar.nc2.ogc.Factories;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;

/**
 * Created by cwardgar on 2014/03/13.
 */
public abstract class NC_DocumentMetadataType {
    private final static DatatypeFactory datatypeFactory;
    static {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // wml2:Collection/wml2:metadata/wml:DocumentMetdata
    public static DocumentMetadataType createDocumentMetadata() {
        DocumentMetadataType documentMetadata = Factories.WATERML.createDocumentMetadataType();

        // gml:id
        String id = generateId();
        documentMetadata.setId(id);

        // wml2:generationDate
        GregorianCalendar gregorianCalendar = new GregorianCalendar();  // Initialized to "now".
        XMLGregorianCalendar generationDate = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
        documentMetadata.setGenerationDate(generationDate);

        return documentMetadata;
    }

    private static int numIds = 0;

    private static String generateId() {
        return DocumentMetadataType.class.getSimpleName() + "." + ++numIds;
    }

    private NC_DocumentMetadataType() { }
}

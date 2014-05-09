package ucar.nc2.ogc;

import net.opengis.waterml.x20.CollectionDocument;
import org.apache.xmlbeans.*;
import org.n52.oxf.xmlbeans.parser.LaxValidationCase;
import org.n52.oxf.xmlbeans.parser.XMLBeansParser;
import org.n52.oxf.xmlbeans.parser.XMLHandlingException;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ogc.waterml.NcCollectionType;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cwardgar on 2014/04/29.
 */
public class MarshallingUtil {
    /////////////////////////////////////////////// ID Creation ///////////////////////////////////////////////

    private static Map<Class<?>, Integer> idsMap = new HashMap<>();

    public static String createIdForType(Class<?> clazz) {
        Integer numIds = idsMap.get(clazz);
        if (numIds == null) {
            numIds = 0;
        }

        String id = clazz.getSimpleName() + "." + ++numIds;
        idsMap.put(clazz, numIds);
        return id;
    }

    public static void resetIds() {
        idsMap.clear();
    }

    /////////////////////////////////////////////// Marshalling ///////////////////////////////////////////////

    public static void marshalPointDataset(FeatureDatasetPoint fdPoint, OutputStream outputStream)
            throws IOException, XMLHandlingException {
        marshalPointDataset(fdPoint, fdPoint.getDataVariables(), outputStream);
    }

    public static void marshalPointDataset(FeatureDatasetPoint fdPoint, List<VariableSimpleIF> dataVars,
            OutputStream outputStream) throws IOException, XMLHandlingException {
        CollectionDocument collectionDoc = CollectionDocument.Factory.newInstance();
        NcCollectionType.initCollection(collectionDoc.addNewCollection(), fdPoint, dataVars);

        resetIds();
        writeObject(collectionDoc, outputStream, true);
    }

    public static void writeObject(XmlObject doc, OutputStream out, boolean validate)
            throws IOException, XMLHandlingException {
        // Add xsi:schemaLocation
        XmlCursor cursor = doc.newCursor();
        if (cursor.toFirstChild()) {
            String location = "http://www.opengis.net/waterml/2.0 http://schemas.opengis.net/waterml/2.0/waterml2.xsd";
            cursor.setAttributeText(new QName("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation"), location);
        }

        if (validate) {
            validate(doc, false);
        }

        doc.save(out, makeOptions());
    }

    private static XmlOptions makeOptions() {
        XmlOptions options = new XmlOptions();

        options.setSaveNamespacesFirst();
        options.setSavePrettyPrint();
        options.setSavePrettyPrintIndent(4);
        options.setSaveAggressiveNamespaces();
        options.setSaveSuggestedPrefixes(getSuggestedPrefixes());

        return options;
    }

    private static Map<String, String> getSuggestedPrefixes() {
        Map<String, String> prefixMap = new HashMap<>();

        prefixMap.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
        prefixMap.put("http://www.w3.org/1999/xlink", "xlink");
        prefixMap.put("http://www.opengis.net/gml/3.2", "gml");
        prefixMap.put("http://www.opengis.net/om/2.0", "om");
        prefixMap.put("http://www.opengis.net/sampling/2.0", "sam");
        prefixMap.put("http://www.opengis.net/samplingSpatial/2.0", "sams");
        prefixMap.put("http://www.opengis.net/swe/2.0", "swe");
        prefixMap.put("http://www.opengis.net/waterml/2.0", "wml2");
        prefixMap.put("http://www.opengis.net/waterml-dr/2.0", "wml2dr");
        prefixMap.put("http://www.opengis.net/gmlcov/1.0", "gmlcov");

        return prefixMap;
    }

    /////////////////////////////////////////////// Validation ///////////////////////////////////////////////
    // The validation methods in org.n52.oxf.xmlbeans.parser.XMLBeansParser are not quite what we want, but we'll
    // borrow lots of code from there.

    // Adapted from XMLBeansParser.validateOnParse().
    public static void validate(XmlObject doc, boolean strict) throws XMLHandlingException {
        XMLBeansParser.getRegisteredLaxValidationCases().clear();
        if (!strict) {
            // The xmlbeans validator has issues with substitution groups, particularly gml:AbstractFeature subtypes.
            XMLBeansParser.registerLaxValidationCase(new GML32AbstractFeatureCase());
        }

        String errorString = createErrorMessage(XMLBeansParser.validate(doc));
        if (errorString.length() > 0) throw new XMLHandlingException(errorString);
    }

    // Direct copy of XMLBeansParser.createErrorMessage().
    private static String createErrorMessage(Collection<XmlError> errors) {
        StringBuilder errorBuilder = new StringBuilder();
        for (XmlError xmlError : errors) {
            errorBuilder.append(xmlError.getMessage()).append(";");
        }

        if (!errors.isEmpty()) {
            errorBuilder.deleteCharAt(errorBuilder.length() - 1);
        }
        return errorBuilder.toString();
    }

    /**
     * Allow substitutions of gml:AbstractFeature. This lax validation lets pass every child, hence
     * it checks not _if_ this is a valid substitution.
     * </p>
     * This is similar to org.n52.oxf.xmlbeans.parser.GMLAbstractFeatureCase, updated for GML 3.2.
     */
    private static class GML32AbstractFeatureCase implements LaxValidationCase {
        private static final Object FEATURE_QN = new QName("http://www.opengis.net/gml/3.2", "AbstractFeature");

        public boolean shouldPass(XmlValidationError xve) {
            return xve.getExpectedQNames() != null && xve.getExpectedQNames().contains(FEATURE_QN);
        }

        public boolean shouldPass(XmlError validationError) {
            if (!(validationError instanceof XmlValidationError)) return false;

            XmlValidationError xve = (XmlValidationError) validationError;
            return shouldPass(xve);
        }
    }

    private MarshallingUtil() { }
}

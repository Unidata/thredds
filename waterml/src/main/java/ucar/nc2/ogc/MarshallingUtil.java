package ucar.nc2.ogc;

import net.opengis.waterml.x20.CollectionDocument;
import org.apache.xmlbeans.*;
import org.joda.time.DateTime;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ogc.waterml.NcCollectionType;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Predicate;

/**
 * Created by cwardgar on 2014/04/29.
 */
public class MarshallingUtil {
    /////////////////////////////////////////////// Testing Util ///////////////////////////////////////////////

    public static DateTime fixedGenerationDate = null;
    public static DateTime fixedResultTime     = null;

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
            throws IOException, XmlException {
        marshalPointDataset(fdPoint, fdPoint.getDataVariables(), outputStream);
    }

    public static void marshalPointDataset(FeatureDatasetPoint fdPoint, List<VariableSimpleIF> dataVars,
            OutputStream outputStream) throws IOException, XmlException {
        resetIds();

        CollectionDocument collectionDoc = CollectionDocument.Factory.newInstance();
        NcCollectionType.initCollection(collectionDoc.addNewCollection(), fdPoint, dataVars);
        writeObject(collectionDoc, outputStream, true);
    }

    public static void writeObject(XmlObject doc, OutputStream out, boolean validate) throws IOException, XmlException {
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

    /**
     * Validates an xml doc. If the validation fails, the exception contains a detailed list of errors.
     *
     * @param doc the document to validate
     * @throws XmlException thrown if the XML is incorrect
     */
    public static void validate(XmlObject doc, boolean strict) throws XmlException {
        // Create an XmlOptions instance and set the error listener.
        Set<XmlError> validationErrors = new HashSet<>();
        XmlOptions validationOptions = new XmlOptions();
        validationOptions.setErrorListener(validationErrors);

        // Validate the XML document
        final boolean isValid = doc.validate(validationOptions);

        // Create Exception with error message if the xml document is invalid
        if (!isValid && !strict) {
            // check if we have special validation cases which could let the message pass anyhow
            validationErrors = filterToOnlySerious(validationErrors);
        }

        if (!validationErrors.isEmpty()) {
            throw new XmlException(createErrorMessage(validationErrors));
        }
    }

    private static Set<XmlError> filterToOnlySerious(Iterable<XmlError> allValidationErrors) {
        Set<XmlError> filteredErrors = new HashSet<>();
        GML32AbstractFeatureLaxCase pred = new GML32AbstractFeatureLaxCase();

        for (final XmlError validationError : allValidationErrors) {
            if (!pred.test(validationError)) {
                filteredErrors.add(validationError);
            }
        }

        return filteredErrors;
    }

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
    private static class GML32AbstractFeatureLaxCase implements Predicate<XmlError> {
        private static final Object FEATURE_QN = new QName("http://www.opengis.net/gml/3.2", "AbstractFeature");

        // Returns 'true' if the error should be ignored because we're doing lax validation.
        @Override public boolean test(XmlError validationError) {
            if (!(validationError instanceof XmlValidationError)) return false;

            XmlValidationError xve = (XmlValidationError) validationError;
            return xve.getExpectedQNames() != null && xve.getExpectedQNames().contains(FEATURE_QN);
        }
    }

    private MarshallingUtil() { }
}

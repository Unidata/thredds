package edu.ucar.ogc;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import net.opengis.waterml_dr.v_2_0_1.ObjectFactory;
import org.custommonkey.xmlunit.*;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.*;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by cwardgar on 2/6/14.
 */
public class RoundTripTest extends XMLTestCase {
    private static final boolean DEBUG = false;

    // xmlunit extends junit 3.8.2. Therefore, we cannot use the junit 4 @Test annotation.
    public void testRoundTrip() throws JAXBException, URISyntaxException, IOException, SAXException, TransformerException {
        Package thePackage = ObjectFactory.class.getPackage();
        JAXBContext jaxbContext = JAXBContext.newInstance(thePackage.getName());

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(getClass().getResource(
                // This will pull in every dependent schema that we'll need for validation, i.e. OM, GML, etc.
                "/waterml/2.0.1/domain-range-informative/timeseries-domain-range.xsd"
        ));

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());
        unmarshaller.setSchema(schema);

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

        // This will find all resources in the "net.opengis" or "org.isotc211" packages that end with ".xml".
        Reflections reflections = new Reflections("net.opengis", "org.isotc211", new ResourcesScanner());
        Pattern pattern = Pattern.compile(".*\\.xml\\b");  // Matches all resources ending in ".xml"
        Set<String> testResources = reflections.getResources(pattern);
        assertTrue("No test resources found.", !testResources.isEmpty());

        for (String testResource : testResources) {
            if (!testResource.startsWith("/")) {
                testResource = "/" + testResource;  // Resource should be absolute.
            }

            System.out.printf("Round-trip testing \"%s\"%n", testResource);
            File controlFile = new File(getClass().getResource(testResource).toURI());
            doRoundTripTest(controlFile, unmarshaller, marshaller);
        }
    }

    // JUnit 3 shouldn't pick this up because it doesn't have the "test" prefix.
    private void doRoundTripTest(File controlFile, Unmarshaller unmarshaller, Marshaller marshaller)
            throws JAXBException, IOException, SAXException, URISyntaxException, TransformerException {
        Object rootElement = unmarshaller.unmarshal(controlFile);

        byte[] marshalledBytes = marshalToByteArray(marshaller, rootElement);
        ByteArrayInputStream testInputStream = new ByteArrayInputStream(marshalledBytes);

        InputSource controlInputSource = new InputSource(new BufferedInputStream(new FileInputStream(controlFile)));
        InputSource testInputSource = new InputSource(testInputStream);
        try {
            // Transform control and test for easier comparison.
            File stylesheet = new File(getClass().getResource("transform.xsl").toURI());
            Document controlDoc = new Transform(controlInputSource, stylesheet).getResultDocument();
            Document testDoc = new Transform(testInputSource, stylesheet).getResultDocument();

            if (DEBUG) {
                System.out.println();
                printDocument(controlDoc, System.out);
                System.out.println();
                printDocument(testDoc, System.out);
                System.out.println();
            }

            XMLUnit.setIgnoreComments(true);
            // These apparently do different things and we need them both.
            XMLUnit.setIgnoreWhitespace(true);
            XMLUnit.setNormalizeWhitespace(true);

            Diff diff = new Diff(controlDoc, testDoc);
            DoubleListDifferenceListener doubleListDifferenceListener = new DoubleListDifferenceListener(1e-7);
            diff.overrideDifferenceListener(doubleListDifferenceListener);

            assertXMLEqual(diff, true);
        } finally {
            controlInputSource.getByteStream().close();
            testInputSource.getByteStream().close();
        }
    }

    private byte[] marshalToByteArray(Marshaller marshaller, Object element) throws JAXBException, IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            marshaller.marshal(element, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } finally {
            byteArrayOutputStream.close();
        }
    }

    public static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        transformer.transform(new DOMSource(doc),
                new StreamResult(new OutputStreamWriter(out, "UTF-8")));
    }

    // Implements comparison of the doubleList datatype. gml has several of these, e.g. "pos" and "posList".
    public static class DoubleListDifferenceListener implements DifferenceListener {
        private final double tolerance;

        public DoubleListDifferenceListener(double tolerance) {
            this.tolerance = tolerance;
        }

        public int differenceFound(Difference difference) {
            if (difference.getId() != DifferenceConstants.ATTR_VALUE_ID &&
                    difference.getId() != DifferenceConstants.TEXT_VALUE_ID) {
                return RETURN_ACCEPT_DIFFERENCE;
            }

            String control = difference.getControlNodeDetail().getValue();
            String test = difference.getTestNodeDetail().getValue();

            if (control == null || test == null) {
                return RETURN_ACCEPT_DIFFERENCE;
            }

            Splitter splitter = Splitter.on(CharMatcher.WHITESPACE).trimResults().omitEmptyStrings();
            List<String> controlTokens = splitter.splitToList(control);
            List<String> testTokens = splitter.splitToList(test);

            for (int i = 0; i < controlTokens.size(); ++i) {
                try {
                    double controlVal = Double.parseDouble(controlTokens.get(i));
                    double testVal = Double.parseDouble(testTokens.get(i));

                    if (Math.abs(controlVal - testVal) > tolerance) {
                        return RETURN_ACCEPT_DIFFERENCE;
                    }
                } catch (NumberFormatException e) {
                    return RETURN_ACCEPT_DIFFERENCE;
                }
            }

            return RETURN_IGNORE_DIFFERENCE_NODES_IDENTICAL;
        }

        public void skippedComparison(Node control, Node test) {
            // This is the default behavior in Diff.skippedComparison() when overrideDifferenceListener() isn't called.
            System.err.println("DifferenceListener.skippedComparison: "
                    + "unhandled control node type=" + control
                    + ", unhandled test node type=" + test);
        }
    }
}

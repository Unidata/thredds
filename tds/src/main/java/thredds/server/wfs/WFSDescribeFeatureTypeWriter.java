package thredds.server.wfs;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the XML for a WFS DescribeFeatureType request
 *
 * @author Stanley Kaymen
 *
 */
public class WFSDescribeFeatureTypeWriter {

    private PrintWriter response;
    private String fileOutput;
    private final String server;
    private final String namespace;
    private List<WFSFeature> featureList;


    public WFSDescribeFeatureTypeWriter(PrintWriter response, String server, String namespace) {
        this.response = response;
        this.fileOutput = "";
        this.server = server;
        this.namespace = namespace;
        this.featureList = new ArrayList<WFSFeature>();
    }

    /**
     * Initiate the response with an XML file with an XML header
     */
    public void startXML() {
        fileOutput += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

        fileOutput += "<schema " + "xmlns:" + WFSController.TDSNAMESPACE + "="  + WFSXMLHelper.encQuotes(namespace) + " " +
                "xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                "xmlns=\"http://www.w3.org/2001/XMLSchema\" xmlns:gml=\"http://www.opengis.net/gml\" " +
                "targetNamespace=\"" + server + "\" elementFormDefault=\"qualified\" " +
                "version=\"0.1\">";
        fileOutput += "<xsd:import namespace=\"http://www.opengis.net/gml\" " +
                "schemaLocation=\"http://schemas.opengis.net/gml/2.1.2/feature.xsd\"/>";
    }

    /**
     * Add a feature to the writer's feature list.
     *
     * @param feature to add
     */

    public void addFeature(WFSFeature feature) {

        featureList.add(feature);
    }

    /**
     * Write the features from the featureList. For each feature, write its attributes
     */
    public void writeFeatures() {

        for (WFSFeature feat : featureList) {
            fileOutput += "<xsd:complexType name=\"" + feat.getTitle() + "\">";
            fileOutput += "<xsd:complexContent>";
            fileOutput += "<xsd:extension base=\"gml:" + feat.getType() + "\">";
            fileOutput += "<xsd:sequence>";

            for (WFSFeatureAttribute attribute : feat.getAttributes()) {
                fileOutput += "<xsd:element name =\"" + attribute.getName() + "\" type=\"" + attribute.getType() + "\"/>";
            }

            fileOutput += "</xsd:sequence>";
            fileOutput += "</xsd:extension>";
            fileOutput += "</xsd:complexContent>";
            fileOutput += "</xsd:complexType>";
            fileOutput += "<xsd:element name =\"" + feat.getName() + "\" type=\"tds:" + feat.getTitle() + "\"/>";

        }
    }

    /**
     * Finish writing the XML file and append it all to the PrintWriter.
     *
     * Once a XML is finished, the WFSDataWriter is no longer usable.
     */
    public void finishXML() {
        fileOutput += "</schema>";
        this.response.append(fileOutput);
        response = null;
        fileOutput = null;
    }
}

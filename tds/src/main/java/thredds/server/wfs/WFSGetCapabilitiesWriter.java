package thredds.server.wfs;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import thredds.server.config.ThreddsConfig;

/**
 * A writer for a WFS compliant Geospatial XML given a print writer.
 * Specifically writes the XML for WFS GetCapabilities requests.
 * 
 * @author wchen@usgs.gov
 *
 */
public class WFSGetCapabilitiesWriter {
	
	private PrintWriter response;
	private String fileOutput;
	private final String server;
	private List<WFSRequestType> operationList;
	private List<WFSFeature> featureList;
	
	/**
	 * Writes the two service sections
	 */
	private void writeServiceInfo() {
		fileOutput += "<ows:ServiceIdentification> "
				+ "<ows:Title>WFS Server on THREDDS</ows:Title> "
				+ "<ows:Abstract>ncWFS uses the NetCDF Java Library to handle WFS requests</ows:Abstract> "
				+ "<ows:ServiceType codeSpace=\"OGC\">WFS</ows:ServiceType> "
				+ "<ows:ServiceTypeVersion>2.0.0</ows:ServiceTypeVersion> "
				+ "<ows:Fees/> "
				+ "<ows:AccessConstraints/> "
		+ "</ows:ServiceIdentification> ";
		
		fileOutput += "<ows:ServiceProvider> "
				+ "<ows:ProviderName>" + ThreddsConfig.get("serverInformation.hostInstitution.name", "hostInstitution") + "</ows:ProviderName> "
				+ "<ows:ProviderSite xlink:href=\"" + ThreddsConfig.get("serverInformation.hostInstitution.webSite", "") +  "\" xlink:type=\"simple\"/> "
				+ "<ows:ServiceContact/> "
				+ "</ows:ServiceProvider> ";
	}
	
	/**
	 * Given the parameter operation name, add the operation
	 * to the operations metadata section.
	 */
	private void writeAOperation(WFSRequestType rt) {
		
		fileOutput += "<ows:Operation name=\"" + rt.toString() + "\"> "
				+ "<ows:DCP> "
				+ "<ows:HTTP> "
				+ "<ows:Get xlink:href=\"" + server + "?\"/> "
				+ "<ows:Post xlink:href=\"" + server + "\"/> "
				+ "</ows:HTTP> "
				+ "</ows:DCP>";
		fileOutput += "</ows:Operation> ";
	}
	
	/**
	 * Writes a constraint OWS element out.
	 * 
	 * @param name of the constraint
	 * @boolean isImplemented or not
	 */
	private void writeAConstraint(String name, boolean isImplemented) {
		
		String defValue;
		
		if(isImplemented) defValue = "TRUE"; else defValue = "FALSE";
		
		fileOutput += "<ows:Constraint name=\"" + name + "\"> "
				+ "<ows:NoValues/> "
				+ "<ows:DefaultValue>" + defValue +"</ows:DefaultValue> "
						+ "</ows:Constraint>";
	}
	
	/**
	 * Writes headers and service sections
	 */
	private void writeHeadersAndSS() {
		fileOutput += "<wfs:WFS_Capabilities xsi:schemaLocation="
				+ WFSXMLHelper.encQuotes("http://www.opengis.net/wfs/2.0 http://schemas.opengis.net/wfs/2.0/wfs.xsd ")
				+ " xmlns:xsi=" + WFSXMLHelper.encQuotes("http://www.w3.org/2001/XMLSchema-instance")
				+ " xmlns:xlink=" + WFSXMLHelper.encQuotes("http://www.w3.org/1999/xlink") 
				+ " xmlns:gml=" + WFSXMLHelper.encQuotes("http://opengis.net/gml")
				+ " xmlns:fes=" + WFSXMLHelper.encQuotes("http://www.opengis.net/fes/2.0")
				+ " xmlns:ogc=" + WFSXMLHelper.encQuotes("http://www.opengis.net/ogc")
				+ " xmlns:ows=" + WFSXMLHelper.encQuotes("http://www.opengis.net/ows/1.1\" xmlns:wfs=\"http://opengis.net/wfs/2.0")
				+ " xmlns=" + WFSXMLHelper.encQuotes("http://www.opengis.net/wfs/2.0")
				+ " version=\"2.0.0\">";
		writeServiceInfo();
	}
	
	/**
	 * Initiate the response with an XML file with an XML header.
	 */
	public void startXML() {
		fileOutput += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		writeHeadersAndSS();
	}
	
	/**
	 * Finish writing the XML file, write the end tag for WFS_Capabilities and append it all to the PrintWriter.
	 * 
	 * Once a XML is finished, the WFSDataWriter is no longer usable.
	 */
	public void finishXML() {
		fileOutput += "</wfs:WFS_Capabilities>";
		this.response.append(fileOutput);
		response = null;
		fileOutput = null;
	}
	
	/**
	 * Given the parameter operation name, add the operation
	 * to the operations metadata section.
	 */
	public void addOperation(WFSRequestType rt) {
		
		this.operationList.add(rt);
	}
	
	/**
	 * Takes all added operations and writes an operations metadata section.
	 */
	public void writeOperations() {
		fileOutput += "<ows:OperationsMetadata> ";
		for(WFSRequestType rt : operationList) {
			writeAOperation(rt);
		}
		
		// Write parameters
		fileOutput += "<ows:Parameter name=\"AcceptVersions\"> "
				+ "<ows:AllowedValues> "
				+ "<ows:Value>2.0.0</ows:Value>"
				+ "</ows:AllowedValues>"
				+ "</ows:Parameter>";
		
		fileOutput += "<ows:Parameter name=\"AcceptFormats\">"
				+ "<ows:AllowedValues> "
				+ "<ows:Value>text/xml</ows:Value>"
				+ "</ows:AllowedValues>"
				+ "</ows:Parameter>";
		
		fileOutput += "<ows:Parameter name=\"Sections\"> "
				+ "<ows:AllowedValues> "
				+ "<ows:Value>ServiceIdentification</ows:Value> "
				+ "<ows:Value>ServiceProvider</ows:Value> "
				+ "<ows:Value>OperationsMetadata</ows:Value> "
				+ "<ows:Value>FeatureTypeList</ows:Value> "
				+ "</ows:AllowedValues>"
				+ "</ows:Parameter>";
		
		fileOutput += "<ows:Parameter name=\"version\"> "
				+ "<ows:AllowedValues> "
				+ "<ows:Value>2.0.0</ows:Value>"
				+ "</ows:AllowedValues>"
				+ "</ows:Parameter>";
		
		// Write constraints
		writeAConstraint("ImplementsBasicWFS", true);
		writeAConstraint("ImplementsTransactionalWFS", false);
		writeAConstraint("ImplementsLockingWFS", false);
		writeAConstraint("KVPEncoding", false);
		writeAConstraint("XMLEncoding", true);
		writeAConstraint("SOAPEncoding", false);
		writeAConstraint("ImplementsInheritance", false);
		writeAConstraint("ImplementsRemoteResolve", false);
		writeAConstraint("ImplementsResultPaging", false);
		writeAConstraint("ImplementsStandardJoins", false);
		writeAConstraint("ImplementsSpatialJoins", false);
		writeAConstraint("ImplementsTemporalJoins", false);
		writeAConstraint("ImplementsFeatureVersioning", false);
		writeAConstraint("ManageStoredQueries", false);
		writeAConstraint("PagingIsTransactionSafe", false);
		writeAConstraint("QueryExpressions", false);
		
		fileOutput += "</ows:OperationsMetadata>";
	}
	
	/**
	 * 
	 */
	public void writeFeatureTypes() {
		fileOutput += "<FeatureTypeList> ";
				
			for(WFSFeature wf : featureList) {
				fileOutput +=
						"<FeatureType> "
					+ 	"<Name>" + wf.getName() + "</Name> "
					+ 	"<Title>" + wf.getTitle() + "</Title> "
					+ 	"<DefaultCRS>" + "urn:ogc:def:crs:EPSG::4326" + "</DefaultCRS>"
					+ 	"<OutputFormats> "
					+ 	"<Format>text/xml; subtype=gml/3.2.1</Format> "
					+ 	"</OutputFormats> "
					+ 	"<ows:WGS84BoundingBox dimensions=\"2\"> "
					+ 	"<ows:LowerCorner>-180 -90</ows:LowerCorner> <ows:UpperCorner>180 90</ows:UpperCorner>"
					+ 	"</ows:WGS84BoundingBox>"
					+	"</FeatureType> ";
			}
		
		fileOutput += "</FeatureTypeList> ";
	}
	
	/**
	 * Add a feature to the writer's feature list.
	 * 
	 * @param feature to add
	 */
	public void addFeature(WFSFeature feature) {
		this.featureList.add(feature);
	}
	
	/**
	 * Opens a WFSDataWriter, writes to the HttpResponse given.
	 * 
	 * @param response to write to
	 * @param server URI
	 */
	public WFSGetCapabilitiesWriter(PrintWriter response, String server){
		this.response = response;
		this.fileOutput = "";
		this.server = server;
		this.operationList = new ArrayList<WFSRequestType>();
		this.featureList = new ArrayList<WFSFeature>();
	}
}

package thredds.server.wfs;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import thredds.core.TdsRequestedDataset;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.simpgeometry.*;
import ucar.nc2.ft2.simpgeometry.adapter.SimpleGeometryCSBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 * Controller for WFS Simple Geometry Web Service
 * 
 * @author wchen@usgs.gov
 *
 */
@Controller
@RequestMapping("/wfs")
public class WFSController extends HttpServlet {

	public static final String TDSNAMESPACE = "tdswfs";
	
	/**
	 * Gets the namespace associated with the WFS Controller and this specific THREDDS server
	 * 
	 * @param hsreq The request which contains the applicable URI of the WFS Controller
	 * @return the namespace value as "SERVER/PATH"
	 */
	public static String getXMLNamespaceXMLNSValue(HttpServletRequest hsreq) {
		return constructServerPath(hsreq) + "geospatial";
	}
	
	/**
	 * Constructs the full server URI from a request
	 * 
	 * @param hsreq The relevant request
	 * @return The URI of the server corresponding to the request
	 */
	public static String constructServerPath(HttpServletRequest hsreq) {
		return hsreq.getScheme() + "://" + hsreq.getServerName() + ":" + hsreq.getServerPort() + "/thredds/wfs/";
	}
	
	/**
	 * Processes GetCapabilities requests.
	 * 
	 * @param out
	 * @return
	 */
	private void getCapabilities(PrintWriter out, HttpServletRequest hsreq, SimpleGeometryCSBuilder sgcs) {
		WFSGetCapabilitiesWriter gcdw = new WFSGetCapabilitiesWriter(out, WFSController.constructServerPath(hsreq));
		gcdw.startXML();
		gcdw.addOperation(WFSRequestType.GetCapabilities); gcdw.addOperation(WFSRequestType.DescribeFeatureType); gcdw.addOperation(WFSRequestType.GetFeature);
		gcdw.writeOperations();
		
		List<String> seriesNames = sgcs.getGeometrySeriesNames();
		
		for(String name : seriesNames) {
			gcdw.addFeature(new WFSFeature(TDSNAMESPACE + ":" + name, name));
		}

		gcdw.writeFeatureTypes();
		gcdw.finishXML();
	}

	private void describeFeatureType(PrintWriter out, HttpServletRequest hsreq, String ftName) {
		WFSDescribeFeatureTypeWriter dftw = new WFSDescribeFeatureTypeWriter(out, WFSController.constructServerPath(hsreq), WFSController.getXMLNamespaceXMLNSValue(hsreq));
		dftw.startXML();
		ArrayList<WFSFeatureAttribute> attributes = new ArrayList<>();
		attributes.add(new WFSFeatureAttribute("geometryInformation", "gml:SurfaceArrayPropertyType"));
		attributes.add(new WFSFeatureAttribute("hruid", "int"));
		attributes.add(new WFSFeatureAttribute("lat", "double"));
		attributes.add(new WFSFeatureAttribute("lon", "double"));
		attributes.add(new WFSFeatureAttribute("catchments_area", "double"));
		attributes.add(new WFSFeatureAttribute("catchments_perimeter", "double"));
		attributes.add(new WFSFeatureAttribute("catchments_veght", "double"));
		attributes.add(new WFSFeatureAttribute("catchments_cov", "double"));
		dftw.addFeature(new WFSFeature(ftName, ftName + "Type", "AbstractFeatureType",attributes));
		dftw.writeFeatures();
		dftw.finishXML();
	}
	
	/**
	 * Processes GetFeature requests.
	 * 
	 * @param out
	 * @return
	 */
	private WFSExceptionWriter getFeature(PrintWriter out, HttpServletRequest hsreq, SimpleGeometryCSBuilder sgcs, String ftName, String fullFtName) {
		
		List<SimpleGeometry> geometryList = new ArrayList<SimpleGeometry>();
		
		GeometryType geoT = sgcs.getGeometryType(ftName);
		
		if(geoT == null){
			return new WFSExceptionWriter("Feature Type of " + fullFtName + " not found.", "GetFeature" , "OperationProcessingFailed");
		}
		
		try {
		
			switch(geoT) {
			case POINT:
				Point pt = sgcs.getPoint(ftName, 0);
				int j = 0;
				while(pt != null) {
					geometryList.add(pt);
					j++;
					pt = sgcs.getPoint(ftName, j);
				}
				break;
				
			case LINE:
				
				Line line = sgcs.getLine(ftName, 0);
				int k = 0;
				while(line != null) {
					geometryList.add(line);
					k++;
					line = sgcs.getLine(ftName, k);
				}	
				
				break;
				
			case POLYGON:
				
				Polygon poly =	sgcs.getPolygon(ftName, 0);
				int i = 0;
				while(poly != null) {
					geometryList.add(poly);
					i++;
					poly = sgcs.getPolygon(ftName, i);
				}
				
				break;
			
			}
		}
		
		// Perhaps will change this to be implemented in the CFPolygon class
		catch(ArrayIndexOutOfBoundsException aout){
			
		}


		WFSGetFeatureWriter gfdw = new WFSGetFeatureWriter(out, WFSController.constructServerPath(hsreq), WFSController.getXMLNamespaceXMLNSValue(hsreq), geometryList, ftName);
		gfdw.startXML();
		gfdw.writeMembers();
		gfdw.finishXML();
		
		return null;
	}
	
	/**
	 * Checks request parameters for errors.
	 * Will send back an XML Exception if any errors are encountered.
	 * 
	 * @param request parameter value
	 * @param version parameter value
	 * @param service parameter value
	 * @param actualFTName parameter value
	 * @return an ExceptionWriter if any errors occurred or null if none occurred
	 */
	private WFSExceptionWriter checkParametersForError(String request, String version, String service, String typeName) {
		// The SERVICE parameter is required. If not specified, is an error (throw exception through XML).
		if(service != null) {
			// For the WFS servlet it must be WFS if not, write out an InvalidParameterValue exception.
			if(!service.equalsIgnoreCase("WFS")) {
					return new WFSExceptionWriter("WFS Server error. SERVICE parameter must be of value WFS.", "service", "InvalidParameterValue");
			}
			
		}
					
		else {
			return new WFSExceptionWriter("WFS server error. SERVICE parameter is required.", "request", "MissingParameterValue");
		}
		
		// The REQUEST Parameter is required. If not specified, is an error (throw exception through XML).
		if(request != null) {
			
			// Only go through version checks if NOT a Get Capabilities request, the VERSION parameter is required for all operations EXCEPT GetCapabilities section 7.6.25 of WFS 2.0 Interface Standard
			if(!request.equalsIgnoreCase(WFSRequestType.GetCapabilities.toString())) {
			
				if(version != null ) {
					// If the version is not failed report exception VersionNegotiationFailed, from OGC Web Services Common Standard section 7.4.1
					
					// Get each part
					String[] versionParts = version.split("\\.");
					
						for(int ind = 0; ind < versionParts.length; ind++) {
						
						// Check if number will throw NumberFormatException if not.
						try {
							Integer.valueOf(versionParts[ind]);
						}
						
						/* Version parameters are only allowed to consist of numbers and periods. If this is not the case then
						 * It qualifies for InvalidParameterException
						 */
						catch (NumberFormatException excep){
							return new WFSExceptionWriter("WFS server error. VERSION parameter consists of invalid characters.", "version", "InvalidParameterValue");
						}
					}
					
					
					/* Now the version parts are all constructed from the parameter
					 * Analyze for correctness. 
					 */
					boolean validVersion = false;
					
					
					// If just number 2 is specified, assume 2.0.0, pass the check
					if(versionParts.length == 1) if(versionParts[0].equals("2")) validVersion = true;
					
					// Two or more version parts specified, make sure it's 2.0.#.#...
					if(versionParts.length >= 2) if(versionParts[0].equals("2") && versionParts[1].equals("0")) validVersion = true;
					
					/* Another exception VersionNegotiationFailed is specified by OGC Web Services Common
					 * for version mismatches. If the version check failed print this exception
					 */
					if(!validVersion){
						return new WFSExceptionWriter("WFS Server error. Version requested is not supported.", null, "VersionNegotiationFailed");
					}
				}
		
				else {
					return new WFSExceptionWriter("WFS server error. VERSION parameter is required.", "request", "MissingParameterValue");

				}
				
				// Last check to see if typenames is specified, must be for GetFeature, DescribeFeatureType
				if(typeName == null) {
					return new WFSExceptionWriter("WFS server error. For the specifed request, parameter typename or typenames must be specified.", request, "MissingParameterValue");
				}
			}
			
			WFSRequestType reqToProc = WFSRequestType.getWFSRequestType(request);
			if(reqToProc == null) return new WFSExceptionWriter("WFS server error. REQUEST parameter is not valid. Possible values: GetCapabilities, "
					+ "DescribeFeatureType, GetFeature", "request", "InvalidParameterValue");
			
		}
		
		else{
			return new WFSExceptionWriter("WFS server error. REQUEST parameter is required.", "request", "MissingParameterValue");
		}
		
		return null;
	}
	
	/**
	 * A handler for WFS based HTTP requests that sends to other request handlers
	 * to handle the request.
	 *
	 * Servlet Path: /wfs/{request}
	 *
	 */
	@RequestMapping("**")
	public void httpHandler(HttpServletRequest hsreq, HttpServletResponse hsres) {
		try {
			
			PrintWriter wr = hsres.getWriter();
			List<String> paramNames = new LinkedList<String>();
			Enumeration<String> paramNamesE = hsreq.getParameterNames();
			while(paramNamesE.hasMoreElements()) paramNames.add(paramNamesE.nextElement());
			
			// Prepare parameters
			String request = null;
			String version = null;
			String service = null;
			String typeNames = null;
			String datasetReqPath = null;
			String actualPath = null;
			String actualFTName = null;
			NetcdfDataset dataset = null;
			
			if(hsreq.getServletPath().length() > 4) {
				datasetReqPath = hsreq.getServletPath().substring(4, hsreq.getServletPath().length());
			}
			
			actualPath = TdsRequestedDataset.getLocationFromRequestPath(datasetReqPath);
			
			if(actualPath != null) dataset = NetcdfDataset.openDataset(actualPath);
			else return;
			
			List<CoordinateSystem> csList = dataset.getCoordinateSystems();
			SimpleGeometryCSBuilder cs = new SimpleGeometryCSBuilder(dataset, csList.get(0), null);
			
			
			/* Look for parameter names to assign values
			 * in order to avoid casing issues with parameter names (such as a mismatch between reQUEST and request and REQUEST).
			 */
			for(String paramName : paramNames) {
				if(paramName.equalsIgnoreCase("REQUEST")) {
					request = hsreq.getParameter(paramName);
				}
				
				if(paramName.equalsIgnoreCase("VERSION")) {
					version = hsreq.getParameter(paramName);
				}
				
				
				if(paramName.equalsIgnoreCase("SERVICE")) {
					service = hsreq.getParameter(paramName);
				}
				
				if(paramName.equalsIgnoreCase("TYPENAMES") || paramName.equalsIgnoreCase("TYPENAME")) {
					typeNames = hsreq.getParameter(paramName);
					
					// Remove namespace header for getFeature
					if(typeNames != null) if(typeNames.length() > TDSNAMESPACE.length()) {
						actualFTName = typeNames.substring(TDSNAMESPACE.length() + 1, typeNames.length());
					}
				}
			}
			
			WFSExceptionWriter paramError = checkParametersForError(request, version, service, typeNames);
			WFSExceptionWriter requestProcessingError = null;
			
			// If parameter checks all pass launch the request
			if(paramError == null) {
				
				WFSRequestType reqToProc = WFSRequestType.getWFSRequestType(request);
				
				switch(reqToProc) {
					case GetCapabilities:
						getCapabilities(wr, hsreq, cs);
					break;
					
					case DescribeFeatureType:
						describeFeatureType(wr, hsreq, actualFTName);
						
					break;
					
					case GetFeature:
						requestProcessingError = getFeature(wr, hsreq, cs, actualFTName, typeNames);
					break;
				}	
				
			}
			
			// Parameter checks did not all pass, print the error and return
			else {
				paramError.write(hsres);
				return;
			}
			
			/* Specifically writes out exceptions that were incurred
			 * while processing requests.
			 */
			if(requestProcessingError != null){
				requestProcessingError.write(hsres);
				return;
			}
		}
		
		catch(IOException io) {	
			throw new RuntimeException("The writer may not have been able to been have retrieved"
					+ " or the requested dataset was not found", io);
		}
	}
}

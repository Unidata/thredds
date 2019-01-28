package thredds.server.wfs;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

/**
 * A simple XML writer for XML-based WFS exceptions.
 * 
 * @author wchen@usgs.gov
 *
 */
public class WFSExceptionWriter {
	private final String text;
	private final String ExceptionCode;
	private final String locator;
	
	/**
	 * Given the information on construction, writes the necessary exception information.
	 * 
	 * @param hsr the Servlet Response to write to
	 * @throws IOException
	 */
	public void write(HttpServletResponse hsr) throws IOException{
		PrintWriter xmlResponse = hsr.getWriter();
		
		xmlResponse.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		xmlResponse.append("<ows:ExceptionReport xml:lang=\"en-US\" xsi:schemaLocation=\"http://www.opengis.net/ows/1.1"
				+ " http://schemas.opengis.net/ows/1.1.0/owsExceptionReport.xsd\" version=\"2.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\""
				+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
		xmlResponse.append("<ows:Exception ");
		if(locator != null) xmlResponse.append("locator=\"" + locator + "\" ");
		xmlResponse.append("exceptionCode=\"" + ExceptionCode + "\">");
		xmlResponse.append("<ows:ExceptionText>" + text + "</ows:ExceptionText>");
		xmlResponse.append("</ows:Exception>");
		xmlResponse.append("</ows:ExceptionReport>");
	}
	
	/**
	 * Creates a new OWS Exception Report based on a text and an exception code.
	 * 
	 * Some exceptions do not specify locators. For these exceptions send in null for the locator.
	 * 
	 * @param text the text associated with the exception
	 * @param locator the locator associated with the exception
	 * @param ExceptionCode the standardized exception code
	 * @param hsr http response to write to
	 */
	public WFSExceptionWriter(String text, String locator, String ExceptionCode) {
		this.text = text;
		this.locator = locator;
		this.ExceptionCode = ExceptionCode;
	}
}

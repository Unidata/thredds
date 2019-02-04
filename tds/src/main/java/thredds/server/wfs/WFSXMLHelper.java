package thredds.server.wfs;

/**
 * General XML writing utilities.
 * 
 * @author wchen@usgs.gov
 *
 */
public class WFSXMLHelper {

	/**
	 * An XML escaped ampersand.
	 */
	public static final String AMPERSAND = "&amp;";
	
	/**
	 * Encloses a string in quotes
	 * 
	 * @param toEnclose the string to enclose
	 * @return the string toEnclose in quotes
	 */
	public static String encQuotes(String toEnclose) {
		return "\"" + toEnclose + "\"";
	}
	
}

/*
 * (c) 1998-2016 University Corporation for Atmospheric Research/Unidata
 */

package thredds.server.ncss.format;

import thredds.util.ContentType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum SupportedFormat {
	
	CSV_STREAM("csv", true, ContentType.csv.toString(), "csv", "text/csv"),
	CSV_FILE("csv_file", false,  ContentType.csv.toString(), "csv_file"  ),

	GEOCSV_STREAM("geocsv", true, ContentType.csv.toString(), "geocsv", "text/geocsv"),
	GEOCSV_FILE("geocsv_file", false,  ContentType.csv.toString(), "geocsv_file"  ),

	XML_STREAM("xml", true, ContentType.xml.toString(), "xml"),
	XML_FILE("xml_file", false, ContentType.xml.toString(), "xml_file"),
	
	NETCDF3("netcdf", false,  ContentType.netcdf.toString(), "netcdf"),
	NETCDF4("netcdf4", false,  ContentType.netcdf4.toString(), "netcdf4"),
    // NETCDF4EXT("netcdf4ext", false, ContentType.netcdf4.toString(), "netcdf4ext"),

	JSON("json", false, ContentType.json.toString(), "json", "geojson"),
	WKT("wkt", false, ContentType.text.toString(), "wkt"),

  WATERML2("waterml2", true, ContentType.xml.toString(), "waterml2");
	
	/*
	 * First alias is used as content-type in the http headers
	 */
	private final List<String> aliases;
	private final String formatName;
	private final boolean isStream;

  private SupportedFormat(String formatName, boolean isStream, String...aliases ){
		this.formatName=formatName;
		this.isStream = isStream;
		List<String> aliasesList = new ArrayList<>();
    Collections.addAll(aliasesList, aliases);
		this.aliases = Collections.unmodifiableList(aliasesList);
	}
	
	public String getFormatName(){
		return formatName;
	}

	
	public List<String> getAliases(){
		return aliases;
	}

  public boolean isAlias(String want){
 		for (String have : aliases)
      if (have.equalsIgnoreCase(want)) return true;
    return false;
 	}

	//The first item in the aliases is the content type for the responses
	public String getResponseContentType(){
		return aliases.get(0);
	}

	public boolean isStream(){
		return isStream;
	}
	
    public boolean isBinary() {
        return formatName.equals("netcdf") || formatName.equals("netcdf4");
    }

    public boolean isText() {
        return !isBinary();
    }
}

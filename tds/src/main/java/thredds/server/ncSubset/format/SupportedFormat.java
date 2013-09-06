package thredds.server.ncSubset.format;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum SupportedFormat{
	
	CSV_STREAM("CSV", true, "text/plain", "csv"  ),
	CSV_FILE("CSV", false,  "text/csv"  ),
	
	XML_STREAM("XML", true, "application/xml", "xml"),
	XML_FILE("XML", false, "text/xml"),
	
	NETCDF3("NETCDF3", false,  "application/x-netcdf","netcdf"),	
	NETCDF4("NETCDF4", false,  "application/x-netcdf4" , "netcdf4"),
	JSON("JSON", false, "application/json", "json", "geojson"),
	WKT("WKT", false, "text/plain", "wkt");

	
	/*
	 * First alias is used as content-type in the http headers
	 */
	private final List<String> aliases;
	private final String formatName;
	private final boolean isStream;
	//private final List<SupportedOperation> operations;
	
	SupportedFormat(String formatName, boolean isStream, String...aliases ){
		this.formatName=formatName;
		//this.operations = operations;	 
		this.isStream = isStream;
		List<String> aliasesList = new ArrayList<String>();
		for(String alias : aliases){
			aliasesList.add(alias);
		}
		this.aliases = Collections.unmodifiableList(aliasesList);
	}
	
	public String getFormatName(){
		return formatName;
	}
	
/*	public boolean isAvailable(){
		return available;
	}*/
	
	public List<String> getAliases(){
		return aliases;
	}
	
	/*List<SupportedOperation> getOperations(){
		return operations;
	}*/
	
	//The first item in the aliases is the content type for the responses
	public String getResponseContentType(){
		return aliases.get(0);
	}

	
	public boolean isStream(){
		return isStream;
	}
	
		
}

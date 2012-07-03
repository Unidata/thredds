package thredds.server.ncSubset.controller;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum SupportedFormat{
	
		
	CSV("CSV","text/plain", "text/csv", "csv"  ),
	XML("XML", "application/xml", "text/xml", "xml"),
	NETCDF("NETCDF","application/x-netcdf","netcdf");
	
	
	private final List<String> aliases;
	private final String formatName;
	//private final List<SupportedOperation> operations;
	
	SupportedFormat(String formatName, String...aliases ){
		this.formatName=formatName;
		//this.operations = operations;
		 
		
		List<String> aliasesList = new ArrayList<String>();
		for(String alias : aliases){
			aliasesList.add(alias);
		}
		this.aliases = Collections.unmodifiableList(aliasesList);
	}
	
	public String getFormatName(){
		return formatName;
	}
	
	List<String> getAliases(){
		return aliases;
	}
	
	/*List<SupportedOperation> getOperations(){
		return operations;
	}*/
	
	//The first item in the aliases is the content type for the responses
	String getResponseContentType(){
		return aliases.get(0);
	}

	
		
}

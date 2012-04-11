package thredds.server.ncSubset.controller;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum SupportedFormat{
	
		
	CSV("CSV",Collections.unmodifiableList( Arrays.asList(new SupportedOperation[] {SupportedOperation.POINT_REQUEST}) ) ,"text/csv", "csv"  ),
	XML("XML",Collections.unmodifiableList( Arrays.asList(new SupportedOperation[] {SupportedOperation.POINT_REQUEST}) ), "application/xml", "text/xml", "xml"),
	NETCDF("NETCDF",Collections.unmodifiableList( Arrays.asList(new SupportedOperation[] {SupportedOperation.POINT_REQUEST,SupportedOperation.GRID_REQUEST }) ), "application/x-netcdf","netcdf");
	
	
	private final List<String> aliases;
	private final String formatName;
	private final List<SupportedOperation> operations;
	
	SupportedFormat(String formatName,List<SupportedOperation> operations, String...aliases ){
		this.formatName=formatName;
		this.operations = operations;
		 
		
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
	
	List<SupportedOperation> getOperations(){
		return operations;
	}
	
	//The first item in the aliases is the content type for the responses
	String getResponseContentType(){
		return aliases.get(0);
	}

	
	public static SupportedFormat isSupportedFormat(String format, SupportedOperation operation){
		
		boolean found = false; 
		SupportedFormat[] sf = SupportedFormat.values();
		int len = sf.length;
		int cont =0;
		while(!found  && cont < len){
			
			if( sf[cont].getOperations().contains(operation) ){			
				List<String> aliases=  sf[cont].getAliases();
				if(aliases.contains(format)) found= true;
			}	
			cont++;		
		}

		if(found ) return sf[cont-1];
		
		return null;
	}
		
}

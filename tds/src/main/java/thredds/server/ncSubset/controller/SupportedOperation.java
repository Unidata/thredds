package thredds.server.ncSubset.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * netcdf subset service allows 3 kinds of operations
 *  
 * @author mhermida
 *
 */
enum SupportedOperation {
	
	DATASET_INFO_REQUEST("Dataset info request", Collections.unmodifiableList(Arrays.asList(new SupportedFormat[]{SupportedFormat.XML })),SupportedFormat.XML),
	POINT_REQUEST("Point data request", Collections.unmodifiableList(Arrays.asList(new SupportedFormat[]{SupportedFormat.XML, SupportedFormat.CSV, SupportedFormat.NETCDF})),SupportedFormat.XML),
	GRID_REQUEST("Grid data request",Collections.unmodifiableList(Arrays.asList(new SupportedFormat[]{SupportedFormat.NETCDF})),SupportedFormat.NETCDF);
	
	private final String operationName; 
	private final List<SupportedFormat> supportedFormats;
	private final SupportedFormat defaultFormat;
	
	SupportedOperation(String operationName, List<SupportedFormat> supportedFormats, SupportedFormat defaultFormat){
		
		this.operationName = operationName;
		this.supportedFormats = supportedFormats;
		this.defaultFormat = defaultFormat;

	}

	String getOperation(){
		return operationName;
	}
	
	List<SupportedFormat> getSupportedFormats(){
		return supportedFormats;
	}

	SupportedFormat getDefaultFormat(){
		return defaultFormat;
	}
	
	public static SupportedFormat isSupportedFormat(String format, SupportedOperation operation){
	
		/*boolean found = false; 
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

		if(found ) return sf[cont-1];*/
		
		List<SupportedFormat> supportedFormats = operation.getSupportedFormats(); 
		int len = supportedFormats.size();
		int cont =0;
		boolean found=false;
		
		while (!found && cont < len) {
			if( supportedFormats.get(cont).getAliases().contains(format) ) found = true;
			cont++;
		} 
 	
		if( cont < len ) return supportedFormats.get(cont-1); 
		
		return operation.defaultFormat;
	}	
}

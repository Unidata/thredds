package thredds.server.ncSubset.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;

/**
 * netcdf subset service allows 3 kinds of operations
 *  
 * @author mhermida
 *
 */
enum SupportedOperation {
	
	DATASET_INFO_REQUEST("Dataset info request", Collections.unmodifiableList(Arrays.asList(new SupportedFormat[]{SupportedFormat.XML })),SupportedFormat.XML),
	POINT_REQUEST("Grid as Point data request", Collections.unmodifiableList(Arrays.asList(new SupportedFormat[]{SupportedFormat.XML, SupportedFormat.CSV, SupportedFormat.NETCDF})),SupportedFormat.XML),
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
	
	public static SupportedFormat isSupportedFormat(String format, SupportedOperation operation) throws UnsupportedResponseFormatException{
		
		List<SupportedFormat> supportedFormats = operation.getSupportedFormats();
		
		int len = supportedFormats.size();
		int cont =0;
		boolean found=false;
		
		while (!found && cont < len) {
			if( supportedFormats.get(cont).getAliases().contains(format) ) found = true;
			cont++;
		} 
 	
		if( found ) return supportedFormats.get(cont-1); 
		
		throw new UnsupportedResponseFormatException("Format "+format+" is not supported for "+operation.getOperation());
	}	
}

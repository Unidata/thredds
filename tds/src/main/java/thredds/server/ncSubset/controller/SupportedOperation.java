/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.ncSubset.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import thredds.server.config.FormatsAvailabilityService;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;
import thredds.server.ncSubset.format.SupportedFormat;

/**
 * netcdf subset service allows 3 kinds of operations
 *  
 * @author mhermida
 *
 */
enum SupportedOperation {
	
	DATASET_INFO_REQUEST("Dataset info request", Collections.unmodifiableList(Arrays.asList(new SupportedFormat[]{SupportedFormat.XML_FILE })),SupportedFormat.XML_FILE),
	DATASET_BOUNDARIES_REQUEST("Dataset grid boundaries request", Collections.unmodifiableList(Arrays.asList(new SupportedFormat[]{SupportedFormat.JSON, SupportedFormat.WKT })),SupportedFormat.WKT),
	POINT_REQUEST("Grid as Point data request", Collections.unmodifiableList(Arrays.asList(new SupportedFormat[]{SupportedFormat.XML_STREAM, SupportedFormat.XML_FILE, SupportedFormat.CSV_STREAM, SupportedFormat.CSV_FILE , SupportedFormat.NETCDF3, SupportedFormat.NETCDF4})),SupportedFormat.XML_STREAM),
	GRID_REQUEST("Grid data request",Collections.unmodifiableList(Arrays.asList(new SupportedFormat[]{SupportedFormat.NETCDF3, SupportedFormat.NETCDF4})),SupportedFormat.NETCDF3);
	
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
		
		if(format == null || format.equals("")){
			return operation.getDefaultFormat();
		}
		
		List<SupportedFormat> supportedFormats = operation.getSupportedFormats();
		
		int len = supportedFormats.size();
		int cont =0;
		boolean found=false;
		
		while (!found && cont < len) {
			//if( supportedFormats.get(cont).getAliases().contains(format) && supportedFormats.get(cont).isAvailable()  ) found = true;
			if( supportedFormats.get(cont).getAliases().contains(format) &&  FormatsAvailabilityService.isFormatAvailable(supportedFormats.get(cont))) found = true;
			cont++;
		} 
 	
		if( found ) return supportedFormats.get(cont-1); 
		
		throw new UnsupportedResponseFormatException("Format "+format+" is not supported for "+operation.getOperation());
	}	
}

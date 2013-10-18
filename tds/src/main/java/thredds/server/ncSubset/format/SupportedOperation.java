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

package thredds.server.ncSubset.format;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import thredds.server.config.FormatsAvailabilityService;
import thredds.server.ncSubset.exception.UnsupportedResponseFormatException;

import static thredds.server.ncSubset.format.SupportedFormat.*;

/**
 * the various operations for netcdf subset service
 *  
 * @author mhermida
 *
 */
public enum SupportedOperation {
	
	DATASET_INFO_REQUEST("Dataset info request", XML_FILE),
	DATASET_BOUNDARIES_REQUEST("Dataset grid boundaries request", WKT, JSON),
	GRID_REQUEST("Grid data request", NETCDF3, NETCDF4),
  POINT_REQUEST("Point data request", XML_STREAM, XML_FILE, CSV_STREAM, CSV_FILE, NETCDF3, NETCDF4);

	private final String operationName; 
	private final List<SupportedFormat> supportedFormats;

  private SupportedOperation(String operationName, SupportedFormat... formats){
		this.operationName = operationName;
		this.supportedFormats = Collections.unmodifiableList( Arrays.asList(formats));
    assert this.supportedFormats.size() > 0;
	}

	public String getName(){
		return operationName;
	}
	
  public List<SupportedFormat> getSupportedFormats(){
		return supportedFormats;
	}

  public SupportedFormat getDefaultFormat(){
		return supportedFormats.get(0);
	}
	
	public SupportedFormat getSupportedFormat(String want) throws UnsupportedResponseFormatException{
		
		if (want == null || want.equals("")){
			return getDefaultFormat();
		}

    for (SupportedFormat f : getSupportedFormats()) {
      if (f.isAlias(want) && FormatsAvailabilityService.isFormatAvailable(f))
        return f;
    }
		
		/*
				List<SupportedFormat> supportedFormats = operation.getSupportedFormats();

		int len = supportedFormats.size();
		int cont =0;
		boolean found=false;

		while (!found && cont < len) {
			//if( supportedFormats.get(cont).getAliases().contains(format) && supportedFormats.get(cont).isAvailable()  ) found = true;
			if( supportedFormats.get(cont).getAliases().contains(format) &&  FormatsAvailabilityService.isFormatAvailable(supportedFormats.get(cont))) found = true;
			cont++;
		} 
 	
		if( found ) return supportedFormats.get(cont-1);  */
		
		throw new UnsupportedResponseFormatException("Format "+want+" is not supported for "+getName());
	}	
}

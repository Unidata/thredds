/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.ncss.exception;

public class FeaturesNotFoundException extends NcssException {
	public FeaturesNotFoundException(String message){
		super(message);
	}
	public FeaturesNotFoundException(String message, Exception cause){
		super(message, cause);
	}	
}

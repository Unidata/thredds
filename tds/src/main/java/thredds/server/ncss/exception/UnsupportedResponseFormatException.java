/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.ncss.exception;

/**
 * 
 * UnsupportedResponseFormatException is thrown by the NcssControllers if some unexpected 'accept' value is in the request params.
 * 
 * @author mhermida
 *
 */
public class UnsupportedResponseFormatException extends NcssException {

	public UnsupportedResponseFormatException(String message){
		super(message);
	}
		
}

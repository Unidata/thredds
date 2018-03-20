/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.exception;

public class RequestTooLargeException extends RuntimeException {

	public RequestTooLargeException(String message){
		super(message);
	}
	
	public RequestTooLargeException(String message, Exception cause){
		super(message, cause);
	}	

}

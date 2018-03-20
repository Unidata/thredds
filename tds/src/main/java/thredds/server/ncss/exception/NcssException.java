/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.ncss.exception;

public class NcssException extends Exception {
	public NcssException(String message){
		super(message);
	}
	public NcssException(String message, Exception cause){
		super(message, cause);
	}
}

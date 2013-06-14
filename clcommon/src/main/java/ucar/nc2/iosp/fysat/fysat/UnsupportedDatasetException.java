/**
 * 
 */
package ucar.nc2.iosp.fysat;

import java.io.IOException;

/**
 * @author Hurricane
 *
 */
public class UnsupportedDatasetException
extends IOException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructs an UnsupportedEncodingException without a detail message.
	 */
	public UnsupportedDatasetException() {
	    super();
	}
	
	/**
	 * Constructs an UnsupportedEncodingException with a detail message.
	 * @param s Describes the reason for the exception.
	 */
	public UnsupportedDatasetException(String s) {
	    super(s);
	}
}

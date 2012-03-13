package thredds.server.ncSubset.exception;

/**
 * An unsupported operation is thrown when the result data could be inconsistent because 
 * the data readers are yet not able to create the appropriate data structure for all the variables requested.   
 * 
 * @author mhermida
 *
 */
public class UnsupportedOperationException extends NcssException {

	/**
	 * Creates an UnsupportedOperationException with a specific message
	 * @param message
	 */
	public UnsupportedOperationException(String message){
		super(message);
		
	}
}

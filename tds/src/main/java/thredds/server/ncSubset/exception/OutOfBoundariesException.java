package thredds.server.ncSubset.exception;

/**
 * An OutOfBoundariesException is thrown by the NcssControllers when the Lat/Lot point in the request is out of the dataset's boundaries 
 * 
 * @author mhermida
 *
 */
public class OutOfBoundariesException extends NcssException{

	private static final long serialVersionUID = 1L;
	
	/**
	 * Creates an OutOfBoundariesException with a specific message
	 * @param message
	 */	
	public OutOfBoundariesException(String message){
		super(message);
	}
	
	/**
	 * Wraps an exception into an OutOfBoundariesException with a specific message 
	 * @param message
	 */		
	public OutOfBoundariesException(String message, Exception cause){
		super(message, cause);
	}
	

}

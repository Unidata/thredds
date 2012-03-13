package thredds.server.ncSubset.exception;

public class RequestTooLargeException extends NcssException {
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * Creates a RequestTooLarge with a specific message
	 * @param message
	 */	
	public RequestTooLargeException(String message){
		super(message);
	}
	
	/**
	 * Wraps an exception into an RequestTooLarge with a specific message 
	 * @param message
	 */		
	public RequestTooLargeException(String message, Exception cause){
		super(message, cause);
	}	

}

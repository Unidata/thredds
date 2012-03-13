package thredds.server.ncSubset.exception;

/**
 * 
 * UnsupportedResponseFormatExcection is thrown by the NcssControllers if some unexpected accecpt value is in the request params. 
 * 
 * @author mhermida
 *
 */
public class UnsupportedResponseFormatException extends NcssException {

	private static final long serialVersionUID = 3872739321501589570L;
	
	/**
	 * Creates an UnsupportedResponseFormatException with a specific message
	 * @param message
	 */
	public UnsupportedResponseFormatException(String message){
		super(message);
	}
		
}

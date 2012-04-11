package thredds.server.ncSubset.exception;

public abstract class NcssException extends Exception {

	private static final long serialVersionUID = 5315106855858518608L;
	
	public NcssException(String message){
		super(message);
	}

	public NcssException(String message, Exception cause){
		super(message, cause);
	}
}

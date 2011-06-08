package thredds.server.dataset;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class DatasetException extends Exception
{
  public DatasetException( String message ) {
    super( message );
  }

  public DatasetException( String message, Throwable cause ) {
    super( message, cause );
  }
}

package thredds.server.wcs;

/**
 * _more_
 *
 * @author edavis
 * @since Sep 5, 2007 10:38:17 AM
 */
public class WcsException extends Exception
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( WcsException.class );

  public WcsException( String message, Throwable cause )
  {
    super( message, cause );
  }

  public WcsException( String message )
  {
    super( message );
  }
}

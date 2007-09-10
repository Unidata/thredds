package thredds.server.wcs;

import thredds.wcs.v1_1_0.ExceptionReport;

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

  private ExceptionReport.Exception exception;

  public WcsException( String message, Throwable cause, ExceptionReport.Exception exception )
  {
    super( message, cause );
    this.exception = exception;
  }

  public WcsException( String message, ExceptionReport.Exception exception )
  {
    super( message );
    this.exception = exception;
  }
}

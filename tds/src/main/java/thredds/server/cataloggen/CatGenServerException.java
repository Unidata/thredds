package thredds.server.cataloggen;

/**
 * Indicates that the CatalogGen server has encountered a problem.
 *
 * @author edavis
 * @since 4.0
 */
public class CatGenServerException extends Exception
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatGenServerException.class );

  public CatGenServerException()
  {
    super();
  }

  public CatGenServerException( String message )
  {
    super( message );
  }

  public CatGenServerException( String message, Throwable cause )
  {
    super( message, cause );
  }

  public CatGenServerException( Throwable cause )
  {
    super( cause );
  }

}
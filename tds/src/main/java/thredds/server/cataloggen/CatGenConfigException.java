package thredds.server.cataloggen;

/**
 * Indicates that CatalogGen has a configuration problem.
 *
 * @author edavis
 * @since 4.0
 */
public class CatGenConfigException extends Exception
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( CatGenConfigException.class );

  public CatGenConfigException()
  {
    super();
  }

  public CatGenConfigException( String message )
  {
    super( message );
  }

  public CatGenConfigException( String message, Throwable cause )
  {
    super( message, cause );
  }

  public CatGenConfigException( Throwable cause )
  {
    super( cause );
  }

}
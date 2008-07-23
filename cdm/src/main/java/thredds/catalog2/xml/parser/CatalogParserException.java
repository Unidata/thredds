package thredds.catalog2.xml.parser;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogParserException extends Exception
{
  public CatalogParserException()
  {
    super();
  }

  public CatalogParserException( String message )
  {
    super( message);
  }

  public CatalogParserException( String message, Throwable cause )
  {
    super( message, cause);
  }

  public CatalogParserException( Throwable cause )
  {
    super( cause);
  }
}

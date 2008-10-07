package thredds.catalog2.xml.parser;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsXmlParserException extends Exception
{
  public ThreddsXmlParserException()
  {
    super();
  }

  public ThreddsXmlParserException( String message )
  {
    super( message);
  }

  public ThreddsXmlParserException( String message, Throwable cause )
  {
    super( message, cause);
  }

  public ThreddsXmlParserException( Throwable cause )
  {
    super( cause);
  }
}

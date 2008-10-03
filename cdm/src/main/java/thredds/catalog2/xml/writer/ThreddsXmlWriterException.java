package thredds.catalog2.xml.writer;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsXmlWriterException extends Exception
{
  public ThreddsXmlWriterException()
  {
    super();
  }

  public ThreddsXmlWriterException( String message )
  {
    super( message);
  }

  public ThreddsXmlWriterException( String message, Throwable cause )
  {
    super( message, cause);
  }

  public ThreddsXmlWriterException( Throwable cause )
  {
    super( cause);
  }
}
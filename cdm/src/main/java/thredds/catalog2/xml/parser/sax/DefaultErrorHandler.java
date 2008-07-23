package thredds.catalog2.xml.parser.sax;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DefaultErrorHandler implements ErrorHandler
{
  public void warning( SAXParseException exception )
          throws SAXException
  {

    // Do nothing
  }

  public void error( SAXParseException exception ) throws SAXException
  {
    // to be filled in later
  }

  public void fatalError( SAXParseException exception )
          throws SAXException
  {

    // to be filled in later
  }
}

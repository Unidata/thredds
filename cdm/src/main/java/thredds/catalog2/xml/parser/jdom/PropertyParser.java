package thredds.catalog2.xml.parser.jdom;

import thredds.catalog2.xml.PropertyElementUtils;
import thredds.catalog2.Property;

import java.net.URI;
import java.io.*;

import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class PropertyParser
{
  private Logger log = LoggerFactory.getLogger( getClass() );

  private SAXBuilder saxBuilder;

  public PropertyParser()
  {
    this.saxBuilder = new SAXBuilder( false );
  }

  public Property readXML( String xmlAsString, URI docBaseUri )
          throws JDOMException
  {
    try
    {
      this.saxBuilder.build( new StringReader( xmlAsString), docBaseUri.toString());
    }
    catch ( IOException e )
    {
      e.printStackTrace();
    }
    return null;
  }

  public Property readXML( URI uri )
          throws IOException, JDOMException
  {
    Document doc = this.saxBuilder.build( uri.toURL() );
    Element propElem = doc.getRootElement();
    if ( propElem.getName() != PropertyElementUtils.ELEMENT_NAME )
    {

    }
    return null;
  }

  public Property readXML( File file, URI docBaseUri )
  {
    return null;
  }

  public Property readXML( Reader reader, URI docBaseUri )
  {
    
    //this.saxBuilder.build( reader, docBaseUri.toString() );
    return null;
  }

  public Property readXML( InputStream is, URI docBaseUri )
  {
    return null;
  }
}

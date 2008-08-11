package thredds.catalog2.xml.parser.sax;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;

import java.util.Map;
import java.util.HashMap;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsCatalogHandler extends DefaultHandler
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( ThreddsCatalogHandler.class );

  private DefaultHandler state;
  private Map<String, String> namespaceMap;


  public ThreddsCatalogHandler()
  {
    this.state = null;
    this.namespaceMap = new HashMap<String, String>();
  }

  void setState( DefaultHandler state )
  {
    this.state = state;
  }

  public void startPrefixMapping( String prefix, String uri ) throws SAXException
  {
    this.namespaceMap.put( prefix, uri );
  }

  public void endPrefixMapping( String prefix ) throws SAXException
  {
    this.namespaceMap.remove( prefix );
  }

  public void startElement( String uri, String localName, String qName, Attributes attributes ) throws SAXException
  {
    this.state.startElement( uri, localName, qName, attributes );
  }

  public void endElement( String uri, String localName, String qName ) throws SAXException
  {
    this.state.endElement( uri, localName, qName );
  }

  public void warning( SAXParseException e ) throws SAXException
  {
    this.state.warning( e );
  }

  public void error( SAXParseException e ) throws SAXException
  {
    this.state.error( e );
  }

  public void fatalError( SAXParseException e ) throws SAXException
  {
    this.state.fatalError( e );
  }
}

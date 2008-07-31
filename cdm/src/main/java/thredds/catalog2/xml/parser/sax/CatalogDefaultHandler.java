package thredds.catalog2.xml.parser.sax;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogDefaultHandler extends DefaultHandler
{
  private ContentHandler state;
  
  private Map<String,String> namespaceMap;

  public CatalogDefaultHandler()
  {
    this.namespaceMap = new HashMap<String,String>();
  }

  @Override
  public void setDocumentLocator( Locator locator )
  {
    super.setDocumentLocator( locator );
  }

  @Override
  public void startDocument() throws SAXException
  {
    System.out.println( "Start of document" );
  }

  @Override
  public void endDocument() throws SAXException
  {
    System.out.println( "End of document" );
  }

  @Override
  public void startPrefixMapping( String prefix, String uri )
          throws SAXException
  {
    this.namespaceMap.put( prefix, uri );
  }

  @Override
  public void endPrefixMapping( String prefix )
          throws SAXException
  {
    this.namespaceMap.remove( prefix );
  }

  @Override
  public void startElement( String uri, String localName, String qName, Attributes atts ) throws SAXException
  {
    //super.startElement( uri, localName, qName, atts );
    StringBuilder sb = new StringBuilder( "Start Element: " ).append( localName);
    if ( localName.equals( "dataset") )
    {
      sb.append( atts.getValue( "name" ));
    }
    System.out.println( sb.toString() );

  }

  @Override
  public void endElement( String uri, String localName, String qName ) throws SAXException
  {
    //super.endElement( uri, localName, qName );
    System.out.println( "End Element: " + localName );
  }

  @Override
  public void characters( char ch[], int start, int length ) throws SAXException
  {
    super.characters( ch, start, length);
  }

  @Override
  public void ignorableWhitespace( char ch[], int start, int length ) throws SAXException
  {
    super.ignorableWhitespace( ch, start, length);
  }

  @Override
  public void processingInstruction( String target, String data ) throws SAXException
  {
    super.processingInstruction( target, data);
  }

  @Override
  public void skippedEntity( String name ) throws SAXException
  {
    super.skippedEntity( name);
  }

  @Override
  public InputSource resolveEntity( String publicId, String systemId ) throws IOException, SAXException
  {
    return super.resolveEntity( publicId, systemId );
  }

  @Override
  public void fatalError( SAXParseException e ) throws SAXException
  {
    super.fatalError( e );
  }

  @Override
  public void error( SAXParseException e ) throws SAXException
  {
    super.error( e );
  }

  @Override
  public void warning( SAXParseException e ) throws SAXException
  {
    super.warning( e );
  }
}

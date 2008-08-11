package thredds.catalog2.xml.parser.sax;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.io.IOException;
import java.net.URI;

import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.CatalogBuilder;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ServiceHandler extends DefaultHandler
{
  private ServiceBuilder service;

  private DefaultHandler top;
  private DefaultHandler parent;

  private Map<String,String> namespaceMap;

  public ServiceHandler( CatalogBuilder parentBuilder, ServiceBuilder altParentBuilder,
                                Attributes atts, DefaultHandler top, DefaultHandler parent )
  {
    this.namespaceMap = new HashMap<String,String>();

    String name = atts.getValue( "", "name");
    String docBaseUriString = null; // ??? atts.getValue
    URI docBaseUri = null;
    String version = atts.getValue( "", "version");
    String expiresDateString = atts.getValue( "", "expires");
    Date expires = null;
    String lastModifiedDateString = null;
    Date lastModified = null;


//    this.service = new CatalogBuilderFactoryImpl()
//            .newCatalogBuilder( name, docBaseUri, version, expires, lastModified );

    this.top = top;
    this.parent = parent;
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
  public void startElement( String uri, String localName, String qName, Attributes atts ) throws SAXException
  {
    if ( localName.equals( "service" ))
    {
//      ServiceHandler sdh = new ServiceHandler( Attributes atts, DefaultHandler
//      top, DefaultHandler
//      parent);
//      String name = atts.getValue( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), "name" )
//      this.service.addService(  )
    }
    else if ( localName.equals( "dataset"))
    {

    }
    else
    {
      // ???
    }
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
}
package thredds.catalog2.xml.parser.sax;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;

import java.util.Map;
import java.util.HashMap;

import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.Catalog;

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

  private CatalogBuilder builder;
  private String docSystemId;

  private DefaultHandler state;
  private ErrorHandler errorHandler;
  private Map<String, String> namespaceMap;


  public ThreddsCatalogHandler( String docSystemId )
  {
    if ( docSystemId == null )
      throw new IllegalArgumentException( "Document system ID must not be null.");
    this.docSystemId = docSystemId;
    this.state = null;
    this.namespaceMap = new HashMap<String, String>();
  }

  Catalog getCatalog() throws BuilderException
  {
    if ( builder != null )
    {
      return builder.build();
    }
    return null;
  }

  void setState( DefaultHandler state )
  {
    this.state = state;
  }
  void setErrorHandler( ErrorHandler eh )
  {
    this.errorHandler = eh;
  }

  @Override
  public void startDocument()
          throws SAXException
  {
    super.startDocument();
  }

  @Override
  public void endDocument()
          throws SAXException
  {
    super.endDocument();
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
  public void startElement( String uri, String localName, String qName, Attributes attributes )
          throws SAXException
  {
    if ( state != null )
      this.state.startElement( uri, localName, qName, attributes );

    if ( localName.equals( "catalog" ))
      this.state = new CatalogHandler( this.docSystemId, attributes, this, this );
    if ( localName.equals( "service" ))
      this.state = new ServiceHandler( null, null, attributes, this, this );
  }

  @Override
  public void endElement( String uri, String localName, String qName )
          throws SAXException
  {
    if ( state != null )
      this.state.endElement( uri, localName, qName );

    throw new SAXException( "Closing XML element [" + localName + "] after parsing completed.");
    // OR call warning/error/fatal?
  }

  @Override
  public void warning( SAXParseException e )
          throws SAXException
  {
    this.errorHandler.warning( e );
  }

  @Override
  public void error( SAXParseException e )
          throws SAXException
  {
    this.errorHandler.error( e );
  }

  @Override
  public void fatalError( SAXParseException e )
          throws SAXException
  {
    this.errorHandler.fatalError( e );
  }
}

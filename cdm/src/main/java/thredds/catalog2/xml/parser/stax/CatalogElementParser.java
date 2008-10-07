package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.CatalogBuilderFactory;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.util.CatalogElementUtils;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;
import javax.xml.XMLConstants;
import java.util.Date;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                      CatalogElementUtils.ELEMENT_NAME );
  private final static QName nameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      CatalogElementUtils.NAME_ATTRIBUTE_NAME );
  private final static QName versionAttName = new QName( XMLConstants.NULL_NS_URI,
                                                         CatalogElementUtils.VERSION_ATTRIBUTE_NAME );
  private final static QName expiresAttName = new QName( XMLConstants.NULL_NS_URI,
                                                         CatalogElementUtils.EXPIRES_ATTRIBUTE_NAME );
  private final static QName lastModifiedAttName = new QName( XMLConstants.NULL_NS_URI,
                                                              CatalogElementUtils.LAST_MODIFIED_ATTRIBUTE_NAME );

  public static boolean isSelfElement( XMLEvent event )
  {
    QName elemName = null;
    if ( event.isStartElement() )
      elemName = event.asStartElement().getName();
    else if ( event.isEndElement() )
      elemName = event.asEndElement().getName();
    else
      return false;

    if ( elemName.equals( elementName ) )
      return true;
    return false;
  }

  private final String docBaseUriString;
  private final XMLEventReader reader;
  private final CatalogBuilderFactory catBuilderFactory;

  public CatalogElementParser( String docBaseUriString, XMLEventReader reader,  CatalogBuilderFactory catBuilderFactory )
          throws ThreddsXmlParserException
  {
    this.docBaseUriString = docBaseUriString;
    this.reader = reader;
    this.catBuilderFactory = catBuilderFactory;
  }

  public CatalogBuilder parse()
          throws ThreddsXmlParserException
  {
    try
    {
      CatalogBuilder builder = this.parseElement( reader.nextEvent() );

      while ( reader.hasNext() )
      {
        XMLEvent event = reader.peek();
        if ( event.isStartElement() )
        {
          handleStartElement( event.asStartElement(), builder );
        }
        else if ( event.isEndElement() )
        {
          if ( isSelfElement( event.asEndElement() ) )
          {
            reader.next();
            break;
          }
          else
          {
            log.error( "parse(): Unrecognized end element [" + event.asEndElement().getName() + "]." );
            break;
          }
        }
        else
        {
          reader.next();
          continue;
        }
      }

      return builder;
    }
    catch ( XMLStreamException e )
    {
      log.error( "parse(): Failed to parse catalog element: " + e.getMessage(), e);
      throw new ThreddsXmlParserException( "Failed to parse catalog element: " + e.getMessage(), e );
    }
  }

  private CatalogBuilder parseElement( XMLEvent event )
          throws ThreddsXmlParserException
  {
    if ( !event.isStartElement() )
      throw new IllegalArgumentException( "Event must be start element." );
    StartElement startCatElem = event.asStartElement();

    Attribute nameAtt = startCatElem.getAttributeByName( nameAttName );
    String nameString = nameAtt.getValue();
    Attribute versionAtt = startCatElem.getAttributeByName( versionAttName );
    String versionString = versionAtt.getValue();
    Attribute expiresAtt = startCatElem.getAttributeByName( expiresAttName );
    Date expiresDate = null;
    Attribute lastModifiedAtt = startCatElem.getAttributeByName( lastModifiedAttName );
    Date lastModifiedDate = null;
    URI docBaseUri = null;
    try
    {
      docBaseUri = new URI( docBaseUriString );
    }
    catch ( URISyntaxException e )
    {
      log.error( "parseElement(): Bad catalog base URI [" + docBaseUriString + "]: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Bad catalog base URI [" + docBaseUriString + "]: " + e.getMessage(), e );
    }
    return catBuilderFactory.newCatalogBuilder( nameString, docBaseUri, versionString, expiresDate, lastModifiedDate );
  }

  private void handleStartElement( StartElement startElement, CatalogBuilder catalogBuilder )
          throws ThreddsXmlParserException
  {
    if ( ServiceElementParser.isSelfElement( startElement ) )
    {
      ServiceElementParser serviceElemParser = new ServiceElementParser( this.reader, catalogBuilder );
      serviceElemParser.parse();
    }
    else if ( PropertyElementParser.isSelfElement( startElement ) )
    {
      PropertyElementParser parser = new PropertyElementParser( this.reader, catalogBuilder );
      parser.parse();
    }
    else
    {
      StaxCatalogParserUtils.consumeElementAndAnyContent( this.reader );
    }
  }
}

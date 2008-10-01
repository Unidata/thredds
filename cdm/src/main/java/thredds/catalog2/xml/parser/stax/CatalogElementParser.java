package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.CatalogBuilderFactory;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.xml.parser.CatalogNamespace;
import thredds.catalog2.xml.parser.CatalogParserException;
import thredds.catalog2.xml.AbstractCatalogElement;

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
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( CatalogElementParser.class );

  private final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                      AbstractCatalogElement.ELEMENT_NAME );
  private final static QName versionAttName = new QName( XMLConstants.NULL_NS_URI,
                                                         AbstractCatalogElement.VERSION_ATTRIBUTE_NAME );
  private final static QName expiresAttName = new QName( XMLConstants.NULL_NS_URI,
                                                         AbstractCatalogElement.EXPIRES_ATTRIBUTE_NAME );
  private final static QName lastModifiedAttName = new QName( XMLConstants.NULL_NS_URI,
                                                              AbstractCatalogElement.LAST_MODIFIED_ATTRIBUTE_NAME );

  public static boolean isRecognizedElement( XMLEvent event )
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

  private final String baseUriString;
  private final XMLEventReader reader;
  private final CatalogBuilderFactory catBuilderFactory;

  public CatalogElementParser( String baseUriString, XMLEventReader reader,  CatalogBuilderFactory catBuilderFactory )
          throws CatalogParserException
  {
    this.baseUriString = baseUriString;
    this.reader = reader;
    this.catBuilderFactory = catBuilderFactory;
  }

  public CatalogBuilder parse()
          throws CatalogParserException
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
          if ( isRecognizedElement( event.asEndElement() ) )
          {
            reader.next();
            break;
          }
          else
          {
            logger.error( "parse(): Unrecognized end element [" + event.asEndElement().getName() + "]." );
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
      throw new CatalogParserException( "Failed to parse catalog element.", e );
    }
  }

  private CatalogBuilder parseElement( XMLEvent event )
          throws CatalogParserException
  {
    if ( !event.isStartElement() )
      throw new IllegalArgumentException( "Event must be start element." );
    StartElement startCatElem = event.asStartElement();

    Attribute versionAtt = startCatElem.getAttributeByName( versionAttName );
    String versionString = versionAtt.getValue();
    Attribute expiresAtt = startCatElem.getAttributeByName( expiresAttName );
    Date expiresDate = null;
    Attribute lastModifiedAtt = startCatElem.getAttributeByName( lastModifiedAttName );
    Date lastModifiedDate = null;
    URI baseUri = null;
    try
    {
      baseUri = new URI( baseUriString );
    }
    catch ( URISyntaxException e )
    {
      throw new CatalogParserException( "Bad catalog base URI [" + baseUriString + "]", e );
    }
    return catBuilderFactory.newCatalogBuilder(
            startCatElem.getName().getLocalPart(),
            baseUri, versionString, expiresDate, lastModifiedDate );
  }

  private void handleStartElement( StartElement startElement, CatalogBuilder catalogBuilder )
          throws CatalogParserException
  {
    if ( ServiceElementParser.isRecognizedElement( startElement ) )
    {
      ServiceElementParser serviceElemParser = new ServiceElementParser( reader, catalogBuilder );
      serviceElemParser.parse();
    }
    else
    {

    }
  }
}

package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.xml.CatalogNamespace;
import thredds.catalog2.xml.parser.CatalogParserException;
import thredds.catalog2.xml.PropertyElementUtils;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;
import javax.xml.XMLConstants;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class PropertyElementParser
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( CatalogElementParser.class );

  private final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                      PropertyElementUtils.ELEMENT_NAME );
  private final static QName nameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      PropertyElementUtils.NAME_ATTRIBUTE_NAME );
  private final static QName valueAttName = new QName( XMLConstants.NULL_NS_URI,
                                                       PropertyElementUtils.VALUE_ATTRIBUTE_NAME );

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

  public boolean isRecognizedChildElement( XMLEvent event )
  { return true; }

  private final XMLEventReader reader;
  private final CatalogBuilder catBuilder;
  private final ServiceBuilder serviceBuilder;

  public PropertyElementParser( XMLEventReader reader,  CatalogBuilder catBuilder )
          throws CatalogParserException
  {
    this.reader = reader;
    this.catBuilder = catBuilder;
    this.serviceBuilder = null;
  }

  public PropertyElementParser( XMLEventReader reader,  ServiceBuilder serviceBuilder )
          throws CatalogParserException
  {
    this.reader = reader;
    this.catBuilder = null;
    this.serviceBuilder = serviceBuilder;
  }

  public void parse()
          throws CatalogParserException
  {
    try
    {
      this.parseElement( reader.nextEvent() );

      while ( reader.hasNext() )
      {
        XMLEvent event = reader.peek();
        if ( event.isStartElement() )
        {
          handleStartElement( event.asStartElement() );
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

      return;
    }
    catch ( XMLStreamException e )
    {
      throw new CatalogParserException( "Failed to parse service element.", e );
    }
  }

  private void parseElement( XMLEvent event )
          throws CatalogParserException
  {
    if ( !event.isStartElement() )
      throw new IllegalArgumentException( "Event must be start element." );
    StartElement startElement = event.asStartElement();

    Attribute nameAtt = startElement.getAttributeByName( nameAttName );
    String name = nameAtt.getValue();
    Attribute valueAtt = startElement.getAttributeByName( valueAttName );
    String value = valueAtt.getValue();

    if ( this.catBuilder != null )
      this.catBuilder.addProperty( name, value );
    else if ( this.serviceBuilder != null )
      this.serviceBuilder.addProperty( name, value );
    else
      throw new CatalogParserException( "Unknown builder - for addProperty()." );

    return;
  }

  private void handleStartElement( StartElement startElement )
          throws CatalogParserException
  {
    if ( isRecognizedChildElement( startElement ) )
      StaxCatalogParserUtils.consumeElementAndAnyContent( this.reader );
  }
}
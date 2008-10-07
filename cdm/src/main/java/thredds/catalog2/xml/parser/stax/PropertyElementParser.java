package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.DatasetNodeBuilder;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.util.PropertyElementUtils;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;

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
  private org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( getClass() );

  private final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                      PropertyElementUtils.ELEMENT_NAME );
  private final static QName nameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      PropertyElementUtils.NAME_ATTRIBUTE_NAME );
  private final static QName valueAttName = new QName( XMLConstants.NULL_NS_URI,
                                                       PropertyElementUtils.VALUE_ATTRIBUTE_NAME );

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

  public boolean isChildElement( XMLEvent event )
  { return false; //property doesn't contain any children
  }

  private final XMLEventReader reader;
  private final CatalogBuilder catBuilder;
  private final DatasetNodeBuilder datasetNodeBuilder;
  private final ServiceBuilder serviceBuilder;

  public PropertyElementParser( XMLEventReader reader,  CatalogBuilder catBuilder )
          throws ThreddsXmlParserException
  {
    this.reader = reader;
    this.catBuilder = catBuilder;
    this.datasetNodeBuilder = null;
    this.serviceBuilder = null;
  }

  public PropertyElementParser( XMLEventReader reader,  DatasetNodeBuilder datasetNodeBuilder )
          throws ThreddsXmlParserException
  {
    this.reader = reader;
    this.catBuilder = null;
    this.datasetNodeBuilder = datasetNodeBuilder;
    this.serviceBuilder = null;
  }

  public PropertyElementParser( XMLEventReader reader,  ServiceBuilder serviceBuilder )
          throws ThreddsXmlParserException
  {
    this.reader = reader;
    this.catBuilder = null;
    this.datasetNodeBuilder = null;
    this.serviceBuilder = serviceBuilder;
  }

  public void parse()
          throws ThreddsXmlParserException
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

      return;
    }
    catch ( XMLStreamException e )
    {
      log.error( "parse(): Failed to parse service element: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Failed to parse service element: " + e.getMessage(), e );
    }
  }

  private void parseElement( XMLEvent event )
          throws ThreddsXmlParserException
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
    else if ( this.datasetNodeBuilder != null )
      this.datasetNodeBuilder.addProperty( name, value );
    else if ( this.serviceBuilder != null )
      this.serviceBuilder.addProperty( name, value );
    else
      throw new ThreddsXmlParserException( "Unknown builder - for addProperty()." );

    return;
  }

  private void handleStartElement( StartElement startElement )
          throws ThreddsXmlParserException
  {
    if ( ! isChildElement( startElement ) )
      StaxThreddsXmlParserUtils.consumeElementAndAnyContent( this.reader );
  }
}
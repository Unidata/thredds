package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.*;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.parser.CatalogParserException;
import thredds.catalog2.xml.util.AccessElementUtils;
import thredds.catalog.DataFormatType;

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
public class AccessElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                      AccessElementUtils.ELEMENT_NAME );
  private final static QName serviceNameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                             AccessElementUtils.SERVICE_NAME_ATTRIBUTE_NAME );
  private final static QName urlPathAttName = new QName( XMLConstants.NULL_NS_URI,
                                                         AccessElementUtils.URL_PATH_ATTRIBUTE_NAME );
  private final static QName dataFormatAttName = new QName( XMLConstants.NULL_NS_URI,
                                                            AccessElementUtils.DATA_FORMAT_ATTRIBUTE_NAME );

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

  private final XMLEventReader reader;
  private final DatasetBuilder datasetBuilder;
  private final CatalogBuilderFactory catBuilderFactory;

  public AccessElementParser( XMLEventReader reader, DatasetBuilder datasetBuilder )
          throws CatalogParserException
  {
    this.reader = reader;
    this.datasetBuilder = datasetBuilder;
    this.catBuilderFactory = null;
  }

  public AccessElementParser( XMLEventReader reader, CatalogBuilderFactory catBuilderFactory )
          throws CatalogParserException
  {
    this.reader = reader;
    this.datasetBuilder = null;
    this.catBuilderFactory = catBuilderFactory;
  }

  public AccessBuilder parse()
          throws CatalogParserException
  {
    try
    {
      AccessBuilder builder = this.parseElement( reader.nextEvent() );

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
      log.error( "parse(): Failed to parse service element: " + e.getMessage(), e );
      throw new CatalogParserException( "Failed to parse service element: " + e.getMessage(), e );
    }
  }

  private AccessBuilder parseElement( XMLEvent event )
          throws CatalogParserException
  {
    if ( !event.isStartElement() )
      throw new IllegalArgumentException( "Event must be start element." );
    StartElement startElement = event.asStartElement();

    AccessBuilder builder = null;
    if ( this.datasetBuilder != null )
      builder = this.datasetBuilder.addAccessBuilder();
    else if ( catBuilderFactory != null )
      builder = catBuilderFactory.newAccessBuilder();
    else
      throw new CatalogParserException( "" );

    Attribute serviceNameAtt = startElement.getAttributeByName( serviceNameAttName );
    String serviceName = serviceNameAtt.getValue();
    ServiceBuilder serviceBuilder = this.datasetBuilder.getParentCatalogBuilder().getServiceBuilderByName( serviceName );
    Attribute urlPathAtt = startElement.getAttributeByName( urlPathAttName );
    String urlPath = urlPathAtt.getValue();

    builder.setServiceBuilder( serviceBuilder );
    builder.setUrlPath( urlPath );

    Attribute dataFormatAtt = startElement.getAttributeByName( dataFormatAttName );
    if ( dataFormatAtt != null )
    {
      builder.setDataFormat( DataFormatType.getType( dataFormatAtt.getValue() ) );
    }

    return builder;
  }

  private void handleStartElement( StartElement startElement, AccessBuilder builder )
          throws CatalogParserException
  {
//    if ( ThreddsMetadataElementParser.DataSizeElementParser.isSelfElement( startElement ) )
//    {
//      ThreddsMetadataElementParser.DataSizeElementParser parser = new ServiceElementParser( reader, builder );
//      parser.parse();
//    }
//    else
    {
      //if ( !isChildElement( startElement ) )
        StaxCatalogParserUtils.consumeElementAndAnyContent( this.reader );
    }
  }
}
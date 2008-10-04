package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.CatalogBuilderFactory;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.util.ServiceElementUtils;
import thredds.catalog2.xml.parser.CatalogParserException;
import thredds.catalog.ServiceType;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;
import javax.xml.XMLConstants;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ServiceElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                      ServiceElementUtils.ELEMENT_NAME );
  private final static QName nameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      ServiceElementUtils.NAME_ATTRIBUTE_NAME );
  private final static QName baseAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      ServiceElementUtils.BASE_ATTRIBUTE_NAME );
  private final static QName serviceTypeAttName = new QName( XMLConstants.NULL_NS_URI,
                                                             ServiceElementUtils.SERVICE_TYPE_ATTRIBUTE_NAME );
  private final static QName descriptionAttName = new QName( XMLConstants.NULL_NS_URI,
                                                             ServiceElementUtils.DESCRIPTION_ATTRIBUTE_NAME );
  private final static QName suffixAttName = new QName( XMLConstants.NULL_NS_URI,
                                                             ServiceElementUtils.SUFFIX_ATTRIBUTE_NAME );

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
  private final CatalogBuilder catBuilder;
  private final ServiceBuilder serviceBuilder;
  private final CatalogBuilderFactory catBuilderFactory;

  public ServiceElementParser( XMLEventReader reader,  CatalogBuilder catBuilder )
          throws CatalogParserException
  {
    this.reader = reader;
    this.catBuilder = catBuilder;
    this.serviceBuilder = null;
    this.catBuilderFactory = null;
  }

  public ServiceElementParser( XMLEventReader reader,  ServiceBuilder serviceBuilder )
          throws CatalogParserException
  {
    this.reader = reader;
    this.catBuilder = null;
    this.serviceBuilder = serviceBuilder;
    this.catBuilderFactory = null;
  }

  public ServiceElementParser( XMLEventReader reader, CatalogBuilderFactory catBuilderFactory )
          throws CatalogParserException
  {
    this.reader = reader;
    this.catBuilder = null;
    this.serviceBuilder = null;
    this.catBuilderFactory = catBuilderFactory;
  }

  public ServiceBuilder parse()
          throws CatalogParserException
  {
    try
    {
      ServiceBuilder builder = this.parseElement( reader.nextEvent() );

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

  private ServiceBuilder parseElement( XMLEvent event )
          throws CatalogParserException
  {
    if ( !event.isStartElement() )
      throw new IllegalArgumentException( "Event must be start element." );
    StartElement startElement = event.asStartElement();

    Attribute nameAtt = startElement.getAttributeByName( nameAttName );
    String name = nameAtt.getValue();
    Attribute serviceTypeAtt = startElement.getAttributeByName( serviceTypeAttName );
    ServiceType serviceType = ServiceType.getType( serviceTypeAtt.getValue() );
    Attribute baseUriAtt = startElement.getAttributeByName( baseAttName );
    String baseUriString = baseUriAtt.getValue();
    URI baseUri = null;
    try
    {
      baseUri = new URI( baseUriString );
    }
    catch ( URISyntaxException e )
    {
      log.error( "parseElement(): Bad service base URI [" + baseUriString + "]: " + e.getMessage(), e );
      throw new CatalogParserException( "Bad service base URI [" + baseUriString + "]", e );
    }
    ServiceBuilder serviceBuilder = null;
    if ( this.catBuilder != null )
      serviceBuilder = this.catBuilder.addService( name, serviceType, baseUri );
    else if ( this.serviceBuilder != null )
      serviceBuilder = this.serviceBuilder.addService( name, serviceType, baseUri );
    else if ( catBuilderFactory != null )
      serviceBuilder = catBuilderFactory.newServiceBuilder( name, serviceType, baseUri );
    else
      throw new CatalogParserException( "" );

    Attribute suffixAtt = startElement.getAttributeByName( suffixAttName );
    if ( suffixAtt != null )
    {
      serviceBuilder.setSuffix( suffixAtt.getValue() );
    }

    Attribute descriptionAtt = startElement.getAttributeByName( descriptionAttName );
    if ( descriptionAtt != null )
    {
      serviceBuilder.setSuffix( descriptionAtt.getValue() );
    }

    return serviceBuilder;
  }

  private void handleStartElement( StartElement startElement, ServiceBuilder builder )
          throws CatalogParserException
  {
    if ( ServiceElementParser.isSelfElement( startElement ) )
    {
      ServiceElementParser serviceElemParser = new ServiceElementParser( reader, builder );
      serviceElemParser.parse();
    }
    else if ( PropertyElementParser.isSelfElement( startElement ))
    {
      PropertyElementParser parser = new PropertyElementParser( reader, builder);
      parser.parse();
    }
    else
    {
      StaxCatalogParserUtils.consumeElementAndAnyContent( this.reader );
    }
  }
}
package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.CatalogBuilderFactory;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.util.ServiceElementUtils;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
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
public class ServiceElementParser2 extends AbstractElementParser
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

  private final CatalogBuilder catBuilder;
  private final ServiceBuilder serviceBuilder;
  private final CatalogBuilderFactory catBuilderFactory;

  public ServiceElementParser2( XMLEventReader reader,  CatalogBuilder catBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, elementName);
    this.catBuilder = catBuilder;
    this.serviceBuilder = null;
    this.catBuilderFactory = null;
  }

  public ServiceElementParser2( XMLEventReader reader,  ServiceBuilder serviceBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = null;
    this.serviceBuilder = serviceBuilder;
    this.catBuilderFactory = null;
  }

  public ServiceElementParser2( XMLEventReader reader, CatalogBuilderFactory catBuilderFactory )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = null;
    this.serviceBuilder = null;
    this.catBuilderFactory = catBuilderFactory;
  }

  protected static boolean isSelfElementStatic( XMLEvent event )
  {
    return isSelfElement( event, elementName );
  }

  protected boolean isSelfElement( XMLEvent event )
  {
    return isSelfElement( event, elementName );
  }

  protected ServiceBuilder parseStartElement( XMLEvent event )
          throws ThreddsXmlParserException
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
      throw new ThreddsXmlParserException( "Bad service base URI [" + baseUriString + "]", e );
    }
    ServiceBuilder serviceBuilder = null;
    if ( this.catBuilder != null )
      serviceBuilder = this.catBuilder.addService( name, serviceType, baseUri );
    else if ( this.serviceBuilder != null )
      serviceBuilder = this.serviceBuilder.addService( name, serviceType, baseUri );
    else if ( catBuilderFactory != null )
      serviceBuilder = catBuilderFactory.newServiceBuilder( name, serviceType, baseUri );
    else
      throw new ThreddsXmlParserException( "" );

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

  protected void handleChildStartElement( StartElement startElement, ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( !( builder instanceof ServiceBuilder ) )
      throw new IllegalArgumentException( "Given ThreddsBuilder must be an instance of DatasetBuilder." );
    ServiceBuilder serviceBuilder = (ServiceBuilder) builder;

    if ( ServiceElementParser2.isSelfElementStatic( startElement ) )
    {
      ServiceElementParser2 serviceElemParser = new ServiceElementParser2( reader, serviceBuilder );
      serviceElemParser.parse();
    }
    else if ( PropertyElementParser2.isSelfElementStatic( startElement ))
    {
      PropertyElementParser2 parser = new PropertyElementParser2( reader, serviceBuilder);
      parser.parse();
    }
    else
    {
      StaxThreddsXmlParserUtils.consumeElementAndAnyContent( this.reader );
    }
  }

  protected void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    return;
  }
}
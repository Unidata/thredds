package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.*;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.util.AccessElementUtils;
import thredds.catalog.DataFormatType;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.namespace.QName;
import javax.xml.XMLConstants;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class AccessElementParser extends AbstractElementParser
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

  private final DatasetBuilder datasetBuilder;
  private final CatalogBuilderFactory catBuilderFactory;

  public AccessElementParser( XMLEventReader reader, DatasetBuilder datasetBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.datasetBuilder = datasetBuilder;
    this.catBuilderFactory = null;
  }

  public AccessElementParser( XMLEventReader reader, CatalogBuilderFactory catBuilderFactory )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.datasetBuilder = null;
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

  protected AccessBuilder parseStartElement( XMLEvent event )
          throws ThreddsXmlParserException
  {
    if ( !event.isStartElement() )
      throw new IllegalArgumentException( "Event must be start element." );
    StartElement startElement = event.asStartElement();
    if ( !startElement.getName().equals( elementName ) )
      throw new IllegalArgumentException( "Start element must be an 'access' element." );

    AccessBuilder builder = null;
    if ( this.datasetBuilder != null )
      builder = this.datasetBuilder.addAccessBuilder();
    else if ( catBuilderFactory != null )
      builder = catBuilderFactory.newAccessBuilder();
    else
      throw new ThreddsXmlParserException( "" );

    Attribute serviceNameAtt = startElement.getAttributeByName( serviceNameAttName );
    String serviceName = serviceNameAtt.getValue();
    // ToDo This only gets top level services, need findServiceBuilderByName() to crawl services
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

  protected void handleChildStartElement( StartElement startElement, ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
//    if ( ThreddsMetadataElementParser.DataSizeElementParser.isSelfElement( startElement ) )
//    {
//      ThreddsMetadataElementParser.DataSizeElementParser parser = new ServiceElementParser( reader, builder );
//      parser.parse();
//    }
//    else
    {
      //if ( !isChildElement( startElement ) )
        StaxThreddsXmlParserUtils.readElementAndAnyContent( this.reader );
    }
  }

  protected void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    return;
  }
}
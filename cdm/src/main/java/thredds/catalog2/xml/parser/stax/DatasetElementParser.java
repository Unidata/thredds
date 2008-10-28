package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.util.DatasetElementUtils;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.builder.*;

import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.XMLEventReader;
import javax.xml.XMLConstants;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DatasetElementParser extends AbstractElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );
  
  protected final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                        DatasetElementUtils.ELEMENT_NAME );
  private final static QName nameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      DatasetElementUtils.NAME_ATTRIBUTE_NAME );
  private final static QName idAttName = new QName( XMLConstants.NULL_NS_URI,
                                                    DatasetElementUtils.ID_ATTRIBUTE_NAME );
  private final static QName urlPathAttName = new QName( XMLConstants.NULL_NS_URI,
                                                         DatasetElementUtils.URL_PATH_ATTRIBUTE_NAME );

  private final static QName collectionTypeAttName = new QName( XMLConstants.NULL_NS_URI,
                                                                DatasetElementUtils.COLLECTION_TYPE_ATTRIBUTE_NAME );
  private final static QName harvestAttName = new QName( XMLConstants.NULL_NS_URI,
                                                         DatasetElementUtils.HARVEST_ATTRIBUTE_NAME );
  private final static QName restrictedAccessAttName = new QName( XMLConstants.NULL_NS_URI,
                                                                  DatasetElementUtils.RESOURCE_CONTROL_ATTRIBUTE_NAME );
  private final static QName aliasAttName = new QName( XMLConstants.NULL_NS_URI,
                                                       DatasetElementUtils.ALIAS_ATTRIBUTE_NAME );

  private final CatalogBuilder catBuilder;
  private final DatasetBuilder datasetBuilder;
  private final CatalogBuilderFactory catBuilderFactory;


  public DatasetElementParser( XMLEventReader reader, CatalogBuilder catBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = catBuilder;
    this.datasetBuilder = null;
    this.catBuilderFactory = null;
  }

  public DatasetElementParser( XMLEventReader reader, DatasetBuilder datasetBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = null;
    this.datasetBuilder = datasetBuilder;
    this.catBuilderFactory = null;
  }

  public DatasetElementParser( XMLEventReader reader, CatalogBuilderFactory catBuilderFactory )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = null;
    this.datasetBuilder = null;
    this.catBuilderFactory = catBuilderFactory;
  }

  private String defaultServiceName;

  protected void setDefaultServiceName( String defaultServiceName )
  {
    if ( defaultServiceName == null ) return;
    this.defaultServiceName = defaultServiceName;
  }

  protected String getDefaultServiceName()
  {
    return this.defaultServiceName;
  }

  protected static boolean isSelfElementStatic( XMLEvent event )
  {
    return isSelfElement( event, elementName );
  }

  protected boolean isSelfElement( XMLEvent event )
  {
    return isSelfElement( event, elementName );
  }

  protected DatasetBuilder parseStartElement( XMLEvent event )
          throws ThreddsXmlParserException
  {
    if ( !event.isStartElement() )
      throw new IllegalArgumentException( "Event must be start element." );
    StartElement startElement = event.asStartElement();
    if ( ! startElement.equals( elementName ))
      throw new IllegalArgumentException( "Start element is not 'dataset' element.");

    Attribute nameAtt = startElement.getAttributeByName( nameAttName );
    String name = nameAtt.getValue();

    DatasetBuilder datasetBuilder = null;
    if ( this.catBuilder != null )
      datasetBuilder = this.catBuilder.addDataset( name );
    else if ( this.datasetBuilder != null )
      datasetBuilder = this.datasetBuilder.addDataset( name );
    else if ( catBuilderFactory != null )
      datasetBuilder = catBuilderFactory.newDatasetBuilder( name );
    else
      throw new ThreddsXmlParserException( "" );

    Attribute idAtt = startElement.getAttributeByName( idAttName );
    if ( idAtt != null )
    {
      datasetBuilder.setId( idAtt.getValue() );
    }

    Attribute urlPathAtt = startElement.getAttributeByName( urlPathAttName );
    if ( urlPathAtt != null )
    {
      // Add AccessBuilder and set urlPath, set ServiceBuilder in postProcessing().
      AccessBuilder accessBuilder = datasetBuilder.addAccessBuilder();
      accessBuilder.setUrlPath( urlPathAtt.getValue() );
    }

    return datasetBuilder;
  }

  protected void handleChildStartElement( StartElement startElement, ThreddsBuilder builder ) throws ThreddsXmlParserException
  {
    if ( !( builder instanceof DatasetBuilder ) )
      throw new IllegalArgumentException( "Given ThreddsBuilder must be an instance of DatasetBuilder." );
    DatasetBuilder datasetBuilder = (DatasetBuilder) builder;

    if ( AccessElementParser.isSelfElementStatic( startElement ) )
    {
      AccessElementParser parser = new AccessElementParser( this.reader, datasetBuilder );
      parser.parse();
    }
    else
    {
      StaxThreddsXmlParserUtils.readElementAndAnyContent( this.reader );
    }
  }

  protected void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( ! ( builder instanceof DatasetBuilder) )
      throw new IllegalArgumentException( "Given ThreddsBuilder must be an instance of DatasetBuilder.");
    DatasetBuilder datasetBuilder = (DatasetBuilder) builder;

    // In any AccessBuilders that don't have a ServiceBuilder, set it with the default service.
    if ( this.defaultServiceName != null
         && ! datasetBuilder.getAccessBuilders().isEmpty() )
    {
      // ToDo This only gets top level services, need findServiceBuilderByName() to crawl services
      ServiceBuilder defaultServiceBuilder = datasetBuilder.getParentCatalogBuilder().getServiceBuilderByName( this.defaultServiceName );

      for ( AccessBuilder curAB : datasetBuilder.getAccessBuilders() )
      {
        if ( curAB.getServiceBuilder() == null )
          curAB.setServiceBuilder( defaultServiceBuilder );
      }
    }
  }
}

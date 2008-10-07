package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.*;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.util.DatasetElementUtils;
import thredds.catalog2.xml.util.CatalogRefElementUtils;
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
public class DatasetElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
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

  private final static QName catRefElementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                         CatalogRefElementUtils.ELEMENT_NAME );

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
    else if ( elemName.equals( catRefElementName ) )
    {
      return true;
    }
    return false;
  }

  private final XMLEventReader reader;
  private final CatalogBuilder catBuilder;
  private final DatasetBuilder datasetBuilder;
  private final CatalogBuilderFactory catBuilderFactory;

  public DatasetElementParser( XMLEventReader reader,  CatalogBuilder catBuilder )
          throws ThreddsXmlParserException
  {
    this.reader = reader;
    this.catBuilder = catBuilder;
    this.datasetBuilder = null;
    this.catBuilderFactory = null;
  }

  public DatasetElementParser( XMLEventReader reader,  DatasetBuilder datasetBuilder )
          throws ThreddsXmlParserException
  {
    this.reader = reader;
    this.catBuilder = null;
    this.datasetBuilder = datasetBuilder;
    this.catBuilderFactory = null;
  }

  public DatasetElementParser( XMLEventReader reader, CatalogBuilderFactory catBuilderFactory )
          throws ThreddsXmlParserException
  {
    this.reader = reader;
    this.catBuilder = null;
    this.datasetBuilder = null;
    this.catBuilderFactory = catBuilderFactory;
  }

  public DatasetBuilder parse()
          throws ThreddsXmlParserException
  {
    try
    {
      DatasetBuilder builder = this.parseElement( reader.nextEvent() );

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
      throw new ThreddsXmlParserException( "Failed to parse service element: " + e.getMessage(), e );
    }
  }

  private DatasetBuilder parseElement( XMLEvent event )
          throws ThreddsXmlParserException
  {
    if ( !event.isStartElement() )
      throw new IllegalArgumentException( "Event must be start element." );
    StartElement startElement = event.asStartElement();

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
      //ToDo Need to postpone adding service to access builder till this dataset is finished.
      //datasetBuilder.getParentCatalogBuilder().getServiceBuilderByName(  )
      AccessBuilder accessBuilder = datasetBuilder.addAccessBuilder();
      accessBuilder.setUrlPath( urlPathAtt.getValue() );
      // Add service in finish() when known.
    }

    return datasetBuilder;
  }

  private void handleStartElement( StartElement startElement, DatasetBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( AccessElementParser.isSelfElement( startElement ))
    {
      AccessElementParser parser = new AccessElementParser( reader, builder);
      parser.parse();
    }
    else
    {
      StaxCatalogParserUtils.consumeElementAndAnyContent( this.reader );
    }
  }
}
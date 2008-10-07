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
public class DatasetNodeElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  protected final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                        DatasetElementUtils.ELEMENT_NAME );
  protected final static QName nameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                        DatasetElementUtils.NAME_ATTRIBUTE_NAME );
  protected final static QName idAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      DatasetElementUtils.ID_ATTRIBUTE_NAME );


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
    else if ( elemName.equals( CatalogRefElementParser.elementName ) )
    {
      return true;
    }
    return false;
  }

  private final XMLEventReader reader;
  private final CatalogBuilder catBuilder;
  private final DatasetNodeBuilder datasetNodeBuilder;
  private final CatalogBuilderFactory catBuilderFactory;

  public DatasetNodeElementParser( XMLEventReader reader,  CatalogBuilder catBuilder )
          throws ThreddsXmlParserException
  {
    this.reader = reader;
    this.catBuilder = catBuilder;
    this.datasetNodeBuilder = null;
    this.catBuilderFactory = null;
  }

  public DatasetNodeElementParser( XMLEventReader reader, DatasetNodeBuilder datasetNodeBuilder )
          throws ThreddsXmlParserException
  {
    this.reader = reader;
    this.catBuilder = null;
    this.datasetNodeBuilder = datasetNodeBuilder;
    this.catBuilderFactory = null;
  }

  public DatasetNodeElementParser( XMLEventReader reader, CatalogBuilderFactory catBuilderFactory )
          throws ThreddsXmlParserException
  {
    this.reader = reader;
    this.catBuilder = null;
    this.datasetNodeBuilder = null;
    this.catBuilderFactory = catBuilderFactory;
  }

  public DatasetNodeBuilder parse()
          throws ThreddsXmlParserException
  {
    try
    {
      DatasetNodeBuilder builder = this.parseElement( reader.nextEvent() );

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

  private DatasetNodeBuilder parseElement( XMLEvent event )
          throws ThreddsXmlParserException
  {
    if ( !event.isStartElement() )
      throw new IllegalArgumentException( "Event must be start element." );
    StartElement startElement = event.asStartElement();

    // Get required attributes.
    Attribute nameAtt = startElement.getAttributeByName( nameAttName );
    String name = nameAtt.getValue();

    // Construct builder.
    DatasetBuilder datasetBuilder = null;
    if ( this.catBuilder != null )
      datasetBuilder = this.catBuilder.addDataset( name );
    else if ( this.datasetNodeBuilder != null )
      datasetBuilder = this.datasetNodeBuilder.addDataset( name );
    else if ( catBuilderFactory != null )
      datasetBuilder = catBuilderFactory.newDatasetBuilder( name );
    else
      throw new ThreddsXmlParserException( "" );

    // Set optional attributes
    Attribute idAtt = startElement.getAttributeByName( idAttName );
    if ( idAtt != null )
    {
      datasetBuilder.setId( idAtt.getValue() );
    }

    return datasetBuilder;
  }

  private void handleStartElement( StartElement startElement, DatasetNodeBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( PropertyElementParser.isSelfElement( startElement ))
    {
      PropertyElementParser parser = new PropertyElementParser( reader, builder);
      parser.parse();
    }
    else
    {
      StaxThreddsXmlParserUtils.consumeElementAndAnyContent( this.reader );
    }
  }
}
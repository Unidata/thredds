package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.*;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.util.CatalogRefElementUtils;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.namespace.QName;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogRefElementParser extends AbstractElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  protected final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                        CatalogRefElementUtils.ELEMENT_NAME );
  protected final static QName titleAttName = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                                                        CatalogRefElementUtils.XLINK_TITLE_ATTRIBUTE_NAME );
  protected final static QName hrefAttName = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                                                        CatalogRefElementUtils.XLINK_HREF_ATTRIBUTE_NAME );
  protected final static QName typeAttName = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                                                        CatalogRefElementUtils.XLINK_TYPE_ATTRIBUTE_NAME );


  private final CatalogBuilder catBuilder;
  private final DatasetNodeBuilder datasetNodeBuilder;
  private final CatalogBuilderFactory catBuilderFactory;

  public CatalogRefElementParser( XMLEventReader reader,  CatalogBuilder catBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = catBuilder;
    this.datasetNodeBuilder = null;
    this.catBuilderFactory = null;
  }

  public CatalogRefElementParser( XMLEventReader reader, DatasetNodeBuilder datasetNodeBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = null;
    this.datasetNodeBuilder = datasetNodeBuilder;
    this.catBuilderFactory = null;
  }

  public CatalogRefElementParser( XMLEventReader reader, CatalogBuilderFactory catBuilderFactory )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = null;
    this.datasetNodeBuilder = null;
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

  protected DatasetNodeBuilder parseStartElement( XMLEvent event )
          throws ThreddsXmlParserException
  {
    if ( !event.isStartElement() )
      throw new IllegalArgumentException( "Event must be start element." );
    StartElement startElement = event.asStartElement();

    // Get required attributes.
    Attribute titleAtt = startElement.getAttributeByName( titleAttName );
    String title = titleAtt.getValue();
    Attribute hrefAtt = startElement.getAttributeByName( hrefAttName );
    String href = hrefAtt.getValue();
    URI hrefUri = null;
    try
    {
      hrefUri = new URI( href );
    }
    catch ( URISyntaxException e )
    {
      log.error( "parseElement(): Bad catalog base URI [" + href + "]: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Bad catalog base URI [" + href + "]: " + e.getMessage(), e );
    }

    // Construct builder.
    CatalogRefBuilder catalogRefBuilder = null;
    if ( this.catBuilder != null )
      catalogRefBuilder = this.catBuilder.addCatalogRef( title, hrefUri );
    else if ( this.datasetNodeBuilder != null )
      catalogRefBuilder = this.datasetNodeBuilder.addCatalogRef( title, hrefUri );
    else if ( catBuilderFactory != null )
      catalogRefBuilder = catBuilderFactory.newCatalogRefBuilder( title, hrefUri );
    else
      throw new ThreddsXmlParserException( "" );

    // Set optional attributes
    DatasetNodeElementParserUtils.parseStartElementIdAttribute( startElement, catalogRefBuilder );
    DatasetNodeElementParserUtils.parseStartElementIdAuthorityAttribute( startElement, catalogRefBuilder );

    return catalogRefBuilder;
  }
  private StringBuilder unknownContent = null;
  protected void handleChildStartElement( StartElement startElement, ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( !( builder instanceof CatalogRefBuilder ) )
      throw new IllegalArgumentException( "Given ThreddsBuilder must be an instance of DatasetBuilder." );

    if ( DatasetNodeElementParserUtils.handleBasicChildStartElement( startElement, this.reader, (CatalogRefBuilder) builder ))
      return;
    else
      StaxThreddsXmlParserUtils.readElementAndAnyContent( this.reader );
  }

  protected void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    return;
  }
}
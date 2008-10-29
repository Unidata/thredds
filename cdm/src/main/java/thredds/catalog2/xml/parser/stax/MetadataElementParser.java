package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.*;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.util.MetadataElementUtils;

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
public class MetadataElementParser extends AbstractElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                      MetadataElementUtils.ELEMENT_NAME );
  private final static QName titleAttName = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                                                       MetadataElementUtils.XLINK_TITLE_ATTRIBUTE_NAME );
  private final static QName externalRefAttName = new QName( CatalogNamespace.XLINK.getNamespaceUri(),
                                                             MetadataElementUtils.XLINK_REFERENCE_ATTRIBUTE_NAME );

  private final DatasetNodeBuilder datasetBuilder;
  private final CatalogBuilderFactory catBuilderFactory;

  public MetadataElementParser( XMLEventReader reader, DatasetNodeBuilder datasetNodeBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.datasetBuilder = datasetNodeBuilder;
    this.catBuilderFactory = null;
  }

  public MetadataElementParser( XMLEventReader reader, CatalogBuilderFactory catBuilderFactory )
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

  protected MetadataBuilder parseStartElement( StartElement startElement )
          throws ThreddsXmlParserException
  {
    if ( ! startElement.getName().equals( elementName ))
      throw new IllegalArgumentException( "Start element is not 'metadata' element.");

    MetadataBuilder builder = null;
    if ( this.datasetBuilder != null )
      builder = this.datasetBuilder.addMetadata();
    else if ( catBuilderFactory != null )
      builder = catBuilderFactory.newMetadataBuilder();
    else
      throw new ThreddsXmlParserException( "" );

    Attribute titleAtt = startElement.getAttributeByName( titleAttName );
    Attribute externalRefAtt = startElement.getAttributeByName( externalRefAttName );
    if ( titleAtt == null && externalRefAtt == null )
    {
      builder.setContainedContent( true );
      return builder;
    }
    if ( titleAtt == null || externalRefAtt == null )
      throw new ThreddsXmlParserException( "MetadataBuilder with link has a null title or link URL ");
    String title = titleAtt.getValue();
    String uriString = externalRefAtt.getValue();
    URI uri = null;
    try
    {
      uri = new URI( uriString );
    }
    catch ( URISyntaxException e )
    {
      throw new ThreddsXmlParserException( "MetadataBuilder with link has link with bad URI syntax.", e);
    }

    builder.setContainedContent( false );
    builder.setTitle( title );
    builder.setExternalReference( uri );

    return builder;
  }
  private StringBuilder content = null;
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
      if ( this.content == null )
        this.content = new StringBuilder();
      //if ( !isChildElement( startElement ) )
      this.content.append( StaxThreddsXmlParserUtils.readElementAndAnyContent( this.reader ));
    }
  }

  protected void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( ! ( builder instanceof MetadataBuilder ) )
      throw new IllegalArgumentException( "Builder must be a MetadataBuilder.");
    MetadataBuilder mdBldr = (MetadataBuilder) builder;
    if ( this.content != null )
      mdBldr.setContent( this.content.toString() );
    
    return;
  }
}
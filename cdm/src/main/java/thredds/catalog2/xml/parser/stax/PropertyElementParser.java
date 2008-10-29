package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.DatasetNodeBuilder;
import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.util.PropertyElementUtils;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;

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
public class PropertyElementParser extends AbstractElementParser
{
  private org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( getClass() );

  private final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                      PropertyElementUtils.ELEMENT_NAME );
  private final static QName nameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      PropertyElementUtils.NAME_ATTRIBUTE_NAME );
  private final static QName valueAttName = new QName( XMLConstants.NULL_NS_URI,
                                                       PropertyElementUtils.VALUE_ATTRIBUTE_NAME );

  public boolean isChildElement( XMLEvent event )
  { return false; //property doesn't contain any children
  }

  private final CatalogBuilder catBuilder;
  private final DatasetNodeBuilder datasetNodeBuilder;
  private final ServiceBuilder serviceBuilder;

  public PropertyElementParser( XMLEventReader reader,  CatalogBuilder catBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, elementName);
    this.catBuilder = catBuilder;
    this.datasetNodeBuilder = null;
    this.serviceBuilder = null;
  }

  public PropertyElementParser( XMLEventReader reader,  DatasetNodeBuilder datasetNodeBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = null;
    this.datasetNodeBuilder = datasetNodeBuilder;
    this.serviceBuilder = null;
  }

  public PropertyElementParser( XMLEventReader reader, ServiceBuilder serviceBuilder )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.catBuilder = null;
    this.datasetNodeBuilder = null;
    this.serviceBuilder = serviceBuilder;
  }

  protected static boolean isSelfElementStatic( XMLEvent event )
  {
    return isSelfElement( event, elementName );
  }

  protected boolean isSelfElement( XMLEvent event )
  {
    return isSelfElement( event, elementName );
  }

  protected ThreddsBuilder parseStartElement( StartElement startElement )
          throws ThreddsXmlParserException
  {
    if ( ! startElement.getName().equals( elementName ) )
      throw new IllegalArgumentException( "Start element must be a 'property' element.");

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

    return null;
  }

  protected void handleChildStartElement( StartElement startElement, ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( ! isChildElement( startElement ) )
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.readElementAndAnyContent( this.reader );
  }

  protected void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    return;
  }
}
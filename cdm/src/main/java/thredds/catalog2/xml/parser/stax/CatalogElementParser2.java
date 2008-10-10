package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.CatalogBuilderFactory;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.util.CatalogElementUtils;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;
import javax.xml.XMLConstants;
import java.util.Date;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogElementParser2 extends AbstractElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final static QName elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                                      CatalogElementUtils.ELEMENT_NAME );
  private final static QName nameAttName = new QName( XMLConstants.NULL_NS_URI,
                                                      CatalogElementUtils.NAME_ATTRIBUTE_NAME );
  private final static QName versionAttName = new QName( XMLConstants.NULL_NS_URI,
                                                         CatalogElementUtils.VERSION_ATTRIBUTE_NAME );
  private final static QName expiresAttName = new QName( XMLConstants.NULL_NS_URI,
                                                         CatalogElementUtils.EXPIRES_ATTRIBUTE_NAME );
  private final static QName lastModifiedAttName = new QName( XMLConstants.NULL_NS_URI,
                                                              CatalogElementUtils.LAST_MODIFIED_ATTRIBUTE_NAME );

  private final String docBaseUriString;
  private final CatalogBuilderFactory catBuilderFactory;

  public CatalogElementParser2( String docBaseUriString, XMLEventReader reader,  CatalogBuilderFactory catBuilderFactory )
          throws ThreddsXmlParserException
  {
    super( reader, elementName );
    this.docBaseUriString = docBaseUriString;
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

  protected CatalogBuilder parseStartElement( XMLEvent event )
          throws ThreddsXmlParserException
  {
    if ( !event.isStartElement() )
      throw new IllegalArgumentException( "Event must be start element." );
    StartElement startCatElem = event.asStartElement();

    Attribute nameAtt = startCatElem.getAttributeByName( nameAttName );
    String nameString = nameAtt.getValue();
    Attribute versionAtt = startCatElem.getAttributeByName( versionAttName );
    String versionString = versionAtt.getValue();
    Attribute expiresAtt = startCatElem.getAttributeByName( expiresAttName );
    Date expiresDate = null;
    Attribute lastModifiedAtt = startCatElem.getAttributeByName( lastModifiedAttName );
    Date lastModifiedDate = null;
    URI docBaseUri = null;
    try
    {
      docBaseUri = new URI( docBaseUriString );
    }
    catch ( URISyntaxException e )
    {
      log.error( "parseElement(): Bad catalog base URI [" + docBaseUriString + "]: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Bad catalog base URI [" + docBaseUriString + "]: " + e.getMessage(), e );
    }
    return catBuilderFactory.newCatalogBuilder( nameString, docBaseUri, versionString, expiresDate, lastModifiedDate );
  }

  protected void handleChildStartElement( StartElement startElement, ThreddsBuilder builder )
          throws ThreddsXmlParserException
  {
    if ( !( builder instanceof CatalogBuilder ) )
      throw new IllegalArgumentException( "Given ThreddsBuilder must be an instance of DatasetBuilder." );
    CatalogBuilder catalogBuilder = (CatalogBuilder) builder;

    if ( ServiceElementParser2.isSelfElementStatic( startElement ) )
    {
      ServiceElementParser2 serviceElemParser = new ServiceElementParser2( this.reader, catalogBuilder );
      serviceElemParser.parse();
    }
    else if ( PropertyElementParser2.isSelfElementStatic( startElement ) )
    {
      PropertyElementParser2 parser = new PropertyElementParser2( this.reader, catalogBuilder );
      parser.parse();
    }
    else if ( DatasetElementParser2.isSelfElementStatic( startElement ) )
    {
      DatasetElementParser2 parser = new DatasetElementParser2( this.reader, catalogBuilder );
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
//    if ( !( builder instanceof CatalogBuilder ) )
//      throw new IllegalArgumentException( "Given ThreddsBuilder must be an instance of DatasetBuilder." );
//    CatalogBuilder catalogBuilder = (CatalogBuilder) builder;
  }
}
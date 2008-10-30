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
import javax.xml.namespace.QName;
import javax.xml.XMLConstants;
import java.util.Date;
import java.net.URI;
import java.net.URISyntaxException;

import ucar.nc2.units.DateType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogElementParser extends AbstractElementParser
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

  public CatalogElementParser( String docBaseUriString, XMLEventReader reader,  CatalogBuilderFactory catBuilderFactory )
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

  protected CatalogBuilder parseStartElement( StartElement startElement )
          throws ThreddsXmlParserException
  {
    if ( ! startElement.getName().equals( elementName ))
      throw new IllegalArgumentException( "Start element not 'catalog' element.");

    Attribute nameAtt = startElement.getAttributeByName( nameAttName );
    String nameString = nameAtt != null ? nameAtt.getValue() : null ;

    Attribute versionAtt = startElement.getAttributeByName( versionAttName );
    String versionString = versionAtt != null ? versionAtt.getValue() : null;
    Attribute expiresAtt = startElement.getAttributeByName( expiresAttName );
    // ToDo Date expiresDate = expiresAtt != null ? new DateType( expiresAtt.getValue(), null, null).getDate() : null;
    Date expiresDate = null;
    Attribute lastModifiedAtt = startElement.getAttributeByName( lastModifiedAttName );
    // ToDo Date lastModifiedDate = lastModifiedAtt != null ? new DateType( lastModifiedAtt.getValue(), null, null).getDate() : null;
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

    if ( ServiceElementParser.isSelfElementStatic( startElement ) )
    {
      ServiceElementParser serviceElemParser = new ServiceElementParser( this.reader, catalogBuilder );
      serviceElemParser.parse();
    }
    else if ( PropertyElementParser.isSelfElementStatic( startElement ) )
    {
      PropertyElementParser parser = new PropertyElementParser( this.reader, catalogBuilder );
      parser.parse();
    }
    else if ( DatasetElementParser.isSelfElementStatic( startElement ) )
    { // ToDo Not sure about the null parameter?
      DatasetElementParser parser = new DatasetElementParser( this.reader, catalogBuilder, null );
      parser.parse();
    }
    else
    {
      // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
      StaxThreddsXmlParserUtils.readElementAndAnyContent( this.reader );
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
package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.CatalogBuilderFactory;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.xml.parser.CatalogNamespace;
import thredds.catalog2.xml.parser.CatalogParserException;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;
import java.util.Date;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogElementParser
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( CatalogElementParser.class );

  private final static QName versionAttName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), "version" );
  private final static QName expiresAttName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), "expires" );
  private final static QName lastModifiedAttName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), "lastModified" );

  private final String baseUriString;
  private final XMLEventReader reader;
  private final CatalogBuilderFactory catBuilderFactory;

  public CatalogElementParser( String baseUriString, XMLEventReader reader,  CatalogBuilderFactory catBuilderFactory )
          throws CatalogParserException
  {
    this.baseUriString = baseUriString;
    this.reader = reader;
    this.catBuilderFactory = catBuilderFactory;
  }

  public void parse()
          throws CatalogParserException
  {
    try
    {
      StartElement startCatElem = (StartElement) reader.nextEvent();
      Attribute versionAtt = startCatElem.getAttributeByName( versionAttName );
      String versionString = versionAtt.getValue();
      Attribute expiresAtt = startCatElem.getAttributeByName( expiresAttName );
      Date expiresDate = null;
      Attribute lastModifiedAtt = startCatElem.getAttributeByName( lastModifiedAttName );
      Date lastModifiedDate = null;
      URI baseUri = new URI( baseUriString);
      CatalogBuilder catBuilder =
              catBuilderFactory.newCatalogBuilder(
                      startCatElem.getName().getLocalPart(),
                      baseUri, versionString, expiresDate, lastModifiedDate );

      while ( reader.hasNext() )
      {
        XMLEvent event = reader.peek();
        if ( event.isStartElement() )
        {
          StartElement se = event.asStartElement();
          if ( se.getName().getNamespaceURI().equals( CatalogNamespace.CATALOG_1_0.getNamespaceUri() )
               && se.getName().getLocalPart().equals( "service" ) )
          {
            System.out.println( "start : " + se.getName().getLocalPart() );
          }
          System.out.println( "start : " + se.getName().getLocalPart() );
        }
        else if ( event.isEndElement() )
        {
          reader.next();
          return;
        }
        else
        {
          reader.next();
          continue;
        }
      }
    }
    catch ( XMLStreamException e )
    {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    catch ( URISyntaxException e )
    {
      throw new CatalogParserException( "", e );
    }


  }
}

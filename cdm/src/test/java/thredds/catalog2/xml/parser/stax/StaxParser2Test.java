package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.simpleImpl.ThreddsBuilderFactoryImpl;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.parser.ThreddsXmlParserIssue;
import thredds.catalog2.xml.parser.CatalogXmlUtils;
import thredds.catalog2.xml.names.CatalogElementNames;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;
import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.StringReader;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class StaxParser2Test
{
  @Test
  public void doSomeStuff()
          throws ThreddsXmlParserException,
                 URISyntaxException,
                 XMLStreamException
  {
    String catDocBaseUri = "http://motherlode.ucar.edu/thredds/idd/models.xml";
    String catAsString = CatalogXmlUtils.getCatalogWithCompoundService( null );

    Source source = new StreamSource( new StringReader( catAsString ), catDocBaseUri.toString() );

    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty( "javax.xml.stream.isCoalescing", Boolean.TRUE );
    factory.setProperty( "javax.xml.stream.supportDTD", Boolean.FALSE );
    XMLEventReader eventReader = factory.createXMLEventReader( source );

    CatalogElementParser2Factory catParserFac = new CatalogElementParser2Factory( CatalogElementNames.CatalogElement);

    ThreddsBuilderFactory catBuilderFac = new ThreddsBuilderFactoryImpl();
    ThreddsBuilder threddsBuilder = null;
    while ( eventReader.hasNext() )
    {
      XMLEvent event = eventReader.peek();
      if ( event.isEndDocument() ) {
        eventReader.next();
        break;
      }
      else if ( event.isStartDocument() ) {
        eventReader.next();
      }
      else if ( event.isStartElement() )
      {
        if ( catParserFac.isEventMyStartElement( event.asStartElement() ) ) {
          CatalogElementParser2 catElemParser = (CatalogElementParser2) catParserFac.getNewElementParser( source.getSystemId(), eventReader, catBuilderFac );
          threddsBuilder = catElemParser.parseElement();
        }
        else
        {
          ThreddsXmlParserIssue issue = StaxThreddsXmlParserUtils.createIssueForUnexpectedElement( "Expected catalog element not found", eventReader );
          fail( issue.getMessage());
        }
      }
      else if ( event.isEndElement() )
      {
        ThreddsXmlParserIssue issue = StaxThreddsXmlParserUtils.createIssueForUnexpectedElement( "Unexpected end element", eventReader );
        fail( issue.getMessage() );
      }
      else
      {
        eventReader.next();
        continue;
      }
    }

    eventReader.close();

    assertNotNull( threddsBuilder);

  }
}

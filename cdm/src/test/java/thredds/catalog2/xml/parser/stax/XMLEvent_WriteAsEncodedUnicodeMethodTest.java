package thredds.catalog2.xml.parser.stax;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import thredds.catalog2.xml.parser.CatalogXmlUtils;

import java.io.*;

/**
 * Test whether the XMLEvent.writeAsEncodedUnicode(Writer) method is working.
 *
 * <p>It is not implemented in Sun's JDK 6u14; though XMLEvent.toString()
 * seems to be implemented in an appropriate manner. However, other JDK
 * implementations (GNU's classpath e.g.) do implement writeAsEncodedUnicode(Writer)
 * but not toString(). So, if this test fails, it probably means that Sun's JDK
 * has been fixed. Of course, need to work on old JDK's for awhile.
 *
 * <p>Once things are working, revisit
 * StaxThreddsXmlParserUtils.readCharacterContent(StartElement,XMLEventReader) and
 * StaxThreddsXmlParserUtils.readElementAndAnyContent(XMLEventReader).
 *
 *
 * @author edavis
 * @since 4.0
 */
public class XMLEvent_WriteAsEncodedUnicodeMethodTest
{
  private XMLInputFactory factory;

  public XMLEvent_WriteAsEncodedUnicodeMethodTest() { }

  @Before
  public void init()
  {
    this.factory = XMLInputFactory.newInstance();
    this.factory.setProperty( "javax.xml.stream.isCoalescing", Boolean.TRUE );
    this.factory.setProperty( "javax.xml.stream.supportDTD", Boolean.FALSE );
//    this.factory.setXMLReporter(  );
//    this.factory.setXMLResolver(  );

  }

  @Test
  public void tryWriteAsEncodedUnicode()
          throws XMLStreamException
  {
    String xml = CatalogXmlUtils.wrapThreddsXmlInCatalog( "<serviceName>OPeNDAP</serviceName>", null );
    String baseUriString = "http://test.metadata.parser/tmd.xml";

    Reader reader = new StringReader( xml );
    Source source = new StreamSource( reader, baseUriString );
    XMLEventReader eventReader = factory.createXMLEventReader( source );

    StringWriter writer = new StringWriter();

    while ( eventReader.hasNext() )
    {
      XMLEvent event = eventReader.nextEvent();
      event.writeAsEncodedUnicode( writer );
    }

    writer.flush();
    String resultingXml = writer.toString();
    assertTrue( "XMLEvent.writeAsEncodedUnicode() is working.\n" + resultingXml,
                resultingXml.equals( "" ));
  }
}

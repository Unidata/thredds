package thredds.catalog2.xml.parser.stax;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import javax.xml.stream.*;

import thredds.catalog2.xml.parser.ThreddsXmlParserException;

/**
 * Test utility methods in thredds.catalog2.xml.parser.stax.StaxThreddsXmlParserUtils.
 *
 * @author edavis
 * @since 4.0
 */
public class StaxThreddsXmlParserUtilsTest
{
  public StaxThreddsXmlParserUtilsTest() { }

  /**
   * Check consumeElementAndConvertToXmlString() method by comparing an input XML element with the resulting text.
   *
   * @throws ThreddsXmlParserException
   * @throws XMLStreamException
   */
  @Test
  public void checkConsumeElementAndConvertToXmlString()
          throws ThreddsXmlParserException, XMLStreamException
  {
    // Create string containing <description> element.
    String descriptionElementAsString = new StringBuilder()
            .append( "<description xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\">")
            .append( "Some really <strong>important</strong> text.")
            .append( "</description>" )
            .toString();

    // Add XML header to create <description> document as String.
    String descriptionDocumentAsString = "<?xml version='1.0' encoding='UTF-8'?>" + descriptionElementAsString;

    // Round-trip the <description> element. I.e., parse XML doc to events and
    // write events as XML to String, starting with startElement event.
    XMLEventReader descriptionDocEventReader
            = StaxParserTestUtils.createXmlEventReaderOnXmlString( descriptionDocumentAsString, "http://server/path/file1.xml" );
    skipToFirstStartElement( descriptionDocEventReader );
    String descriptionElementAfterFirstRoundTripAsString
            = StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( descriptionDocEventReader );

    // Check that round-trip-ed equals original.
    assertEquals( descriptionElementAsString, descriptionElementAfterFirstRoundTripAsString );

    // Create string containing XML doc from round-trip-ed <description> element string.
    String descriptionDocumentAfterFirstRoundTripAsString = new StringBuilder()
            .append( "<?xml version='1.0' encoding='UTF-8'?>\n" )
            .append( descriptionElementAfterFirstRoundTripAsString ).toString();

    // Perform second round-trip.
    XMLEventReader descriptionDocumentAfterRoundTripEventReader = StaxParserTestUtils.createXmlEventReaderOnXmlString( descriptionDocumentAfterFirstRoundTripAsString, "http://server/path/file2.xml" );
    skipToFirstStartElement( descriptionDocumentAfterRoundTripEventReader );
    String descriptionElementAfterSecondRoundTripAsString = StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( descriptionDocumentAfterRoundTripEventReader );

    // Check that text after second round-trip equals original text.
    assertEquals( descriptionElementAsString, descriptionElementAfterSecondRoundTripAsString );
  }

  private void skipToFirstStartElement( XMLEventReader eventReader )
          throws XMLStreamException
  {
    while ( ! eventReader.peek().isStartElement() )
      eventReader.nextEvent();
    assert eventReader.peek().isStartElement();
  }
}

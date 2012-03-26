package thredds.catalog2.xml.parser.stax;

import org.junit.Test;
import static org.junit.Assert.*;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.namespace.QName;
import java.util.Map;
import java.util.HashMap;

import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.names.CatalogNamespace;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DateTypeParserTest
{
  @Test
  public void checkCompleteDateElement() throws XMLStreamException, ThreddsXmlParserException
  {
    String elemName = "date";
    String date = "2009-09-15T12:15";
    String format = "format";
    String type = "type";

    QName elemQualName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), elemName );
    String xml = getDateTypeElement( elemName, date, format, type );

    assertDateTypeXmlAsExpected( elemQualName, date, format, type, xml );
  }

  @Test
  public void checkCompleteAltNamedDateElement() throws XMLStreamException, ThreddsXmlParserException
  {
    String elemName = "myDate";
    String date = "2009-09-15T12:15";
    String format = "format";
    String type = "type";

    QName elemQualName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), elemName );
    String xml = getDateTypeElement( elemName, date, format, type );

    assertDateTypeXmlAsExpected( elemQualName, date, format, type, xml );
  }

  @Test
  public void checkFormattedDateElement() throws XMLStreamException, ThreddsXmlParserException
  {
    String elemName = "date";
    String date = "2009-09-15T12:15";
    String format = "format";

    QName elemQualName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), elemName );
    String xml = getDateTypeElement( elemName, date, format, null );

    assertDateTypeXmlAsExpected( elemQualName, date, format, null, xml );
  }

  @Test
  public void checkBareDateElement() throws XMLStreamException, ThreddsXmlParserException
  {
    String elemName = "date";
    String date = "2009-09-15T12:15";

    QName elemQualName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), elemName );
    String xml = getDateTypeElement( elemName, date, null, null );

    assertDateTypeXmlAsExpected( elemQualName, date, null, null, xml );
  }

  @Test
  public void checkEmptyDateElement() throws XMLStreamException, ThreddsXmlParserException
  {
    String elemName = "date";
    String format = "format";
    String type = "type";

    QName elemQualName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), elemName );
    String xml = getDateTypeElement( elemName, null, format, type );

    assertDateTypeXmlAsExpected( elemQualName, "", format, type, xml );
  }

  private String getDateTypeElement( String dateElementName, String dateString, String format, String type )
  {
    Map<String, String> attributes = new HashMap<String, String>();
    if ( format != null )
      attributes.put( "format", format );
    if ( type != null )
      attributes.put( "type", type );

    return StaxParserUtils.wrapContentXmlInXmlDocRootElement( dateElementName, attributes, dateString );
  }

  private void assertDateTypeXmlAsExpected( QName elemName, String date, String format, String type, String xml )
          throws XMLStreamException, ThreddsXmlParserException
  {
    XMLEventReader reader = StaxParserUtils.createXmlEventReaderOnXmlString( xml, "http://test.catalog2.thredds/DateTypeParserTest/someTest.xml" );

    DateTypeParser.Factory fac = new DateTypeParser.Factory( elemName );
    StaxParserUtils.advanceReaderToFirstStartElement( reader );
    assertTrue( fac.isEventMyStartElement( reader.peek() ));

    DateTypeParser dateTypeParser = fac.getNewDateTypeParser();
    assertNotNull( dateTypeParser );

    dateTypeParser.parseElement( reader );

    assertEquals( date, dateTypeParser.getValue());
    assertEquals( format, dateTypeParser.getFormat());
    assertEquals( type, dateTypeParser.getType());
  }
}

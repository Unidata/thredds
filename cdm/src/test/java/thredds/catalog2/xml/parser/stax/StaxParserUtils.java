package thredds.catalog2.xml.parser.stax;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.util.Map;
import java.io.Reader;
import java.io.StringReader;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class StaxParserUtils
{
  private StaxParserUtils() {}

  static String wrapContentXmlInXmlDocRootElement( String rootElementName, Map<String,String> rootElementAttributes,
                                                   String contentXml )
  {
    StringBuilder sb = new StringBuilder()
            .append( "<?xml version='1.0' encoding='UTF-8'?>\n" )
            .append( "<" ).append( rootElementName )
            .append( " xmlns='http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0'\n" );
    if ( rootElementAttributes != null )
      for ( Map.Entry<String,String> atts : rootElementAttributes.entrySet())
        sb.append( " ").append( atts.getKey()).append( "='").append( atts.getValue()).append( "'");
    if ( contentXml == null || contentXml.isEmpty())
      sb.append( " />");
    else
    {
      sb.append( ">\n" )
              .append( contentXml )
              .append( "</" ).append( rootElementName ).append( ">" );
    }
    return sb.toString();
  }

  static XMLEventReader createXmlEventReaderOnXmlString( String xml, String docBaseUri )
          throws XMLStreamException
  {
    Reader stringReader = new StringReader( xml );
    Source source = new StreamSource( stringReader, docBaseUri.toString() );
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty( "javax.xml.stream.isCoalescing", Boolean.TRUE );
    factory.setProperty( "javax.xml.stream.supportDTD", Boolean.FALSE );

    return factory.createXMLEventReader( source );
  }

  static void advanceReaderToFirstStartElement( XMLEventReader reader )
          throws XMLStreamException
  {
    while ( reader.hasNext() )
    {
      XMLEvent event = reader.peek();
      if ( event.isStartElement() )
        break;
      else if ( event.isCharacters() )
        event = reader.nextEvent();
      else if ( event.isStartDocument() )
        event = reader.nextEvent();
      else
        throw new IllegalStateException( "Unexpected event [" + event + "]." );
    }
  }
}

package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.simpleImpl.ThreddsBuilderFactoryImpl;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class DocumentParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  ThreddsBuilder parseMetadataIntoBuilder( XMLEventReader reader, String docBaseUri )
          throws ThreddsXmlParserException
  {
    try
    {
      ThreddsBuilderFactory catBuilderFac = new ThreddsBuilderFactoryImpl();
      CatalogBuilder catBuilder = null;
      while ( reader.hasNext() )
      {
        XMLEvent event = reader.peek();
        if ( event.isEndDocument() )
        {
          reader.next();
          continue;
        }
        else if ( event.isStartDocument() )
        {
          reader.next();
          continue;
        }
        else if ( event.isStartElement() )
        {
          if ( CatalogElementParser.isSelfElementStatic( event.asStartElement() ) )
          {
            CatalogElementParser catElemParser = new CatalogElementParser( docBaseUri, reader, catBuilderFac );
            catBuilder = (CatalogBuilder) catElemParser.parse();
          }
          else
          {
            // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
            StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( reader );
            log.warn( "readCatalogXML(): Unrecognized start element [" + event.asStartElement().getName() + "]." );
            reader.next();
            continue;
          }
        }
        else if ( event.isEndElement() )
        {
          if ( CatalogElementParser.isSelfElementStatic( event.asEndElement() ) )
          {
            break;
          }
          else
          {
            log.error( "readCatalogXML(): Unrecognized end element [" + event.asEndElement().getName() + "]." );
            break;
          }
        }
        else
        {
          log.debug( "readCatalogXML(): Unhandled event [" + event.getLocation() + "--" + event + "]." );
          reader.next();
          continue;
        }
      }

      reader.close();

      if ( catBuilder == null )
        return null;

      return catBuilder;
    }

    catch ( XMLStreamException e )
    {
      log.error( "readCatalogXML(): Failed to parse catalog document: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Failed to parse catalog document: " + e.getMessage(), e );
    }
//    catch ( BuilderException e )
//    {
//      log.error( "readCatalogXML(): Failed to parse catalog document: " + e.getMessage(), e );
//      throw new ThreddsXmlParserException( "Failed to parse catalog document: " + e.getMessage(), e );
//    }

  }

}

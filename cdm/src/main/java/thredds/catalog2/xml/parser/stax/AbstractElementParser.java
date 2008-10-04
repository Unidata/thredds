package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.xml.parser.CatalogParserException;
import thredds.catalog2.xml.CatalogNamespace;

import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLEventReader;
import javax.xml.namespace.QName;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public abstract class AbstractElementParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private final QName elementName;

  AbstractElementParser( String elementNameLocalPart )
  {
    if ( elementNameLocalPart == null )
      throw new IllegalArgumentException( "Element name may not be null.");
    this.elementName = new QName( CatalogNamespace.CATALOG_1_0.getNamespaceUri(),
                                  elementNameLocalPart );
  }

  public boolean isSelfElement( XMLEvent event )
  {
    QName elemName = null;
    if ( event.isStartElement() )
      elemName = event.asStartElement().getName();
    else if ( event.isEndElement() )
      elemName = event.asEndElement().getName();
    else
      return false;

    if ( elemName.equals( elementName ) )
      return true;
    return false;

  }

  abstract ThreddsBuilder parseElement( XMLEvent event )
          throws CatalogParserException;
  abstract void handleStartElement( StartElement startElement, ThreddsBuilder builder )
          throws CatalogParserException;

  public ThreddsBuilder parse( XMLEventReader reader )
          throws CatalogParserException
  {
    try
    {
      ThreddsBuilder builder = this.parseElement( reader.nextEvent() );

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
          this.handleStartElement( event.asStartElement(), builder );
        }
        else if ( event.isEndElement() )
        {
          if ( this.isSelfElement( event.asEndElement() ) )
          {
            return builder;
          }
          else
          {
            log.warn( "parse(): Unrecognized end element [" + event.asEndElement().getName() + "]." );
            reader.next();
            continue;
          }
        }
        else
        {
          log.debug( "parse(): Unhandled event [" + event.getLocation() + "--" + event + "]." );
          reader.next();
          continue;
        }
      }
      throw new CatalogParserException( "Unexpected end of document.");
    }
    catch ( XMLStreamException e )
    {
      throw new CatalogParserException( "", e );
    }

  }
}

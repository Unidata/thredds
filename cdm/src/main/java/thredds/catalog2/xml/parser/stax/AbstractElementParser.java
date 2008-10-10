package thredds.catalog2.xml.parser.stax;

import thredds.catalog2.builder.ThreddsBuilder;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;

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

  protected final XMLEventReader reader;
  protected final QName elementName;

  AbstractElementParser( XMLEventReader reader, QName elementName )
  {
    this.reader = reader;
    this.elementName = elementName;
  }

  protected static boolean isSelfElement( XMLEvent event, QName selfElementName )
  {
    QName elemName = null;
    if ( event.isStartElement() )
      elemName = event.asStartElement().getName();
    else if ( event.isEndElement() )
      elemName = event.asEndElement().getName();
    else
      return false;

    if ( elemName.equals( selfElementName ) )
      return true;
    return false;
  }

  protected abstract boolean isSelfElement( XMLEvent event );

  protected abstract ThreddsBuilder parseStartElement( XMLEvent event )
          throws ThreddsXmlParserException;

  protected abstract void handleChildStartElement( StartElement startElement, ThreddsBuilder builder )
          throws ThreddsXmlParserException;

  protected abstract void postProcessing( ThreddsBuilder builder )
          throws ThreddsXmlParserException;

  public final ThreddsBuilder parse()
          throws ThreddsXmlParserException
  {
    try
    {
      ThreddsBuilder builder = this.parseStartElement( this.reader.nextEvent() );

      while ( this.reader.hasNext() )
      {
        XMLEvent event = this.reader.peek();
        if ( event.isStartElement() )
        {
          this.handleChildStartElement( event.asStartElement(), builder );
        }
        else if ( event.isEndElement() )
        {
          if ( this.isSelfElement( event.asEndElement() ) )
          {
            this.reader.next();
            break;
          }
          else
          {
            log.error( "parse(): Unrecognized end element [" + event.asEndElement().getName() + "]." );
            this.reader.next();
            continue;
          }
        }
        else
        {
          log.debug( "parse(): Unhandled event [" + event.getLocation() + "--" + event + "]." );
          this.reader.next();
          continue;
        }
      }

      this.postProcessing( builder );
      return builder;
    }
    catch ( XMLStreamException e )
    {
      log.error( "parse(): Failed to parse " + this.elementName + " element: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Failed to parse " + this.elementName + " element: " + e.getMessage(), e );
    }

  }
}

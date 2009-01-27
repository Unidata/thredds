/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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

  protected abstract ThreddsBuilder parseStartElement( StartElement startElement )
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
      XMLEvent event = this.reader.nextEvent();
      if ( ! event.isStartElement() )
        throw new ThreddsXmlParserException( "Next XML event not a start element.");
      ThreddsBuilder builder = this.parseStartElement( event.asStartElement() );

      while ( this.reader.hasNext() )
      {
        event = this.reader.peek();
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

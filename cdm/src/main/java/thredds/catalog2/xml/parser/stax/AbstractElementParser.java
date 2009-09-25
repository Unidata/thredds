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
import thredds.catalog2.builder.ThreddsBuilderFactory;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.parser.ThreddsXmlParserIssue;

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
abstract class AbstractElementParser
{
  org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  final QName elementName;
  final XMLEventReader reader;
  final ThreddsBuilderFactory builderFactory;

  AbstractElementParser( QName elementName, XMLEventReader reader,
                         ThreddsBuilderFactory builderFactory )
  {
    if ( elementName == null || reader == null || builderFactory == null )
      throw new IllegalArgumentException( "Element name, XMLEventReader, and/or BuilderFactory may not be null.");

    this.elementName = elementName;
    this.reader = reader;
    this.builderFactory = builderFactory;
  }

  boolean isSelfElement( XMLEvent event ) {
    return StaxThreddsXmlParserUtils.isEventStartOrEndElementWithMatchingName( event, this.elementName );
  }

  abstract void parseStartElement()
          throws ThreddsXmlParserException;

  abstract void handleChildStartElement()
          throws ThreddsXmlParserException;

  abstract void postProcessingAfterEndElement()
          throws ThreddsXmlParserException;

  abstract ThreddsBuilder getSelfBuilder();

  final ThreddsBuilder parse()
          throws ThreddsXmlParserException
  {
    try
    {
      this.parseStartElement();

      while ( this.reader.hasNext() )
      {
        XMLEvent event = this.reader.peek();
        if ( event.isStartElement() )
        {
          this.handleChildStartElement();
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
            if ( this instanceof ThreddsMetadataElementParser )
            {
              if ( log.isDebugEnabled())
              {
                String msg = "End element probably parent of ThreddsMetadata [" + event.asEndElement().getName() + "].";
                ThreddsXmlParserIssue issue = StaxThreddsXmlParserUtils
                        .createIssueForUnexpectedEvent( msg, ThreddsXmlParserIssue.Severity.WARNING,
                                                        this.reader, event );
                // ToDo Figure out a better way to deal with this situation.
                log.debug( "parse(): " + issue.getMessage() );
              }
              break;
            }
            else
            {
              String msg = "Unrecognized end element [" + event.asEndElement().getName() + "].";
              ThreddsXmlParserIssue issue = StaxThreddsXmlParserUtils
                      .createIssueForUnexpectedEvent( msg, ThreddsXmlParserIssue.Severity.FATAL,
                                                      this.reader, event );
              log.error( this.getClass().getName() + ".parse(): " + issue.getMessage() );
              // ToDo Gather issues (and "this.reader.next(); continue;") rather than throw exception.
              throw new ThreddsXmlParserException( issue );
            }
          }
        }
        else
        {
          log.debug( this.getClass().getName() + ".parse(): Unhandled event [" + event.getLocation() + "--" + event + "]." );
          this.reader.next();
          continue;
        }
      }

      this.postProcessingAfterEndElement();
      return this.getSelfBuilder();
    }
    catch ( XMLStreamException e )
    {
      log.error( "parse(): Failed to parse " + this.elementName + " element: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Failed to parse " + this.elementName + " element: " + e.getMessage(), e );
    }

  }

  StartElement getNextEventIfStartElementIsMine()
          throws ThreddsXmlParserException
  {
    if ( ! this.reader.hasNext() )
      throw new ThreddsXmlParserException( "XMLEventReader has no further events." );

    StartElement startElement = null;
    try
    {
      XMLEvent event = this.reader.peek();
      if ( ! event.isStartElement() )
        throw new ThreddsXmlParserException( "Next event must be StartElement." );

      if ( ! event.asStartElement().getName().equals( this.elementName ) )
        throw new ThreddsXmlParserException( "Start element must be an '" + this.elementName.getLocalPart() + "' element." );

      startElement = this.reader.nextEvent().asStartElement();
    }
    catch ( XMLStreamException e )
    {
      throw new ThreddsXmlParserException( "Problem reading from XMLEventReader." );
    }

    return startElement;
  }

  StartElement peekAtNextEventIfStartElement()
          throws ThreddsXmlParserException
  {
    if ( ! this.reader.hasNext() )
      throw new ThreddsXmlParserException( "XMLEventReader has no further events." );

    StartElement startElement = null;
    while ( this.reader.hasNext() )
    {
      XMLEvent event = null;
      try
      {
        event = this.reader.peek();
      }
      catch (XMLStreamException e)
      {
        String msg = "Problem reading from XMLEventReader.";
        ThreddsXmlParserIssue issue = StaxThreddsXmlParserUtils
                .createIssueForException( msg, this.reader, e);
        log.error("peekAtNextEventIfStartElement(): " + issue.getMessage());
        // ToDo Gather issues rather than throw exception.
        throw new ThreddsXmlParserException(issue);
      }

      if (event.isStartElement())
      {
        startElement = event.asStartElement();
        break;
      }
      else if( event.isCharacters() && event.asCharacters().isWhiteSpace())
      {
        // Skip any whitespace characters.
        this.reader.next();
      }
      else
      {
        String msg = "Expecting StartElement for next event [" + event.getClass().getName() + "]";
        ThreddsXmlParserIssue issue = StaxThreddsXmlParserUtils
                .createIssueForUnexpectedEvent( msg, ThreddsXmlParserIssue.Severity.FATAL, this.reader,event );
        log.error( "peekAtNextEventIfStartElement(): " + issue.getMessage());
          // ToDo Gather issues rather than throw exception.
        throw new ThreddsXmlParserException( issue );
      }
    }

    return startElement;
  }
}

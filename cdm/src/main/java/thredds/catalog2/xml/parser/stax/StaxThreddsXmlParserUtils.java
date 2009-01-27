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

import thredds.catalog2.xml.parser.ThreddsXmlParserException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.StartElement;
import javax.xml.namespace.QName;
import java.io.Writer;
import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class StaxThreddsXmlParserUtils
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( StaxThreddsXmlParserUtils.class );
  
  public static String readElementAndAnyContent( XMLEventReader xmlEventReader )
          throws ThreddsXmlParserException
  {
    if ( xmlEventReader == null )
      throw new IllegalArgumentException( "XMLEventReader may not be null." );
    Writer writer = null;
    Location startLocation = null;
    try
    {
      XMLEvent event = xmlEventReader.peek();
      if ( !event.isStartElement() )
        throw new IllegalArgumentException( "Next event in reader must be start element." );
      writer = new StringWriter();
      startLocation = event.getLocation();

      // Track start and end elements so know when done.
      // Use name list as FILO, push on name of start element and pop off matching name of end element.
      List<QName> nameList = new ArrayList<QName>();
      while ( xmlEventReader.hasNext() )
      {
        event = xmlEventReader.nextEvent();
        if ( event.isStartElement() )
        {
          nameList.add( event.asStartElement().getName() );
        }
        else if ( event.isEndElement() )
        {
          QName endElemName = event.asEndElement().getName();
          QName lastName = nameList.get( nameList.size() - 1 );
          if ( lastName.equals( endElemName ) )
            nameList.remove( nameList.size() - 1 );
          else
          {
            // Parser should have had FATAL error for this.
            String msg = "Badly formed XML? End element [" + endElemName.getLocalPart() + "] doesn't match expected start element [" + lastName.getLocalPart() + "].";
            log.error( "readElementAndAnyContent(): " + msg );
            throw new ThreddsXmlParserException( "FATAL? " + msg );
          }
        }

        event.writeAsEncodedUnicode( writer );
        if ( nameList.isEmpty() )
          break;
      }
    }
    catch ( XMLStreamException e )
    {
      throw new ThreddsXmlParserException( "Problem reading unknown element [" + startLocation + "]. Underlying cause: " + e.getMessage(), e );
    }

    return writer.toString();
  }
  public static String readCharacterContent( StartElement startElement,
                                             XMLEventReader xmlEventReader )
          throws ThreddsXmlParserException
  {
    if ( startElement == null )
      throw new IllegalArgumentException( "Start element may not be null." );
    if ( xmlEventReader == null )
      throw new IllegalArgumentException( "XMLEventReader may not be null." );

    QName startElementName = startElement.getName();
    Writer writer = new StringWriter();
    Location location = null;
    try
    {
      while ( xmlEventReader.hasNext() )
      {
        XMLEvent event = xmlEventReader.peek();
        location = startElement.getLocation();

        if ( event.isCharacters())
        {
          event = xmlEventReader.nextEvent();
          event.writeAsEncodedUnicode( writer );
        }
        else if ( event.isEndElement())
        {
          if ( event.asEndElement().getName().equals( startElementName ))
          {
            return writer.toString();
          }
          throw new IllegalStateException( "Badly formed XML? Unexpected end element [" + event.asEndElement().getName().getLocalPart() + "]["+location+"] doesn't match expected start element [" + startElementName.getLocalPart() + "].");
        }
        else if ( event.isStartElement() )
        {
          throw new IllegalStateException( "Badly formed XML? Unexpected start element [" + event.asStartElement().getName().getLocalPart() + "][" + location + "] when characters expected." );
        }
        else
        {
          event = xmlEventReader.nextEvent();
          event.writeAsEncodedUnicode( writer );
        }
      }
    }
    catch ( XMLStreamException e )
    {
      throw new ThreddsXmlParserException( "Problem reading unknown event [" + location + "]. Underlying cause: " + e.getMessage(), e );
    }

    return writer.toString();
  }
}

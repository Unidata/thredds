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
import thredds.catalog2.xml.parser.ThreddsXmlParserIssue;
import thredds.util.HttpUriResolver;
import thredds.util.HttpUriResolverFactory;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.StartElement;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
class StaxThreddsXmlParserUtils
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( StaxThreddsXmlParserUtils.class );

  private StaxThreddsXmlParserUtils() {}

  static boolean isEventStartOrEndElementWithMatchingName( XMLEvent event, QName elementName )
  {
    if ( event == null )
      throw new IllegalArgumentException( "Event may not be null.");

    QName eventElementName = null;
    if ( event.isStartElement() )
      eventElementName = event.asStartElement().getName();
    else if ( event.isEndElement() )
      eventElementName = event.asEndElement().getName();
    else
      return false;

    if ( eventElementName.equals( elementName ) )
      return true;
    return false;
  }

  public static StartElement readNextEventCheckItIsStartElementWithExpectedName( XMLEventReader xmlEventReader,
                                                                          QName startElementName )
          throws ThreddsXmlParserException
  {
    if ( ! xmlEventReader.hasNext() )
      throw new IllegalStateException( "XMLEventReader has no further events." );

    StartElement startElement = null;
    try
    {
      XMLEvent event = xmlEventReader.peek();
      if ( ! event.isStartElement() )
        throw new IllegalStateException( "Next event must be StartElement." );

      if ( ! event.asStartElement().getName().equals( startElementName ) )
        throw new IllegalStateException( "Start element must be an '" + startElementName.getLocalPart() + "' element." );

      startElement = xmlEventReader.nextEvent().asStartElement();
    }
    catch ( XMLStreamException e )
    {
      String msg = "Problem reading XML stream.";
      log.warn( "readNextEventCheckItIsStartElementWithExpectedName(): " + msg, e );
      throw new ThreddsXmlParserException( new ThreddsXmlParserIssue( ThreddsXmlParserIssue.Severity.FATAL, msg, null, e ) );
    }

    return startElement;
  }

  public static void readNextEventCheckItIsEndElementWithExpectedName( XMLEventReader xmlEventReader,
                                                                      QName elementName )
          throws ThreddsXmlParserException
  {
    if ( ! xmlEventReader.hasNext() )
      throw new IllegalStateException( "XMLEventReader has no further events." );

    try
    {
      XMLEvent event = xmlEventReader.peek();
      if ( ! event.isEndElement() )
        throw new IllegalStateException( "Next event must be EndElement." );

      if ( ! event.asEndElement().getName().equals( elementName ) )
        throw new IllegalStateException( "End element must be an '" + elementName.getLocalPart() + "' element." );

      xmlEventReader.nextEvent(); // .asEndElement();
    }
    catch ( XMLStreamException e )
    {
      String msg = "Problem reading XML stream.";
      log.warn( "readNextEventCheckItIsEndElementWithExpectedName(): " + msg, e );
      throw new ThreddsXmlParserException( new ThreddsXmlParserIssue( ThreddsXmlParserIssue.Severity.FATAL, msg, null, e ) );
    }
  }

  static String getLocationInfo( XMLEventReader xmlEventReader )
  {
    Location location = getLocation( xmlEventReader);
    StringBuilder sb = new StringBuilder()
            .append( "Location: SysId[")
            .append( location.getSystemId())
            .append( "] line[")
            .append( location.getLineNumber())
            .append( "] column[" )
            .append( location.getColumnNumber())
            .append( "] charOffset[" )
            .append( location.getCharacterOffset())
            .append( "]." );
    return sb.toString();
  }

  static Location getLocation( XMLEventReader xmlEventReader)
  {
    if ( xmlEventReader == null )
      throw new IllegalArgumentException( "XMLEventReader may not be null.");
    if ( ! xmlEventReader.hasNext() )
      throw new IllegalArgumentException( "XMLEventReader must have next event.");

    XMLEvent nextEvent = null;
    try
    {
      nextEvent = xmlEventReader.peek();
    }
    catch ( XMLStreamException e )
    {
      throw new IllegalArgumentException( "Could not peek() next event.");
    }

    return nextEvent.getLocation();
  }

  static ThreddsXmlParserIssue createIssueForException( String message, XMLEventReader xmlEventReader, Exception e )
          throws ThreddsXmlParserException
  {
    String locationInfo = getLocationInfo( xmlEventReader);
    String msg = message + ":\n    " + locationInfo + ": " + e.getMessage();
    log.debug( "createIssueForException(): " + msg );
    return new ThreddsXmlParserIssue( ThreddsXmlParserIssue.Severity.WARNING, msg, null, e );
  }

  static ThreddsXmlParserIssue createIssueForUnexpectedElement( String message, XMLEventReader xmlEventReader )
          throws ThreddsXmlParserException
  {
    String locationInfo = getLocationInfo( xmlEventReader);
    String unexpectedElemAsString = StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( xmlEventReader );
    String msg = message + ":\n    " + locationInfo + ":\n" + unexpectedElemAsString;
    log.debug( "createIssueForUnexpectedElement(): " + msg );
    return new ThreddsXmlParserIssue( ThreddsXmlParserIssue.Severity.WARNING, msg, null, null );
  }

  static ThreddsXmlParserIssue createIssueForUnexpectedEvent( String message,
                                                                     ThreddsXmlParserIssue.Severity severity,
                                                                     XMLEventReader xmlEventReader, XMLEvent event )
          throws ThreddsXmlParserException
  {
    String locationInfo = getLocationInfo( xmlEventReader);
    String msg = message + " [" + severity.toString() + "]:\n    " + locationInfo + ":\n";
    log.debug( "createIssueForUnexpectedElement(): " + msg );
    return new ThreddsXmlParserIssue( severity, msg, null, null );
  }

  static String consumeElementAndConvertToXmlString( XMLEventReader xmlEventReader )
          throws ThreddsXmlParserException
  {
    if ( xmlEventReader == null )
      throw new IllegalArgumentException( "XMLEventReader may not be null." );

    // ToDo Capture as valid XML since writeAsEncodedUnicode() isn't impl-ed in Sun's JDK 6u14.
    StringWriter writerUsingWriteAsEncodedUnicode = new StringWriter();
    StringWriter writerUsingToString = new StringWriter();
    Location startLocation = null;
    try
    {
      XMLEvent event = xmlEventReader.peek();
      if ( ! event.isStartElement() )
        throw new IllegalArgumentException( "Next event in reader must be start element." );
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
            log.error( "consumeElementAndConvertToXmlString(): " + msg );
            throw new ThreddsXmlParserException( "FATAL? " + msg );
          }
        }

        event.writeAsEncodedUnicode( writerUsingWriteAsEncodedUnicode );
        writerUsingToString.write( event.toString());
        if ( nameList.isEmpty() )
          break;
      }
    }
    catch ( XMLStreamException e )
    {
      throw new ThreddsXmlParserException( "Problem reading unknown element [" + startLocation + "]. Underlying cause: " + e.getMessage(), e );
    }

    String result = writerUsingWriteAsEncodedUnicode.toString();
    if ( result == null || result.equals( "" ))
      result = writerUsingToString.toString();

    return result;
  }

  /**
   * Return the character contents of the containing element as a String trimmed of whitespace.
   * <p>
   * When called, the StartElement must have already been read from the reader. Until the
   * EndElement is reached, only Characters events are expected. Upon completion, the reader is
   * left with the EndElement as the next event.
   *
   * @param xmlEventReader the event reader from which to consume Characters events.
   * @param containingElementName the QName of the containing element.
   * @return a String representation of the character contents of the containing element.
   * @throws ThreddsXmlParserException  if had trouble reading from the XMLEventReader.
   */
  static String getCharacterContent( XMLEventReader xmlEventReader, QName containingElementName )
          throws ThreddsXmlParserException
  {
    if ( xmlEventReader == null )
      throw new IllegalArgumentException( "XMLEventReader may not be null." );
    if ( containingElementName == null )
      throw new IllegalArgumentException( "Containing element name may not be null." );

    if ( ! xmlEventReader.hasNext())
      throw new IllegalStateException( "XMLEventReader must have next.");

    StringBuilder stringBuilder = new StringBuilder();
    Location location = null;
    try
    {
      while ( xmlEventReader.hasNext() )
      {
        XMLEvent event = xmlEventReader.peek();
        location = event.getLocation();

        if ( event.isCharacters())
        {
          event = xmlEventReader.nextEvent();
          stringBuilder.append( event.asCharacters().getData());
        }
        else if ( event.isEndElement())
        {
          if ( event.asEndElement().getName().equals( containingElementName ))
          {
            return stringBuilder.toString().trim();
          }
          throw new IllegalStateException( "Badly formed XML? Unexpected end element [" + event.asEndElement().getName().getLocalPart() + "]["+location+"] doesn't match expected start element [" + containingElementName.getLocalPart() + "].");
        }
        else if ( event.isStartElement() )
        {
          throw new IllegalStateException( "Badly formed XML? Unexpected start element [" + event.asStartElement().getName().getLocalPart() + "][" + location + "] when characters expected." );
        }
        else
        {
          xmlEventReader.next();
        }
      }
    }
    catch ( XMLStreamException e )
    {
      throw new ThreddsXmlParserException( "Problem reading unknown event [" + location + "]. Underlying cause: " + e.getMessage(), e );
    }

    throw new ThreddsXmlParserException( "Unexpected end of XMLEventReader.");
  }

  /**
   * Return a StreamSource given a URI.
   *
   * @param documentUri the target URI.
   * @return a StreamSource for the resource located at the given URI.
   * @throws ThreddsXmlParserException if there is a problem reading from the URI.
   */
  static Source getSourceFromUri( URI documentUri )
          throws ThreddsXmlParserException
  {
    HttpUriResolver httpUriResolver = HttpUriResolverFactory.getDefaultHttpUriResolver( documentUri );
    InputStream is = null;
    try
    {
      httpUriResolver.makeRequest();
      is = httpUriResolver.getResponseBodyAsInputStream();
    }
    catch ( IOException e )
    {
      throw new ThreddsXmlParserException( "Problem accessing resource [" + documentUri.toString() + "].", e );
    }

    return new StreamSource( is, documentUri.toString() );
  }

  /**
   * Return a StreamSource given a File.
   *
   * @param file the target File.
   * @param docBaseUri the document base URI to use for this resource or null to use the File as docBase.
   * @return a StreamSource for the resource located at the given File.
   * @throws ThreddsXmlParserException if there is a problem reading from the File.
   */
  static Source getSourceFromFile( File file, URI docBaseUri )
          throws ThreddsXmlParserException
  {
    if ( file == null )
      throw new IllegalArgumentException( "File may not be null." );
    Source source = null;
    if ( docBaseUri == null )
      source = new StreamSource( file );
    else
    {
      InputStream is = null;
      try
      {
        is = new FileInputStream( file );
      }
      catch ( FileNotFoundException e )
      {
        String message = "Couldn't find file [" + file.getPath() + "].";
        log.error( "parseIntoBuilder(): " + message, e );
        throw new ThreddsXmlParserException( message, e );
      }
      source = new StreamSource( is, docBaseUri.toString() );
    }

    return source;
  }

  /**
   * Return an XMLEventReader given a Source and an XMLInputFactory.
   *
   * @param source the source.
   * @param factory the factory to be used.
   * @return an XMLEventReader for the given Source.
   * @throws ThreddsXmlParserException if have problems reading the source.
   */
  static XMLEventReader getEventReaderFromSource( Source source, XMLInputFactory factory )
          throws ThreddsXmlParserException
  {
    XMLEventReader reader;
    try
    {
      reader = factory.createXMLEventReader( source );
    }
    catch( XMLStreamException e )
    {
      String message = "Problems reading stream [" + source.getSystemId() + "].";
      log.error( "getEventReaderFromSource(): " + message, e );
      throw new ThreddsXmlParserException( message, e );
    }
    return reader;
  }
}

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

import thredds.catalog2.xml.parser.ThreddsXmlParser;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.Catalog;
import thredds.catalog2.simpleImpl.ThreddsBuilderFactoryImpl;
import thredds.catalog2.builder.*;

import java.net.URI;
import java.io.*;


import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class StaxThreddsXmlParser implements ThreddsXmlParser
{
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );

  private CatalogElementParser.Factory catElemParserFactory;
//  private boolean isValidating = false;
//  private Schema schema = null;

  // ToDo Use a factory object to allow configuring validation, error handling, and such.
  public static StaxThreddsXmlParser newInstance()
  {
    return new StaxThreddsXmlParser();
  }

  private StaxThreddsXmlParser() {
    this.catElemParserFactory = new CatalogElementParser.Factory();
  }

  private XMLInputFactory getFactory()
  {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    factory.setProperty( "javax.xml.stream.isCoalescing", Boolean.TRUE );
    factory.setProperty( "javax.xml.stream.supportDTD", Boolean.FALSE );
//    factory.setXMLReporter(  );
//    factory.setXMLResolver(  );
    return factory;
  }

  private CatalogBuilder readCatalogXML( Source source )
          throws ThreddsXmlParserException
  {
    try
    {
      XMLInputFactory factory = getFactory();
      XMLEventReader eventReader = factory.createXMLEventReader( source );

      ThreddsBuilderFactory catBuilderFac = new ThreddsBuilderFactoryImpl();
      ThreddsBuilder threddsBuilder = null;
      while ( eventReader.hasNext() )
      {
        XMLEvent event = eventReader.peek();
        if ( event.isEndDocument())
        { // Done!
          eventReader.next();
          break;
        }
        else if ( event.isStartDocument())
        { // Don't need any info from StartDocument event.
          eventReader.next();
        }
        else if ( event.isStartElement())
        {
          if ( this.catElemParserFactory.isEventMyStartElement( event.asStartElement() ))
          {
            CatalogElementParser catElemParser = this.catElemParserFactory.getNewParser( source.getSystemId(),
                                                                                         eventReader, catBuilderFac);
            threddsBuilder = catElemParser.parse();
          }
          // ToDo Implement reading a document with "dataset" root element.
          // ToDo Implement reading a document with "metadata" root element.
          else
          {
            // ToDo Save the results in a ThreddsXmlParserIssue (Warning) and report.
            StaxThreddsXmlParserUtils.consumeElementAndConvertToXmlString( eventReader );
            log.warn( "readCatalogXML(): Unrecognized start element [" + event.asStartElement().getName() + "]." );
            //eventReader.next();
          }
        }
        else if ( event.isEndElement())
        {
          log.error( "readCatalogXML(): Unrecognized end element [" + event.asEndElement().getName() + "]." );
          break;
        }
        else
        {
          log.debug( "readCatalogXML(): Unhandled event [" + event.getLocation() + "--" + event + "].");
          eventReader.next();
          continue;
        }
      }

      eventReader.close();

      if ( threddsBuilder == null )
        return null;

      return (CatalogBuilder) threddsBuilder;
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


  public Catalog parse( URI documentUri )
          throws ThreddsXmlParserException
  {
    Source s = StaxThreddsXmlParserUtils.getSourceFromUri( documentUri );
    try
    {
      return readCatalogXML( s ).build();
    }
    catch ( BuilderException e )
    {
      log.error( "parse(): Failed to parse catalog document.", e );
      throw new ThreddsXmlParserException( "Failed to parse catalog document.", e );
    }
  }

  public Catalog parse( File file, URI docBaseUri )
          throws ThreddsXmlParserException
  {
    try
    {
      return parseIntoBuilder( file, docBaseUri ).build();
    }
    catch ( BuilderException e )
    {
      log.error( "parse(): Failed to parse catalog document: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Failed to parse catalog document: " + e.getMessage(), e );
    }
  }

  public Catalog parse( Reader reader, URI docBaseUri )
          throws ThreddsXmlParserException
  {
    try
    {
      return parseIntoBuilder( reader, docBaseUri ).build();
    }
    catch ( BuilderException e )
    {
      log.error( "parse(): Failed to parse catalog document: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Failed to parse catalog document: " + e.getMessage(), e );
    }
  }

  public Catalog parse( InputStream is, URI docBaseUri )
          throws ThreddsXmlParserException
  {
    try
    {
      return parseIntoBuilder( is, docBaseUri ).build();
    }
    catch ( BuilderException e )
    {
      log.error( "parse(): Failed to parse catalog document: " + e.getMessage(), e );
      throw new ThreddsXmlParserException( "Failed to parse catalog document: " + e.getMessage(), e );
    }
  }

  public CatalogBuilder parseIntoBuilder( URI documentUri )
          throws ThreddsXmlParserException
  {
    Source s = StaxThreddsXmlParserUtils.getSourceFromUri( documentUri );
    return readCatalogXML( s );
  }

  public CatalogBuilder parseIntoBuilder( File file, URI docBaseUri )
          throws ThreddsXmlParserException
  {
    Source s = StaxThreddsXmlParserUtils.getSourceFromFile( file, docBaseUri );
    return readCatalogXML( s );
  }

  public CatalogBuilder parseIntoBuilder( Reader reader, URI docBaseUri )
          throws ThreddsXmlParserException
  {
    Source source = new StreamSource( reader, docBaseUri.toString() );
    return readCatalogXML( source );
  }

  public CatalogBuilder parseIntoBuilder( InputStream is, URI docBaseUri )
          throws ThreddsXmlParserException
  {
    Source source = new StreamSource( is, docBaseUri.toString() );
    return readCatalogXML( source );
  }
}
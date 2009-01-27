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
package thredds.catalog2.xml.parser.sax;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.ErrorHandler;

import java.util.Map;
import java.util.HashMap;

import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.BuilderException;
import thredds.catalog2.Catalog;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ThreddsCatalogHandler extends DefaultHandler
{
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( ThreddsCatalogHandler.class );

  private CatalogBuilder builder;
  private String docSystemId;

  private DefaultHandler state;
  private ErrorHandler errorHandler;
  private Map<String, String> namespaceMap;


  public ThreddsCatalogHandler( String docSystemId )
  {
    if ( docSystemId == null )
      throw new IllegalArgumentException( "Document system ID must not be null.");
    this.docSystemId = docSystemId;
    this.state = null;
    this.namespaceMap = new HashMap<String, String>();
  }

  Catalog getCatalog() throws BuilderException
  {
    if ( builder != null )
    {
      return builder.build();
    }
    return null;
  }

  void setState( DefaultHandler state )
  {
    this.state = state;
  }
  void setErrorHandler( ErrorHandler eh )
  {
    this.errorHandler = eh;
  }

  @Override
  public void startDocument()
          throws SAXException
  {
    super.startDocument();
  }

  @Override
  public void endDocument()
          throws SAXException
  {
    super.endDocument();
  }

  @Override
  public void startPrefixMapping( String prefix, String uri )
          throws SAXException
  {
    this.namespaceMap.put( prefix, uri );
  }

  @Override
  public void endPrefixMapping( String prefix )
          throws SAXException
  {
    this.namespaceMap.remove( prefix );
  }

  @Override
  public void startElement( String uri, String localName, String qName, Attributes attributes )
          throws SAXException
  {
    if ( state != null )
      this.state.startElement( uri, localName, qName, attributes );

    if ( localName.equals( "catalog" ))
      this.state = new CatalogHandler( this.docSystemId, attributes, this, this );
    if ( localName.equals( "service" ))
      this.state = new ServiceHandler( null, null, attributes, this, this );
  }

  @Override
  public void endElement( String uri, String localName, String qName )
          throws SAXException
  {
    if ( state != null )
      this.state.endElement( uri, localName, qName );

    throw new SAXException( "Closing XML element [" + localName + "] after parsing completed.");
    // OR call warning/error/fatal?
  }

  @Override
  public void warning( SAXParseException e )
          throws SAXException
  {
    this.errorHandler.warning( e );
  }

  @Override
  public void error( SAXParseException e )
          throws SAXException
  {
    this.errorHandler.error( e );
  }

  @Override
  public void fatalError( SAXParseException e )
          throws SAXException
  {
    this.errorHandler.fatalError( e );
  }
}

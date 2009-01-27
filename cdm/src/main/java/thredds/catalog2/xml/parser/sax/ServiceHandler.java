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

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.io.IOException;
import java.net.URI;

import thredds.catalog2.builder.ServiceBuilder;
import thredds.catalog2.builder.CatalogBuilder;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class ServiceHandler extends DefaultHandler
{
  private ServiceBuilder service;

  private DefaultHandler top;
  private DefaultHandler parent;

  private Map<String,String> namespaceMap;

  public ServiceHandler( CatalogBuilder parentBuilder, ServiceBuilder altParentBuilder,
                                Attributes atts, DefaultHandler top, DefaultHandler parent )
  {
    this.namespaceMap = new HashMap<String,String>();

    String name = atts.getValue( "", "name");
    String docBaseUriString = null; // ??? atts.getValue
    URI docBaseUri = null;
    String version = atts.getValue( "", "version");
    String expiresDateString = atts.getValue( "", "expires");
    Date expires = null;
    String lastModifiedDateString = null;
    Date lastModified = null;


//    this.service = new CatalogBuilderFactoryImpl()
//            .newCatalogBuilder( name, docBaseUri, version, expires, lastModified );

    this.top = top;
    this.parent = parent;
  }

  @Override
  public void setDocumentLocator( Locator locator )
  {
    super.setDocumentLocator( locator );
  }

  @Override
  public void startDocument() throws SAXException
  {
    System.out.println( "Start of document" );
  }

  @Override
  public void endDocument() throws SAXException
  {
    System.out.println( "End of document" );
  }

  @Override
  public void startElement( String uri, String localName, String qName, Attributes atts ) throws SAXException
  {
    if ( localName.equals( "service" ))
    {
//      ServiceHandler sdh = new ServiceHandler( Attributes atts, DefaultHandler
//      top, DefaultHandler
//      parent);
//      String name = atts.getValue( CatalogNamespace.CATALOG_1_0.getNamespaceUri(), "name" )
//      this.service.addService(  )
    }
    else if ( localName.equals( "dataset"))
    {

    }
    else
    {
      // ???
    }
    //super.startElement( uri, localName, qName, atts );
    StringBuilder sb = new StringBuilder( "Start Element: " ).append( localName);
    if ( localName.equals( "dataset") )
    {
      sb.append( atts.getValue( "name" ));
    }
    System.out.println( sb.toString() );

  }

  @Override
  public void endElement( String uri, String localName, String qName ) throws SAXException
  {
    //super.endElement( uri, localName, qName );
    System.out.println( "End Element: " + localName );
  }

  @Override
  public void characters( char ch[], int start, int length ) throws SAXException
  {
    super.characters( ch, start, length);
  }

  @Override
  public void ignorableWhitespace( char ch[], int start, int length ) throws SAXException
  {
    super.ignorableWhitespace( ch, start, length);
  }

  @Override
  public void processingInstruction( String target, String data ) throws SAXException
  {
    super.processingInstruction( target, data);
  }

  @Override
  public void skippedEntity( String name ) throws SAXException
  {
    super.skippedEntity( name);
  }

  @Override
  public InputSource resolveEntity( String publicId, String systemId ) throws IOException, SAXException
  {
    return super.resolveEntity( publicId, systemId );
  }
}
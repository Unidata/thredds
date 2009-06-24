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

import thredds.catalog2.xml.parser.ThreddsXmlParser;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.xml.util.CatalogNamespace;
import thredds.catalog2.Catalog;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.BuilderException;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import java.net.URI;
import java.io.*;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class SAXCatalogParser implements ThreddsXmlParser
{
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( SAXCatalogParser.class );

  private boolean isValidating = false;
  private Schema schema = null;

  private SAXCatalogParser()
  {
  }

  public static SAXCatalogParser getInstance()
  {
    return new SAXCatalogParser();
  }

  public boolean wantValidating( boolean wantValidating )
  {
    if ( wantValidating && schema == null )
    {
      try
      {
        this.schema = CatalogNamespace.CATALOG_1_0.getSchema();
      }
      catch ( IOException e )
      {
        log.warn( "wantValidating(): Failed to read schema.", e );
        this.schema = null;
      }
      catch ( SAXException e )
      {
        log.warn( "wantValidating(): Failed to parse schema.", e );
        this.schema = null;
      }
      if ( schema != null )
        this.isValidating = true;
      else
        this.isValidating = false;
    }
    else if ( wantValidating && schema != null )
    {
      this.isValidating = true;
    }
    else if ( ! wantValidating )
      this.isValidating = false;

    return this.isValidating;
  }

  public boolean isValidating()
  {
    return this.isValidating;
  }

  private Catalog readXML( InputSource source )
          throws ThreddsXmlParserException
  {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setNamespaceAware( true );
    if ( this.isValidating )
      factory.setSchema( schema );
    SAXParser parser = null;
    try
    {
      parser = factory.newSAXParser();
    }
    catch ( ParserConfigurationException e )
    {
      e.printStackTrace();
    }
    catch ( SAXException e )
    {
      e.printStackTrace();
    }

    ThreddsCatalogHandler catHandler = new ThreddsCatalogHandler( source.getSystemId() );
    try
    {
      parser.parse( source, catHandler );
    }
    catch ( SAXException e )
    {
      e.printStackTrace();
    }
    catch ( IOException e )
    {
      e.printStackTrace();
    }
    try
    {
      return catHandler.getCatalog();
    }
    catch ( BuilderException e )
    {
      throw new ThreddsXmlParserException( "Catalog builder in bad state.", e);
    }
  }

  public Catalog parse( URI uri )
          throws ThreddsXmlParserException
  {
    InputSource is = new InputSource( uri.toString() );
    return readXML( is );
  }

  public Catalog parse( File file, URI baseUri )
          throws ThreddsXmlParserException
  {
    InputSource is = null;
    try
    {
      is = new InputSource( new FileReader( file ));
    }
    catch ( FileNotFoundException e )
    {
      e.printStackTrace();
    }
    is.setSystemId( baseUri.toString() );
    return readXML( is);
  }

  public Catalog parse( Reader reader, URI baseUri )
          throws ThreddsXmlParserException
  {
    InputSource is = new InputSource( reader );
    is.setSystemId( baseUri.toString() );
    return readXML( is);
  }

  public Catalog parse( InputStream is, URI baseUri )
          throws ThreddsXmlParserException
  {
    InputSource inSource = new InputSource( is );
    inSource.setSystemId( baseUri.toString() );
    return readXML( inSource );
  }

  public CatalogBuilder parseIntoBuilder( URI uri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public CatalogBuilder parseIntoBuilder( File file, URI baseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public CatalogBuilder parseIntoBuilder( Reader reader, URI baseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

  public CatalogBuilder parseIntoBuilder( InputStream is, URI baseUri ) throws ThreddsXmlParserException
  {
    return null;
  }

}

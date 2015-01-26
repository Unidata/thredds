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
package thredds.catalog2.xml.writer.stax;

import thredds.catalog2.xml.writer.ThreddsXmlWriter;
import thredds.catalog2.xml.writer.ThreddsXmlWriterException;
import thredds.catalog2.Catalog;
import thredds.catalog2.Dataset;
import thredds.catalog2.Metadata;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.*;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class StaxWriter
        implements ThreddsXmlWriter
{
  private org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger( getClass() );

  private static final String defaultCharEncoding = "UTF-8";
  private static final String indentString = "  ";
  public static String getIndentString ( int nestLevel )
  {
    StringBuilder sb = new StringBuilder();
    for ( int i = 0; i < nestLevel; i++)
      sb.append( indentString );
    return sb.toString();
  }

  private final XMLOutputFactory factory;
  public StaxWriter()
  {
    this.factory = XMLOutputFactory.newInstance();
    this.factory.setProperty( XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.FALSE );
    if ( this.factory.isPropertySupported( "javax.xml.stream.isPrefixDefaulting" ))
      this.factory.setProperty( "javax.xml.stream.isPrefixDefaulting", Boolean.TRUE );
  }

  public void writeCatalog( Catalog catalog, File file )
          throws ThreddsXmlWriterException, IOException
  {
    if ( file == null )
      throw new IllegalArgumentException( "File must not be null." );
    OutputStream os = new FileOutputStream( file);
    XMLStreamWriter xmlStreamWriter = this.getXmlStreamWriter( os );
    CatalogElementWriter catalogWriter = new CatalogElementWriter();
    catalogWriter.writeElement( catalog, xmlStreamWriter, 0 );
    os.close();
  }

  public void writeCatalog( Catalog catalog, Writer writer )
          throws ThreddsXmlWriterException
  {
    XMLStreamWriter xmlStreamWriter = getXmlStreamWriter( writer );
    CatalogElementWriter catalogWriter = new CatalogElementWriter();
    catalogWriter.writeElement( catalog, xmlStreamWriter, 0 );
  }

  public void writeCatalog( Catalog catalog, OutputStream os )
          throws ThreddsXmlWriterException
  {
    XMLStreamWriter xmlStreamWriter = getXmlStreamWriter( os );
    CatalogElementWriter catalogWriter = new CatalogElementWriter();
    catalogWriter.writeElement( catalog, xmlStreamWriter, 0 );
  }

  public void writeDataset( Dataset dataset, File file )
          throws ThreddsXmlWriterException, IOException
  {
    if ( file == null )
      throw new IllegalArgumentException( "File must not be null." );
    OutputStream os = new FileOutputStream( file );
    XMLStreamWriter xmlStreamWriter = this.getXmlStreamWriter( os );
    // Do good stuff
    os.close();
  }

  public void writeDataset( Dataset dataset, Writer writer )
          throws ThreddsXmlWriterException
  {
    XMLStreamWriter xmlStreamWriter = this.getXmlStreamWriter( writer );
  }

  public void writeDataset( Dataset dataset, OutputStream os )
          throws ThreddsXmlWriterException
  {
    XMLStreamWriter xmlStreamWriter = this.getXmlStreamWriter( os );
  }

  public void writeMetadata( Metadata metadata, File file )
          throws ThreddsXmlWriterException, IOException
  {
    if ( file == null )
      throw new IllegalArgumentException( "File must not be null." );
    OutputStream os = new FileOutputStream( file );
    XMLStreamWriter xmlStreamWriter = this.getXmlStreamWriter( os );
    // Do good stuff
    os.close();
  }

  public void writeMetadata( Metadata metadata, Writer writer )
          throws ThreddsXmlWriterException
  {
    XMLStreamWriter xmlStreamWriter = this.getXmlStreamWriter( writer );
  }

  public void writeMetadata( Metadata metadata, OutputStream os )
          throws ThreddsXmlWriterException
  {
    XMLStreamWriter xmlStreamWriter = this.getXmlStreamWriter( os );
  }

  private XMLStreamWriter getXmlStreamWriter( Writer writer )
          throws ThreddsXmlWriterException
  {
    if ( writer == null )
      throw new IllegalArgumentException( "Writer may not be null.");
    try
    {
      return this.factory.createXMLStreamWriter( writer );
    }
    catch ( XMLStreamException e )
    {
      logger.error( "getXmlStreamWriter(): Failed to create XMLStreamWriter: " + e.getMessage() );
      throw new ThreddsXmlWriterException( "Failed to create XMLStreamWriter: " + e.getMessage(), e );
    }
  }

  private XMLStreamWriter getXmlStreamWriter( OutputStream os )
          throws ThreddsXmlWriterException
  {
    if ( os == null )
      throw new IllegalArgumentException( "OutputStream must not be null." );
    try
    {
      return this.factory.createXMLStreamWriter( os, defaultCharEncoding );
    }
    catch ( XMLStreamException e )
    {
      logger.error( "getXmlStreamWriter(): Failed to create XMLStreamWriter: " + e.getMessage() );
      throw new ThreddsXmlWriterException( "Failed to create XMLStreamWriter: " + e.getMessage(), e );
    }
  }
}

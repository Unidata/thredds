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
  private org.slf4j.Logger logger =
          org.slf4j.LoggerFactory.getLogger( StaxWriter.class );

  private static final String defaultCharEncoding = "UTF-8";

  private final XMLOutputFactory factory;
  public StaxWriter()
  {
    this.factory = XMLOutputFactory.newInstance();
    this.factory.setProperty( XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.FALSE );
    if ( this.factory.isPropertySupported( "javax.xml.stream.isPrefixDefaulting" ))
      this.factory.setProperty( "javax.xml.stream.isPrefixDefaulting", Boolean.TRUE );
  }

  public void writeCatalog( Catalog catalog, File file )
          throws FileNotFoundException, ThreddsXmlWriterException
  {
    XMLStreamWriter xmlStreamWriter = this.getXmlStreamWriter( file );
    writeStartDocument( xmlStreamWriter );
    CatalogElementWriter catalogWriter = new CatalogElementWriter();
    catalogWriter.writeElement( catalog, xmlStreamWriter );
  }

  public void writeCatalog( Catalog catalog, Writer writer )
          throws ThreddsXmlWriterException
  {
    XMLStreamWriter xmlStreamWriter = getXmlStreamWriter( writer );
    writeStartDocument( xmlStreamWriter );
    CatalogElementWriter catalogWriter = new CatalogElementWriter();
    catalogWriter.writeElement( catalog, xmlStreamWriter );
  }

  public void writeCatalog( Catalog catalog, OutputStream os )
          throws ThreddsXmlWriterException
  {
    XMLStreamWriter xmlStreamWriter = getXmlStreamWriter( os );
    writeStartDocument( xmlStreamWriter );
    CatalogElementWriter catalogWriter = new CatalogElementWriter();
    catalogWriter.writeElement( catalog, xmlStreamWriter );
  }

  public void writeDataset( Dataset dataset, File file )
          throws FileNotFoundException, ThreddsXmlWriterException
  {
    XMLStreamWriter xmlStreamWriter = this.getXmlStreamWriter( file );
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
          throws FileNotFoundException, ThreddsXmlWriterException
  {
    XMLStreamWriter xmlStreamWriter = this.getXmlStreamWriter( file );
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

  private void writeStartDocument( XMLStreamWriter xmlStreamWriter )
          throws ThreddsXmlWriterException
  {
    try
    {
      xmlStreamWriter.writeStartDocument();
    }
    catch ( XMLStreamException e )
    {
      logger.error( "writeStartDocument(): Failed to write document start to XMLStreamWriter." );
      throw new ThreddsXmlWriterException( "Failed writing document start to XMLStreamWriter.", e );
    }
  }

  private XMLStreamWriter getXmlStreamWriter( File file )
          throws FileNotFoundException, ThreddsXmlWriterException
  {
    if ( file == null )
      throw new IllegalArgumentException( "File must not be null." );
    try
    {
      return this.factory.createXMLStreamWriter( new FileOutputStream( file ), defaultCharEncoding );
    }
    catch ( XMLStreamException e )
    {
      logger.error( "getXmlStreamWriter(): Failed to create XMLStreamWriter." );
      throw new ThreddsXmlWriterException( "Failed to create XMLStreamWriter.", e );
    }
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
      logger.error( "getXmlStreamWriter(): Failed to create XMLStreamWriter." );
      throw new ThreddsXmlWriterException( "Failed to create XMLStreamWriter.", e );
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
      logger.error( "getXmlStreamWriter(): Failed to create XMLStreamWriter." );
      throw new ThreddsXmlWriterException( "Failed to create XMLStreamWriter.", e );
    }
  }
}

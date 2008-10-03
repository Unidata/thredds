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
    if ( file == null ) throw new IllegalArgumentException( "File must not be null.");
    OutputStream os = new FileOutputStream( file);
    try
    {
      XMLStreamWriter xmlStreamWriter =
              this.factory.createXMLStreamWriter( os, "UTF-8" );
    }
    catch ( XMLStreamException e )
    {
      throw new ThreddsXmlWriterException( "", e);
    }
  }

  public void writeCatalog( Catalog catalog, Writer writer )
          throws ThreddsXmlWriterException
  {
    try
    {
      XMLStreamWriter xmlStreamWriter =
              this.factory.createXMLStreamWriter( writer );
    }
    catch ( XMLStreamException e )
    {
      throw new ThreddsXmlWriterException( "", e );
    }
  }

  public void writeCatalog( Catalog catalog, OutputStream os )
          throws ThreddsXmlWriterException
  {
    try
    {
      XMLStreamWriter xmlStreamWriter = this.factory.createXMLStreamWriter( os, "UTF-8");
      this.writeCatalog( catalog, xmlStreamWriter);
    }
    catch ( XMLStreamException e )
    {
      throw new ThreddsXmlWriterException( "", e );
    }
  }

  private void writeCatalog( Catalog catalog, XMLStreamWriter writer)
          throws XMLStreamException
  {
    writer.writeStartDocument();
  }

  public void writeDataset( Dataset dataset, File file )
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void writeDataset( Dataset dataset, Writer writer )
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void writeDataset( Dataset dataset, OutputStream os )
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void writeMetadata( Metadata metadata, File file )
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void writeMetadata( Metadata metadata, Writer writer )
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void writeMetadata( Metadata metadata, OutputStream os )
  {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}

package thredds.catalog2.xml.writer;

import thredds.catalog2.Catalog;
import thredds.catalog2.Dataset;
import thredds.catalog2.Metadata;

import java.io.File;
import java.io.Writer;
import java.io.OutputStream;
import java.io.FileNotFoundException;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface ThreddsXmlWriter
{
  public void writeCatalog( Catalog catalog, File file )
          throws ThreddsXmlWriterException, FileNotFoundException;
  public void writeCatalog( Catalog catalog, Writer writer )
          throws ThreddsXmlWriterException;
  public void writeCatalog( Catalog catalog, OutputStream os )
          throws ThreddsXmlWriterException;

  public void writeDataset( Dataset dataset, File file )
          throws ThreddsXmlWriterException, FileNotFoundException;
  public void writeDataset( Dataset dataset, Writer writer )
          throws ThreddsXmlWriterException;
  public void writeDataset( Dataset dataset, OutputStream os )
          throws ThreddsXmlWriterException;

  public void writeMetadata( Metadata metadata, File file )
          throws ThreddsXmlWriterException, FileNotFoundException;
  public void writeMetadata( Metadata metadata, Writer writer )
          throws ThreddsXmlWriterException;
  public void writeMetadata( Metadata metadata, OutputStream os )
          throws ThreddsXmlWriterException;
}

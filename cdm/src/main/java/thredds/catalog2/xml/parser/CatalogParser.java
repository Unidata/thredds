package thredds.catalog2.xml.parser;

import thredds.catalog2.Catalog;
import thredds.catalog2.Dataset;
import thredds.catalog2.Metadata;
import thredds.catalog2.builder.CatalogBuilder;
import thredds.catalog2.builder.DatasetBuilder;
import thredds.catalog2.builder.MetadataBuilder;

import java.net.URI;
import java.io.File;
import java.io.Reader;
import java.io.InputStream;

/**
 * Provide methods for parsing THREDDS catalog XML documents and
 * generating Catalog, CatalogBuilder, Dataset, DatasetBuilder,
 * Metadata, or MetadataBuilder object.
 *
 * @author edavis
 * @since 4.0
 */
public interface CatalogParser
{
  public Catalog parse( URI documentUri) throws ThreddsXmlParserException;
  public Catalog parse( File file, URI docBaseUri) throws ThreddsXmlParserException;
  public Catalog parse( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException;
  public Catalog parse( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException;

  public CatalogBuilder parseIntoBuilder( URI documentUri) throws ThreddsXmlParserException;
  public CatalogBuilder parseIntoBuilder( File file, URI docBaseUri) throws ThreddsXmlParserException;
  public CatalogBuilder parseIntoBuilder( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException;
  public CatalogBuilder parseIntoBuilder( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException;

  public Dataset parseDataset( URI documentUri ) throws ThreddsXmlParserException;
  public Dataset parseDataset( File file, URI docBaseUri ) throws ThreddsXmlParserException;
  public Dataset parseDataset( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException;
  public Dataset parseDataset( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException;

  public DatasetBuilder parseDatasetIntoBuilder( URI documentUri ) throws ThreddsXmlParserException;
  public DatasetBuilder parseDatasetIntoBuilder( File file, URI docBaseUri ) throws ThreddsXmlParserException;
  public DatasetBuilder parseDatasetIntoBuilder( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException;
  public DatasetBuilder parseDatasetIntoBuilder( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException;

  public Metadata parseMetadata( URI documentUri ) throws ThreddsXmlParserException;
  public Metadata parseMetadata( File file, URI docBaseUri ) throws ThreddsXmlParserException;
  public Metadata parseMetadata( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException;
  public Metadata parseMetadata( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException;

  public MetadataBuilder parseMetadataIntoBuilder( URI documentUri ) throws ThreddsXmlParserException;
  public MetadataBuilder parseMetadataIntoBuilder( File file, URI docBaseUri ) throws ThreddsXmlParserException;
  public MetadataBuilder parseMetadataIntoBuilder( Reader reader, URI docBaseUri ) throws ThreddsXmlParserException;
  public MetadataBuilder parseMetadataIntoBuilder( InputStream is, URI docBaseUri ) throws ThreddsXmlParserException;
}
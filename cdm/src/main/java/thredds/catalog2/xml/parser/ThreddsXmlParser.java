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
 * generating a Catalog or CatalogBuilder object.
 *
 * @author edavis
 * @since 4.0
 */
public interface ThreddsXmlParser
{
  public Catalog parseCatalog( URI uri) throws ThreddsXmlParserException;
  public Catalog parseCatalog( File file, URI baseUri) throws ThreddsXmlParserException;
  public Catalog parseCatalog( Reader reader, URI baseUri ) throws ThreddsXmlParserException;
  public Catalog parseCatalog( InputStream is, URI baseUri ) throws ThreddsXmlParserException;

  public CatalogBuilder parseCatalogIntoBuilder( URI uri) throws ThreddsXmlParserException;
  public CatalogBuilder parseCatalogIntoBuilder( File file, URI baseUri) throws ThreddsXmlParserException;
  public CatalogBuilder parseCatalogIntoBuilder( Reader reader, URI baseUri ) throws ThreddsXmlParserException;
  public CatalogBuilder parseCatalogIntoBuilder( InputStream is, URI baseUri ) throws ThreddsXmlParserException;

  public Dataset parseDataset( URI uri) throws ThreddsXmlParserException;
  public Dataset parseDataset( File file, URI baseUri) throws ThreddsXmlParserException;
  public Dataset parseDataset( Reader reader, URI baseUri ) throws ThreddsXmlParserException;
  public Dataset parseDataset( InputStream is, URI baseUri ) throws ThreddsXmlParserException;

  public DatasetBuilder parseDatasetIntoBuilder( URI uri) throws ThreddsXmlParserException;
  public DatasetBuilder parseDatasetIntoBuilder( File file, URI baseUri) throws ThreddsXmlParserException;
  public DatasetBuilder parseDatasetIntoBuilder( Reader reader, URI baseUri ) throws ThreddsXmlParserException;
  public DatasetBuilder parseDatasetIntoBuilder( InputStream is, URI baseUri ) throws ThreddsXmlParserException;

  public Metadata parseMetadata( URI uri) throws ThreddsXmlParserException;
  public Metadata parseMetadata( File file, URI baseUri) throws ThreddsXmlParserException;
  public Metadata parseMetadata( Reader reader, URI baseUri ) throws ThreddsXmlParserException;
  public Metadata parseMetadata( InputStream is, URI baseUri ) throws ThreddsXmlParserException;

  public MetadataBuilder parseMetadataIntoBuilder( URI uri) throws ThreddsXmlParserException;
  public MetadataBuilder parseMetadataIntoBuilder( File file, URI baseUri) throws ThreddsXmlParserException;
  public MetadataBuilder parseMetadataIntoBuilder( Reader reader, URI baseUri ) throws ThreddsXmlParserException;
  public MetadataBuilder parseMetadataIntoBuilder( InputStream is, URI baseUri ) throws ThreddsXmlParserException;
}
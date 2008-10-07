package thredds.catalog2.xml.parser;

import thredds.catalog2.Catalog;
import thredds.catalog2.builder.CatalogBuilder;

import java.net.URI;
import java.io.File;
import java.io.Reader;
import java.io.InputStream;

/**
 * Provide methods for parsing a THREDDS catalog XML document and
 * generating a Catalog or CatalogBuilder object.
 *
 * @author edavis
 * @since 4.0
 */
public interface CatalogParser
{
  public Catalog parse( URI uri) throws ThreddsXmlParserException;
  public Catalog parse( File file, URI baseUri) throws ThreddsXmlParserException;
  public Catalog parse( Reader reader, URI baseUri ) throws ThreddsXmlParserException;
  public Catalog parse( InputStream is, URI baseUri ) throws ThreddsXmlParserException;

  public CatalogBuilder parseIntoBuilder( URI uri) throws ThreddsXmlParserException;
  public CatalogBuilder parseIntoBuilder( File file, URI baseUri) throws ThreddsXmlParserException;
  public CatalogBuilder parseIntoBuilder( Reader reader, URI baseUri ) throws ThreddsXmlParserException;
  public CatalogBuilder parseIntoBuilder( InputStream is, URI baseUri ) throws ThreddsXmlParserException;
}

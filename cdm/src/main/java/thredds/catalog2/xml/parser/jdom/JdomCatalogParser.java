package thredds.catalog2.xml.parser.jdom;

import thredds.catalog2.xml.parser.CatalogParser;
import thredds.catalog2.xml.parser.ThreddsXmlParserException;
import thredds.catalog2.Catalog;
import thredds.catalog2.builder.CatalogBuilder;

import java.net.URI;
import java.io.File;
import java.io.Reader;
import java.io.InputStream;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class JdomCatalogParser implements CatalogParser
{
  public Catalog parse( URI uri )
  {
    return null;
  }

  public Catalog parse( File file, URI baseUri )
  {
    return null;
  }

  public Catalog parse( Reader reader, URI baseUri )
  {
    return null;
  }

  public Catalog parse( InputStream is, URI baseUri )
  {
    return null;
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

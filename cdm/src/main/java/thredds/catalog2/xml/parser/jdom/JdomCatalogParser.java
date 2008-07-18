package thredds.catalog2.xml.parser.jdom;

import thredds.catalog2.xml.parser.CatalogParser;
import thredds.catalog2.Catalog;

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
  @Override
  public Catalog readXML( URI uri )
  {
    return null;
  }

  @Override
  public Catalog readXML( File file, URI baseUri )
  {
    return null;
  }

  @Override
  public Catalog readXML( Reader reader, URI baseUri )
  {
    return null;
  }

  @Override
  public Catalog readXML( InputStream is, URI baseUri )
  {
    return null;
  }
}

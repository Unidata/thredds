package thredds.catalog2.xml.parser;

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
public interface CatalogBuilder
{
  public Catalog readXML( URI uri);
  public Catalog readXML( File file, URI baseUri);
  public Catalog readXML( Reader reader, URI baseUri );
  public Catalog readXML( InputStream is, URI baseUri );
}

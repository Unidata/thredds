package thredds.catalog2.builder;

import thredds.catalog2.Catalog;

import java.util.Date;
import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class CatalogBuilderFactory
{
  public CatalogBuilder newCatalogBuilder( String name, URI docBaseUri, String version, Date expires, Date lastModified )
  { return null; }

  public CatalogBuilder newCatalogBuilder( Catalog catalog )
  {
    return null;
  }
}

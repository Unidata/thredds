package thredds.catalog2;

import java.net.URI;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface CatalogRef extends MetadataContainer
{
  public String getTitle();
  public URI getUri();
}

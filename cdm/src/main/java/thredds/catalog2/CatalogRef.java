package thredds.catalog2;

import java.net.URI;
import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface CatalogRef extends DatasetNode
{
  public URI getReference();
}

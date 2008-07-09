package thredds.catalog2;

import java.net.URI;

/**
 * Interface for various strategies that, given an Access instance,
 * build access URIs.
 *
 * @author edavis
 * @since 4.0
 */
public interface AccessUriBuilder
{
  public URI buildAccessUri( Access access, URI docBaseUri);
}

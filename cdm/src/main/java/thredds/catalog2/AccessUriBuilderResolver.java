package thredds.catalog2;

import java.util.List;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface AccessUriBuilderResolver
{
  public AccessUriBuilder resolveAccessUriBuilder( Dataset dataset, Access access );
}

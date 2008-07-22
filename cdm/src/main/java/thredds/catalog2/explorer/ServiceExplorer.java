package thredds.catalog2.explorer;

import thredds.catalog2.Service;
import thredds.catalog2.Property;

/**
 * Extends Service with additional search methods.
 *
 * @author edavis
 * @since 4.0
 */
public interface ServiceExplorer extends Service
{
  public Property getProperty( String name );
  public Service getService( String name );
}

package thredds.catalog2.explorer;

import thredds.catalog2.Access;
import thredds.catalog2.Dataset;
import thredds.catalog.ServiceType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface DatasetExplorer extends Dataset, DatasetNodeExplorer
{
  public Access getAccess( ServiceType type );
}

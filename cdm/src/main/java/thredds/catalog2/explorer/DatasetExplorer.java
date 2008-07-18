package thredds.catalog2.explorer;

import thredds.catalog2.Access;
import thredds.catalog2.Dataset;
import thredds.catalog2.Property;
import thredds.catalog.ServiceType;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public interface DatasetExplorer extends Dataset
{
  public Access getAccess( ServiceType type );

  public Dataset getDatasetByName( String name );

  public Dataset getDatasetById( String id );

  public Property getPropertyByName( String name );

}

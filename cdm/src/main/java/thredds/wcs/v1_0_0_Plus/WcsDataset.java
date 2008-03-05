package thredds.wcs.v1_0_0_Plus;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;

import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class WcsDataset
{
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( WcsDataset.class );

  // Dataset specific
  private String datasetPath;
  private String datasetName;
  private GridDataset dataset;
  private HashMap<String, WcsCoverage> availableCoverages;

  public WcsDataset( GridDataset dataset, String datasetPath )
  {
    this.datasetPath = datasetPath;
    int pos = datasetPath.lastIndexOf( "/" );
    this.datasetName = ( pos > 0 ) ? datasetPath.substring( pos + 1 ) : datasetPath;
    this.dataset = dataset;

    this.availableCoverages = new HashMap<String, WcsCoverage>();

    for ( GridDataset.Gridset curGridset : this.dataset.getGridsets() )
    {
      GridCoordSystem gcs = curGridset.getGeoCoordSystem();
      if ( ! gcs.isRegularSpatial() )
        continue;
      this.availableCoverages.put( gcs.getName(), new WcsCoverage( curGridset, this) );
    }
  }

  public String getDatasetPath() { return datasetPath; }
  public String getDatasetName() { return datasetName; }
  public GridDataset getDataset() { return dataset; }

  public boolean isAvailableCoverageName( String name )
  {
    return availableCoverages.containsKey( name );
  }

  public WcsCoverage getAvailableCoverage( String name )
  {
    return availableCoverages.get( name );
  }

  public Collection<WcsCoverage> getAvailableCoverageCollection()
  {
    return Collections.unmodifiableCollection( availableCoverages.values() );
  }
}

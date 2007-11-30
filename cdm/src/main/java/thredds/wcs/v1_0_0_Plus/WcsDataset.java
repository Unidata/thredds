package thredds.wcs.v1_0_0_Plus;

import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
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
  private GridDataset dataset;
  private HashMap<String, WcsCoverage> availableCoverages;

  public WcsDataset( GridDataset dataset, String datasetPath )
  {
    this.datasetPath = datasetPath;
    this.dataset = dataset;

    this.availableCoverages = new HashMap<String, WcsCoverage>();

    // ToDo WCS 1.0PlusPlus - compartmentalize coverage to hide GridDatatype vs GridDataset.Gridset ???
    // ToDo WCS 1.0Plus - change FROM coverage for each parameter TO coverage for each coordinate system
    // This is WCS 1.0 coverage for each parameter
    for ( GridDatatype curGridDatatype : this.dataset.getGrids() )
    {
      GridCoordSystem gcs = curGridDatatype.getCoordinateSystem();
      if ( !gcs.isRegularSpatial() )
        continue;
      this.availableCoverages.put( curGridDatatype.getName(), new WcsCoverage( curGridDatatype, this) );
    }
    // ToDo WCS 1.0Plus - change FROM coverage for each parameter TO coverage for each coordinate system
    // This is WCS 1.1 style coverage for each coordinate system
    // for ( GridDataset.Gridset curGridSet : this.dataset.getGridsets())
    // {
    //   GridCoordSystem gcs = curGridSet.getGeoCoordSystem();
    //   if ( !gcs.isRegularSpatial() )
    //     continue;
    //   this.availableCoverages.put( gcs.getName(), curGridSet );
    // }
  }

  public String getDatasetPath() { return datasetPath; }
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

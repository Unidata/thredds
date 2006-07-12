// $Id$
package ucar.nc2.dt.trajectory;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasetCache;
import ucar.nc2.dt.TrajectoryObsDataset;

/**
 * A factory for TrajectoryObsDataset.
 *
 * @author edavis
 * @since Feb 9, 2005T12:08:44 PM
 */
public class TrajectoryObsDatasetFactory
{

  static public TrajectoryObsDataset open( String netcdfFileURI) throws java.io.IOException {
     return open( netcdfFileURI, null);
  }

  static public TrajectoryObsDataset open( String netcdfFileURI, ucar.nc2.util.CancelTask cancelTask)
          throws java.io.IOException
  {
    NetcdfDataset ds = NetcdfDatasetCache.acquire( netcdfFileURI, cancelTask);
    if ( RafTrajectoryObsDataset.isMine( ds) )
      return new RafTrajectoryObsDataset( ds);
    else if ( SimpleTrajectoryObsDataset.isMine( ds))
      return new SimpleTrajectoryObsDataset( ds);
    else if ( Float10TrajectoryObsDataset.isMine( ds))
      return new Float10TrajectoryObsDataset( ds);
    else if ( ZebraClassTrajectoryObsDataset.isMine( ds ) )
      return new ZebraClassTrajectoryObsDataset( ds );
    else if ( ARMTrajectoryObsDataset.isMine( ds ) )
      return new ARMTrajectoryObsDataset( ds );
    else
      return null;

  }
}

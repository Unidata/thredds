// $Id: TrajectoryObsDatasetFactory.java 63 2006-07-12 21:50:51Z edavis $
package ucar.nc2.dt.trajectory;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.TrajectoryObsDataset;

/**
 * A factory for TrajectoryObsDataset.
 *
 * @author edavis
 * @since Feb 9, 2005T12:08:44 PM
 * @deprecated use ucar.nc2.dt.TypedDatasetFactory
 */
public class TrajectoryObsDatasetFactory
{

  static public TrajectoryObsDataset open( String netcdfFileURI) throws java.io.IOException {
     return open( netcdfFileURI, null);
  }

  static public TrajectoryObsDataset open( String netcdfFileURI, ucar.nc2.util.CancelTask cancelTask)
          throws java.io.IOException
  {
    NetcdfDataset ds = NetcdfDataset.acquireDataset( netcdfFileURI, cancelTask);
    if ( RafTrajectoryObsDataset.isValidFile( ds) )
      return new RafTrajectoryObsDataset( ds);
    else if ( SimpleTrajectoryObsDataset.isValidFile( ds))
      return new SimpleTrajectoryObsDataset( ds);
    else if ( Float10TrajectoryObsDataset.isValidFile( ds))
      return new Float10TrajectoryObsDataset( ds);
    else if ( ZebraClassTrajectoryObsDataset.isValidFile( ds ) )
      return new ZebraClassTrajectoryObsDataset( ds );
    else if ( ARMTrajectoryObsDataset.isValidFile( ds ) )
      return new ARMTrajectoryObsDataset( ds );
    else
      return null;

  }
}

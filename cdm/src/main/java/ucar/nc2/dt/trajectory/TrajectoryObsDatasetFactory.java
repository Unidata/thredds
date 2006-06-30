// $Id: TrajectoryObsDatasetFactory.java,v 1.9 2006/02/13 19:51:33 caron Exp $
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

/*
 * $Log: TrajectoryObsDatasetFactory.java,v $
 * Revision 1.9  2006/02/13 19:51:33  caron
 * javadoc
 *
 * Revision 1.8  2005/11/17 00:48:18  caron
 * NcML aggregation
 * caching close/synch
 * grid subset bug
 *
 * Revision 1.7  2005/07/24 01:25:19  caron
 * use cache wherever possible.
 *
 * Revision 1.6  2005/05/16 23:49:54  edavis
 * Add ucar.nc2.dt.trajectory.ARMTrajectoryObsDataset to handle
 * ARM sounding files. Plus a few other fixes and updates to the
 * tests.
 *
 * Revision 1.5  2005/05/16 18:26:37  edavis
 * Add ucar.nc2.dt.trajectory.ZebraClassTrajectoryObsDataset to handle
 * RICO sounding files (Zebra class soundings).
 *
 * Revision 1.4  2005/05/11 00:10:03  caron
 * refactor StuctureData, dt.point
 *
 * Revision 1.3  2005/03/18 00:29:07  edavis
 * Finish trajectory implementations with the new TrajectoryObsDatatype
 * and TrajectoryObsDataset interfaces and update tests.
 *
 * Revision 1.2  2005/03/11 23:02:13  caron
 * *** empty log message ***
 *
 * Revision 1.1  2005/03/10 21:34:17  edavis
 * Redo trajectory implementations with new TrajectoryObsDatatype and
 * TrajectoryObsDataset interfaces.
 *
 *
 */
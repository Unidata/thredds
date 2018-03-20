/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
// $Id: TrajectoryObsDatasetFactory.java 63 2006-07-12 21:50:51Z edavis $
package ucar.nc2.dt.trajectory;

import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.TrajectoryObsDataset;

import java.io.IOException;

/**
 * A factory for TrajectoryObsDataset.
 *
 * @author edavis
 * @since Feb 9, 2005T12:08:44 PM
 * @deprecated use ucar.nc2.dt.TypedDatasetFactory
 */
public class TrajectoryObsDatasetFactory {

  static public TrajectoryObsDataset open(String netcdfFileURI) throws IOException {
    return open(netcdfFileURI, null);
  }

  static public TrajectoryObsDataset open(String netcdfFileURI, ucar.nc2.util.CancelTask cancelTask)
          throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(netcdfFileURI);
    NetcdfDataset ds = NetcdfDataset.acquireDataset(durl, true, cancelTask);
    if (RafTrajectoryObsDataset.isValidFile(ds))
      return new RafTrajectoryObsDataset(ds);
    else if (SimpleTrajectoryObsDataset.isValidFile(ds))
      return new SimpleTrajectoryObsDataset(ds);
    else if (Float10TrajectoryObsDataset.isValidFile(ds))
      return new Float10TrajectoryObsDataset(ds);
    else if (ZebraClassTrajectoryObsDataset.isValidFile(ds))
      return new ZebraClassTrajectoryObsDataset(ds);
    else if (ARMTrajectoryObsDataset.isValidFile(ds))
      return new ARMTrajectoryObsDataset(ds);
    else
      return null;

  }
}

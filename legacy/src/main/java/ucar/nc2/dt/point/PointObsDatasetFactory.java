/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id:PointObsDatasetFactory.java 51 2006-07-12 17:13:13Z caron $

package ucar.nc2.dt.point;

import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.PointObsDataset;
import ucar.nc2.NetcdfFile;
import thredds.catalog.InvAccess;

/**
 * A factory for both PointObsDataset and StationObsDataset.
 *
 * @author caron
 * @deprecated use ucar.nc2.ft.point
 */
public class PointObsDatasetFactory {

  static public PointObsDataset open(InvAccess access, StringBuffer logMessages) throws java.io.IOException {
    return open( access, null, logMessages);
  }

  static public PointObsDataset open(InvAccess access, ucar.nc2.util.CancelTask task, StringBuffer logMessages) throws java.io.IOException {
    return open( access.getStandardUrlName(), task, logMessages);
  }

  static public PointObsDataset open( String location) throws java.io.IOException {
    return open(location, null, null);
  }

  static public PointObsDataset open( String location, StringBuffer log) throws java.io.IOException {
    return open(location, null, log);
  }

  static public PointObsDataset open( String location, ucar.nc2.util.CancelTask task, StringBuffer log) throws java.io.IOException {

    // otherwise open as netcdf and have a look. use NetcdfDataset in order to deal with scale/enhance, etc.
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    NetcdfDataset ncfile = NetcdfDataset.acquireDataset( durl, true, task);

    // add record variable if there is one.
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    if (UnidataStationObsDataset.isValidFile( ncfile))
      return new UnidataStationObsDataset( ncfile);

    if (UnidataPointObsDataset.isValidFile( ncfile))
      return new UnidataPointObsDataset( ncfile);

    /* if (DapperDataset.isValidFile( ncfile))
      return DapperDataset.factory( ncfile);

    if (SequenceObsDataset.isValidFile( ncfile))
      return new SequenceObsDataset( ncfile, task); */

    if (UnidataStationObsDataset2.isValidFile( ncfile))
      return new UnidataStationObsDataset2( ncfile);

    if (NdbcDataset.isValidFile( ncfile))
      return new NdbcDataset( ncfile);

    if (MadisStationObsDataset.isValidFile( ncfile))
      return new MadisStationObsDataset( ncfile);

    if (OldUnidataStationObsDataset.isValidFile(ncfile))
      return new OldUnidataStationObsDataset( ncfile);

    // put at end to minimize false positive
    if (OldUnidataPointObsDataset.isValidFile( ncfile))
       return new OldUnidataPointObsDataset( ncfile);

    if (null != log) log.append("Cant find a Point/Station adapter for ").append(location);
    ncfile.close();
    return null;
  }

}

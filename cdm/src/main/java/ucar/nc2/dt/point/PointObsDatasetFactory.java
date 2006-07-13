// $Id:PointObsDatasetFactory.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.dt.point;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasetCache;
import ucar.nc2.dt.PointObsDataset;
import ucar.nc2.adde.AddeStationObsDataset;
import thredds.catalog.InvAccess;
import thredds.catalog.ServiceType;
import thredds.catalog.query.DqcFactory;
import thredds.catalog.query.QueryCapability;

/**
 * A factory for both PointObsDataset and StationObsDataset.
 *
 * @author caron
 * @version $Revision:51 $ $Date:2006-07-12 17:13:13Z $
 */
public class PointObsDatasetFactory {

  static public PointObsDataset open(InvAccess access, StringBuffer logMessages) throws java.io.IOException {
    return open( access, null, logMessages);
  }

  static public PointObsDataset open(InvAccess access, ucar.nc2.util.CancelTask task, StringBuffer logMessages) throws java.io.IOException {
    if (access.getService().getServiceType() == ServiceType.ADDE)
      return new AddeStationObsDataset(access, task);

    return open( access.getStandardUrlName(), task, logMessages);
  }

  static public PointObsDataset open( String location) throws java.io.IOException {
    return open(location, null, null);
  }

  static public PointObsDataset open( String location, StringBuffer log) throws java.io.IOException {
    return open(location, null, log);
  }

  static public PointObsDataset open( String location, ucar.nc2.util.CancelTask task, StringBuffer log) throws java.io.IOException {

    if (location.startsWith("adde:"))
      return new AddeStationObsDataset( location, task);

    if (location.startsWith("thredds:")) { // LOOK need to distinguish between a DQC and a Catalog !!
      location = location.substring(8);
      DqcFactory dqcFactory = new DqcFactory(true);
      QueryCapability dqc = dqcFactory.readXML(location);
      if (dqc.hasFatalError()) {
        if (null != log) log.append(dqc.getErrorMessages());
        return null;
      }

      return ucar.nc2.thredds.DqcStationObsDataset.factory( null, dqc);
    }

    // otherwise open as netcdf and have a look. use NetcdfDataset in order to deal with scale/enhance, etc.
    NetcdfDataset ncfile = NetcdfDatasetCache.acquire( location, task);

    // add record variable if there is one.
    ncfile.addRecordStructure();

    if (UnidataStationObsDataset.isValidFile( ncfile))
      return new UnidataStationObsDataset( ncfile);

    if (UnidataPointObsDataset.isValidFile( ncfile))
      return new UnidataPointObsDataset( ncfile);

    if (DapperDataset.isValidFile( ncfile))
      return DapperDataset.factory( ncfile);

    if (SequenceObsDataset.isValidFile( ncfile))
      return new SequenceObsDataset( ncfile, task);

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

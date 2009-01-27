// $Id:PointObsDatasetFactory.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.dt.point;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.PointObsDataset;
import ucar.nc2.iosp.adde.AddeStationObsDataset;
import ucar.nc2.NetcdfFile;
import thredds.catalog.InvAccess;
import thredds.catalog.ServiceType;

/**
 * A factory for both PointObsDataset and StationObsDataset.
 *
 * @author caron
 * @deprecated use ucar.nc2.dt.TypedDatasetFactory
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

    /* if (location.startsWith("thredds:")) { // LOOK need to distinguish between a DQC and a Catalog !!
      location = location.substring(8);
      DqcFactory dqcFactory = new DqcFactory(true);
      QueryCapability dqc = dqcFactory.readXML(location);
      if (dqc.hasFatalError()) {
        if (null != log) log.append(dqc.getErrorMessages());
        return null;
      }

      return ucar.nc2.thredds.DqcStationObsDataset.factory( null, dqc);
    } */

    // otherwise open as netcdf and have a look. use NetcdfDataset in order to deal with scale/enhance, etc.
    NetcdfDataset ncfile = NetcdfDataset.acquireDataset( location, task);

    // add record variable if there is one.
    ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

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

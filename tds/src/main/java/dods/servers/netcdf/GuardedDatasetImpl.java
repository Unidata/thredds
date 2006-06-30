// $Id: GuardedDatasetImpl.java,v 1.8 2006/01/20 20:42:02 caron Exp $
/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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
package dods.servers.netcdf;

import dods.servlet.GuardedDataset;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.NetcdfFileCache;

import java.io.IOException;

/**
 * This uses NetcdfFileCache to implement a GuardedDataset, caching the underlying files
 * in NetcdfFileCache for performance.
 */
public class GuardedDatasetImpl implements GuardedDataset  {
  static protected org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GuardedDatasetImpl.class);

  private boolean hasSession;
  private NetcdfFile org_file;
  private NcDDS dds;
  private NcDAS das;

  public GuardedDatasetImpl( String reqPath, String location) throws IOException {
    this( reqPath, location, null);
  }

  public GuardedDatasetImpl( String reqPath, String location, org.jdom.Element netcdfElem) throws IOException {
    this.org_file = NetcdfDataset.acquireFile(location, null);
    NetcdfFile ncfile = org_file;

    if (netcdfElem != null) {
      NetcdfDataset ncd = (org_file instanceof NetcdfDataset) ? (NetcdfDataset) org_file : new NetcdfDataset( org_file, false);
      new NcMLReader().readNetcdf( reqPath, ncd, ncd, netcdfElem, null);
      ncfile = ncd;
    }

    this.dds = new NcDDS( reqPath, ncfile);
    this.das = new NcDAS( ncfile);
  }

  public void release() {
    if (!hasSession)
      close();
  }

  public void close() {
    try {
      org_file.close();
    } catch (IOException e) {
      log.error("GuardedDatasetImpl close", e);
    }
  }

  public GuardedDatasetImpl( String reqPath, NetcdfFile ncfile, boolean hasSession) {
    this.org_file = ncfile;
    this.dds = new NcDDS( reqPath, ncfile);
    this.das = new NcDAS( ncfile);
    this.hasSession = hasSession;
  }

  public dods.dap.Server.ServerDDS getDDS() { return (dods.dap.Server.ServerDDS) dds.clone(); }
  public dods.dap.DAS getDAS() { return (dods.dap.DAS) das.clone(); }

  public String toString() {
    String name = org_file.getLocation();
    return name == null ? org_file.getCacheName() : name;
  }
}
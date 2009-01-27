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
package thredds.server.opendap;

import opendap.servlet.GuardedDataset;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

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
    NetcdfFile ncfile = NetcdfDataset.acquireFile(location, null);
    this.org_file = ncfile;
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

  public opendap.dap.Server.ServerDDS getDDS() { return (opendap.dap.Server.ServerDDS) dds.clone(); }
  public opendap.dap.DAS getDAS() { return (opendap.dap.DAS) das.clone(); }

  public String toString() {
    String name = org_file.getLocation();
    return name == null ? org_file.getCacheName() : name;
  }
}
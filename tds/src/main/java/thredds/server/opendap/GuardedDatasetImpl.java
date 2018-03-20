/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.opendap;

import opendap.servlet.GuardedDataset;
import ucar.nc2.NetcdfFile;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;

/**
 * Regenerate DDS, DAS each time
 */
@Immutable
public class GuardedDatasetImpl implements GuardedDataset {
  static protected org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GuardedDataset.class);

  private final boolean hasSession;
  private final NetcdfFile org_file;
  private final String reqPath;
  private boolean closed = false;

  public void release() {
    if (!hasSession)
      close();
    closed = true;
  }

  public void close() {
    try {
      org_file.close();
    } catch (IOException e) {
      log.error("GuardedDatasetImpl close", e);
    }
  }

  public GuardedDatasetImpl(String reqPath, NetcdfFile ncfile, boolean hasSession) {
    this.org_file = ncfile;
    this.reqPath = reqPath;
    this.hasSession = hasSession;
  }

  public opendap.servers.ServerDDS getDDS() {
    if (closed) throw new IllegalStateException("getDDS(): "+this + " closed");
    return new NcDDS(reqPath, org_file);
  }

  public opendap.dap.DAS getDAS() {
    if (closed) throw new IllegalStateException("getDAS(): "+this + " closed");
    return new NcDAS(org_file);
  }

  public String toString() {
    String name = org_file.getLocation();
    return name == null ? org_file.getCacheName() : name;
  }
}

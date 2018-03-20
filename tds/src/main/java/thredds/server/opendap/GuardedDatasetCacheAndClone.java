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
 * This creates and caches DDS, DAS, then clones them when they are needed.
 */
@Immutable
public class GuardedDatasetCacheAndClone implements GuardedDataset {
  static protected org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GuardedDataset.class);

  private final boolean hasSession;
  private final NetcdfFile org_file;
  private final NcDDS dds;
  private final NcDAS das;

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

  public GuardedDatasetCacheAndClone(String reqPath, NetcdfFile ncfile, boolean hasSession) {
    this.org_file = ncfile;
    this.dds = new NcDDS(reqPath, ncfile);
    this.das = new NcDAS(ncfile);
    this.hasSession = hasSession;
  }

  public opendap.servers.ServerDDS getDDS() {
    return (opendap.servers.ServerDDS) dds.clone();
  }

  public opendap.dap.DAS getDAS() {
    return (opendap.dap.DAS) das.clone();
  }

  public String toString() {
    String name = org_file.getLocation();
    return name == null ? org_file.getCacheName() : name;
  }
}

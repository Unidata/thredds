/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.monitor;

import ucar.nc2.dataset.NetcdfDataset;

import java.util.List;
import java.util.ArrayList;

/**
 * Class Description.
 *
 * @author caron
 * @since Jan 13, 2009
 */
public class NetcdfFileCacheMonitorImpl implements NetcdfFileCacheMonitor {

  public List<String> getCachedFiles() {
    ucar.nc2.util.cache.FileCacheIF fc = NetcdfDataset.getNetcdfFileCache();
    if (fc == null) return new ArrayList<>();

    return fc.showCache();
  }
}

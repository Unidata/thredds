/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.monitor;

import java.util.List;

/**
 * interface for monitoring NetcdfFile cache files, used by JMX.
 *
 * @author caron
 * @since Jan 13, 2009
 */
public interface NetcdfFileCacheMonitor {
  List<String> getCachedFiles();
}

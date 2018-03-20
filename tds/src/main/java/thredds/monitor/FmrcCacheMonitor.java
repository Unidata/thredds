/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.monitor;

import java.util.List;

/**
 * interface for monitoring BDB cache files, used by JMX.
 *
 * @author caron
 * @since Apr 19, 2010
 */
public interface FmrcCacheMonitor {
  List<String> getCachedCollections();
}

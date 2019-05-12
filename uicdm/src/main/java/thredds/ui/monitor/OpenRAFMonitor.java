/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.ui.monitor;

import java.util.List;

/**
 * @author caron
 * @since Mar 15, 2008
 */
public interface OpenRAFMonitor {
  boolean getDebugLeaks();
  void setDebugLeaks(boolean b);
  List<String> getOpenFiles();
}

/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.monitor;

import java.util.List;

/**
 * interface for monitoring RAF files, used by JMX.
 * @author caron
 * @since Mar 15, 2008
 */
public interface OpenRAFMonitor {
  boolean getDebugLeaks();
  void setDebugLeaks(boolean b);
  List<String> getOpenFiles();
  int getNseeks();
  long getNbytes();
}


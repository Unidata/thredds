/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.ui.monitor;

import ucar.unidata.io.RandomAccessFile;

import java.util.List;

/**
 * @author caron
 * @since Mar 15, 2008
 */
public class OpenRAFMonitorImpl implements OpenRAFMonitor {
  public boolean getDebugLeaks() {
    return RandomAccessFile.getDebugLeaks();
  }

  public void setDebugLeaks(boolean b) {
    RandomAccessFile.setDebugLeaks(b);
  }

  public List<String> getOpenFiles() {
    return RandomAccessFile.getOpenFiles();
  }
}

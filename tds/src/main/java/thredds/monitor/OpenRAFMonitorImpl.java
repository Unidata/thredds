/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.monitor;

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

  @Override
  public int getNseeks() {
    return RandomAccessFile.getDebugNseeks();
  }

  @Override
  public long getNbytes() {
    return RandomAccessFile.getDebugNbytes();
  }
}


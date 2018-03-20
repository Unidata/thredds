/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.filter;

import thredds.inventory.MFileFilter;
import thredds.inventory.MFile;

/**
 * Accept datasets whose last modified date is at least the given amount of time in the past.
 *
 * @author edavis
 * @author jcaron
 * @since Jul 8, 2009
 */


public class LastModifiedLimit implements MFileFilter {
  private final long lastModifiedLimitInMillis;

  /**
   * Constructor.
   *
   * @param lastModifiedLimitInMillis accept datasets whose lastModified() time is at least this many msecs in the past
   */
  public LastModifiedLimit(long lastModifiedLimitInMillis) {
    this.lastModifiedLimitInMillis = lastModifiedLimitInMillis;
  }

  public boolean accept(MFile dataset) {
    long lastModified = dataset.getLastModified();
    if (lastModified < 0) return true;  // means dont know - can happen for remote files

    long now = System.currentTimeMillis();
    if (now - lastModified > lastModifiedLimitInMillis)
      return true;
    return false;
  }

  public long getLastModifiedLimitInMillis() {
    return lastModifiedLimitInMillis;
  }

}

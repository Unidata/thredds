/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.cache;

/**
 * A FileCache that does nothing when release() is called.
 *
 * @author caron
 * @since Mar 3, 2009
 */
public class FileCacheNOP extends FileCache {

  public FileCacheNOP() {
    super("FileCacheNOP", 0, 0, 0, 0);
  }

  public boolean release(FileCacheable ncfile) {
    // no - op
    return false;
  }

}

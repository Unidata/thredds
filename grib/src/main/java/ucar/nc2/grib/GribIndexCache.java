/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import ucar.nc2.grib.collection.Grib;
import ucar.nc2.util.DiskCache2;

import java.io.File;

/**
 * manages where the grib index files live
 *
 * @author caron
 * @since 12/18/2014
 */
public class GribIndexCache {

  private static DiskCache2 diskCache;

  public static synchronized void setDiskCache2(DiskCache2 dc) {
    diskCache = dc;
  }

  public static synchronized DiskCache2 getDiskCache2() {
    if (diskCache == null)
      diskCache = DiskCache2.getDefault();
    return diskCache;
  }

  /**
   * Get index file, may be in cache directory, may not exist
   *
   * @param fileLocation full path of original index filename
   * @return File, possibly in cache, may or may not exist
   */
  public static File getFileOrCache(String fileLocation) {
    File result = getExistingFileOrCache(fileLocation);
    if (result != null) return result;
    return getDiskCache2().getFile(fileLocation);
  }

  /**
   * Looking for an existing file, in cache or not
   *
   * @param fileLocation full path of original index filename
   * @return existing file if you can find it, else null
   */
  public static File getExistingFileOrCache(String fileLocation) {
    File result =  getDiskCache2().getExistingFileOrCache(fileLocation);
    if (result == null && Grib.debugGbxIndexOnly && fileLocation.endsWith(".gbx9.ncx4")) { // might create only from gbx9 for debugging
      int length = fileLocation.length();
      String maybeIndexAlreadyExists = fileLocation.substring(0, length-10)+".ncx4";
      result =  getDiskCache2().getExistingFileOrCache(maybeIndexAlreadyExists);
    }
    return result;
  }
}

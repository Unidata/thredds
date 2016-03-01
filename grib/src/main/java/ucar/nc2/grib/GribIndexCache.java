/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
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

  static private DiskCache2 diskCache;

  static synchronized public void setDiskCache2(DiskCache2 dc) {
    diskCache = dc;
  }

  static synchronized public DiskCache2 getDiskCache2() {
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
  static public File getFileOrCache(String fileLocation) {
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
  static public File getExistingFileOrCache(String fileLocation) {
    File result =  getDiskCache2().getExistingFileOrCache(fileLocation);
    if (result == null && Grib.debugGbxIndexOnly && fileLocation.endsWith(".gbx9.ncx4")) { // might create only from gbx9 for debugging
      int length = fileLocation.length();
      String maybeIndexAlreadyExists = fileLocation.substring(0, length-10)+".ncx4";
      result =  getDiskCache2().getExistingFileOrCache(maybeIndexAlreadyExists);
    }
    return result;
  }
}

/*
* Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
*
* Portions of this software were developed by the Unidata Program at the
* University Corporation for Atmospheric Research.
*
* Access and use of this software shall impose the following obligations
* and understandings on the user. The user is granted the right, without
* any fee or cost, to use, copy, modify, alter, enhance and distribute
* this software, and any derivative works thereof, and its supporting
* documentation for any purpose whatsoever, provided that this entire
* notice appears in all copies of the software, derivative works and
* supporting documentation.  Further, UCAR requests that the user credit
* UCAR/Unidata in any publications that result from the use of this
* software or in any product that includes this software. The names UCAR
* and/or Unidata, however, may not be used in any advertising or publicity
* to endorse or promote any products or commercial entity unless specific
* written permission is obtained from UCAR/Unidata. The user also
* understands that UCAR/Unidata is not obligated to provide the user with
* any support, consulting, training or assistance of any kind with regard
* to the use, operation and performance of this software nor to provide
* the user with any updates, revisions, new versions or "bug fixes."
*
* THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
* IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
* INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
* FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
* NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
* WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
*/

/**
 * Created by IntelliJ IDEA.
 * User: Robb
 * Date: Sep 4, 2009
 * Time: 8:37:47 AM
 * To change this template use File | Settings | File Templates.
 */


package ucar.grib;

import java.io.File;
import java.io.IOException;

public class GribIndexName {
  /**
   * Current Index suffix, suffix to use by default
   */
  public static final String currentSuffix = ".gbx8";

  /**
   * Old Index suffix
   */
  public static final String oldSuffix = ".gbx";

  /**
   * Update to current suffix
   */
  public static boolean updateToCurrent = false;

  /**
   * Create current index
   */
  public static boolean createCurrent = true;

  /**
   * Cache Directory, directory to create index if it can't be create in the same directory as
   * the grib file directory
   */
  public static String cache = null;

  /**
   * Get Index Name, return currentSuffix  
   * if no index exist, create name with currentSuffix
   * @param gribName used to make Index name
   * @return gribIndexName String
   */
  public static String getCurrentSuffix(String gribName ) {
    return getIndex( gribName, false );
  }

  /**
   * Get Index Name, return currentSuffix first, then oldSuffix if it exists.
   * if no index exist, create name with currentSuffix
   * @param gribName used to make Index name
   * @return gribIndexName String
   */
  public static String get(String gribName ) {
    return getIndex( gribName, true );
  }

  public static String getIndex(String gribName, boolean includeOld ) {

    File fidx;
    if (gribName.endsWith(oldSuffix))
      gribName = gribName.replace(oldSuffix, "");
    if (gribName.endsWith(currentSuffix)) {
      fidx = new File(gribName);
    } else {
      fidx = new File(gribName + currentSuffix);
    }
    if (fidx.exists()) {
      return fidx.getPath();
    } else if( includeOld ) { // try older suffix
      fidx = new File(gribName + oldSuffix);
      if (fidx.exists())
        return fidx.getPath();
    }

    // create and test write permissions
    fidx = new File(gribName + currentSuffix);
    // now comes the tricky part to make sure we can open and write to it
    try {
      if (fidx.createNewFile()) {
        fidx.delete();
        return fidx.getPath();
      }
    } catch (IOException e) {
      // cant write to it - drop through
    }
    if (cache != null) {
      return cache + gribName + currentSuffix;
    }
    // punt
    return gribName + currentSuffix;
  }

  public static boolean isUpdateToCurrent() {
    return updateToCurrent;
  }

  public static void setUpdateToCurrent(boolean updateToCurrent) {
    GribIndexName.updateToCurrent = updateToCurrent;
  }

  public static boolean isCreateCurrent() {
    return createCurrent;
  }

  public static void setCreateCurrent(boolean createCurrent) {
    GribIndexName.createCurrent = createCurrent;
  }

  public static String getCache() {
    return cache;
  }

  public static void setCache(String cache) {
    GribIndexName.cache = cache;
  }

}

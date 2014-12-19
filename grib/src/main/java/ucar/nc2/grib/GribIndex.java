/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.grib;

import thredds.inventory.CollectionManager;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MFile;
import ucar.nc2.grib.grib1.Grib1Index;
import ucar.nc2.grib.grib2.Grib2Index;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;

/**
 * Abstract superclass for Grib1Index and Grib2Index.
 * Handles gbx9 index for grib.
 * <p/>
 * Static methods for creating gbx9 indices for a single file.
 *
 * @author John
 * @since 9/5/11
 */
public abstract class GribIndex {
  public static final String GBX9_IDX = ".gbx9";
  public static final boolean debug = false;

  private static final CollectionManager.ChangeChecker gribCC = new CollectionManager.ChangeChecker() {
    public boolean hasChangedSince(MFile file, long when) {
      String idxPath = file.getPath();
      if (!idxPath.endsWith(GBX9_IDX)) idxPath += GBX9_IDX;
      File idxFile = GribIndexCache.getExistingFileOrCache(idxPath);
      if (idxFile == null) return true;

      long idxLastModified =  idxFile.lastModified();
      if (idxLastModified < file.getLastModified()) return true;
      if (0 < when && when < idxLastModified) return true;
      return false;
    }
    public boolean hasntChangedSince(MFile file, long when) {
      String idxPath = file.getPath();
      if (!idxPath.endsWith(GBX9_IDX)) idxPath += GBX9_IDX;
      File idxFile = GribIndexCache.getExistingFileOrCache(idxPath);
      if (idxFile == null) return true;

      if (idxFile.lastModified() < file.getLastModified()) return true;
      if (0 < when && idxFile.lastModified() < when) return true;
      return false;
    }
  };
  /////////////////////////////////////////////////////////////////////////

  public static CollectionManager.ChangeChecker getChangeChecker() {
    return gribCC;
  }

  public static GribIndex open(boolean isGrib1, MFile mfile) throws IOException {

    GribIndex index = isGrib1 ? new Grib1Index() : new Grib2Index();

    if (!index.readIndex(mfile.getPath(), mfile.getLastModified(), CollectionUpdateType.never)) {
      return null;
    }

    return index;
  }

  /**
   * Create a gbx9 index from a single grib1 or grib2 file.
   * Use the existing index if it already exists.
   *
   * @param isGrib1 true if grib1
   * @param mfile the grib file
   * @param force  force writing index
   * @return the resulting GribIndex
   * @throws IOException on io error
   */
  public static GribIndex readOrCreateIndexFromSingleFile(boolean isGrib1, MFile mfile, CollectionUpdateType force, org.slf4j.Logger logger) throws IOException {

    GribIndex index = isGrib1 ? new Grib1Index() : new Grib2Index();

    if (!index.readIndex(mfile.getPath(), mfile.getLastModified(), force)) { // heres where the index date is checked against the data file
      index.makeIndex(mfile.getPath(), null);
      logger.debug("  Index written: {} == {} records", mfile.getName() + GBX9_IDX, index.getNRecords());
    } else if (debug) {
      logger.debug("  Index read: {} == {} records", mfile.getName() + GBX9_IDX, index.getNRecords());
    }

    return index;
  }

  //////////////////////////////////////////

  /**
   * Read the gbx9 index file.
   *
   * @param location location of the data file
   * @param dataModified last modified date of the data file
   * @param force rewrite? always, test, nocheck, never
   * @return true if index was successfully read, false if index must be (re)created
   * @throws IOException on io error
   */
  public abstract boolean readIndex(String location, long dataModified, CollectionUpdateType force) throws IOException;

  /**
   * Make the gbx9 index file.
   *
   * @param location location of the data file
   * @param dataRaf already opened data raf (leave open); if null, makeIndex opens and closes)
   * @return true
   * @throws IOException on io error
   */
  public abstract boolean makeIndex(String location, RandomAccessFile dataRaf) throws IOException;

  /**
   * The number of records in the index.
   * @return The number of records in the index.
   */
  public abstract int getNRecords();
}

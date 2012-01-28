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

import thredds.filesystem.MFileOS;
import thredds.inventory.CollectionManager;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.MFile;
import ucar.nc2.grib.grib1.Grib1CollectionBuilder;
//import ucar.nc2.grib.grib2.Grib2Index;
import ucar.nc2.grib.grib1.Grib1Index;
import ucar.nc2.grib.grib2.Grib2CollectionBuilder;
import ucar.nc2.grib.grib2.Grib2Index;
import ucar.nc2.util.DiskCache;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;

/**
 * Abstract superclass for Grib1Index and Grib2Index.
 * Handles gbx9 index for grib.
 * <p/>
 * Static methods for creating gbx9 and ncx indices for a single file.
 *
 * @author John
 * @since 9/5/11
 */
public abstract class GribIndex {
  public static final String IDX_EXT = ".gbx9";
  public static final boolean debug = false;

  private static final CollectionManager.ChangeChecker gribCC = new CollectionManager.ChangeChecker() {
    public boolean hasChangedSince(MFile file, long when) {
      File idxFile = getIndexFile(file.getPath());
      if (!idxFile.exists()) return true;
      if (idxFile.lastModified() < file.getLastModified()) return true;
      if (0 < when && when < idxFile.lastModified()) return true;
      return false;
    }
    public boolean hasntChangedSince(MFile file, long when) {
      File idxFile = getIndexFile(file.getPath());
      if (!idxFile.exists()) return true;
      if (idxFile.lastModified() < file.getLastModified()) return true;
      if (0 < when && idxFile.lastModified() < when) return true;
      return false;
    }
  };

  static public File getIndexFile(String path) {
    return DiskCache.getFile(path + IDX_EXT, false);
  }

  static public CollectionManager.ChangeChecker getChangeChecker() {
    return gribCC;
  }

  /**
   * Create a gbx9 and ncx index and a grib collection from a single grib1 or grib2 file.
   * Use the existing index is it already exists.
   *
   * @param isGrib1 true if grib1
   * @param dataRaf the grib file already open
   * @param config  special configuration
   * @param force  force writing index
   * @param f      put info here
   * @return the resulting GribCollection
   * @throws IOException on io error
   */
  public static GribCollection makeGribCollectionFromSingleFile(boolean isGrib1, RandomAccessFile dataRaf, FeatureCollectionConfig.GribConfig config,
                                                                CollectionManager.Force force, Formatter f) throws IOException {

    GribIndex gribIndex = isGrib1 ? new Grib1Index() : new Grib2Index() ;

    String filename = dataRaf.getLocation();
    File dataFile = new File(filename);
    boolean readOk;
    try {
      readOk = gribIndex.readIndex(filename, dataFile.lastModified(), force); // heres where the gbx9 file date is checked against the data file
    } catch (IOException ioe) {
      readOk = false;
    }

    // make or remake the index
    if (!readOk) {
      gribIndex.makeIndex(filename, f);
      f.format("  Index written: %s%n", filename + IDX_EXT);
    } else if (debug) {
      f.format("  Index read: %s%n", filename + IDX_EXT);
    }

    // heres where the ncx file date is checked against the data file
    MFile mfile = new MFileOS(dataFile);
    if (isGrib1)
      return Grib1CollectionBuilder.readOrCreateIndexFromSingleFile(mfile, force, config, f);
    else
      return Grib2CollectionBuilder.readOrCreateIndexFromSingleFile(mfile, force, config, f);
  }

  /**
   * Create a gbx9 and ncx index from a single grib1 or grib2 file.
   * Use the existing index is it already exists.
   *
   * @param isGrib1 true if grib1
   * @param createCollectionIndex true is you also want the ncx file to be created
   * @param mfile the grib file
   * @param config  special configuration
   * @param force  force writing index
   * @param f      put info here
   * @return the resulting GribIndex
   * @throws IOException on io error
   */
  public static GribIndex readOrCreateIndexFromSingleFile(boolean isGrib1, boolean createCollectionIndex,
         MFile mfile, FeatureCollectionConfig.GribConfig config, CollectionManager.Force force, Formatter f) throws IOException {

    GribIndex index = isGrib1 ? new Grib1Index() : new Grib2Index();

    if (!index.readIndex(mfile.getPath(), mfile.getLastModified(), force)) { // heres where the index date is checked against the data file
      index.makeIndex(mfile.getPath(), f);
      f.format("  Index written: %s == %d records %n", mfile.getName() + IDX_EXT, index.getNRecords());
    } else if (debug) {
      f.format("  Index read: %s == %d records %n", mfile.getName() + IDX_EXT, index.getNRecords());
    }

    if (!createCollectionIndex) return index;

     // heres where the ncx file date is checked against the data file
    GribCollection gc;
    if (isGrib1)
      gc = Grib1CollectionBuilder.readOrCreateIndexFromSingleFile(mfile, force, config, f);
    else
      gc = Grib2CollectionBuilder.readOrCreateIndexFromSingleFile(mfile, force, config, f);
    gc.close(); // dont need this right now

    return index;
  }

  /**
   * Read the gbx9 index file.
   *
   * @param location location of the data file
   * @param dataModified last modified date of the data file
   * @param force always, test or nocheck
   * @return true if index was successfully read, false if index must be (re)created
   * @throws IOException on io error
   */
  public abstract boolean readIndex(String location, long dataModified, CollectionManager.Force force) throws IOException;

  /**
   * Make the gbx9 index file.
   *
   * @param location location of the data file
   * @param f put error message here
   * @return true
   * @throws IOException on io error
   */
  public abstract boolean makeIndex(String location, Formatter f) throws IOException;

  /**
   * The number of records in the index.
   * @return The number of records in the index.
   */
  public abstract int getNRecords();
}

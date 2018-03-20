/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.grid;

import ucar.ma2.*;

import ucar.nc2.*;
//import ucar.nc2.dt.fmr.FmrcCoordSys;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.util.CancelTask;

import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

/**
 * Superclass for Gempak grid, MciDAS grid
 *
 * @author IDV Development Team
 */
public abstract class GridServiceProvider extends AbstractIOServiceProvider {

  public enum IndexExtendMode {
    /**
     * if data file changes, completely rewrite the index
     */
    rewrite,

    /**
     * if data file changes, assume its been extended, so extend the index
     */
    extendwrite,

    /**
     * if index file exists, use it as is
     */
    readonly
  }

  // these defaults are for clients, TDS sets these explicitly
  static protected IndexExtendMode indexFileModeOnOpen = IndexExtendMode.rewrite; // default is to rewrite
  static protected IndexExtendMode indexFileModeOnSync = IndexExtendMode.extendwrite; // default is to extend

  static protected boolean addLatLon = false; // add lat/lon coordinates for strict CF compliance LOOK should not be static !
  static protected boolean forceNewIndex = false; // force that a new index file is written - for debugging
  static protected boolean alwaysInCache = false;

  /**
   * debug flags
   */
  public static boolean debugOpen = false,
          debugMissing = false,
          debugMissingDetails = false,
          debugProj = false,
          debugTiming = false,
          debugVert = false;

  /**
   * Set whether to force new index or not
   *
   * @param b true to use
   */
  static public void forceNewIndex(boolean b) {
    forceNewIndex = b;
  }
  
  /**
   * Set the debug flags
   *
   * @param debugFlag debug flags
   */
  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugOpen = debugFlag.isSet("Grid/open");
    debugMissing = debugFlag.isSet("Grid/missing");
    debugMissingDetails = debugFlag.isSet("Grid/missingDetails");
    debugProj = debugFlag.isSet("Grid/projection");
    debugVert = debugFlag.isSet("Grid/vertical");
    debugTiming = debugFlag.isSet("Grid/timing");
  }

  /**
   * This controls what happens when a GRIB file is opened, and the data file has changed since the index was written.
   * <ol>
   * <li>IndexExtendMode.extendwrite: when GRIB file length increases, extend the index. This is the case when the file
   * is being appended to, as new data arrives.
   * <li>IndexExtendMode.rewrite: when GRIB file length changes, rewrite the index. This is the safest thing to do,
   * at the expense of performance.
   * <li>IndexExtendMode.readonly: never modify an existing index, just use it. However, if there is no index, created one
   * </ol>
   *
   * @param mode IndexExtendMode when file is opened
   */
  static public void setIndexFileModeOnOpen(IndexExtendMode mode) {
    indexFileModeOnOpen = mode;
  }

  /**
   * This controls what happens when sync() is called on a GRIB file. The main use of sync() is when you are using
   * NetcdfFile object caching. Before NetcdfFile is returned from a cache hit, sync() is called on it.
   * Default is IndexExtendMode.extend.
   *
   * @param mode IndexExtendMode when sync() is called. Same meaning as setIndexExtendMode(IndexExtendMode mode)
   */
  /**
   * This controls what happens when a GRIB file is synced (usually from FileCache), and the data or index file has changed
   *  since the file was placed in the cache.
   * <ol>
   * <li>IndexExtendMode.extendwrite: when GRIB file or index length increases, extend the index. If file or index length
   *   decreases, rewrite it.
   * <li>IndexExtendMode.rewrite: when GRIB file length changes, rewrite the index.
   * <li>IndexExtendMode.readonly: never modify an existing index, just use it. However, if there is no index, created one
   * </ol>
   *
   * @param mode IndexExtendMode when file is opened
   */
  static public void setIndexFileModeOnSync(IndexExtendMode mode) {
    indexFileModeOnSync = mode;
  }

  // backwards compatible with old API
  /**
   * Set how indexes are used for both open and sync
   *
   * @param b if true, set modes to IndexExtendMode.extendwrite, else IndexExtendMode.readonly
   * @deprecated use setIndexFileModeOnSync and setIndexFileModeOnOpen
   */
  static public void setExtendIndex(boolean b) {
    indexFileModeOnOpen = b ? IndexExtendMode.extendwrite : IndexExtendMode.readonly;
    indexFileModeOnSync = b ? IndexExtendMode.extendwrite : IndexExtendMode.readonly;
  }

  /**
   * Set disk cache policy for index files.
   * Default = false, meaning try to write index files in same directory as grib file.
   * True means always use the DiskCache area. TDS sets this to true, so it wont interfere with external indexer.
   *
   * @param b set to this value
   */
  static public void setIndexAlwaysInCache(boolean b) {
    alwaysInCache = b;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Use the given index to fill the NetcdfFile object with attributes and variables.
   *
   * @param index      GridIndex to use
   * @param cancelTask cancel task
   * @throws IOException problem reading the file
   */
  protected abstract void open(GridIndex index, CancelTask cancelTask) throws IOException;

  /**
   * Open the service provider for reading.
   *
   * @param raf        file to read from
   * @param ncfile     netCDF file we are writing to (memory)
   * @param cancelTask task for cancelling
   * @throws IOException problem reading file
   */
  @Override
  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    super.open(raf, ncfile, cancelTask);
  }

  /**
   * Read the data for the variable
   *
   * @param v2      Variable to read
   * @param section section infomation
   * @return Array of data
   * @throws IOException           problem reading from file
   * @throws InvalidRangeException invalid Range
   */
  @Override
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    long start = System.currentTimeMillis();

    Array dataArray = Array.factory(DataType.FLOAT, section.getShape());
    GridVariable pv = (GridVariable) v2.getSPobject();

    // Canonical ordering is ens, time, level, lat, lon
    int rangeIdx = 0;
    Range ensRange = pv.hasEnsemble() ? section.getRange(rangeIdx++) : new Range( 0, 0 );
    Range timeRange = (section.getRank() > 2) ? section.getRange(rangeIdx++) : new Range( 0, 0 );
    Range levRange = pv.hasVert() ? section.getRange(rangeIdx++) : new Range( 0, 0 );
    Range yRange = section.getRange(rangeIdx++);
    Range xRange = section.getRange(rangeIdx);

    IndexIterator ii = dataArray.getIndexIterator();

    for (int ensIdx : ensRange) {
      for (int timeIdx : timeRange) {
        for (int levelIdx : levRange) {
          readXY(v2, ensIdx, timeIdx, levelIdx, yRange, xRange, ii);
        }
      }
    }

    if (debugTiming) {
      long took = System.currentTimeMillis() - start;
      System.out.println("  read data took=" + took + " msec ");
    }

    return dataArray;
  }

  /**
   * read one YX array
   *
   * @param v2      variable to put the data into
   * @param ensIdx  ensemble index
   * @param timeIdx time index
   * @param levIdx  level index
   * @param yRange  x range
   * @param xRange  y range
   * @param ii      index iterator
   * @throws IOException           problem reading the file
   * @throws InvalidRangeException invalid range
   */
  private void readXY(Variable v2, int ensIdx, int timeIdx, int levIdx, Range yRange, Range xRange, IndexIterator ii)
          throws IOException, InvalidRangeException {

    GridVariable pv = (GridVariable) v2.getSPobject();
    GridHorizCoordSys hsys = pv.getHorizCoordSys();
    int nx = hsys.getNx();
    GridRecord record = pv.findRecord(ensIdx, timeIdx, levIdx);

    if (record == null) {
      Attribute att = v2.findAttribute("missing_value");
      float missing_value = (att == null) ? -9999.0f : att.getNumericValue().floatValue();

      int xyCount = yRange.length() * xRange.length();
      for (int j = 0; j < xyCount; j++) {
        ii.setFloatNext(missing_value);
      }
      return;
    }

    // otherwise read it
    float[] data = _readData(record);
    if (data == null) {
      _readData(record); // debug
      return;
    }

    // LOOK can improve with System.copy ??
    for (int y : yRange) {
      for (int x : xRange) {
        int index = y * nx + x;
        ii.setFloatNext(data[index]);
      }
    }
  }

  /**
   * Is this XY level missing?
   *
   * @param v2      Variable
   * @param timeIdx time index
   * @param ensIdx  ensemble index
   * @param levIdx  level index
   * @return true if missing
   * @throws InvalidRangeException invalid range
   */
  public boolean isMissingXY(Variable v2, int timeIdx, int ensIdx, int levIdx) throws InvalidRangeException {
    GridVariable pv = (GridVariable) v2.getSPobject();
    if ((timeIdx < 0) || (timeIdx >= pv.getNTimes())) {
      throw new InvalidRangeException("timeIdx=" + timeIdx);
    }
    if ((levIdx < 0) || (levIdx >= pv.getVertNlevels())) {
      throw new InvalidRangeException("levIdx=" + levIdx);
    }
    if ((ensIdx < 0) || (ensIdx >= pv.getNEnsembles())) {
      throw new InvalidRangeException("ensIdx=" + ensIdx);
    }
    return (null == pv.findRecord(ensIdx, timeIdx, levIdx));
  }

  /**
   * Read the data for this GridRecord
   *
   * @param gr grid identifier
   * @return the data (or null)
   * @throws IOException problem reading the data
   */
  protected abstract float[] _readData(GridRecord gr) throws IOException;

}

/*
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp.grib;

import ucar.grib.Index;
import ucar.grib.grib1.Grib1IndexExtender;
import ucar.grib.grib2.Grib2IndexExtender;
import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.iosp.AbstractIOServiceProvider;
import ucar.nc2.dt.fmr.FmrcCoordSys;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache;

import ucar.unidata.io.RandomAccessFile;

import java.io.*;
import java.net.URL;

/**
 * Superclass for grib1 and grib2 iosp.
 *
 * @author caron
 */
public abstract class GribServiceProvider extends AbstractIOServiceProvider {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GribServiceProvider.class);

  public enum IndexExtendMode {
    extend, rewrite, none
  }

  protected NetcdfFile ncfile;
  protected RandomAccessFile raf;
  protected FmrcCoordSys fmrcCoordSys;

  // keep this info to reopen index when extending or syncing
  private long rafLength;    // length of the file when opened - used for syncing
  private int saveEdition = 0;
  private String saveLocation;

  // debugging
  static boolean debugOpen = false, debugMissing = false, debugMissingDetails = false, debugProj = false, debugTiming = false, debugVert = false;

  static private boolean alwaysInCache = false;
  static public boolean addLatLon = false; // add lat/lon coordinates for striuct CF compliance
  static public boolean forceNewIndex = false; // force that a new index file is written
  static public boolean useMaximalCoordSys = false;
  static public IndexExtendMode extendMode = IndexExtendMode.rewrite; // default is to rewrite
  static public IndexExtendMode syncMode = IndexExtendMode.extend; // default is to extend

  static public void useMaximalCoordSys(boolean b) {
    useMaximalCoordSys = b;
  }

  static public void setIndexExtendMode(IndexExtendMode mode) {
    extendMode = mode;
  }

  static public void setIndexSyncMode(IndexExtendMode mode) {
    syncMode = mode;
  }

  // backwards compatible with old API
  static public void setExtendIndex(boolean b) {
    extendMode = b ? IndexExtendMode.extend : IndexExtendMode.none;
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

  static public void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugOpen = debugFlag.isSet("Grib/open");
    debugMissing = debugFlag.isSet("Grib/missing");
    debugMissingDetails = debugFlag.isSet("Grib/missingDetails");
    debugProj = debugFlag.isSet("Grib/projection");
    debugVert = debugFlag.isSet("Grib/vertical");
    debugTiming = debugFlag.isSet("Grib/timing");
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////

  protected abstract void open(Index index, CancelTask cancelTask) throws IOException;

  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {
    this.raf = raf;
    this.ncfile = ncfile;
    this.rafLength = raf.length();

    long start = System.currentTimeMillis();

    int edition = (this instanceof Grib1ServiceProvider) ? 1 : 2;
    Index index = getIndex(edition, raf.getLocation());

    open(index, cancelTask);
    log.debug("GribServiceProvider.open " + ncfile.getLocation() + " took " + (System.currentTimeMillis() - start));
  }

  public String toStringDebug(Object o) {
    if (o instanceof Variable) {
      Variable v = (Variable) o;
      GribVariable pv = (GribVariable) v.getSPobject();
      return (pv == null) ? "null GribVariable" : pv.dump();
    }
    return null;
  }

  /**
   * Open the index file. If not exists, create it.
   * When writing use DiskCache, to make sure location is writeable.
   *
   * @param edition    which grib edition
   * @param location   location of the file. The index file has ".gbx" appended.
   * @return ucar.grib.Index
   * @throws IOException on io error
   */
  protected Index getIndex(int edition, String location) throws IOException {
    // get an Index
    saveEdition = edition;
    saveLocation = location;
    String indexLocation = location + ".gbx";

    Index index;
    File indexFile;

    if (indexLocation.startsWith("http:")) { // direct access through http
      InputStream ios = indexExistsAsURL(indexLocation);
      if (ios != null) {
        index = new Index();
        index.open(indexLocation, ios);
        log.debug("opened HTTP index = " + indexLocation);
        return index;

      } else { // otherwise write it to / get it from the cache
        indexFile = DiskCache.getCacheFile(indexLocation);
        log.debug("HTTP index = " + indexFile.getPath());
      }

    } else {
      // always check first if the index file lives in the same dir as the regular file, and use it
      indexFile = new File(indexLocation);
      if (!indexFile.exists()) { // look in cache if need be
        log.debug("saveIndexFile not exist " + indexFile.getPath() + " ++ " + indexLocation);
        indexFile = DiskCache.getFile(indexLocation, alwaysInCache);
        log.debug("GribServiceProvider: use " + indexFile.getPath());
      }
    }

    // if index exist already, read it
    if (!forceNewIndex && indexFile.exists()) {
      index = new Index();
      boolean ok = true;
      try {
        ok = index.open(indexFile.getPath());
      } catch (Exception e) {
        ok = false;
      }

      if (ok) {
        log.debug("  opened index = " + indexFile.getPath());

        // deal with possiblity that the grib file has changed, and the index should be extended or rewritten.
        // action depends on extendMode
        String lengthS = index.getGlobalAttribute("length");
        long indexRafLength = (lengthS == null) ? 0 : Long.parseLong(lengthS);
        if (indexRafLength != rafLength) {

          if ((extendMode == IndexExtendMode.extend) && (indexRafLength < rafLength)) {
            log.debug("  extend Index = " + indexFile.getPath());
            index = extendIndex(edition, raf, indexFile, index);

          } else if (extendMode == IndexExtendMode.rewrite) {
            // write new index
            log.debug("  rewrite index = " + indexFile.getPath());
            index = writeIndex(edition, indexFile, raf);

          } else {
            log.debug("  index had new length, ignore ");
          }
        }

      } else {  // rewrite if fail to open
        log.debug("  write index = " + indexFile.getPath());
        index = writeIndex(edition, indexFile, raf);
      }

    } else {
      // doesnt exist (or is being forced), create it and write it
      log.debug("  write index = " + indexFile.getPath());
      index = writeIndex(edition, indexFile, raf);
    }

    return index;
  }

  private File getIndexFile(String location) throws IOException {
    String indexLocation = location + ".gbx";
    File indexFile = null;

    if (indexLocation.startsWith("http:")) { // LOOK direct access through http maybe should disallow ??
      indexFile = DiskCache.getCacheFile(indexLocation);
      log.debug("  HTTP index = " + indexFile.getPath());

    } else {
      // always check first if the index file lives in the same dir as the regular file, and use it
      indexFile = new File(indexLocation);
      if (!indexFile.exists()) { // look in cache if need be
        log.debug("GribServiceProvider: saveIndexFile not exist " + indexFile.getPath() + " ++ " + indexLocation);
        indexFile = DiskCache.getFile(indexLocation, alwaysInCache);
        log.debug("GribServiceProvider: use " + indexFile.getPath());
      }
    }

    return indexFile;
  }


  private Index writeIndex(int edition, File indexFile, RandomAccessFile raf) throws IOException {
    if (indexFile.exists()) {
      indexFile.delete();
      log.debug("Delete old index " + indexFile);
    }

    Index index = null;
    raf.seek(0);

    if (edition == 1) {
      ucar.grib.grib1.Grib1Indexer indexer = new ucar.grib.grib1.Grib1Indexer();
      PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(indexFile)));
      index = indexer.writeFileIndex(raf, ps, true);

    } else if (edition == 2) {
      ucar.grib.grib2.Grib2Indexer indexer2 = new ucar.grib.grib2.Grib2Indexer();
      PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(indexFile)));
      index = indexer2.writeFileIndex(raf, ps, true);
    }

    return index;
  }

  public boolean sync() throws IOException {
    if (syncMode == IndexExtendMode.none) return false;

    // has the file chenged?
    if (rafLength != raf.length()) {
      File indexFile = getIndexFile(saveLocation);
      Index index;

      if ((syncMode == IndexExtendMode.extend) && (rafLength < raf.length())) {
        log.debug("  sync extend Index = " + indexFile.getPath());
        extendIndex(saveEdition, raf, indexFile, null);
        index = new Index();
        index.open(indexFile.getPath());
        
      } else {
        // write new index
        log.debug("  sync rewrite index = " + indexFile.getPath());
        index = writeIndex(saveEdition, indexFile, raf);
      }

      // reconstruct the ncfile objects
      ncfile.empty();
      open(index, null);

      return true;
    }

    return false;
  }

  /*
   * takes a grib data file, a .gbx index file and a index, reads the current 
   * .gbx index, reads the data file starting at old eof for new
   * data, updates the *.gbx file and the index
   *
   */
  private Index extendIndex(int edition, RandomAccessFile raf, File indexFile, Index index) throws IOException {

    if (edition == 1) {
      Grib1IndexExtender indexExt = new Grib1IndexExtender();
      index = indexExt.extendIndex(raf, indexFile, index);

    } else if (edition == 2) {
      Grib2IndexExtender indexExt = new Grib2IndexExtender();
      index = indexExt.extendIndex(raf, indexFile, index);
    }
    return index;
  }

  // if exists, return input stream, otherwise null
  private InputStream indexExistsAsURL(String indexLocation) {
    try {
      URL url = new URL(indexLocation);
      return url.openStream();
    } catch (IOException e) {
      return null;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////
  public Array readData(Variable v2, Section section) throws IOException, InvalidRangeException {
    long start = System.currentTimeMillis();

    Array dataArray = Array.factory(DataType.FLOAT, section.getShape());
    GribVariable pv = (GribVariable) v2.getSPobject();

    int count = 0;
    Range timeRange = section.getRange(count++);
    Range levRange = pv.hasVert() ? section.getRange(count++) : null;
    Range yRange = section.getRange(count++);
    Range xRange = section.getRange(count);

    IndexIterator ii = dataArray.getIndexIteratorFast();

    // loop over time
    for (int timeIdx = timeRange.first(); timeIdx <= timeRange.last(); timeIdx += timeRange.stride()) {
      if (pv.hasVert())
        readLevel(v2, timeIdx, levRange, yRange, xRange, ii);
      else
        readXY(v2, timeIdx, 0, yRange, xRange, ii);
    }

    if (debugTiming) {
      long took = System.currentTimeMillis() - start;
      System.out.println("  read data took=" + took + " msec ");
    }

    return dataArray;
  }

  // loop over level
  public void readLevel(Variable v2, int timeIdx, Range levelRange, Range yRange, Range xRange, IndexIterator ii) throws IOException, InvalidRangeException {
    for (int levIdx = levelRange.first(); levIdx <= levelRange.last(); levIdx += levelRange.stride()) {
      readXY(v2, timeIdx, levIdx, yRange, xRange, ii);
    }
  }

  // read one product
  public void readXY(Variable v2, int timeIdx, int levIdx, Range yRange, Range xRange, IndexIterator ii) throws IOException, InvalidRangeException {
    Attribute att = v2.findAttribute("missing_value");
    float missing_value = (att == null) ? -9999.0f : att.getNumericValue().floatValue();

    GribVariable pv = (GribVariable) v2.getSPobject();
    GribHorizCoordSys hsys = pv.getHorizCoordSys();
    int nx = hsys.getNx();

    GribRecordLW record = pv.findRecord(timeIdx, levIdx);
    if (record == null) {
      int xyCount = yRange.length() * xRange.length();
      for (int j = 0; j < xyCount; j++)
        ii.setFloatNext(missing_value);
      return;
    }

    // otherwise read it
    float[] data;
    //try {
      data = _readData(record.offset1, record.offset2, record.decimalScale, record.bmsExists);
    //} catch (Exception e) {
    //  e.printStackTrace();
    //  return;
    //}

    for (int y = yRange.first(); y <= yRange.last(); y += yRange.stride()) {
      for (int x = xRange.first(); x <= xRange.last(); x += xRange.stride()) {
        int index = y * nx + x;
        ii.setFloatNext(data[index]);
      }
    }
  }

  public boolean isMissingXY(Variable v2, int timeIdx, int levIdx) throws InvalidRangeException {
    GribVariable pv = (GribVariable) v2.getSPobject();
    if (null == pv)
      System.out.println("HEY");
    if ((timeIdx < 0) || (timeIdx >= pv.getNTimes()))
      throw new InvalidRangeException("timeIdx=" + timeIdx);
    if ((levIdx < 0) || (levIdx >= pv.getVertNlevels()))
      throw new InvalidRangeException("levIdx=" + levIdx);
    return (null == pv.findRecord(timeIdx, levIdx));
  }

  protected abstract float[] _readData(long offset1, long offset2, int decimalScale, boolean bmsExists) throws IOException;

  public void close() throws IOException {
    raf.close();
  }

  public Object sendIospMessage(Object special) {
    if (special instanceof FmrcCoordSys)
      fmrcCoordSys = (FmrcCoordSys) special;
    return null;
  }


  public String getDetailInfo() {
    return null; // (parseInfo == null) ? "None" : parseInfo.toString();
  }


} // end GribServiceProvider

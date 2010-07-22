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
package ucar.nc2.dataset;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileFactory;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.ncml.NcMLGWriter;

// factories for remote access
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.thredds.ThreddsDataFactory;

import ucar.unidata.util.StringUtil;

import java.io.*;
import java.util.*;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.HeadMethod;
import thredds.catalog.ServiceType;

/**
 * NetcdfDataset extends the netCDF API, adding standard attribute parsing such as
 * scale and offset, and explicit support for Coordinate Systems.
 * A NetcdfDataset wraps a NetcdfFile, or is defined by an NcML document.
 * <p/>
 * <p> Be sure to close the dataset when done, best practice is to enclose in a try/finally block:
 * <pre>
 * NetcdfDataset ncd = null;
 * try {
 *   ncd = NetcdfDataset.openDataset(fileName);
 *   ...
 * } finally {
 *   ncd.close();
 * }
 * </pre>
 * <p/>
 * <p/>
 * By default NetcdfDataset is opened with all enhancements turned on. The default "enhance
 * mode" can be set through setDefaultEnhanceMode(). One can also explicitly set the enhancements you want in
 * the dataset factory methods. The enhancements are:
 * <ul>
 * <li>ScaleMissing : process scale/offset/missing attributes, and automatically convert the data.
 * <li>ScaleMissingDefer : process scale/offset/missing attributes, but dont automatically convert the data. You can call
 * VariableEnhanced.convertScaleOffsetMissing() on the data manually.
 * <li>CoordSystem : extract CoordinateSystem using the CoordSysBuilder plug-in mechanism
 * <li>ConvertEnums : automaticlly convert enum values to their corresponding Strings. If you want to do this manually, you
 * can call Variable.lookupEnumString() manually.
 * </ul>
 * <p/>
 * Automatic ScaleMissing processing has some overhead, and if you need maximum performance, but still want to use
 * scale/offset/missing value handling, open the NetcdfDataset with ScaleMissingDefer.
 * The VariableDS data type is not promoted, and the data is not converted on a read, but you can call the
 * convertScaleOffsetMissing() routines which will do the conversion on a point-by-point basis.
 *
 * @author caron
 * @see ucar.nc2.NetcdfFile
 */

/* Implementation notes.
 *  1) NetcdfDataset wraps a NetcdfFile.
       orgFile = NetcdfFile
       variables are wrapped by VariableDS, but are not reparented. VariableDS uses original variable for read.
       Groups get reparented.
    2) NcML standard
       NcML location is read in as the NetcdfDataset, then modified by the NcML
       orgFile = null
    3) NcML explicit
       NcML location is read in, then transfered to new NetcdfDataset as needed
       orgFile = file defined by NcML location
    4) NcML new
       NcML location = null
       orgFile = null
       NetcdfDataset defined only by NcML, data is set to FillValue unless explicitly defined
 */

public class NetcdfDataset extends ucar.nc2.NetcdfFile {

  /**
   * Possible enhancements for a NetcdfDataset
   */
  static public enum Enhance {
    /**
     * Calculate scale/offset and missing values, promoting data type if needed
     */
    ScaleMissing,
    /**
     * Calculate scale/offset/missing info, but dont automatically convert data.
     * Data can then be converted manually through VariableDS.convertScaleOffsetMissing().
     * Dont use both ScaleMissingDefer and ScaleMissing
     */
    ScaleMissingDefer,
    /**
     * build coordinate systems
     */
    CoordSystems,
    /**
     * convert enums to Strings
     */
    ConvertEnums
  }

  static private Set<Enhance> EnhanceAll = Collections.unmodifiableSet(EnumSet.of(Enhance.ScaleMissing, Enhance.CoordSystems, Enhance.ConvertEnums));
  static private Set<Enhance> EnhanceNone = Collections.unmodifiableSet(EnumSet.noneOf(Enhance.class));
  static private Set<Enhance> defaultEnhanceMode = EnhanceAll;
  static private Set<Enhance> coordSysEnhanceMode = null;

  static public Set<Enhance> getEnhanceAll() {
    return EnhanceAll;
  }

  static public Set<Enhance> getEnhanceNone() {
    return EnhanceNone;
  }

  static public Set<Enhance> getEnhanceDefault() {
    return defaultEnhanceMode;
  }

  /**
   * Set the default set of Enhancements to do for all subsequent dataset opens and acquires.
   *
   * @param mode the default set of Enhancements for open and acquire factory methods
   */
  static public void setDefaultEnhanceMode(Set<Enhance> mode) {
    defaultEnhanceMode = Collections.unmodifiableSet(mode);
    coordSysEnhanceMode = null;
  }

  /**
   * Get the default set of Enhancements
   *
   * @return the the default set of Enhancements for open and acquire factory methods
   */
  static public Set<Enhance> getDefaultEnhanceMode() {
    return defaultEnhanceMode;
  }

  /**
   * Get the default set of Enhancements, and add CoordSystems if not present
   *
   * @return EnhanceMode including CoordSystems
   */
  static public Set<Enhance> getCoordSysEnhanceMode() {
    if (coordSysEnhanceMode == null) {
      EnumSet<NetcdfDataset.Enhance> mode = EnumSet.copyOf(defaultEnhanceMode);
      mode.add(NetcdfDataset.Enhance.CoordSystems);
      coordSysEnhanceMode = Collections.unmodifiableSet(mode);
    }
    return coordSysEnhanceMode;
  }

  /**
   * Find the set of Enhancements that matches the String. For backwards compatibility, 'true' = All.
   *
   * @param enhanceMode : 'None', 'All', 'ScaleMissing', 'ScaleMissingDefer', 'CoordSystems', All',  case insensitive
   * @return EnumSet<EnhanceMode>
   */
  static public Set<Enhance> parseEnhanceMode(String enhanceMode) {
    if (enhanceMode == null) return null;

    Set<Enhance> mode = null;

    if (enhanceMode.equalsIgnoreCase("true") || enhanceMode.equalsIgnoreCase("All")) {
      mode = getEnhanceAll();
    } else if (enhanceMode.equalsIgnoreCase("AllDefer")) {
      mode = EnumSet.of(Enhance.ScaleMissingDefer, Enhance.CoordSystems, Enhance.ConvertEnums);
    } else if (enhanceMode.equalsIgnoreCase("ScaleMissing")) {
      mode = EnumSet.of(Enhance.ScaleMissing);
    } else if (enhanceMode.equalsIgnoreCase("ScaleMissingDefer")) {
      mode = EnumSet.of(Enhance.ScaleMissingDefer);
    } else if (enhanceMode.equalsIgnoreCase("CoordSystems")) {
      mode = EnumSet.of(Enhance.CoordSystems);
    } else if (enhanceMode.equalsIgnoreCase("ConvertEnums")) {
      mode = EnumSet.of(Enhance.ConvertEnums);
    }

    return mode;
  }

  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfDataset.class);
  static protected boolean useNaNs = true;
  static protected boolean fillValueIsMissing = true, invalidDataIsMissing = true, missingDataIsMissing = true;

  /**
   * Set whether to use NaNs for missing values, for efficiency
   *
   * @param b true if want to replace missing values with NaNs (default true)
   */
  static public void setUseNaNs(boolean b) {
    useNaNs = b;
  }

  /**
   * Get whether to use NaNs for missing values, for efficiency
   *
   * @return whether to use NaNs for missing values, for efficiency
   */
  static public boolean getUseNaNs() {
    return useNaNs;
  }

  /**
   * Set if _FillValue attribute is considered isMissing()
   *
   * @param b true if _FillValue are missing (default true)
   */
  static public void setFillValueIsMissing(boolean b) {
    fillValueIsMissing = b;
  }

  /**
   * Get if _FillValue attribute is considered isMissing()
   *
   * @return if _FillValue attribute is considered isMissing()
   */
  static public boolean getFillValueIsMissing() {
    return fillValueIsMissing;
  }

  /**
   * Set if valid_range attribute is considered isMissing()
   *
   * @param b true if valid_range are missing (default true)
   */
  static public void setInvalidDataIsMissing(boolean b) {
    invalidDataIsMissing = b;
  }

  /**
   * Get if valid_range attribute is considered isMissing()
   *
   * @return if valid_range attribute is considered isMissing()
   */
  static public boolean getInvalidDataIsMissing() {
    return invalidDataIsMissing;
  }

  /**
   * Set if missing_data attribute is considered isMissing()
   *
   * @param b true if missing_data are missing (default true)
   */
  static public void setMissingDataIsMissing(boolean b) {
    missingDataIsMissing = b;
  }

  /**
   * Get if missing_data attribute is considered isMissing()
   *
   * @return if missing_data attribute is considered isMissing()
   */
  static public boolean getMissingDataIsMissing() {
    return missingDataIsMissing;
  }

  ////////////////////////////////////////////////////////////////////////////////////

  static private ucar.nc2.util.cache.FileCache fileCache = null;
  static private ucar.nc2.util.cache.FileFactory defaultNetcdfFileFactory = null;

  /**
   * Enable file caching. call this before calling acquireFile().
   * When application terminates, call NetcdfDataset.shutdown().
   *
   * @param minElementsInMemory keep this number in the cache
   * @param maxElementsInMemory trigger a cleanup if it goes over this number.
   * @param period              (secs) do periodic cleanups every this number of seconds.
   */
  static public void initNetcdfFileCache(int minElementsInMemory, int maxElementsInMemory, int period) {
    fileCache = new ucar.nc2.util.cache.FileCache("NetcdfFileCache ", minElementsInMemory, maxElementsInMemory, -1, period);
    defaultNetcdfFileFactory = new MyNetcdfFileFactory();
  }

  /**
   * Enable file caching. call this before calling acquireFile().
   * When application terminates, call NetcdfDataset.shutdown().
   *
   * @param minElementsInMemory keep this number in the cache
   * @param maxElementsInMemory trigger a cleanup if it goes over this number.
   * @param hardLimit           if > 0, never allow more than this many elements. This causes a cleanup to be done in the calling thread.
   * @param period              (secs) do periodic cleanups every this number of seconds.
   */
  static public void initNetcdfFileCache(int minElementsInMemory, int maxElementsInMemory, int hardLimit, int period) {
    fileCache = new ucar.nc2.util.cache.FileCache("NetcdfFileCache ", minElementsInMemory, maxElementsInMemory, hardLimit, period);
    defaultNetcdfFileFactory = new MyNetcdfFileFactory();
  }

  static public void disableNetcdfFileCache() {
    if (null != fileCache) fileCache.disable();
    fileCache = null;
    shutdown();
  }

  // no state, so a singleton is ok
  static private class MyNetcdfFileFactory implements ucar.nc2.util.cache.FileFactory {
    public NetcdfFile open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      return openFile(location, buffer_size, cancelTask, iospMessage);
    }
  }

  /**
   * Call when application exits, if you have previously called initNetcdfFileCache.
   * This shuts down any background threads in order to get a clean process shutdown.
   */
  static public void shutdown() {
    FileCache.shutdown();
  }

  /**
   * Get the File Cache
   *
   * @return File Cache or null if not enabled.
   */
  static public ucar.nc2.util.cache.FileCache getNetcdfFileCache() {
    return fileCache;
  }

  ////////////////////////////////////////////////////////////////////////////////////

  /**
   * Make NetcdfFile into NetcdfDataset with given enhance mode
   *
   * @param ncfile      wrap this
   * @param enhanceMode using this enhance mode (may be null)
   * @return NetcdfDataset wrapping the given ncfile
   * @throws IOException on io error
   */
  static public NetcdfDataset wrap(NetcdfFile ncfile, Set<Enhance> enhanceMode) throws IOException {
    NetcdfDataset ncd;
    if (ncfile instanceof NetcdfDataset) {
      ncd = (NetcdfDataset) ncfile;
      ncd.enhance(enhanceMode);
    } else {
      ncd = new NetcdfDataset(ncfile, enhanceMode);
    }
    return ncd;
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location location of file
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  static public NetcdfDataset openDataset(String location) throws IOException {
    return openDataset(location, true, null);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location   location of file
   * @param enhance    if true, process scale/offset/missing and add Coordinate Systems
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  static public NetcdfDataset openDataset(String location, boolean enhance, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return openDataset(location, enhance, -1, cancelTask, null);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location    location of file
   * @param enhance     if true, use defaultEnhanceMode, else no enhancements
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param spiObject   sent to iosp.setSpecial() if not null
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  static public NetcdfDataset openDataset(String location, boolean enhance, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
    return openDataset(location, enhance ? defaultEnhanceMode : null, buffer_size, cancelTask, spiObject);
  }

  /**
   * Factory method for opening a dataset through the netCDF API, and identifying its coordinate variables.
   *
   * @param location    location of file
   * @param enhanceMode set of enhancements. If null, then none
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param spiObject   sent to iosp.setSpecial() if not null
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  static public NetcdfDataset openDataset(String location, Set<Enhance> enhanceMode, int buffer_size,
                                          ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
    // do not acquire
    NetcdfFile ncfile = openOrAcquireFile(null, null, null, location, buffer_size, cancelTask, spiObject);
    NetcdfDataset ds;
    if (ncfile instanceof NetcdfDataset) {
      ds = (NetcdfDataset) ncfile;
      enhance(ds, enhanceMode, cancelTask); // enhance "in place", ie modify the NetcdfDataset
    } else {
      ds = new NetcdfDataset(ncfile, enhanceMode); // enhance when wrapping
    }

    return ds;
  }

  /*
   * Enhancement use cases
   *  1. open NetcdfDataset(enhance).
   *  2. NcML - must create the NetcdfDataset, and enhance when its done.
   *
   * Enhance mode is set when
   *   1) the NetcdfDataset is opened
   *   2) enhance(EnumSet<Enhance> mode) is called.
   *
   * Possible remove all direct access to Variable.enhance
   */
  static private CoordSysBuilderIF enhance(NetcdfDataset ds, Set<Enhance> mode, CancelTask cancelTask) throws IOException {
    //if (ds.isEnhanceProcessed) return;
    if (mode == null) return null;

     // CoordSysBuilder may enhance dataset: add new variables, attributes, etc
    CoordSysBuilderIF builder = null;
    if (mode.contains(Enhance.CoordSystems) && !ds.enhanceMode.contains(Enhance.CoordSystems)) {
      builder = ucar.nc2.dataset.CoordSysBuilder.factory(ds, cancelTask);
      builder.augmentDataset( ds, cancelTask);
      ds.convUsed = builder.getConventionUsed();
    }

    // now enhance scale/offset, using augmented dataset
    if (mode.contains(Enhance.ConvertEnums) || mode.contains(Enhance.ScaleMissing) || mode.contains(Enhance.ScaleMissingDefer)) {
      for (Variable v : ds.getVariables()) {
        VariableEnhanced ve = (VariableEnhanced) v;
        ve.enhance(mode);
        if ((cancelTask != null) && cancelTask.isCancel()) return null;
      }
    }

    // now find coord systems which may change some Variables to axes, etc
    if (builder != null) {
       builder.buildCoordinateSystems(ds);
    }

    ds.finish(); // recalc the global lists
    ds.enhanceMode.addAll(mode);

    return builder;
  }

  /**
   * Same as openDataset, but file is acquired through the File Cache, with defaultEnhanceMode.
   * You still close with NetcdfDataset.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param location   location of file, passed to FileFactory
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  static public NetcdfDataset acquireDataset(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return acquireDataset(null, location, defaultEnhanceMode, -1, cancelTask, null);
  }

  static public NetcdfDataset acquireDataset(String location, boolean enhance, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return acquireDataset(null, location, enhance ? defaultEnhanceMode : null, -1, cancelTask, null);
  }

  /**
   * Same as openDataset, but file is acquired through the File Cache.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   * You still close with NetcdfDataset.close(), the release is handled automatically.
   *
   * @param fac         if not null, use this factory if the file is not in the cache. If null, use the default factory.
   * @param location    location of file, passed to FileFactory
   * @param enhanceMode how to enhance. if null, then no enhancement
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param iospMessage sent to iosp.setSpecial() if not null
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  static public NetcdfDataset acquireDataset(FileFactory fac, String location, Set<Enhance> enhanceMode, int buffer_size,
                                             ucar.nc2.util.CancelTask cancelTask, Object iospMessage) throws IOException {

    // caching not turned on
    if (fileCache == null) {
      if (fac == null)
        return openDataset(location, enhanceMode, buffer_size, cancelTask, iospMessage);
      else
        // must use the factory if there is one
        return (NetcdfDataset) fac.open(location, buffer_size, cancelTask, iospMessage);
    }

    if (fac != null)
      return (NetcdfDataset) openOrAcquireFile(fileCache, fac, null, location, buffer_size, cancelTask, iospMessage);

    fac = new MyNetcdfDatasetFactory(location, enhanceMode);
    return (NetcdfDataset) openOrAcquireFile(fileCache, fac, fac.hashCode(), location, buffer_size, cancelTask, iospMessage);
  }

  static private class MyNetcdfDatasetFactory implements ucar.nc2.util.cache.FileFactory {
    String location;
    EnumSet<Enhance> enhanceMode;

    MyNetcdfDatasetFactory(String location, Set<Enhance> enhanceMode) {
      this.location = location;
      this.enhanceMode = (enhanceMode == null) ? EnumSet.noneOf(Enhance.class) : EnumSet.copyOf(enhanceMode);
    }

    public NetcdfFile open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      return openDataset(location, enhanceMode, buffer_size, cancelTask, iospMessage);
    }

    public int hashCode() { // unique key, must be different than a plain NetcdfFile, deal with possible different enhancing
      int result = location.hashCode();
      result += 37 * result + enhanceMode.hashCode();
      return result;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Factory method for opening a NetcdfFile through the netCDF API.
   *
   * @param location   location of dataset.
   * @param cancelTask use to allow task to be cancelled; may be null.
   * @return NetcdfFile object
   * @throws java.io.IOException on read error
   */
  public static NetcdfFile openFile(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return openOrAcquireFile(null, null, null, location, -1, cancelTask, null);
  }


  /**
   * Factory method for opening a NetcdfFile through the netCDF API. May be any kind of file that
   * can be read through the netCDF API, including OpenDAP and NcML.
   * <p/>
   * <p> This does not necessarily return a NetcdfDataset, or enhance the dataset; use NetcdfDataset.openDataset() method for that.
   *
   * @param location    location of dataset. This may be a
   *                    <ol>
   *                    <li>local filename (with a file: prefix or no prefix) for netCDF (version 3), hdf5 files, or any file type
   *                    registered with NetcdfFile.registerIOProvider().
   *                    <li>OpenDAP dataset URL (with a dods: or http: prefix).
   *                    <li>NcML file or URL if the location ends with ".xml" or ".ncml"
   *                    <li>NetCDF file through an HTTP server (http: prefix)
   *                    <li>thredds dataset (thredds: prefix), see ThreddsDataFactory.openDataset(String location, ...));
   *                    </ol>
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param spiObject   sent to iosp.setSpecial() if not null
   * @return NetcdfFile object
   * @throws java.io.IOException on read error
   */
  public static NetcdfFile openFile(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
    return openOrAcquireFile(null, null, null, location, buffer_size, cancelTask, spiObject);
  }

  /**
   * Same as openFile, but file is acquired through the File Cache.
   * You still close with NetcdfFile.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param location   location of file, passed to FileFactory
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfFile object
   * @throws java.io.IOException on read error
   */
  static public NetcdfFile acquireFile(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return acquireFile(null, null, location, -1, cancelTask, null);
  }

  /**
   * Same as openFile, but file is acquired through the File Cache.
   * You still close with NetcdfFile.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param factory     if not null, use this factory to read the file. If null, use the default factory.
   * @param hashKey     if not null, use as the cache key, else use the location
   * @param location    location of file, passed to FileFactory
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param spiObject   sent to iosp.setSpecial(); may be null
   * @return NetcdfFile object
   * @throws java.io.IOException on read error
   */
  static public NetcdfFile acquireFile(ucar.nc2.util.cache.FileFactory factory, Object hashKey,
                                       String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {

    // must use the factory if there is one
    if ((fileCache == null) && (factory != null)) {
      return (NetcdfFile) factory.open(location, buffer_size, cancelTask, spiObject);
    }

    return openOrAcquireFile(fileCache, factory, hashKey, location, buffer_size, cancelTask, spiObject);
  }

  /**
   * Open or acquire a NetcdfFile.
   *
   * @param cache       if not null, acquire through this NetcdfFileCache, otherwise simply open
   * @param factory     if not null, use this factory if the file is not in the cache. If null, use the default factory.
   * @param hashKey     if not null, use as the cache key, else use the location
   * @param location    location of file
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param spiObject   sent to iosp.setSpecial() if not null
   * @return NetcdfFile object
   * @throws java.io.IOException on read error
   */
  static private NetcdfFile openOrAcquireFile(FileCache cache, FileFactory factory, Object hashKey,
                                              String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {

    if (location == null)
      throw new IOException("NetcdfDataset.openFile: location is null");
    location = location.trim();
    location = StringUtil.replace(location, '\\', "/");

    if (location.startsWith("dods:")) {
      return acquireDODS(cache, factory, hashKey, location, buffer_size, cancelTask, spiObject);  // open through DODS

    } else if (location.startsWith(CdmRemote.SCHEME)) {
      return acquireRemote(cache, factory, hashKey, location, buffer_size, cancelTask, spiObject);  // open through ncstream

    } else if (location.startsWith(ThreddsDataFactory.SCHEME)) {
      Formatter log = new Formatter();
      ThreddsDataFactory tdf = new ThreddsDataFactory();
      NetcdfFile ncfile = tdf.openDataset(location, false, cancelTask, log); // LOOK acquire ??
      if (ncfile == null)
        throw new IOException(log.toString());
      return ncfile;

    } else if (location.endsWith(".xml") || location.endsWith(".ncml")) { //open as a NetcdfDataset through NcML
      if (!location.startsWith("http:") && !location.startsWith("file:"))
        location = "file:" + location;
      return acquireNcml(cache, factory, hashKey, location, buffer_size, cancelTask, spiObject);

    } else if (location.startsWith("http:")) {
      ServiceType stype = disambiguateHttp(location);
      if (stype == ServiceType.OPENDAP)
        return acquireDODS(cache, factory, hashKey, location, buffer_size, cancelTask, spiObject); // try as a dods file
      else if (stype == ServiceType.CdmRemote)
        return acquireRemote(cache, factory, hashKey, location, buffer_size, cancelTask, spiObject);  // open through CDM remote
      // else fall through for HttpService
    }

    if (cache != null) {
      if (factory == null) factory = defaultNetcdfFileFactory;
      return (NetcdfFile) cache.acquire(factory, hashKey, location, buffer_size, cancelTask, spiObject);
    } else {
      return NetcdfFile.open(location, buffer_size, cancelTask, spiObject);
    }
  }

  /*
   * 'Bare' urls starting with 'http:' can be of the following service type:
   * <ol>
   * <li> opendap
   * <li> remote ncstream
   * <li> http byte range
   * </ol>
   * Do a HEAD call on the URL. 
   * Look for the header "Content-Description" = "ncstream" or "dods".
   */
  static private ServiceType disambiguateHttp(String location) throws IOException {
    initHttpClient();

    // have to do dods first
    ServiceType result = checkIfDods(location);
    if (result != null)
      return result;

    HttpMethod method = null;
    try {
      method = new HeadMethod(location);
      method.setFollowRedirects(true);
      int statusCode = httpClient.executeMethod(method);
      if (statusCode >= 300) {
        if (statusCode == 401)
          throw new IOException("Unauthorized to open dataset " + location);
        else
          throw new IOException(location + " is not a valid URL, return status=" + statusCode);
      }

      Header h = method.getResponseHeader("Content-Description");
      if ((h != null) && (h.getValue() != null)) {
        String v = h.getValue();
        if (v.equalsIgnoreCase("ncstream"))
          return ServiceType.CdmRemote;
      }

      return null;

    } finally {
      if (method != null) method.releaseConnection();
    }
  }

  // not sure what other opendap servers do, so fall back on check for dds
  static private ServiceType checkIfDods(String location) throws IOException {
    HttpMethod method = null;
    try {
      method = new HeadMethod(location + ".dds");
      method.setFollowRedirects(true);
      int status = httpClient.executeMethod(method);
      if (status == 200) {
        Header h = method.getResponseHeader("Content-Description");
        if ((h != null) && (h.getValue() != null)) {
          String v = h.getValue();
          if (v.equalsIgnoreCase("dods-dds") || v.equalsIgnoreCase("dods_dds"))
            return ServiceType.OPENDAP;
          else
            throw new IOException("OPeNDAP Server Error= " + method.getResponseBodyAsString());
        }
      }
      if (status == 401)
        throw new IOException("Unauthorized to open dataset " + location);

      // not dods
      return null;

    } finally {
      if (method != null) method.releaseConnection();
    }
  }

  /**
   * Set the HttpClient object - so that a single, shared instance is used within the application.
   *
   * @param client the HttpClient object
   */
  static public void setHttpClient(HttpClient client) {
    httpClient = client;
  }

  private static HttpClient httpClient = null;

  private static synchronized void initHttpClient() {
    if (httpClient != null) return;
    MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
    httpClient = new HttpClient(connectionManager);
  }

  static private NetcdfFile acquireDODS(FileCache cache, FileFactory factory, Object hashKey,
                                        String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
    if (cache == null) return new DODSNetcdfFile(location, cancelTask);

    if (factory == null) factory = new DodsFactory();
    return (NetcdfFile) cache.acquire(factory, hashKey, location, buffer_size, cancelTask, spiObject);
  }

  static private class DodsFactory implements FileFactory {
    public NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
      return new DODSNetcdfFile(location, cancelTask);
    }
  }

  static private NetcdfFile acquireNcml(FileCache cache, FileFactory factory, Object hashKey,
                                        String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
    if (cache == null) return NcMLReader.readNcML(location, cancelTask);

    if (factory == null) factory = new NcMLFactory();
    return (NetcdfFile) cache.acquire(factory, hashKey, location, buffer_size, cancelTask, spiObject);
  }

  static private class NcMLFactory implements FileFactory {
    public NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
      return NcMLReader.readNcML(location, cancelTask);
    }
  }

  static private NetcdfFile acquireRemote(FileCache cache, FileFactory factory, Object hashKey,
                                          String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
    if (cache == null) return new CdmRemote(location);

    if (factory == null) factory = new RemoteFactory();
    return (NetcdfFile) cache.acquire(factory, hashKey, location, buffer_size, cancelTask, spiObject);
  }

  static private class RemoteFactory implements FileFactory {
    public NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
      return new CdmRemote(location);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////
  private NetcdfFile orgFile = null;

  private List<CoordinateSystem> coordSys = new ArrayList<CoordinateSystem>();
  private List<CoordinateAxis> coordAxes = new ArrayList<CoordinateAxis>();
  private List<CoordinateTransform> coordTransforms = new ArrayList<CoordinateTransform>();
  private String convUsed;

  private EnumSet<Enhance> enhanceMode = EnumSet.noneOf(Enhance.class); // enhancement mode for this specific dataset

  // If its an aggregation
  private ucar.nc2.ncml.Aggregation agg = null; // used to close underlying files

  /**
   * If its an NcML aggregation, it has an Aggregation object associated.
   * This is public for use by NcmlWriter.
   *
   * @return Aggregation or null
   */
  public ucar.nc2.ncml.Aggregation getAggregation() {
    return agg;
  }

  /**
   * Set the Aggregation object associated with this NcML dataset
   *
   * @param agg the Aggregation object
   */
  public void setAggregation(ucar.nc2.ncml.Aggregation agg) {
    this.agg = agg;
  }

  /**
   * Get the list of all CoordinateSystem objects used by this dataset.
   *
   * @return list of type CoordinateSystem; may be empty, not null.
   */
  public List<CoordinateSystem> getCoordinateSystems() {
    return coordSys;
  }

  /**
   * Get conventions used to analyse coordinate systems.
   *
   * @return conventions used to analyse coordinate systems
   */
  public String getConventionUsed() {
    return convUsed;
  }

  /**
   * Get the current state of dataset enhancement.
   *
   * @return the current state of dataset enhancement.
   */
  public EnumSet<Enhance> getEnhanceMode() {
    return enhanceMode;
  }

  /**
   * Get the list of all CoordinateTransform objects used by this dataset.
   *
   * @return list of type CoordinateTransform; may be empty, not null.
   */
  public List<CoordinateTransform> getCoordinateTransforms() {
    return coordTransforms;
  }

  /**
   * Get the list of all CoordinateAxis objects used by this dataset.
   *
   * @return list of type CoordinateAxis; may be empty, not null.
   */
  public List<CoordinateAxis> getCoordinateAxes() {
    return coordAxes;
  }

  /**
   * Clear Coordinate System metadata, to allow them to be redone
   */
  public void clearCoordinateSystems() {
    coordSys = new ArrayList<CoordinateSystem>();
    coordAxes = new ArrayList<CoordinateAxis>();
    coordTransforms = new ArrayList<CoordinateTransform>();

    for (Variable v : getVariables()) {
      VariableEnhanced ve = (VariableEnhanced) v;
      ve.clearCoordinateSystems(); // ??
    }

    enhanceMode.remove(Enhance.CoordSystems);
  }

  /*
   * Set whether Coordinate System metadata has been added.
   *
   * @param coordSysWereAdded set to this value
   *
  public void setCoordSysWereAdded(boolean coordSysWereAdded) {
    this.coordSysWereAdded = coordSysWereAdded;
  } */

  /**
   * Retrieve the CoordinateAxis with the specified Axis Type.
   *
   * @param type axis type
   * @return the first CoordinateAxis that has that type, or null if not found
   */
  public CoordinateAxis findCoordinateAxis(AxisType type) {
    if (type == null) return null;
    for (CoordinateAxis v : coordAxes) {
      if (type == v.getAxisType())
        return v;
    }
    return null;
  }

  /**
   * Retrieve the CoordinateAxis with the specified type.
   *
   * @param fullName full name of the coordinate axis
   * @return the CoordinateAxis, or null if not found
   */
  public CoordinateAxis findCoordinateAxis(String fullName) {
    if (fullName == null) return null;
    for (CoordinateAxis v : coordAxes) {
      String n = v.getName();
      if (fullName.equals(n)) // LOOK WRONG must be escaped !!
        return v;
    }
    return null;
  }

  /**
   * Retrieve the CoordinateSystem with the specified name.
   *
   * @param name String which identifies the desired CoordinateSystem
   * @return the CoordinateSystem, or null if not found
   */
  public CoordinateSystem findCoordinateSystem(String name) {
    if (name == null) return null;
    for (CoordinateSystem v : coordSys) {
      if (name.equals(v.getName()))
        return v;
    }
    return null;
  }

  /**
   * Retrieve the CoordinateTransform with the specified name.
   *
   * @param name String which identifies the desired CoordinateSystem
   * @return the CoordinateSystem, or null if not found
   */
  public CoordinateTransform findCoordinateTransform(String name) {
    if (name == null) return null;
    for (CoordinateTransform v : coordTransforms) {
      if (name.equals(v.getName()))
        return v;
    }
    return null;
  }

  /**
   * Close all resources (files, sockets, etc) associated with this dataset.
   * If the underlying file was acquired, it will be released, otherwise closed.
   */
  @Override
  public synchronized void close() throws java.io.IOException {
    if (agg != null) agg.persistWrite(); // LOOK  maybe only on real close ??

    if (cache != null) {
      unlocked = true;
      cache.release(this);

    } else {
      if (agg != null) agg.close();
      agg = null;
      if (orgFile != null) orgFile.close();
      orgFile = null;
    }

  }

  /*
  @Override
  protected void finalize() throws Throwable {
    if (!isClosed) {
      try {
        if (agg != null) agg.close();
        agg = null;
        if (orgFile != null) orgFile.close();
        orgFile = null;
      } finally {
        super.finalize();
      }
      isClosed = true;
    }
  }  */

  /**
   * Check if file has changed, and reread metadata if needed.
   * All previous object references (variables, dimensions, etc) may become invalid - you must re-obtain.
   *
   * @return true if file was changed.
   * @throws IOException
   */
  public boolean sync() throws IOException {
    unlocked = false;

    if (agg != null)
      return agg.sync();

    if (orgFile != null) {
      if (orgFile.sync()) {
        // start over again
        this.location = orgFile.getLocation();
        this.id = orgFile.getId();
        this.title = orgFile.getTitle();

        // build global lists
        empty();
        convertGroup(getRootGroup(), orgFile.getRootGroup());
        finish();

        // redo enhance
        EnumSet<Enhance> saveMode = this.enhanceMode;
        this.enhanceMode = EnumSet.noneOf(Enhance.class);
        enhance(this, saveMode, null);
        return true;
      }
    }

    return false;
  }

  @Override
  public void empty() {
    super.empty();
    coordSys = new ArrayList<CoordinateSystem>();
    coordAxes = new ArrayList<CoordinateAxis>();
    coordTransforms = new ArrayList<CoordinateTransform>();
    convUsed = null;
  }

  public boolean syncExtend() throws IOException {
    unlocked = false;

    if (agg != null)
      return agg.syncExtend();

    // synch orgFile if it has an unlimited dimension
    if (orgFile != null) {
      boolean wasExtended = orgFile.syncExtend();

      // propagate changes. LOOK rather ad-hoc
      if (wasExtended) {
        Dimension ndim = orgFile.getUnlimitedDimension();
        int newLength = ndim.getLength();

        Dimension udim = getUnlimitedDimension();
        udim.setLength(newLength);

        for (Variable v : getVariables()) {
          if (v.isUnlimited()) // set it in all of the record variables
            v.setDimensions(v.getDimensions());
        }
        return true;
      }
    }

    return false;
  }

  /**
   * Write the NcML representation.
   *
   * @param os  write to this Output Stream.
   * @param uri use this for the uri attribute; if null use getLocation().
   * @throws IOException
   */
  public void writeNcML(java.io.OutputStream os, String uri) throws IOException {
    new NcMLWriter().writeXML(this, os, uri);
  }

  /**
   * Write the NcML-G representation.
   *
   * @param os         write to this Output Stream.
   * @param showCoords shoe the values of coordinate axes
   * @param uri        use this for the uri attribute; if null use getLocation().
   * @throws IOException on write error
   */
  public void writeNcMLG(java.io.OutputStream os, boolean showCoords, String uri) throws IOException {
    new NcMLGWriter().writeXML(this, os, showCoords, uri);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Transform a NetcdfFile into a NetcdfDataset.
   * You must not use the underlying NetcdfFile after this call.
   *
   * @param ncfile NetcdfFile to transform.
   * @throws java.io.IOException on read error
   */
  public NetcdfDataset(NetcdfFile ncfile) throws IOException {
    this(ncfile, defaultEnhanceMode);
  }

  /**
   * Transform a NetcdfFile into a NetcdfDataset, optionally enhance it.
   * You must not use the original NetcdfFile after this call.
   *
   * @param ncfile  NetcdfFile to transform, do not use independently after this.
   * @param enhance if true, enhance with defaultEnhanceMode
   * @throws java.io.IOException on read error
   */
  public NetcdfDataset(NetcdfFile ncfile, boolean enhance) throws IOException {
    this(ncfile, enhance ? defaultEnhanceMode : null);
  }

  /**
   * Transform a NetcdfFile into a NetcdfDataset, optionally enhance it.
   * You must not use the original NetcdfFile after this call.
   *
   * @param ncfile NetcdfFile to transform, do not use independently after this.
   * @param mode   set of enhance modes. If null, then none
   * @throws java.io.IOException on read error
   */
  public NetcdfDataset(NetcdfFile ncfile, Set<Enhance> mode) throws IOException {
    super(ncfile);

    this.orgFile = ncfile;
    this.spi = null; // has a orgFile, not an iosp
    convertGroup(getRootGroup(), ncfile.getRootGroup());
    finish(); // build global lists

    enhance(this, mode, null);
  }

  private void convertGroup(Group g, Group from) {
    for (EnumTypedef et : from.getEnumTypedefs())
      g.addEnumeration(et);

    for (Dimension d : from.getDimensions())
      g.addDimension( new Dimension(d.getName(), d));

    for (Attribute a : from.getAttributes())
      g.addAttribute(a);

    for (Variable v : from.getVariables())
      g.addVariable(convertVariable(g, v));

    for (Group nested : from.getGroups()) {
      Group nnested = new Group(this, g, nested.getShortName());
      g.addGroup(nnested);
      convertGroup(nnested, nested);
    }
  }

  private Variable convertVariable(Group g, Variable v) {
    Variable newVar;
    if (v instanceof Sequence) {
      newVar = new SequenceDS(g, (Sequence) v);
    } else if (v instanceof Structure) {
        newVar = new StructureDS(g, (Structure) v);
    } else {
      newVar = new VariableDS(g, v, false); // enhancement done later
    }
    return newVar;
  }

  //////////////////////////////////////

  @Override
  protected Boolean makeRecordStructure() {
    if (this.orgFile == null) return false;

    Boolean hasRecord = (Boolean) this.orgFile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);
    if ((hasRecord == null) || !hasRecord) return false;

    Variable orgV = this.orgFile.getRootGroup().findVariable("record");
    if ((orgV == null) || !(orgV instanceof Structure)) return false;
    Structure orgStructure = (Structure) orgV;

    Dimension udim = getUnlimitedDimension();
    if (udim == null) return false;

    Group root = getRootGroup();
    StructureDS newStructure = new StructureDS(this, root, null, "record", udim.getName(), null, null);
    newStructure.setOriginalVariable(orgStructure);

    for (Variable v : getVariables()) {
      if (!v.isUnlimited()) continue;
      VariableDS memberV;

      try {
        memberV = (VariableDS) v.slice(0, 0); // set unlimited dimension to 0
      } catch (InvalidRangeException e) {
        log.error("Cant slice variable " + v);
        return false;
      }
      memberV.setParentStructure(newStructure); // reparent
      /* memberV.createNewCache(); // decouple caching
      //orgV = orgStructure.findVariable(v.getShortName());
      //if (orgV != null)
      //  memberV.setOriginalVariable(orgV);

      // remove record dimension
      List<Dimension> dims = new ArrayList<Dimension>(v.getDimensions());
      dims.remove(0);
      memberV.setDimensions(dims); */

      newStructure.addMemberVariable(memberV);
    }

    root.addVariable(newStructure);
    finish();

    //if (isEnhancedScaleOffset())
    //  newStructure.enhance();
    return true;
  }

  /**
   * Sort Variables, CoordAxes by name.
   */
  public void sort() {
    Collections.sort(variables, new VariableComparator());
    Collections.sort(coordAxes, new VariableComparator());
  }

  // sort by coord sys, then name
  private class VariableComparator implements java.util.Comparator {
    public int compare(Object o1, Object o2) {
      VariableEnhanced v1 = (VariableEnhanced) o1;
      VariableEnhanced v2 = (VariableEnhanced) o2;
      List list1 = v1.getCoordinateSystems();
      String cs1 = (list1.size() > 0) ? ((CoordinateSystem) list1.get(0)).getName() : "";
      List list2 = v2.getCoordinateSystems();
      String cs2 = (list2.size() > 0) ? ((CoordinateSystem) list2.get(0)).getName() : "";

      if (cs2.equals(cs1))
        return v1.getName().compareToIgnoreCase(v2.getName());
      else
        return cs1.compareToIgnoreCase(cs2);
    }

    public boolean equals(Object obj) {
      return (this == obj);
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  // used by NcMLReader for NcML without a referenced dataset

  /**
   * No-arg Constructor
   */
  public NetcdfDataset() {
  }

  /**
   * A NetcdfDataset usually wraps a NetcdfFile, where the actual I/O happens.
   * This is called the "referenced file". CAUTION : this may have been modified in ways that make it
   * unsuitable for general use.
   *
   * @return underlying NetcdfFile, or null if none.
   */
  public NetcdfFile getReferencedFile() {
    return orgFile;
  }

  @Override
  public IOServiceProvider getIosp() {
    return (orgFile == null) ? null : orgFile.getIosp();
  }

  /**
   * Set underlying file. CAUTION - normally only done through the constructor.
   *
   * @param ncfile underlying "referenced file"
   */
  public void setReferencedFile(NetcdfFile ncfile) {
    orgFile = ncfile;
  }

  protected String toStringDebug(Object o) {
    return "";
  }

  /* public boolean isEnhanceProcessed() {
    return isEnhanceProcessed;
  }

  public void setEnhanceProcessed(boolean enhanceProcessed) {
    isEnhanceProcessed = enhanceProcessed;
  } */

///////////////////////////////////////////////////////////////////////////////////
  // constructor methods

  /**
   * Add a CoordinateSystem to the dataset.
   *
   * @param cs add this CoordinateSystem to the dataset
   */
  public void addCoordinateSystem(CoordinateSystem cs) {
    coordSys.add(cs);
  }

  /**
   * Add a CoordinateTransform to the dataset.
   *
   * @param ct add this CoordinateTransform to the dataset
   */
  public void addCoordinateTransform(CoordinateTransform ct) {
    if (!coordTransforms.contains(ct))
      coordTransforms.add(ct);
  }

  /**
   * Add a CoordinateAxis to the dataset, by turning the VariableDS into a CoordinateAxis (if needed).
   * Also adds it to the list of variables. Replaces any existing Variable and CoordinateAxis with the same name.
   *
   * @param v make this VariableDS into a CoordinateAxis
   * @return the CoordinateAxis
   */
  public CoordinateAxis addCoordinateAxis(VariableDS v) {
    if (v == null) return null;
    CoordinateAxis oldVar = findCoordinateAxis(v.getName());
    if (oldVar != null)
      coordAxes.remove(oldVar);

    CoordinateAxis ca = (v instanceof CoordinateAxis) ? (CoordinateAxis) v : CoordinateAxis.factory(this, v);
    coordAxes.add(ca);

    if (v.isMemberOfStructure()) {
      Structure parentOrg = v.getParentStructure();  // gotta be careful to get the wrapping parent
      Structure parent = (Structure) findVariable(parentOrg.getNameEscaped());
      parent.replaceMemberVariable(ca);

    } else {
      removeVariable(v.getParentGroup(), v.getShortName()); // remove by short name if it exists
      addVariable(ca.getParentGroup(), ca);
    }

    return ca;
  }

  @Override
  public Variable addVariable(Group g, Variable v) {
    if (!(v instanceof VariableDS) && !(v instanceof StructureDS))
      throw new IllegalArgumentException("NetcdfDataset variables must be VariableEnhanced objects");
    return super.addVariable(g, v);
  }

  /**
   * recalc enhancement info - use default enhance mode
   *
   * @throws java.io.IOException on error
   * @return the CoordSysBuilder used, for debugging. do not modify or retain a reference
   */
  public CoordSysBuilderIF enhance() throws IOException {
    return enhance(this, defaultEnhanceMode, null);
  }

  /**
   * recalc enhancement info
   *
   * @param mode how to enhance
   * @throws java.io.IOException on error
   */
  public void enhance(Set<Enhance> mode) throws IOException {
    enhance(this, mode, null);
  }
  ///////////////////////////////////////////////////////////////////////
  // setting variable data values

  /**
   * Generate the list of values from a starting value and an increment.
   * Will reshape to variable if needed.
   *
   * @param v     for this variable
   * @param npts  number of values, must = v.getSize()
   * @param start starting value
   * @param incr  increment
   */
  public void setValues(Variable v, int npts, double start, double incr) {
    if (npts != v.getSize())
      throw new IllegalArgumentException("bad npts = " + npts + " should be " + v.getSize());
    Array data = Array.makeArray(v.getDataType(), npts, start, incr);
    if (v.getRank() != 1)
      data = data.reshape(v.getShape());
    v.setCachedData(data, true);
  }

  /**
   * Set the data values from a list of Strings.
   *
   * @param v      for this variable
   * @param values list of Strings
   * @throws IllegalArgumentException if values array not correct size, or values wont parse to the correct type
   */
  public void setValues(Variable v, List<String> values) throws IllegalArgumentException {
    Array data = Array.makeArray(v.getDataType(), values);

    if (data.getSize() != v.getSize())
      throw new IllegalArgumentException("Incorrect number of values specified for the Variable " + v.getName() +
              " needed= " + v.getSize() + " given=" + data.getSize());

    if (v.getRank() != 1) // dont have to reshape for rank 1
      data = data.reshape(v.getShape());

    v.setCachedData(data, true);
  }

  /**
   * Make a 1D array from a list of strings.
   *
   * @param dtype        data type of the array.
   * @param stringValues list of strings.
   * @return resulting 1D array.
   * @throws NumberFormatException if string values not parssable to specified data type
   * @deprecated use Array#makeArray directly
   */
  static public Array makeArray(DataType dtype, List<String> stringValues) throws NumberFormatException {
    return Array.makeArray(dtype, stringValues);
  }

  ////////////////////////////////////////////////////////////////////
  // debugging

  // private NetcdfDatasetInfo info = null;

  /**
   * Show debug / underlying implementation details
   */
  @Override
  public void getDetailInfo(Formatter f) {
    f.format("NetcdfDataset location= %s%n", getLocation());
    f.format("  title= %s%n", getTitle());
    f.format("  id= %s%n", getId());
    f.format("  fileType= %s%n", getFileTypeId());
    f.format("  fileDesc= %s%n", getFileTypeDescription());

    f.format("  class= %s%n", getClass().getName());

    if (agg == null) {
      f.format("  has no Aggregation element%n");
    } else {
      f.format("%nAggregation:%n");
      agg.getDetailInfo(f);
    }

    if (orgFile == null) {
      f.format("  has no referenced NetcdfFile%n");
      showCached(f);
      showProxies(f);
    } else {
      f.format("%nReferenced File:%n");
      f.format(orgFile.getDetailInfo());
    }
  }

  /*
   * Debugging: get the information from parsing
   *
   * @return NetcdfDatasetInfo object
   *
  public NetcdfDatasetInfo getInfo() {
    return new NetcdfDatasetInfo(this);
  } */

  void dumpClasses(Group g, PrintStream out) {

    out.println("Dimensions:");
    for (Dimension ds : g.getDimensions()) {
      out.println("  " + ds.getName() + " " + ds.getClass().getName());
    }

    out.println("Atributes:");
    for (Attribute a : g.getAttributes()) {
      out.println("  " + a.getName() + " " + a.getClass().getName());
    }

    out.println("Variables:");
    dumpVariables(g.getVariables(), out);

    out.println("Groups:");
    for (Group nested : g.getGroups()) {
      out.println("  " + nested.getName() + " " + nested.getClass().getName());
      dumpClasses(nested, out);
    }
  }

  private void dumpVariables(List<Variable> vars, PrintStream out) {
    for (Variable v : vars) {
      out.print("  " + v.getName() + " " + v.getClass().getName()); // +" "+Integer.toHexString(v.hashCode()));
      if (v instanceof CoordinateAxis)
        out.println("  " + ((CoordinateAxis) v).getAxisType());
      else
        out.println();

      if (v instanceof Structure)
        dumpVariables(((Structure) v).getVariables(), out);
    }
  }

  /**
   * Debugging
   *
   * @param out write here
   * @param ncd info about this
   */
  public static void debugDump(PrintStream out, NetcdfDataset ncd) {
    String referencedLocation = ncd.orgFile == null ? "(null)" : ncd.orgFile.getLocation();
    out.println("\nNetcdfDataset dump = " + ncd.getLocation() + " uri= " + referencedLocation + "\n");
    ncd.dumpClasses(ncd.getRootGroup(), out);
  }

  @Override
  public String getFileTypeId() {
    if (orgFile != null) return orgFile.getFileTypeId();
    if (agg != null) return agg.getFileTypeId();
    return "N/A";
  }

  @Override
  public String getFileTypeDescription() {
    if (orgFile != null) return orgFile.getFileTypeDescription();
    if (agg != null) return agg.getFileTypeDescription();
    return "N/A";
  }

  public void check(Formatter f) {
    for (Variable v : getVariables()) {
      VariableDS vds =  (VariableDS) v;
      if (vds.getOriginalDataType() != vds.getDataType()) {
        f.format("Variable %s has type %s, org = %s%n", vds.getName(), vds.getOriginalDataType(), vds.getDataType());
      }

      if (vds.getOriginalVariable() != null) {
        Variable orgVar = vds.getOriginalVariable();
        if (orgVar.getRank() != vds.getRank())
          f.format("Variable %s has rank %d, org = %d%n", vds.getName(), vds.getRank(), orgVar.getRank());
      }


    }
  }

  /**
   * Main program - cover to ucar.nc2.FileWriter, for all files that can be read by NetcdfDataset.openFile()
   * <p><strong>ucar.nc2.dataset.NetcdfDataset -in fileIn -out fileOut
   * <p>where: <ul>
   * <li> fileIn : path of any CDM readable file
   * <li> fileOut: local pathname where netdf-3 file will be written
   * </ol>
   *
   * @param arg -in fileIn -out fileOut [-delay millisecs]
   * @throws IOException on read or write error
   */
  public static void main(String arg[]) throws IOException {
    String usage = "usage: ucar.nc2.dataset.NetcdfDataset -in <fileIn> -out <fileOut> [-isLargeFile]";
    if (arg.length < 4) {
      System.out.println(usage);
      System.exit(0);
    }

    boolean isLargeFile = false;
    String datasetIn = null, datasetOut = null;
    for (int i = 0; i < arg.length; i++) {
      String s = arg[i];
      if (s.equalsIgnoreCase("-in")) datasetIn = arg[i + 1];
      if (s.equalsIgnoreCase("-out")) datasetOut = arg[i + 1];
      if (s.equalsIgnoreCase("-isLargeFile")) isLargeFile = true;
    }
    if ((datasetIn == null) || (datasetOut == null)) {
      System.out.println(usage);
      System.exit(0);
    }

    NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDataset.openFile(datasetIn, null);
    System.out.println("Read from " + datasetIn + " write to " + datasetOut);

    NetcdfFile ncfileOut = ucar.nc2.FileWriter.writeToFile(ncfileIn, datasetOut, false, -1, isLargeFile);
    ncfileIn.close();
    ncfileOut.close();
    System.out.println("NetcdfFile written = " + ncfileOut);
  }

}
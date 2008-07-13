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
package ucar.nc2.dataset;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.stream.NetcdfRemote;
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
import java.net.*;
import java.util.*;

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
 *
 *
 * By default NetcdfDataset is opened with NetcdfDataset.EnhanceMode.All, which adds automatic scale/offset/missing value
 *   handling by VariableDS, and automatic CoordinateSystem construction by the CoordSysBuilder plugins. The default "enhance
 *   mode" can be set through setDefaultEnhanceMode(). One can also explicitly set the EnhanceMode of the dataset
 *   in the factory methods:
 *   <ul>
 *   <li>All : scale/offset/missing and CoordinateSystem
 *   <li>ScaleMissing : just scale/offset/missing
 *   <li>ScaleMissingDefer : caclulate scale/offset/missing info, but dont automatically convert
 *   <li>CoordSystem : just CoordinateSystem
 *   <li>AllDefer : ScaleMissingDefer and CoordSystem
 *   <li>None : no enhancements
 *   </ul>
 *
 * Automatic conversion has a lot of overhead, and if you need maximum performance, but still want to use
 *   scale/offset/missing value handling, open the NetcdfDataset with EnhanceMode.ScaleOffsetDefer (or AllDefer).
 *   The VariableDS data type is not promoted, and the data is not converted on a read, but you can call the
 *   convertScaleOffset() routines which will do the conversion on a point-by-point basis, or convertArray()
 *   which will convert the entire Array.
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
   * Possible enhancement modes for a NetcdfDataset
   */
  static public enum EnhanceMode {
    /** implement scale/offset and missing values, promoting data type if needed */
    ScaleMissing,
    /** caclulate scale/offset/missing params, but dont automatically convert data. dont use both ScaleMissingDefer and ScaleMissing */ 
    ScaleMissingDefer,
    /** build coordinate systems */
    CoordSystems,
    /** convert enums to Strings */
    ConvertEnums
  }

  // LOOK! need immutable
  //static private EnumSet<EnhanceMode> EnhanceNone = EnumSet.noneOf(EnhanceMode.class);
  static private EnumSet<EnhanceMode> EnhanceAll = EnumSet.of(EnhanceMode.ScaleMissing, EnhanceMode.CoordSystems, EnhanceMode.ConvertEnums);
  static public EnumSet<EnhanceMode> getEnhanceAll() { return EnumSet.copyOf( EnhanceAll); }
  static public EnumSet<EnhanceMode> getEnhanceNone() { return EnumSet.noneOf(EnhanceMode.class); }
  static protected EnumSet<EnhanceMode> defaultEnhanceMode = EnhanceAll;

  /**
   * Find the EnhanceMode that matches the String. For backwards compatibility, 'true' = All.
   * @param enhanceMode : 'None', 'All', 'ScaleMissing', 'ScaleMissingDefer', 'CoordSystems', All',  case insensitive
   * @return EnumSet<EnhanceMode> 
   */
  static public EnumSet<EnhanceMode> parseEnhanceMode(String enhanceMode) {
    if (enhanceMode == null) return null;

    EnumSet<EnhanceMode> mode = null;

    if (enhanceMode.equalsIgnoreCase("true") || enhanceMode.equalsIgnoreCase("All")) {
      mode = getEnhanceAll();
    } else if (enhanceMode.equalsIgnoreCase("AllDefer")) {
      mode = EnumSet.of(EnhanceMode.ScaleMissingDefer, EnhanceMode.CoordSystems, EnhanceMode.ConvertEnums);
    } else if (enhanceMode.equalsIgnoreCase("ScaleMissing")) {
      mode = EnumSet.of(EnhanceMode.ScaleMissing);
    } else if (enhanceMode.equalsIgnoreCase("ScaleMissingDefer")) {
      mode = EnumSet.of(EnhanceMode.ScaleMissingDefer);
    } else if (enhanceMode.equalsIgnoreCase("CoordSystems")) {
      mode = EnumSet.of(EnhanceMode.CoordSystems);
    } else if (enhanceMode.equalsIgnoreCase("ConvertEnums")) {
      mode = EnumSet.of(EnhanceMode.ConvertEnums);
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
   * Set the default set of Enhancements to do for all subsequent dataset opens and acquires.
   *
   * @param mode the default set of Enhancements for open and acquire factory methods
   */
  static public void setDefaultEnhanceMode(EnumSet<EnhanceMode> mode) {
    defaultEnhanceMode = mode;
  }

  /**
   * Get the default set of Enhancements
   *
   * @return the the default set of Enhancements for open and acquire factory methods
   */
  static public EnumSet<EnhanceMode> getDefaultEnhanceMode() {
    return EnumSet.copyOf(defaultEnhanceMode);
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
   * @param minElementsInMemory keep this number in the cache
   * @param maxElementsInMemory trigger a cleanup if it goes over this number.
   * @param period              (secs) do periodic cleanups every this number of seconds.
   */
  static public void initNetcdfFileCache(int minElementsInMemory, int maxElementsInMemory, int period) {
    fileCache = new ucar.nc2.util.cache.FileCache( minElementsInMemory, maxElementsInMemory, period);
    defaultNetcdfFileFactory = new MyNetcdfFileFactory();
  }

  // no state, so a singleton is ok
  static private class MyNetcdfFileFactory implements ucar.nc2.util.cache.FileFactory {
    public NetcdfFile open(String location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
      return openFile(location, buffer_size, cancelTask, iospMessage);
    }
  }

  /**
    * Call when application exits, if you have previously called initNetcdfFileCache.
   *  This shuts down any background threads in order to get a clean process shutdown.
    */
  static public void shutdown() {
    FileCache.shutdown();
  }

  /**
   * Get the File Cache
   * @return File Cache or null if not enabled.
   */
  static public ucar.nc2.util.cache.FileCache getNetcdfFileCache() {
    return fileCache;
  }

  ////////////////////////////////////////////////////////////////////////////////////


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
   * @param enhanceMode how to enhance
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param spiObject   sent to iosp.setSpecial() if not null
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  static public NetcdfDataset openDataset(String location, EnumSet<EnhanceMode> enhanceMode, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
    // do not acquire
    NetcdfFile ncfile = openOrAcquireFile( null, null, null, location, buffer_size, cancelTask, spiObject);
    NetcdfDataset ds;
    if (ncfile instanceof NetcdfDataset) {
      ds = (NetcdfDataset) ncfile;
      enhance(ds, enhanceMode, cancelTask); // enhance "in place", eg modify the NetcdfDataset
    } else {
      ds = new NetcdfDataset(ncfile, enhanceMode); // enhance when wrapping
    }

    return ds;
  }

  static private void enhance(NetcdfDataset ds, EnumSet<EnhanceMode> mode, CancelTask cancelTask) throws IOException {
    ds.enhanceMode = (mode == null) ? EnumSet.noneOf(EnhanceMode.class) : EnumSet.copyOf(mode);

    // enhance scale/offset first, so its transferred to CoordinateAxes in next section
    if (ds.enhanceMode.contains(EnhanceMode.ScaleMissing)) {
      for (Variable v : ds.getVariables()) {
        VariableEnhanced ve = (VariableEnhanced) v;
        ve.enhance(ds.enhanceMode);
        if ((cancelTask != null) && cancelTask.isCancel()) return;
      }
    }

    // now find coord systems which may add new variables, change some Variables to axes, etc
    if (ds.enhanceMode.contains(EnhanceMode.CoordSystems)) {
      if (!ds.coordSysWereAdded) // LOOK why do we need this ? why not recalculate ??
        ucar.nc2.dataset.CoordSysBuilder.addCoordinateSystems(ds, cancelTask);
      ds.coordSysWereAdded = true;
    }

    ds.finish(); // recalc the global lists
  }

   /**
   * Same as openDataset, but file is acquired through the File Cache.
   * You still close with NetcdfDataset.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param location    location of file, passed to FileFactory
   * @param cancelTask  allow task to be cancelled; may be null.
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error

   */
  static public NetcdfDataset acquireDataset(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return acquireDataset(null, location, defaultEnhanceMode, -1, cancelTask, null);
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
   * @param iospMessage   sent to iosp.setSpecial() if not null
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error

   */
  static public NetcdfDataset acquireDataset(FileFactory fac, String location, EnumSet<EnhanceMode> enhanceMode, int buffer_size,
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

  static class MyNetcdfDatasetFactory implements ucar.nc2.util.cache.FileFactory {
    String location;
    EnumSet<EnhanceMode>  enhanceMode;
    MyNetcdfDatasetFactory(String location, EnumSet<EnhanceMode>  enhanceMode) {
      this.location = location;
      this.enhanceMode = enhanceMode;
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
    return openOrAcquireFile( null, null, null, location, buffer_size, cancelTask, spiObject);
  }

  /**
   * Same as openFile, but file is acquired through the File Cache.
   * You still close with NetcdfFile.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param location    location of file, passed to FileFactory
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
   * @param factory if not null, use this factory to read the file. If null, use the default factory.
   * @param hashKey if not null, use as the cache key, else use the location
   * @param location    location of file, passed to FileFactory
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param spiObject   sent to iosp.setSpecial(); may be null
   *
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
   * @param cache if not null, acquire through this NetcdfFileCache, otherwise simply open
   * @param factory if not null, use this factory if the file is not in the cache. If null, use the default factory.
   * @param hashKey if not null, use as the cache key, else use the location
   * @param location   location of file
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param spiObject   sent to iosp.setSpecial() if not null
   * @return NetcdfFile object
   * @throws java.io.IOException on read error
   */
  static private NetcdfFile openOrAcquireFile(FileCache cache, FileFactory factory, Object hashKey,
                String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {

    if (location == null) throw new IOException("NetcdfDataset.openFile: location is null");
    location = location.trim();
    location = StringUtil.replace(location, '\\', "/");

    if (location.startsWith("dods:")) {
      return acquireDODS(cache, factory, hashKey, location, buffer_size, cancelTask, spiObject);  // open through DODS

    } else if (location.startsWith(NetcdfRemote.SCHEME)) {
      return acquireRemote(cache, factory, hashKey, location, buffer_size, cancelTask, spiObject);  // open through netcdf remote

    } else if (location.startsWith("thredds:")) {
      StringBuilder log = new StringBuilder();
      ThreddsDataFactory tdf = new ThreddsDataFactory();
      NetcdfFile ncfile = tdf.openDataset( location, false, cancelTask, log); // LOOK acquire ??
      if (ncfile == null)
        throw new IOException(log.toString());
      return ncfile;

    } else if (location.endsWith(".xml") || location.endsWith(".ncml")) { //open as a NetcdfDataset through NcML
      if (!location.startsWith("http:") && !location.startsWith("file:"))
        location = "file:" + location;
      return acquireNcml(cache, factory, hashKey, location, buffer_size, cancelTask, spiObject);

    } else if (location.startsWith("http:") && isDODS(location)) {
        return acquireDODS(cache, factory, hashKey, location, buffer_size, cancelTask, spiObject); // try as a dods file
    }

    if (cache != null) {
      if (factory == null) factory = defaultNetcdfFileFactory;
      return (NetcdfFile) cache.acquire(factory, hashKey, location, buffer_size, cancelTask, spiObject);
    } else {
      return NetcdfFile.open(location, buffer_size, cancelTask, spiObject);
    }
  }

  /*
   * Check for existence of dods URL with a HEAD
   * LOOK should probably get rid of this, just try getting the dds
   * LOOK by going through HttpURLConnection instead of DConnect, may not be setting permissions etc
   */
  static private boolean isDODS(String location) throws IOException {
    try {
      URL u = new URL(location + ".dds");
      HttpURLConnection conn = (HttpURLConnection) u.openConnection();
      conn.setRequestMethod("HEAD");
      int code = conn.getResponseCode();
      return (code == 200);

    } catch (Exception e) {
      throw new IOException(location + " is not a valid URL." + e.getMessage());
    }
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
    if (cache == null) return new NetcdfRemote(location, cancelTask);

    if (factory == null) factory = new RemoteFactory();
    return (NetcdfFile) cache.acquire(factory, hashKey, location, buffer_size, cancelTask, spiObject);
  }

  static private class RemoteFactory implements FileFactory {
     public NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
       return new NetcdfRemote(location, cancelTask);
      }
  }

  ////////////////////////////////////////////////////////////////////////////////////
  private NetcdfFile orgFile = null;

  private List<CoordinateSystem> coordSys = new ArrayList<CoordinateSystem>();
  private List<CoordinateAxis> coordAxes = new ArrayList<CoordinateAxis>();
  private List<CoordinateTransform> coordTransforms = new ArrayList<CoordinateTransform>();
  private boolean coordSysWereAdded = false;

  private EnumSet<EnhanceMode> enhanceMode = EnumSet.noneOf(EnhanceMode.class); // enhancement mode for this specific dataset

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
   * Get the current state of dataset enhancement.
   * @return the current state of dataset enhancement.
   */
  public EnumSet<EnhanceMode> getEnhanceMode() {
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
   * Clear any Coordinate System metadata.
   */
  public void clearCoordinateSystems() {
    coordSys = new ArrayList<CoordinateSystem>();
    coordAxes = new ArrayList<CoordinateAxis>();
    coordTransforms = new ArrayList<CoordinateTransform>();
    coordSysWereAdded = false;
  }

  /**
   * Set whether Coordinate System metadata has been added.
   * @param coordSysWereAdded set to this value
   */
  public void setCoordSysWereAdded(boolean coordSysWereAdded) {
    this.coordSysWereAdded = coordSysWereAdded;
  }

  /**
   * Retrieve the CoordinateAxis with the specified name.
   *
   * @param fullName full name of the coordinate axis
   * @return the CoordinateAxis, or null if not found
   */
  public CoordinateAxis findCoordinateAxis(String fullName) {
    if (fullName == null) return null;
    for (CoordinateAxis v : coordAxes) {
      if (fullName.equals(v.getName())) // LOOK WRONG must be escaped !!
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
    if (agg != null) agg.persistWrite(); // LOOK
    if (cache != null) {
      cache.release(this);

    /* if (getCacheState() == 3)
      return;
    else if (getCacheState() == 2)
      NetcdfDatasetCache.release(this);
    else if (getCacheState() == 1) {
      if (agg != null) agg.persist(); // LOOK
      NetcdfFileCacheOld.release(this);  */
    } else {
      if (isClosed) return;
      if (agg != null) agg.close();
      agg = null;
      if (orgFile != null) orgFile.close();
      orgFile = null;
      isClosed = true;
    }

  }

  /**
   * Check if file has changed, and reread metadata if needed.
   * All previous object references (variables, dimensions, etc) may become invalid - you must re-obtain.
   *
   * @return true if file was changed.
   * @throws IOException
   */
  public boolean sync() throws IOException {
    if (agg != null)
      return agg.sync();

    if (orgFile != null)
      return orgFile.sync();

    return false;
  }

  public boolean syncExtend() throws IOException {
    if (agg != null)
      return agg.syncExtend(false);

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
   * @param mode   enhancement mode : process scale/offset/missing and/or add Coordinate Systems
   * @throws java.io.IOException on read error
   */
  public NetcdfDataset(NetcdfFile ncfile, EnumSet<EnhanceMode> mode) throws IOException {
    super(ncfile);

    this.orgFile = ncfile;
    convertGroup(getRootGroup(), ncfile.getRootGroup());
    finish(); // build global lists

    enhance(this, mode, null);
  }

  private void convertGroup(Group g, Group from) {
    for (Dimension d : from.getDimensions())
      g.addDimension(d);

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
    if (v instanceof Structure) {
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
   */
  public void enhance() throws IOException {
    enhance(this, defaultEnhanceMode, null);
  }

  /**
   * recalc enhancement info
   *
   * @param mode how to enhance
   * @throws java.io.IOException on error
   */
  public void enhance(EnumSet<EnhanceMode> mode) throws IOException {
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
    if (v.getRank() > 1)
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

    if (v.getRank() > 1) // dont have to reshape for rank < 2
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

  private NetcdfDatasetInfo info = null;

  /**
   * Show debug / underlying implementation details
   */
  public String getDetailInfo() {
    StringBuilder sbuff = new StringBuilder(5000);
    sbuff.append("NetcdfDataset location= ").append(getLocation()).append("\n");
    sbuff.append("  title= ").append(getTitle()).append("\n");
    sbuff.append("  id= ").append(getId()).append("\n");

    if (orgFile == null) {
      sbuff.append("  has no referenced NetcdfFile!\n");
    } else {
      sbuff.append("\nReferenced File:\n");
      sbuff.append(orgFile.getDetailInfo());
    }

    return sbuff.toString();
  }

  /**
   * Debugging: get the information from parsing
   *
   * @return NetcdfDatasetInfo object
   */
  public NetcdfDatasetInfo getInfo() {
    if (null == info)
      info = new NetcdfDatasetInfo(this);
    return info;
  }

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

  /**
   * Debugging
   *
   * @param arg arguments
   */
  public static void main2(String arg[]) {
    //String urls = "file:///C:/data/buoy/cwindRM.xml";
    //String urls = "C:/data/conventions/wrf/wrf_masscore.nc";
    //String urls = "http://motherlode.ucar.edu/cgi-bin/dods/DODS-3.2.1/nph-dods/dods/model/2004050712_eta_211.nc";
    //String defaultUrl = "R:/testdata/grid/netcdf/atd-radar/rgg.20020411.000000.lel.ll.nc";
    //String defaultUrl = "R:/testdata/grid/grib/grib2/test/NAM_CONUS_12km_20060604_1800.grib2";
    String defaultUrl = "C:/data/grib/nam/conus12/NAM_CONUS_12km_20060604_1800.grib2";

    String filename = (arg.length > 0) ? arg[0] : defaultUrl;

    try {
      NetcdfDataset ncDataset = NetcdfDataset.openDataset(filename, true, null);

      System.out.println("NetcdfDataset = " + filename + "\n" + ncDataset);
      debugDump(System.out, ncDataset);
    } catch (Exception ioe) {
      System.out.println("error = " + filename);
      ioe.printStackTrace();
    }
  }

  public static void main(String arg[]) throws IOException {
    //String datasetIn = "http://motherlode.ucar.edu:9080/thredds/dodsC/nexrad/level2/KVTX/20070206/Level2_KVTX_20070206_2341.ar2v";
    String datasetIn = "dods://tashtego.marine.rutgers.edu:8080/thredds/dodsC/met/ncep-nam-1hour/Uwind_nam_1hourly_MAB_and_GoM.nc";
    String filenameOut = "C:/temp/test.nc";
    NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDataset.openFile(datasetIn, null);
    ucar.nc2.FileWriter.writeToFile(ncfileIn, filenameOut);
  }

}
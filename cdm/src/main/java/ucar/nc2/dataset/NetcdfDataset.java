/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.jdom2.Element;
import thredds.client.catalog.ServiceType;
import thredds.client.catalog.tools.DataFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.iosp.IOServiceProvider;
import ucar.nc2.ncml.NcMLReader;
import ucar.nc2.ncml.NcMLWriter;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.CancelTaskImpl;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * NetcdfDataset extends the netCDF API, adding standard attribute parsing such as
 * scale and offset, and explicit support for Coordinate Systems.
 * A NetcdfDataset wraps a NetcdfFile, or is defined by an NcML document.
 * <p>
 * <p> Be sure to close the dataset when done, best practice is to use try-with-resource:
 * <pre>
 * try (NetcdfDataset ncd = NetcdfDataset.openDataset(fileName)) {
 *   ...
 * }
 * </pre>
 * <p>
 * <p>
 * By default NetcdfDataset is opened with all enhancements turned on. The default "enhance
 * mode" can be set through setDefaultEnhanceMode(). One can also explicitly set the enhancements you want in
 * the dataset factory methods. The enhancements are:
 * <ul>
 *   <li>ConvertEnums: convert enum values to their corresponding Strings. If you want to do this manually,
 *       you can call Variable.lookupEnumString().</li>
 *   <li>ConvertUnsigned: reinterpret the bit patterns of any negative values as unsigned.</li>
 *   <li>ApplyScaleOffset: process scale/offset attributes, and automatically convert the data.</li>
 *   <li>ConvertMissing: replace missing data with NaNs, for efficiency.</li>
 *   <li>CoordSystems: extract CoordinateSystem using the CoordSysBuilder plug-in mechanism.</li>
 * </ul>
 * <p>
 * Automatic scale/offset processing has some overhead that you may not want to incur up-front. If so, open the
 * NetcdfDataset without {@code ApplyScaleOffset}. The VariableDS data type is not promoted and the data is not
 * converted on a read, but you can call the convertScaleOffset() routines to do the conversion later.
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
       NcML location is read in, then transferred to new NetcdfDataset as needed
       orgFile = file defined by NcML location
    4) NcML new
       NcML location = null
       orgFile = null
       NetcdfDataset defined only by NcML, data is set to FillValue unless explicitly defined
 */

public class NetcdfDataset extends ucar.nc2.NetcdfFile {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NetcdfDataset.class);

  /**
   * Possible enhancements for a NetcdfDataset
   */
  public enum Enhance {
    /** Convert enums to Strings. */
    ConvertEnums,
    /**
     * For {@link ucar.nc2.constants.CDM#UNSIGNED} variables, reinterpret the bit patterns of any negative values as
     * unsigned. The result will be positive values that must be stored in a
     * {@link EnhanceScaleMissingUnsignedImpl#nextLarger larger data type}.
     */
    ConvertUnsigned,
    /** Apply scale and offset to values, promoting the data type if needed. */
    ApplyScaleOffset,
    /**
     * Replace {@link EnhanceScaleMissingUnsigned#isMissing missing} data with NaNs, for efficiency. Note that if the
     * enhanced data type is not {@code FLOAT} or {@code DOUBLE}, this has no effect.
     */
    ConvertMissing,
    /** Build coordinate systems. */
    CoordSystems,
    /** Build coordinate systems allowing for incomplete coordinate systems (i.e. not
     * every dimension in a variable has a corresponding coordinate variable.
     */
    IncompleteCoordSystems,
  }

  static private Set<Enhance> EnhanceAll = Collections.unmodifiableSet(EnumSet.of(Enhance.ConvertEnums,
          Enhance.ConvertUnsigned, Enhance.ApplyScaleOffset, Enhance.ConvertMissing, Enhance.CoordSystems));
  static private Set<Enhance> EnhanceNone = Collections.unmodifiableSet(EnumSet.noneOf(Enhance.class));
  static private Set<Enhance> defaultEnhanceMode = EnhanceAll;

  static public Set<Enhance> getEnhanceAll() {
    return EnhanceAll;
  }
  static public Set<Enhance> getEnhanceNone() {
    return EnhanceNone;
  }
  static public Set<Enhance> getDefaultEnhanceMode() {
    return defaultEnhanceMode;
  }

  /**
   * Set the default set of Enhancements to do for all subsequent dataset opens and acquires.
   *
   * @param mode the default set of Enhancements for open and acquire factory methods
   */
  static public void setDefaultEnhanceMode(Set<Enhance> mode) {
    defaultEnhanceMode = Collections.unmodifiableSet(mode);
  }

  /**
   * Retrieve the set of Enhancements that is associated with the given string.
   * <p/>
   * <table border="1">
   *   <tr>  <th>String</th>  <th>Enhancements</th>  </tr>
   *   <tr>  <td>All</td>  <td>ConvertEnums, ConvertUnsigned, ApplyScaleOffset, ConvertMissing, CoordSystems</td>  </tr>
   *   <tr>  <td>None</td>                   <td> &lt;empty&gt; </td>                                    </tr>
   *   <tr>  <td>ConvertEnums</td>           <td>ConvertEnums</td>                                       </tr>
   *   <tr>  <td>ConvertUnsigned</td>        <td>ConvertUnsigned</td>                                    </tr>
   *   <tr>  <td>ApplyScaleOffset</td>       <td>ApplyScaleOffset</td>                                   </tr>
   *   <tr>  <td>ConvertMissing</td>         <td>ConvertMissing</td>                                     </tr>
   *   <tr>  <td>CoordSystems</td>           <td>CoordSystems</td>                                       </tr>
   *   <tr>  <td>IncompleteCoordSystems</td> <td>CoordSystems</td>                                       </tr>
   *   <tr>  <td>true</td>                   <td>Alias for "All"</td>                                    </tr>
   *   <tr>  <td>ScaleMissingDefer</td>      <td>Alias for "None"</td>                                   </tr>
   *   <tr>  <td>AllDefer</td>               <td>ConvertEnums, CoordSystems</td>                         </tr>
   *   <tr>  <td>ScaleMissing</td>           <td>ConvertUnsigned, ApplyScaleOffset, ConvertMissing</td>  </tr>
   * </table>
   *
   * @param enhanceMode  a string from the above table.
   * @return  the set corresponding to {@code enhanceMode}, or {@code null} if there is no correspondence.
   */
  static public Set<Enhance> parseEnhanceMode(String enhanceMode) {
    if (enhanceMode == null) return null;
    
    switch (enhanceMode.toLowerCase()) {
      case "all":                    return getEnhanceAll();
      case "none":                   return getEnhanceNone();
      case "convertenums":           return EnumSet.of(Enhance.ConvertEnums);
      case "convertunsigned":        return EnumSet.of(Enhance.ConvertUnsigned);
      case "applyscaleoffset":       return EnumSet.of(Enhance.ApplyScaleOffset);
      case "convertmissing":         return EnumSet.of(Enhance.ConvertMissing);
      case "coordsystems":           return EnumSet.of(Enhance.CoordSystems);
      case "incompletecoordsystems": return EnumSet.of(Enhance.CoordSystems,
                                                       Enhance.IncompleteCoordSystems);
      // Legacy strings, retained for backwards compatibility:
      case "true":                   return getEnhanceAll();
      case "scalemissingdefer":      return getEnhanceNone();
      case "alldefer":               return EnumSet.of(Enhance.ConvertEnums, Enhance.CoordSystems);
      case "scalemissing":           return EnumSet.of(
              Enhance.ConvertUnsigned, Enhance.ApplyScaleOffset, Enhance.ConvertMissing);
      // Return null by default, since some valid strings actually return an empty set.
      default: return null;
    }
  }

  static protected boolean fillValueIsMissing = true, invalidDataIsMissing = true, missingDataIsMissing = true;

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
  // NetcdfFile caching

  static private ucar.nc2.util.cache.FileCache netcdfFileCache = null;
  static private ucar.nc2.util.cache.FileFactory defaultNetcdfFileFactory = new MyNetcdfFileFactory();

  // no state, so a singleton is ok
  static private class MyNetcdfFileFactory implements ucar.nc2.util.cache.FileFactory {
    public NetcdfFile open(DatasetUrl location, int buffer_size, CancelTask cancelTask, Object iospMessage)
            throws IOException {
      return openFile(location, buffer_size, cancelTask, iospMessage);
    }
  }

  /**
   * Enable file caching. call this before calling acquireFile().
   * When application terminates, call NetcdfDataset.shutdown().
   *
   * @param minElementsInMemory keep this number in the cache
   * @param maxElementsInMemory trigger a cleanup if it goes over this number.
   * @param period              (secs) do periodic cleanups every this number of seconds. set to < 0 to not cleanup
   */
  static public synchronized void initNetcdfFileCache(int minElementsInMemory, int maxElementsInMemory, int period) {
    initNetcdfFileCache(minElementsInMemory, maxElementsInMemory, -1, period);
  }

  /**
   * Enable file caching. call this before calling acquireFile().
   * When application terminates, call NetcdfDataset.shutdown().
   *
   * @param minElementsInMemory keep this number in the cache
   * @param maxElementsInMemory trigger a cleanup if it goes over this number.
   * @param hardLimit           if > 0, never allow more than this many elements. This causes a cleanup to be done in
   *                            the calling thread.
   * @param period              (secs) do periodic cleanups every this number of seconds.
   */
  static public synchronized void initNetcdfFileCache(
          int minElementsInMemory, int maxElementsInMemory, int hardLimit, int period) {
    netcdfFileCache = new ucar.nc2.util.cache.FileCache(
            "NetcdfFileCache ", minElementsInMemory, maxElementsInMemory, hardLimit, period);
  }

  static public synchronized void disableNetcdfFileCache() {
    if (null != netcdfFileCache) netcdfFileCache.disable();
    netcdfFileCache = null;
  }

  /**
   * Call when application exits, if you have previously called initNetcdfFileCache.
   * This shuts down any background threads in order to get a clean process shutdown.
   */
  static public synchronized void shutdown() {
    disableNetcdfFileCache();
    FileCache.shutdown();
  }

  /**
   * Get the File Cache
   *
   * @return File Cache or null if not enabled.
   */
  static public synchronized ucar.nc2.util.cache.FileCacheIF getNetcdfFileCache() {
    return netcdfFileCache;
  }

  ////////////////////////////////////////////////////////////////////////////////////

  /**
   * Make NetcdfFile into NetcdfDataset with given enhance mode
   *
   * @param ncfile wrap this
   * @param mode   using this enhance mode (may be null, meaning no enhance)
   * @return NetcdfDataset wrapping the given ncfile
   * @throws IOException on io error
   */
  static public NetcdfDataset wrap(NetcdfFile ncfile, Set<Enhance> mode) throws IOException {
    if (ncfile instanceof NetcdfDataset) {
      NetcdfDataset ncd = (NetcdfDataset) ncfile;
      if (!ncd.enhanceNeeded(mode))
        return (NetcdfDataset) ncfile;
    }

    // enhancement requires wrappping, to not modify underlying dataset, eg if cached
    // perhaps need a method variant that allows the ncfile to be modified
    return new NetcdfDataset(ncfile, mode);
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
   * @param enhance    if true, use defaultEnhanceMode, else no enhancements
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
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    return openDataset(durl, enhance ? defaultEnhanceMode : null, buffer_size, cancelTask, spiObject);
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
  static public NetcdfDataset openDataset(DatasetUrl location, Set<Enhance> enhanceMode, int buffer_size,
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
    if (mode == null) {
        mode = EnumSet.noneOf(Enhance.class);
    }

    // CoordSysBuilder may enhance dataset: add new variables, attributes, etc
    CoordSysBuilderIF builder = null;
    if (mode.contains(Enhance.CoordSystems) && !ds.enhanceMode.contains(Enhance.CoordSystems)) {
      builder = ucar.nc2.dataset.CoordSysBuilder.factory(ds, cancelTask);
      builder.augmentDataset(ds, cancelTask);
      ds.convUsed = builder.getConventionUsed();
    }

    // now enhance enum/scale/offset/unsigned, using augmented dataset
    if ((mode.contains(Enhance.ConvertEnums) && !ds.enhanceMode.contains(Enhance.ConvertEnums))
            || (mode.contains(Enhance.ConvertUnsigned) && !ds.enhanceMode.contains(Enhance.ConvertUnsigned))
            || (mode.contains(Enhance.ApplyScaleOffset) && !ds.enhanceMode.contains(Enhance.ApplyScaleOffset))
            || (mode.contains(Enhance.ConvertMissing) && !ds.enhanceMode.contains(Enhance.ConvertMissing))) {
      for (Variable v : ds.getVariables()) {
        VariableEnhanced ve = (VariableEnhanced) v;
        ve.enhance(mode);
        if ((cancelTask != null) && cancelTask.isCancel()) return null;
      }
    }

    // now find coord systems which may change some Variables to axes, etc
    if (builder != null) {
      // temporarily set enhanceMode if incomplete coordinate systems are allowed
      if (mode.contains(Enhance.IncompleteCoordSystems)) {
        ds.enhanceMode.add(Enhance.IncompleteCoordSystems);
        builder.buildCoordinateSystems(ds);
        ds.enhanceMode.remove(Enhance.IncompleteCoordSystems);
      } else {
        builder.buildCoordinateSystems(ds);
      }
    }

    /* timeTaxis must be CoordinateAxis1DTime
    for (CoordinateSystem cs : ds.getCoordinateSystems()) {
      cs.makeTimeAxis();
    } */


    ds.finish(); // recalc the global lists
    ds.enhanceMode.addAll(mode);

    return builder;
  }

  /**
   * Same as openDataset, but file is acquired through the File Cache, with defaultEnhanceMode,
   * without the need of setting the enhanceMode via the signature.
   * You still close with NetcdfDataset.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param location   location of file, passed to FileFactory
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */
  static public NetcdfDataset acquireDataset(DatasetUrl location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return acquireDataset(null, location, defaultEnhanceMode, -1, cancelTask, null);
  }

  /**
   * Same as openDataset, but file is acquired through the File Cache, with defaultEnhanceMode.
   * You still close with NetcdfDataset.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param location   location of file, passed to FileFactory
   * @param enhanceMode how to enhance. if null, then no enhancement
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */

  static public NetcdfDataset acquireDataset(DatasetUrl location, boolean enhanceMode, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return acquireDataset(null, location, enhanceMode ? defaultEnhanceMode : null, -1, cancelTask, null);
  }

  /**
   * Same as openDataset, but file is acquired through the File Cache, with specified enhancements.
   * You still close with NetcdfDataset.close(), the release is handled automatically.
   * You must first call initNetcdfFileCache() for caching to actually take place.
   *
   * @param location   location of file, passed to FileFactory
   * @param enhanceMode how to enhance. if null, then no enhancement
   * @param cancelTask allow task to be cancelled; may be null.
   * @return NetcdfDataset object
   * @throws java.io.IOException on read error
   */

  static public NetcdfDataset acquireDataset(DatasetUrl location, Set<Enhance> enhanceMode, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    return acquireDataset(null, location, enhanceMode, -1, cancelTask, null);
  }

  /*
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
  /* static public NetcdfDataset acquireDataset(FileFactory fac, String location, Set<Enhance> enhanceMode, int buffer_size,
                                             ucar.nc2.util.CancelTask cancelTask, Object iospMessage) throws IOException {

    // caching not turned on
    if (netcdfFileCache == null) {
      if (fac == null)
        return openDataset(location, enhanceMode, buffer_size, cancelTask, iospMessage);
      else
        // must use the factory if there is one
        return (NetcdfDataset) fac.open(location, buffer_size, cancelTask, iospMessage);
    }

    if (fac != null)
      return (NetcdfDataset) openOrAcquireFile(netcdfFileCache, fac, null, location, buffer_size, cancelTask, iospMessage);

    fac = new MyNetcdfDatasetFactory(location, enhanceMode);
    return (NetcdfDataset) openOrAcquireFile(netcdfFileCache, fac, fac.hashCode(), location, buffer_size, cancelTask, iospMessage);
  } */

  static public NetcdfDataset acquireDataset(FileFactory fac, DatasetUrl durl, Set<Enhance> enhanceMode, int buffer_size,
                                             ucar.nc2.util.CancelTask cancelTask, Object iospMessage) throws IOException {

    // caching not turned on
    if (netcdfFileCache == null) {
      if (fac == null)
        return openDataset(durl, enhanceMode, buffer_size, cancelTask, iospMessage);
      else
        // must use the factory if there is one
        return (NetcdfDataset) fac.open(durl, buffer_size, cancelTask, iospMessage);
    }

    if (fac != null)
      return (NetcdfDataset) openOrAcquireFile(netcdfFileCache, fac, null, durl, buffer_size, cancelTask, iospMessage);

    fac = new MyNetcdfDatasetFactory(durl, enhanceMode);
    return (NetcdfDataset) openOrAcquireFile(netcdfFileCache, fac, fac.hashCode(), durl, buffer_size, cancelTask, iospMessage);
  }

  static private class MyNetcdfDatasetFactory implements ucar.nc2.util.cache.FileFactory {
    DatasetUrl location;
    EnumSet<Enhance> enhanceMode;

    MyNetcdfDatasetFactory(DatasetUrl location, Set<Enhance> enhanceMode) {
      this.location = location;
      this.enhanceMode = (enhanceMode == null) ? EnumSet.noneOf(Enhance.class) : EnumSet.copyOf(enhanceMode);
    }

    public NetcdfFile open(DatasetUrl location, int buffer_size, CancelTask cancelTask, Object iospMessage) throws IOException {
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
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    return openOrAcquireFile(null, null, null, durl, -1, cancelTask, null);
  }

  /**
   * Factory method for opening a NetcdfFile through the netCDF API. May be any kind of file that
   * can be read through the netCDF API, including OpenDAP and NcML.
   * <p>
   * <p> This does not necessarily return a NetcdfDataset, or enhance the dataset; use NetcdfDataset.openDataset() method for that.
   *
   * @param location    location of dataset. This may be a
   *                    <ol>
   *                    <li>local filename (with a file: prefix or no prefix) for netCDF (version 3), hdf5 files, or any file type
   *                    registered with NetcdfFile.registerIOProvider().
   *                    <li>OpenDAP dataset URL (with a dods:, dap4:, or http: prefix).
   *                    <li>NcML file or URL if the location ends with ".xml" or ".ncml"
   *                    <li>NetCDF file through an HTTP server (http: prefix)
   *                    <li>thredds dataset (thredds: prefix), see DataFactory.openDataset(String location, ...));
   *                    </ol>
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param spiObject   sent to iosp.setSpecial() if not null
   * @return NetcdfFile object
   * @throws java.io.IOException on read error
   */
  public static NetcdfFile openFile(DatasetUrl location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
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
  static public NetcdfFile acquireFile(DatasetUrl location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
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
                                       DatasetUrl location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {

    // must use the factory if there is one but no fileCache
    if ((netcdfFileCache == null) && (factory != null)) {
      return (NetcdfFile) factory.open(location, buffer_size, cancelTask, spiObject);
    }

    return openOrAcquireFile(netcdfFileCache, factory, hashKey, location, buffer_size, cancelTask, spiObject);
  }

  /*
   * Open or acquire a NetcdfFile.
   *
   * @param cache       if not null, acquire through this NetcdfFileCache, otherwise simply open
   * @param factory     if not null, use this factory if the file is not in the cache. If null, use the default factory.
   * @param hashKey     if not null, use as the cache key, else use the location
   * @param orgLocation location of file
   * @param buffer_size RandomAccessFile buffer size, if <= 0, use default size
   * @param cancelTask  allow task to be cancelled; may be null.
   * @param spiObject   sent to iosp.setSpecial() if not null
   * @return NetcdfFile object
   * @throws java.io.IOException on read error
   *
  static private NetcdfFile openOrAcquireFile(FileCache cache, FileFactory factory, Object hashKey, String orgLocation,
                                              int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {

    if (orgLocation == null)
      throw new IOException("NetcdfDataset.openFile: location is null");

    String location = StringUtil2.replace(orgLocation.trim(), '\\', "/");
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    return openOrAcquireFile(cache, factory, hashKey, durl, buffer_size, cancelTask, spiObject);
  } */

  static private NetcdfFile openOrAcquireFile(FileCache cache, FileFactory factory, Object hashKey, DatasetUrl durl,
                                              int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {

    if (durl.serviceType != null) {
      switch (durl.serviceType) {

        case OPENDAP:
          return acquireDODS(cache, factory, hashKey, durl.trueurl, buffer_size, cancelTask, spiObject);

        case CdmRemote:
          return acquireCdmRemote(cache, factory, hashKey, durl.trueurl, buffer_size, cancelTask, spiObject);

        case DAP4:
          return acquireDap4(cache, factory, hashKey, durl.trueurl, buffer_size, cancelTask, spiObject);

        case NCML:
          return acquireNcml(cache, factory, hashKey, durl.trueurl, buffer_size, cancelTask, spiObject);

        case THREDDS:
          Formatter log = new Formatter();
          DataFactory tdf = new DataFactory();
          NetcdfFile ncfile = tdf.openDataset(durl.trueurl, false, cancelTask, log); // LOOK acquire ??
          if (ncfile == null)
            throw new IOException(log.toString());
          return ncfile;

        case File:
        case HTTPServer:
          break; // fall through

        default:
          throw new IOException("Unknown service type: " + durl.serviceType.toString());
      }
    }

    // Next to last resort: find in the cache
    if (cache != null) {
      if (factory == null) factory = defaultNetcdfFileFactory;
      return (NetcdfFile) cache.acquire(factory, hashKey, durl, buffer_size, cancelTask, spiObject);
    }

    // Last resort: try to open as a file or remote file
    return NetcdfFile.open(durl.trueurl, buffer_size, cancelTask, spiObject);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // opendap


  /**
   * Define the dap4 path
   */
  static private final String DAP4_PATH = "dap4.cdm.nc2";

  static private NetcdfFile acquireDODS(FileCache cache, FileFactory factory, Object hashKey,
                                        String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
    if (cache == null) {
      return openDodsByReflection(location, cancelTask);
    }

    if (factory == null) factory = new DodsFactory();
    return (NetcdfFile) cache.acquire(factory, hashKey, new DatasetUrl(ServiceType.OPENDAP, location), buffer_size, cancelTask, spiObject);
  }

  static private NetcdfFile acquireDap4(FileCache cache,
                                        FileFactory factory,
                                        Object hashKey,
                                        String location,
                                        int buffer_size,
                                        ucar.nc2.util.CancelTask cancelTask,
                                        Object spiObject)
          throws IOException {
    if (cache == null) {
      return openDap4ByReflection(location, cancelTask);
    }

    if (factory == null) factory = new Dap4Factory();
    return (NetcdfFile) cache.acquire(factory, hashKey, new DatasetUrl(ServiceType.DAP4, location), buffer_size, cancelTask, spiObject);
  }

  static private class DodsFactory implements FileFactory {
    public NetcdfFile open(DatasetUrl location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
      return openDodsByReflection(location.trueurl, cancelTask);
    }
  }

  static private class Dap4Factory implements FileFactory {
    public NetcdfFile open(DatasetUrl location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
      return openDap4ByReflection(location.trueurl, cancelTask);
    }
  }

  static private NetcdfFile openDodsByReflection(String location, ucar.nc2.util.CancelTask cancelTask) throws IOException {
    Constructor con;
    Class c;
    NetcdfFile file;
    try {
      c = NetcdfDataset.class.getClassLoader().loadClass("ucar.nc2.dods.DODSNetcdfFile");
      con = c.getConstructor(String.class, ucar.nc2.util.CancelTask.class);
    } catch (ClassNotFoundException e) {
      log.info("opendap.jar is not on class path or is incorrect version");
      throw new IOException("opendap.jar is not on classpath or is incorrect version");
    } catch (Throwable e) {
      log.error("Error openDodsByReflection: ", e);
      throw new IOException("opendap.jar is not on classpath or is incorrect version");
    }
    try {
      file = (NetcdfFile) con.newInstance(location, cancelTask);
      return file;
    } catch (Exception e) {
      log.error("Error openDodsByReflection: ", e.getCause());
      throw new IOException(e.getCause());
    }

  }

  static private NetcdfFile openDap4ByReflection(String location, ucar.nc2.util.CancelTask cancelTask)
          throws IOException {
    Constructor constructormethod;
    Class dap4class;
    NetcdfFile file;
    String target = DAP4_PATH + ".DapNetcdfFile";
    try {
      dap4class = NetcdfFile.class.getClassLoader().loadClass(target);
      constructormethod = dap4class.getConstructor(String.class, ucar.nc2.util.CancelTask.class);
      file = (NetcdfFile) constructormethod.newInstance(location, cancelTask);
      return file;
    } catch (ClassNotFoundException e) {
      String msg = "DapNetcdfFile is not on class path or is incorrect version: " + target;
      log.error(msg);
      throw new IOException(msg);
    } catch (NoSuchMethodException e) {
      String msg = "DapNetcdfFile constructor not found";
      log.error(msg);
      throw new IOException(msg);
    } catch (InstantiationException e) {
      String msg = "DapNetcdfFile constructor cannot be invoked";
      log.error(msg);
      throw new IOException(msg);
    } catch (IllegalAccessException iace) {
      String msg = "DapNetcdfFile constructor cannot be invoked";
      log.error(msg);
      throw new IOException(msg, iace);
    } catch (IllegalArgumentException iare) {
      String msg = "DapNetcdfFile constructor: illegal argument";
      log.error(msg);
      throw new IOException(msg, iare);
    } catch (InvocationTargetException ite) {
      String msg = "DapNetcdfFile constructor failed: "
              + ite.getCause().getMessage();
      log.error(msg);
      throw new IOException(msg, ite);
    }

  }

  ////////////////////////////////////////////////////////////////////////////////////

  static private NetcdfFile acquireNcml(FileCache cache, FileFactory factory, Object hashKey,
                                        String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
    if (cache == null) return NcMLReader.readNcML(location, cancelTask);

    if (factory == null) factory = new NcMLFactory();  // LOOK maybe always should use NcMLFactory ?
    return (NetcdfFile) cache.acquire(factory, hashKey, DatasetUrl.findDatasetUrl(location), buffer_size, cancelTask, spiObject);
  }

  static private class NcMLFactory implements FileFactory {
    public NetcdfFile open(DatasetUrl location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
      return NcMLReader.readNcML(location.trueurl, cancelTask);
    }
  }

  static private NetcdfFile acquireCdmRemote(FileCache cache, FileFactory factory, Object hashKey,
                                             String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
    if (cache == null) return new CdmRemote(location);

    if (factory == null) factory = new RemoteFactory();
    return (NetcdfFile) cache.acquire(factory, hashKey, new DatasetUrl(ServiceType.CdmRemote, location), buffer_size, cancelTask, spiObject);
  }

  static private class RemoteFactory implements FileFactory {
    public NetcdfFile open(DatasetUrl location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
      return new CdmRemote(location.trueurl);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////
  private NetcdfFile orgFile = null;

  private List<CoordinateSystem> coordSys = new ArrayList<>();
  private List<CoordinateAxis> coordAxes = new ArrayList<>();
  private List<CoordinateTransform> coordTransforms = new ArrayList<>();
  private String convUsed;

  private EnumSet<Enhance> enhanceMode = EnumSet.noneOf(Enhance.class); // enhancement mode for this specific dataset

  // If its an aggregation
  private ucar.nc2.ncml.Aggregation agg = null;

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
    coordSys = new ArrayList<>();
    coordAxes = new ArrayList<>();
    coordTransforms = new ArrayList<>();

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
   * @param fullName full escaped name of the coordinate axis
   * @return the CoordinateAxis, or null if not found
   */
  public CoordinateAxis findCoordinateAxis(String fullName) {
    if (fullName == null) return null;
    for (CoordinateAxis v : coordAxes) {
      if (fullName.equals(v.getFullName()))
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
    if (agg != null) {
      agg.persistWrite(); // LOOK  maybe only on real close ??
      agg.close();
    }

    if (cache != null) {
      //unlocked = true;
      if (cache.release(this)) return;
    }

    if (orgFile != null) orgFile.close();
    orgFile = null;
  }

  // optionally release any resources like file handles
  public void release() throws IOException {
    if (orgFile != null)
      orgFile.release();
  }

  // reacquire any resources like file handles
  public void reacquire() throws IOException {
    if (orgFile != null)
      orgFile.reacquire();
  }


  @Override
  public long getLastModified() {
    if (agg != null) {
      return agg.getLastModified();
    }

    return (orgFile != null) ? orgFile.getLastModified() : 0;
  }

  @Override
  public void empty() {
    super.empty();
    coordSys = new ArrayList<>();
    coordAxes = new ArrayList<>();
    coordTransforms = new ArrayList<>();
    convUsed = null;
  }

  public boolean syncExtend() throws IOException {
    //unlocked = false;

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
   * @param uri use this for the url attribute; if null use getLocation().
   * @throws IOException
   */
  @Override
  public void writeNcML(java.io.OutputStream os, String uri) throws IOException {
    NcMLWriter ncmlWriter = new NcMLWriter();
    Element netcdfElem = ncmlWriter.makeNetcdfElement(this, uri);
    ncmlWriter.writeToStream(netcdfElem, os);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Transform a NetcdfFile into a NetcdfDataset, with default enhancement.
   * You must not use the underlying NetcdfFile after this call, because it gets modified.
   * Therefore you should not use this with a cached file.
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
      g.addDimension(new Dimension(d.getShortName(), d));

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
    StructureDS newStructure = new StructureDS(this, root, null, "record", udim.getShortName(), null, null);
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
  private static class VariableComparator implements java.util.Comparator {
    public int compare(Object o1, Object o2) {
      VariableEnhanced v1 = (VariableEnhanced) o1;
      VariableEnhanced v2 = (VariableEnhanced) o2;
      List list1 = v1.getCoordinateSystems();
      String cs1 = (list1.size() > 0) ? ((CoordinateSystem) list1.get(0)).getName() : "";
      List list2 = v2.getCoordinateSystems();
      String cs2 = (list2.size() > 0) ? ((CoordinateSystem) list2.get(0)).getName() : "";

      if (cs2.equals(cs1))
        return v1.getShortName().compareToIgnoreCase(v2.getShortName());
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
    CoordinateAxis oldVar = findCoordinateAxis(v.getFullName());
    if (oldVar != null)
      coordAxes.remove(oldVar);

    CoordinateAxis ca = (v instanceof CoordinateAxis) ? (CoordinateAxis) v : CoordinateAxis.factory(this, v);
    coordAxes.add(ca);

    if (v.isMemberOfStructure()) {
      Structure parentOrg = v.getParentStructure();  // gotta be careful to get the wrapping parent
      Structure parent = (Structure) findVariable(parentOrg.getFullNameEscaped());
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
   * @return the CoordSysBuilder used, for debugging. do not modify or retain a reference
   * @throws java.io.IOException on error
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


  /**
   * is this enhancement already done ?
   *
   * @param want enhancements wanted
   * @return true if wanted enhancement is not done
   * @throws java.io.IOException on error
   */
  public boolean enhanceNeeded(Set<Enhance> want) throws IOException {
    if (want == null) return false;
    for (Enhance mode : want) {
      if (!this.enhanceMode.contains(mode)) return true;
    }
    return false;
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
   * @deprecated use Variable.setValues()
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
   * @deprecated use Variable.setValues()
   */
  public void setValues(Variable v, List<String> values) throws IllegalArgumentException {
    Array data = Array.makeArray(v.getDataType(), values);

    if (data.getSize() != v.getSize())
      throw new IllegalArgumentException("Incorrect number of values specified for the Variable " + v.getFullName() +
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
      f.format("%s", orgFile.getDetailInfo());
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

  void dumpClasses(Group g, PrintWriter out) {

    out.println("Dimensions:");
    for (Dimension ds : g.getDimensions()) {
      out.println("  " + ds.getShortName() + " " + ds.getClass().getName());
    }

    out.println("Atributes:");
    for (Attribute a : g.getAttributes()) {
      out.println("  " + a.getShortName() + " " + a.getClass().getName());
    }

    out.println("Variables:");
    dumpVariables(g.getVariables(), out);

    out.println("Groups:");
    for (Group nested : g.getGroups()) {
      out.println("  " + nested.getFullName() + " " + nested.getClass().getName());
      dumpClasses(nested, out);
    }
  }

  private void dumpVariables(List<Variable> vars, PrintWriter out) {
    for (Variable v : vars) {
      out.print("  " + v.getFullName() + " " + v.getClass().getName()); // +" "+Integer.toHexString(v.hashCode()));
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
  public static void debugDump(PrintWriter out, NetcdfDataset ncd) {
    String referencedLocation = ncd.orgFile == null ? "(null)" : ncd.orgFile.getLocation();
    out.println("\nNetcdfDataset dump = " + ncd.getLocation() + " url= " + referencedLocation + "\n");
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
      VariableDS vds = (VariableDS) v;
      if (vds.getOriginalDataType() != vds.getDataType()) {
        f.format("Variable %s has type %s, org = %s%n", vds.getFullName(), vds.getOriginalDataType(), vds.getDataType());
      }

      if (vds.getOriginalVariable() != null) {
        Variable orgVar = vds.getOriginalVariable();
        if (orgVar.getRank() != vds.getRank())
          f.format("Variable %s has rank %d, org = %d%n", vds.getFullName(), vds.getRank(), orgVar.getRank());
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
   * @param arg -in <fileIn> -out <fileOut> [-isLargeFile] [-netcdf4]
   * @throws IOException on read or write error
   */
  // LOOK: Can we use CFPointWriter.CommandLine for CLI parsing instead? Would that break existing scripts?
  public static void main(String arg[]) throws IOException {
    String usage = "usage: ucar.nc2.dataset.NetcdfDataset -in <fileIn> -out <fileOut> [-isLargeFile] [-netcdf4]";
    if (arg.length < 4) {
      System.out.println(usage);
      System.exit(0);
    }

    boolean isLargeFile = false;
    boolean netcdf4 = false;
    String datasetIn = null, datasetOut = null;
    for (int i = 0; i < arg.length; i++) {
      String s = arg[i];
      if (s.equalsIgnoreCase("-in")) datasetIn = arg[i + 1];
      if (s.equalsIgnoreCase("-out")) datasetOut = arg[i + 1];
      if (s.equalsIgnoreCase("-isLargeFile")) isLargeFile = true;
      if (s.equalsIgnoreCase("-netcdf4")) netcdf4 = true;
    }
    if ((datasetIn == null) || (datasetOut == null)) {
      System.out.println(usage);
      System.exit(0);
    }

    CancelTaskImpl cancel = new CancelTaskImpl();
    NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDataset.openFile(datasetIn, cancel);
    System.out.printf("NetcdfDatataset read from %s write to %s ", datasetIn, datasetOut);

    NetcdfFileWriter.Version version = netcdf4 ? NetcdfFileWriter.Version.netcdf4 : NetcdfFileWriter.Version.netcdf3;
    FileWriter2 writer = new ucar.nc2.FileWriter2(ncfileIn, datasetOut, version, null);
    writer.getNetcdfFileWriter().setLargeFile(isLargeFile);
    NetcdfFile ncfileOut = writer.write(cancel);
    if (ncfileOut != null) ncfileOut.close();
    ncfileIn.close();
    cancel.setDone(true);
    System.out.printf("%s%n", cancel);
  }

}

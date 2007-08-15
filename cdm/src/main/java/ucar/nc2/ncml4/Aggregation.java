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
package ucar.nc2.ncml4;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.ncml.AggregationIF;
import ucar.nc2.dataset.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.util.StringUtil;

import java.util.*;
import java.io.*;

import thredds.util.DateFromString;

/**
 * Implement NcML Aggregation.
 * <p/>
 * <h2>Implementation Notes</h2>
 * <h3>Caching</h3>
 * <ul>
 * <li>Case 1. Explicit list / Scan static directories (recheck=null)
 * <ul>
 * <li>A. AggCaching - keep track of ncoords, coordValues for joinExisting. Read on open, write on close.
 * Could skip scan if cache exists.
 * <li>B. NetcdfFileCache - write on close if changed (only first time). On sync, recheck = null means wont be reread.
 * </ul>
 * <li>Case 2. Scan dynamic directories (recheck non-null)
 * <ul>
 * <li>A. AggCaching - keep track of ncoords, coordValues for joinExisting. Read on open, write on close.
 * Could skip scan if cache exists, and recheck time not expired.
 * <li>B. NetcdfFileCache - write on close if changed. On sync, if recheck time, then rescan.
 * </ul>
 * </ul>
 * <h3>Aggregation Coordinate Variable (aggCoord) Processing</h3>
 * Construction:
 * <ol>
 * <li> The aggregation element is processed first.
 * <li> agg.finish() is called.
 * <li> If the user has defined the aggCoord in the NcML, it is then processed, overriding whatever the aggregation has constructed.
 * If values are defined, they are cached in the new variable.
 * </ol>
 * Data Reading:
 * <ol>
 * <li> If values are cached, agg.read() is never called.
 * <li> Each Dataset may have a coordinate value(s) defined in the NcML coordValue attribute.
 * <li> If not, the coordinate value(s) is cached when the dataset is opened.
 * <li> agg.read() uses those if they exist, else reads and caches.
 * </ol>
 *
 * @author caron
 */
public abstract class Aggregation implements AggregationIF, ProxyReader2 {
  static protected int TYPICAL_DATASET_RANDOM = 0;
  static protected int TYPICAL_DATASET_LATEST = 1;
  static protected int TYPICAL_DATASET_PENULTIMATE = 2;
  static protected int typicalDatasetMode = 0;

  static protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Aggregation.class);
  static protected DiskCache2 diskCache2 = null;

  static public void setPersistenceCache(DiskCache2 dc) {
    diskCache2 = dc;
  }

  static public void setTypicalDatasetMode(String mode) {
    if (mode.equalsIgnoreCase("random"))
      typicalDatasetMode = TYPICAL_DATASET_RANDOM;
    else if (mode.equalsIgnoreCase("latest"))
      typicalDatasetMode = TYPICAL_DATASET_LATEST;
    else if (mode.equalsIgnoreCase("penultimate"))
      typicalDatasetMode = TYPICAL_DATASET_PENULTIMATE;
    else
      logger.error("Unknown setTypicalDatasetMode= " + mode);
  }

  //////////////////////////////////////////////////////////////////////////////////////////

  protected NetcdfDataset ncDataset; // the aggregation belongs to this dataset
  protected Type type; // the aggregation type
  protected Object spiObject;

  protected List<Aggregation.Dataset> explicitDatasets = new ArrayList<Aggregation.Dataset>(); // explicitly created Dataset objects from netcdf elements
  protected List<Aggregation.Dataset> datasets = new ArrayList<Aggregation.Dataset>(); // all : explicit and scanned
  protected DatasetCollectionManager datasetManager; // manages scanning
  protected boolean cacheDirty = true; // aggCache persist file needs updating

  protected String dimName; // the aggregation dimension name
  protected List<String> aggVarNames = new ArrayList<String>(); // joinNew

  // experimental
  protected String dateFormatMark;
  protected boolean enhance = false, isDate = false;
  protected DateFormatter formatter = new DateFormatter();

  protected boolean debug = false, debugOpenFile = false, debugSyncDetail = false, debugProxy = false,
      debugRead = false, debugDateParse = false;

  /**
   * Create an Aggregation for the given NetcdfDataset.
   * The following addXXXX methods are called, then finish(), before the object is ready for use.
   *
   * @param ncd      Aggregation belongs to this NetcdfDataset
   * @param dimName  the aggregation dimension name
   * @param type     the Aggregation.Type
   * @param recheckS how often to check if files have changes
   */
  protected Aggregation(NetcdfDataset ncd, String dimName, Type type, String recheckS) {
    this.ncDataset = ncd;
    this.dimName = dimName;
    this.type = type;
    datasetManager = new DatasetCollectionManager(recheckS);
  }

  /**
   * Add a nested dataset, specified by an explicit netcdf element.
   * enhance is handled by the reader, so its always false here.
   *
   * @param cacheName   a unique name to use for caching
   * @param location    attribute "location" on the netcdf element
   * @param ncoordS     attribute "ncoords" on the netcdf element
   * @param coordValueS attribute "coordValue" on the netcdf element
   * @param reader      factory for reading this netcdf dataset
   * @param cancelTask  user may cancel, may be null
   */
  public void addExplicitDataset(String cacheName, String location, String ncoordS, String coordValueS, NetcdfFileFactory reader, CancelTask cancelTask) {
    Dataset nested = makeDataset(cacheName, location, ncoordS, coordValueS, false, reader);
    explicitDatasets.add(nested);
  }

  public void addDataset(Dataset nested) {
    explicitDatasets.add(nested);
  }

  /**
   * Add a scan elemnt
   *
   * @param dirName             scan this directory
   * @param suffix              filter on this suffix (may be null)
   * @param regexpPatternString include if full name matches this regular expression (may be null)
   * @param dateFormatMark      create dates from the filename (may be null)
   * @param enhanceS            should files bne enhanced?
   * @param subdirs             equals "false" if should not descend into subdirectories
   * @param olderThan           files must be older than this time (now - lastModified >= olderThan); must be a time unit, may ne bull
   * @throws IOException if I/O error
   */
  public void addDirectoryScan(String dirName, String suffix, String regexpPatternString, String dateFormatMark, String enhanceS, String subdirs, String olderThan) throws IOException {
    this.dateFormatMark = dateFormatMark;
    if ((enhanceS != null) && enhanceS.equalsIgnoreCase("true"))
      enhance = true;

    //DirectoryScan d = new DirectoryScan(type, dirName, suffix, regexpPatternString, dateFormatMark, enhance, subdirs, olderThan);
    CrawlableScanner d = new CrawlableScanner( dirName, suffix, regexpPatternString, subdirs, olderThan);
    datasetManager.addDirectoryScan(d);
  }

  /**
   * Add a name from a variableAgg element
   *
   * @param varName name of agg variable
   */
  public void addVariable(String varName) {
    aggVarNames.add(varName);
  }

  /**
   * Get type of aggregation
   * @return type of aggregation
   */
  public Type getType() {
    return type;
  }

  /**
   * Get dimension name to join on
   * @return dimension name or null if type union/tiled
   */
  public String getDimensionName() {
    return dimName;
  }

  /**
   * Get the list of aggregation variable names: variables whose data spans multiple files.
   * For type joinNew only.
   * @return the list of aggregation variable names
   */
  List<String> getAggVariableNames() {
    return aggVarNames;
  }

  /**
   * What is the data type of the aggregation coordinate ?
   *
   * @return the data type of the aggregation coordinate
   */
  public DataType getCoordinateType() {
    List<Dataset> nestedDatasets = getDatasets();
    Dataset first = nestedDatasets.get(0);
    return first.isStringValued ? DataType.STRING : DataType.DOUBLE;
  }

  /**
   * Release all resources associated with the aggregation
   * @throws IOException on error
   */
  public void close() throws IOException {
    persist();
    closeDatasets();
  }

  protected void closeDatasets() throws IOException {
    if (datasets != null) {
      for (Aggregation.Dataset ds : datasets)
        ds.close();
    }
    datasets = null;
  }

  /**
   * Allow information to be make persistent. Overridden in AggregationExisting
   *
   * @throws IOException on error
   */
  public void persist() throws IOException {
  }

  /**
   * read info from the persistent XML file, if it exists; overridden in AggregationExisting
   */
  protected void persistRead() {
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  // all elements are processed, finish construction

  public void finish(CancelTask cancelTask) throws IOException {
    datasetManager.scan( cancelTask);
    cacheDirty = true;
    closeDatasets();
    makeDatasets(cancelTask);

    // check persistence info
    persistRead();

    //ucar.unidata.io.RandomAccessFile.setDebugAccess( true);
    buildDataset(true, cancelTask);
    //ucar.unidata.io.RandomAccessFile.setDebugAccess( false);
  }

  protected List<Dataset> getDatasets() {
    return datasets;
  }

  /**
   * Make the Dataset objects.
   * @param cancelTask user can cancel
   * @throws IOException on i/o error
   */
  protected void makeDatasets(CancelTask cancelTask) throws IOException {
    List<MyCrawlableDataset> fileList = datasetManager.getFiles();
    for (MyCrawlableDataset myf : fileList) {
      // optionally parse for date
      if (null != dateFormatMark) {
        String filename = myf.file.getName();
        myf.dateCoord = DateFromString.getDateUsingDemarkatedCount(filename, dateFormatMark, '#');
        myf.dateCoordS = formatter.toDateTimeStringISO(myf.dateCoord);
        if (debugDateParse) System.out.println("  adding " + myf.file.getPath() + " date= " + myf.dateCoordS);
      } else {
        if (debugDateParse) System.out.println("  adding " + myf.file.getPath());
      }
    }

    // Sort by date if it exists, else filename.
    Collections.sort(fileList, new Comparator<MyCrawlableDataset>() {
      public int compare(MyCrawlableDataset mf1, MyCrawlableDataset mf2) {
        if (mf1.dateCoord != null) // LOOK
          return mf1.dateCoord.compareTo(mf2.dateCoord);
        else
          return mf1.file.getName().compareTo(mf2.file.getName());
      }
    });

    // create new list of Datasets, transfer explicit in order
    datasets = new ArrayList<Dataset>();
    for (Aggregation.Dataset dataset : explicitDatasets) {
      if (dataset.checkOK(null))
        datasets.add(dataset);
    }

    // now add the ordered list of Datasets to the result List
    for (MyCrawlableDataset myf : fileList) {
      String location = myf.file.getPath();
      String coordValue = (type == AggregationIF.Type.JOIN_NEW) || (type == AggregationIF.Type.JOIN_EXISTING_ONE) || (type == AggregationIF.Type.FORECAST_MODEL_COLLECTION) ? myf.dateCoordS : null;
      Aggregation.Dataset ds = makeDataset(location, location, null, coordValue, enhance, null);
      ds.coordValueDate = myf.dateCoord;
      datasets.add(ds);
    }
  }

  /**
   * Call this to build the dataset objects
   *
   * @param isNew      true when called for the first time
   * @param cancelTask maybe cancel
   * @throws IOException on read error
   */
  protected abstract void buildDataset(boolean isNew, CancelTask cancelTask) throws IOException;

  //////////////////////////////////////////////////////////////
  // LOOK: this whole sync stuff is crap, not tested, only works sometimes !!!!

  /**
   * Check to see if its time to rescan directory, and if so, rescan and extend dataset if needed.
   *
   * @param force if true, always rescan even if time not expired
   * @return true if directory was rescanned and dataset may have been updated
   * @throws IOException on io error
   */
  public synchronized boolean syncExtend(boolean force) throws IOException {
    if (!force && !datasetManager.timeToRescan())
      return false;
    if (!datasetManager.rescan())
      return false;
    cacheDirty = true;
    closeDatasets();
    makeDatasets(null);

    // only the set of datasets may have changed
    if ((getType() == Aggregation.Type.FORECAST_MODEL_COLLECTION) || (getType() == Aggregation.Type.FORECAST_MODEL_SINGLE)) {
      //ucar.unidata.io.RandomAccessFile.setDebugAccess( true);
      syncDataset(null);
      //ucar.unidata.io.RandomAccessFile.setDebugAccess( false);

    } /*else if (getType() == Aggregation.Type.FORECAST_MODEL_SINGLE) {
      ncDataset.empty(); 
      buildDataset(false, null);
      ncDataset.finish();
      if (ncDataset.isEnhanced()) { // force recreation of the coordinate systems
        ncDataset.setCoordSysWereAdded(false);
        ncDataset.enhance();
        ncDataset.finish();
      }
    } */

    /* if (getType() == Aggregation.Type.JOIN_NEW)
      resetNewDimension();
    else if (getType() == Aggregation.Type.JOIN_EXISTING)
      resetAggDimensionLength();*/ // LOOK

    return true;
  }

  protected void syncDataset(CancelTask cancelTask) throws IOException {
  }

  public synchronized boolean sync() throws IOException {
    if (!datasetManager.timeToRescan())
      return false;
    if (!datasetManager.rescan())
      return false;
    cacheDirty = true;
    closeDatasets();
    makeDatasets(null);

    //ncd.empty(); LOOK not emptying !!

    // LOOK: do we have to reread the NcML ?? This whole thing is fishy.
    // rebuild the metadata
    buildDataset(false, null);
    ncDataset.finish();
    if (ncDataset.isEnhanced()) { // force recreation of the  coordinate systems
      ncDataset.setCoordSysWereAdded(false);
      ncDataset.enhance();
      ncDataset.finish();
    }

    return true;
  }

  /////////////////////////////////////////////////////////////////////////


  /**
   * Open one of the nested datasets as a template for the aggregation dataset.
   *
   * @return a typical Dataset
   * @throws FileNotFoundException if there are no datasets
   */
  protected Dataset getTypicalDataset() throws IOException {
    List<Dataset> nestedDatasets = getDatasets();
    int n = nestedDatasets.size();
    if (n == 0)
      throw new FileNotFoundException("No datasets in this aggregation");

    int select;
    if (typicalDatasetMode == TYPICAL_DATASET_LATEST)
      select = n - 1;
    else if (typicalDatasetMode == TYPICAL_DATASET_PENULTIMATE)
      select = (n < 2) ? 0 : n - 2;
    else // random is default
      select = (n < 2) ? 0 : new Random().nextInt(n);

    return nestedDatasets.get(select);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Read an aggregation variable: A variable whose data spans multiple files.
   *
   * @param mainv      the aggregation variable
   * @param cancelTask allow the user to cancel
   * @return the data array
   * @throws IOException
   */
  public abstract Array read(Variable mainv, CancelTask cancelTask) throws IOException;

  /**
   * Read a section of an aggregation variable.
   *
   * @param mainv      the aggregation variable
   * @param cancelTask allow the user to cancel
   * @param section    read just this section of the data, array of Range
   * @return the data array section
   * @throws IOException
   */
  public abstract Array read(Variable mainv, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException;

  /**
   * Dataset factory, so subclasses can override
   *
   * @param cacheName   a unique name to use for caching
   * @param location    attribute "location" on the netcdf element
   * @param ncoordS     attribute "ncoords" on the netcdf element
   * @param coordValueS attribute "coordValue" on the netcdf element
   * @param enhance     open dataset in enhance mode
   * @param reader      factory for reading this netcdf dataset
   * @return a Aggregation.Dataset
   */
  protected Dataset makeDataset(String cacheName, String location, String ncoordS, String coordValueS, boolean enhance, NetcdfFileFactory reader) {
    return new Dataset(cacheName, location, ncoordS, coordValueS, enhance, reader);
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Encapsolates a NetcdfFile that is a component of the aggregation.
   * public for NcMLWriter
   */
  class Dataset {
    protected String location; // location attribute on the netcdf element

    // deferred opening
    protected String cacheName;
    protected NetcdfFileFactory reader;
    protected boolean enhance;

    // used by AggregatinOuterDimension
    protected int ncoord; // number of coordinates in outer dimension for this dataset; joinExisting
    protected String coordValue;  // if theres a coordValue on the netcdf element
    protected Date coordValueDate;  // if its a date
    private boolean isStringValued = false;
    private int aggStart = 0, aggEnd = 0; // index in aggregated dataset; aggStart <= i < aggEnd

    /**
     * For subclasses.
     *
     * @param location location attribute on the netcdf element
     */
    protected Dataset(String location) {
      this.location = (location == null) ? null : StringUtil.substitute(location, "\\", "/");
    }

    /**
     * Dataset constructor.
     * With this constructor, the actual opening of the dataset is deferred, and done by the reader.
     * Used with explicit netcdf elements, and scanned files.
     *
     * @param cacheName   a unique name to use for caching
     * @param location    attribute "location" on the netcdf element
     * @param ncoordS     attribute "ncoords" on the netcdf element
     * @param coordValueS attribute "coordValue" on the netcdf element
     * @param enhance     open dataset in enhance mode
     * @param reader      factory for reading this netcdf dataset; if null, use NetcdfDataset.open( location)
     */
    protected Dataset(String cacheName, String location, String ncoordS, String coordValueS, boolean enhance, NetcdfFileFactory reader) {
      this(location);
      this.cacheName = cacheName;
      this.coordValue = coordValueS;
      this.enhance = enhance;
      this.reader = (reader != null) ? reader : new PolymorphicReader();

      if ((type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING_ONE)) {
        this.ncoord = 1;
      } else if (ncoordS != null) {
        try {
          this.ncoord = Integer.parseInt(ncoordS);
        } catch (NumberFormatException e) {
          logger.error("bad ncoord attribute on dataset=" + location);
        }
      }

      if ((type == Type.JOIN_NEW) || (type == Type.JOIN_EXISTING_ONE) || (type == Type.FORECAST_MODEL_COLLECTION)) {
        if (coordValueS == null) {
          int pos = this.location.lastIndexOf("/");
          this.coordValue = (pos < 0) ? this.location : this.location.substring(pos + 1);
          this.isStringValued = true;
        } else {
          try {
            Double.parseDouble(coordValueS);
          } catch (NumberFormatException e) {
            this.isStringValued = true;
          }
        }
      }

      // allow coordValue attribute on JOIN_EXISTING, may be multiple values seperated by blanks or commas
      if ((type == Type.JOIN_EXISTING) && (coordValueS != null)) {
        StringTokenizer stoker = new StringTokenizer(coordValueS, " ,");
        this.ncoord = stoker.countTokens();
      }
    }

    /**
     * Get the coordinate value(s) as a String for this Dataset
     *
     * @return the coordinate value(s) as a String
     */
    public String getCoordValueString() {
      return coordValue;
    }

    /**
     * Get the coordinate value as a Date for this Dataset; may be null
     *
     * @return the coordinate value as a Date, or null
     */
    public Date getCoordValueDate() {
      return coordValueDate;
    }

    /**
     * Get the location of this Dataset
     *
     * @return the location of this Dataset
     */
    public String getLocation() {
      return location;
    }

    /**
     * Get number of coordinates in this Dataset.
     * If not already set, open the file and get it from the aggregation dimension.
     *
     * @param cancelTask allow cancellation
     * @return number of coordinates in this Dataset.
     * @throws java.io.IOException if io error
     */
    public int getNcoords(CancelTask cancelTask) throws IOException {
      if (ncoord <= 0) {
        NetcdfFile ncd = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel()) return 0;

        Dimension d = ncd.getRootGroup().findDimension(dimName);
        if (d != null)
          ncoord = d.getLength();
        ncd.close();
      }
      return ncoord;
    }

    /**
     * Set the starting and ending index into the aggregation dimension
     *
     * @param aggStart   starting index
     * @param cancelTask allow to bail out
     * @return number of coordinates in this dataset
     * @throws IOException if io error
     */
    protected int setStartEnd(int aggStart, CancelTask cancelTask) throws IOException {
      this.aggStart = aggStart;
      this.aggEnd = aggStart + getNcoords(cancelTask);
      return ncoord;
    }

    /**
     * Get the desired Range, reletive to this Dataset, if no overlap, return null.
     * <p> wantStart, wantStop are the indices in the aggregated dataset, wantStart <= i < wantEnd.
     * if this overlaps, set the Range required for the nested dataset.
     * note this should handle strides ok.
     *
     * @param totalRange desired range, reletive to aggregated dimension.
     * @return desired Range or null if theres nothing wanted from this datase.
     * @throws InvalidRangeException if invalid range request
     */
    protected Range getNestedJoinRange(Range totalRange) throws InvalidRangeException {
      int wantStart = totalRange.first();
      int wantStop = totalRange.last() + 1; // Range has last inclusive, we use last exclusive

      // see if this dataset is needed
      if (!isNeeded(wantStart, wantStop))
        return null;

      int firstInInterval = totalRange.getFirstInInterval(aggStart);
      if ((firstInInterval < 0) || (firstInInterval >= aggEnd))
        return null;

      int start = Math.max(aggStart, wantStart) - aggStart;
      int stop = Math.min(aggEnd, wantStop) - aggStart;

      return new Range(start, stop - 1, totalRange.stride()); // Range has last inclusive
    }

    protected boolean isNeeded(Range totalRange) {
      int wantStart = totalRange.first();
      int wantStop = totalRange.last() + 1; // Range has last inclusive, we use last exclusive
      return isNeeded(wantStart, wantStop);
    }

    // wantStart, wantStop are the indices in the aggregated dataset, wantStart <= i < wantEnd
    // find out if this overlaps this nested Dataset indices
    private boolean isNeeded(int wantStart, int wantStop) {
      if (wantStart >= wantStop)
        return false;
      if ((wantStart >= aggEnd) || (wantStop <= aggStart))
        return false;

      return true;
    }

    protected NetcdfFile acquireFile(CancelTask cancelTask) throws IOException {
      try {
        return _acquireFile(cancelTask);
      } catch (IOException ioe) {
        syncExtend(true); // LOOK data has changed, how to notify the user permanently?
        throw ioe;
      }
    }

    private NetcdfFile _acquireFile(CancelTask cancelTask) throws IOException {
      NetcdfFile ncfile;
      long start = System.currentTimeMillis();
      if (debugOpenFile) System.out.println(" try to acquire " + cacheName);
      if (enhance)
        ncfile = NetcdfDatasetCache.acquire(cacheName, -1, cancelTask, spiObject, (NetcdfDatasetFactory) reader);
      else
        ncfile = NetcdfFileCache.acquire(cacheName, -1, cancelTask, spiObject, reader);

      if (debugOpenFile) System.out.println(" acquire " + cacheName + " took " + (System.currentTimeMillis() - start));
      if (type == Type.JOIN_EXISTING)
        cacheCoordValues(ncfile);
      return ncfile;
    }

    protected void close() throws IOException {
    }

    private void cacheCoordValues(NetcdfFile ncfile) throws IOException {
      if (coordValue != null) return;

      Variable coordVar = ncfile.findVariable(dimName);
      if (coordVar != null) {
        Array data = coordVar.read();
        coordValue = data.toString();
      }
    }

    protected Array read(Variable mainv, CancelTask cancelTask) throws IOException {
      NetcdfFile ncd = null;
      try {
        ncd = acquireFile(cancelTask);

        if ((cancelTask != null) && cancelTask.isCancel())
          return null;

        Variable v = modifyVariable(ncd, mainv.getName());
        return v.read();

      } finally {
        if (ncd != null) ncd.close();
      }
    }

    protected Array read(Variable mainv, CancelTask cancelTask, List<Range> section) throws IOException, InvalidRangeException {
      NetcdfFile ncd = null;
      try {
        ncd = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel())
          return null;

        if (debugRead) {
          System.out.print("agg read " + ncd.getLocation() + " nested= " + getLocation());
          for (Range range : section)
            System.out.print(" " + range + ":");
          System.out.println("");
        }

        Variable v = modifyVariable(ncd, mainv.getName());

        // its possible that we are asking for more of the time coordinate than actually exists (fmrc ragged time)
        // so we need to read only what is there
        Range fullRange = v.getRanges().get(0);
        Range want = section.get(0);
        if (fullRange.last() < want.last()) {
          Range limitRange = new Range(want.first(), fullRange.last(), want.stride());
          section = new ArrayList<Range>(section); // make a copy
          section.set(0, limitRange);
        }

        return v.read(section);

      } finally {
        if (ncd != null) ncd.close();
      }
    }

    // Datasets with the same locations are equal
    public boolean equals(Object oo) {
      if (this == oo) return true;
      if (!(oo instanceof Dataset)) return false;
      Dataset other = (Dataset) oo;
      return location.equals(other.location);
    }

    public int hashCode() {
      return location.hashCode();
    }

    protected boolean checkOK(CancelTask cancelTask) {
      return true;  // LOOK ??
    }

    // allow subclasses to override
    protected Variable modifyVariable(NetcdfFile ncfile, String name) throws IOException {
      return ncfile.findVariable(name);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    class PolymorphicReader implements NetcdfFileFactory, NetcdfDatasetFactory {

      public NetcdfDataset openDataset(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws java.io.IOException {
        return NetcdfDataset.openDataset(location, true, buffer_size, cancelTask, spiObject);
      }

      public NetcdfFile open(String location, int buffer_size, ucar.nc2.util.CancelTask cancelTask, Object spiObject) throws IOException {
        return NetcdfDataset.openFile(location, buffer_size, cancelTask, spiObject);
      }
    }
  }


  /**
   * All non-agg variables use a proxy to acquire the file before reading.
   * If the variable is caching, read data into cache now.
   *
   * @param typicalDataset read from a "typical dataset"
   * @param newds containing dataset
   * @throws IOException on i/o error
   */
  protected void makeProxies(Dataset typicalDataset, NetcdfDataset newds) throws IOException {

    // all normal variables must use a proxy to lock the file
    DatasetProxyReader proxy = new DatasetProxyReader(typicalDataset);
    List<Variable> allVars = newds.getVariables();
    for (Variable v : allVars) {
      VariableEnhanced ve = (VariableEnhanced) v; // need this for getProxyReader2()
      if (ve.getProxyReader2() != null) {
        if (debugProxy) System.out.println(" debugProxy: hasProxyReader " + ve.getName());
        continue; // dont mess with agg variables
      }

      if (v.isCaching()) {  // cache the small ones
        if (!v.hasCachedData()) {
          ve.read();
          if (debugProxy) System.out.println(" debugProxy: cached " + ve.getName());
        } else {
          if (debugProxy) System.out.println(" debugProxy: already cached " + ve.getName());
        }

      } else if (null == ve.getProxyReader2()) { // put proxy on the rest
        ve.setProxyReader2(proxy);
        if (debugProxy) System.out.println(" debugProxy: proxy on " + ve.getName());
      }
    }
  }

  protected class DatasetProxyReader implements ProxyReader2 {
    Dataset dataset;

    DatasetProxyReader(Dataset dataset) {
      this.dataset = dataset;
    }

    public Array read(Variable mainV, CancelTask cancelTask) throws IOException {
      NetcdfFile ncfile = null;
      try {
        ncfile = dataset.acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel()) return null;
        Variable proxyV = ncfile.findVariable(mainV.getName());  // LOOK assumes they have the same name - may have been renamed in NcML!
        return proxyV.read();
      } finally {
        if (ncfile != null) ncfile.close();
      }
    }

    public Array read(Variable mainV, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
      NetcdfFile ncfile = null;
      try {
        ncfile = dataset.acquireFile(cancelTask);
        Variable proxyV = ncfile.findVariable(mainV.getName());
        if ((cancelTask != null) && cancelTask.isCancel()) return null;
        return proxyV.read(section);
      } finally {
        if (ncfile != null) ncfile.close();
      }
    }
  }


  /**
   * Argument to DatasetConstructor.transferDataset(ncfile, ncDataset, MyReplaceVariableCheck);
   * Used only on a rescan.
   * For JOIN_NEW, its replaced if its a listed aggregation variable
   * For JOIN_EXISTING, its replaced if its NOT an aggregation variable (!!??)
   */
  protected class MyReplaceVariableCheck implements ReplaceVariableCheck {
    public boolean replace(Variable v) {

      if (getType() == Type.JOIN_NEW) {
        return isAggVariable(v.getName());

      } else { // needs to be replaced if its not an agg variable
        if (v.getRank() < 1) return true;
        Dimension d = v.getDimension(0);
        return !getDimensionName().equals(d.getName());
      }
    }
  }

  /**
   * Is the named variable an "aggregation variable" ?
   *
   * @param name variable name
   * @return true if the named variable is an aggregation variable
   */
  private boolean isAggVariable(String name) {
    for (String vname : aggVarNames) {
      if (vname.equals(name))
        return true;
    }
    return false;
  }

}
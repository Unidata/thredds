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
import org.jdom.Element;

/**
 * Superclass for NcML Aggregation.
 *
 * @author caron
 */

/* May be out of date
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

 */
public abstract class Aggregation implements AggregationIF, ProxyReader {
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

  // experimental
  protected boolean timeUnitsChange = false;
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
   * @param sectionSpec attribute "section" on the netcdf element
   * @param reader      factory for reading this netcdf dataset
   * @param cancelTask  user may cancel, may be null
   */
  public void addExplicitDataset(String cacheName, String location, String ncoordS, String coordValueS, String sectionSpec,
                                 NetcdfFileFactory reader, CancelTask cancelTask) {
    Dataset nested = makeDataset(cacheName, location, ncoordS, coordValueS, sectionSpec, false, reader);
    explicitDatasets.add(nested);
  }

  public void addDataset(Dataset nested) {
    explicitDatasets.add(nested);
  }

  /**
   * Add a crawlable dataset scan
   *
   * @param crawlableDatasetElement defines a CrawlableDataset, or null
   * @param dirName             scan this directory
   * @param suffix              filter on this suffix (may be null)
   * @param regexpPatternString include if full name matches this regular expression (may be null)
   * @param dateFormatMark      create dates from the filename (may be null)
   * @param enhanceS            should files bne enhanced?
   * @param subdirs             equals "false" if should not descend into subdirectories
   * @param olderThan           files must be older than this time (now - lastModified >= olderThan); must be a time unit, may ne bull
   * @throws IOException if I/O error
   */
  public void addCrawlableDatasetScan(Element crawlableDatasetElement, String dirName, String suffix, String regexpPatternString, String dateFormatMark, String enhanceS, String subdirs, String olderThan) throws IOException {
    this.dateFormatMark = dateFormatMark;

    if ((enhanceS != null) && enhanceS.equalsIgnoreCase("true"))
      enhance = true;

    if (dateFormatMark != null) {
      isDate = true;
      if (type == Type.JOIN_EXISTING) type = Type.JOIN_EXISTING_ONE; // tricky
    }

    CrawlableScanner d = new CrawlableScanner(crawlableDatasetElement, dirName, suffix, regexpPatternString, subdirs, olderThan);
    datasetManager.addDirectoryScan(d);
  }

  /**
   * Get type of aggregation
   *
   * @return type of aggregation
   */
  public Type getType() {
    return type;
  }

  /**
   * Get dimension name to join on
   *
   * @return dimension name or null if type union/tiled
   */
  public String getDimensionName() {
    return dimName;
  }

  /**
   * Release all resources associated with the aggregation
   *
   * @throws IOException on error
   */
  public void close() throws IOException {
    persist();
    closeDatasets();
  }

  protected void closeDatasets() throws IOException {
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
    datasetManager.scan(cancelTask);
    cacheDirty = true;
    closeDatasets();
    makeDatasets(cancelTask);

    // check persistence info
    persistRead();

    //ucar.unidata.io.RandomAccessFile.setDebugAccess( true);
    buildDataset(cancelTask);
    //ucar.unidata.io.RandomAccessFile.setDebugAccess( false);
  }

  protected List<Dataset> getDatasets() {
    return datasets;
  }

  /**
   * Make the Dataset objects.
   *
   * @param cancelTask user can cancel
   * @throws IOException on i/o error
   */
  protected void makeDatasets(CancelTask cancelTask) throws IOException {
    Collection<MyCrawlableDataset> fileList = datasetManager.getFiles();
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

    // create new list of Datasets, transfer explicit first
    datasets = new ArrayList<Dataset>();
    for (Aggregation.Dataset dataset : explicitDatasets) {
      datasets.add(dataset);
    }

    // now add the ordered list of Datasets to the result List
    for (MyCrawlableDataset myf : fileList) {
      String location = myf.file.getPath();
      Aggregation.Dataset ds = makeDataset(location, location, null, myf.dateCoordS, null, enhance, null);
      ds.setInfo(myf);
      datasets.add(ds);
    }

    // Sort by date if it exists, else filename.
    Collections.sort(datasets, new Comparator<Aggregation.Dataset>() {
      public int compare(Aggregation.Dataset ds1, Aggregation.Dataset ds2) {
        if(ds1.cd == null)
           return ds1.getLocation().compareTo(ds2.getLocation()) ;
        if (ds1.cd.dateCoord != null) // LOOK can we generalize
          return ds1.cd.dateCoord.compareTo(ds2.cd.dateCoord);
        else
          return ds1.cd.file.getName().compareTo(ds2.cd.file.getName());
      }
    });

  }

  /**
   * Call this to build the dataset objects
   *
   * @param cancelTask maybe cancel
   * @throws IOException on read error
   */
  protected abstract void buildDataset(CancelTask cancelTask) throws IOException;


  /**
   * Call this when rescan has found changed datasets
   *
   * @throws IOException on read error
   */
  protected abstract void rebuildDataset() throws IOException;

  //////////////////////////////////////////////////////////////

  /**
   * Check to see if its time to rescan directory, and if so, rescan and extend dataset if needed.
   * Note that this just calls sync(), so structural metadata may be modified (!!)
   *
   * @param force if true, always rescan even if time not expired
   * @return true if directory was rescanned and dataset may have been updated
   * @throws IOException on io error
   */
  public synchronized boolean syncExtend(boolean force) throws IOException {
    if (!force && !datasetManager.timeToRescan())
      return false;

    return _sync();
  }

  public synchronized boolean sync() throws IOException {
    return datasetManager.timeToRescan() && _sync();
  }

  private boolean _sync() throws IOException {
    if (!datasetManager.rescan())
      return false; // nothing changed LOOK what about grib extention ??
    cacheDirty = true;
    closeDatasets();
    makeDatasets(null);

    // rebuild the metadata
    rebuildDataset();
    ncDataset.finish();
    if (ncDataset.getEnhanceMode() != NetcdfDataset.EnhanceMode.None) { // force recreation of the coordinate systems
      ncDataset.clearCoordinateSystems();
      ncDataset.enhance(ncDataset.getEnhanceMode());
      ncDataset.finish();
    }

    return true;
  }

  /////////////////////////////////////////////////////////////////////////


  protected String getLocation() {
    return ncDataset.getLocation();
  }
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
   * @param section    read just this section of the data, refers to aggregated Variable's section.
   * @return the data array section
   * @throws IOException
   */
  public abstract Array read(Variable mainv, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException;

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Dataset factory, so subclasses can override
   *
   * @param cacheName   a unique name to use for caching
   * @param location    attribute "location" on the netcdf element
   * @param ncoordS     attribute "ncoords" on the netcdf element
   * @param coordValueS attribute "coordValue" on the netcdf element
   * @param sectionSpec attribute "sectionSpec" on the netcdf element
   * @param enhance     open dataset in enhance mode
   * @param reader      factory for reading this netcdf dataset
   * @return a Aggregation.Dataset
   */
  protected Dataset makeDataset(String cacheName, String location, String ncoordS, String coordValueS, String sectionSpec, boolean enhance, NetcdfFileFactory reader) {
    //return new Dataset(cacheName, location, ncoordS, coordValueS, sectionSpec, enhance, reader);
    return new Dataset(cacheName, location, enhance, reader);
  }


  /**
   * Encapsolates a NetcdfFile that is a component of the aggregation.
   * public for NcMLWriter
   */
  class Dataset {
    protected String location; // location attribute on the netcdf element
    protected MyCrawlableDataset cd;

    // deferred opening
    protected String cacheName;
    protected NetcdfFileFactory reader;
    protected boolean enhance;

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
     * @param cacheName a unique name to use for caching
     * @param location  attribute "location" on the netcdf element
     * @param enhance   open dataset in enhance mode
     * @param reader    factory for reading this netcdf dataset; if null, use NetcdfDataset.open( location)
     */
    protected Dataset(String cacheName, String location, boolean enhance, NetcdfFileFactory reader) {
      this(location);
      this.cacheName = cacheName;
      this.enhance = enhance;
      this.reader = (reader != null) ? reader : new PolymorphicReader();
    }

    /**
     * Get the location of this Dataset
     *
     * @return the location of this Dataset
     */
    public String getLocation() {
      return location;
    }

    protected NetcdfFile acquireFile(CancelTask cancelTask) throws IOException {
      NetcdfFile ncfile;
      long start = System.currentTimeMillis();
      if (debugOpenFile) System.out.println(" try to acquire " + cacheName);
      if (enhance)
        ncfile = NetcdfDatasetCache.acquire(cacheName, -1, cancelTask, spiObject, (NetcdfDatasetFactory) reader);
      else
        ncfile = NetcdfFileCache.acquire(cacheName, -1, cancelTask, spiObject, reader);

      if (debugOpenFile) System.out.println(" acquire " + cacheName + " took " + (System.currentTimeMillis() - start));
      //if (type == Type.JOIN_EXISTING) // LOOK should others use this?
      //  cacheCoordValues(ncfile);
      // cacheVariables(ncfile); infinite loop

      return ncfile;
    }

    protected void close(NetcdfFile ncfile) throws IOException {
      if (ncfile == null) return;
      cacheVariables(ncfile);
      ncfile.close();
    }

        // overridden in DatasetOuterDimension
    protected void cacheVariables(NetcdfFile ncfile) throws IOException {
    }

    // overridden in DatasetOuterDimension
    protected void setInfo(MyCrawlableDataset cd) {
      this.cd = cd;
    }

    protected Array read(Variable mainv, CancelTask cancelTask) throws IOException {
      NetcdfFile ncd = null;
      try {
        ncd = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel())
          return null;

        Variable v = ncd.findVariable(mainv.getName());
        return v.read();

      } finally {
        close( ncd);
      }
    }

    /**
     * Read a section of the local Variable.
     *
     * @param mainv      aggregated Variable
     * @param cancelTask let user cancel
     * @param section    reletive to the local Variable
     * @return the complete Array for mainv
     * @throws IOException           on I/O error
     * @throws InvalidRangeException on section error
     */
    protected Array read(Variable mainv, CancelTask cancelTask, List<Range> section) throws IOException, InvalidRangeException {
      NetcdfFile ncd = null;
      try {
        ncd = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel())
          return null;

        if (debugRead)
          System.out.print("agg read " + ncd.getLocation() + " nested= " + getLocation() + " " + Range.toString(section));

        Variable v = ncd.findVariable(mainv.getName());
        return v.read(section);

      } finally {
        close( ncd);
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

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * All non-agg variables use a proxy to acquire the file before reading.
   * If the variable is caching, read data into cache now.
   *
   * @param typicalDataset read from a "typical dataset"
   * @param newds          containing dataset
   * @throws IOException on i/o error
   */
  protected void setDatasetAcquireProxy(Dataset typicalDataset, NetcdfDataset newds) throws IOException {

    // all normal (non agg) variables must use a proxy to lock the file
    DatasetProxyReader proxy = new DatasetProxyReader(typicalDataset);
    List<Variable> allVars = newds.getRootGroup().getVariables();
    for (Variable v : allVars) {
      VariableEnhanced ve = (VariableEnhanced) v; // need this for getProxyReader2()

      if (ve.getProxyReader() != null) {
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

      } else if (null == ve.getProxyReader()) { // put proxy on the rest
        ve.setProxyReader(proxy);
        if (debugProxy) System.out.println(" debugProxy: proxy on " + ve.getName());
      }
    }
  }

  protected class DatasetProxyReader implements ProxyReader {
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
        dataset.close( ncfile);
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
        dataset.close( ncfile);
      }
    }
  }

  /*
   * Argument to DatasetConstructor.transferDataset(ncfile, ncDataset, MyReplaceVariableCheck);
   * Used only on a rescan.
   * For JOIN_NEW, its replaced if its a listed aggregation variable
   * For JOIN_EXISTING, its replaced if its NOT an aggregation variable (!!??)
   *
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

   * Is the named variable an "aggregation variable" ?
   *
   * @param name variable name
   * @return true if the named variable is an aggregation variable
  private boolean isAggVariable(String name) {
    for (String vname : aggVarNames) {
      if (vname.equals(name))
        return true;
    }
    return false;
  } */

}
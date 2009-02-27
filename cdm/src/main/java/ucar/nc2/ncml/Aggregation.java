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
package ucar.nc2.ncml;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.dataset.*;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache2;
import ucar.unidata.util.StringUtil;

import java.util.*;
import java.util.concurrent.Executor;
import java.io.*;

import org.jdom.Element;
import thredds.crawlabledataset.CrawlableDataset;

/**
 * Superclass for NcML Aggregation.
 *
 * An Aggregation acts as a ProxyReader for VariableDS. That, is it must implement:
 * <pre>
 *   public Array read(Variable mainv);
 *   public Array read(Variable mainv, Section section);
 * </pre>
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
public abstract class Aggregation implements ProxyReader {

  static protected enum Type {
    forecastModelRunCollection,
    forecastModelRunSingleCollection,
    joinExisting,
    joinExistingOne, // joinExisting with a DateFormatMark makes it into a joinExistingOne - must have only one coord / file
    joinNew,
    tiled,
    union
  }

  static protected enum TypicalDataset {RANDOM, LATEST, PENULTIMATE }
  static protected TypicalDataset typicalDatasetMode;

  static protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Aggregation.class);
  static protected DiskCache2 diskCache2 = null;

  // this is where  persist() reads/writes files
  static public void setPersistenceCache(DiskCache2 dc) {
    diskCache2 = dc;
  }

  // experimental multithreading
  static protected Executor executor;
  static public void setExecutor(Executor exec) {
    executor = exec;
  }

  static public void setTypicalDatasetMode(String mode) {
    if (mode.equalsIgnoreCase("random"))
      typicalDatasetMode = TypicalDataset.RANDOM;
    else if (mode.equalsIgnoreCase("latest"))
      typicalDatasetMode = TypicalDataset.LATEST;
    else if (mode.equalsIgnoreCase("penultimate"))
      typicalDatasetMode = TypicalDataset.PENULTIMATE;
    else
      logger.error("Unknown setTypicalDatasetMode= " + mode);
  }


  static protected boolean debug = false, debugOpenFile = false, debugSyncDetail = false, debugProxy = false,
      debugRead = false, debugDateParse = false;

  //////////////////////////////////////////////////////////////////////////////////////////

  protected NetcdfDataset ncDataset; // the aggregation belongs to this dataset
  protected Type type; // the aggregation type
  protected Object spiObject; // pass to NetcdfFile.open()

  protected List<Aggregation.Dataset> explicitDatasets = new ArrayList<Aggregation.Dataset>(); // explicitly created Dataset objects from netcdf elements
  protected List<Aggregation.Dataset> datasets = new ArrayList<Aggregation.Dataset>(); // all : explicit and scanned
  protected DatasetCollectionManager datasetManager; // manages scanning
  protected boolean cacheDirty = true; // aggCache persist file needs updating

  protected String dimName; // the aggregation dimension name

  // experimental
  protected String dateFormatMark;
  protected EnumSet<NetcdfDataset.Enhance> enhance = null; // default no enhancement
  protected boolean isDate = false;
  protected DateFormatter dateFormatter = new DateFormatter();


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
   * @param id          attribute "id" on the netcdf element
   * @param ncoordS     attribute "ncoords" on the netcdf element
   * @param coordValueS attribute "coordValue" on the netcdf element
   * @param sectionSpec attribute "section" on the netcdf element
   * @param reader      factory for reading this netcdf dataset
   */
  public void addExplicitDataset(String cacheName, String location, String id, String ncoordS, String coordValueS, String sectionSpec,
                                 ucar.nc2.util.cache.FileFactory reader) {

    Dataset nested = makeDataset(cacheName, location, id, ncoordS, coordValueS, sectionSpec, null, reader);
    explicitDatasets.add(nested);
  }

  public void addDataset(Dataset nested) {
    explicitDatasets.add(nested);
  }

  /**
   * Add a dataset scan
   *
   * @param crawlableDatasetElement defines a CrawlableDataset, or null
   * @param dirName             scan this directory
   * @param suffix              filter on this suffix (may be null)
   * @param regexpPatternString include if full name matches this regular expression (may be null)
   * @param dateFormatMark      create dates from the filename (may be null)
   * @param mode                how should files be enhanced
   * @param subdirs             equals "false" if should not descend into subdirectories
   * @param olderThan           files must be older than this time (now - lastModified >= olderThan); must be a time unit, may ne bull
   */
  public void addDatasetScan(Element crawlableDatasetElement, String dirName, String suffix,
          String regexpPatternString, String dateFormatMark, EnumSet<NetcdfDataset.Enhance> mode, String subdirs, String olderThan) {
    this.dateFormatMark = dateFormatMark;
    this.enhance = mode;

    if (dateFormatMark != null) {
      isDate = true;
      if (type == Type.joinExisting) type = Type.joinExistingOne; // tricky
    }

    DatasetScanner d = new DatasetScanner(crawlableDatasetElement, dirName, suffix, regexpPatternString, subdirs, olderThan);
    datasetManager.addDirectoryScan(d);
  }

  private Element mergeNcml = null;
  public void setModifications(Element ncmlMods) {
    this.mergeNcml = ncmlMods;
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
    persistWrite();
    closeDatasets();
  }

  protected void closeDatasets() throws IOException {
    datasets = null;
  }

  public void getDetailInfo(Formatter f) {
    f.format("  Type=%s%n", type);
    f.format("  dimName=%s%n", dimName);
    f.format("  Datasets%n");
    for (Dataset ds : datasets)
      ds.show(f);
  }

  /**
   * Allow information to be make persistent. Overridden in AggregationExisting
   *
   * @throws IOException on error
   */
  public void persistWrite() throws IOException {
  }

  /**
   * read info from the persistent XML file, if it exists; overridden in AggregationExisting
   */
  protected void persistRead() {
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  // all elements are processed, finish construction

  public void finish(CancelTask cancelTask) throws IOException {
    datasetManager.scan(cancelTask); // Make the list of Datasets, by scanning if needed.
    cacheDirty = true;
    closeDatasets();
    makeDatasets(cancelTask);

    //ucar.unidata.io.RandomAccessFile.setDebugAccess( true);
    buildNetcdfDataset(cancelTask);
    //ucar.unidata.io.RandomAccessFile.setDebugAccess( false);
  }

  protected List<Dataset> getDatasets() {
    return datasets;
  }

  /**
   * Make the list of Datasets, from explicit and scans.
   *
   * @param cancelTask user can cancel
   * @throws IOException on i/o error
   */
  protected void makeDatasets(CancelTask cancelTask) throws IOException {

    // heres where the results will go
    datasets = new ArrayList<Dataset>();

    // convert a MyCrawlableDataset into an Aggregation.Dataset
    // we really just need the location, assumed to work in the FileFactory
    for (CrawlableDataset cd : datasetManager.getFiles()) {
      datasets.add( makeDataset(cd));
    }

    // sort using Aggregation.Dataset as Comparator.
    // Sort by date if it exists, else filename.
    Collections.sort(datasets);

      /* optionally extract the date
      String dateCoordS = null;
      if (null != dateFormatMark) {
        String filename = myf.getName(); // LOOK operates on name, not path
        Date dateCoord = DateFromString.getDateUsingDemarkatedCount(filename, dateFormatMark, '#');
        dateCoordS = formatter.toDateTimeStringISO(dateCoord);
        if (debugDateParse) System.out.println("  adding " + myf.getPath() + " date= " + dateCoordS);
      } else {
        if (debugDateParse) System.out.println("  adding " + myf.getPath());
      }

      String location = myf.getPath();
      Aggregation.Dataset ds = makeDataset(location, location, null, null, dateCoordS, null, enhance, null);
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
    });  */

    // add the explicit datasets - these need to be kept in order
    // LOOK - should they be before or after scanned? Does it make sense to mix scan and explicit?
    for (Aggregation.Dataset dataset : explicitDatasets) {
      datasets.add(dataset);
    }

    // check for duplicate location
    Set<String> dset = new HashSet<String>( 2 * datasets.size());
    for (Aggregation.Dataset dataset : datasets) {
      if (dset.contains(dataset.cacheLocation))
        logger.warn("Duplicate dataset in aggregation = "+dataset.cacheLocation);
      dset.add(dataset.cacheLocation);
    }
  }

  /**
   * Call this to build the dataset objects in the NetcdfDataset
   *
   * @param cancelTask maybe cancel
   * @throws IOException on read error
   */
  protected abstract void buildNetcdfDataset(CancelTask cancelTask) throws IOException;


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
    if (ncDataset.getEnhanceMode().contains(NetcdfDataset.Enhance.CoordSystems)) { // force recreation of the coordinate systems
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
    if (typicalDatasetMode == TypicalDataset.LATEST)
      select = n - 1;
    else if (typicalDatasetMode == TypicalDataset.PENULTIMATE)
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
   * @param id          attribute "id" on the netcdf element
   * @param ncoordS     attribute "ncoords" on the netcdf element
   * @param coordValueS attribute "coordValue" on the netcdf element
   * @param sectionSpec attribute "sectionSpec" on the netcdf element
   * @param enhance     open dataset in enhance mode
   * @param reader      factory for reading this netcdf dataset
   * @return a Aggregation.Dataset
   */
  protected Dataset makeDataset(String cacheName, String location, String id, String ncoordS, String coordValueS,
          String sectionSpec, EnumSet<NetcdfDataset.Enhance> enhance, ucar.nc2.util.cache.FileFactory reader) {
    return new Dataset(cacheName, location, id, enhance, reader); // overridden in OuterDim, tiled
  }

  protected Dataset makeDataset(CrawlableDataset dset) {
    return new Dataset(dset);
  }

  /**
   * Encapsolates a NetcdfFile that is a component of the aggregation.
   */
  class Dataset implements Comparable {
    protected String location; // location attribute on the netcdf element
    protected String id; // id attribute on the netcdf element

    // deferred opening
    protected String cacheLocation;
    protected ucar.nc2.util.cache.FileFactory reader;
    protected EnumSet<NetcdfDataset.Enhance> enhance;

    protected Object extraInfo;

    /**
     * For subclasses.
     *
     * @param location location attribute on the netcdf element
     */
    protected Dataset(String location) {
      this.location = (location == null) ? null : StringUtil.substitute(location, "\\", "/");
    }

    protected Dataset(CrawlableDataset cd) {
      this( cd.getPath());
      this.cacheLocation = location;
      this.enhance = Aggregation.this.enhance;
    }

    /**
     * Dataset constructor.
     * With this constructor, the actual opening of the dataset is deferred, and done by the reader.
     * Used with explicit netcdf elements, and scanned files.
     *
     * @param cacheLocation a unique name to use for caching
     * @param location  attribute "location" on the netcdf element
     * @param id  attribute "id" on the netcdf element
     * @param enhance   open dataset in enhance mode, may be null
     * @param reader    factory for reading this netcdf dataset; if null, use NetcdfDataset.open( location)
     */
    protected Dataset(String cacheLocation, String location, String id, EnumSet<NetcdfDataset.Enhance> enhance, ucar.nc2.util.cache.FileFactory reader) {
      this(location);
      this.cacheLocation = cacheLocation;
      this.id = id;
      this.enhance = enhance;
      this.reader = reader;
    }

    /**
     * Get the location of this Dataset
     *
     * @return the location of this Dataset
     */
    public String getLocation() {
      return location;
    }

    public String getId() {
      if (id != null) return id;
      if (location != null) return location;
      return null;
    }

    protected NetcdfFile acquireFile(CancelTask cancelTask) throws IOException {
      if (debugOpenFile) System.out.println(" try to acquire " + cacheLocation);
      long start = System.currentTimeMillis();

      NetcdfFile ncfile;
      // no enhance
      if (enhance == null || enhance.isEmpty()) {
        if (mergeNcml == null)
          ncfile = NetcdfDataset.acquireFile(reader, null, cacheLocation, -1, cancelTask, spiObject);
        else {
          ncfile = NetcdfDataset.acquireDataset(reader, cacheLocation, enhance, -1, cancelTask, spiObject);
          new NcMLReader().merge((NetcdfDataset) ncfile, mergeNcml);
        }

        // yes enhance
      } else {
        if (mergeNcml == null) {
          ncfile = NetcdfDataset.acquireDataset(reader, cacheLocation, enhance, -1, cancelTask, spiObject);
        } else {
          // wait till merge to enhance
          NetcdfDataset ncd = NetcdfDataset.acquireDataset(reader, cacheLocation, null, -1, cancelTask, spiObject);
          new NcMLReader().merge(ncd, mergeNcml);
          ncd.enhance( enhance);
          ncfile = ncd;
        }
      }

      if (debugOpenFile) System.out.println(" acquire " + cacheLocation + " took " + (System.currentTimeMillis() - start));
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

    public void show(Formatter f) {
      f.format("   %s%n", location);
    }

    protected Array read(Variable mainv, CancelTask cancelTask) throws IOException {
      NetcdfFile ncd = null;
      try {
        ncd = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel())
          return null;

        Variable v = findVariable(ncd, mainv);
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
          System.out.print("agg read " + ncd.getLocation() + " nested= " + getLocation() + " " + new Section(section));

        Variable v = findVariable(ncd, mainv);
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

    public int compareTo(Object o) {
      Dataset other = (Dataset) o;
      return location.compareTo( other.location);
    }
  } // class Dataset

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * All non-agg variables use a proxy to acquire the file before reading.
   * If the variable is caching, read data into cache now.
   * If not caching, VariableEnhanced.setProxyReader() is called.
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
      VariableEnhanced ve = (VariableEnhanced) v;

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
        if (debugProxy) System.out.println(" debugProxy: set proxy on " + ve.getName());
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
        Variable proxyV = findVariable(ncfile, mainV);
        return proxyV.read();
      } finally {
        dataset.close( ncfile);
      }
    }

    public Array read(Variable mainV, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException {
      NetcdfFile ncfile = null;
      try {
        ncfile = dataset.acquireFile(cancelTask);
        Variable proxyV = findVariable(ncfile, mainV);
        if ((cancelTask != null) && cancelTask.isCancel()) return null;
        return proxyV.read(section);
      } finally {
        dataset.close( ncfile);
      }
    }
  }

  protected Variable findVariable(NetcdfFile ncfile, Variable mainV) {
    Variable v = ncfile.findVariable(mainV.getName());
    if (v == null) {  // might be renamed
      VariableEnhanced ve = (VariableEnhanced) mainV;
      v = ncfile.findVariable(ve.getOriginalName());
    }
    return v;
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
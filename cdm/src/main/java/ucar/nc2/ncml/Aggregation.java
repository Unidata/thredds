/*
 * Copyright (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
 */
package ucar.nc2.ncml;

import org.jdom2.Element;
import thredds.filesystem.MFileOS;
import thredds.filesystem.MFileOS7;
import thredds.inventory.DateExtractor;
import thredds.inventory.DateExtractorFromName;
import thredds.inventory.MFile;
import thredds.inventory.MFileCollectionManager;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ProxyReader;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.DiskCache2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executor;

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
public abstract class Aggregation {

  static protected enum Type {
    forecastModelRunCollection,
    forecastModelRunSingleCollection,
    joinExisting,
    joinExistingOne, // joinExisting with a DateFormatMark makes it into a joinExistingOne - must have only one coord / file
    joinNew,
    tiled,
    union
  }

  static protected enum TypicalDataset {FIRST, RANDOM, LATEST, PENULTIMATE }
  static protected TypicalDataset typicalDatasetMode;

  static protected org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Aggregation.class);
  static protected DiskCache2 diskCache2 = null;

  // this is where  persist() reads/writes files
  static public void setPersistenceCache(DiskCache2 dc) {
    diskCache2 = dc;
    if (diskCache2 != null)
      diskCache2.setAlwaysUseCache(true); // the persistence cache file has same name as the ncml - must put it into the cache else clobber ncml  7/31/2014
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
    else if (mode.equalsIgnoreCase("first"))
       typicalDatasetMode = TypicalDataset.FIRST;
     else
      logger.error("Unknown setTypicalDatasetMode= " + mode);
  }

  static protected boolean debug = false, debugOpenFile = false, debugSyncDetail = false, debugProxy = false,
      debugRead = false, debugDateParse = false, debugConvert = false;

  //////////////////////////////////////////////////////////////////////////////////////////

  protected NetcdfDataset ncDataset; // the aggregation belongs to this dataset
  protected Type type; // the aggregation type
  protected Object spiObject; // pass to NetcdfFile.open()

  protected List<Aggregation.Dataset> explicitDatasets = new ArrayList<>(); // explicitly created Dataset objects from netcdf elements
  protected List<Aggregation.Dataset> datasets = new ArrayList<>(); // all : explicit and scanned
  protected MFileCollectionManager datasetManager; // manages scanning
  protected boolean cacheDirty = true; // aggCache persist file needs updating

  protected String dimName; // the aggregation dimension name

  private Element mergeNcml = null;

  // experimental
  protected String dateFormatMark;
  //protected EnumSet<NetcdfDataset.Enhance> enhance = null; // default no enhancement
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
    String name = ncd.getLocation();
    if (name == null) name = "Agg-"+Integer.toString(ncd.hashCode());
    datasetManager = MFileCollectionManager.openWithRecheck(name, recheckS);
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
   * @param enhanceMode         how should files be enhanced
   * @param subdirs             equals "false" if should not descend into subdirectories
   * @param olderThan           files must be older than this time (now - lastModified >= olderThan); must be a time unit, may ne bull
   */
  public void addDatasetScan(Element crawlableDatasetElement, String dirName, String suffix,
          String regexpPatternString, String dateFormatMark, Set<NetcdfDataset.Enhance> enhanceMode, String subdirs, String olderThan) {

    datasetManager.addDirectoryScan(dirName, suffix, regexpPatternString, subdirs, olderThan, enhanceMode);

    this.dateFormatMark = dateFormatMark;
    if (dateFormatMark != null) {
      isDate = true;
      if (type == Type.joinExisting) type = Type.joinExistingOne; // tricky
      DateExtractor dateExtractor = new DateExtractorFromName(dateFormatMark, true);
      datasetManager.setDateExtractor(dateExtractor);
    }
 }

  // experimental
  public void addCollection(String spec, String olderThan) throws IOException {
    datasetManager = MFileCollectionManager.open(spec, spec, olderThan, new Formatter());
 }

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


  protected String getLocation() {
    return ncDataset.getLocation();
  }

  /////////////////////////////////////////////////////////////////////

  public void close() throws IOException {
    persistWrite();
  }

  /**
   * Check to see if its time to rescan directory, and if so, rescan and extend dataset if needed.
   * Note that this just calls sync(), so structural metadata may be modified (!!)
   *
   * @return true if directory was rescanned and dataset may have been updated
   * @throws IOException on io error
   */
  public synchronized boolean syncExtend() throws IOException {
    return datasetManager.isScanNeeded() && _sync();
  }

  //public synchronized boolean sync() throws IOException {
  //  return datasetManager.isScanNeeded() && _sync();
  //}

  // LOOK could also use syncExtend()
  public long getLastModified() {
    try {
      datasetManager.scanIfNeeded();
    } catch (IOException e) {
      logger.error("Aggregation scan failed, e");
    }
    return datasetManager.getLastChanged();
  }

  private boolean _sync() throws IOException {
    if (!datasetManager.scan(true))
      return false; // nothing changed LOOK what about grib extention ??
    cacheDirty = true;
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

  public String getFileTypeId() { // LOOK - should cache ??
     Dataset ds = null;
     NetcdfFile ncfile = null;
     try {
       ds = getTypicalDataset();
       ncfile = ds.acquireFile(null);
       return ncfile.getFileTypeId();

     } catch (Exception e) {
       logger.error("failed to open "+ds);

     } finally {
       if (ds != null) try {
         ds.close(ncfile);
       } catch (IOException e) {
         logger.error("failed to close "+ds);
  }
     }
     return "N/A";
   }

   public String getFileTypeDescription() { // LOOK - should cache ??
     Dataset ds = null;
     NetcdfFile ncfile = null;
     try {
       ds = getTypicalDataset();
       ncfile = ds.acquireFile(null);
       return ncfile.getFileTypeDescription();

     } catch (Exception e) {
       logger.error("failed to open "+ds);

     } finally {
       if (ds != null) try {
         ds.close(ncfile);
       } catch (IOException e) {
         logger.error("failed to close "+ds);
       }
     }
     return "N/A";
   }


  ///////////////////////////////////////////////////////////////////////////////////////////////////////
  // stuff for subclasses to override

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

  public void getDetailInfo(Formatter f) {
    f.format("  Type=%s%n", type);
    f.format("  dimName=%s%n", dimName);
    f.format("  Datasets (%d) %n", datasets.size());
    for (Dataset ds : datasets)
      ds.show(f);
  }


  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

  // all elements are processed, finish construction

  public void finish(CancelTask cancelTask) throws IOException {
    datasetManager.scan(true); // Make the list of Datasets, by scanning if needed.
    cacheDirty = true;
    makeDatasets(cancelTask);

    //ucar.unidata.io.RandomAccessFile.setDebugAccess( true);
    buildNetcdfDataset(cancelTask);
    //ucar.unidata.io.RandomAccessFile.setDebugAccess( false);
  }

  public List<Dataset> getDatasets() {
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
    datasets = new ArrayList<>();

    for (MFile cd : datasetManager.getFilesSorted()) {
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
    // AggFmrcSingle sets explicit datasets - the scan is empty
    for (Aggregation.Dataset dataset : explicitDatasets) {
      datasets.add(dataset);
    }

    // Remove unreadable files (i.e. due to permissions) from the aggregation.
    // LOOK: Is this logic we should install "upstream", perhaps in MFileCollectionManager?
    // It would affect other collections than just NcML aggregation in that case.
    for (Iterator<Dataset> datasetsIter = datasets.iterator(); datasetsIter.hasNext(); ) {
      Dataset dataset = datasetsIter.next();

      Path datasetPath;
      if (dataset.getMFile() instanceof MFileOS) {
        datasetPath = ((MFileOS) dataset.getMFile()).getFile().toPath();
      } else if (dataset.getMFile() instanceof MFileOS7) {
        datasetPath = ((MFileOS7) dataset.getMFile()).getNioPath();
      } else {
        continue;
      }

      if (!Files.isReadable(datasetPath)) {  // File.canRead() is broken on Windows, but the JDK7 methods work.
        logger.warn("Aggregation member isn't readable (permissions issue?). Skipping: " + datasetPath);
        datasetsIter.remove();
      }
    }

    // check for duplicate location
    Set<String> dset = new HashSet<>( 2 * datasets.size());
    for (Aggregation.Dataset dataset : datasets) {
      if (dset.contains(dataset.cacheLocation))
        logger.warn("Duplicate dataset in aggregation = "+dataset.cacheLocation);
      dset.add(dataset.cacheLocation);
    }

    if (datasets.size() == 0) {
      throw new IllegalStateException("There are no datasets in the aggregation " + datasetManager);
    }
  }

  /**
   * Open one of the nested datasets as a template for the aggregation dataset.
   *
   * @return a typical Dataset
   * @throws IOException if there are no datasets
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
    else if (typicalDatasetMode == TypicalDataset.FIRST)
      select = 0;
    else { // random is default
      if (r == null) r = new Random();
      select = (n < 2) ? 0 : r.nextInt(n);
    }

    return nestedDatasets.get(select);
  }
  private Random r;


  //////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Read an aggregation variable: A variable whose data spans multiple files.
   *
   * @param mainv      the aggregation variable
   * @param cancelTask allow the user to cancel
   * @return the data array
   * @throws IOException
   */
  //public abstract Array read(Variable mainv, CancelTask cancelTask) throws IOException;
  //public abstract Array reallyRead() throws IOException;

  /**
   * Read a section of an aggregation variable.
   *
   * @param mainv      the aggregation variable
   * @param cancelTask allow the user to cancel
   * @param section    read just this section of the data, refers to aggregated Variable's section.
   * @return the data array section
   * @throws IOException
   */
  //public abstract Array read(Variable mainv, Section section, CancelTask cancelTask) throws IOException, InvalidRangeException;
  //public abstract Array reallyRead(Section section) throws IOException, InvalidRangeException;

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
   * @param enhance     open dataset in enhance mode NOT USED
   * @param reader      factory for reading this netcdf dataset
   * @return a Aggregation.Dataset
   */
  protected Dataset makeDataset(String cacheName, String location, String id, String ncoordS, String coordValueS,
          String sectionSpec, EnumSet<NetcdfDataset.Enhance> enhance, ucar.nc2.util.cache.FileFactory reader) {
    return new Dataset(cacheName, location, id, enhance, reader); // overridden in OuterDim, tiled
  }

  protected Dataset makeDataset(MFile dset) {
    return new Dataset(dset);
  }

  /**
   * Encapsolates a NetcdfFile that is a component of the aggregation.
   */
  public class Dataset implements Comparable {
    MFile mfile;
    // protected final String location; // location attribute on the netcdf element
    protected String id; // id attribute on the netcdf element

    // deferred opening
    protected String cacheLocation;
    protected ucar.nc2.util.cache.FileFactory reader;
    protected Set<NetcdfDataset.Enhance> enhance; // used by Fmrc to read enhanced datasets

    protected Object extraInfo;

    /*
     * For subclasses.
     *
     * @param location location attribute on the netcdf element
     *
    protected Dataset(String location) {
      this.location = (location == null) ? null : StringUtil2.substitute(location, "\\", "/");
    }  */

    protected Dataset(MFile mfile) {
      this.mfile = mfile;
      this.cacheLocation = mfile.getPath();
      this.enhance = (Set<NetcdfDataset.Enhance>) mfile.getAuxInfo();
    }

    /**
     * Dataset constructor.
     * With this constructor, the actual opening of the dataset is deferred, and done by the reader.
     * Used with explicit netcdf elements, and scanned files.
     *
     * @param cacheLocation a unique name to use for caching
     * @param location  attribute "location" on the netcdf element
     * @param id  attribute "id" on the netcdf element
     * @param enhance   open dataset in enhance mode, may be null NOT USED
     * @param reader    factory for reading this netcdf dataset; if null, use NetcdfDataset.open( location)
     */
    protected Dataset(String cacheLocation, String location, String id, EnumSet<NetcdfDataset.Enhance> enhance, ucar.nc2.util.cache.FileFactory reader) {
      this.mfile = MFileOS.getExistingFile(location);
      this.cacheLocation = cacheLocation;
      this.id = id;
      //this.enhance = enhance;  // LOOK why ??
      this.reader = reader;
    }

    /**
     * Get the location of this Dataset
     *
     * @return the location of this Dataset
     */
    public String getLocation() {
      return (mfile == null) ? cacheLocation : mfile.getPath();
    }

    /**
     *
     * @return MFile or null
     */
    public MFile getMFile() {
      return mfile;
    }

    public String getCacheLocation() {
      return cacheLocation;
    }

    public String getId() {
      if (id != null) return id;
      if (mfile != null) return mfile.getPath();
      return Integer.toString(this.hashCode());
    }

    public NetcdfFile acquireFile(CancelTask cancelTask) throws IOException {
      if (debugOpenFile) System.out.println(" try to acquire " + cacheLocation);
      long start = System.currentTimeMillis();

      NetcdfFile ncfile = NetcdfDataset.acquireFile(reader, null, cacheLocation, -1, cancelTask, spiObject);

      // must merge NcML before enhancing
      if (mergeNcml != null)
        ncfile = NcMLReader.mergeNcML(ncfile, mergeNcml); // create new dataset
      if (enhance == null || enhance.isEmpty()) {
        if (debugOpenFile) System.out.println(" acquire (no enhance) " + cacheLocation + " took " + (System.currentTimeMillis() - start));
        return ncfile;
      }

      // must enhance
      NetcdfDataset ds;
      if (ncfile instanceof NetcdfDataset) {
        ds = (NetcdfDataset) ncfile;
        ds.enhance(enhance); // enhance "in place", ie modify the NetcdfDataset
      } else {
        ds = new NetcdfDataset(ncfile, enhance); // enhance when wrapping
      }

      if (debugOpenFile) System.out.println(" acquire (enhance) " + cacheLocation + " took " + (System.currentTimeMillis() - start));
      return ds;
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
      f.format("   %s%n", mfile.getPath());
    }

    protected Array read(Variable mainv, CancelTask cancelTask) throws IOException {
      NetcdfFile ncd = null;
      try {
        ncd = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel())
          return null;

        Variable v = findVariable(ncd, mainv);
        if (debugRead)
          System.out.printf("Agg.read %s from %s in %s%n", mainv.getNameAndDimensions(), v.getNameAndDimensions(), getLocation());
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
    protected Array read(Variable mainv, CancelTask cancelTask, List<Range> section) throws  IOException, InvalidRangeException {
      NetcdfFile ncd = null;
      try {
        ncd = acquireFile(cancelTask);
        if ((cancelTask != null) && cancelTask.isCancel())
          return null;

        Variable v = findVariable(ncd, mainv);
        if (debugRead) {
          Section want = new Section(section);
          System.out.printf("Agg.read(%s) %s from %s in %s%n", want, mainv.getNameAndDimensions(), v.getNameAndDimensions(), getLocation());
        }

        return v.read(section);

      } finally {
        close( ncd);
      }
    }

    protected Variable findVariable(NetcdfFile ncfile, Variable mainV) {
      Variable v = ncfile.findVariable(mainV.getFullNameEscaped());
      if (v == null) {  // might be renamed
        VariableEnhanced ve = (VariableEnhanced) mainV;
        v = ncfile.findVariable(ve.getOriginalName()); // LOOK not escaped
      }
      return v;
    }

    // Datasets with the same locations are equal
    public boolean equals(Object oo) {
      if (this == oo) return true;
      if (!(oo instanceof Dataset)) return false;
      Dataset other = (Dataset) oo;
      return getLocation().equals(other.getLocation());
    }

    public int hashCode() {
      return getLocation().hashCode();
    }

    @Override
    public int compareTo(Object o) {
      Dataset other = (Dataset) o;
      return getLocation().compareTo( other.getLocation());
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
    DatasetProxyReader proxy = new DatasetProxyReader(typicalDataset);
    setDatasetAcquireProxy( proxy, newds.getRootGroup());
  }

  protected void setDatasetAcquireProxy(DatasetProxyReader proxy, Group g) throws IOException {

    // all normal (non agg) variables must use a proxy to lock the file
    for (Variable v : g.getVariables()) {

      if (v.getProxyReader() != v) {
        if (debugProxy) System.out.println(" debugProxy: hasProxyReader " + v.getFullName());
        continue; // dont mess with agg variables
      }

      if (v.isCaching()) {  // cache the small ones
          v.setCachedData( v.read()); // cache the variableDS directly

      } else { // put proxy on the rest
        v.setProxyReader(proxy);
        if (debugProxy) System.out.println(" debugProxy: set proxy on " + v.getFullName());
      }
    }

    // recurse
    for (Group nested : g.getGroups()) {
      setDatasetAcquireProxy(proxy, nested);
    }
  }


  protected class DatasetProxyReader implements ProxyReader {
    Dataset dataset;

    DatasetProxyReader(Dataset dataset) {
      this.dataset = dataset;
    }

    @Override
    public Array reallyRead(Variable mainV, CancelTask cancelTask) throws IOException {
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

    @Override
    public Array reallyRead(Variable mainV, Section section, CancelTask cancelTask) throws  IOException, InvalidRangeException {
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
    Variable v = ncfile.findVariable(mainV.getFullNameEscaped());
    if (v == null) {  // might be renamed
      VariableEnhanced ve = (VariableEnhanced) mainV;
      v = ncfile.findVariable(ve.getOriginalName()); // LOOK not escaped
    }
    return v;
  }

}

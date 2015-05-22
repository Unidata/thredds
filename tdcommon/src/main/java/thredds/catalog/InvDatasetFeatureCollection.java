/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package thredds.catalog;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.*;
import thredds.inventory.MCollection;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.util.log.LoggerFactory;
import ucar.nc2.util.log.LoggerFactoryImpl;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Abstract superclass for Feature Collection InvDatasets.
 * This is a InvCatalogRef subclass. So the reference is placed in the parent, but
 *  the catalog itself isnt constructed until the following call from DataRootHandler.makeDynamicCatalog():
 *   match.dataRoot.featCollection.makeCatalog(match.remaining, path, baseURI);
 * <p/>
 * The InvDatasetFeatureCollection object is created once and held in the DataRootHandler's collection of DataRoots.
 *
 * @author caron
 * @since Mar 3, 2010
 */
@ThreadSafe
public abstract class InvDatasetFeatureCollection extends InvCatalogRef implements CollectionUpdateListener {
  static protected final String LATEST_DATASET_CATALOG = "latest.xml";
  static protected final String LATEST_SERVICE = "latest";
  static protected final String VARIABLES = "?metadata=variableMap";
  static protected final String FILES = "files";
  static protected final String Virtual_Services = "VirtualServices"; // exclude HTTPServer
  static protected final String Default_Services = "DefaultServices";
  static protected final String Download_Services = InvService.fileServer.getName();

  static private String catalogServletName = "/catalog";            // LOOK
  static protected String context = "/thredds";                     // LOOK
  static private String cdmrFeatureServiceUrlPath = "/cdmrFeature"; // LOOK

  static private LoggerFactory loggerFactory = new LoggerFactoryImpl();
  static private org.slf4j.Logger initLogger = org.slf4j.LoggerFactory.getLogger(InvDatasetFeatureCollection.class.getName() + ".catalogInit");

  static public void setContext(String c) {
    context = c;
  }

  static public void setCatalogServletName(String catServletName) {
    catalogServletName = catServletName;
  }

  static protected String buildCatalogServiceHref(String path) {
    return context + (catalogServletName == null ? "" : catalogServletName) + "/" + path + "/catalog.xml";
  }

  static public void setCdmrFeatureServiceUrlPath(String urlPath) {
    cdmrFeatureServiceUrlPath = urlPath;
  }

  static public void setLoggerFactory(LoggerFactory fac) {
    loggerFactory = fac;
  }

  static private InvService makeCdmrFeatureService() {
    return new InvService("cdmrFeature", "cdmrFeature", context + cdmrFeatureServiceUrlPath, null, null);
  }

  static public InvDatasetFeatureCollection factory(InvDatasetImpl parent, FeatureCollectionConfig config) {
    InvDatasetFeatureCollection result;
    if (config.type == FeatureCollectionType.FMRC)
      result = new InvDatasetFcFmrc(parent, config);

    else if (config.type == FeatureCollectionType.GRIB1 || config.type == FeatureCollectionType.GRIB2) {
      // use reflection to decouple from grib.jar
      try {
        Class c = InvDatasetFeatureCollection.class.getClassLoader().loadClass("thredds.catalog.InvDatasetFcGrib");
        Constructor ctor = c.getConstructor(InvDatasetImpl.class, FeatureCollectionConfig.class);
        result = (InvDatasetFeatureCollection) ctor.newInstance(parent, config);

      } catch (Throwable e) {
        initLogger.error("Failed to open " + config.collectionName + " path=" + config.path, e);
        return null;
      }

    } else {
      result = new InvDatasetFcPoint(parent, config);
    }

    result.finishConstruction(); // stuff that shouldnt be done in a constructor
    return result;
  }

  /////////////////////////////////////////////////////////////////////////////
  // heres how we manage state changes in a thread-safe way
  protected class State {
    // catalog metadata
    protected ThreddsMetadata.Variables vars;
    protected ThreddsMetadata.GeospatialCoverage coverage;
    protected CalendarDateRange dateRange;

    protected InvDatasetImpl top;        // top dataset
    protected long lastInvChange;        // last time dataset inventory was changed
    protected long lastProtoChange;      // last time proto dataset was changed

    protected State(State from) {
      if (from != null) {
        this.vars = from.vars;
        this.coverage = from.coverage;
        this.dateRange = from.dateRange;
        this.lastProtoChange = from.lastProtoChange;

        this.top = from.top;
        this.lastInvChange = from.lastInvChange;
      }
    }

    protected State copy() {  // allow override
      return new State(this);
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // not changed after first call
  protected InvService orgService, virtualService;
  protected InvService cdmrService;  // LOOK why do we need to specify this seperately ??
  protected org.slf4j.Logger logger;

  // from the config catalog
  protected final String configPath;
  protected final FeatureCollectionType fcType;
  protected final FeatureCollectionConfig config;
  protected String topDirectory;
  protected MFileCollectionManager datasetCollection; // defines the collection of datasets in this feature collection, actually final NOT USED BY GRIB

  @GuardedBy("lock")
  protected State state;
  @GuardedBy("lock")
  protected boolean first = true;
  protected final Object lock = new Object();

  protected InvDatasetFeatureCollection(InvDatasetImpl parent, FeatureCollectionConfig config) {
    super(parent, config.name, buildCatalogServiceHref(config.path));
    this.configPath = config.path;
    this.fcType = config.type;
    this.config = config;

    this.getLocalMetadataInheritable().setDataType(fcType.getFeatureType());
    this.logger = loggerFactory.getLogger("fc." + config.collectionName); // seperate log file for each feature collection
    this.logger.info("FeatureCollection added = {}", getConfig());
  }

  protected void makeCollection() {

    Formatter errlog = new Formatter();
    datasetCollection = new MFileCollectionManager(config, errlog, this.logger);

    /*if (config.spec != null && config.spec.startsWith(MFileCollectionManager.CATALOG)) { // LOOK CHANGE THIS
      datasetCollection = new CollectionManagerCatalog(config.collectionName, config.spec, null, errlog);
    } else {
      datasetCollection = new MFileCollectionManager(config, errlog, this.logger);
    } */
    topDirectory = datasetCollection.getRoot();
    String errs = errlog.toString();
    if (errs.length() > 0) logger.warn("MFileCollectionManager parse error = {} ", errs);
  }

  // stuff that shouldnt be done in a constructor - eg dont let 'this' escape
  // LOOK maybe not best design to start tasks from here
  // LOOK we want to get notified of events, but no longer initiate changes.
  protected void finishConstruction() {
    CollectionUpdater.INSTANCE.scheduleTasks(config, this, logger);
  }

  public String getCollectionName() {
    return config.collectionName;
  }

  // CollectionUpdater sends this message asynchronously
  @Override
  public void sendEvent(CollectionUpdateType type) {
    try {
      update(type);
    } catch (IOException e) {
      logger.error("Error processing event", e);
    }
  }

  public void showStatus(Formatter f) {
    try {
      checkState();
      _showStatus(f, false, null);

    } catch (Throwable t) {
      StringWriter sw = new StringWriter(5000);
      t.printStackTrace(new PrintWriter(sw));
      f.format(sw.toString());
    }
  }

  public String showStatusShort(String type) {
    Formatter f = new Formatter();
    try {
      checkState();
      _showStatus(f, true, type);

    } catch (Throwable t) {
      StringWriter sw = new StringWriter(5000);
      t.printStackTrace(new PrintWriter(sw));
      f.format(sw.toString());
    }

    return f.toString();
  }

  protected void _showStatus(Formatter f, boolean summaryOnly, String type) throws IOException {
  }

  // localState is synched, may be directly changed
  abstract protected void updateCollection(State localState, CollectionUpdateType force);

  abstract protected void makeDatasetTop(State localState) throws IOException;

  // this allows us to put warnings into the catalogInit.log
  @Override
  boolean check(StringBuilder out, boolean show) {
    boolean isValid = true;

    // no longer need default service for FC 3/11/14
    /* if (getServiceDefault() == null) {
      out.append("**Warning: Dataset (").append(getFullName()).append("): has no default service\n");
      isValid = false;
    }  */

    return isValid && super.check(out, show);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  protected String getCatalogHref(String what) {
    return buildCatalogServiceHref(configPath + "/" + what);
  }

  // call this first time a request comes in
  protected void firstInit() {
    this.orgService = getServiceDefault();
    if (this.orgService == null) {
      this.orgService = makeDefaultService();
    }
    this.virtualService = makeServiceVirtual(this.orgService);
    this.cdmrService = makeCdmrFeatureService(); // WTF ??
  }

  /**
   * A request has come in, check that the state has been initialized.
   * this is called from the request thread.
   *
   * @return a copy of the State
   */
  protected State checkState() throws IOException {
    State localState;

    synchronized (lock) {
      if (first) {
        firstInit();
        updateCollection(state, config.updateConfig.updateType);
        makeDatasetTop(state);
        first = false;
      }
      localState = state.copy();
    }

    return localState;
  }

  /**
   * Collection was changed, update internal objects.
   * called by CollectionUpdater, trigger via handleCollectionEvent, so in a quartz scheduler thread
   *
   * @param force update type
   */
  protected void update(CollectionUpdateType force) throws IOException {  // this may be called from a background thread, or from checkState() request thread
    State localState;

    synchronized (lock) {
      if (first) {
        state = checkState();
        state.lastInvChange = System.currentTimeMillis();
        return;
      }
      // do the update in a local object
      localState = state.copy();
    }

    updateCollection(localState, force);
    makeDatasetTop(localState);
    localState.lastInvChange = System.currentTimeMillis();

    // switch to live
    synchronized (lock) {
      state = localState;
    }

  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String getPath() {
    return configPath;
  }

  public String getLatestFileName() {
    if (config.gribConfig.latestNamer != null) {
      return config.gribConfig.latestNamer;
    } else {
      return "Latest "+name+" File";
    }
  }

  public String getTopDirectoryLocation() {
    return topDirectory;
  }

  public FeatureCollectionConfig getConfig() {
    return config;
  }

  public MCollection getDatasetCollectionManager() {
    return datasetCollection;
  }

  public Logger getLogger() {
    return logger;
  }

  @Override
  public java.util.List<InvDataset> getDatasets() { // probably not used
    State localState;
    try {
      localState = checkState();
    } catch (Exception e) {
      logger.error("Error in checkState", e);
      return new ArrayList<>(0);
    }
    List<InvDataset> tops = new ArrayList<>(1);
    tops.add(localState.top);
    return tops;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////
  protected InvService makeDefaultService() {

    // LOOK need (thredds.server.config.AllowableService)
    InvService result = new InvService(Default_Services, ServiceType.COMPOUND.toString(), null, null, null);
    result.addService(InvService.opendap);
    result.addService(InvService.fileServer);
    result.addService(InvService.wms);
    result.addService(InvService.wcs);
    result.addService(InvService.ncss);
    result.addService(InvService.cdmremote);
    result.addService(InvService.ncml);
    result.addService(InvService.uddc);
    result.addService(InvService.iso);
    return result;
  }

  protected InvService makeDownloadService() {
     return InvService.fileServer;
   }

   protected InvService makeServiceVirtual(InvService org) {
    if (org.getServiceType() != ServiceType.COMPOUND) return org;

    InvService result = new InvService(Virtual_Services, ServiceType.COMPOUND.toString(), null, null, null);
    for (InvService service : org.getServices()) {
      if (service.getServiceType() != ServiceType.HTTPServer) {
        result.addService(service);
      }
    }
    return result;
  }

  /**
   * Get one of the catalogs contained in this collection,
   * called by DataRootHandler.makeDynamicCatalog()
   *
   * @param match   match.remaining
   * @param orgPath the path for the request.
   * @param catURI  the base URI for the catalog to be made, used to resolve relative URLs.
   * @return containing catalog
   */
  abstract public InvCatalogImpl makeCatalog(String match, String orgPath, URI catURI) throws IOException;

  /**
   * Make the containing catalog of this feature collection
   * "http://server:port/thredds/catalog/path/catalog.xml"
   *
   * @param catURI     base URI of the request
   * @param localState current state to use
   * @return the top FMRC catalog
   * @throws java.io.IOException         on I/O error
   * @throws java.net.URISyntaxException if path is misformed
   */
  protected InvCatalogImpl makeCatalogTop(URI catURI, State localState) throws IOException, URISyntaxException {
    InvCatalogImpl parentCatalog = (InvCatalogImpl) getParentCatalog();
    InvCatalogImpl mainCatalog = new InvCatalogImpl(getName(), parentCatalog.getVersion(), catURI);

    mainCatalog.addDataset(localState.top);
    mainCatalog.addService(InvService.latest);  // in case its needed
    mainCatalog.addService(virtualService);
    // top.getLocalMetadataInheritable().setServiceName(virtualService.getName());  //??
    mainCatalog.finish();
    return mainCatalog;
  }

  // this catalog lists the individual files comprising the collection.
  protected InvCatalogImpl makeCatalogFiles(URI catURI, State localState, List<String> filenames, boolean addLatest) throws IOException {

    //String collectionName = gc.getName();
    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
    //URI myURI = baseURI.resolve(getCatalogHref(collectionName));
    //URI myURI = baseURI.resolve(getCatalogHref(FILES));
    InvCatalogImpl result = new InvCatalogImpl(getFullName(), parent.getVersion(), catURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent(), true); // make all inherited metadata local
    top.setName(FILES);

    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.coverage != null) tmi.setGeospatialCoverage(localState.coverage);
    tmi.setTimeCoverage((DateRange) null); // LOOK

    result.addDataset(top);

    // services need to be local
    result.addService(orgService);
    top.getLocalMetadataInheritable().setServiceName(orgService.getName());

    //String id = getID();
    //if (id == null) id = getPath();

    if (addLatest) {
      InvDatasetImpl ds = new InvDatasetImpl(this, getLatestFileName());
      ds.setUrlPath(LATEST_DATASET_CATALOG);
      // ds.setID(getPath() + "/" + FILES + "/" + LATEST_DATASET_CATALOG);
      ds.setServiceName(LATEST_SERVICE);
      ds.finish();
      top.addDataset(ds);
      result.addService(InvService.latest);
    }

    //sort copy of files
    List<String> sortedFilenames = new ArrayList<>(filenames);
    Collections.sort(sortedFilenames, String.CASE_INSENSITIVE_ORDER);

    // if not increasing (i.e. we WANT newest file listed first), reverse sort
    if (this.config.gribConfig != null && !this.config.gribConfig.filesSortIncreasing) {
      Collections.reverse(sortedFilenames);
    }

    for (String f : sortedFilenames) {
      if (!f.startsWith(topDirectory))
        logger.warn("File {} doesnt start with topDir {}", f, topDirectory);

      String fname = f.substring(topDirectory.length() + 1);
      InvDatasetImpl ds = new InvDatasetImpl(this, fname);
      String lpath = getPath() + "/" + FILES + "/" + fname;
      ds.setUrlPath(lpath);
      ds.setID(lpath);
      ds.tmi.addVariableMapLink(makeMetadataLink(lpath, VARIABLES));
      File file = new File(f);
      ds.tm.setDataSize(file.length());
      ds.finish();
      top.addDataset(ds);
    }

    result.finish();
    return result;
  }

  protected String makeMetadataLink(String datasetName, String metadata) {
    return context + "/metadata/" + datasetName + metadata;
  }

  // called by DataRootHandler.makeDynamicCatalog()
  public InvCatalogImpl makeLatest(String matchPath, String reqPath, URI catURI) throws IOException {
    checkState();

    InvCatalogImpl parent = (InvCatalogImpl) getParentCatalog();
    InvCatalogImpl result = new InvCatalogImpl(getFullName(), parent.getVersion(), catURI);
    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    top.transferMetadata((InvDatasetImpl) this.getParent(), true); // make all inherited metadata local
    top.setName(getLatestFileName());

    // add Variables, GeospatialCoverage, TimeCoverage
    // ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    //if (localState.gc != null) tmi.setGeospatialCoverage(localState.gc);
    //if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

    result.addDataset(top);

    // services need to be local
    // result.addService(InvService.latest);
    if (orgService != null) {
      result.addService(orgService);
      top.getLocalMetadataInheritable().setServiceName(orgService.getName());
    }

    MFile mfile = datasetCollection.getLatestFile();  // LOOK - assumes dcm is up to date
    if (mfile == null) return null;

    String mpath = mfile.getPath();
    if (!mpath.startsWith(topDirectory))
      logger.warn("File {} doesnt start with topDir {}", mpath, topDirectory);
    String fname = mpath.substring(topDirectory.length() + 1);

    String path = FILES + "/" + fname;
    top.setUrlPath(this.configPath + "/" + path);
    top.setID(this.configPath + "/" + path);
    top.tmi.addVariableMapLink(makeMetadataLink(this.configPath + "/" + path, VARIABLES));
    top.tm.setDataSize(mfile.getLength());

    result.finish();
    return result;
  }

  /////////////////////////////////////////////////////////////////////////

  /**
   * Get the associated Grid Dataset, if any. called by DatasetHandler.openGridDataset()
   *
   * @param matchPath match.remaining
   * @return Grid Dataset, or null if n/a
   * @throws IOException on error
   */
  public ucar.nc2.dt.grid.GridDataset getGridDataset(String matchPath) throws IOException {
    int pos = matchPath.indexOf('/');
    String type = (pos > -1) ? matchPath.substring(0, pos) : matchPath;
    String name = (pos > -1) ? matchPath.substring(pos + 1) : "";

    // this assumes that these are files. also might be remote datasets from a catalog
    if (type.equalsIgnoreCase(FILES)) {
      if (topDirectory == null) return null;

      String filename = topDirectory + (topDirectory.endsWith("/") ? "" : "/") + name;
      NetcdfDataset ncd = NetcdfDataset.acquireDataset(null, filename, null, -1, null, null); // no enhancement
      return new ucar.nc2.dt.grid.GridDataset(ncd);
    }

    return null;
  }

  abstract public FeatureDataset getFeatureDataset();

  ///////////////////////////////////////////////////////////////////////////////
  // handle individual files

  /**
   * Get the dataset named by the path.
   * called by DatasetHandler.getNetcdfFile()
   *
   * @param matchPath remaining path from match
   * @return requested dataset
   * @throws IOException if read error
   */
  public NetcdfDataset getNetcdfDataset(String matchPath) throws IOException {
    int pos = matchPath.indexOf('/');
    String type = (pos > -1) ? matchPath.substring(0, pos) : matchPath;
    String name = (pos > -1) ? matchPath.substring(pos + 1) : "";

    // this assumes that these are files. also might be remote datasets from a catalog
    if (type.equalsIgnoreCase(FILES)) {
      if (topDirectory == null) return null;

      String filename = new StringBuilder(topDirectory)
              .append(topDirectory.endsWith("/") ? "" : "/")
              .append(name).toString();
      return NetcdfDataset.acquireDataset(null, filename, null, -1, null, null); // no enhancement
    }

    GridDataset gds = getGridDataset(matchPath); // LOOK cant be right
    return (gds == null) ? null : (NetcdfDataset) gds.getNetcdfFile();
  }

  // called by DataRootHandler.getCrawlableDatasetAsFile()
  // may have to remove the extra "files" from the path
  // this says that a File URL has to be topDirectory + [FILES/ +] + match.remaining
  public File getFile(String remaining) {
    if (null == topDirectory) return null;
    int pos = remaining.indexOf(FILES);
    StringBuilder fname = new StringBuilder(topDirectory);
    if (!topDirectory.endsWith("/"))
      fname.append("/");
    fname.append((pos > -1) ? remaining.substring(pos + FILES.length() + 1) : remaining);
    return new File(fname.toString());
  }

  // specialized filter handles olderThan and/or filename pattern matching
  // for InvDatasetScan
  static class ScanFilter implements CrawlableDatasetFilter {
    private final Pattern p;
    private final long olderThan;

    public ScanFilter(Pattern p, long olderThan) {
      this.p = p;
      this.olderThan = olderThan;
    }

    @Override
    public boolean accept(CrawlableDataset dataset) {
      if (dataset.isCollection()) return true;

      if (p != null) {
        java.util.regex.Matcher matcher = p.matcher(dataset.getName());
        if (!matcher.matches()) return false;
      }

      if (olderThan > 0) {
        Date lastModDate = dataset.lastModified();
        if (lastModDate != null) {
          long now = System.currentTimeMillis();
          if (now - lastModDate.getTime() <= olderThan)
            return false;
        }
      }

      return true;
    }

    @Override
    public Object getConfigObject() {
      return null;
    }
  }

}

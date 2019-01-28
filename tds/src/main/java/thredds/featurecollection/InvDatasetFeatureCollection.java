/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.featurecollection;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import thredds.client.catalog.*;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.core.AllowedServices;
import thredds.core.StandardService;
import thredds.inventory.*;
import thredds.server.catalog.FeatureCollectionRef;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.simpgeometry.SimpleGeometryFeatureDataset;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.URLnaming;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;

/**
 * Abstract superclass for Feature Collection Datasets.
 * This is a InvCatalogRef subclass. So the reference is placed in the parent, but
 * the catalog itself isnt constructed until the following call from DataRootHandler.makeDynamicCatalog():
 * match.dataRoot.featCollection.makeCatalog(match.remaining, path, baseURI);
 * <p>
 * The DatasetFeatureCollection object is held in the DataRootManager's FeatureCollectionCache; it may get closed and recreated.
 *
 * @author caron
 * @since Mar 3, 2010
 */
@ThreadSafe
public abstract class InvDatasetFeatureCollection implements Closeable {
  private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(InvDatasetFeatureCollection.class);

  static protected final String LATEST_DATASET_CATALOG = "latest.xml";
  static protected final String VARIABLES = "?metadata=variableMap";
  static protected final String FILES = "files";

  // can be changed
  static protected AllowedServices allowedServices;
  static protected String contextName = "/thredds";  // set by TdsInit

  // cant use spring wiring because InvDatasetFeatureCollection not a spring component because depends on catalog config
  static public void setContextName(String c) {
    contextName = c;
  }

  static public void setAllowedServices(AllowedServices _allowedServices) {
    allowedServices = _allowedServices;
  }

  static protected String buildCatalogServiceHref(String path) {
    return contextName + "/catalog/" + path + "/catalog.xml";
  }

  static public InvDatasetFeatureCollection factory(FeatureCollectionRef parent, FeatureCollectionConfig config) {
    InvDatasetFeatureCollection result;
    if (config.type == FeatureCollectionType.FMRC)
      result = new InvDatasetFcFmrc(parent, config);

    else if (config.type == FeatureCollectionType.GRIB1 || config.type == FeatureCollectionType.GRIB2) {
      result = new InvDatasetFcGrib(parent, config);

    } else {
      result = new InvDatasetFcPoint(parent, config);
    }

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////
  // heres how we manage state changes in a thread-safe way
  protected class State {
    // catalog metadata
    protected ThreddsMetadata.VariableGroup vars;
    protected ThreddsMetadata.GeospatialCoverage coverage;
    protected CalendarDateRange dateRange;

    //protected DatasetBuilder top;        // top dataset LOOK why ??
    protected long lastInvChange;        // last time dataset inventory was changed
    protected long lastProtoChange;      // last time proto dataset was changed

    protected State(State from) {
      if (from != null) {
        this.vars = from.vars;
        this.coverage = from.coverage;
        this.dateRange = from.dateRange;
        this.lastProtoChange = from.lastProtoChange;

        //this.top = from.top;
        this.lastInvChange = from.lastInvChange;
      }
    }

    protected State copy() {  // allow override
      return new State(this);
    }
  }

  /////////////////////////////////////////////////////////////////////////////

  // not changed after first call
  protected FeatureCollectionRef parent;
  protected Service orgService, virtualService, latestService, downloadService;

  // from the config catalog
  protected final String name;
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

  protected InvDatasetFeatureCollection(FeatureCollectionRef parent, FeatureCollectionConfig config) {
    this.parent = parent;
    this.name = config.name;
    this.configPath = config.path;
    this.fcType = config.type;
    this.config = config;

    makeDefaultServices();

    // this.getLocalMetadataInheritable().setDataType(fcType.getFeatureType());
    logger.info("FeatureCollection added = {}", getConfig());
  }

  protected void makeDefaultServices() {
    latestService = allowedServices.getStandardService(StandardService.resolver);
    downloadService = allowedServices.getStandardService(StandardService.httpServer);

    orgService = parent.getServiceDefault();
    if (orgService == null) {
      String orgServiceName = parent.getServiceNameDefault();
      orgService = allowedServices.findGlobalService(orgServiceName);
    }
    if (orgService == null) {
      orgService = allowedServices.getStandardServices(fcType.getFeatureType());
    }
    if (orgService == null)
      return;

    if (orgService.getType() != ServiceType.Compound) {
      virtualService = orgService;
      return;
    }

    // remove http service
    List<Service> nestedOk = new ArrayList<>();
    for (Service service : orgService.getNestedServices()) {
      if (service.getType() != ServiceType.HTTPServer) {
        nestedOk.add(service);
      }
    }
    virtualService = new Service("VirtualServices", "", ServiceType.Compound.toString(), ServiceType.Compound.getDescription(),
            null, nestedOk, orgService.getProperties(), ServiceType.Compound.getAccessType());
  }

  public void close() {
    if (datasetCollection != null)
      datasetCollection.close();
  }

  //////////////////////////////////////////////////////
  // old school : used by FMRC and Point

  protected void makeCollection() {
    Formatter errlog = new Formatter();
    datasetCollection = new MFileCollectionManager(config, errlog, logger);
    topDirectory = datasetCollection.getRoot();
    String errs = errlog.toString();
    if (errs.length() > 0) logger.warn("MFileCollectionManager parse error = {} ", errs);
  }

  //////////////////////////////////////////////////////
  // called by eventBus, this is where the trigger comes in
  @Subscribe
  public void processEvent(CollectionUpdateEvent event) {
    if (!config.collectionName.equals(event.getCollectionName())) return; // not for me

    try {
      update(event.getType());
    } catch (IOException e) {
      logger.error("Error processing event", e);
    }
  }

  ///////////////////////////////////////////////////

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

  protected String getPath() {
    return parent.getUrlPath();
  }

  //////////////////////////////////////////////////////////////////////////////////////////
  // for subclasses

  // localState is synched, may be directly changed
  abstract protected void updateCollection(State localState, CollectionUpdateType force);

  ////////////////////////////////////////////////////////////////////////////////////////////

  protected String getCatalogHref(String what) {
    return buildCatalogServiceHref(configPath + "/" + what);
  }

  // call this first time a request comes in. LOOK could be at construction time i think
  protected void firstInit() {
    // makeDefaultServices();
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
        // makeDatasetTop(state);
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
    // makeDatasetTop(localState);
    localState.lastInvChange = System.currentTimeMillis();

    // switch to live
    synchronized (lock) {
      state = localState;
    }

  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  public String getName() {
    return name;
  }

  public String getConfigPath() {
    return configPath;
  }

  public String getLatestFileName() {
    if (config.gribConfig.latestNamer != null) {
      return config.gribConfig.latestNamer;
    } else {
      return "Latest " + name + " File";
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

  ///////////////////////////////////////////////////////////////////////////////////////////////////

  protected String makeFullName(DatasetNode ds) {
    if (ds.getParent() == null) return ds.getName();
    String parentName = makeFullName(ds.getParent());
    if (parentName == null || parentName.length() == 0) return ds.getName();
    return parentName + "/" + ds.getName();
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
  abstract public CatalogBuilder makeCatalog(String match, String orgPath, URI catURI) throws IOException;

  abstract protected DatasetBuilder makeDatasetTop(URI catURI, State localState) throws IOException;

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
  protected CatalogBuilder makeCatalogTop(URI catURI, State localState) throws IOException, URISyntaxException {
    Catalog parentCatalog = parent.getParentCatalog();

    CatalogBuilder topCatalog = new CatalogBuilder();
    topCatalog.setName(makeFullName(parent));
    topCatalog.setVersion(parentCatalog.getVersion());
    topCatalog.setBaseURI(catURI);

    DatasetBuilder top = makeDatasetTop(catURI, localState);
    topCatalog.addDataset(top);
    // topCatalog.addService(StandardServices.latest.getService());  // in case its needed
    topCatalog.addService(virtualService);
    return topCatalog;
  }

  // this catalog lists the individual files comprising the collection.
  protected CatalogBuilder makeCatalogFiles(URI catURI, State localState, List<String> filenames, boolean addLatest) throws IOException {
    Catalog parentCatalog = parent.getParentCatalog();

    CatalogBuilder result = new CatalogBuilder();
    result.setName(makeFullName(parent));
    result.setVersion(parentCatalog.getVersion());
    result.setBaseURI(catURI);
    result.addService(orgService);

    DatasetBuilder top = new DatasetBuilder(null);
    top.transferInheritedMetadata(parent); // make all inherited metadata local
    top.setName(FILES);

    // add Variables, GeospatialCoverage, TimeCoverage
    ThreddsMetadata tmi = top.getInheritableMetadata();
    tmi.set(Dataset.TimeCoverage, null);      // LOOK
    if (localState.coverage != null) {
      tmi.set(Dataset.GeospatialCoverage, localState.coverage);
    }
    tmi.set(Dataset.ServiceName, orgService.getName());
    result.addDataset(top);

    if (addLatest) {
      DatasetBuilder latest = new DatasetBuilder(top);
      latest.setName(getLatestFileName());
      latest.put(Dataset.UrlPath, LATEST_DATASET_CATALOG);
      latest.put(Dataset.Id, LATEST_DATASET_CATALOG);
      latest.put(Dataset.ServiceName, latestService.getName());
      latest.addServiceToCatalog(latestService);
      top.addDataset(latest);
    }

    // sort copy of files
    List<String> sortedFilenames = new ArrayList<>(filenames);
    Collections.sort(sortedFilenames, String.CASE_INSENSITIVE_ORDER);

    // if not increasing (i.e. we WANT newest file listed first), reverse sort
    if (!this.config.getSortFilesAscending()) {
      Collections.reverse(sortedFilenames);
    }

    for (String f : sortedFilenames) {
      if (!f.startsWith(topDirectory))
        logger.warn("File {} doesnt start with topDir {}", f, topDirectory);

      DatasetBuilder ds = new DatasetBuilder(top);

      String fname = f.substring(topDirectory.length() + 1);
      ds.setName(fname);

      String lpath = this.configPath + "/" + FILES + "/" + fname;
      // String lpath = getPath() + "/" + FILES + "/" + fname;
      ds.put(Dataset.UrlPath, lpath);
      ds.put(Dataset.Id, lpath);
      ds.put(Dataset.VariableMapLinkURI, new ThreddsMetadata.UriResolved(makeMetadataLink(lpath, VARIABLES), catURI));

      File file = new File(f);
      ds.put(Dataset.DataSize, file.length());
      top.addDataset(ds);
    }

    return result;
  }

  protected String makeMetadataLink(String datasetName, String metadata) {
    return contextName + "/metadata/" + datasetName + metadata;
  }

  protected ThreddsMetadata.UriResolved makeUriResolved(URI baseURI, String href) {
    try {
      String mapUri = URLnaming.resolve(baseURI.toString(), href);
      return new ThreddsMetadata.UriResolved(href, new URI(mapUri));
    } catch (Exception e) {
      logger.error(" ** Invalid URI= '" + baseURI.toString() + "' href='" + href + "'%n", e);
      return null;
    }
  }

  // called by DataRootHandler.makeDynamicCatalog()
  public CatalogBuilder makeLatest(String matchPath, String reqPath, URI catURI) throws IOException {
    checkState();

    Catalog parentCatalog = parent.getParentCatalog();

    CatalogBuilder result = new CatalogBuilder();
    result.setName(makeFullName(parent));
    result.setVersion(parentCatalog.getVersion());
    result.setBaseURI(catURI);
    result.addService(orgService);

    DatasetBuilder top = new DatasetBuilder(null);
    top.transferInheritedMetadata(parent); // make all inherited metadata local
    top.setName(getLatestFileName());
    top.put(Dataset.ServiceName, orgService.getName());

    result.addDataset(top);

    MFile mfile = datasetCollection.getLatestFile();  // LOOK - assumes dcm is up to date
    if (mfile == null) return null;

    String mpath = mfile.getPath();
    if (!mpath.startsWith(topDirectory))
      logger.warn("File {} doesnt start with topDir {}", mpath, topDirectory);
    String fname = mpath.substring(topDirectory.length() + 1);

    String path = FILES + "/" + fname;
    top.put(Dataset.UrlPath, this.configPath + "/" + path);
    top.put(Dataset.Id, this.configPath + "/" + path);
    String lpath = this.configPath + "/" + path;
    top.put(Dataset.VariableMapLinkURI, new ThreddsMetadata.UriResolved(makeMetadataLink(lpath, VARIABLES), catURI));
    top.put(Dataset.DataSize, mfile.getLength());

    return result;
  }

  /////////////////////////////////////////////////////////////////////////

  /**
   * Get the associated Point Dataset, if any. called by DatasetHandler.openPointDataset()
   *
   * @param matchPath match.remaining
   * @return Grid Dataset, or null if n/a
   * @throws IOException on error
   */
  public FeatureDatasetPoint getPointDataset(String matchPath) throws IOException {
    return null;
  }

  /**
   * Get the associated Grid Dataset, if any. called by DatasetHandler.openGridDataset()
   *
   * @param matchPath match.remaining
   * @return Grid Dataset, or null if n/a
   * @throws IOException on error
   */
  public ucar.nc2.dt.grid.GridDataset getGridDataset(String matchPath) throws IOException {
    return null;
  }

  public CoverageCollection getGridCoverage(String matchPath) throws IOException {
    return null;
  }
  public SimpleGeometryFeatureDataset getSimpleGeometryDataset(String matchPath) throws IOException{
    return null;
  }

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
      DatasetUrl durl = new DatasetUrl(null, filename);

      return NetcdfDataset.acquireDataset(null, durl, null, -1, null, null); // no enhancement
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

}

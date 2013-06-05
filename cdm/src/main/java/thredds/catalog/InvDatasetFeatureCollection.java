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
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.util.log.LoggerFactory;
import ucar.nc2.util.log.LoggerFactoryImpl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Abstract superclass for Feature Collection Datasets.
 * This is a InvCatalogRef subclass. So the reference is placed in the parent, but
 * the catalog itself isnt constructed until the following call from DataRootHandler.makeDynamicCatalog():
 *       match.dataRoot.featCollection.makeCatalog(match.remaining, path, baseURI);
 * <p/>
 * The InvDatasetFeatureCollection object is created once and held in the DataRootHandler's collection
 * of DataRoots.
 *
 * @author caron
 * @since Mar 3, 2010
 */
@ThreadSafe
public abstract class InvDatasetFeatureCollection extends InvCatalogRef implements CollectionManager.TriggerListener {
  static protected final String LATEST_DATASET_CATALOG = "latest.xml";
  static protected final String LATEST_SERVICE = "latest";
  static protected final String VARIABLES = "?metadata=variableMap";
  static protected final String FILES = "files";
  static protected final String Virtual_Services = "VirtualServices"; // exclude HTTPServer

  static private String catalogServletName = "/catalog";
  static protected String context = "/thredds";
  static private String cdmrFeatureServiceUrlPath = "/cdmrFeature";
  static private LoggerFactory loggerFactory = new LoggerFactoryImpl();
  static private org.slf4j.Logger classLogger = org.slf4j.LoggerFactory.getLogger(InvDatasetFeatureCollection.class);

  static public void setContext( String c ) {
    context = c;
  }

  static public void setCatalogServletName( String catServletName ) {
    catalogServletName = catServletName;
  }

  static private String buildCatalogServiceHref( String path ) {
    return context + ( catalogServletName == null ? "" : catalogServletName ) + "/" + path + "/catalog.xml";
  }

  static public void setCdmrFeatureServiceUrlPath( String urlPath) {
    cdmrFeatureServiceUrlPath = urlPath;
  }

  static public void setLoggerFactory( LoggerFactory fac) {
    loggerFactory = fac;
  }

  static private InvService makeCdmrFeatureService() {
    return new InvService( "cdmrFeature","cdmrFeature", context + cdmrFeatureServiceUrlPath, null,null );
  }

  static public InvDatasetFeatureCollection factory(InvDatasetImpl parent, String name, String path, FeatureCollectionType fcType, FeatureCollectionConfig config) {
    InvDatasetFeatureCollection result;
    if (fcType == FeatureCollectionType.FMRC)
      result = new InvDatasetFcFmrc(parent, name, path, fcType, config);

    else if (fcType == FeatureCollectionType.GRIB) {
      // use reflection to decouple from grib.jar
      try {
        Class c = InvDatasetFeatureCollection.class.getClassLoader().loadClass("thredds.catalog.InvDatasetFcGrib");
      // public InvDatasetFcGrib(InvDatasetImpl parent, String name, String path, FeatureType featureType, FeatureCollectionConfig config) {
        Constructor ctor = c.getConstructor(InvDatasetImpl.class, String.class, String.class, FeatureCollectionType.class, FeatureCollectionConfig.class);
        result = (InvDatasetFeatureCollection) ctor.newInstance(parent, name, path, fcType, config);

      } catch (Throwable e) {
        classLogger.error("Failed to open "+name+" path="+path, e);
        return null;
      }

    } else  {
      result =  new InvDatasetFcPoint(parent, name, path, fcType, config);
    }

    if (result != null) {
      result.finishConstruction(); // stuff that shouldnt be done in a constructor
    }

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////
  // heres how we manage state changes in a thread-safe way
  protected class State {
    // catalog metadata
    protected ThreddsMetadata.Variables vars;
    protected ThreddsMetadata.GeospatialCoverage coverage;
    protected CalendarDateRange dateRange;

    protected List<InvDataset> datasets; // top datasets, ie immediately nested in this catalog
    protected long lastInvChange;        // last time dataset inventory was changed
    protected long lastProtoChange;      // last time proto dataset was changed

    protected State(State from) {
      if (from != null) {
        this.vars = from.vars;
        this.coverage = from.coverage;
        this.dateRange = from.dateRange;
        this.lastProtoChange = from.lastProtoChange;

        this.datasets = from.datasets;
        this.lastInvChange = from.lastInvChange;
      }
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // not changed after first call
  protected InvService orgService, virtualService;
  protected InvService cdmrService;  // LOOK why do we need to specify this seperately ??
  protected org.slf4j.Logger logger;

  // from the config catalog
  protected final String path;
  protected final FeatureCollectionType fcType;
  protected final FeatureCollectionConfig config;
  protected final String topDirectory;
  protected CollectionManager dcm; // defines the collection of datasets in this feature collection

  @GuardedBy("lock")
  protected State state;
  protected final Object lock = new Object();
  protected String latestFileName = "Latest File";

  protected InvDatasetFeatureCollection(InvDatasetImpl parent, String name, String path, FeatureCollectionType fcType, FeatureCollectionConfig config) {
    super(parent, name, buildCatalogServiceHref( path) );
    this.path = path;
    this.fcType = fcType;

    this.getLocalMetadataInheritable().setDataType(fcType.getFeatureType());

    this.config = config;
    if (config.gribConfig.latestNamer != null) {
      this.latestFileName = config.gribConfig.latestNamer;
    }

    String collectionName = CollectionManagerAbstract.cleanName(config.name != null ? config.name : name);
    config.name =  collectionName;
    this.logger = loggerFactory.getLogger("fc."+collectionName); // seperate log file for each feature collection (!!)

    Formatter errlog = new Formatter();
    if (config.spec.startsWith(MFileCollectionManager.CATALOG)) {
      dcm = new CatalogCollectionManager(config.spec);
    } else {
      dcm = new MFileCollectionManager(config, errlog, this.logger);
    }
    topDirectory = dcm.getRoot();

    this.logger.info("FeatureCollection added = {}", getConfig());
    String errs = errlog.toString();
    if (errs.length()> 0) logger.warn("MFileCollectionManager parse error = {} ", errs);
  }

  // stuff that shouldnt be done in a constructor - eg dont let 'this' escape
  // LOOK maybe not best design to start tasks from here
  protected void finishConstruction() {
    dcm.addEventListener(this); // now wired for events
    CollectionUpdater.INSTANCE.scheduleTasks(config, dcm); // see if any background tasks are needed
  }

  protected String getCatalogHref( String what) {
    return buildCatalogServiceHref( path + "/" + what );
  }

  // call this first time a request comes in
  protected void firstInit() {
    this.orgService = getServiceDefault();
    if (this.orgService == null) throw new IllegalStateException("No default service for InvDatasetFeatureCollection "+name);
    this.virtualService = makeVirtualService(this.orgService);
    this.cdmrService = makeCdmrFeatureService();
  }

  @Override
  // DatasetCollectionManager was changed asynchronously
  public void handleCollectionEvent(CollectionManager.TriggerEvent event) {
    // if this is the TDS, and its using the TDM, then you're not allowed to update
    boolean tdsUsingTdm = !CollectionUpdater.INSTANCE.isTdm() && config.tdmConfig != null;

    if (event.getType() == CollectionManager.TriggerType.updateNocheck)
      update(CollectionManager.Force.nocheck);

    else if (event.getType() == CollectionManager.TriggerType.update)
      //update(tdsUsingTdm ? CollectionManager.Force.nocheck : CollectionManager.Force.test); // this may be startup
      update(CollectionManager.Force.always);

    else if (event.getType() == CollectionManager.TriggerType.proto)
      updateProto();
   }

  /**
   * Collection was changed, update internal objects.
   * called by CollectionUpdater, trigger via handleCollectionEvent, so in a quartz scheduler thread
   * @param force test : update index if anything changed or nocheck - use index if it exists
   */
  abstract public void update(CollectionManager.Force force);

  /**
   * update the proto dataset being used.
   * called by CollectionUpdater via handleCollectionEvent, so in a quartz scheduler thread
   */
  abstract public void updateProto();

  /**
   *  a request has come in, check that the state is up-to-date
   *
   * @return the State, updated if needed
   * @throws java.io.IOException on read error
   */
  abstract protected State checkState() throws IOException;

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public String getPath() {
    return path;
  }

  public String getLatestFileName() {
    return latestFileName;
  }

  public String getTopDirectoryLocation() {
    return topDirectory;
  }

  public FeatureCollectionConfig getConfig() {
    return config;
  }

  public CollectionManager getDatasetCollectionManager() {
    return dcm;
  }

  public Logger getLogger() {
    return logger;
  }

  @Override
  public java.util.List<InvDataset> getDatasets() {
    try {
      checkState();
    } catch (Exception e) {
      logger.error("Error in checkState", e);
    }
    return state.datasets;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////
  protected InvService makeVirtualService(InvService org) {
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
   * @param match match.remaining
   * @param orgPath    the path for the request.
   * @param catURI the base URI for the catalog to be made, used to resolve relative URLs.

   * @return containing catalog
   */
  abstract public InvCatalogImpl makeCatalog(String match, String orgPath, URI catURI);

  /**
   * Make the containing catalog of this feature collection
   * "http://server:port/thredds/catalog/path/catalog.xml"
   *
   * @param catURI base URI of the request
   * @param localState current state to use
   * @return the top FMRC catalog
   * @throws java.io.IOException         on I/O error
   * @throws java.net.URISyntaxException if path is misformed
   */
  protected InvCatalogImpl makeCatalogTop(URI catURI, State localState) throws IOException, URISyntaxException {
    InvCatalogImpl parentCatalog = (InvCatalogImpl) getParentCatalog();
    //URI myURI = catURI.resolve(getXlinkHref());  LOOK
    InvCatalogImpl mainCatalog = new InvCatalogImpl(getName(), parentCatalog.getVersion(), catURI);

    InvDatasetImpl top = new InvDatasetImpl(this);
    top.setParent(null);
    InvDatasetImpl parent = (InvDatasetImpl) this.getParent();
    if (parent != null)
      top.transferMetadata(parent, true); // make all inherited metadata local

    String id = getID();
    if (id == null)
      id = getPath();
    top.setID(id);  

    // add Variables, GeospatialCoverage, TimeCoverage LOOK doesnt seem to work
    ThreddsMetadata tmi = top.getLocalMetadataInheritable();
    if (localState.vars != null) tmi.addVariables(localState.vars);
    if (localState.coverage != null) tmi.setGeospatialCoverage(localState.coverage);
    if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

    mainCatalog.addDataset(top);

    // any referenced services need to be local
    // remove http service for virtual datasets
    //mainCatalog.addService(virtualService);
    //top.getLocalMetadataInheritable().setServiceName(virtualService.getName());

    for (InvDataset ds : getDatasets())
      top.addDataset((InvDatasetImpl) ds);

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
    //if (localState.dateRange != null) tmi.setTimeCoverage(localState.dateRange);

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

    //COPY FILENAMES TO MODIFIABLE LIST, SORT, AND PASS
    List<String> sortedFilenames = new ArrayList<String>(filenames);

    // if not increasing (i.e. we WANT newest file listed first), reverse sort
    if (this.config.gribConfig != null && !this.config.gribConfig.filesSortIncreasing) {
      Collections.reverse(sortedFilenames);
    } else {
      Collections.sort(sortedFilenames,String.CASE_INSENSITIVE_ORDER);
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

  public InvCatalogImpl makeLatest(String matchPath, String reqPath, URI catURI) {

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

     /* boolean isSingleGroup = gc.getGroups().size() == 1;
     List<String> filenames = isSingleGroup ? gc.getFilenames() : group.getFilenames();
     String f = filenames.get(filenames.size()-1);
     if (!f.startsWith(topDirectory))
       logger.warn("File {} doesnt start with topDir {}", f, topDirectory);

     String fname = f.substring(topDirectory.length() + 1); */

     MFile mfile = dcm.getLatestFile();  // LOOK - assumes dcm is up to date
     String mpath = mfile.getPath();
     if (!mpath.startsWith(topDirectory))
      logger.warn("File {} doesnt start with topDir {}", mpath, topDirectory);
     String fname = mpath.substring(topDirectory.length() + 1);

     String path = FILES + "/" + fname;
     top.setUrlPath(this.path + "/" + path);
     top.setID(this.path + "/" + path);
     top.tmi.addVariableMapLink(makeMetadataLink(this.path + "/" + path, VARIABLES));
     top.tm.setDataSize(mfile.getLength());

     result.finish();
     return result;
   }

  /////////////////////////////////////////////////////////////////////////

  /**
   * Get the associated Grid Dataset, if any. called by DatasetHandler.openGridDataset()
   * @param matchPath match.remaining
   * @return Grid Dataset, or null if n/a
   * @throws IOException on error
   */
  public ucar.nc2.dt.GridDataset getGridDataset(String matchPath) throws IOException {
    int pos = matchPath.indexOf('/');
    String type = (pos > -1) ? matchPath.substring(0, pos) : matchPath;
    String name = (pos > -1) ? matchPath.substring(pos + 1) : "";

    // this assumes that these are files. also might be remote datasets from a catalog
    if (type.equals(FILES)) {
      if (topDirectory == null) return null;

      String filename = new StringBuilder(topDirectory)
              .append(topDirectory.endsWith("/") ? "" : "/")
              .append(name).toString();
      NetcdfDataset ncd = NetcdfDataset.acquireDataset(null, filename, null, -1, null, null); // no enhancement
      return new ucar.nc2.dt.grid.GridDataset(ncd);
    }

    return null;
  }

  public FeatureDatasetPoint getFeatureDatasetPoint() {
    return null;
  }

  ///////////////////////////////////////////////////////////////////////////////
  // handle individual files

  /**
   * Get the dataset named by the path.
   * called by DatasetHandler.getNetcdfFile()
   * @param matchPath remaining path from match
   * @return requested dataset
   * @throws IOException if read error
   */
  public NetcdfDataset getNetcdfDataset(String matchPath) throws IOException {
    int pos = matchPath.indexOf('/');
    String type = (pos > -1) ? matchPath.substring(0, pos) : matchPath;
    String name = (pos > -1) ? matchPath.substring(pos + 1) : "";

    // this assumes that these are files. also might be remote datasets from a catalog
    if (type.equals(FILES)) {
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
  // have to remove the extra "files" from the path
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

/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.core;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import thredds.client.catalog.*;
import thredds.filesystem.MFileOS7;
import thredds.inventory.MFile;
import thredds.server.admin.DebugController;
import thredds.server.catalog.*;
import thredds.server.config.TdsContext;

import thredds.servlet.PathMatcher;
import thredds.util.*;
import thredds.util.filesource.FileSource;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * The DataRootHandler manages all the "data roots" for a given web application
 * and provides mappings from URLs to catalog and datasets
 * <p/>
 * <p>The "data roots" are read in from one or more trees of config catalogs
 * and are defined by the datasetScan and datasetRoot elements in the config catalogs.
 * <p/>
 * <p> Uses the singleton design pattern.
 *
 * @author caron
 * @since 1/23/2015
 */
// @Component("DataRootHandler")
// @DependsOn("CdmInit")
public final class DataRootHandler implements InitializingBean {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataRootHandler.class);
  static private org.slf4j.Logger logCatalogInit = org.slf4j.LoggerFactory.getLogger(DataRootHandler.class.getName() + ".catalogInit");
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

  // dont need to Guard/synchronize singleton, since creation and publication is only done by a servlet init() and therefore only in one thread (per ClassLoader).
  //Spring bean so --> there will be one per context (by default is a singleton in the Spring realm) 
  static private DataRootHandler singleton = null;
  static private final String ERROR = "*** ERROR ";
  static public final boolean debug = false;

  /**
   * Initialize the DataRootHandler singleton instance.
   * <p/>
   * This method should only be called once. Additional calls will be ignored.
   *
   * @param drh the singleton instance of DataRootHandler being used
   */
  static public void setInstance(DataRootHandler drh) {
    if (singleton != null) {
      log.warn("setInstance(): Singleton already set: ignoring call.");
      return;
    }
    singleton = drh;
  }

  /**
   * Get the singleton.
   * <p/>
   * The setInstance() method must be called before this method is called.
   *
   * @return the singleton instance.
   * @throws IllegalStateException if setInstance() has not been called.
   */
  static public DataRootHandler getInstance() {
    if (singleton == null) {
      logCatalogInit.error(ERROR + "getInstance(): Called without setInstance() having been called.");
      throw new IllegalStateException("setInstance() must be called first.");
    }
    return singleton;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private PathMatcher pathMatcher;

  @Autowired
  private ConfigCatalogManager ccManager;

  //private final TdsContext tdsContext;
  //private boolean staticCache;

  // @GuardedBy("this") LOOK should be able to access without synchronization
  //private HashMap<String, ConfigCatalog> staticCatalogHash; // Hash of static catalogs, key = path
  //private Set<String> staticCatalogNames; // Hash of static catalogs, key = path

  // @GuardedBy("this")
  //private HashSet<String> idHash = new HashSet<>(); // Hash of ids, to look for duplicates

  //  PathMatcher is "effectively immutable"; use volatile for visibilty
  // private volatile PathMatcher pathMatcher = new PathMatcher(); // collection of DataRoot objects

  private List<ConfigListener> configListeners = new ArrayList<>();


  /**
   * Constructor.
   * Managed bean - dont do nuttin else !!
   */
  private DataRootHandler(TdsContext tdsContext) {
    this.tdsContext = tdsContext;
  }

  private DataRootHandler() {

  }

  //Set method must be called so annotation at method level rather than property level
  @Resource(name = "dataRootLocationAliasExpanders")
  public void setDataRootLocationAliasExpanders(Map<String, String> aliases) {
    for (Map.Entry<String, String> entry : aliases.entrySet())
      ConfigCatalog.addAlias(entry.getKey(), entry.getValue());
  }

  //////////////////////////////////////////////

  //public void init() {
  public void afterPropertiesSet() {

    //Registering first the AccessConfigListener
    registerConfigListener(new RestrictedAccessConfigListener());

    // Initialize any given DataRootLocationAliasExpanders that are TdsConfiguredPathAliasReplacement
    FileSource fileSource = tdsContext.getPublicDocFileSource();
    if (fileSource != null) {
      File file = fileSource.getFile("");
      if (file != null)
        ConfigCatalog.addAlias("content", StringUtils.cleanPath(file.getPath())); // LOOK
    }

    ccManager = new ConfigCatalogManager();
    ccManager.initCatalogs();

    this.makeDebugActions();
    DatasetHandler.makeDebugActions();

    //Set the instance
    DataRootHandler.setInstance(this);
  }

  public boolean registerConfigListener(ConfigListener cl) {
    if (cl == null) return false;
    if (configListeners.contains(cl)) return false;
    return configListeners.add(cl);
  }

  public boolean unregisterConfigListener(ConfigListener cl) {
    if (cl == null) return false;
    return configListeners.remove(cl);
  }

  /**
   * Reinitialize lists of static catalogs, data roots, dataset Ids.
   */
  public synchronized void reinit() {
    // Notify listeners of start of initialization.
    isReinit = true;
    for (ConfigListener cl : configListeners)
      cl.configStart();

    logCatalogInit.info("\n**************************************\n**************************************\nStarting TDS config catalog reinitialization\n["
            + CalendarDate.present() + "]");

    // cleanup 
    thredds.inventory.bdb.MetadataManager.closeAll();

    // Empty all config catalog information.
    pathMatcher = new PathMatcher();
    //idHash = new HashSet<>();

    DatasetHandler.reinit(); // NcML datasets
    ccManager = new ConfigCatalogManager();
    ccManager.initCatalogs();

    isReinit = false;

    logCatalogInit.info("\n**************************************\n**************************************\nDone with TDS config catalog reinitialization\n["
            + CalendarDate.present() + "]");
  }

  volatile boolean isReinit = false;

  /* void initCatalogs() {
    ArrayList<String> catList = new ArrayList<>();
    catList.add("catalog.xml"); // always first
    catList.addAll(ThreddsConfig.getCatalogRoots()); // add any others listed in ThreddsConfig

    logCatalogInit.info("initCatalogs(): initializing " + catList.size() + " root catalogs.");
    this.initCatalogs(catList);
  }

  public synchronized void initCatalogs(List<String> configCatalogRoots) {
    // Notify listeners of start of initialization if not reinit (in which case it is already done).
    if (!isReinit)
      for (ConfigListener cl : configListeners)
        cl.configStart();
    isReinit = false;

    staticCache = ThreddsConfig.getBoolean("Catalog.cache", true);  // user can turn off static catalog caching
    startupLog.info("DataRootHandler: staticCache= " + staticCache);

    this.staticCatalogNames = new HashSet<>();
    this.staticCatalogHash = new HashMap<>();

    for (String path : configCatalogRoots) {
      try {
        path = StringUtils.cleanPath(path);
        logCatalogInit.info("\n**************************************\nCatalog init " + path + "\n[" + CalendarDate.present() + "]");
        initCatalog(path, true, true);
      } catch (Throwable e) {
        logCatalogInit.error(ERROR + "initializing catalog " + path + "; " + e.getMessage(), e);
      }
    }

    for (ConfigListener cl : configListeners)
      cl.configEnd();
  }


  /*
   * Reads a catalog, finds datasetRoot, datasetScan, datasetFmrc, NcML and restricted access datasets
   * <p/>
   * Only called by synchronized methods.
   *
   * @param path    file path of catalog, reletive to contentPath, ie catalog fullpath = contentPath + path.
   * @param recurse if true, look for catRefs in this catalog
   * @param cache   if true, always cache
   * @throws java.io.IOException if reading catalog fails
   *
  private void initCatalog(String path, boolean recurse, boolean cache) throws IOException {
    path = StringUtils.cleanPath(path);
    File f = this.tdsContext.getConfigFileSource().getFile(path);
    if (f == null) {
      logCatalogInit.error(ERROR + "initCatalog(): Catalog [" + path + "] does not exist in config directory.");
      return;
    }
    System.out.printf("initCatalog %s%n", f.getPath());

    // make sure we dont already have it
    if (staticCatalogNames.contains(path)) {
      logCatalogInit.error(ERROR + "initCatalog(): Catalog [" + path + "] already seen, possible loop (skip).");
      return;
    }
    staticCatalogNames.add(path);
    if (logCatalogInit.isDebugEnabled()) logCatalogInit.debug("initCatalog {} -> {}", path, f.getAbsolutePath());

    // read it
    InvCatalogFactory factory = this.getCatalogFactory(true); // always validate the config catalogs
    Catalog cat = readCatalog(factory, path, f.getPath());
    if (cat == null) {
      logCatalogInit.error(ERROR + "initCatalog(): failed to read catalog <" + f.getPath() + ">.");
      return;
    }

    // Notify listeners of config catalog.
    for (ConfigListener cl : configListeners)
      cl.configCatalog(cat);

    // look for datasetRoots
    for (DataRootConfig p : cat.getDatasetRoots()) {
      addRoot(p, true);
    }

    List<String> disallowedServices = AllowableService.checkCatalogServices(cat);
    if (!disallowedServices.isEmpty()) {
      logCatalogInit.error(ERROR + "initCatalog(): declared services: " + disallowedServices.toString() + " in catalog: " + f.getPath() + " are disallowed in threddsConfig file");
    }

    // old style - in the service elements
    for (Service s : cat.getServices()) {
      for (Property p : s.getDatasetRoots()) {
        addRoot(p.getName(), p.getValue(), true);
      }
    }

    // get the directory path, reletive to the contentPath
    int pos = path.lastIndexOf("/");
    String dirPath = (pos > 0) ? path.substring(0, pos + 1) : "";

    // look for datasetScans and NcML elements and Fmrc and featureCollections
    boolean needsCache = initSpecialDatasets(cat.getDatasets());

    // optionally add catalog to cache
    if (staticCache || cache || needsCache) {
      cat.setStatic(true);
      staticCatalogHash.put(path, cat);
      if (logCatalogInit.isDebugEnabled()) logCatalogInit.debug("  add static catalog to hash=" + path);
    }

    if (recurse) {
      initFollowCatrefs(dirPath, cat.getDatasets());
    }
  }

  // testing only
  void initCatalog(String path, String absPath) throws IOException {
    // read it
    InvCatalogFactory factory = this.getCatalogFactory(true); // always validate the config catalogs
    Catalog cat = readCatalog(factory, path, absPath);
    if (cat == null) {
      logCatalogInit.error(ERROR + "initCatalog(): failed to read catalog <" + absPath + ">.");
      return;
    }

    // look for datasetRoots
    for (DataRootConfig p : cat.getDatasetRoots()) {
      addRoot(p, true);
    }

    List<String> disallowedServices = AllowableService.checkCatalogServices(cat);
    if (!disallowedServices.isEmpty()) {
      logCatalogInit.error(ERROR + "initCatalog(): declared services: " + disallowedServices.toString() + " in catalog: " + cat.getName() + " are disallowed in threddsConfig file");
    }

    // old style - in the service elements
    for (Service s : cat.getServices()) {
      for (Property p : s.getDatasetRoots()) {
        addRoot(p.getName(), p.getValue(), true);
      }
    }
  }
  
  /*private void checkServices(Service service, String path ){

	  if( service.getServiceType() == ServiceType.COMPOUND ){
		  for(Service s : service.getServices() ){
			checkServices(s, path);  
		  }
	  }else{
		  
		  if( service.getServiceType() == ServiceType.WMS ){			  
			  
			  //if(! ThreddsConfig.getBoolean("WMS.allow", false) ) {
			  if(AllowableService.WMS.isAllowed()){
				  logCatalogInit.warn("initCatalog(): Service "+service.getName()+ " in catalog "+ path +"is not allowed in the server settings");
			  }
		  }

	  }
	  
  }*

  public InvCatalogFactory getCatalogFactory(boolean validate) {
    InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(validate);
    if (!this.dataRootLocationAliasExpanders.isEmpty())
      factory.setDataRootLocationAliasExpanders(this.dataRootLocationAliasExpanders);
    return factory;
  }

  /**
   * Does the actual work of reading a catalog.
   *
   * @param factory         use this InvCatalogFactory
   * @param path            reletive path starting from content root
   * @param catalogFullPath absolute location on disk
   * @return the Catalog, or null if failure
   *
  private Catalog readCatalog(InvCatalogFactory factory, String path, String catalogFullPath) {
    URI uri;
    try {
      uri = new URI("file:" + StringUtil2.escape(catalogFullPath, "/:-_.")); // LOOK needed ?
    } catch (URISyntaxException e) {
      logCatalogInit.error(ERROR + "readCatalog(): URISyntaxException=" + e.getMessage());
      return null;
    }

    // read the catalog
    logCatalogInit.info("\n-------readCatalog(): full path=" + catalogFullPath + "; path=" + path);
    Catalog cat = null;
    FileInputStream ios = null;
    try {
      ios = new FileInputStream(catalogFullPath);
      cat = factory.readXML(ios, uri);

      StringBuilder sbuff = new StringBuilder();
      if (!cat.check(sbuff)) {
        logCatalogInit.error(ERROR + "   invalid catalog -- " + sbuff.toString());
        return null;
      }
      String warn = sbuff.toString();
      if (warn.length() > 0)
        logCatalogInit.debug(warn);

    } catch (Throwable t) {
      String msg = (cat == null) ? "null catalog" : cat.getLog();
      logCatalogInit.error(ERROR + "  Exception on catalog=" + catalogFullPath + " " + t.getMessage() + "\n log=" + msg, t);
      return null;

    } finally {
      if (ios != null) {
        try {
          ios.close();
        } catch (IOException e) {
          logCatalogInit.error("  error closing" + catalogFullPath);
        }
      }
    }

    return cat;
  }

  /**
   * Finds datasetScan, datasetFmrc, NcML and restricted access datasets.
   * Look for duplicate Ids (give message). Dont follow catRefs.
   * Only called by synchronized methods.
   *
   * @param dsList the list of Dataset
   * @return true if the containing catalog should be cached
   *
  private boolean initSpecialDatasets(List<Dataset> dsList) {
    boolean needsCache = false;

    Iterator<Dataset> iter = dsList.iterator();
    while (iter.hasNext()) {
      Dataset Dataset = (Dataset) iter.next();

      // look for duplicate ids
      String id = Dataset.getUniqueID();
      if (id != null) {
        if (idHash.contains(id)) {
          logCatalogInit.error(ERROR + "Duplicate id on  '" + Dataset.getFullName() + "' id= '" + id + "'");
        } else {
          idHash.add(id);
        }
      }

      // Notify listeners of config datasets.
      for (ConfigListener cl : configListeners)
        cl.configDataset(Dataset);

      if (Dataset instanceof DatasetScan) {
        DatasetScan ds = (DatasetScan) Dataset;
        Service service = ds.getServiceDefault();
        if (service == null) {
          logCatalogInit.error(ERROR + "DatasetScan " + ds.getFullName() + " has no default Service - skipping");
          continue;
        }
        if (!addRoot(ds))
          iter.remove();

      } else if (Dataset instanceof FeatureCollection) {
        FeatureCollection fc = (FeatureCollection) Dataset;
        addRoot(fc);
        needsCache = true;

        // not a DatasetScan or DatasetFmrc or FeatureCollection
      } else if (Dataset.getNcmlElement() != null) {
        DatasetHandler.putNcmlDataset(Dataset.getUrlPath(), Dataset);
      }

      if (!(Dataset instanceof CatalogRef)) {
        // recurse
        initSpecialDatasets(Dataset.getDatasets());
      }
    }

    return needsCache;
  }

  // Only called by synchronized methods
  private void initFollowCatrefs(String dirPath, List<Dataset> datasets) throws IOException {
    for (Dataset Dataset : datasets) {

      if ((Dataset instanceof CatalogRef) && !(Dataset instanceof DatasetScan)
              && !(Dataset instanceof FeatureCollection)) {
        CatalogRef catref = (CatalogRef) Dataset;
        String href = catref.getXlinkHref();
        if (logCatalogInit.isDebugEnabled()) logCatalogInit.debug("  catref.getXlinkHref=" + href);

        // Check that catRef is relative
        if (!href.startsWith("http:")) {
          // Clean up relative URLs that start with "./"
          if (href.startsWith("./")) {
            href = href.substring(2);
          }

          String path;
          String contextPathPlus = this.tdsContext.getContextPath() + "/";
          if (href.startsWith(contextPathPlus)) {
            path = href.substring(contextPathPlus.length()); // absolute starting from content root
          } else if (href.startsWith("/")) {
            // Drop the catRef because it points to a non-TDS served catalog.
            logCatalogInit.error(ERROR + "Skipping catalogRef <xlink:href=" + href + ">. Reference is relative to the server outside the context path [" + contextPathPlus + "]. " +
                    "Parent catalog info: Name=\"" + catref.getParentCatalog().getName() + "\"; Base URI=\"" + catref.getParentCatalog().getUriString() + "\"; dirPath=\"" + dirPath + "\".");
            continue;
          } else {
            path = dirPath + href;  // reletive starting from current directory
          }

          initCatalog(path, true, false);
        }

      } else if (!(Dataset instanceof DatasetScan) && !(Dataset instanceof FeatureCollection)) {
        // recurse through nested datasets
        initFollowCatrefs(dirPath, Dataset.getDatasets());
      }
    }
  }

  // Only called by synchronized methods
  private boolean addRoot(DatasetScan dscan) {
    // check for duplicates
    String path = dscan.getPath();

    if (path == null) {
      logCatalogInit.error(ERROR + dscan.getFullName() + " missing a path attribute.");
      return false;
    }

    DataRoot droot = (DataRoot) pathMatcher.get(path);
    if (droot != null) {
      if (!droot.dirLocation.equals(dscan.getScanLocation())) {
        logCatalogInit.error(ERROR + "DatasetScan already have dataRoot =<" + path + ">  mapped to directory= <" + droot.dirLocation + ">" +
                " wanted to map to fmrc=<" + dscan.getScanLocation() + "> in catalog " + dscan.getParentCatalog().getUriString());
      }

      return false;
    }

    // Check whether DatasetScan is valid before adding.
    if (!dscan.isValid()) {
      logCatalogInit.error(ERROR + dscan.getInvalidMessage() + "\n... Dropping this datasetScan [" + path + "].");
      return false;
    }

    // add it
    droot = new DataRoot(dscan);
    pathMatcher.put(path, droot);

    logCatalogInit.debug(" added rootPath=<" + path + ">  for directory= <" + dscan.getScanLocation() + ">");
    return true;
  }

  public List<FeatureCollection> getFeatureCollections() {
    List<FeatureCollection> result = new ArrayList<>();
    Iterator iter = pathMatcher.iterator();
    while (iter.hasNext()) {
      DataRoot droot = (DataRoot) iter.next();
      if (droot.featCollection != null)
        result.add(droot.featCollection);
    }
    return result;
  }

  public FeatureCollection findFcByCollectionName(String collectionName) {
    Iterator iter = pathMatcher.iterator();
    while (iter.hasNext()) {
      DataRoot droot = (DataRoot) iter.next();
      if ((droot.featCollection != null) && droot.featCollection.getCollectionName().equals(collectionName))
        return droot.featCollection;
    }
    return null;
  }


  // Only called by synchronized methods
  private boolean addRoot(FeatureCollection fc) {
    // check for duplicates
    String path = fc.getPath();

    if (path == null) {
      logCatalogInit.error(ERROR + fc.getName() + " missing a path attribute.");
      return false;
    }

    DataRoot droot = (DataRoot) pathMatcher.get(path);
    if (droot != null) {
      logCatalogInit.error(ERROR + "FeatureCollection already have dataRoot =<" + path + ">  mapped to directory= <" + droot.dirLocation + ">" +
              " wanted to use by FeatureCollection Dataset =<" + fc.getName() + ">");
      return false;
    }

    // add it
    droot = new DataRoot(fc);

    if (droot.dirLocation != null) {
      File file = new File(droot.dirLocation);
      if (!file.exists()) {
        logCatalogInit.error(ERROR + "FeatureCollection = '" + fc.getName() + "' directory= <" + droot.dirLocation + "> does not exist\n");
        return false;
      }
    }

    pathMatcher.put(path, droot);
    logCatalogInit.debug(" added rootPath=<" + path + ">  for feature collection= <" + fc.getFullName() + ">");
    return true;
  }

  // Only called by synchronized methods
  private boolean addRoot(String path, String dirLocation, boolean wantErr) {
    // check for duplicates
    DataRoot droot = (DataRoot) pathMatcher.get(path);
    if (droot != null) {
      if (wantErr)
        logCatalogInit.error(ERROR + "already have dataRoot =<" + path + ">  mapped to directory= <" + droot.dirLocation + ">" +
                " wanted to map to <" + dirLocation + ">");

      return false;
    }

    File file = new File(dirLocation);
    if (!file.exists()) {
      logCatalogInit.error(ERROR + "Data Root =" + path + " directory= <" + dirLocation + "> does not exist");
      return false;
    }

    // add it
    droot = new DataRoot(path, dirLocation, true);
    pathMatcher.put(path, droot);

    logCatalogInit.debug(" added rootPath=<" + path + ">  for directory= <" + dirLocation + ">");
    return true;
  }

  // Only called by synchronized methods
  private boolean addRoot(DataRootConfig config, boolean wantErr) {
    String path = config.getName();
    String location = config.getValue();

    // check for duplicates
    DataRoot droot = (DataRoot) pathMatcher.get(path);
    if (droot != null) {
      if (wantErr)
        logCatalogInit.error(ERROR + "DataRootConfig already have dataRoot =<" + path + ">  mapped to directory= <" + droot.dirLocation + ">" +
                " wanted to map to <" + location + ">");

      return false;
    }

    location = expandAliasForDataRoot(location);
    File file = new File(location);
    if (!file.exists()) {
      logCatalogInit.error(ERROR + "DataRootConfig path =" + path + " directory= <" + location + "> does not exist");
      return false;
    }

    // add it
    droot = new DataRoot(path, location, config.isCache());
    pathMatcher.put(path, droot);

    logCatalogInit.debug(" added rootPath=<" + path + ">  for directory= <" + location + ">");
    return true;
  }

  public String expandAliasForDataRoot(String location) {
    for (PathAliasReplacement par : this.dataRootLocationAliasExpanders) {
      String result = par.replaceIfMatch(location);
      if (result != null)
        return result;
    }
    return location;
  }  */

  ////////////////////////////////////////////////////////////////////////////////////////

  static public class DataRootMatch {
    String rootPath;     // this is the matching part of the URL
    String remaining;   // this is the part of the URL that didnt match
    String dirLocation;   // this is the directory that should be substituted for the rootPath
    DataRoot dataRoot;  // this is the directory that should be substituted for the rootPath
  }

  /**
   * Find the location match for a dataRoot.
   * Aliasing has been done.
   *
   * @param path the dataRoot path name
   * @return best DataRoot location or null if no match.
   */
  public String findDataRootLocation(String path) {
    if ((path.length() > 0) && (path.charAt(0) == '/'))
      path = path.substring(1);

    DataRoot dataRoot = (DataRoot) pathMatcher.match(path);
    return (dataRoot == null) ? null : dataRoot.getDirLocation();
  }

  /**
   * Find the longest match for this path.
   *
   * @param fullpath the complete path name
   * @return best DataRoot or null if no match.
   */
  private DataRoot findDataRoot(String fullpath) {
    if ((fullpath.length() > 0) && (fullpath.charAt(0) == '/'))
      fullpath = fullpath.substring(1);

    return (DataRoot) pathMatcher.match(fullpath);
  }

  /**
   * Extract the DataRoot from the request.
   * Use this when you need to manipulate the path based on the part that matches a DataRoot.
   *
   * @param req the request
   * @return the DataRootMatch, or null if not found
   */
  //  see essupport SEG-622383
  public DataRootMatch findDataRootMatch(HttpServletRequest req) {
    String spath = TdsPathUtils.extractPath(req, null);

    if (spath.length() > 0) {
      if (spath.startsWith("/"))
        spath = spath.substring(1);
    }

    return findDataRootMatch(spath);
  }

  public DataRootMatch findDataRootMatch(String spath) {
    if (spath.startsWith("/"))
      spath = spath.substring(1);
    DataRoot dataRoot = findDataRoot(spath);
    if (dataRoot == null)
      return null;

    DataRootMatch match = new DataRootMatch();
    match.rootPath = dataRoot.getPath();
    match.remaining = spath.substring(match.rootPath.length());
    if (match.remaining.startsWith("/"))
      match.remaining = match.remaining.substring(1);
    match.dirLocation = dataRoot.getDirLocation();
    match.dataRoot = dataRoot;
    return match;
  }

  /**
   * Return true if the given path matches a dataRoot, otherwise return false.
   * A succesful match means that the request is either for a dynamic catalog
   * or a dataset.
   *
   * @param path the request path, ie req.getServletPath() + req.getPathInfo()
   * @return true if the given path matches a dataRoot, otherwise false.
   */
  public boolean hasDataRootMatch(String path) {
    if (path.length() > 0)
      if (path.startsWith("/"))
        path = path.substring(1);

    DataRoot dataRoot = findDataRoot(path);
    if (dataRoot == null) {
      if (log.isDebugEnabled()) log.debug("hasDataRootMatch(): no DatasetScan for " + path);
      return false;
    }
    return true;
  }

  /**
   * Return the the MFile to which the given path maps.
   * Null is returned if the dataset does not exist, the
   * matching DatasetScan or DataRoot filters out the requested MFile, the MFile does not represent a File
   * (i.e., it is not a CrawlableDatasetFile), or an I/O error occurs
   *
   * @param path the request path.
   * @return the requested java.io.File or null.
   * @throws IllegalStateException if the request is not for a descendant of (or the same as) the matching DatasetRoot collection location.
   */
  public MFile getFileFromRequestPath(String path) {
    if (path.length() > 0) {
      if (path.startsWith("/"))
        path = path.substring(1);
    }

    DataRoot reqDataRoot = findDataRoot(path);
    if (reqDataRoot == null)
      return null;

    String location = reqDataRoot.getFileLocationFromRequestPath(path);

    try {
      return new MFileOS7(location);
    } catch (IOException e) {
      return null;  // LOOK would happen if file does not exist
    }
  }

  // LOOK WTF ??
  /* private boolean isProxyDatasetResolver(String path) {
    ProxyDatasetHandler pdh = this.getMatchingProxyDataset(path);
    if (pdh == null)
      return false;

    return pdh.isProxyDatasetResolver();
  }

  private ProxyDatasetHandler getMatchingProxyDataset(String path) {
    DatasetScan scan = this.getMatchingScan(path);
    if (null == scan) return null;

    int index = path.lastIndexOf("/");
    String proxyName = path.substring(index + 1);

    Map pdhMap = scan.getProxyDatasetHandlers();
    if (pdhMap == null) return null;

    return (ProxyDatasetHandler) pdhMap.get(proxyName);
  }

  private DatasetScan getMatchingScan(String path) {
    DataRoot reqDataRoot = findDataRoot(path);
    if (reqDataRoot == null)
      return null;

    DatasetScan scan = null;
    if (reqDataRoot.scan != null)
      scan = reqDataRoot.scan;

    return scan;
  }

  public Catalog getProxyDatasetResolverCatalog(String path, URI baseURI) {
    if (!isProxyDatasetResolver(path))
      throw new IllegalArgumentException("Not a proxy dataset resolver path <" + path + ">.");

    DatasetScan scan = this.getMatchingScan(path);
    if (scan == null) return null;

    // Call the matching DatasetScan to make the proxy dataset resolver catalog.
    //noinspection UnnecessaryLocalVariable
    Catalog cat = scan.makeProxyDsResolverCatalog(path, baseURI);

    return cat;
  }  */

  /**
   * If a catalog exists and is allowed (not filtered out) for the given path, return
   * the catalog as an Catalog. Otherwise, return null.
   * <p/>
   * The validity of the returned catalog is not guaranteed. Use Catalog.check() to
   * check that the catalog is valid.
   *
   * @param path    the path for the requested catalog.
   * @param baseURI the base URI for the catalog, used to resolve relative URLs.
   * @return the requested Catalog, or null if catalog does not exist or is not allowed.
   */
  public Catalog getCatalog(String path, URI baseURI) throws IOException {
    if (path == null)
      return null;

    String workPath = path;
    if (workPath.startsWith("/"))
      workPath = workPath.substring(1);

    // Check for static catalog.
    boolean reread = false;
    Catalog catalog = ccManager.getStaticCatalog(workPath);
    if (catalog != null) {  // see if its stale
      CalendarDate expiresDateType = catalog.getExpires();
      if ((expiresDateType != null) && expiresDateType.getMillis() < System.currentTimeMillis())
        reread = true;

    } else {
      reread = ccManager.isStaticCatalogNotInCache(workPath); // see if we know if its a static catalog
    }

    /* its a static catalog that needs to be read
    if (reread) {
      File catFile = this.tdsContext.getConfigFileSource().getFile(workPath);
      if (catFile != null) {
        String catalogFullPath = catFile.getPath();
        logCatalogInit.info("**********\nReading catalog {} at {}\n", catalogFullPath, CalendarDate.present());

        InvCatalogFactory factory = getCatalogFactory(true);
        Catalog reReadCat = readCatalog(factory, workPath, catalogFullPath);

        if (reReadCat != null) {
          catalog = reReadCat;
          if (staticCache) { // a static catalog has been updated
            synchronized (this) {
              reReadCat.setStatic(true);
              staticCatalogHash.put(workPath, reReadCat);
            }
          }
        }

      } else {
        logCatalogInit.error(ERROR + "Static catalog does not exist that we expected = " + workPath);
      }
    }  */

    // LOOK is this needd ??
    /* if ((catalog != null) && catalog.getBaseURI() == null) { for some reason you have to keep setting - is someone setting to null ?
    if (catalog != null) {
      // this is the first time we actually know an absolute, external path for the catalog, so we set it here
      // LOOK however, this causes a possible thread safety problem
      catalog.setBaseURI(baseURI);
    } */

    // Check for dynamic catalog.
    if (catalog == null)
      catalog = makeDynamicCatalog(workPath, baseURI);

    // Check for proxy dataset resolver catalog.
    //if (catalog == null && this.isProxyDatasetResolver(workPath))
    //  catalog = (Catalog) this.getProxyDatasetResolverCatalog(workPath, baseURI);

    return catalog;
  }

  private Catalog makeDynamicCatalog(String path, URI baseURI) throws IOException {
    String workPath = path;

    // Make sure this is a dynamic catalog request.
    if (!path.endsWith("/catalog.xml") && !path.endsWith("/latest.xml"))
      return null;

    // strip off the filename
    int pos = workPath.lastIndexOf("/");
    if (pos >= 0)
      workPath = workPath.substring(0, pos);

    // now look through the data roots for a maximal match
    DataRootMatch match = findDataRootMatch(workPath);
    if (match == null)
      return null;

    // look for the feature Collection
    if (match.dataRoot.getFeatureCollection() != null) {
      boolean isLatest = path.endsWith("/latest.xml");
      if (isLatest)
        return match.dataRoot.getFeatureCollection().makeLatest(match.remaining, path, baseURI);
      else
        return match.dataRoot.getFeatureCollection().makeCatalog(match.remaining, path, baseURI);
    }

    /* Check that path is allowed, ie not filtered out LOOK
    try {
      if (getCrawlableDataset(workPath) == null)
        return null;
    } catch (IOException e) {
      log.error("makeDynamicCatalog(): I/O error on request <" + path + ">: " + e.getMessage(), e);
      return null;
    }

    // at this point, its gotta be a DatasetScan, not a DatasetRoot
    if (match.dataRoot.getDatasetScan() == null) {
      log.warn("makeDynamicCatalog(): No DatasetScan for =" + workPath + " request path= " + path);
      return null;
    } */

    if (path.endsWith("/latest.xml")) return null; // latest is not handled here

    DatasetScan dscan = match.dataRoot.getDatasetScan();
    if (log.isDebugEnabled())
      log.debug("makeDynamicCatalog(): Calling makeCatalogForDirectory( " + baseURI + ", " + path + ").");
    Catalog cat = dscan.makeCatalogForDirectory(path, baseURI);

    if (null == cat) {
      log.error("makeDynamicCatalog(): makeCatalogForDirectory failed = " + workPath);
    }

    return cat;
  }

  /*
   * ***********************************************************************
   */

  // debugging only !!
  public PathMatcher getPathMatcher() {
    return pathMatcher;
  }

  public void showRoots(Formatter f) {
    Iterator iter = pathMatcher.iterator();
    while (iter.hasNext()) {
      DataRoot ds = (DataRoot) iter.next();
      f.format(" %s%n", ds.toString2());
    }
  }


  public void makeDebugActions() {
    DebugController.Category debugHandler = DebugController.find("catalogs");
    DebugController.Action act;
    /* act = new DebugHandler.Action("showError", "Show catalog error logs") {
     public void doAction(DebugHandler.Event e) {
       synchronized ( DataRootHandler.this )
       {
         try {
          File fileOut = new File(contentPath + "logs/catalogError.log");
          FileOutputStream fos = new FileOutputStream(fileOut);
          //String contents = catalogErrorLog.toString();
          thredds.util.IO.writeContents(contents, fos);
          fos.close();
        } catch (IOException ioe) {
          log.error("DataRootHandler.writeCatalogErrorLog error ", ioe);
        }

         e.pw.println( StringUtil.quoteHtmlContent( "\n"+catalogErrorLog.toString()));
       }
     }
   };
   debugHandler.addAction( act);  */

    act = new DebugController.Action("showStatic", "Show static catalogs") {
      public void doAction(DebugController.Event e) {
        StringBuilder sbuff = new StringBuilder();
        synchronized (DataRootHandler.this) {
          List<String> list = ccManager.getStaticCatalogPaths();
          Collections.sort(list);
          for (String catPath : list) {
            ConfigCatalog cat = ccManager.getStaticCatalog(catPath);
            sbuff.append(" catalog= ").append(catPath).append("; ");
            String filename = StringUtil2.unescape(cat.getUriString());
            sbuff.append(" from= ").append(filename).append("\n");
          }
        }
        e.pw.println(StringUtil2.quoteHtmlContent("\n" + sbuff.toString()));
      }
    };
    debugHandler.addAction(act);

    act = new DebugController.Action("showRoots", "Show data roots") {
      public void doAction(DebugController.Event e) {
        synchronized (DataRootHandler.this) {
          Iterator iter = pathMatcher.iterator();
          while (iter.hasNext()) {
            DataRoot ds = (DataRoot) iter.next();
            e.pw.print(" <b>" + ds.getPath() + "</b>");
            String url = DataRootHandler.this.tdsContext.getContextPath() + "/admin/dataDir/" + ds.getPath() + "/";
            String type = (ds.getDatasetScan() == null) ? "root" : "scan";
            e.pw.println(" for " + type + " directory= <a href='" + url + "'>" + ds.getDirLocation() + "</a> ");
          }
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugController.Action("getRoots", "Check data roots") {
      public void doAction(DebugController.Event e) {
        synchronized (DataRootHandler.this) {
          e.pw.print("<pre>\n");
          Iterator iter = pathMatcher.iterator();
          boolean ok = true;
          while (iter.hasNext()) {
            DataRoot ds = (DataRoot) iter.next();
            if ((ds.getDirLocation() == null)) continue;

            try {
              File f = new File(ds.getDirLocation());
              if (!f.exists()) {
                e.pw.print("MISSING on dir = " + ds.getDirLocation() + " for path = " + ds.getPath() + "\n");
                ok = false;
              }
            } catch (Throwable t) {
              e.pw.print("ERROR on dir = " + ds.getDirLocation() + " for path = " + ds.getPath() + "\n");
              e.pw.print(t.getMessage() + "\n");
              ok = false;
            }
          }
          if (ok)
            e.pw.print("ALL OK\n");
          e.pw.print("</pre>\n");
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugController.Action("reinit", "Reinitialize") {
      public void doAction(DebugController.Event e) {
        try {
          singleton.reinit();
          e.pw.println("reinit ok");

        } catch (Exception e1) {
          e.pw.println("Error on reinit " + e1.getMessage());
          log.error("Error on reinit " + e1.getMessage());
        }
      }
    };
    debugHandler.addAction(act);

  }

  /**
   * To receive notice of TDS configuration events, implement this interface
   * and use the DataRootHandler.registerConfigListener() method to register
   * an instance with a DataRootHandler instance.
   * <p/>
   * Configuration events include start and end of configuration, inclusion
   * of a catalog in configuration, and finding a dataset in configuration.
   * <p/>
   * Concurrency issues:<br>
   * 1) As this is a servlet framework, requests that configuration be reinitialized
   * may occur concurrently with requests for the information the listener is
   * keeping. Be careful not to allow access to the information during
   * configuration.<br>
   * 2) The longer the configuration process, the more time there is to have
   * a concurrency issue come up. So, try to keep responses to these events
   * fairly light weight. Or build new configuration information and synchronize
   * and switch only when the already built config information is being switched
   * with the existing config information.
   */
  public interface ConfigListener {
    /**
     * Recieve notification that configuration has started.
     */
    public void configStart();

    /**
     * Recieve notification that configuration has completed.
     */
    public void configEnd();

    /**
     * Recieve notification on the inclusion of a configuration catalog.
     *
     * @param catalog the catalog being included in configuration.
     */
    public void configCatalog(Catalog catalog);

    /**
     * Recieve notification that configuration has found a dataset.
     *
     * @param dataset the dataset found during configuration.
     */
    public void configDataset(Dataset dataset);
  }

}



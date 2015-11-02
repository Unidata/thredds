/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
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
package thredds.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import thredds.catalog.DataRootConfig;
import thredds.catalog.InvCatalog;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetFeatureCollection;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvDatasetScan;
import thredds.catalog.InvProperty;
import thredds.catalog.InvService;
import thredds.cataloggen.ProxyDatasetHandler;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetDods;
import thredds.crawlabledataset.CrawlableDatasetFile;
import thredds.server.admin.DebugController;
import thredds.server.config.AllowableService;
import thredds.server.config.TdsContext;
import thredds.util.*;
import thredds.util.filesource.FileSource;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateType;
import ucar.unidata.util.StringUtil2;

/**
 * The DataRootHandler manages all the "data roots" for a given web application
 * and provides mappings from URLs to catalog and data objects (e.g.,
 * InvCatalog and CrawlableDataset).
 * <p/>
 * <p>The "data roots" are read in from one or more trees of config catalogs
 * and are defined by the datasetScan and datasetRoot elements in the config
 * catalogs.
 * <p/>
 * <p> Uses the singleton design pattern.
 *
 * @author caron
 */
@Component("DataRootHandler")
@DependsOn("CdmInit")
public final class DataRootHandler implements InitializingBean {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataRootHandler.class);
  static private org.slf4j.Logger logCatalogInit = org.slf4j.LoggerFactory.getLogger(DataRootHandler.class.getName() + ".catalogInit");
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

  // dont need to Guard/synchronize singleton, since creation and publication is only done by a servlet init() and therefore only in one thread (per ClassLoader).
  //Spring bean so --> there will be one per context (by default is a singleton in the Spring realm) 
  static private DataRootHandler singleton = null;
  static private final String ERROR = "*** ERROR ";
  static private final boolean debug = false;

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
  //private final TdsContext tdsContext;
  private boolean staticCache;

  // @GuardedBy("this") LOOK should be able to access without synchronization
  private HashMap<String, InvCatalogImpl> staticCatalogHash; // Hash of static catalogs, key = path
  private Set<String> staticCatalogNames; // Hash of static catalogs, key = path

  // @GuardedBy("this")
  private HashSet<String> idHash = new HashSet<>(); // Hash of ids, to look for duplicates

  //  PathMatcher is "effectively immutable"; use volatile for visibilty
  private volatile PathMatcher pathMatcher = new PathMatcher(); // collection of DataRoot objects

  private List<ConfigListener> configListeners = new ArrayList<>();

  private List<PathAliasReplacement> dataRootLocationAliasExpanders = new ArrayList<>();

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
    dataRootLocationAliasExpanders = PathAliasReplacementImpl.makePathAliasReplacements(aliases);
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
        dataRootLocationAliasExpanders.add(new PathAliasReplacementImpl("content", StringUtils.cleanPath(file.getPath())));
    }

    //this.contentPath = this.tdsContext.
    this.initCatalogs();

    this.makeDebugActions();
    DatasetHandler.makeDebugActions();

    //Set the instance
    DataRootHandler.setInstance(this);
  }

 /*  private void getExtraCatalogs(List<String> extraList) {
    // if there are some roots in ThreddsConfig, then dont read extraCatalogs.txt
    ThreddsConfig.getCatalogRoots(extraList);
    if (extraList.size() > 0)
      return;

    // see if extraCatalogs.txt exists
    File file = this.tdsContext.getConfigFileSource().getFile("extraCatalogs.txt");
    if (file != null && file.exists()) {

      try {
        FileInputStream fin = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
        while (true) {
          String line = reader.readLine();
          if (line == null) break;
          line = line.trim();
          if (line.length() == 0) continue;
          if (line.startsWith("#")) continue; // Skip comment lines.
          extraList.add(line);
        }
        fin.close();

      } catch (IOException e) {
        logCatalogInit.error(ERROR+"getExtraCatalogs ", e);
      }
    }
  } */

  public boolean registerConfigListener(ConfigListener cl) {
    if (cl == null) return false;
    if (configListeners.contains(cl)) return false;
    return configListeners.add(cl);
  }

  public boolean unregisterConfigListener(ConfigListener cl) {
    if (cl == null) return false;
    return configListeners.remove(cl);
  }

  // @TODO Should pull the init construction of hashes and such out of synchronization and only synchronize the change over to the constructed hashes. (How would that work with ConfigListeners?)
  // @TODO This method is synchronized seperately from actual initialization which means that requests in between the two calls will fail.

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
    idHash = new HashSet<>();

    DatasetHandler.reinit(); // NcML datasets
    initCatalogs();

    isReinit = false;

    logCatalogInit.info("\n**************************************\n**************************************\nDone with TDS config catalog reinitialization\n["
            + CalendarDate.present() + "]");
  }

  volatile boolean isReinit = false;

  void initCatalogs() {
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


  /**
   * Reads a catalog, finds datasetRoot, datasetScan, datasetFmrc, NcML and restricted access datasets
   * <p/>
   * Only called by synchronized methods.
   *
   * @param path    file path of catalog, reletive to contentPath, ie catalog fullpath = contentPath + path.
   * @param recurse if true, look for catRefs in this catalog
   * @param cache   if true, always cache
   * @throws IOException if reading catalog fails
   */
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
    InvCatalogImpl cat = readCatalog(factory, path, f.getPath());
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
    for (InvService s : cat.getServices()) {
      for (InvProperty p : s.getDatasetRoots()) {
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
    InvCatalogImpl cat = readCatalog(factory, path, absPath);
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
    for (InvService s : cat.getServices()) {
      for (InvProperty p : s.getDatasetRoots()) {
        addRoot(p.getName(), p.getValue(), true);
      }
    }
  }
  
  /*private void checkServices(InvService service, String path ){

	  if( service.getServiceType() == ServiceType.COMPOUND ){
		  for(InvService s : service.getServices() ){
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
	  
  }*/

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
   * @return the InvCatalogImpl, or null if failure
   */
  private InvCatalogImpl readCatalog(InvCatalogFactory factory, String path, String catalogFullPath) {
    URI uri;
    try {
      uri = new URI("file:" + StringUtil2.escape(catalogFullPath, "/:-_.")); // LOOK needed ?
    } catch (URISyntaxException e) {
      logCatalogInit.error(ERROR + "readCatalog(): URISyntaxException=" + e.getMessage());
      return null;
    }

    // read the catalog
    logCatalogInit.info("\n-------readCatalog(): full path=" + catalogFullPath + "; path=" + path);
    InvCatalogImpl cat = null;
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
   * @param dsList the list of InvDatasetImpl
   * @return true if the containing catalog should be cached
   */
  private boolean initSpecialDatasets(List<InvDataset> dsList) {
    boolean needsCache = false;

    Iterator<InvDataset> iter = dsList.iterator();
    while (iter.hasNext()) {
      InvDatasetImpl invDataset = (InvDatasetImpl) iter.next();

      // look for duplicate ids
      String id = invDataset.getUniqueID();
      if (id != null) {
        if (idHash.contains(id)) {
          logCatalogInit.error(ERROR + "Duplicate id on  '" + invDataset.getFullName() + "' id= '" + id + "'");
        } else {
          idHash.add(id);
        }
      }

      // Notify listeners of config datasets.
      for (ConfigListener cl : configListeners)
        cl.configDataset(invDataset);

      if (invDataset instanceof InvDatasetScan) {
        InvDatasetScan ds = (InvDatasetScan) invDataset;
        InvService service = ds.getServiceDefault();
        if (service == null) {
          logCatalogInit.error(ERROR + "InvDatasetScan " + ds.getFullName() + " has no default Service - skipping");
          continue;
        }
        if (!addRoot(ds))
          iter.remove();

      } else if (invDataset instanceof InvDatasetFeatureCollection) {
        InvDatasetFeatureCollection fc = (InvDatasetFeatureCollection) invDataset;
        addRoot(fc);
        needsCache = true;

        // not a DatasetScan or InvDatasetFmrc or InvDatasetFeatureCollection
      } else if (invDataset.getNcmlElement() != null) {
        DatasetHandler.putNcmlDataset(invDataset.getUrlPath(), invDataset);
      }

      if (!(invDataset instanceof InvCatalogRef)) {
        // recurse
        initSpecialDatasets(invDataset.getDatasets());
      }
    }

    return needsCache;
  }

  // Only called by synchronized methods
  private void initFollowCatrefs(String dirPath, List<InvDataset> datasets) throws IOException {
    for (InvDataset invDataset : datasets) {

      if ((invDataset instanceof InvCatalogRef) && !(invDataset instanceof InvDatasetScan)
              && !(invDataset instanceof InvDatasetFeatureCollection)) {
        InvCatalogRef catref = (InvCatalogRef) invDataset;
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

      } else if (!(invDataset instanceof InvDatasetScan) && !(invDataset instanceof InvDatasetFeatureCollection)) {
        // recurse through nested datasets
        initFollowCatrefs(dirPath, invDataset.getDatasets());
      }
    }
  }

  // Only called by synchronized methods
  private boolean addRoot(InvDatasetScan dscan) {
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

    // Check whether InvDatasetScan is valid before adding.
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

  public List<InvDatasetFeatureCollection> getFeatureCollections() {
    List<InvDatasetFeatureCollection> result = new ArrayList<>();
    Iterator iter = pathMatcher.iterator();
    while (iter.hasNext()) {
      DataRoot droot = (DataRoot) iter.next();
      if (droot.featCollection != null)
        result.add(droot.featCollection);
    }
    return result;
  }

  public InvDatasetFeatureCollection findFcByCollectionName(String collectionName) {
    Iterator iter = pathMatcher.iterator();
    while (iter.hasNext()) {
      DataRoot droot = (DataRoot) iter.next();
      if ((droot.featCollection != null) && droot.featCollection.getCollectionName().equals(collectionName))
        return droot.featCollection;
    }
    return null;
  }


  // Only called by synchronized methods
  private boolean addRoot(InvDatasetFeatureCollection fc) {
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
  }

  ////////////////////////////////////////////////////////////////////////////////////////

  static public class DataRootMatch {
    String rootPath;     // this is the matching part of the URL
    String remaining;   // this is the part of the URL that didnt match
    String dirLocation;   // this is the directory that should be substituted for the rootPath
    DataRoot dataRoot;  // this is the directory that should be substituted for the rootPath
  }

  static public class DataRoot {
    private String path;         // match this path
    private String dirLocation;  // to this directory
    private InvDatasetScan scan; // the InvDatasetScan that created this (may be null)
    private InvDatasetFeatureCollection featCollection; // the InvDatasetFeatureCollection that created this (may be null)
    private boolean cache = true;

    // Use this to access CrawlableDataset in dirLocation.
    // I.e., used by datasets that reference a <datasetRoot>
    private InvDatasetScan datasetRootProxy;

    DataRoot(InvDatasetFeatureCollection featCollection) {
      setPath(featCollection.getPath());
      this.featCollection = featCollection;
      this.dirLocation = featCollection.getTopDirectoryLocation();
      show();
    }

    DataRoot(InvDatasetScan scan) {
      setPath(scan.getPath());
      this.scan = scan;
      this.dirLocation = scan.getScanLocation();
      this.datasetRootProxy = null;
      show();
    }

    DataRoot(String path, String dirLocation, boolean cache) {
      setPath(path);
      this.dirLocation = dirLocation;
      this.cache = cache;
      this.scan = null;

      makeProxy();
      show();
    }

    private void setPath(String path) {
      // if (path.endsWith("/")) path = path + "/";
      this.path = path;
    }


    private void show() {
      if (debug) System.out.printf(" DataRoot %s==%s%n", path, dirLocation);
    }

    void makeProxy() {
      /*   public InvDatasetScan( InvDatasetImpl parent, String name, String path, String scanLocation,
                         String configClassName, Object configObj, CrawlableDatasetFilter filter,
                         CrawlableDatasetLabeler identifier, CrawlableDatasetLabeler namer,
                         boolean addDatasetSize,
                         CrawlableDatasetSorter sorter, Map proxyDatasetHandlers,
                         List childEnhancerList, CatalogRefExpander catalogRefExpander ) */
      this.datasetRootProxy = new InvDatasetScan(null, "", this.path, this.dirLocation, null, null, null, null, null, false, null, null, null, null);
    }

    public InvDatasetScan getScan() {
      return scan;
    }

    public InvDatasetFeatureCollection getFeatCollection() {
      return featCollection;
    }

    public boolean isCache() {
      return cache;
    }

    public InvDatasetScan getDatasetRootProxy() {
      return datasetRootProxy;
    }

    // used by PathMatcher
    public String toString() {
      return path;
    }

    // debug
    public String toString2() {
      return path + "," + dirLocation;
    }

    /**
     * Instances which have same path are equal.
     */
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      DataRoot root = (DataRoot) o;
      return path.equals(root.path);
    }

    public int hashCode() {
      return path.hashCode();
    }
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
    return (dataRoot == null) ? null : dataRoot.dirLocation;
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
    match.rootPath = dataRoot.path;
    match.remaining = spath.substring(match.rootPath.length());
    if (match.remaining.startsWith("/"))
      match.remaining = match.remaining.substring(1);
    match.dirLocation = dataRoot.dirLocation;
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
      if (log.isDebugEnabled()) log.debug("hasDataRootMatch(): no InvDatasetScan for " + path);
      return false;
    }
    return true;
  }

  /**
   * Return the CrawlableDataset to which the given path maps, null if the
   * dataset does not exist or the matching InvDatasetScan filters out the
   * requested CrawlableDataset.
   * <p/>
   * Use this method to check that a data request is requesting an allowed dataset.
   *
   * @param path the request path.
   * @return the requested CrawlableDataset or null if the requested dataset is not allowed by the matching InvDatasetScan.
   * @throws IOException if an I/O error occurs while locating the requested dataset.
   */
  public CrawlableDataset getCrawlableDataset(String path)
          throws IOException {
    if (path.length() > 0) {
      if (path.startsWith("/"))
        path = path.substring(1);
    }

    DataRoot reqDataRoot = findDataRoot(path);
    if (reqDataRoot == null)
      return null;

    if (reqDataRoot.scan != null)
      return reqDataRoot.scan.requestCrawlableDataset(path);

    if (reqDataRoot.featCollection != null)
      return null; // if featCollection exists, bail out and deal with it in caller

    // must be a data root
    if (reqDataRoot.dirLocation != null) {
      if (reqDataRoot.datasetRootProxy == null)
        reqDataRoot.makeProxy();
      return reqDataRoot.datasetRootProxy.requestCrawlableDataset(path);
    }

    return null;
  }

  /**
   * Return the java.io.File represented by the CrawlableDataset to which the
   * given path maps. Null is returned if the dataset does not exist, the
   * matching InvDatasetScan or DataRoot filters out the requested
   * CrawlableDataset, the CrawlableDataset does not represent a File
   * (i.e., it is not a CrawlableDatasetFile), or an I/O error occurs while
   * locating the requested dataset.
   *
   * @param path the request path.
   * @return the requested java.io.File or null.
   * @throws IllegalStateException if the request is not for a descendant of (or the same as) the matching DatasetRoot collection location.
   */
  public File getCrawlableDatasetAsFile(String path) {
    if (path.length() > 0) {
      if (path.startsWith("/"))
        path = path.substring(1);
    }

    // hack in the fmrc for fileServer
    DataRootMatch match = findDataRootMatch(path);
    if (match == null)
      return null;
    if (match.dataRoot.featCollection != null)
      return match.dataRoot.featCollection.getFile(match.remaining);

    CrawlableDataset crDs;
    try {
      crDs = getCrawlableDataset(path);
    } catch (IOException e) {
      return null;
    }
    if (crDs == null) return null;
    File retFile = null;
    if (crDs instanceof CrawlableDatasetFile)
      retFile = ((CrawlableDatasetFile) crDs).getFile();

    return retFile;
  }

  /**
   * Return the OPeNDAP URI represented by the CrawlableDataset to which the
   * given path maps. Null is returned if the dataset does not exist, the
   * matching InvDatasetScan or DataRoot filters out the requested
   * CrawlableDataset, the CrawlableDataset is not a CrawlableDatasetDods,
   * or an I/O error occurs while locating the requested dataset.
   *
   * @param path the request path.
   * @return the requested OPeNDAP URI or null.
   * @throws IllegalStateException if the request is not for a descendant of (or the same as) the matching DatasetRoot collection location.
   */
  public URI getCrawlableDatasetAsOpendapUri(String path) {
    if (path.length() > 0) {
      if (path.startsWith("/"))
        path = path.substring(1);
    }

    CrawlableDataset crDs;
    try {
      crDs = getCrawlableDataset(path);
    } catch (IOException e) {
      return null;
    }
    if (crDs == null) return null;
    URI retUri = null;
    if (crDs instanceof CrawlableDatasetDods)
      retUri = ((CrawlableDatasetDods) crDs).getUri();

    return retUri;
  }

  public boolean isProxyDataset(String path) {
    ProxyDatasetHandler pdh = this.getMatchingProxyDataset(path);
    return pdh != null;
  }

  public boolean isProxyDatasetResolver(String path) {
    ProxyDatasetHandler pdh = this.getMatchingProxyDataset(path);
    if (pdh == null)
      return false;

    return pdh.isProxyDatasetResolver();
  }

  private ProxyDatasetHandler getMatchingProxyDataset(String path) {
    InvDatasetScan scan = this.getMatchingScan(path);
    if (null == scan) return null;

    int index = path.lastIndexOf("/");
    String proxyName = path.substring(index + 1);

    Map pdhMap = scan.getProxyDatasetHandlers();
    if (pdhMap == null) return null;

    return (ProxyDatasetHandler) pdhMap.get(proxyName);
  }

  private InvDatasetScan getMatchingScan(String path) {
    DataRoot reqDataRoot = findDataRoot(path);
    if (reqDataRoot == null)
      return null;

    InvDatasetScan scan = null;
    if (reqDataRoot.scan != null)
      scan = reqDataRoot.scan;

    return scan;
  }

  public InvCatalog getProxyDatasetResolverCatalog(String path, URI baseURI) {
    if (!isProxyDatasetResolver(path))
      throw new IllegalArgumentException("Not a proxy dataset resolver path <" + path + ">.");

    InvDatasetScan scan = this.getMatchingScan(path);
    if (scan == null) return null;

    // Call the matching InvDatasetScan to make the proxy dataset resolver catalog.
    //noinspection UnnecessaryLocalVariable
    InvCatalogImpl cat = scan.makeProxyDsResolverCatalog(path, baseURI);

    return cat;
  }

//  public CrawlableDataset getProxyDatasetActualDatset( String path)
//  {
//    ProxyDatasetHandler pdh = this.getMatchingProxyDataset( path );
//    if ( pdh == null )
//      return null;
//
//    pdh.getActualDataset( );
//
//  }

  /**
   * @deprecated
   */
  public void handleRequestForProxyDatasetResolverCatalog(HttpServletRequest req, HttpServletResponse res)
          throws IOException {
    String path = TdsPathUtils.extractPath(req, null);
    if (!isProxyDatasetResolver(path)) {
      String resMsg = "Request <" + path + "> not for proxy dataset resolver.";
      log.error("handleRequestForProxyDatasetResolverCatalog(): " + resMsg);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg);
      return;

    }

    String baseUriString = req.getRequestURL().toString();
    URI baseURI;
    try {
      baseURI = new URI(baseUriString);
    } catch (URISyntaxException e) {
      String resMsg = "Request URL <" + baseUriString + "> not a valid URI: " + e.getMessage();
      log.error("handleRequestForProxyDatasetResolverCatalog(): " + resMsg);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg);
      return;
    }

    InvCatalogImpl cat = (InvCatalogImpl) this.getProxyDatasetResolverCatalog(path, baseURI);
    if (cat == null) {
      String resMsg = "Could not generate proxy dataset resolver catalog <" + path + ">.";
      log.error("handleRequestForProxyDatasetResolverCatalog(): " + resMsg);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg);
      return;
    }

    // Return catalog as XML response.
    InvCatalogFactory catFactory = getCatalogFactory(false);
    String result = catFactory.writeXML(cat);

    res.setContentType(ContentType.xml.getContentHeader());
    thredds.servlet.ServletUtil.setResponseContentLength(res, result);
    PrintWriter pw = res.getWriter();
    pw.write(result);
  }

  /**
   * DO NOT USE, this is Ethan's attempt at designing a generic way to handle data requests.
   * <p/>
   * Only used is in ExampleThreddsServlet.
   *
   * @throws IOException
   * @deprecated DO NOT USE
   */
  public void handleRequestForDataset(String path, DataServiceProvider dsp, HttpServletRequest req, HttpServletResponse res)
          throws IOException {
    // Can the DataServiceProvider handle the data request given by the path?
    DataServiceProvider.DatasetRequest dsReq = dsp.getRecognizedDatasetRequest(path, req);
    String crDsPath;
    boolean dspCanHandle = false;
    if (dsReq != null) {
      String dsPath = dsReq.getDatasetPath();
      if (dsPath != null) {
        // Use the path returned by the DataServiceProvider.
        crDsPath = dsPath;
        dspCanHandle = true;
      } else {
        // DataServiceProvider recognized request path but returned a null dataset path. Hmm?
        log.warn("handleRequestForDataset(): DataServiceProvider recognized request path <" + path + "> but returned a null dataset path, using request path.");
        // Use the incoming request path.
        crDsPath = path;
      }
    } else {
      // DataServiceProvider  did not recognized request path.
      // Use the incoming request path.
      crDsPath = path;
    }

    // Find the CrawlableDataset represented by the request path.
    CrawlableDataset crDs = this.getCrawlableDataset(crDsPath);
    if (crDs == null) {
      // @todo Check if this is a proxy dataset request (not resolver).
      // Request is not for a known (or allowed) dataset.
      res.sendError(HttpServletResponse.SC_NOT_FOUND); // 404
      return;
    }

    if (dspCanHandle) {
      // Request recognized by DataServiceProvider, handle dataset request.
      dsp.handleRequestForDataset(dsReq, crDs, req, res);

    } else {
      // Request not recognized by DataServiceProvider.
      if (crDs.isCollection()) {
        // Handle request for a collection dataset.
        dsp.handleUnrecognizedRequestForCollection(crDs, req, res);
        return;
      }

      // Handle request for an atomic dataset.
      dsp.handleUnrecognizedRequest(crDs, req, res);
    }
  }

  /*
   * Check whether the given path is a request for a catalog. Before checking,
   * converts paths ending with "/" to end with "/catalog.xml" and converts
   * paths ending with ".html" to end with ".xml".
   *
   * @param path the request path
   * @return true if the path is a request for a catalog, false otherwise.
   * @deprecated actually, this is experimental
   *
  public boolean isRequestForCatalog(String path) {
    String workPath = path;
    if (workPath == null)
      return false;
    else if (workPath.endsWith("/")) {
      workPath = workPath + "catalog.xml";
    } else if (workPath.endsWith(".html")) {
      // Change ".html" to ".xml"
      int len = workPath.length();
      workPath = workPath.substring(0, len - 4) + "xml";
    } else if (!workPath.endsWith(".xml")) {
      // Not a catalog request.
      return false;
    }

    boolean hasCatalog = false;

    if (workPath.startsWith("/"))
      workPath = workPath.substring(1);

    // Check for static catalog.
    synchronized (this) {
      if (staticCatalogHash.containsKey(workPath))
        return true;
    }

    //----------------------
    DataRootMatch match = findDataRootMatch(workPath);
    if (match == null)
      return false;

//    // look for the fmrc
//    if ( match.dataRoot.fmrc != null )
//    {
//      return match.dataRoot.fmrc.makeCatalog( match.remaining, path, baseURI );
//    }
//
//    // Check that path is allowed, ie not filtered out
//    try
//    {
//      if ( getCrawlableDataset( workPath ) == null )
//        return null;
//    }
//    catch ( IOException e )
//    {
//      log.error( "makeDynamicCatalog(): I/O error on request <" + path + ">: " + e.getMessage(), e );
//      return null;
//    }
//
//    // at this point, its gotta be a DatasetScan, not a DatasetRoot
//    if ( match.dataRoot.scan == null )
//    {
//      log.warn( "makeDynamicCatalog(): No InvDatasetScan for =" + workPath + " request path= " + path );
//      return null;
//    }
//
//    InvDatasetScan dscan = match.dataRoot.scan;
//    log.debug( "Calling makeCatalogForDirectory( " + baseURI + ", " + path + ")." );
//    InvCatalogImpl cat = dscan.makeCatalogForDirectory( path, baseURI );
//
//    if ( null == cat )
//    {
//      log.error( "makeCatalogForDirectory failed = " + workPath );
//    }
//
//    //----------------------
//
//    // Check for proxy dataset resolver catalog.
//    if ( catalog == null && this.isProxyDatasetResolver( workPath ) )
//      catalog = (InvCatalogImpl) this.getProxyDatasetResolverCatalog( workPath, baseURI );

    //----------------------


    return hasCatalog;
  } */

  /**
   * This looks to see if this is a request for a catalog.
   * The request must end in ".html" or ".xml"  or "/"
   * Check to see if a static or dynamic catalog exists for it.
   * If so, it processes the request completely.
   * req.getPathInfo() is used for the path (ie ignores context and servlet path)
   *
   * @param req the request
   * @param res the response
   * @return true if request was handled
   * @throws IOException      on I/O error
   * @throws ServletException on other errors
   * @deprecated Instead forward() request to
   */
  public boolean processReqForCatalog(HttpServletRequest req, HttpServletResponse res)
          throws IOException, ServletException {

    // LOOK convoluted, clean this up!
    String catPath = TdsPathUtils.extractPath(req, "catalog/");
    if (catPath == null)
      return false;

    // Handle case for root catalog.
    // ToDo Is this needed? OPeNDAP no longer allows "/thredds/dodsC/catalog.html"
    if (catPath.equals(""))
      catPath = "catalog.html";

    if (catPath.endsWith("/"))
      catPath += "catalog.html";

    if (!catPath.endsWith(".xml") && !catPath.endsWith(".html"))
      return false;

    // Forward request to the "/catalog" servlet.
    String path = "/catalog/" + catPath;

    // Handle case for root catalog.
    // ToDo Is this needed? OPeNDAP no longer allows "/thredds/dodsC/catalog.html"
    if (catPath.equals("catalog.html") || catPath.equals("catalog.xml"))
      path = "/" + catPath;

    RequestForwardUtils.forwardRequestRelativeToCurrentContext(path, req, res);

    return true;
  }

  /**
   * If a catalog exists and is allowed (not filtered out) for the given path, return
   * the catalog as an InvCatalog. Otherwise, return null.
   * <p/>
   * The validity of the returned catalog is not guaranteed. Use InvCatalog.check() to
   * check that the catalog is valid.
   *
   * @param path    the path for the requested catalog.
   * @param baseURI the base URI for the catalog, used to resolve relative URLs.
   * @return the requested InvCatalog, or null if catalog does not exist or is not allowed.
   */
  public InvCatalog getCatalog(String path, URI baseURI) throws IOException {
    if (path == null)
      return null;

    String workPath = path;
    if (workPath.startsWith("/"))
      workPath = workPath.substring(1);

    // Check for static catalog.
    boolean reread = false;
    InvCatalogImpl catalog = staticCatalogHash.get(workPath);
    if (catalog != null) {  // see if its stale
      DateType expiresDateType = catalog.getExpires();
      if ((expiresDateType != null) && expiresDateType.getCalendarDate().getMillis() < System.currentTimeMillis())
        reread = true;

    } else if (!staticCache) {
      reread = staticCatalogNames.contains(workPath); // see if we know if its a static catalog
    }

    // its a static catalog that needs to be read
    if (reread) {
      File catFile = this.tdsContext.getConfigFileSource().getFile(workPath);
      if (catFile != null) {
        String catalogFullPath = catFile.getPath();
        logCatalogInit.info("**********\nReading catalog {} at {}\n", catalogFullPath, CalendarDate.present());

        InvCatalogFactory factory = getCatalogFactory(true);
        InvCatalogImpl reReadCat = readCatalog(factory, workPath, catalogFullPath);

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
    }

    // if ((catalog != null) && catalog.getBaseURI() == null) { for some reason you have to keep setting - is someone setting to null ?
    if (catalog != null) {
      // this is the first time we actually know an absolute, external path for the catalog, so we set it here
      // LOOK however, this causes a possible thread safety problem
      catalog.setBaseURI(baseURI);
    }

    // Check for dynamic catalog.
    if (catalog == null)
      catalog = makeDynamicCatalog(workPath, baseURI);

    // Check for proxy dataset resolver catalog.
    if (catalog == null && this.isProxyDatasetResolver(workPath))
      catalog = (InvCatalogImpl) this.getProxyDatasetResolverCatalog(workPath, baseURI);

    return catalog;
  }

  private InvCatalogImpl makeDynamicCatalog(String path, URI baseURI) throws IOException {
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
    if (match.dataRoot.featCollection != null) {
      boolean isLatest = path.endsWith("/latest.xml");
      if (isLatest)
        return match.dataRoot.featCollection.makeLatest(match.remaining, path, baseURI);
      else
        return match.dataRoot.featCollection.makeCatalog(match.remaining, path, baseURI);
    }

    // Check that path is allowed, ie not filtered out
    try {
      if (getCrawlableDataset(workPath) == null)
        return null;
    } catch (IOException e) {
      log.error("makeDynamicCatalog(): I/O error on request <" + path + ">: " + e.getMessage(), e);
      return null;
    }

    // at this point, its gotta be a DatasetScan, not a DatasetRoot
    if (match.dataRoot.scan == null) {
      log.warn("makeDynamicCatalog(): No InvDatasetScan for =" + workPath + " request path= " + path);
      return null;
    }

    if (path.endsWith("/latest.xml")) return null; // latest is not handled here

    InvDatasetScan dscan = match.dataRoot.scan;
    if (log.isDebugEnabled())
      log.debug("makeDynamicCatalog(): Calling makeCatalogForDirectory( " + baseURI + ", " + path + ").");
    InvCatalogImpl cat = dscan.makeCatalogForDirectory(path, baseURI);

    if (null == cat) {
      log.error("makeDynamicCatalog(): makeCatalogForDirectory failed = " + workPath);
    }

    return cat;
  }

  /**
   * Process a request for the "latest" dataset. This must be configured
   * through the datasetScan element. Typically you call if the path ends with
   * "latest.xml". Actually this is a "resolver" service, that returns a
   * catalog.xml containing latest dataset.
   *
   * @param servlet requesting servlet
   * @param req     request
   * @param res     response
   * @return true if request was processed successfully.
   * @throws IOException if have I/O trouble writing response.
   * @deprecated Instead use {@link #processReqForCatalog(HttpServletRequest, HttpServletResponse) processReqForCatalog()} which provides more general proxy dataset handling.
   */
  public boolean processReqForLatestDataset(HttpServlet servlet, HttpServletRequest req, HttpServletResponse res) throws IOException {
    String orgPath = TdsPathUtils.extractPath(req, null);

    String path = orgPath;

    // strip off the filename
    int pos = path.lastIndexOf("/");
    if (pos >= 0) {
      path = path.substring(0, pos);
    }

    if (path.equals("/") || path.equals("")) {
      String resMsg = "No data at root level, '/latest.xml' request not available.";
      if (log.isDebugEnabled()) log.debug("processReqForLatestDataset(): " + resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    // Find InvDatasetScan with a maximal match.
    DataRoot dataRoot = findDataRoot(path);
    if (dataRoot == null) {
      String resMsg = "No scan root matches requested path <" + path + ">.";
      log.warn("processReqForLatestDataset(): " + resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    // its gotta be a DatasetScan, not a DatasetRoot
    InvDatasetScan dscan = dataRoot.scan;
    if (dscan == null) {
      String resMsg = "Probable conflict between datasetScan and datasetRoot for path <" + path + ">.";
      log.warn("processReqForLatestDataset(): " + resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    // Check that latest is allowed
    if (dscan.getProxyDatasetHandlers() == null) {
      String resMsg = "No \"addProxies\" or \"addLatest\" on matching scan root <" + path + ">.";
      log.warn("processReqForLatestDataset(): " + resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    String reqBase = ServletUtil.getRequestBase(req); // this is the base of the request
    URI reqBaseURI;
    try {
      reqBaseURI = new URI(reqBase);
    } catch (URISyntaxException e) {
      String resMsg = "Request base URL <" + reqBase + "> not valid URI (???): " + e.getMessage();
      log.error("processReqForLatestDataset(): " + resMsg);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg);
      return false;
    }
    InvCatalog cat = dscan.makeLatestCatalogForDirectory(orgPath, reqBaseURI);

    if (null == cat) {
      String resMsg = "Failed to build response catalog <" + path + ">.";
      log.error("processReqForLatestDataset(): " + resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    // Send latest.xml catalog as response.
    InvCatalogFactory catFactory = getCatalogFactory(false);
    String catAsString = catFactory.writeXML((InvCatalogImpl) cat);
    res.setContentType(ContentType.xml.getContentHeader());
    res.setStatus(HttpServletResponse.SC_OK);
    PrintWriter out = res.getWriter();
    out.print(catAsString);
    if (log.isDebugEnabled()) log.debug("processReqForLatestDataset(): Finished \"" + orgPath + "\".");
    return true;
  }

  /**
   * Try to match the given path with all available data roots. If match, then see if there is an NcML document associated
   * with the path.
   *
   * @param path the reletive path, ie req.getServletPath() + req.getPathInfo()
   * @return the NcML (as a JDom element) assocated assocated with this path, or null if no dataroot matches, or no associated NcML.
   */
  public org.jdom2.Element getNcML(String path) {
    if (path.startsWith("/"))
      path = path.substring(1);

    DataRoot dataRoot = findDataRoot(path);
    if (dataRoot == null) {
      if (log.isDebugEnabled()) log.debug("_getNcML no InvDatasetScan for =" + path);
      return null;
    }

    InvDatasetScan dscan = dataRoot.scan;
    if (dscan == null) dscan = dataRoot.datasetRootProxy;
    if (dscan == null) return null;
    return dscan.getNcmlElement();
  }

  /*   public boolean isRequestForConfigCatalog(String path) {
    String catPath = path;
    if (catPath.endsWith(".html")) {
      // Change ".html" to ".xml"
      int len = catPath.length();
      catPath = catPath.substring(0, len - 4) + "xml";
    } else if (! catPath.endsWith(".xml")) {
      // Not a catalog request.
      return false;
    }

    if (staticHash.containsKey(catPath)) return true;
    if (catHasScanHash.get(catPath) != null) return true;
    return false;
  }

  public boolean isRequestForGeneratedCatalog(String path) {
    if (path.endsWith("/")
            || path.endsWith("/catalog.html")
            || path.endsWith("/catalog.xml")) {
      String dsPath = path;

      // strip off the filename
      int pos = dsPath.lastIndexOf("/");
      if (pos >= 0)
        dsPath = dsPath.substring(0, pos);

      // Check that path is allowed.
      CrawlableDataset crDs = findRequestedDataset(dsPath);
      if (crDs == null)
        return false;
      if (crDs.isCollection())
        return true;
    }
    return false;
  } */


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
        ArrayList<String> list;
        StringBuilder sbuff = new StringBuilder();
        synchronized (DataRootHandler.this) {
          list = new ArrayList<>(staticCatalogHash.keySet());
          Collections.sort(list);
          for (String catPath : list) {
            InvCatalogImpl cat = staticCatalogHash.get(catPath);
            sbuff.append(" catalog= ").append(catPath).append("; ");
            String filename = StringUtil2.unescape(cat.getCreateFrom());
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
            e.pw.print(" <b>" + ds.path + "</b>");
            String url = DataRootHandler.this.tdsContext.getContextPath() + "/admin/dataDir/" + ds.path + "/";
            String type = (ds.scan == null) ? "root" : "scan";
            e.pw.println(" for " + type + " directory= <a href='" + url + "'>" + ds.dirLocation + "</a> ");
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
            if ((ds.dirLocation == null)) continue;

            try {
              File f = new File(ds.dirLocation);
              if (!f.exists()) {
                e.pw.print("MISSING on dir = " + ds.dirLocation + " for path = " + ds.path + "\n");
                ok = false;
              }
            } catch (Throwable t) {
              e.pw.print("ERROR on dir = " + ds.dirLocation + " for path = " + ds.path + "\n");
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
    public void configCatalog(InvCatalog catalog);

    /**
     * Recieve notification that configuration has found a dataset.
     *
     * @param dataset the dataset found during configuration.
     */
    public void configDataset(InvDataset dataset);
  }

}


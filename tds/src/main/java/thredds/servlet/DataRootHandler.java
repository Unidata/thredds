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
package thredds.servlet;

import org.quartz.*;
import thredds.catalog.*;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFile;
import thredds.crawlabledataset.CrawlableDatasetDods;
import thredds.cataloggen.ProxyDatasetHandler;
import thredds.inventory.CollectionUpdater;
import thredds.server.config.TdsContext;
import thredds.util.PathAliasReplacement;
import thredds.util.StartsWithPathAliasReplacement;
import thredds.util.TdsPathUtils;
import thredds.util.RequestForwardUtils;
import ucar.nc2.units.DateType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.ServletException;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;

import org.springframework.util.StringUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.StringUtil;

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
public class DataRootHandler {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataRootHandler.class);
  static private org.slf4j.Logger logCatalogInit = org.slf4j.LoggerFactory.getLogger(DataRootHandler.class.getName() + ".catalogInit");
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

  // dont need to Guard/synchronize singleton, since creation and publication is only done by a servlet init() and therefore only in one thread (per ClassLoader).
  static private DataRootHandler singleton = null;

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
      logCatalogInit.error("getInstance(): Called without setInstance() having been called.");
      throw new IllegalStateException("setInstance() must be called first.");
    }
    return singleton;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  private final TdsContext tdsContext;
  private boolean staticCache;

  // @GuardedBy("this") LOOK should be able to access without synchronization
  private HashMap<String, InvCatalogImpl> staticCatalogHash; // Hash of static catalogs, key = path
  private Set<String> staticCatalogNames; // Hash of static catalogs, key = path

  // @GuardedBy("this")
  private HashSet<String> idHash = new HashSet<String>(); // Hash of ids, to look for duplicates

  //  PathMatcher is "effectively immutable"; use volatile for visibilty
  private volatile PathMatcher pathMatcher = new PathMatcher(); // collection of DataRoot objects

  private List<ConfigListener> configListeners = new ArrayList<ConfigListener>();

  /**
   * Constructor.
   * Managed bean - dont do nuttin else !!
   *
   * @param tdsContext
   */
  public DataRootHandler(TdsContext tdsContext) {
    this.tdsContext = tdsContext;
  }

  private PathAliasReplacement contentPathAliasReplacement = null;
  private PathAliasReplacement iddDataRootPathAliasReplacement = null;
  private List<PathAliasReplacement> fullDataRootLocationAliasExpanders = new ArrayList<PathAliasReplacement>();
  private List<PathAliasReplacement> dataRootLocAliasExpanders = Collections.emptyList();

  public void setDataRootLocationAliasExpanders(List<PathAliasReplacement> dataRootLocAliasExpanders) {
    if (dataRootLocAliasExpanders != null)
      this.dataRootLocAliasExpanders = new ArrayList<PathAliasReplacement>(dataRootLocAliasExpanders);
    this.updateFullDataRootLocationAliasExpanders(this.dataRootLocAliasExpanders);
  }

  public List<PathAliasReplacement> getDataRootLocationAliasExpanders() {
    return Collections.unmodifiableList(this.dataRootLocAliasExpanders);
  }

  /////////////////////
  // could use Spring DI
  /*
  private org.quartz.Scheduler scheduler = null;
  public void initScheduler() {
    SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
    try {
      scheduler = schedFact.getScheduler();
      scheduler.start();
      logScan.info(scheduler.getMetaData().toString());
      
    } catch (SchedulerException e) {
      logScan.error("cronExecutor failed to initialize", e);
      scheduler = null;
    }
  }

  private static final String FC_NAME= "fc";
  private void scheduleTasks(InvDatasetFeatureCollection invFeatCollection) {
    if (scheduler == null) return;
    FeatureCollectionConfig config = invFeatCollection.getConfig();

    JobDetail updateJob = new JobDetail(config.spec, "FeatureCollection", ScanFmrcJob.class);
    org.quartz.JobDataMap map = new org.quartz.JobDataMap();
    map.put(FC_NAME, invFeatCollection);
    updateJob.setJobDataMap(map);

    FeatureCollectionConfig.UpdateConfig update = config.updateConfig;
    if (update.startup) {
      // wait 30 secs to trigger
      Date runTime = new Date(new Date().getTime() + 30 * 1000);
      Trigger trigger0 = new SimpleTrigger(config.spec, "startup", runTime);
      try {
        scheduler.scheduleJob(updateJob, trigger0);
        logScan.info("Schedule startup scan for "+config.spec+" at "+ runTime);
      } catch (SchedulerException e) {
        logScan.error("cronExecutor failed to schedule startup Job", e);
        //e.printStackTrace();
      }
    }

    if (update.rescan != null) {
      try {
        Trigger trigger1 = new CronTrigger(config.spec, "rescan", update.rescan);
        if (update.startup) {
          trigger1.setJobName(updateJob.getName());
          trigger1.setJobGroup(updateJob.getGroup());
          scheduler.scheduleJob(trigger1);
        } else {
          scheduler.scheduleJob(updateJob, trigger1);
        }
        logScan.info("Schedule recurring scan for "+config.spec+" cronExpr="+ update.rescan);
      } catch (ParseException e) {
        logScan.error("cronExecutor failed: bad cron expression= "+ update.rescan, e);
      } catch (SchedulerException e) {
        logScan.error("cronExecutor failed to schedule cron Job", e);
        e.printStackTrace();
      }
    }

   FeatureCollectionConfig.ProtoConfig pconfig = config.protoConfig;
    if (pconfig.change != null) {
      JobDetail protoJob = new JobDetail(config.spec, "fcProto", RereadProtoJob.class);
      org.quartz.JobDataMap pmap = new org.quartz.JobDataMap();
      pmap.put(FC_NAME, invFeatCollection);
      protoJob.setJobDataMap(pmap);

      try {
        Trigger trigger2 = new CronTrigger(config.spec, "rereadProto", pconfig.change);
        scheduler.scheduleJob(protoJob, trigger2);
        logScan.info("Schedule Reread Proto for "+config.spec);
      } catch (ParseException e) {
        logScan.error("cronExecutor failed: RereadProto has bad cron expression= "+ pconfig.change, e);
      } catch (SchedulerException e) {
        logScan.error("cronExecutor failed to schedule RereadProtoJob", e);
        e.printStackTrace();
      }
    }

  }

  public void shutdown() {
    if (scheduler == null) return;
    try {
      scheduler.shutdown( true);
      org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger( "serverStartup" );
      logServerStartup.info( "DataRootHandler / scheduler shutdown" );
    } catch (SchedulerException e) {
      log.error("Scheduler failed to shutdown", e);
      scheduler = null;
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
  }

  public static class ScanFmrcJob implements org.quartz.Job {
    public ScanFmrcJob() {}
    public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
        InvDatasetFeatureCollection fc = (InvDatasetFeatureCollection) context.getJobDetail().getJobDataMap().get(FC_NAME);
        logScan.info("Trigger scan for "+fc.getName());
        fc.triggerRescan();
      } catch (Throwable e) {
        logScan.error("InitFmrcJob failed", e);
      }
    }
  }

  public static class RereadProtoJob implements org.quartz.Job {
    public RereadProtoJob() {}
    public void execute(JobExecutionContext context) throws JobExecutionException {
      try {
        InvDatasetFeatureCollection fc = (InvDatasetFeatureCollection) context.getJobDetail().getJobDataMap().get(FC_NAME);
        logScan.info("Trigger rereadProto for "+fc.getName());
        fc.triggerProto();
      } catch (Throwable e) {
        logScan.error("RereadProtoJob failed", e);
      }
    }
  } */

  //////////////////////////////////////////////

  public void init() {
    // Initialize any given DataRootLocationAliasExpanders that are TdsConfiguredPathAliasReplacement
    String contentReplacementPath = StringUtils.cleanPath(tdsContext.getPublicDocFileSource().getFile("").getPath());
    this.contentPathAliasReplacement = new StartsWithPathAliasReplacement("content", contentReplacementPath);

    String iddDataRootReplacementPath = ThreddsConfig.get("DataRoots.idd", null);
    if (iddDataRootReplacementPath != null) {
      iddDataRootReplacementPath = StringUtils.cleanPath(iddDataRootReplacementPath);
      this.iddDataRootPathAliasReplacement = new StartsWithPathAliasReplacement("${iddDataRoot}", iddDataRootReplacementPath);
    }
    this.updateFullDataRootLocationAliasExpanders(null);

    //this.contentPath = this.tdsContext.
    this.initCatalogs();

    this.makeDebugActions();
    DatasetHandler.makeDebugActions();
  }

  private void updateFullDataRootLocationAliasExpanders(List<PathAliasReplacement> list) {
    this.fullDataRootLocationAliasExpanders = new ArrayList<PathAliasReplacement>();
    this.fullDataRootLocationAliasExpanders.add(this.contentPathAliasReplacement);
    if (iddDataRootPathAliasReplacement != null)
      this.fullDataRootLocationAliasExpanders.add(this.iddDataRootPathAliasReplacement);
    if (list != null && !list.isEmpty())
      this.fullDataRootLocationAliasExpanders.addAll(list);
  }

  private void getExtraCatalogs(List<String> extraList) {
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

      }
      catch (IOException e) {
        logCatalogInit.error("Error on getExtraCatalogs ", e);
      }
    }
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

    logCatalogInit.info( "\n**************************************\n**************************************\nStarting TDS config catalog reinitialization\n[" + DateUtil.getCurrentSystemTimeAsISO8601() + "]" );

    // cleanup 
    thredds.inventory.bdb.MetadataManager.closeAll();

    // Empty all config catalog information.
    pathMatcher = new PathMatcher();
    idHash = new HashSet<String>();

    DatasetHandler.reinit(); // NcML datasets
    initCatalogs();

    isReinit = false;

    logCatalogInit.info( "\n**************************************\n**************************************\nDone with TDS config catalog reinitialization\n[" + DateUtil.getCurrentSystemTimeAsISO8601() + "]" );
  }

  volatile boolean isReinit = false;

  void initCatalogs() {
    ArrayList<String> catList = new ArrayList<String>();
    catList.add("catalog.xml"); // always first
    ThreddsConfig.getCatalogRoots(catList);

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
    startupLog.info("DataRootHandler: staticCache= "+staticCache);

    this.staticCatalogNames = new HashSet<String>();
    this.staticCatalogHash = new HashMap<String, InvCatalogImpl>();

    for (String path : configCatalogRoots) {
      try {
        path = StringUtils.cleanPath(path);
        logCatalogInit.info("\n**************************************\nCatalog init " + path + "\n[" + DateUtil.getCurrentSystemTimeAsISO8601() + "]");
        initCatalog(path, true, true);
      } catch (Throwable e) {
        logCatalogInit.error("initCatalogs(): Error initializing catalog " + path + "; " + e.getMessage(), e);
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
   * @param cache if true, always cache
   * @throws IOException if reading catalog fails
   */
  private void initCatalog(String path, boolean recurse, boolean cache) throws IOException {
    path = StringUtils.cleanPath(path);
    File f = this.tdsContext.getConfigFileSource().getFile(path);

    if (f == null) {
      logCatalogInit.error("initCatalog(): Catalog [" + path + "] does not exist in config directory.");
      return;
    }

    // make sure we dont already have it
    if (staticCatalogNames.contains(path)) {
      logCatalogInit.warn("initCatalog(): Catalog [" + path + "] already seen, possible loop (skip).");
      return;
    }
    staticCatalogNames.add(path);

    // read it
    InvCatalogFactory factory = this.getCatalogFactory(true); // always validate the config catalogs
    InvCatalogImpl cat = readCatalog(factory, path, f.getPath());
    if (cat == null) {
      logCatalogInit.warn("initCatalog(): failed to read catalog <" + f.getPath() + ">.");
      return;
    }

    // Notify listeners of config catalog.
    for (ConfigListener cl : configListeners)
      cl.configCatalog(cat);

    // look for datasetRoots
    for (DataRootConfig p : cat.getDatasetRoots()) {
      addRoot(p, true);
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

  private InvCatalogFactory getCatalogFactory(boolean validate) {
    InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(validate);
    if (!this.fullDataRootLocationAliasExpanders.isEmpty())
      factory.setDataRootLocationAliasExpanders(this.fullDataRootLocationAliasExpanders);
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
      uri = new URI("file:" + StringUtil.escape(catalogFullPath, "/:-_.")); // LOOK needed ?
    }
    catch (URISyntaxException e) {
      logCatalogInit.error("readCatalog(): URISyntaxException=" + e.getMessage());
      return null;
    }

    // read the catalog
    logCatalogInit.info("readCatalog(): full path=" + catalogFullPath + "; path=" + path);
    InvCatalogImpl cat = null;
    FileInputStream ios = null;
    try {
      ios = new FileInputStream(catalogFullPath);
      cat = factory.readXML(ios, uri);

      StringBuilder sbuff = new StringBuilder();
      if (!cat.check(sbuff)) {
        logCatalogInit.error("   invalid catalog -- " + sbuff.toString());
        return null;
      }
      logCatalogInit.info("   valid catalog -- " + sbuff.toString());

    } catch (Throwable t) {
      String msg = (cat == null) ? "null catalog" : cat.getLog();
      logCatalogInit.error("  Exception on catalog=" + catalogFullPath + " " + t.getMessage() + "\n log=" + msg, t);
      return null;

    } finally {
      if (ios != null) {
        try {
          ios.close();
        }
        catch (IOException e) {
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
          logCatalogInit.warn("Duplicate id on  '" + invDataset.getFullName() + "' id= '" + id + "'");
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
          logCatalogInit.error("InvDatasetScan " + ds.getFullName() + " has no default Service - skipping");
          continue;
        }
        if (!addRoot(ds))
          iter.remove();

      } else if (invDataset instanceof InvDatasetFmrc) {
        InvDatasetFmrc fmrc = (InvDatasetFmrc) invDataset;
        addRoot(fmrc);
        needsCache = true;

      } else if (invDataset instanceof InvDatasetFeatureCollection) {
        InvDatasetFeatureCollection fc = (InvDatasetFeatureCollection) invDataset;
        addRoot(fc);
        needsCache = true;

        // not a DatasetScan or InvDatasetFmrc
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

      if ((invDataset instanceof InvCatalogRef) && !(invDataset instanceof InvDatasetScan) && !(invDataset instanceof InvDatasetFmrc)
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
            logCatalogInit.warn("**Warning: Skipping catalogRef <xlink:href=" + href + ">. Reference is relative to the server outside the context path [" + contextPathPlus + "]. " +
                    "Parent catalog info: Name=\"" + catref.getParentCatalog().getName() + "\"; Base URI=\"" + catref.getParentCatalog().getUriString() + "\"; dirPath=\"" + dirPath + "\".");
            continue;
          } else {
            path = dirPath + href;  // reletive starting from current directory
          }

          initCatalog(path, true, false);
        }

      } else if (!(invDataset instanceof InvDatasetScan) && !(invDataset instanceof InvDatasetFmrc) && !(invDataset instanceof InvDatasetFeatureCollection)) {
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
      logCatalogInit.error("**Error: " + dscan.getFullName() + " missing a path attribute.");
      return false;
    }

    DataRoot droot = (DataRoot) pathMatcher.get(path);
    if (droot != null) {
      if (!droot.dirLocation.equals(dscan.getScanLocation())) {
        logCatalogInit.error("**Error: already have dataRoot =<" + path + ">  mapped to directory= <" + droot.dirLocation + ">" +
                " wanted to map to fmrc=<" + dscan.getScanLocation() + "> in catalog " + dscan.getParentCatalog().getUriString());
      }

      return false;
    }

    // Check whether InvDatasetScan is valid before adding.
    if (!dscan.isValid()) {
      logCatalogInit.error(dscan.getInvalidMessage() + "\n... Dropping this datasetScan [" + path + "].");
      return false;
    }

    // add it
    droot = new DataRoot(dscan);
    pathMatcher.put(path, droot);

    logCatalogInit.debug(" added rootPath=<" + path + ">  for directory= <" + dscan.getScanLocation() + ">");
    return true;
  }

  // Only called by synchronized methods
  private boolean addRoot(InvDatasetFmrc fmrc) {
    // check for duplicates
    String path = fmrc.getPath();

    if (path == null) {
      logCatalogInit.error(fmrc.getFullName() + " missing a path attribute.");
      return false;
    }

    DataRoot droot = (DataRoot) pathMatcher.get(path);
    if (droot != null) {
      logCatalogInit.error("**Error: already have dataRoot =<" + path + ">  mapped to directory= <" + droot.dirLocation + ">" +
              " wanted to use by FMRC Dataset =<" + fmrc.getFullName() + ">");
      return false;
    }

    // add it
    droot = new DataRoot(fmrc);

    if (droot.dirLocation != null) {
      File file = new File(droot.dirLocation);
      if (!file.exists()) {
        logCatalogInit.error("**Error: DatasetFmrc =" + droot.path + " directory= <" + droot.dirLocation + "> does not exist");
        return false;
      }
    }

    pathMatcher.put(path, droot);

    logCatalogInit.debug(" added rootPath=<" + path + ">  for fmrc= <" + fmrc.getFullName() + ">");
    return true;
  }

  public List<InvDatasetFeatureCollection> getFeatureCollections() {
    List<InvDatasetFeatureCollection> result = new ArrayList<InvDatasetFeatureCollection>();
    Iterator iter =  pathMatcher.iterator();
    while (iter.hasNext()) {
      DataRoot droot = (DataRoot) iter.next();
      if (droot.featCollection != null)
        result.add(droot.featCollection);
    }
    return result;
  }

  public InvDatasetFeatureCollection getFeatureCollection(String want) {
    Iterator iter =  pathMatcher.iterator();
    while (iter.hasNext()) {
      DataRoot droot = (DataRoot) iter.next();
      if ((droot.featCollection != null) && droot.featCollection.getName().equals(want))
        return droot.featCollection;
    }
    return null;
  }



  // Only called by synchronized methods
  private boolean addRoot(InvDatasetFeatureCollection fc) {
    // check for duplicates
    String path = fc.getPath();

    if (path == null) {
      logCatalogInit.error(fc.getFullName() + " missing a path attribute.");
      return false;
    }

    DataRoot droot = (DataRoot) pathMatcher.get(path);
    if (droot != null) {
      logCatalogInit.error("**Error: already have dataRoot =<" + path + ">  mapped to directory= <" + droot.dirLocation + ">" +
              " wanted to use by FeatureCollection Dataset =<" + fc.getFullName() + ">");
      return false;
    }

    // add it
    droot = new DataRoot(fc);

    if (droot.dirLocation != null) {
      File file = new File(droot.dirLocation);
      if (!file.exists()) {
        logCatalogInit.error("**Error: DatasetFmrc =" + droot.path + " directory= <" + droot.dirLocation + "> does not exist");
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
        logCatalogInit.error("**Error: already have dataRoot =<" + path + ">  mapped to directory= <" + droot.dirLocation + ">" +
                " wanted to map to <" + dirLocation + ">");

      return false;
    }

    File file = new File(dirLocation);
    if (!file.exists()) {
      logCatalogInit.error("**Error: Data Root =" + path + " directory= <" + dirLocation + "> does not exist");
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
        logCatalogInit.error("**Error: already have dataRoot =<" + path + ">  mapped to directory= <" + droot.dirLocation + ">" +
                " wanted to map to <" + location + ">");

      return false;
    }

    File file = new File(location);
    if (!file.exists()) {
      logCatalogInit.error("**Error: Data Root =" + path + " directory= <" + location + "> does not exist");
      return false;
    }

    // add it
    droot = new DataRoot(path, location, config.isCache());
    pathMatcher.put(path, droot);

    logCatalogInit.debug(" added rootPath=<" + path + ">  for directory= <" + location + ">");
    return true;
  }

  ////////////////////////////////////////////////////////////////////////////////////////

  public class DataRootMatch {
    String rootPath;     // this is the matching part of the URL
    String remaining;   // this is the part of the URL that didnt match
    String dirLocation;   // this is the directory that should be substituted for the rootPath
    DataRoot dataRoot;  // this is the directory that should be substituted for the rootPath
  }

  public class DataRoot {
    String path;         // match this path
    String dirLocation;  // to this directory
    InvDatasetScan scan; // the InvDatasetScan that created this (may be null)
    InvDatasetFmrc fmrc; // the InvDatasetFmrc that created this (may be null)
    InvDatasetFeatureCollection featCollection; // the InvDatasetFeatureCollection that created this (may be null)
    boolean cache = true;

    // Use this to access CrawlableDataset in dirLocation.
    // I.e., used by datasets that reference a <datasetRoot>
    InvDatasetScan datasetRootProxy;

    DataRoot(InvDatasetFeatureCollection featCollection) {
      this.path = featCollection.getPath();
      this.featCollection = featCollection;
      this.dirLocation = featCollection.getTopDirectoryLocation();
      logCatalogInit.info(" DataRoot adding featureCollection {}\n", featCollection.getConfig());
    }

    DataRoot(InvDatasetFmrc fmrc) {
      this.path = fmrc.getPath();
      this.fmrc = fmrc;

      InvDatasetFmrc.InventoryParams params = fmrc.getFmrcInventoryParams();
      if (null != params)
        dirLocation = params.location;
    }

    DataRoot(InvDatasetScan scan) {
      this.path = scan.getPath();
      this.scan = scan;
      this.dirLocation = scan.getScanLocation();
      this.datasetRootProxy = null;
    }

    DataRoot(String path, String dirLocation, boolean cache) {
      this.path = path;
      this.dirLocation = dirLocation;
      this.cache = cache;
      this.scan = null;

      makeProxy();
    }

    void makeProxy() {
      /*   public InvDatasetScan( InvDatasetImpl parent, String name, String path, String scanLocation,
                         String configClassName, Object configObj, CrawlableDatasetFilter filter,
                         CrawlableDatasetLabeler identifier, CrawlableDatasetLabeler namer,
                         boolean addDatasetSize,
                         CrawlableDatasetSorter sorter, Map proxyDatasetHandlers,
                         List childEnhancerList, CatalogRefExpander catalogRefExpander ) */
      this.datasetRootProxy = new InvDatasetScan(null, "", this.path, this.dirLocation,
              null, null, null, null, null, false, null, null, null, null);
    }


    // used by PathMatcher
    public String toString() {
      return path;
    }

    // debug
    public String toString2() {
      return path+","+dirLocation;
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
  public DataRootMatch findDataRootMatch(HttpServletRequest req) {
    String spath = req.getPathInfo();

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

    if (reqDataRoot.fmrc != null)
      return null; // if fmrc exists, bail out and deal with it in caller

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
   * (i.e., it is not a CrawlableDatasetFile), or an I/O error occurs whil
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
    if (match.dataRoot.fmrc != null)
      return match.dataRoot.fmrc.getFile(match.remaining);
    if (match.dataRoot.featCollection != null)
      return match.dataRoot.featCollection.getFile(match.remaining);


    CrawlableDataset crDs;
    try {
      crDs = getCrawlableDataset(path);
    }
    catch (IOException e) {
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
    }
    catch (IOException e) {
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
    else if (reqDataRoot.fmrc != null)  // TODO refactor UGLY FMRC HACK
       scan = reqDataRoot.fmrc.getRawFileScan();
    else if (reqDataRoot.featCollection != null)  // TODO refactor UGLY FMRC HACK
       scan = reqDataRoot.featCollection.getRawFileScan();

    return scan;
  }

  public InvCatalog getProxyDatasetResolverCatalog(String path, URI baseURI) {
    if (!isProxyDatasetResolver(path))
      throw new IllegalArgumentException("Not a proxy dataset resolver path <" + path + ">.");

    InvDatasetScan scan = this.getMatchingScan(path);

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
    String path = req.getPathInfo();
    if (!isProxyDatasetResolver(path)) {
      String resMsg = "Request <" + path + "> not for proxy dataset resolver.";
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg.length()));
      log.error("handleRequestForProxyDatasetResolverCatalog(): " + resMsg);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg);
      return;

    }

    String baseUriString = req.getRequestURL().toString();
    URI baseURI;
    try {
      baseURI = new URI(baseUriString);
    }
    catch (URISyntaxException e) {
      String resMsg = "Request URL <" + baseUriString + "> not a valid URI: " + e.getMessage();
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg.length()));
      log.error("handleRequestForProxyDatasetResolverCatalog(): " + resMsg);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg);
      return;
    }

    InvCatalogImpl cat = (InvCatalogImpl) this.getProxyDatasetResolverCatalog(path, baseURI);
    if (cat == null) {
      String resMsg = "Could not generate proxy dataset resolver catalog <" + path + ">.";
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg.length()));
      log.error("handleRequestForProxyDatasetResolverCatalog(): " + resMsg);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg);
      return;
    }

    // Return catalog as XML response.
    InvCatalogFactory catFactory = getCatalogFactory(false);
    String result = catFactory.writeXML(cat);
    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, result.length()));

    res.setContentLength(result.length());
    res.setContentType("text/xml");
    res.getOutputStream().write(result.getBytes());
  }

  /**
   * DO NOT USE, this is Ethan's attempt at designing a generic way to handle data requests.
   * <p/>
   * Only used is in ExampleThreddsServlet.
   *
   * @param path
   * @param dsp
   * @param req
   * @param res
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
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
      return;
    }

    if (dspCanHandle) {
      // Request recognized by DataServiceProvider, handle dataset request.
      dsp.handleRequestForDataset(dsReq, crDs, req, res);
      return;
    } else {
      // Request not recognized by DataServiceProvider.
      if (crDs.isCollection()) {
        // Handle request for a collection dataset.
        dsp.handleUnrecognizedRequestForCollection(crDs, req, res);
        return;
      }

      // Handle request for an atomic dataset.
      dsp.handleUnrecognizedRequest(crDs, req, res);
      return;
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
    String catPath = TdsPathUtils.extractPath(req);
    if (catPath == null)
      return false;

    // Handle case for root catalog.
    // ToDo Is this needed? OPeNDAP no longer allows "/thredds/dodsC/catalog.html"
    if (catPath.equals(""))
      catPath = "catalog.html";

    if (catPath.endsWith("/"))
      catPath += "catalog.html";

    if (!catPath.endsWith(".xml")
            && !catPath.endsWith(".html"))
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
  public InvCatalog getCatalog(String path, URI baseURI) {
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
      if ((expiresDateType != null) && expiresDateType.getDate().getTime() < System.currentTimeMillis())
        reread = true;

    } else if (!staticCache) {
      reread = staticCatalogNames.contains(workPath); // see if we know if its a static catalog
    }

    // its a static catalog that needs to be read
    if (reread) {
      File catFile = this.tdsContext.getConfigFileSource().getFile(workPath);
      if (catFile != null) {
        String catalogFullPath = catFile.getPath();
        logCatalogInit.info( "**********\nReading catalog {} at {}\n",catalogFullPath, DateUtil.getCurrentSystemTimeAsISO8601());

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
        logCatalogInit.error("Static catalog does not exist that we expected = " + workPath);
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

  private InvCatalogImpl makeDynamicCatalog(String path, URI baseURI) {
    String workPath = path;

    // Make sure this is a dynamic catalog request.
    if (!path.endsWith("/catalog.xml"))
      return null;

    // strip off the filename
    int pos = workPath.lastIndexOf("/");
    if (pos >= 0)
      workPath = workPath.substring(0, pos);

    // now look through the InvDatasetScans for a maximal match
    DataRootMatch match = findDataRootMatch(workPath);
    if (match == null)
      return null;

    // look for the fmrc
    if (match.dataRoot.fmrc != null) {
      return match.dataRoot.fmrc.makeCatalog(match.remaining, path, baseURI);
    }

    // look for the feature Collection
    if (match.dataRoot.featCollection != null) {
      return match.dataRoot.featCollection.makeCatalog(match.remaining, path, baseURI);
    }

    // Check that path is allowed, ie not filtered out
    try {
      if (getCrawlableDataset(workPath) == null)
        return null;
    }
    catch (IOException e) {
      log.error("makeDynamicCatalog(): I/O error on request <" + path + ">: " + e.getMessage(), e);
      return null;
    }

    // at this point, its gotta be a DatasetScan, not a DatasetRoot
    if (match.dataRoot.scan == null) {
      log.warn("makeDynamicCatalog(): No InvDatasetScan for =" + workPath + " request path= " + path);
      return null;
    }

    InvDatasetScan dscan = match.dataRoot.scan;
    if (log.isDebugEnabled()) log.debug("makeDynamicCatalog(): Calling makeCatalogForDirectory( " + baseURI + ", " + path + ").");
    InvCatalogImpl cat = dscan.makeCatalogForDirectory(path, baseURI);

    if (null == cat) {
      log.error("makeDynamicCatalog(): makeCatalogForDirectory failed = " + workPath);
    }

    return cat;
  }

  private InvCatalogImpl makeTDRDynamicCatalog(String path, URI baseURI) {
    // Make sure this is a tdr catalog request.
    if (!path.startsWith("tdr"))
      return null;

    File catFile = this.tdsContext.getConfigFileSource().getFile(path);
    if (catFile == null)
      return null;
    String catalogFullPath = catFile.getPath();

    InvCatalogFactory factory = getCatalogFactory(false); // no validation
    InvCatalogImpl cat = readCatalog(factory, path, catalogFullPath);
    if (cat == null) {
      log.warn("makeTDRDynamicCatalog(): failed to read tdr catalog <" + catalogFullPath + ">.");
      return null;
    }

    /* look for datasetRoots
  Iterator roots = cat.getDatasetRoots().iterator();
  while ( roots.hasNext() ) {
    InvProperty p = (InvProperty) roots.next();
    addRoot( p.getName(), p.getValue(), false );
  }  */

    cat.setBaseURI(baseURI);
    return cat;
  }


  /* public void handleRequestForDataset(String path, DataServiceProvider dsp, HttpServletRequest req, HttpServletResponse res)
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
    CrawlableDataset crDs = this.findRequestedDataset(crDsPath);
    if (crDs == null) {
      // Request is not for a known (or allowed) dataset.
      res.sendError(HttpServletResponse.SC_NOT_FOUND); // 404
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
      return;
    }

    if (dspCanHandle) {
      // Request recognized by DataServiceProvider, handle dataset request.
      dsp.handleRequestForDataset(dsReq, crDs, req, res);
      return;
    } else {
      // Request not recognized by DataServiceProvider.
      if (crDs.isCollection()) {
        // Handle request for a collection dataset.
        dsp.handleUnrecognizedRequestForCollection(crDs, req, res);
        return;
      }

      // Handle request for an atomic dataset.
      dsp.handleUnrecognizedRequest(crDs, req, res);
      return;
    }
  }  */

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
  public boolean processReqForLatestDataset(HttpServlet servlet, HttpServletRequest req, HttpServletResponse res)
          throws IOException {
    String orgPath = req.getPathInfo();
    if (orgPath.startsWith("/"))
      orgPath = orgPath.substring(1);

    String path = orgPath;

    // strip off the filename
    int pos = path.lastIndexOf("/");
    if (pos >= 0) {
      path = path.substring(0, pos);
    }

    if (path.equals("/") || path.equals("")) {
      String resMsg = "No data at root level, \"/latest.xml\" request not available.";
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, resMsg.length()));
      if (log.isDebugEnabled()) log.debug("processReqForLatestDataset(): " + resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    // Find InvDatasetScan with a maximal match.
    DataRoot dataRoot = findDataRoot(path);
    if (dataRoot == null) {
      String resMsg = "No scan root matches requested path <" + path + ">.";
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, resMsg.length()));
      log.warn("processReqForLatestDataset(): " + resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    // its gotta be a DatasetScan, not a DatasetRoot
    InvDatasetScan dscan = dataRoot.scan;
    if (dscan == null) {
      String resMsg = "Probable conflict between datasetScan and datasetRoot for path <" + path + ">.";
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, resMsg.length()));
      log.warn("processReqForLatestDataset(): " + resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    // Check that latest is allowed
    if (dscan.getProxyDatasetHandlers() == null) {
      String resMsg = "No \"addProxies\" or \"addLatest\" on matching scan root <" + path + ">.";
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, resMsg.length()));
      log.warn("processReqForLatestDataset(): " + resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    String reqBase = ServletUtil.getRequestBase(req); // this is the base of the request
    URI reqBaseURI;
    try {
      reqBaseURI = new URI(reqBase);
    }
    catch (URISyntaxException e) {
      String resMsg = "Request base URL <" + reqBase + "> not valid URI (???): " + e.getMessage();
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg.length()));
      log.error("processReqForLatestDataset(): " + resMsg);
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, resMsg);
      return false;
    }
    InvCatalog cat = dscan.makeLatestCatalogForDirectory(orgPath, reqBaseURI);

    if (null == cat) {
      String resMsg = "Failed to build response catalog <" + path + ">.";
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, resMsg.length()));
      log.error("processReqForLatestDataset(): " + resMsg);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, resMsg);
      return false;
    }

    // Send latest.xml catalog as response.
    InvCatalogFactory catFactory = getCatalogFactory(false);
    String catAsString = catFactory.writeXML((InvCatalogImpl) cat);
    PrintWriter out = res.getWriter();
    res.setContentType("text/xml");
    res.setStatus(HttpServletResponse.SC_OK);
    out.print(catAsString);
    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, catAsString.length()));
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
  public org.jdom.Element getNcML(String path) {
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

  public void makeDebugActions() {
    DebugHandler debugHandler = DebugHandler.get("catalogs");
    DebugHandler.Action act;
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

    act = new DebugHandler.Action("showStatic", "Show static catalogs") {
      public void doAction(DebugHandler.Event e) {
        ArrayList<String> list;
        StringBuilder sbuff = new StringBuilder();
        synchronized (DataRootHandler.this) {
          list = new ArrayList<String>(staticCatalogHash.keySet());
          Collections.sort(list);
          for (String catPath : list) {
            InvCatalogImpl cat = staticCatalogHash.get(catPath);
            sbuff.append(" catalog= ").append(catPath).append("; ");
            String filename = StringUtil.unescape(cat.getCreateFrom());
            sbuff.append(" from= ").append(filename).append("\n");
          }
        }
        e.pw.println(StringUtil.quoteHtmlContent("\n" + sbuff.toString()));
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("showRoots", "Show data roots") {
      public void doAction(DebugHandler.Event e) {
        synchronized (DataRootHandler.this) {
          Iterator iter = pathMatcher.iterator();
          while (iter.hasNext()) {
            DataRoot ds = (DataRoot) iter.next();
            e.pw.print(" <b>" + ds.path + "</b>");
            String url = DataRootHandler.this.tdsContext.getContextPath() + "/admin/dataDir/" + ds.path + "/";
            if (ds.fmrc == null) {
              String type = (ds.scan == null) ? "root" : "scan";
              e.pw.println(" for " + type + " directory= <a href='" + url + "'>" + ds.dirLocation + "</a> ");
            } else {
              if (ds.dirLocation == null) {
                //url = DataRootHandler.this.tdsContext.getContextPath() + "/" + ds.path;
                e.pw.println("  for fmrc= <a href='" + ds.fmrc.getXlinkHref() + "'>" + ds.fmrc.getXlinkHref() + "</a>");
              } else {
                e.pw.println("  for fmrc= <a href='" + url + "'>" + ds.dirLocation + "</a>");
              }
            }
          }
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("getRoots", "Check data roots") {
      public void doAction(DebugHandler.Event e) {
        synchronized (DataRootHandler.this) {
          e.pw.print("<pre>\n");
          Iterator iter = pathMatcher.iterator();
          boolean ok = true;
          while (iter.hasNext()) {
            DataRoot ds = (DataRoot) iter.next();
            if ((ds.dirLocation == null) && (ds.fmrc != null)) continue;
            
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

    act = new DebugHandler.Action("reinit", "Reinitialize") {
      public void doAction(DebugHandler.Event e) {
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

    act = new DebugHandler.Action("sched", "Show scheduler") {
      public void doAction(DebugHandler.Event e) {
        org.quartz.Scheduler scheduler = CollectionUpdater.INSTANCE.getScheduler();
        if (scheduler == null) return;

        try {
           e.pw.println(scheduler.getMetaData());
          String[] groups = scheduler.getJobGroupNames();
          for (String groupName : groups) {
            e.pw.println("Group "+groupName);
            String[] names = scheduler.getJobNames(groupName);
            for (String name : names) {
              e.pw.println("  Job "+name);
              e.pw.println("    "+ scheduler.getJobDetail(name, groupName));

              Trigger[] triggers =  scheduler.getTriggersOfJob(name, groupName);
              for (Trigger t : triggers)
                e.pw.println("  Trigger "+t);
            }
          }

          String[] triggerGroups = scheduler.getTriggerGroupNames();
          for (String groupName : triggerGroups) {
              e.pw.println("Group: " + groupName + " contains the following triggers");
              String[]  triggersInGroup = scheduler.getTriggerNames(groupName);
             for (String name : triggersInGroup) {
                 e.pw.println("- " + name);
             }
          }

        } catch (Exception e1) {
          e.pw.println("Error on scheduler " + e1.getMessage());
          log.error("Error on scheduler " + e1.getMessage());
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


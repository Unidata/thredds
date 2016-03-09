/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.core;

import com.coverity.security.Escape;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import thredds.client.catalog.Access;
import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.featurecollection.FeatureCollectionCache;
import thredds.server.admin.DebugCommands;
import thredds.server.catalog.*;
import thredds.server.catalog.builder.ConfigCatalogBuilder;
import thredds.server.catalog.tracker.*;
import thredds.server.config.TdsContext;
import thredds.server.config.ThreddsConfig;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Counters;
import ucar.util.prefs.PreferencesExt;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;

/**
 * Reads in the Config catalogs on startup, and if triggered.
 * Spring managed auto-wired.
 *
 * @author caron
 * @since 3/21/2015
 */
@Component
public class ConfigCatalogInitialization {
  static private final org.slf4j.Logger logCatalogInit = org.slf4j.LoggerFactory.getLogger(ConfigCatalogInitialization.class);
  static private final String ERROR = "*** ERROR: ";
  static private final boolean show = true;
  static private final ReadMode defaultReadMode = ReadMode.check;

  @Autowired
  private TdsContext tdsContext;  // used for  getContentDirectory, contextPath

  @Autowired
  private AllowedServices allowedServices;

  @Autowired
  private ConfigCatalogCache ccc;

  @Autowired
  private DataRootManager dataRootManager;

  @Autowired
  private DatasetManager datasetManager;

  @Autowired
  private DebugCommands debugCommands;

  @Autowired
  private FeatureCollectionCache fcCache;

  ///////////////////////////////////////////////////////
  public enum ReadMode {always, check, triggerOnly;
    static public ReadMode get(String name) {
      for (ReadMode mode : values()) {
        if (mode.name().equalsIgnoreCase(name)) return mode;
      }
      return null;
    }
  }

  private PreferencesExt prefs;
  private long readNow;
  private long trackerNumber = 1;
  private long nextCatId = 1;
  private int numberCatalogs = 10;
  private File contentRootPath;    // ${tds.content.root.path}.
  private String contextPath;      // thredds
  private String trackerDir;       // the tracker "databases" are kept in this directory
  private long maxDatasets;        // chronicle limit

   // on reread, construct new objects, so cant be spring beans
  private DataRootPathMatcher dataRootPathMatcher;
  private DataRootTracker dataRootTracker;
  private DatasetTracker datasetTracker;

  // temporary, discard after init
  private CatalogTracker catalogTracker;
  private Set<String> catPathMap;          // Hash of paths, to look for duplicate catalogs
  private Map<String, String> fcNameMap;   // Hash of featureCollection ids, to look for duplicates
  private List<String> rootCatalogKeys;    // needed ??

  // track stats
  private DatasetTracker.Callback callback;

  public ConfigCatalogInitialization() {
  }

  public synchronized void setTrackerDir(String trackerDir) {
    this.trackerDir = trackerDir;
  }

  public void setMaxDatasetToTrack(long maxDatasets) {
    this.maxDatasets = maxDatasets;
  }

  // called from TdsInit on spring-managed auto-wired bean
  public synchronized void init(ReadMode readMode, PreferencesExt prefs) {
    if (readMode == null)
      readMode = defaultReadMode;
    this.prefs = prefs;
    trackerNumber = prefs.getLong("trackerNumber", 1);
    numberCatalogs = prefs.getInt("numberCatalogs", 10);
    nextCatId = prefs.getLong("nextCatId", 1);

    makeDebugActions();
    this.contentRootPath = this.tdsContext.getThreddsDirectory();
    this.contextPath = tdsContext.getContextPath();

    reread(readMode, true);
  }

  // called from init() and from trigger controller
  public synchronized boolean reread(ReadMode readMode, boolean isStartup) {
    readNow = System.currentTimeMillis();
    logCatalogInit.info("=========================================================================================\n"+
                    "ConfigCatalogInitialization readMode={} isStartup={}", readMode, isStartup);
    catPathMap = new HashSet<>();
    fcNameMap = new HashMap<>();
    if (ccc != null) ccc.invalidateAll(); // remove anything in cache
    if (fcCache != null) fcCache.invalidateAll(); // remove anything in cache

    if (!isStartup && readMode == ReadMode.always) trackerNumber++;  // must write a new database if TDS is already running and rereading all
    if (!isDebugMode || this.datasetTracker == null)
      this.datasetTracker = new DatasetTrackerChronicle(trackerDir, maxDatasets, trackerNumber);

    boolean databaseAlreadyExists = datasetTracker.exists(); // detect if tracker database exists
    if (!databaseAlreadyExists) {
      readMode = ReadMode.always;
      logCatalogInit.info("ConfigCatalogInitializion datasetTracker database does not exist, set readMode to=" + readMode);
    }

    if (this.callback == null)
      this.callback = new StatCallback(readMode);

    // going to reread global services
    allowedServices.clearGlobalServices();

    switch (readMode) {
      case always:
        if (databaseAlreadyExists) this.datasetTracker.reinit();
        this.catalogTracker = new CatalogTracker(trackerDir, true, numberCatalogs, nextCatId);
        this.dataRootTracker = new DataRootTracker(trackerDir, true, callback);
        this.dataRootPathMatcher = new DataRootPathMatcher(ccc, dataRootTracker);  // starting over
        readRootCatalogs(readMode);
        break;

      case check:
        this.catalogTracker = new CatalogTracker(trackerDir, false, numberCatalogs, nextCatId);        // use existing catalog list
        this.dataRootTracker = new DataRootTracker(trackerDir, false, callback);      // use existing data roots
        this.dataRootPathMatcher = new DataRootPathMatcher(ccc, dataRootTracker);
        readRootCatalogs(readMode);           // read just roots to get global services
        checkExistingCatalogs(readMode);
        break;

      case triggerOnly:
        this.catalogTracker = new CatalogTracker(trackerDir, false, numberCatalogs, nextCatId);               // use existing catalog list
        this.dataRootTracker = new DataRootTracker(trackerDir, false, callback);             // use existing data roots
        this.dataRootPathMatcher = new DataRootPathMatcher(ccc, dataRootTracker);
        readRootCatalogs(readMode);           // read just roots to get global services
        break;
    }

    numberCatalogs = catalogTracker.size();
    nextCatId = catalogTracker.getNextCatId();
    if (prefs != null) {
      prefs.putLong("trackerNumber", trackerNumber);
      prefs.putLong("nextCatId", nextCatId);
      prefs.putInt("numberCatalogs", numberCatalogs);
    }
    callback.finish();
    logCatalogInit.info("\nConfigCatalogInitializion stats\n" + callback);

    try {
      datasetTracker.save();
      catalogTracker.save();
      dataRootTracker.save();
    } catch (IOException e) {
      // e.printStackTrace();
      logCatalogInit.error("datasetTracker.save() failed", e);
    }

    // heres where we may be doing a switcheroo in a running TDS
    if (dataRootManager != null)
      dataRootManager.setDataRootPathMatcher(dataRootPathMatcher);
    if (datasetManager != null)
      datasetManager.setDatasetTracker(datasetTracker);

    // cleanup old version of the database
    if (!isStartup && readMode == ReadMode.always) {
      DatasetTrackerChronicle.cleanupBefore(trackerDir, trackerNumber);
    }

    long took = System.currentTimeMillis() - readNow;
    logCatalogInit.info("ConfigCatalogInitializion finished took={} msecs", took);

    // cleanup
    catPathMap = null;
    fcNameMap = null;
    catalogTracker = null;

    return true; // ok
  }

  private void readRootCatalogs(ReadMode readMode) {
    rootCatalogKeys = new ArrayList<>();
    rootCatalogKeys.add("catalog.xml"); // always first
    // add any others listed in ThreddsConfig
    for (String location : ThreddsConfig.getRootList("catalogRoot"))
      rootCatalogKeys.add( location );
    logCatalogInit.info("ConfigCatalogInit: initializing " + rootCatalogKeys.size() + " root catalogs.");

    // all root catalogs are checked
    for (String pathname : rootCatalogKeys) {
      try {
        pathname = StringUtils.cleanPath(pathname);
        logCatalogInit.info( "Checking catalogRoot = " + pathname);
        checkCatalogToRead(readMode, pathname, true, 0);
      } catch (Throwable e) {
        logCatalogInit.error(ERROR + "initializing catalog " + pathname + "; " + e.getMessage(), e);
      }
    }
  }

  private void checkExistingCatalogs(ReadMode readMode) {
    for (CatalogExt catalogExt : catalogTracker.getCatalogs()) {
      if (catalogExt.isRoot()) continue; // already read in

      String pathname = catalogExt.getCatRelLocation();
      try {
        logCatalogInit.info("\n**************************************\nCatalog init " + pathname + "[" + CalendarDate.present() + "]");
        pathname = StringUtils.cleanPath(pathname);
        checkCatalogToRead(readMode, pathname, catalogExt.isRoot(), catalogExt.getLastRead());
      } catch (Throwable e) {
        logCatalogInit.error(ERROR + "initializing catalog " + pathname + "; " + e.getMessage(), e);
      }
    }
  }

  // decide if we need to read this catalog or not. if yes, follow any catrefs
  // catalogRelpath must be relative to rootDir
  private void checkCatalogToRead(ReadMode readMode, String catalogRelPath, boolean isRoot, long lastRead) throws IOException {
    if (exceedLimit) return;

    catalogRelPath = StringUtils.cleanPath(catalogRelPath);
    File catalogFile = new File(this.contentRootPath, catalogRelPath);
    if (!catalogFile.exists()) {
      catalogTracker.removeCatalog(catalogRelPath);
      logCatalogInit.error(ERROR + "initCatalog(): Catalog [" + catalogRelPath + "] does not exist.");
      return;
    }
    long lastModified = catalogFile.lastModified();
    if (!isRoot && readMode != ReadMode.always && lastModified < lastRead) return; // skip catalogs that havent changed
    if (!isRoot && readMode == ReadMode.triggerOnly) return;                    // skip non-root catalogs for trigger only
    if (show) System.out.printf("initCatalog %s%n", catalogRelPath);

    // make sure we havent already read it
    if (catPathMap.contains(catalogRelPath)) {
      logCatalogInit.error(ERROR + "initCatalog(): Catalog [" + catalogRelPath + "] already seen, possible loop (skip).");
      return;
    }
    catPathMap.add(catalogRelPath);
    Set<String> idSet = new HashSet<>();  // look for unique ids

    // if (logCatalogInit.isDebugEnabled()) logCatalogInit.debug("initCatalog {} -> {}", path, f.getAbsolutePath());

    // read it
    ConfigCatalog cat = readCatalog(catalogRelPath, catalogFile.getPath());
    if (cat == null) {
      logCatalogInit.error(ERROR + "initCatalog(): failed to read catalog <" + catalogFile.getPath() + ">.");
      return;
    }
    long catId = catalogTracker.put(new CatalogExt(0, catalogRelPath, isRoot, readNow));

    if (isRoot) {
      if (ccc != null) ccc.put(catalogRelPath, cat);
      allowedServices.addGlobalServices(cat.getServices());
      if (readMode == ReadMode.triggerOnly) return;                    // thats all we need
    }

    if (callback != null) callback.hasCatalogRef(cat);

    // look for datasetRoots
    for (DatasetRootConfig p : cat.getDatasetRoots())
      dataRootPathMatcher.addRoot(p, catalogRelPath, readMode == ReadMode.always); // check for duplicates on complete reread

    if (callback == null) {   // LOOK WTF?
      List<String> disallowedServices = allowedServices.getDisallowedServices(cat.getServices());
      if (!disallowedServices.isEmpty()) {
        allowedServices.getDisallowedServices(cat.getServices());
        logCatalogInit.error(ERROR + "initCatalog(): declared services: " + Arrays.toString(disallowedServices.toArray()) + " in catalog: " + catalogFile.getPath() + " are disallowed in threddsConfig file");
      }
    }

    // look for dataRoots in datasetScans and featureCollections
    dataRootPathMatcher.extractDataRoots(catalogRelPath, cat.getDatasetsLocal(), readMode == ReadMode.always, fcNameMap);

    // get the directory path, reletive to the rootDir
    int pos = catalogRelPath.lastIndexOf("/");
    String dirPath = (pos > 0) ? catalogRelPath.substring(0, pos + 1) : "";
    processDatasets(catId, readMode, dirPath, cat.getDatasetsLocal(), idSet);     // recurse

    // look for catalogScans
    for (CatalogScan catScan : cat.getCatalogScans()) {
      if (exceedLimit) return;
      Path relLocation = Paths.get(dirPath, catScan.getLocation());
      Path absLocation = Paths.get(catalogFile.getParent(), catScan.getLocation());
      // if (catalogWatcher != null) catalogWatcher.registerAll(absLocation);
      readCatsInDirectory(readMode, relLocation.toString(), absLocation);
    }
  }

  /**
   * Does the actual work of reading a catalog.
   *
   * @param catalogRelPath            reletive path starting from content root
   * @param catalogFullPath           absolute location on disk
   * @return the Catalog, or null if failure
   */
  private ConfigCatalog readCatalog(String catalogRelPath, String catalogFullPath)  {
    URI uri;
    try {
      // uri = new URI("file:" + StringUtil2.escape(catalogFullPath, "/:-_.")); // needed ?
      uri = new URI(this.contextPath + "/catalog/" + catalogRelPath);
    } catch (URISyntaxException e) {
      logCatalogInit.error(ERROR + "readCatalog(): URISyntaxException=" + e.getMessage());
      return null;
    }

    ConfigCatalogBuilder builder = new ConfigCatalogBuilder();
    try {
      // read the catalog
      logCatalogInit.info("-------readCatalog(): path=" + catalogRelPath);
      ConfigCatalog cat = (ConfigCatalog) builder.buildFromLocation(catalogFullPath, uri);
      if (builder.hasFatalError()) {
        logCatalogInit.error(ERROR + "   invalid catalog -- " + builder.getErrorMessage());
        return null;
      }

      if (builder.getErrorMessage().length() > 0)
        logCatalogInit.debug(builder.getErrorMessage());

      return cat;

    } catch (Throwable t) {
      logCatalogInit.error(ERROR + "  Exception on catalog=" + catalogFullPath + " " + t.getMessage() + "\n log=" + builder.getErrorMessage(), t);
      return null;
    }
  }

  // dirPath = the directory path, reletive to the rootDir
  private void processDatasets(long catId, ReadMode readMode, String dirPath, List<Dataset> datasets, Set<String> idMap) throws IOException {
    if (exceedLimit) return;

    for (Dataset ds : datasets) {
      if (datasetTracker.trackDataset(catId, ds, callback)) countDatasets++;
      if (maxDatasetsProcess > 0 && countDatasets > maxDatasetsProcess) exceedLimit = true;

      // look for duplicate ids
      String id = ds.getID();
      if (id != null) {
        if (idMap.contains(id)) {
          logCatalogInit.error(ERROR + "Duplicate id on  '" + ds.getName() + "' id= '" + id + "'");
        } else {
          idMap.add(id);
        }
      }

      if ((ds instanceof DatasetScan) || (ds instanceof FeatureCollectionRef)) continue;
      if (ds instanceof CatalogScan) continue;

      if (ds instanceof CatalogRef) { // follow catalog refs
        CatalogRef catref = (CatalogRef) ds;
        String href = catref.getXlinkHref();
        // if (logCatalogInit.isDebugEnabled()) logCatalogInit.debug("  catref.getXlinkHref=" + href);

        // Check that catRef is relative
        if (!href.startsWith("http:")) {
          // Clean up relative URLs that start with "./"
          if (href.startsWith("./")) {
            href = href.substring(2);
          }

          String path;
          String contextPathPlus = this.contextPath + "/";
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

          CatalogExt ext = catalogTracker.get(path);
          long lastRead = (ext == null) ? 0 : ext.getLastRead();
          checkCatalogToRead(readMode, path, false, lastRead);
        }

      } else {
        // recurse through nested datasets
        processDatasets(catId, readMode, dirPath, ds.getDatasetsLocal(), idMap);
      }
    }
  }

  // dirPath is the directory relative to rootDir, directory is absolute
  private void readCatsInDirectory(ReadMode readMode, String dirPath, Path directory) throws IOException {
    if (exceedLimit) return;

     // do any catalogs first
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(directory, "*.xml")) {
      for (Path p : ds) {
        if (!Files.isDirectory(p)) {
          // path must be relative to rootDir
          String filename = p.getFileName().toString();
          String path = dirPath.length() == 0 ? filename :  dirPath + "/" + filename;  // reletive starting from current directory

          CatalogExt ext = catalogTracker.get(path);
          long lastRead = (ext == null) ? 0 : ext.getLastRead();
          checkCatalogToRead(readMode, path, false, lastRead);
        }
      }
    }

    // now recurse into the directory
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(directory)) {
       for (Path dir : ds) {
         if (Files.isDirectory(dir)) {
           String dirPathChild = dirPath + "/" + dir.getFileName().toString();  // reletive starting from current directory
           readCatsInDirectory(readMode, dirPathChild, dir);
         }
       }
     }
   }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public void makeDebugActions() {
    DebugCommands.Category debugHandler = debugCommands.findCategory("Catalogs");
    DebugCommands.Action act;

     act = new DebugCommands.Action("showCatalogExt", "Show known catalogs") {
       public void doAction(DebugCommands.Event e) {
         e.pw.printf("numberCatalogs=%d nextCatId=%d%n", numberCatalogs, nextCatId);
         e.pw.printf("%nid  root  lastRead     path%n");

         CatalogTracker catalogTracker = new CatalogTracker(trackerDir, false, numberCatalogs, 0);
         for (CatalogExt cat : catalogTracker.getCatalogs()) {
           e.pw.printf("%3d: %5s %s %s%n", cat.getCatId(), cat.isRoot(), CalendarDate.of(cat.getLastRead()), cat.getCatRelLocation());
         }
       }
     };
     debugHandler.addAction(act);

    act = new DebugCommands.Action("showRoots", "Show root catalogs") {
      public void doAction(DebugCommands.Event e) {
        StringBuilder sbuff = new StringBuilder();
        synchronized (ConfigCatalogInitialization.this) {
          for (String catPath : rootCatalogKeys) {
            sbuff.append(" catalog= ").append(catPath).append("\n");
            //String filename = StringUtil2.unescape(cat.getUriString());
            //sbuff.append(" from= ").append(filename).append("\n");
          }
        }
        e.pw.println();
        e.pw.println(Escape.html(sbuff.toString()));
      }
    };
    debugHandler.addAction(act);

    act = new DebugCommands.Action("showStats", "Show catalog initialization stats") {
      public void doAction(DebugCommands.Event e) {
        if (callback != null)
          e.pw.printf("%n%s%n", Escape.html(callback.toString()));
        else
          e.pw.printf("N/A%n");
      }
    };
    debugHandler.addAction(act);
  }

  /////////////////////////////////////////////////////////
  static class Stats {
    int catrefs;
    int datasets, trackedDatasets;
    int dataRoot, dataRootFc, datasetScan, datasetRoot, catalogScan;
    int ncml, ncmlOne;
    int restrict;
    Counters counters = new Counters();

    public Stats() {
      counters.add("restrict");
      counters.add("nAccess");
      counters.add("serviceType");
      counters.add("ncmlAggSize");
    }

    String show(Formatter f) {
      f.format("         catalogs=%d%n", catrefs);
      f.format("         datasets=%d%n", datasets);
      f.format("  trackedDatasets=%d%n", trackedDatasets);
      f.format("           restrict=%d%n", restrict);
      f.format("            hasNcml=%d%n%n", ncml);

      f.format("      dataRoot=%d%n", dataRoot);
      f.format("    featCollect=%d%n", dataRootFc);
      f.format("    datasetScan=%d%n", datasetScan);
      f.format("    catalogScan=%d%n", catalogScan);
      f.format("    datasetRoot=%d%n%n", datasetRoot);

      f.format("DatasetExt.total_count %d%n", DatasetExt.total_count);
      f.format("DatasetExt.total_nbytes %d%n", DatasetExt.total_nbytes);
      float avg = DatasetExt.total_count == 0 ? 0 : ((float) DatasetExt.total_nbytes) / DatasetExt.total_count;
      f.format("DatasetExt.avg_nbytes %5.0f%n", avg);

      counters.show(f);
      return f.toString();
    }
  }

  static public class StatCallback implements DatasetTracker.Callback {
    ReadMode readMode;
    Stats stat2;
    long start = System.currentTimeMillis();
    double took;

    public StatCallback(ReadMode readMode) {
      this.readMode = readMode;
      stat2 = new Stats();
    }

    @Override
    public void finish() {
      took = (System.currentTimeMillis() - start) / 1000.0;
    }

    @Override
    public void hasDataRoot(DataRootExt dataRoot) {
      stat2.dataRoot++;
      switch (dataRoot.getType()) {
        case featureCollection:
          stat2.dataRootFc++;
          break;
        case catalogScan:
          stat2.catalogScan++;
          break;
        case datasetScan:
          stat2.datasetScan++;
          break;
        case datasetRoot:
          stat2.datasetRoot++;
          break;
      }
    }

    @Override
    public void hasDataset(Dataset ds) {
      stat2.datasets++;
      List<Access> access = ds.getAccess();
      stat2.counters.count("nAccess", access.size());
      for (Access acc : access)
        if (acc.getService() != null) stat2.counters.count("serviceType", acc.getService().toString());
    }

    @Override
    public void hasTrackedDataset(Dataset ds) {
      stat2.trackedDatasets++;
    }

    @Override
    public void hasNcml(Dataset ds) {
      stat2.ncml++;
      org.jdom2.Element netcdfElem = ds.getNcmlElement();
      org.jdom2.Element agg =  netcdfElem.getChild("aggregation", thredds.client.catalog.Catalog.ncmlNS);
      if (agg == null) return;
      List<org.jdom2.Element> nested =  agg.getChildren("netcdf", thredds.client.catalog.Catalog.ncmlNS);
      if (nested == null) return;
      if (nested.size() == 1)
        stat2.ncmlOne++;
      // look for nested ncml - count how many
      stat2.counters.count("ncmlAggSize", nested.size());
    }

    @Override
    public void hasRestriction(Dataset ds) {
      stat2.restrict++;
      String restrict = ds.getRestrictAccess();
      if (restrict != null) stat2.counters.count("restrict", restrict);
    }

    @Override
    public void hasCatalogRef(ConfigCatalog dd) {
      stat2.catrefs++;
    }

    @Override
    public String toString() {
      Formatter f = new Formatter();
      f.format("ConfigCatalogInitialization started %s took %f secs using readMode=%s%n", CalendarDate.of(start), took, readMode);
      return stat2.show(f);
    }
  }

  /////////////////////////////////////////////////////////////////////////////////
  // debugging mode, to test outside of tomcat/spring

  private boolean isDebugMode;
  private long countDatasets = 0;
  private long maxDatasetsProcess;
  private boolean exceedLimit;

  // used from outside of tomcat/spring for testing
  public ConfigCatalogInitialization(ReadMode readMode, File contentRootPath, String trackerDir, DatasetTracker datasetTracker,
                                     AllowedServices allowedServices, DatasetTracker.Callback callback, long maxDatasetsProcess) throws IOException {
    this.contentRootPath = contentRootPath;
    this.contextPath = "/thredds";
    this.trackerDir = trackerDir != null ? trackerDir : new File(contentRootPath, "cache/catalog").getPath();
    this.datasetTracker = datasetTracker;
    this.allowedServices = allowedServices;
    this.callback = callback;
    this.maxDatasetsProcess = maxDatasetsProcess;
    this.isDebugMode = true;

    File trackerFile = new File(this.trackerDir);
    if (!trackerFile.exists()) {
      boolean ok = trackerFile.mkdirs();
      System.out.printf("ConfigCatalogInitialization make tracker directory '%s' make ok = %s%n", this.trackerDir, ok);
    }

    reread(readMode, true);
  }
}

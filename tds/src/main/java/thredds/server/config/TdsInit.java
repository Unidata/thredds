/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.config;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import thredds.client.catalog.tools.CatalogXmlWriter;
import thredds.client.catalog.tools.DataFactory;
import thredds.core.AllowedServices;
import thredds.core.ConfigCatalogInitialization;
import thredds.core.DatasetManager;
import thredds.core.StandardService;
import thredds.featurecollection.CollectionUpdater;
import thredds.featurecollection.InvDatasetFeatureCollection;
import thredds.server.catalog.ConfigCatalogCache;
import thredds.server.catalog.DatasetScan;
import thredds.server.ncss.controller.NcssDiskCache;
import thredds.server.ncss.format.FormatsAvailabilityService;
import thredds.server.ncss.format.SupportedFormat;
import thredds.server.notebook.JupyterNotebookServiceCache;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.ncml.Aggregation;
import ucar.nc2.stream.CdmRemote;
import ucar.nc2.util.DebugFlags;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.nc2.util.DiskCache;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.cache.FileCache;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

/**
 * A Singleton class to initialize and shutdown the CDM/TDS
 * Formerly CdmInit
 *
 * @author caron
 * @since Feb 20, 2009
 */

@Component
public class TdsInit implements ApplicationListener<ContextRefreshedEvent>, DisposableBean {
  static private final Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");
  static private final Logger logCatalogInit = org.slf4j.LoggerFactory.getLogger("catalogInit");

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  TdsConfigMapper tdsConfigMapper;

  @Autowired
  private TdsUpdateConfigBean tdsUpdateConfig;

  @Autowired
  private DatasetManager datasetManager;

  @Autowired
  private ConfigCatalogCache ccc;

  @Autowired
  private ConfigCatalogInitialization configCatalogInitializer;

  @Autowired
  CollectionUpdater collectionUpdater;

  @Autowired
  @Qualifier("fcTriggerExecutor")
  private ExecutorService executor;  // need this so we can shut it down

  @Autowired
  private AllowedServices allowedServices;

  @Autowired
  private JupyterNotebookServiceCache jupyterNotebooks;

  @Autowired
  private NcssDiskCache ncssDiskCache;

  private Timer cdmDiskCacheTimer;
  private boolean wasInitialized;

  private XMLStore store;
  private PreferencesExt mainPrefs;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (event != null) {  // startup
      synchronized (this) {
        if (!wasInitialized) {
          wasInitialized = true;
          startupLog.info("TdsInit: {}", event);
          startupLog.info("TdsInit: getContentRootPathAbsolute= " + tdsContext.getContentRootPathProperty());

          // set debug flags passed into the JVM, as long as flags are actually passed in
          String tdsDebugFlags = tdsContext.getTdsDebugFlags();
          if (!tdsDebugFlags.isEmpty()) {
            startupLog.info(String.format("Setting the following debug flags: %s", tdsDebugFlags));
            setDebugFlags(new DebugFlagsImpl(tdsDebugFlags));
          }

          readState();
          initThreddsConfig();
          readThreddsConfig();
          logVersionMessage();
          // add warning about web start deprecation
          startupLog.warn("Web Start has been deprecated for >Java 9 and is slated for removal - see http://www.oracle.com/technetwork/java/eol-135779.html)");

          // read catalogs
          String readModeS = ThreddsConfig.get("ConfigCatalog.reread", "always");
          ConfigCatalogInitialization.ReadMode readMode =  ConfigCatalogInitialization.ReadMode.get(readModeS);
          if (readMode == null) readMode = ConfigCatalogInitialization.ReadMode.always;
          configCatalogInitializer.init(readMode, (PreferencesExt) mainPrefs.node("configCatalog"));
          startupLog.info("TdsInit complete");
        }
      }
    }
  }

  private void setDebugFlags(DebugFlags debugFlags) {
    NetcdfFile.setDebugFlags(debugFlags);
    ucar.nc2.iosp.hdf5.H5iosp.setDebugFlags(debugFlags);
    ucar.nc2.ncml.NcMLReader.setDebugFlags(debugFlags);
    ucar.nc2.dods.DODSNetcdfFile.setDebugFlags(debugFlags);
    CdmRemote.setDebugFlags(debugFlags);
    Nc4Iosp.setDebugFlags(debugFlags);
    DataFactory.setDebugFlags(debugFlags);

    ucar.nc2.FileWriter2.setDebugFlags(debugFlags);
    ucar.nc2.ft.point.standard.PointDatasetStandardFactory.setDebugFlags(debugFlags);
    ucar.nc2.grib.collection.Grib.setDebugFlags(debugFlags);
  }

  private void readState() {
    File prefsDir = new File(tdsContext.getThreddsDirectory(), "/state/");
    if (!prefsDir.exists()) {
      boolean ok = prefsDir.mkdirs();
      startupLog.info("TdsInit: makeDir= " + prefsDir.getAbsolutePath() + " ok=" + ok);
    }

    File prefsFile = new File(prefsDir, "prefs.xml");
    try {
      store = XMLStore.createFromFile(prefsFile.getAbsolutePath(), null);
      mainPrefs = store.getPreferences();
    } catch (IOException e) {
      startupLog.error("TdsInit: failed to get prefs file= " + prefsDir.getAbsolutePath(), e);
    }
  }

  private void initThreddsConfig() {
    // read in persistent user-defined params from threddsConfig.xml
    File tdsConfigFile = new File(tdsContext.getThreddsDirectory(), tdsContext.getConfigFileProperty());
    if (!tdsConfigFile.exists()) {
      startupLog.warn("TdsInit: TDS configuration file '{}' doesn't exist, using all defaults ", tdsConfigFile
              .getAbsolutePath());
      return;
    }
    ThreddsConfig.init(tdsConfigFile.getPath());
  }

  private void logVersionMessage() {

    // log current server version in catalogInit, where it is most likely to be seen by the user
    String version = tdsContext.getVersionInfo();
    String message = "You are currently running TDS version " + version;
    logCatalogInit.info(message);

    // check and log the latest stable and development version information
    //  only if it is OK according to the threddsConfig file.
    if (tdsUpdateConfig.isLogVersionInfo()) {
      Map<String, String> latestVersionInfo = tdsUpdateConfig.getLatestVersionInfo(version);
      if (!latestVersionInfo.isEmpty()) {
        logCatalogInit.info("Latest Available TDS Version Info:");
        for (Map.Entry entry : latestVersionInfo.entrySet()) {
          message = "latest " + entry.getKey() + " version = " + entry.getValue();
          startupLog.info("TdsInit: " + message);
          logCatalogInit.info("    " + message);
        }
        logCatalogInit.info("");
      }
    }
  }

  private void readThreddsConfig() {
    // initialize the tds configuration beans
    tdsConfigMapper.init(tdsContext);

    // prefer cdmRemote when available
    DataFactory.setPreferCdm(true);
    // netcdf-3 files can only grow, not have metadata changes
    ucar.nc2.NetcdfFile.setProperty("syncExtendOnly", "true");

    // Global config
    boolean useBytesForDataSize = ThreddsConfig.getBoolean("catalogWriting.useBytesForDataSize", false);
    CatalogXmlWriter.useBytesForDataSize(useBytesForDataSize);
    startupLog.info("TdsInit: catalogWriting.useBytesForDataSize= " + useBytesForDataSize);

    // datasetSource plug-in
    ThreddsConfig.getRootList("datasetSource").forEach(datasetManager::registerDatasetSource);

    //////////////////////////////////////////////////////////
    // Controlling Data Services
    // allows users to control services in ThreddsConfig, but not override the default if they havent set
    // LOOK only exposing the ones alreaady in use. maybe let user override tdsGlobalConfig.xml (phase out ThreddsConfig)

    allowedServices.setAllowService(StandardService.catalogRemote, ThreddsConfig.getBoolean("CatalogServices.allowRemote"));
    allowedServices.setAllowService(StandardService.wcs, ThreddsConfig.getBoolean("WCS.allow"));
    allowedServices.setAllowService(StandardService.wms, ThreddsConfig.getBoolean("WMS.allow"));
    allowedServices.setAllowService(StandardService.netcdfSubsetGrid, ThreddsConfig.getBoolean("NetcdfSubsetService.allow"));
    allowedServices.setAllowService(StandardService.netcdfSubsetPoint, ThreddsConfig.getBoolean("NetcdfSubsetService.allow"));
    allowedServices.setAllowService(StandardService.iso_ncml, ThreddsConfig.getBoolean("NCISO.ncmlAllow"));
    allowedServices.setAllowService(StandardService.uddc, ThreddsConfig.getBoolean("NCISO.uddcAllow"));
    allowedServices.setAllowService(StandardService.iso, ThreddsConfig.getBoolean("NCISO.isoAllow"));
    allowedServices.setAllowService(StandardService.jupyterNotebook, ThreddsConfig.getBoolean("JupyterNotebookService.allow"));


    // CDM configuration

    allowedServices.finish(); // finish when we know everything is wired
    InvDatasetFeatureCollection.setAllowedServices(allowedServices);
    DatasetScan.setSpecialServices(allowedServices.getStandardService(StandardService.resolver),
              allowedServices.getStandardService(StandardService.httpServer));
    DatasetScan.setAllowedServices(allowedServices);
    allowedServices.makeDebugActions();

    /*
      <Netcdf4Clibrary>
        <libraryPath>/usr/local/lib</libraryPath>
        <libraryName>netcdf</libraryName>
        <useForReading>false</useForReading>
      </Netcdf4Clibrary>
    */
    String libraryPath = ThreddsConfig.get("Netcdf4Clibrary.libraryPath", null);
    String libraryName = ThreddsConfig.get("Netcdf4Clibrary.libraryName", null);
    if (libraryPath != null || libraryName != null) {
      Nc4Iosp.setLibraryAndPath(libraryPath, libraryName);
    }

    Boolean useForReading = ThreddsConfig.getBoolean("Netcdf4Clibrary.useForReading", false);
    if (useForReading) {
      if (Nc4Iosp.isClibraryPresent()) {
        try {
          // Registers Nc4Iosp in front of all the other IOSPs already registered in NetcdfFile.<clinit>().
          // Crucially, this means that we'll try to open a file with Nc4Iosp before we try it with H5iosp.
          NetcdfFile.registerIOProvider(Nc4Iosp.class);
        } catch (IllegalAccessException | InstantiationException e) {
          startupLog.error("TdsInit: Unable to register IOSP: " + Nc4Iosp.class.getCanonicalName(), e);
        }
      } else {
        startupLog.warn("TdsInit: In threddsConfig.xml, 'Netcdf4Clibrary.useForReading' is 'true' but the native C " +
                "library couldn't be found on the system. Falling back to the pure-Java reader.");
      }
    }

    if (Nc4Iosp.isClibraryPresent()) {  // NetCDF-4 lib could be set as an environment variable or as a JVM parameter.
      FormatsAvailabilityService.setFormatAvailability(SupportedFormat.NETCDF4, true);
      // FormatsAvailabilityService.setFormatAvailability(SupportedFormat.NETCDF4EXT, true);
    }

    // how to choose the typical dataset ?
    String typicalDataset = ThreddsConfig.get("Aggregation.typicalDataset", "penultimate");
    Aggregation.setTypicalDatasetMode(typicalDataset);
    startupLog.info("TdsInit: Aggregation.setTypicalDatasetMode= " + typicalDataset);

    ////////////////////////////////////////////////////////////////
    // Disk Caching
    String dir;
    int scourSecs, maxAgeSecs;

    // Nj22 disk cache
    dir = ThreddsConfig.get("DiskCache.dir", new File(tdsContext.getThreddsDirectory(), "/cache/cdm/").getPath());
    boolean alwaysUse = ThreddsConfig.getBoolean("DiskCache.alwaysUse", false);
    scourSecs = ThreddsConfig.getSeconds("DiskCache.scour", 60 * 60); // default once an hour
    long maxSize = ThreddsConfig.getBytes("DiskCache.maxSize", (long) 1000 * 1000 * 1000);  // default 1 Gbyte
    DiskCache.setRootDirectory(dir);
    DiskCache.setCachePolicy(alwaysUse);
    startupLog.info("TdsInit: CdmCache= " + dir + " scour = " + scourSecs + " maxSize = " + maxSize);
    if (scourSecs > 0) {
      Calendar c = Calendar.getInstance(); // contains current startup time
      c.add(Calendar.SECOND, scourSecs / 2); // starting in half the scour time
      cdmDiskCacheTimer = new Timer("CdmDiskCache");
      cdmDiskCacheTimer.scheduleAtFixedRate(new CacheScourTask(maxSize), c.getTime(), (long) 1000 * scourSecs);
    }

    // persist joinExisting aggregations. default every 24 hours, delete stuff older than 90 days
    dir = ThreddsConfig.get("AggregationCache.dir", new File(tdsContext.getThreddsDirectory().getPath(), "/cache/agg/").getPath());
    scourSecs = ThreddsConfig.getSeconds("AggregationCache.scour", 24 * 60 * 60);
    maxAgeSecs = ThreddsConfig.getSeconds("AggregationCache.maxAge", 90 * 24 * 60 * 60);
    DiskCache2 aggCache = new DiskCache2(dir, false, maxAgeSecs / 60, scourSecs / 60);
    String cachePathPolicy = ThreddsConfig.get("AggregationCache.cachePathPolicy", null);
    aggCache.setPolicy(cachePathPolicy);
    Aggregation.setPersistenceCache(aggCache);
    startupLog.info("TdsInit: AggregationCache= " + dir + " scour = " + scourSecs + " maxAgeSecs = " + maxAgeSecs);

    /* 4.3.15: grib index file placement, using DiskCache2  */
    String gribIndexDir = ThreddsConfig.get("GribIndex.dir", new File(tdsContext.getThreddsDirectory(), "/cache/grib/").getPath());
    Boolean gribIndexAlwaysUse = ThreddsConfig.getBoolean("GribIndex.alwaysUse", false);
    Boolean gribIndexNeverUse = ThreddsConfig.getBoolean("GribIndex.neverUse", false);
    String gribIndexPolicy = ThreddsConfig.get("GribIndex.policy", null);
    int gribIndexScourSecs = ThreddsConfig.getSeconds("GribIndex.scour", 0);
    int gribIndexMaxAgeSecs = ThreddsConfig.getSeconds("GribIndex.maxAge", 90 * 24 * 60 * 60);
    DiskCache2 gribCache = new DiskCache2(gribIndexDir, false, gribIndexMaxAgeSecs / 60, gribIndexScourSecs / 60);
    gribCache.setPolicy(gribIndexPolicy);
    gribCache.setAlwaysUseCache(gribIndexAlwaysUse);
    gribCache.setNeverUseCache(gribIndexNeverUse);
    GribIndexCache.setDiskCache2(gribCache);
    startupLog.info("TdsInit: GribIndex=" + gribCache);

    // LOOK just create the diskCache here and send it in
    ncssDiskCache.init();

    // LOOK is this used ??
    // 4.3.16
    /* dir = ThreddsConfig.get("CdmRemote.dir", new File(tdsContext.getContentDirectory().getPath(), "/cache/cdmr/").getPath());
    scourSecs = ThreddsConfig.getSeconds("CdmRemote.scour", 30 * 60);
    maxAgeSecs = ThreddsConfig.getSeconds("CdmRemote.maxAge", 60 * 60);
    DiskCache2 cdmrCache = new DiskCache2(dir, false, maxAgeSecs / 60, scourSecs / 60);
    CdmrFeatureController.setDiskCache(cdmrCache);
    startupLog.info("TdsInit: CdmRemote= " + dir + " scour = " + scourSecs + " maxAgeSecs = " + maxAgeSecs); */

    // turn back on for 4.6 needed for FMRC
    // turned off for 4.5 not used ??
    // new for 4.2 - feature collection caching
    // in 4.4, change name to FeatureCollectionCache, but keep old for backwards compatibility
    String fcCache = ThreddsConfig.get("FeatureCollectionCache.dir", null);
    if (fcCache == null)
      fcCache = ThreddsConfig.get("FeatureCollection.dir", null);
    if (fcCache == null)
      fcCache = ThreddsConfig.get("FeatureCollection.cacheDirectory", tdsContext.getThreddsDirectory().getPath() + "/cache/collection/");  // cacheDirectory is old way

    long maxSizeBytes = ThreddsConfig.getBytes("FeatureCollectionCache.maxSize", -1);
    if (maxSizeBytes == -1)
      maxSizeBytes = ThreddsConfig.getBytes("FeatureCollection.maxSize", 0);

    int jvmPercent = ThreddsConfig.getInt("FeatureCollectionCache.jvmPercent", -1);
    if (-1 == jvmPercent)
      jvmPercent = ThreddsConfig.getInt("FeatureCollection.jvmPercent", 2);

    try {
      thredds.inventory.bdb.MetadataManager.setCacheDirectory(fcCache, maxSizeBytes, jvmPercent);
      thredds.inventory.CollectionManagerAbstract.setMetadataStore(thredds.inventory.bdb.MetadataManager.getFactory());  // LOOK
      startupLog.info("TdsInit: CollectionManagerAbstract.setMetadataStore= " + fcCache);
    } catch (Exception e) {
      startupLog.error("TdsInit: Failed to open CollectionManagerAbstract.setMetadataStore= " + fcCache, e);
    }

    ///////////////////////////////////////////////
    // Object caching
    int min, max, secs;

    // RandomAccessFile: default is allow 400 - 500 open files, cleanup every 11 minutes
    min = ThreddsConfig.getInt("RandomAccessFile.minFiles", 400);
    max = ThreddsConfig.getInt("RandomAccessFile.maxFiles", 500);
    secs = ThreddsConfig.getSeconds("RandomAccessFile.scour", 11 * 60);
    if (max > 0) {
      RandomAccessFile.setGlobalFileCache(new FileCache("RandomAccessFile", min, max, -1, secs));
      startupLog.info("TdsInit: RandomAccessFile.initPartitionCache= [" + min + "," + max + "] scour = " + secs);
    }

    // NetcdfFileCache : default is allow 100 - 150 open files, cleanup every 12 minutes
    min = ThreddsConfig.getInt("NetcdfFileCache.minFiles", 100);
    max = ThreddsConfig.getInt("NetcdfFileCache.maxFiles", 150);
    secs = ThreddsConfig.getSeconds("NetcdfFileCache.scour", 12 * 60);
    if (max > 0) {
      NetcdfDataset.initNetcdfFileCache(min, max, secs);
      startupLog.info("TdsInit: NetcdfDataset.initNetcdfFileCache= [" + min + "," + max + "] scour = " + secs);
    }

    // GribCollection partitions: default is allow 100 - 150 objects, cleanup every 13 minutes
    min = ThreddsConfig.getInt("TimePartition.minFiles", 100);
    max = ThreddsConfig.getInt("TimePartition.maxFiles", 150);
    secs = ThreddsConfig.getSeconds("TimePartition.scour", 13 * 60);
    if (max > 0) {
      GribCdmIndex.initDefaultCollectionCache(min, max, secs);
      startupLog.info("TdsInit: GribCdmIndex.initDefaultCollectionCache= [" + min + "," + max + "] scour = " + secs);
    }

    //RandomAccessFile.enableDefaultGlobalFileCache();
    //RandomAccessFile.setDebugLeaks(true);

    // Config Cat Cache
    max = ThreddsConfig.getInt("ConfigCatalog.keepInMemory", 100);
    String rootPath = tdsContext.getContentRootPathProperty() + "thredds/";
    ccc.init(rootPath, max);

    // Config Dataset Tracker
    String trackerDir = ThreddsConfig.get("ConfigCatalog.dir", new File(tdsContext.getThreddsDirectory().getPath(), "/cache/catalog/").getPath());
    int trackerMax = ThreddsConfig.getInt("ConfigCatalog.maxDatasets", 10 * 1000);
    File trackerDirFile = new File(trackerDir);
    if (!trackerDirFile.exists()) {
      boolean ok = trackerDirFile.mkdirs();
      startupLog.info("TdsInit: tracker directory {} make ok = {}", trackerDir, ok);
    }
    configCatalogInitializer.setTrackerDir(trackerDir);
    configCatalogInitializer.setMaxDatasetToTrack(trackerMax);

    // Jupyter notebook service cache
    if (allowedServices.isAllowed(StandardService.jupyterNotebook)) {
      max = ThreddsConfig.getInt("JupyterNotebookService.maxFiles", 100);
      secs = ThreddsConfig.getSeconds("JupyterNotebookService.maxAge", 60*60);
      jupyterNotebooks.init(max, secs);
    }
  }

  static private class CacheScourTask extends TimerTask {
    long maxBytes;

    CacheScourTask(long maxBytes) {
      this.maxBytes = maxBytes;
    }

    public void run() {
      StringBuilder sbuff = new StringBuilder();
      DiskCache.cleanCache(maxBytes, sbuff); // 1 Gbyte
      sbuff.append("----------------------\n");
      // cacheLog.info(sbuff.toString());
    }
  }

  /*
  http://stackoverflow.com/questions/24660408/how-can-i-get-intellij-debugger-to-allow-my-apps-shutdown-hooks-to-run?lq=1
  "Unfortunately, you can't use breakpoints in your shutdown hook body when you use Stop button: these breakpoints are silently ignored."
   */
  @Override
  public void destroy() {
    System.out.printf("TdsInit.destroy() is called%n");

    // prefs
    try {
      store.save();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      startupLog.error("TdsInit: Prefs save failed", ioe);
    }

    // background threads
    if (cdmDiskCacheTimer != null)
      cdmDiskCacheTimer.cancel();
    FileCache.shutdown();              // this handles background threads for all instances of FileCache
    DiskCache2.exit();                // this handles background threads for all instances of DiskCache2
    thredds.inventory.bdb.MetadataManager.closeAll();
    executor.shutdownNow();

    /* try {
      catalogWatcher.close();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      startupLog.error("catalogWatcher close failed", ioe);
    } */

    // open file caches
    RandomAccessFile.shutdown();
    NetcdfDataset.shutdown();

    // memory caches
    GribCdmIndex.shutdown();
    datasetManager.setDatasetTracker(null); // closes the existing tracker

    collectionUpdater.shutdown();
    startupLog.info("TdsInit shutdown");
    MDC.clear();
  }

}

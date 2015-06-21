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

package thredds.server.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import thredds.client.catalog.tools.CatalogXmlWriter;
import thredds.client.catalog.tools.DataFactory;
import thredds.core.AllowedServices;
import thredds.server.catalog.ConfigCatalogCache;
import thredds.core.ConfigCatalogInitialization;
import thredds.core.DatasetManager;
import thredds.featurecollection.CollectionUpdater;
import thredds.featurecollection.InvDatasetFeatureCollection;
import thredds.server.catalog.tracker.DatasetTracker;
import thredds.server.ncss.format.FormatsAvailabilityService;
import thredds.server.ncss.format.SupportedFormat;
import thredds.util.LoggerFactorySpecial;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.ncml.Aggregation;
import ucar.nc2.util.DiskCache;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.log.LoggerFactory;
import ucar.unidata.io.RandomAccessFile;
import ucar.util.prefs.PreferencesExt;
import ucar.util.prefs.XMLStore;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A Singleton class to initialize and shutdown the CDM/TDS
 * Formerly CdmInit
 *
 * @author caron
 * @since Feb 20, 2009
 */

@Component("TdsInit")
public class TdsInit implements ApplicationListener<ContextRefreshedEvent>, DisposableBean {
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private DatasetManager datasetManager;

  @Autowired
  private ConfigCatalogCache ccc;

  @Autowired
  private DatasetTracker datasetTracker;

  @Autowired
  private ConfigCatalogInitialization configCatalogInitializer;

  @Autowired
  private AllowedServices allowedServices;

  private DiskCache2 aggCache, gribCache, cdmrCache;
  private Timer timer;
  private boolean wasInitialized;

  private XMLStore store;
  private PreferencesExt mainPrefs;

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    if (event instanceof ContextRefreshedEvent) {  // startup
      startupLog.info("TdsInit {}", event);
      startupLog.info("TdsInit getContentRootPathAbsolute= " + tdsContext.getContentRootPathProperty());
      synchronized (this) {
        if (!wasInitialized) {
          wasInitialized = true;
          readState();
          readThreddsConfig();
          boolean useEsgfMode = ThreddsConfig.getBoolean("ESGF.allow", false);
          configCatalogInitializer.init(ConfigCatalogInitialization.ReadMode.check, (PreferencesExt) mainPrefs.node("configCatalog"));
        }
      }
    }
  }

  public void readState() {
    File prefsDir = new File(tdsContext.getContentDirectory(), "/state/");
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


  public void readThreddsConfig() {

    // prefer cdmRemote when available
    DataFactory.setPreferCdm(true);
    // netcdf-3 files can only grow, not have metadata changes
    ucar.nc2.NetcdfFile.setProperty("syncExtendOnly", "true");

    // Global config
    boolean useBytesForDataSize = ThreddsConfig.getBoolean("catalogWriting.useBytesForDataSize", false);
    CatalogXmlWriter.useBytesForDataSize(useBytesForDataSize);
    startupLog.info("TdsInit: catalogWriting.useBytesForDataSize= " + useBytesForDataSize);

    // datasetSource plug-in
    for (String className : ThreddsConfig.getRootList("datasetSource")) {
      datasetManager.registerDatasetSource(className);
    }

    //////////////////////////////////////////////////////////
    // Controlling Data Services
    // see thredds.server.config.AllowableServices

    // CDM configuration

    // 4.3.17
    // feature collection logging
    /*
     <FeatureCollection>
        <RollingFileAppender>
          <MaxFileSize>1 MB</MaxFileSize>
          <MaxBackups>5</MaxBackups>
          <Level>INFO</Level>
        </RollingFileAppender>
      </FeatureCollection>
     */
    startupLog.info("TdsInit: set LoggerFactorySpecial with logging directory " + System.getProperty("tds.log.dir"));
    long maxFileSize = ThreddsConfig.getBytes("FeatureCollection.RollingFileAppender.MaxFileSize", 1000 * 1000);
    int maxBackupIndex = ThreddsConfig.getInt("FeatureCollection.RollingFileAppender.MaxBackups", 10);
    String level = ThreddsConfig.get("FeatureCollection.RollingFileAppender.Level", "INFO");
    LoggerFactory fac = new LoggerFactorySpecial(maxFileSize, maxBackupIndex, level);
    InvDatasetFeatureCollection.setLoggerFactory(fac);

    allowedServices.finish(); // finish when we know everything is wired
    InvDatasetFeatureCollection.setAllowedServices(allowedServices);

    /* <Netcdf4Clibrary>
       <libraryPath>C:/cdev/lib/</libraryPath>
       <libraryName>netcdf4</libraryName>
     </Netcdf4Clibrary>
    */
    String libraryPath = ThreddsConfig.get("Netcdf4Clibrary.libraryPath", null);
    String libraryName = ThreddsConfig.get("Netcdf4Clibrary.libraryName", null);
    if (libraryPath != null || libraryName != null) {
      Nc4Iosp.setLibraryAndPath(libraryPath, libraryName);
    }

    //Netcdf4 library could be set as a environment variable or as a jvm parameter
    if (Nc4Iosp.isClibraryPresent()) {
      FormatsAvailabilityService.setFormatAvailability(SupportedFormat.NETCDF4, true);
      FormatsAvailabilityService.setFormatAvailability(SupportedFormat.NETCDF4EXT, true);

      if (libraryName == null) libraryName = "netcdf";
      startupLog.info("netcdf4 c library loaded from jna_path='" + System.getProperty("jna.library.path") + "' " +
              "libname=" + libraryName + "");
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
    dir = ThreddsConfig.get("DiskCache.dir", new File(tdsContext.getContentDirectory(), "/cache/cdm/").getPath());
    boolean alwaysUse = ThreddsConfig.getBoolean("DiskCache.alwaysUse", false);
    scourSecs = ThreddsConfig.getSeconds("DiskCache.scour", 60 * 60); // default once an hour
    long maxSize = ThreddsConfig.getBytes("DiskCache.maxSize", (long) 1000 * 1000 * 1000);  // default 1 Gbyte
    DiskCache.setRootDirectory(dir);
    DiskCache.setCachePolicy(alwaysUse);
    startupLog.info("TdsInit:  CdmCache= " + dir + " scour = " + scourSecs + " maxSize = " + maxSize);
    if (scourSecs > 0) {
      Calendar c = Calendar.getInstance(); // contains current startup time
      c.add(Calendar.SECOND, scourSecs / 2); // starting in half the scour time
      timer = new Timer("CdmDiskCache");
      timer.scheduleAtFixedRate(new CacheScourTask(maxSize), c.getTime(), (long) 1000 * scourSecs);
    }

    // persist joinExisting aggregations. default every 24 hours, delete stuff older than 90 days
    dir = ThreddsConfig.get("AggregationCache.dir", new File(tdsContext.getContentDirectory().getPath(), "/cache/agg/").getPath());
    scourSecs = ThreddsConfig.getSeconds("AggregationCache.scour", 24 * 60 * 60);
    maxAgeSecs = ThreddsConfig.getSeconds("AggregationCache.maxAge", 90 * 24 * 60 * 60);
    aggCache = new DiskCache2(dir, false, maxAgeSecs / 60, scourSecs / 60);
    Aggregation.setPersistenceCache(aggCache);
    startupLog.info("TdsInit:  AggregationCache= " + dir + " scour = " + scourSecs + " maxAgeSecs = " + maxAgeSecs);

    /* 4.3.15: grib index file placement, using DiskCache2  */
    String gribIndexDir = ThreddsConfig.get("GribIndex.dir", new File(tdsContext.getContentDirectory().getPath(), "/cache/grib/").getPath());
    Boolean gribIndexAlwaysUse = ThreddsConfig.getBoolean("GribIndex.alwaysUse", false);
    Boolean gribIndexNeverUse = ThreddsConfig.getBoolean("GribIndex.neverUse", false);
    String gribIndexPolicy = ThreddsConfig.get("GribIndex.policy", null);
    int gribIndexScourSecs = ThreddsConfig.getSeconds("GribIndex.scour", 0);
    int gribIndexMaxAgeSecs = ThreddsConfig.getSeconds("GribIndex.maxAge", 90 * 24 * 60 * 60);
    gribCache = new DiskCache2(gribIndexDir, false, gribIndexMaxAgeSecs / 60, gribIndexScourSecs / 60);
    gribCache.setPolicy(gribIndexPolicy);
    gribCache.setAlwaysUseCache(gribIndexAlwaysUse);
    gribCache.setNeverUseCache(gribIndexNeverUse);
    GribIndexCache.setDiskCache2(gribCache);
    startupLog.info("TdsInit: GribIndex=" + gribCache);

    // LOOK is this used ??
    // 4.3.16
    dir = ThreddsConfig.get("CdmRemote.dir", new File(tdsContext.getContentDirectory().getPath(), "/cache/cdmr/").getPath());
    scourSecs = ThreddsConfig.getSeconds("CdmRemote.scour", 30 * 60);
    maxAgeSecs = ThreddsConfig.getSeconds("CdmRemote.maxAge", 60 * 60);
    cdmrCache = new DiskCache2(dir, false, maxAgeSecs / 60, scourSecs / 60);
    //CdmrFeatureController.setDiskCache(cdmrCache);
    startupLog.info("TdsInit:  CdmRemote= " + dir + " scour = " + scourSecs + " maxAgeSecs = " + maxAgeSecs);


    // turn back on for 4.6 needed for FMRC
    // turned off for 4.5 not used ??
    // new for 4.2 - feature collection caching
    // in 4.4, change name to FeatureCollectionCache, but keep old for backwards compatibility
    String fcCache = ThreddsConfig.get("FeatureCollectionCache.dir", null);
    if (fcCache == null)
      fcCache = ThreddsConfig.get("FeatureCollection.dir", null);
    if (fcCache == null)
      fcCache = ThreddsConfig.get("FeatureCollection.cacheDirectory", tdsContext.getContentDirectory().getPath() + "/cache/collection/");  // cacheDirectory is old way

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
      startupLog.info("NetcdfDataset.initNetcdfFileCache= [" + min + "," + max + "] scour = " + secs);
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
    max = ThreddsConfig.getInt("Catalog.cacheCatalogs", 100);
    String rootPath = tdsContext.getContentRootPathProperty() +"thredds/";
    ccc.init(rootPath, max);

    // Config Dataset Tracker
    String trackerDir = ThreddsConfig.get("ESGF.dir", new File(tdsContext.getContentDirectory().getPath(), "/cache/catalog/").getPath());
    int trackerMax = ThreddsConfig.getInt("ESGF.maxDatasets", 1000 * 1000);
    try {
      File trackerFile = new File(trackerDir);
      if (!trackerFile.exists()) {
        boolean ok = trackerFile.mkdir();
        startupLog.info("TdsInit: tracker directory {} make ok = {}", trackerDir, ok);
      }

      if (!datasetTracker.init(trackerDir, trackerMax))
        throw new IllegalStateException("Cant start datasetTracker");

    } catch (IOException e) {
      startupLog.error("Error initializing dataset tracker " + datasetTracker.getClass().getName(), e);
    }

    startupLog.info("TdsInit complete");
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

  @Override
  public void destroy() {
    try {
      store.save();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    // background threads
    if (timer != null) timer.cancel();
    FileCache.shutdown();              // this handles background threads for all instances of FileCache
    if (aggCache != null) aggCache.exit();
    if (gribCache != null) gribCache.exit();
    if (cdmrCache != null) cdmrCache.exit();
    thredds.inventory.bdb.MetadataManager.closeAll(); // LOOK used ??
    CollectionUpdater.INSTANCE.shutdown();

    // open files caches
    RandomAccessFile.shutdown();
    NetcdfDataset.shutdown();

    // memory caches
    GribCdmIndex.shutdown();
    datasetTracker.close();

    startupLog.info("TdsInit shutdown");
    MDC.clear();
  }

}

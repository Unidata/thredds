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

package thredds.server.config;

import java.io.File;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Level;
import org.slf4j.MDC;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import thredds.catalog.InvDatasetFeatureCollection;
import thredds.catalog.parser.jdom.InvCatalogFactory10;
import thredds.inventory.CollectionManagerAbstract;
import thredds.inventory.CollectionUpdater;
import thredds.server.cdmremote.CdmrFeatureController;
import thredds.server.ncSubset.format.SupportedFormat;
import thredds.servlet.ServletUtil;
import thredds.servlet.ThreddsConfig;
import thredds.util.LoggerFactorySpecial;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.grib.GribCollection;
import ucar.nc2.grib.TimePartition;
import ucar.nc2.iosp.grid.GridServiceProvider;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.ncml.Aggregation;
import ucar.nc2.thredds.ThreddsDataFactory;
import ucar.nc2.util.DiskCache;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.log.LoggerFactory;

/**
 * A Singleton class to initialize the CDM, instantiated by Spring.
 * Called from TdsConfigContextListener.
 *
 * @author caron
 * @since Feb 20, 2009
 */

@Component ("CdmInit")
public class CdmInit implements InitializingBean,  DisposableBean{
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

  private DiskCache2 aggCache, gribCache, cdmrCache;
  private Timer timer;
  private thredds.inventory.MController cacheManager;
  
  @Autowired
  private TdsContext tdsContext;

  public void afterPropertiesSet(){
    // prefer cdmRemote when available
    ThreddsDataFactory.setPreferCdm(true);

    // 4.3.17
    // feature collection logging
    /* from log4j RollingFileAppender:
       <param name="MaxFileSize" value="100KB"/>
       <param name="MaxBackupIndex" value="2"/>

       currently we have bdb stuff:
     <FeatureCollection>
       <dir>/tomcat_home/content/thredds/cache/collection/</dir>
       <maxSize>20 Mb</maxSize>
       <jvmPercent>2</jvmPercent>
     </FeatureCollection>

     how about:
     <FeatureCollection>
        <RollingFileAppender>
          <MaxFileSize>1 MB</MaxFileSize>
          <MaxBackupIndex>5</MaxBackupIndex>
          <Level>INFO</Level>
        </RollingFileAppender>
      </FeatureCollection>
     */
    startupLog.info("CdmInit: set LoggerFactorySpecial with logging directory "+System.getProperty("tds.log.dir"));
    long maxFileSize = ThreddsConfig.getBytes("FeatureCollection.RollingFileAppender.MaxFileSize", 1000 * 1000);
    int maxBackupIndex = ThreddsConfig.getInt("FeatureCollection.RollingFileAppender.MaxBackupIndex", 5);
    String level = ThreddsConfig.get("FeatureCollection.RollingFileAppender.Level", "INFO");
    LoggerFactory fac = new LoggerFactorySpecial(maxFileSize, maxBackupIndex, level);
    InvDatasetFeatureCollection.setLoggerFactory(fac);

    // 4.3.16
    String dir = ThreddsConfig.get("CdmRemote.dir", new File( tdsContext.getContentDirectory().getPath(), "/cache/cdmr/").getPath());
    int scourSecs = ThreddsConfig.getSeconds("CdmRemote.scour", 30 * 60);
    int maxAgeSecs = ThreddsConfig.getSeconds("CdmRemote.maxAge", 60 * 60);
    cdmrCache = new DiskCache2(dir, false, maxAgeSecs / 60, scourSecs / 60);
    CdmrFeatureController.setDiskCache(cdmrCache);
    startupLog.info("CdmInit:  CdmRemote= "+dir+" scour = "+scourSecs+" maxAgeSecs = "+maxAgeSecs);

    /* 4.3.15: grib index file placement, using DiskCache2  */
    String gribIndexDir = ThreddsConfig.get("GribIndex.dir", new File( tdsContext.getContentDirectory().getPath(), "/cache/grib/").getPath());
    Boolean gribIndexAlwaysUse = ThreddsConfig.getBoolean("GribIndex.alwaysUse", false);
    String gribIndexPolicy = ThreddsConfig.get("GribIndex.policy", null);
    int gribIndexScourSecs = ThreddsConfig.getSeconds("GribIndex.scour", 0);
    int gribIndexMaxAgeSecs = ThreddsConfig.getSeconds("GribIndex.maxAge", 90 * 24 * 60 * 60);
    gribCache = new DiskCache2(gribIndexDir, false, gribIndexMaxAgeSecs / 60, gribIndexScourSecs / 60);
    gribCache.setPolicy(gribIndexPolicy);
    gribCache.setAlwaysUseCache(gribIndexAlwaysUse);
    GribCollection.setDiskCache2(gribCache);
    startupLog.info("CdmInit: GribIndex="+gribCache);

    // new for 4.2 - feature collection caching
    String fcCache = ThreddsConfig.get("FeatureCollection.dir", null);
    if (fcCache == null)
      fcCache = ThreddsConfig.get("FeatureCollection.cacheDirectory", tdsContext.getContentDirectory().getPath() + "/cache/collection/");  // cacheDirectory is old way
    long maxSizeBytes = ThreddsConfig.getBytes("FeatureCollection.maxSize", 0);
    int jvmPercent = ThreddsConfig.getInt("FeatureCollection.jvmPercent", 2);

    try {
      thredds.inventory.bdb.MetadataManager.setCacheDirectory(fcCache, maxSizeBytes, jvmPercent);
      thredds.inventory.CollectionManagerAbstract.enableMetadataManager();
      startupLog.info("CdmInit: FeatureCollection.cacheDirectory= "+fcCache);
    } catch (Exception e) {
      startupLog.error("CdmInit: Failed to open FeatureCollection.cacheDirectory= "+fcCache, e);
    }

    /*
    // new for 4.1 - ehcache object caching
    String ehConfig = ThreddsConfig.get("ehcache.configFile", tdsContext.getWebinfPath() + "/ehcache.xml");
    String ehDirectory = ThreddsConfig.get("ehcache.dir", null);
    if (ehDirectory == null)
      ehDirectory = ThreddsConfig.get("ehcache.directory", tdsContext.getContentDirectory().getPath() + "/cache/ehcache/");  // directory is old way
    try {
      cacheManager = thredds.filesystem.ControllerCaching.makeStandardController(ehConfig, ehDirectory);
      DatasetCollectionMFiles.setController(cacheManager);
      startupLog.info("CdmInit: ehcache.config= "+ehConfig+" directory= "+ehDirectory);

    } catch (IOException ioe) {
      startupLog.error("CdmInit: Cant read ehcache config file "+ehConfig, ioe);
    }
    */

    boolean useBytesForDataSize = ThreddsConfig.getBoolean("catalogWriting.useBytesForDataSize", false);    
    InvCatalogFactory10.useBytesForDataSize(useBytesForDataSize);
    startupLog.info("CdmInit: catalogWriting.useBytesForDataSize= "+useBytesForDataSize);

    ////////////////////////////////////
    //AggregationFmrc.setDefinitionDirectory(new File(tdsContext.getRootDirectory(), fmrcDefinitionDirectory));
    // FmrcInventoryServlet.setDefinitionDirectory(new File(tdsContext.getRootDirectory(), fmrcDefinitionDirectory));

    // NetcdfFileCache : default is allow 50 - 100 open files, cleanup every 11 minutes
    int min = ThreddsConfig.getInt("NetcdfFileCache.minFiles", 50);
    int max = ThreddsConfig.getInt("NetcdfFileCache.maxFiles", 100);
    int secs = ThreddsConfig.getSeconds("NetcdfFileCache.scour", 11 * 60);
    if (max > 0) {
      NetcdfDataset.initNetcdfFileCache(min, max, secs);
      startupLog.info("CdmInit  private boolean isNetcdf4Available = false;: NetcdfDataset.initNetcdfFileCache= ["+min+","+max+"] scour = "+secs);
    }

    // GribCollection partitions: default is allow 50 - 100 open files, cleanup every 12 minutes
    min = ThreddsConfig.getInt("TimePartition.minFiles", 50);
    max = ThreddsConfig.getInt("TimePartition.maxFiles", 100);
    secs = ThreddsConfig.getSeconds("TimePartition.scour", 12 * 60);
    if (max > 0) {
      TimePartition.initPartitionCache(min, max, secs);
      startupLog.info("CdmInit: TimePartition.initPartitionCache= ["+min+","+max+"] scour = "+secs);
    }

    // GribCollection data files : default is allow 50 - 100 open files, cleanup every 13 minutes
    min = ThreddsConfig.getInt("GribCollection.minFiles", 50);
    max = ThreddsConfig.getInt("GribCollection.maxFiles", 100);
    secs = ThreddsConfig.getSeconds("GribCollection.scour", 13 * 60);
    if (max > 0) {
      GribCollection.initDataRafCache(min, max, secs);
      startupLog.info("CdmInit: GribCollection.initDataRafCache= ["+min+","+max+"] scour = "+secs);
    }

    // HTTP file access : // allow 10 - 20 open datasets, cleanup every 17 minutes
    min = ThreddsConfig.getInt("HTTPFileCache.minFiles", 10);
    max = ThreddsConfig.getInt("HTTPFileCache.maxFiles", 20);
    secs = ThreddsConfig.getSeconds("HTTPFileCache.scour", 17 * 60);
    if (max > 0) {
      ServletUtil.setFileCache( new FileCache("HTTP File Cache", min, max, -1, secs));
      startupLog.info("CdmInit: HTTPFileCache.initCache= [" + min + "," + max + "] scour = " + secs);
    }

    // for backwards compatibility - should be replaced by direct specifying of the IndexExtendMode
    // turn off Grib extend indexing; indexes are automatically done every 10 minutes externally
    boolean extendIndex = ThreddsConfig.getBoolean("GribIndexing.setExtendIndex", false);
    GridServiceProvider.IndexExtendMode mode = extendIndex ? GridServiceProvider.IndexExtendMode.extendwrite : GridServiceProvider.IndexExtendMode.readonly;
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexFileModeOnOpen( mode);
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexFileModeOnSync( mode);
    startupLog.info("CdmInit: GridServiceProvider.IndexExtendMode= "+mode);

    boolean alwaysUseCache = ThreddsConfig.getBoolean("GribIndexing.alwaysUseCache", false);
    ucar.nc2.iosp.grid.GridServiceProvider.setIndexAlwaysInCache( alwaysUseCache );
    startupLog.info("CdmInit: GribIndexing.alwaysUseCache= "+alwaysUseCache);

    // optimization: netcdf-3 files can only grow, not have metadata changes
    ucar.nc2.NetcdfFile.setProperty("syncExtendOnly", "true");

    // persist joinExisting aggregations. default every 24 hours, delete stuff older than 90 days
    dir = ThreddsConfig.get("AggregationCache.dir", new File( tdsContext.getContentDirectory().getPath(), "/cache/agg/").getPath());
    scourSecs = ThreddsConfig.getSeconds("AggregationCache.scour", 24 * 60 * 60);
    maxAgeSecs = ThreddsConfig.getSeconds("AggregationCache.maxAge", 90 * 24 * 60 * 60);
    aggCache = new DiskCache2(dir, false, maxAgeSecs / 60, scourSecs / 60);
    Aggregation.setPersistenceCache(aggCache);
    startupLog.info("CdmInit:  AggregationCache= "+dir+" scour = "+scourSecs+" maxAgeSecs = "+maxAgeSecs);

    // how to choose the typical dataset ?
    String typicalDataset = ThreddsConfig.get("Aggregation.typicalDataset", "penultimate");
    Aggregation.setTypicalDatasetMode(typicalDataset);
    startupLog.info("CdmInit: Aggregation.setTypicalDatasetMode= "+typicalDataset);

    // Nj22 disk cache
    dir = ThreddsConfig.get("DiskCache.dir", new File( tdsContext.getContentDirectory(), "/cache/cdm/" ).getPath());
    boolean alwaysUse = ThreddsConfig.getBoolean("DiskCache.alwaysUse", false);
    scourSecs = ThreddsConfig.getSeconds("DiskCache.scour", 60 * 60); // default once an hour
    long maxSize = ThreddsConfig.getBytes("DiskCache.maxSize", (long) 1000 * 1000 * 1000);  // default 1 Gbyte
    DiskCache.setRootDirectory(dir);
    DiskCache.setCachePolicy(alwaysUse);
    startupLog.info("CdmInit:  CdmCache= "+dir+" scour = "+scourSecs+" maxSize = "+maxSize);

    if (scourSecs > 0) {
      Calendar c = Calendar.getInstance(); // contains current startup time
      c.add(Calendar.SECOND, scourSecs / 2); // starting in half the scour time
      timer = new Timer("CdmDiskCache");
      timer.scheduleAtFixedRate(new CacheScourTask(maxSize), c.getTime(), (long) 1000 * scourSecs);
    }

     /* <Netcdf4Clibrary>
       <libraryPath>C:/cdev/lib/</libraryPath>
       <libraryName>netcdf4</libraryName>
     </Netcdf4Clibrary> */
    String libraryPath = ThreddsConfig.get("Netcdf4Clibrary.libraryPath", null);
    String libraryName = ThreddsConfig.get("Netcdf4Clibrary.libraryName", null);
    if (libraryPath != null || libraryName != null){
      Nc4Iosp.setLibraryAndPath(libraryPath, libraryName);
    } else {
      Nc4Iosp.setWarnOff(); // if they dont have a Netcdf4Clibrary element, suppress warnings
    }
    
    //Netcdf4 library could be set as a environment variable or as a jvm parameter 
    if(!Nc4Iosp.isClibraryPresent() ){
      startupLog.warn("netcdf4 c library not present jna_path='" + libraryPath + "' libname=" + libraryName + "" );    	 
    }else{    	  
      FormatsAvailabilityService.setFormatAvailability(SupportedFormat.NETCDF4, true);      
      if(libraryName == null) libraryName="netcdf";
      startupLog.info("netcdf4 c library loaded from jna_path='" + System.getProperty("jna.library.path") + "' libname=" + libraryName + "" );
    }
    
    startupLog.info("CdmInit complete");
  }

  //should be called when tomcat exits
  public void destroy() throws Exception {
    if (timer != null) timer.cancel();
    FileCache.shutdown();
    if (aggCache != null) aggCache.exit();
    if (gribCache != null) gribCache.exit();
    if (cdmrCache != null) cdmrCache.exit();
    if (cacheManager != null) cacheManager.close();
    thredds.inventory.bdb.MetadataManager.closeAll();
    CollectionUpdater.INSTANCE.shutdown();
    startupLog.info("CdmInit shutdown");
    MDC.clear();
  }

  private class CacheScourTask extends TimerTask {
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

}

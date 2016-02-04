/*
 *
 *  * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
 *  *
 *  *  Portions of this software were developed by the Unidata Program at the
 *  *  University Corporation for Atmospheric Research.
 *  *
 *  *  Access and use of this software shall impose the following obligations
 *  *  and understandings on the user. The user is granted the right, without
 *  *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  *  this software, and any derivative works thereof, and its supporting
 *  *  documentation for any purpose whatsoever, provided that this entire
 *  *  notice appears in all copies of the software, derivative works and
 *  *  supporting documentation.  Further, UCAR requests that the user credit
 *  *  UCAR/Unidata in any publications that result from the use of this
 *  *  software or in any product that includes this software. The names UCAR
 *  *  and/or Unidata, however, may not be used in any advertising or publicity
 *  *  to endorse or promote any products or commercial entity unless specific
 *  *  written permission is obtained from UCAR/Unidata. The user also
 *  *  understands that UCAR/Unidata is not obligated to provide the user with
 *  *  any support, consulting, training or assistance of any kind with regard
 *  *  to the use, operation and performance of this software nor to provide
 *  *  the user with any updates, revisions, new versions or "bug fixes."
 *  *
 *  *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 *
 */

package thredds.tdm;

import org.apache.http.auth.*;
import org.slf4j.Logger;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.*;
import thredds.util.*;
import ucar.httpservices.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.log.LoggerFactory;
import ucar.unidata.io.RandomAccessFile;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Describe
 *
 * @author caron
 * @since 12/13/13
 */
public class Tdm {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Tdm.class);
  private static final boolean debug = false;
  private static final boolean debugOpenFiles = false;

  private Path contentDir;
  private Path contentThreddsDir;
  private Path contentTdmDir;
  private Path threddsConfig;

  private String user, pass;
  private boolean sendTriggers;
  private String[] serverNames;
  private List<Server> servers;

  private java.util.concurrent.ExecutorService executor;
  private Resource catalog;
  private boolean showOnly = false; // if true, just show dirs and exit

  private String loglevel;
  private boolean forceOnStartup = false; // if true, just show dirs and exit

  LoggerFactory loggerFactory;
  List<Resource> catalogRoots = new ArrayList<>();

  private static class Server {
    String name;
    HTTPSession session;

    private Server(String name, HTTPSession session) {
      this.name = name;
      this.session = session;
      System.out.printf("Server added %s%n", name);
      log.info("TDS server added " + name);
    }
  }

  public void setContentDir(String contentDir) throws IOException {
    System.out.printf("contentDir=%s%n", contentDir);
    this.contentDir = Paths.get(contentDir);
    this.contentThreddsDir = Paths.get(contentDir, "thredds");
    this.threddsConfig = Paths.get(contentDir, "thredds", "threddsConfig.xml");
    this.contentTdmDir = Paths.get(contentDir, "tdm");
    this.catalog = new FileSystemResource(contentThreddsDir.toString() + "/catalog.xml");
    System.out.printf("catalog=%s%n", catalog.getFile().getPath());
  }

  public void setShowOnly(boolean showOnly) {
    this.showOnly = showOnly;
  }

  public void setNThreads(int n) {
    executor = Executors.newFixedThreadPool(n);
    log.info(" TDM nthreads= {}", n);
  }

  public void setForceOnStartup(boolean forceOnStartup) {
    this.forceOnStartup = forceOnStartup;
  }

  public void setLoglevel(String loglevel) {
    this.loglevel = loglevel;
  }

  // spring beaned
  public void setExecutor(ExecutorService executor) {
    this.executor = executor;
  }

  public void setCatalog(Resource catalog) {
    this.catalog = catalog;
  }

  public void setServerNames(String[] serverNames) {
    this.serverNames = serverNames;
  }

  public void initServers() throws HTTPException {
    if (serverNames == null) {
      servers = new ArrayList<>(); // empty list
      return;
    }

    this.servers = new ArrayList<>(this.serverNames.length);
    for (String name : this.serverNames) {
      try (HTTPSession session = HTTPFactory.newSession(name)) {
        if (user != null && pass != null)
          session.setCredentials(new UsernamePasswordCredentials(user, pass));
        session.setUserAgent("TDM");
        servers.add(new Server(name, session));
      }
    }
  }

  AliasHandler aliasHandler;

  public void setPathAliasReplacements(List<PathAliasReplacement> aliasExpanders) {
    aliasHandler = new AliasHandler(aliasExpanders);
  }

  boolean init() throws HTTPException {
    initServers();

    System.setProperty("tds.log.dir", contentTdmDir.toString());

    if (!Files.exists(threddsConfig)) {
      log.error("config file {} does not exist, set -Dtds.content.root.path=<dir>", threddsConfig);
      System.out.printf("threddsConfig does not exist=%s%n", threddsConfig);
      return false;
    }
    ThreddsConfigReader reader = new ThreddsConfigReader(threddsConfig.toString(), log);

    for (String location : reader.getRootList("catalogRoot")) {
      Resource r = new FileSystemResource(contentThreddsDir.toString() + "/" + location);
      catalogRoots.add(r);
    }

    // LOOK following has been duplicated from tds cdmInit
    // 4.3.17
    long maxFileSize = reader.getBytes("FeatureCollection.RollingFileAppender.MaxFileSize", 1000 * 1000);
    int maxBackupIndex = reader.getInt("FeatureCollection.RollingFileAppender.MaxBackups", 10);
    String level = reader.get("FeatureCollection.RollingFileAppender.Level", "INFO");
    if (this.loglevel != null) level = this.loglevel;
    loggerFactory = new LoggerFactorySpecial(maxFileSize, maxBackupIndex, level);

    /* 4.3.15: grib index file placement, using DiskCache2  */
    String gribIndexDir = reader.get("GribIndex.dir", new File(contentThreddsDir.toString(), "cache/grib/").getPath());
    Boolean gribIndexAlwaysUse = reader.getBoolean("GribIndex.alwaysUse", false);
    Boolean gribIndexNeverUse = reader.getBoolean("GribIndex.neverUse", false);
    String gribIndexPolicy = reader.get("GribIndex.policy", null);
    DiskCache2 gribCache = gribIndexNeverUse ? DiskCache2.getNoop() : new DiskCache2(gribIndexDir, false, -1, -1);
    gribCache.setPolicy(gribIndexPolicy);
    gribCache.setAlwaysUseCache(gribIndexAlwaysUse);
    gribCache.setNeverUseCache(gribIndexNeverUse);
    GribIndexCache.setDiskCache2(gribCache);
    log.info("TDM set " + gribCache);

    return true;
  }

  void start() throws IOException {
    System.out.printf("Tdm startup at %s%n", new Date());

    List<FeatureCollectionConfig> fcList = new ArrayList<>();
    CatalogConfigReader reader = new CatalogConfigReader(catalog, aliasHandler);
    fcList.addAll(reader.getFcList());

    // do the catalogRoots
    for (Resource catr : catalogRoots) {
      CatalogConfigReader r = new CatalogConfigReader(catr, aliasHandler);
      fcList.addAll(r.getFcList());
    }

    if (showOnly) {
      List<String> result = new ArrayList<>();
      for (FeatureCollectionConfig config : fcList) {
        result.add(config.collectionName);
      }
      Collections.sort(result);

      System.out.printf("%nFeature Collection names:%n");
      for (String name : result)
        System.out.printf(" %s%n", name);

      System.out.printf("%nTriggers:%n");
      for (String name : result)
        System.out.printf(" %s%n", makeTriggerUrl(name));

      executor.shutdown();
      CollectionUpdater.INSTANCE.shutdown();
      return;
    }

    for (FeatureCollectionConfig config : fcList) {
      if (config.type != FeatureCollectionType.GRIB1 && config.type != FeatureCollectionType.GRIB2) continue;
      System.out.printf("FeatureCollection %s scheduled %n", config.collectionName);

      if (forceOnStartup) // on startup, force rewrite of indexes
        config.tdmConfig.startupType = CollectionUpdateType.always;

      Logger logger = loggerFactory.getLogger("fc." + config.collectionName); // seperate log file for each feature collection (!!)
      logger.info("FeatureCollection config=" + config);
      CollectionUpdater.INSTANCE.scheduleTasks(config, new Listener(config, logger), logger); // now wired for events
    }

     /* show whats up
     Formatter f = new Formatter();
     f.format("Feature Collections found:%n");
     for (FeatureCollectionConfig fc : fcList) {
       CollectionManager dcm = fc.getDatasetCollectionManager();
       f.format("  %s == %s%n%s%n%n", fc, fc.getClass().getName(), dcm);
     }
     System.out.printf("%s%n", f.toString()); */
  }

  // these objects listen for schedule events from quartz.
  // one listener for each fc.
  private class Listener implements CollectionUpdateListener {
    FeatureCollectionConfig config;
    //MCollection dcm;
    AtomicBoolean inUse = new AtomicBoolean(false);
    org.slf4j.Logger logger;

    private Listener(FeatureCollectionConfig config, Logger logger) {
      this.config = config;
      this.logger = logger;
    }

    /* old
    public void handleCollectionEvent(CollectionManager.TriggerEvent event) {
      if (event.getType() != CollectionManager.TriggerType.update) return;

      // make sure that each collection is only being indexed by one thread at a time
      if (inUse.get()) {
        logger.debug("** Update already in progress for {} {}", config.name, event.getType());
        return;
      }
      if (!inUse.compareAndSet(false, true)) return;

      executor.execute(new IndexTask(config, dcm, this, logger));
    } */

    @Override
    public String getCollectionName() {
      return config.collectionName;
    }

    @Override
    public void sendEvent(CollectionUpdateType event) {
      if (!inUse.compareAndSet(false, true)) return; // if already working, skip another execution
      executor.execute(new IndexTask(config, this, event, logger));
    }
  }

  private String makeTriggerUrl(String name) {
    return "thredds/admin/collection/trigger?trigger=never&collection=" + name;
    // return "thredds/admin/collection/trigger?nocheck&collection=" + name;  // LOOK changed to nocheck for triggering 4.3, temp kludge

  }

  private class IndexTask implements Runnable {
    String name;
    FeatureCollectionConfig config;
    CollectionUpdateType updateType;
    Listener liz;
    org.slf4j.Logger logger;

    private IndexTask(FeatureCollectionConfig config, Listener liz, CollectionUpdateType updateType, org.slf4j.Logger logger) {
      this.name = config.collectionName;
      this.config = config;
      this.liz = liz;
      this.updateType = updateType;
      this.logger = logger;
    }

    @Override
    public void run() {
      try {
        // log.info("Tdm call GribCdmIndex.updateGribCollection "+config.collectionName);
        if (debug) System.out.printf("---------------------%nIndexTask updateGribCollection %s%n", config.collectionName);
        boolean changed = GribCdmIndex.updateGribCollection(config, updateType, logger);
        log.info("GribCdmIndex.updateGribCollection {} changed {}", config.collectionName, changed);

        logger.debug("{} {} changed {}", CalendarDate.present(), config.collectionName, changed);
        if (changed) System.out.printf("%s %s changed%n", CalendarDate.present(), config.collectionName);

        if (changed && config.tdmConfig.triggerOk && sendTriggers) { // send a trigger if enabled
          String path = makeTriggerUrl(name);

          sendTriggers(path);
        }
      } catch (Throwable e) {
        logger.error("Tdm.IndexTask " + name, e);
        e.printStackTrace();

      } finally {
        // tell liz that task is done
        if (!liz.inUse.getAndSet(false))
          logger.warn("Listener InUse should have been set");
      }

      if (debugOpenFiles) {
        List<String> openFiles = RandomAccessFile.getOpenFiles();
        if (openFiles.size() > 0) {
          System.out.printf("Open Files%n");
          for (String filename : RandomAccessFile.getOpenFiles()) {
            System.out.printf("  %s%n", filename);
          }
          System.out.printf("End Open Files%n");
        }
      }

    }

    private void sendTriggers(String path)
    {
      for(Server server : servers) {
        String url = server.name + path;
        try {
          try (HTTPMethod m = HTTPFactory.Get(server.session, url)) {
            int status = m.execute();
            if(status == 200)
              logger.info("send trigger to {} status = {}", url, status);
            else
              logger.warn("FAIL send trigger to {} status = {}", url, status);
          }
        } catch (HTTPException e) {
          Throwable cause = e.getCause();
          if(cause instanceof ConnectException) {
            logger.warn("server {} not running", server.name);
          } else {
            e.printStackTrace();
            logger.error("FAIL send trigger to " + url + " failed", cause);
          }

        }

      }
    }

    /* private void doManage(String deleteAfterS) throws IOException {
      TimeDuration deleteAfter = null;
      if (deleteAfterS != null) {
        try {
          deleteAfter = new TimeDuration(deleteAfterS);
        } catch (Exception e) {
          logger.error(dcm.getCollectionName() + ": Invalid time unit for deleteAfter = {}", deleteAfter);
          return;
        }
      }

      // awkward
      double val = deleteAfter.getValue();
      CalendarPeriod.Field unit = CalendarPeriod.fromUnitString(deleteAfter.getTimeUnit().getUnitString());
      CalendarPeriod period = CalendarPeriod.of(1, unit);
      CalendarDate now = CalendarDate.of(new Date());
      CalendarDate last = now.add(-val, unit);

      try (CloseableIterator<MFile> iter = dcm.getFileIterator()) {
        while (iter.hasNext()) {
          MFile mfile = iter.next();
          CalendarDate cd = dcm.extractDate(mfile);
          int n = period.subtract(cd, now);
          if (cd.isBefore(last)) {
            logger.info("delete={} age = {}", mfile.getPath(), n + " " + unit);
          } else {
            logger.debug("dont delete={} age = {}", mfile.getPath(), n + " " + unit);
          }
        }
      }
    } */

  }


  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /* public static void main2(String[] args) throws IOException {
    long start = System.currentTimeMillis();

    FeatureCollectionConfig config = FeatureCollectionReader.readFeatureCollection("F:/data/grib/idd/modelsNcep.xml#DGEX-CONUS_12km");
    if (config == null) return;

     /* Formatter errlog = new Formatter();
     Path topPath = Paths.get("B:/ndfd/200901/20090101");
     GribCollection gc = makeGribCollectionIndexOneDirectory(config, CollectionManager.Force.always, topPath, errlog);
     System.out.printf("%s%n", errlog);
     gc.close();


    //Path topPath = Paths.get("B:/ndfd/200906");
    // rewriteIndexesPartitionAll(config, topPath);
    //Grib2TimePartition tp = makeTimePartitionIndexOneDirectory(config, CollectionManager.Force.always, topPath);
    //tp.close();

    System.out.printf("name = %s%n", config.name);
    System.out.printf("spec = %s%n", config.spec);

    GribCdmIndex2.rewriteFilePartition(config, CollectionManager.Force.always, CollectionManager.Force.always);
    //rewriteDirectoryCollection(config, topPath, false);

    long took = System.currentTimeMillis() - start;
    System.out.printf("that all took %s msecs%n", took);
  } */

  public static void main(String args[]) throws IOException, InterruptedException {
    try (FileSystemXmlApplicationContext springContext = new FileSystemXmlApplicationContext("classpath:resources/application-config.xml")) {
      Tdm driver = (Tdm) springContext.getBean("testDriver2");

      Map<String, String> aliases = (Map<String, String>) springContext.getBean("dataRootLocationAliasExpanders");
      List<PathAliasReplacement> aliasExpanders = PathAliasReplacementImpl.makePathAliasReplacements(aliases);
      driver.setPathAliasReplacements(aliasExpanders);
      CollectionUpdater.INSTANCE.setTdm(true);

      String contentDir = System.getProperty("tds.content.root.path");
      if (contentDir == null) contentDir = "../content";
      driver.setContentDir(contentDir);

      RandomAccessFile.setDebugLeaks(true);
      HTTPSession.setGlobalUserAgent("TDM v4.6");
      // GribCollection.getDiskCache2().setNeverUseCache(true);
      String logLevel;

      // /opt/jdk/bin/java -d64 -Xmx3g -jar -Dtds.content.root.path=/opt/tds-dev/content tdm-4.5.jar -cred tdm:trigger -tds "http://thredds-dev.unidata.ucar.edu/"
      for (int i = 0; i < args.length; i++) {
        if (args[i].equalsIgnoreCase("-help")) {
          System.out.printf("usage: <Java> <Java_OPTS> -Dtds.content.root.path=<contentDir> [-catalog <cat>] [-tds <tdsServer>] [-cred <user:passwd>] [-showOnly] [-forceOnStartup]%n");
          System.out.printf("example: /opt/jdk/bin/java -Xmx3g -Dtds.content.root.path=/my/content -jar tdm-4.5.jar -tds http://thredds-dev.unidata.ucar.edu/%n");
          System.exit(0);
        }

        if (args[i].equalsIgnoreCase("-contentDir")) {
          driver.setContentDir(args[i + 1]);
          i++;
        } else if (args[i].equalsIgnoreCase("-catalog")) {
          Resource cat = new FileSystemResource(args[i + 1]);
          driver.setCatalog(cat);
        } else if (args[i].equalsIgnoreCase("-tds")) {
          String tds = args[i + 1];
          if (tds.equalsIgnoreCase("none")) {
            driver.setServerNames(null);
            driver.sendTriggers = false;

          } else {
            String[] tdss = tds.split(","); // comma separated
            driver.setServerNames(tdss);
            driver.sendTriggers = true;
          }
        }
        // scheme://username:password@domain:port/path?query_string#fragment_id
        else if (args[i].equalsIgnoreCase("-cred")) {  // LOOK could be http://user:password@server
          String cred = args[i + 1];
          String[] split = cred.split(":");
          driver.user = split[0];
          driver.pass = split[1];
          driver.sendTriggers = true;
        } else if (args[i].equalsIgnoreCase("-nthreads")) {
          int n = Integer.parseInt(args[i + 1]);
          driver.setNThreads(n);
        } else if (args[i].equalsIgnoreCase("-showOnly")) {
          driver.setShowOnly(true);
        } else if (args[i].equalsIgnoreCase("-log")) {
          logLevel = args[i + 1];
          driver.setLoglevel(logLevel);
        } else if (args[i].equalsIgnoreCase("-forceOnStartup")) {
          driver.setForceOnStartup(true);
        }
      }

      if (!driver.showOnly && driver.pass == null && driver.sendTriggers) {
        Scanner scanner = new Scanner(System.in, CDM.UTF8);
        String passw;
        while (true) {
          System.out.printf("%nEnter password for tds trigger: ");
          passw = scanner.nextLine();
          System.out.printf("%nPassword = '%s' OK (Y/N)?", passw);
          String ok = scanner.nextLine();
          if (ok.equalsIgnoreCase("Y")) break;
        }
        if (passw != null) {
          driver.pass = passw;
          driver.user = "tdm";
        } else {
          driver.sendTriggers = false;
        }
      }

      if (driver.init()) {
        driver.start();
      } else {
        System.out.printf("%nEXIT DUE TO ERRORS");
      }
    }
  }

}

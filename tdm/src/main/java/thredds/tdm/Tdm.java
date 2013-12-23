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

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.*;
import thredds.util.*;
import ucar.nc2.grib.collection.GribCdmIndex2;
import ucar.nc2.grib.collection.GribCollection;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.DiskCache2;
import ucar.nc2.util.log.LoggerFactory;
import ucar.nc2.util.net.HTTPException;
import ucar.nc2.util.net.HTTPMethod;
import ucar.nc2.util.net.HTTPSession;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Describe
 *
 * @author caron
 * @since 12/13/13
 */
public class Tdm {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( Tdm.class);

  private Path contentDir;
  private Path contentThreddsDir;
  private Path contentTdmDir;
  private Path threddsConfig;

  private String user, pass;
  private boolean sendTriggers;
  private List<Server> servers;

  private java.util.concurrent.ExecutorService executor;
  private Resource catalog;
  private boolean showOnly = false; // if true, just show dirs and exit

  LoggerFactory loggerFactory;

  private class Server {
    String name;
    HTTPSession session;

    private Server(String name, HTTPSession session) {
      this.name = name;
      this.session = session;
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
    // TODO
  }

  // spring beaned
  public void setExecutor(ExecutorService executor) {
    this.executor = executor;
  }

  public void setCatalog(Resource catalog) {
    this.catalog = catalog;
  }

  public void setServerNames(String[] serverNames) throws HTTPException {
    if (serverNames == null) {
      servers = new ArrayList<Server>(); // empty list
      return;
    }

    servers = new ArrayList<Server>(serverNames.length);
    for (String name : serverNames) {
      HTTPSession session = new HTTPSession(name);
      session.setCredentialsProvider(new CredentialsProvider() {
        public Credentials getCredentials(AuthScheme authScheme, String s, int i, boolean b) throws CredentialsNotAvailableException {
          //System.out.printf("getCredentials called %s %s%n", user, pass);
          return new UsernamePasswordCredentials(user, pass);
        }
      });
      session.setUserAgent("tdmRunner");
      servers.add(new Server(name, session));
    }
  }

  AliasHandler aliasHandler;
  public void setPathAliasReplacements(List<PathAliasReplacement> aliasExpanders) {
    aliasHandler = new AliasHandler(aliasExpanders);
  }

  boolean init() {
    System.setProperty("tds.log.dir", contentTdmDir.toString());

    if (!Files.exists(threddsConfig)) {
      log.error("config file {} does not exist, set -Dtds.content.root.path=<dir>", threddsConfig);
      System.out.printf("threddsConfig does not exist=%s%n", threddsConfig);
      return false;
    }
    ThreddsConfigReader reader = new ThreddsConfigReader(threddsConfig.toString(), log);

   // LOOK following has been duplicated from tds cdmInit

    // 4.3.17
    long maxFileSize = reader.getBytes("FeatureCollection.RollingFileAppender.MaxFileSize", 1000 * 1000);
    int maxBackupIndex = reader.getInt("FeatureCollection.RollingFileAppender.MaxBackups", 10);
    String level = reader.get("FeatureCollection.RollingFileAppender.Level", "INFO");
    loggerFactory = new LoggerFactorySpecial(maxFileSize, maxBackupIndex, level);

    /* 4.3.15: grib index file placement, using DiskCache2  */
    String gribIndexDir = reader.get("GribIndex.dir", new File(contentThreddsDir.toString(), "thredds/cache/grib/").getPath());
    Boolean gribIndexAlwaysUse = reader.getBoolean("GribIndex.alwaysUse", false);
    Boolean gribIndexNeverUse = reader.getBoolean("GribIndex.neverUse", true);
    String gribIndexPolicy = reader.get("GribIndex.policy", null);
    DiskCache2 gribCache = new DiskCache2(gribIndexDir, false, -1, -1);
    gribCache.setPolicy(gribIndexPolicy);
    gribCache.setAlwaysUseCache(gribIndexAlwaysUse);
    gribCache.setNeverUseCache(gribIndexNeverUse);
    GribCollection.setDiskCache2(gribCache);

    return true;
  }

  void start() throws IOException {
     System.out.printf("Tdm startup at %s%n", new Date());

     //CatalogConfigReader reader = new CatalogConfigReader(catalog, aliasExpanders);
     CatalogConfigReader reader = new CatalogConfigReader(catalog, aliasHandler);
     List<FeatureCollectionConfig> fcList = reader.getFcList();

     if (showOnly) {
       List<String> result = new ArrayList<>();
       for (FeatureCollectionConfig config : fcList) {
         result.add(config.name);
       }
       Collections.sort(result);

       System.out.printf("Feature Collections:%n");
       for (String dir : result)
         System.out.printf(" %s%n", dir);
       return;
     }

     for (FeatureCollectionConfig config : fcList) {
       if (config.type != FeatureCollectionType.GRIB2) continue;
       System.out.printf("FeatureCollection %s scheduled %n", config.name);
       /* CollectionManager dcm = fc.getDatasetCollectionManager(); // LOOK this will fail
       if (config != null && config.gribConfig != null && config.gribConfig.gdsHash != null)
         dcm.putAuxInfo("gdsHash", config.gribConfig.gdsHash); // sneak in extra config info  */

       Logger logger = loggerFactory.getLogger("fc." + config.name); // seperate log file for each feature collection (!!)
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
      return config.name;
    }

    @Override
    public void sendEvent(CollectionUpdateType event) {
      if (!inUse.compareAndSet(false, true)) return; // if already working, skip another execution
      executor.execute(new IndexTask(config, this, logger));
    }
  }

  // Task causes a new index to be written - we know collection has changed, dont test again
  // run these through the executor so we can control how many we can do at once.
  // thread pool set in spring config file
  private class IndexTask implements Runnable {
    String name;
    FeatureCollectionConfig config;
    //MCollection dcm;
    Listener liz;
    org.slf4j.Logger logger;

    private IndexTask(FeatureCollectionConfig config, Listener liz, org.slf4j.Logger logger) {
      this.name = config.name;
      this.config = config;
      this.liz = liz;
      this.logger = logger;
    }

    @Override
    public void run() {
      try {
        Formatter errlog = new Formatter();

        boolean changed = GribCdmIndex2.rewriteFilePartition(config, CollectionUpdateType.test, CollectionUpdateType.test, logger);

        // delete any files first
        //if (config.tdmConfig.deleteAfter != null) {
        //  doManage(config.tdmConfig.deleteAfter);
        //}

        if (changed)
          System.out.printf("%s %s changed%n", CalendarDate.present(), config.name);

        if (changed && config.tdmConfig.triggerOk && sendTriggers) { // send a trigger if enabled
          String path = "thredds/admin/collection/trigger?nocheck&collection=" + name;
          sendTriggers(path, errlog);
        }
        errlog.format("**** TimePartitionBuilder.factory complete %s%n", name);
        logger.debug("\n------------------------\n{}\n------------------------\n", errlog.toString());

      } catch (Throwable e) {
        logger.error("TimePartitionBuilder.factory " + name, e);

      } finally {
        // tell liz that task is done
        if (!liz.inUse.getAndSet(false))
          logger.warn("Listener InUse should have been set");
      }

      /* System.out.printf("OpenFiles:%n");
      for (String s : RandomAccessFile.getOpenFiles())
        System.out.printf("%s%n", s);*/
    }

    private void sendTriggers(String path, Formatter f) {
      for (Server server : servers) {
        String url = server.name + path;
        logger.debug("send trigger to {}", url);
        HTTPMethod m = null;
        try {
          m = HTTPMethod.Get(server.session, url);
          int status = m.execute();
          String statuss = m.getResponseAsString();
          f.format(" trigger %s status = %d (%s)%n", url, status, statuss);

        } catch (HTTPException e) {
          Throwable cause = e.getCause();
          if (cause instanceof ConnectException) {
            logger.info("server {} not running", server.name);
          } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
            e.printStackTrace(new PrintStream(bos));
            f.format("%s == %s", url, bos.toString());
            e.printStackTrace();
          }

        } finally {
          if (m != null) m.close();
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
    ApplicationContext springContext = new FileSystemXmlApplicationContext("classpath:resources/application-config.xml");
    Tdm driver = (Tdm) springContext.getBean("testDriver2");

    Map<String, String> aliases = (Map<String, String> ) springContext.getBean("dataRootLocationAliasExpanders");
    List<PathAliasReplacement> aliasExpanders = PathAliasReplacementImpl.makePathAliasReplacements(aliases);
    driver.setPathAliasReplacements( aliasExpanders);

    //RandomAccessFile.setDebugLeaks(true);
    HTTPSession.setGlobalUserAgent("TDM v4.5");
    // GribCollection.getDiskCache2().setNeverUseCache(true);
    String logLevel = "INFO";
    //String contentDir;

    for (int i = 0; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("-help")) {
        System.out.printf("usage: <Java> <Java_OPTS> -Dtds.content.root.path=<contentDir> [-catalog <cat>] [-tds <tdsServer>] [-cred <user:passwd>] [-showOnly] [-log level]%n");
        System.out.printf("example: /opt/jdk/bin/java -d64 -Xmx8g -server -jar tdm-4.3.jar -Dtds.content.root.path=/my/content -cred user:passwd%n");
        System.exit(0);
      }

      if (args[i].equalsIgnoreCase("-contentDir")) {
        driver.setContentDir(args[i+1]);
        i++;
      }

      else if (args[i].equalsIgnoreCase("-catalog")) {
        Resource cat = new FileSystemResource(args[i + 1]);
        driver.setCatalog(cat);
      }

      else if (args[i].equalsIgnoreCase("-tds")) {
        String tds = args[i + 1];
        if (tds.equalsIgnoreCase("none")) {
          driver.setServerNames(null);
          driver.sendTriggers = false;

        } else {
          String[] tdss = tds.split(","); // comma separated
          driver.setServerNames( tdss);
        }
      }

      else if (args[i].equalsIgnoreCase("-cred")) {  // LOOK could be user:password@server, and we parse the user:password
        String cred = args[i + 1];
        String[] split = cred.split(":");
        driver.user = split[0];
        driver.pass = split[1];
        driver.sendTriggers = true;
      }

      else if (args[i].equalsIgnoreCase("-nthreads")) {
        int n = Integer.parseInt(args[i + 1]);
        driver.setNThreads(n);
      }

      else if (args[i].equalsIgnoreCase("-showOnly")) {
        driver.setShowOnly(true);
      }

      else if (args[i].equalsIgnoreCase("-log")) {
        logLevel = args[i + 1];
      }
    }

    CollectionUpdater.INSTANCE.setTdm(true);

    String contentDir = System.getProperty("tds.content.root.path");
    if (contentDir == null)  contentDir = "../content";
    driver.setContentDir(contentDir);

    if (driver.init()) {
      driver.start();
    } else {
      System.out.printf("EXIT DUE TO ERRORS%n");
    }

  }

}

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
 *
 */
package thredds.tdm;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.util.ThreddsConfigReader;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.util.AliasTranslator;
import ucar.nc2.util.Counters;
import ucar.nc2.util.DiskCache2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;

/**
 * GribCollection Building - make summary of the feature collections.
 * calls GCpass1 and writes the gbx files if needed.
 * Does not build ncx files, so no large memory is needed.
 * So can run with many threads.
 *
 * @author caron
 * @since 12/20/2015.
 */
public class GCsummary {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GCsummary.class);
  private static final boolean debug = false;
  private static final boolean debugOpenFiles = false;

  private Path contentDir;
  private Path contentThreddsDir;
  private Path contentTdmDir;
  private Path threddsConfig;

  private java.util.concurrent.ExecutorService executor;
  private Resource catalog;
  private boolean showOnly = false; // if true, just show dirs and exit

  List<Resource> catalogRoots = new ArrayList<>();

  Map<String, GCsummaryTask> fcMap = new TreeMap<>();

  public void setContentDir(String contentDir) throws IOException {
    System.out.printf("contentDir=%s%n", contentDir);
    this.contentDir = Paths.get(contentDir);
    this.contentThreddsDir = Paths.get(contentDir, "thredds");
    this.threddsConfig = Paths.get(contentDir, "thredds", "threddsConfig.xml");
    this.contentTdmDir = Paths.get(contentDir, "tdm");
    this.catalog = new FileSystemResource(contentThreddsDir.toString() + "/catalog.xml");
  }

  public void setShowOnly(boolean showOnly) {
    this.showOnly = showOnly;
  }

  public void setNThreads(int n) {
    executor = Executors.newFixedThreadPool(n);
    log.info(" TDM nthreads= {}", n);
  }

  // spring beaned
  public void setExecutor(ExecutorService executor) {
    this.executor = executor;
  }

  public void setCatalog(String catalog) throws IOException {
    this.catalog = new FileSystemResource(contentThreddsDir.toString() + "/" + catalog);
    System.out.printf("use catalog=%s%n", this.catalog.getFile().getPath());

  }

  ////////////////////////////////////////////////////////////////////
  boolean init() {
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

    // LOOK check TdsInit
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
    System.out.printf("GCsummary startup at %s%n", new Date());

    List<FeatureCollectionConfig> fcList = new ArrayList<>();
    CatalogConfigReader reader = new CatalogConfigReader(contentThreddsDir, catalog);
    fcList.addAll(reader.getFcList());

    // do the catalogRoots
    for (Resource catr : catalogRoots) {
      CatalogConfigReader r = new CatalogConfigReader(contentThreddsDir, catr);
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

      executor.shutdown();
      return;
    }

    for (FeatureCollectionConfig config : fcList) {
      if (config.type != FeatureCollectionType.GRIB1 && config.type != FeatureCollectionType.GRIB2) continue;
      System.out.printf(" FeatureCollection scheduled %s == %s %n", config.collectionName, config.spec);

      // schedule execution of the GCsummary task
      GCsummaryTask task = new GCsummaryTask(config);
      fcMap.put(config.getCollectionName(), task);
      executor.execute(task);
    }
  }

  public void finish() {
    // no more tasks will be submitted, wait until all are done
    executor.shutdown();

    try {
      executor.awaitTermination(1, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      System.out.printf("Interupted%n");
    }

    System.out.printf("Finished All tasks%n");
    GCpass1.Accum all = new GCpass1.Accum();
    long tookAll = 0;

    Formatter f = new Formatter();
    f.format("%40s,  type, ptype,    took,  nfiles, nrecords,  idx(MB), data(GB), data/idx, bytes/rec, variables, runtimes, gds %n", "Collection");
    for (GCsummaryTask task : fcMap.values()) {
      f.format("%40s, %5s, %5s, %8d,", task.config.collectionName, task.config.type, task.config.ptype, task.took);
      GCpass1.Accum acc = task.pass1.accumAll;
      f.format("%8d, %8d, %8.3f, %8.3f, ", acc.nfiles, acc.nrecords, acc.indexSize, acc.fileSize / 1000);
      f.format("%8.3f, %8.0f,", acc.fileSize / acc.indexSize, acc.indexSize * 1000 * 1000 / acc.nrecords);

      Counters counters = task.pass1.countersAll;
      f.format("%8d,", counters.get("variable").getUnique());
      f.format("%8d,", counters.get("referenceDate").getUnique());
      f.format("%8d,", counters.get("gds").getUnique());
      f.format("%n");

      all.add(acc);
      tookAll += task.took;
    }

    f.format("%n");
    f.format("%40s, %5s, %5s, %8d,", "total", "", "", tookAll);
    f.format("%8d, %8d, %8.3f, %8.3f, ", all.nfiles, all.nrecords, all.indexSize, all.fileSize / 1000);
    f.format("%8.3f, %8.0f", all.fileSize / all.indexSize, all.indexSize * 1000 * 1000 / all.nrecords);
    f.format("%n");

    System.out.printf("%s%n", f);

    try (FileOutputStream fileOut = new FileOutputStream("GCsummary.csv")) {
      fileOut.write(f.toString().getBytes());
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  private class GCsummaryTask implements Runnable {
    String name;
    FeatureCollectionConfig config;
    GCpass1 pass1;
    long took;

    private GCsummaryTask(FeatureCollectionConfig config) {
      this.name = config.collectionName;
      this.config = config;
    }

    @Override
    public void run() {
      try {
        long start = System.currentTimeMillis();

        String fileOutName = config.collectionName + ".GCsummary.txt";
        try (FileOutputStream fileOut = new FileOutputStream(fileOutName)) {
          System.out.printf("GCsummaryTask %s started write to %s%n", name, fileOutName);
          Formatter f = new Formatter(fileOut);
          this.pass1 = new GCpass1(config, f);
          pass1.scanAndReport();
          f.flush();
          fileOut.flush();
        }
        this.took = (System.currentTimeMillis() - start) / 1000; // secs
        System.out.printf(" GCsummaryTask %s finished in %d secs%n", name, took);

      } catch (Throwable e) {
        e.printStackTrace();
      }
    }

  }


  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /*
        // /opt/jdk/bin/java -d64 -Xmx3g -jar -Dtds.content.root.path=/opt/tds-dev/content tdm-4.5.jar -cred tdm:trigger -tds "http://thredds-dev.unidata.ucar.edu/"
           System.out.printf("usage: <Java> <Java_OPTS> -Dtds.content.root.path=<contentDir> [-catalog <cat>] [-tds <tdsServer>] [-cred <user:passwd>] [-showOnly] [-forceOnStartup]%n");
            System.out.printf("example: /opt/jdk/bin/java -Xmx3g -Dtds.content.root.path=/my/content -jar tdm-4.5.jar -tds http://thredds-dev.unidata.ucar.edu/%n");
 */

  private static class CommandLine {
    @Parameter(names = {"-catalog"}, description = "specific catalog", required = false)
    public String catalog;

    @Parameter(names = {"-nthreads"}, description = "number of threads", required = false)
    public int nthreads;

    @Parameter(names = {"-showOnly"}, description = "show collections and exit", required = false)
    public boolean showOnly;

    @Parameter(names = {"-h", "--help"}, description = "Display this help and exit", help = true)
    public boolean help = false;

    private final JCommander jc;

    public CommandLine(String progName, String[] args) throws ParameterException {
      this.jc = new JCommander(this, args);  // Parses args and uses them to initialize *this*.
      jc.setProgramName(progName);           // Displayed in the usage information.
    }

    public void printUsage() {
      jc.usage();
    }

  }

  public static void main(String args[]) throws IOException, InterruptedException {
    try (FileSystemXmlApplicationContext springContext = new FileSystemXmlApplicationContext("classpath:resources/application-config.xml")) {
      GCsummary app = (GCsummary) springContext.getBean("GCsummary");

      Map<String, String> aliases = (Map<String, String>) springContext.getBean("dataRootLocationAliasExpanders");
      for (Map.Entry<String, String> entry : aliases.entrySet())
        AliasTranslator.addAlias(entry.getKey(), entry.getValue());

      String progName = GCsummary.class.getName();

      try {
        CommandLine cmdLine = new CommandLine(progName, args);
        if (cmdLine.help) {
          cmdLine.printUsage();
          return;
        }

        String contentDir = System.getProperty("tds.content.root.path");
        if (contentDir == null) contentDir = "../content";
        app.setContentDir(contentDir);

        if (cmdLine.catalog != null)
          app.setCatalog(cmdLine.catalog);

        if (cmdLine.nthreads != 0)
          app.setNThreads(cmdLine.nthreads);

        if (cmdLine.showOnly)
          app.setShowOnly(true);

        if (app.init()) {
          app.start();
          app.finish();
          System.exit(0);
        } else {
          System.out.printf("%nEXIT DUE TO ERRORS");
        }

      } catch (ParameterException e) {
        System.err.println(e.getMessage());
        System.err.printf("Try \"%s --help\" for more information.%n", progName);
      }
    }
  }
}


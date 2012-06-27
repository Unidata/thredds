/*
 * Copyright (c) 1998 - 2011. University Corporation for Atmospheric Research/Unidata
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

package thredds.tdm;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.log4j.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import thredds.catalog.DataFormatType;
import thredds.catalog.InvDatasetFeatureCollection;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.inventory.*;

import ucar.nc2.grib.GribCollection;
import ucar.nc2.grib.TimePartition;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.units.TimeDuration;
import ucar.nc2.util.net.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thredds Data Manager.
 * <p/>
 * run: java -Xmx4g -server -jar tdm-4.3.jar
 * if you need to muck with that, use:
 * java -Xmx4g -server -jar tdm-4.3.jar -catalog <muck.xml>
 * where you can start with tdm-4.3.jar/resources/indexNomads.xml
 * or modify resources/applicatin-config.xml to set the catalog
 *
 * @author caron
 * @since 4/26/11
 */
public class TdmRunner {
  //static private final Logger logger = org.slf4j.LoggerFactory.getLogger(TdmRunner.class);
  static private boolean seperateFiles = true;
  static private String serverName = "http://localhost:8080/"; // set in the spring config file
  static private HTTPSession session;

  private java.util.concurrent.ExecutorService executor;
  private Resource catalog;
  private boolean indexOnly = false; // if true, just use existing .ncx
  private boolean showOnly = false; // if true, just show dirs and exit

  public void setShowOnly(boolean showOnly) {
    this.showOnly = showOnly;
  }

  // spring beaned
  public void setExecutor(ExecutorService executor) {
    this.executor = executor;
  }

  public void setCatalog(Resource catalog) {
    this.catalog = catalog;
  }

  public void setTdsServer(String serverName) {
    this.serverName = serverName;
  }

  // Task causes a new index to be written - we know collection has changed, dont test again
  // run these through the executor so we can control how many we can do at once.
  // thread pool set in spring config file
  private class IndexTask implements Runnable {
    String name;
    InvDatasetFeatureCollection fc;
    CollectionManager dcm;
    Listener liz;
    org.slf4j.Logger logger;

    private IndexTask(InvDatasetFeatureCollection fc, CollectionManager dcm, Listener liz, org.slf4j.Logger logger) {
      this.name = fc.getName();
      this.fc = fc;
      this.dcm = dcm;
      this.liz = liz;
      this.logger = logger;
    }

    @Override
    public void run() {
      try {
        FeatureCollectionConfig config = fc.getConfig();
        thredds.catalog.DataFormatType format = fc.getDataFormatType();

        // delete any files first
        //if (config.tdmConfig.deleteAfter != null) {
        //  doManage(config.tdmConfig.deleteAfter);
        //}

        if (dcm instanceof TimePartitionCollection) {
          TimePartitionCollection tpc = (TimePartitionCollection) dcm;
          logger.debug("**** running TimePartitionBuilder.factory {} thread {}", name, Thread.currentThread().hashCode());
          Formatter f = new Formatter();
          try {
            TimePartition tp = TimePartition.factory(format == DataFormatType.GRIB1, tpc, CollectionManager.Force.always, f); // "we know collection has changed, dont test again" ??? LOOK
            tp.close();
            if (config.tdmConfig.triggerOk && sendTriggers) { // send a trigger if enabled
              String url = serverName + "thredds/admin/collection/trigger?nocheck&collection=" + fc.getName();
              int status = sendTrigger(url, f);
              f.format(" trigger %s status = %d%n", url, status);
            }
            f.format("**** TimePartitionBuilder.factory complete %s%n", name);
          } catch (Throwable e) {
            logger.error("TimePartitionBuilder.factory " + name, e);
          }
          logger.debug("\n------------------------\n{}\n------------------------\n", f.toString());

        } else {
          logger.debug("**** running GribCollectionBuilder.factory {} Thread {}", name, Thread.currentThread().hashCode());
          Formatter f = new Formatter();
          try {
            GribCollection gc = GribCollection.factory(format == DataFormatType.GRIB1, dcm, CollectionManager.Force.always, f);
            gc.close();
            if (config.tdmConfig.triggerOk && sendTriggers) { // LOOK is there any point if you dont have trigger = true ?
              String url = serverName + "thredds/admin/collection/trigger?nocheck&collection=" + fc.getName();
              int status = sendTrigger(url, f);
              f.format(" trigger %s status = %d%n", url, status);
            }
            f.format("**** GribCollectionBuilder.factory complete %s%n", name);

          } catch (Throwable e) {
            logger.error("GribCollectionBuilder.factory " + name, e);
          }
          logger.debug("\n------------------------\n{}\n------------------------\n", f.toString());
        }

      } finally {
        // tell liz that task is done
        if (!liz.inUse.getAndSet(false))
          logger.warn("Listener InUse should have been set");
      }

      /* System.out.printf("OpenFiles:%n");
      for (String s : RandomAccessFile.getOpenFiles())
        System.out.printf("%s%n", s);*/
    }

    private int sendTrigger(String url, Formatter f) {
      logger.debug("send trigger to {}", url);
      HTTPMethod m = null;
      try {
        m = HTTPMethod.Get(session, url);
        int status = m.execute();
        String s = m.getResponseAsString();
        f.format("%s == %s", url, s);
        System.out.printf("Trigger response = %s%n", s);
        return status;

      } catch (HTTPException e) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
        e.printStackTrace(new PrintStream(bos));
        f.format("%s == %s", url, bos.toString());
        e.printStackTrace();
        return -1;

      } finally {
        if (m != null) m.close();
      }

    }

    private void doManage(String deleteAfterS) {
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

      for (MFile mfile : dcm.getFiles()) {
        CalendarDate cd = dcm.extractRunDate(mfile);
        int n = period.subtract(cd, now);
        if (cd.isBefore(last)) {
          logger.info("delete={} age = {}", mfile.getPath(), n + " " + unit);
        } else {
          logger.debug("dont delete={} age = {}", mfile.getPath(), n + " " + unit);
        }
      }

    }

  }

  // these objects listen for schedule events from quartz and dcm.
  // one listener for each dcm.
  private class Listener implements CollectionManager.TriggerListener {
    InvDatasetFeatureCollection fc;
    CollectionManager dcm;
    AtomicBoolean inUse = new AtomicBoolean(false);
    org.slf4j.Logger logger;

    private Listener(InvDatasetFeatureCollection fc, CollectionManager dcm) {
      this.fc = fc;
      this.dcm = dcm;

      if (seperateFiles) {
        try {
          //create logger in log4j
          Layout layout = new PatternLayout("%d{yyyy-MM-dd'T'HH:mm:ss.SSS Z} %-5p - %c - %m%n");
          String loggerName = fc.getName() + ".log";
          FileAppender app = new FileAppender(layout, loggerName);
          org.apache.log4j.Logger log4j = LogManager.getLogger(fc.getName());
          log4j.addAppender(app);
          log4j.setLevel(Level.DEBUG);

          // get wrapper in slf4j
          logger = org.slf4j.LoggerFactory.getLogger(fc.getName());
        } catch (IOException ioe) {

        }

      } else {
        logger = org.slf4j.LoggerFactory.getLogger(getClass());
      }
    }

    @Override
    public void handleCollectionEvent(CollectionManager.TriggerEvent event) {
      if (event.getType() != CollectionManager.TriggerType.update) return;

      // make sure that each collection is only being indexed by one thread at a time
      if (inUse.get()) {
        logger.debug("** Update already in progress for {} {}", fc.getName(), event.getType());
        return;
      }
      if (!inUse.compareAndSet(false, true)) return;

      executor.execute(new IndexTask(fc, dcm, this, logger));
    }

    /* private boolean needsUpdate(long indexLastModified) {
      if (dcm instanceof TimePartitionCollection)
        return needsPartitionUpdate((TimePartitionCollection) dcm, indexLastModified);
      else
        return needsUpdate(dcm, indexLastModified);
    }

    private boolean needsPartitionUpdate(TimePartitionCollection tpc, long indexLastModified) {
      try {
        for (CollectionManager cm : tpc.makePartitions()) {
          if (needsUpdate(cm, indexLastModified)) return true;
        }
      } catch (IOException ioe) {
        logger.warn("** needsPartitionUpdate ", ioe);
      }
      return false;
    }

    private boolean needsUpdate(CollectionManager cm, long since) {
      int count = 0;
      for (MFile f : cm.getFiles()) {
        if (wasUpdated(f, since)) return true;
        count++;
      }
      if (count == 0) return true; // not scanned yet
      // LOOK return !dcm.directoryWasModifiedAfter(lastDate)// LOOK - what if files were deleted ?
      return false;
    }

    // could make this into a strategy thats passed into CollectionManager
    private boolean wasUpdated(MFile mfile, long since) {
      File gribFile = new File(mfile.getPath());
      File idxFile = DiskCache.getFile(mfile.getPath() + GribIndex.IDX_EXT, false);
      if (!idxFile.exists()) return true;
      if (idxFile.lastModified() < gribFile.lastModified()) return true;
      if (since < idxFile.lastModified()) return true;
      return false;
    } */

  }

  void start() throws IOException {
    System.out.printf("Tdm startup at %s%n", new Date());
    CatalogReader reader = new CatalogReader(catalog);
    List<InvDatasetFeatureCollection> fcList = reader.getFcList();

    if (showOnly) {
      List<String> result = new ArrayList<String>();
      for (InvDatasetFeatureCollection fc : fcList) {
        CollectionManager dcm = fc.getDatasetCollectionManager();
        result.add(dcm.getRoot());
      }
      Collections.sort(result);

      System.out.printf("Directories:%n");
      for (String dir : result)
        System.out.printf(" %s%n", dir);
      return;
    }

    for (InvDatasetFeatureCollection fc : fcList) {
      CollectionManager dcm = fc.getDatasetCollectionManager();
      FeatureCollectionConfig fcConfig = fc.getConfig();
      if (fcConfig != null && fcConfig.gribConfig != null && fcConfig.gribConfig.gdsHash != null)
        dcm.putAuxInfo("gdsHash", fcConfig.gribConfig.gdsHash); // sneak in extra config info

      dcm.addEventListener(new Listener(fc, dcm)); // now wired for events
      dcm.removeEventListener(fc); // not needed
      // CollectionUpdater.INSTANCE.scheduleTasks( CollectionUpdater.FROM.tdm, fc.getConfig(), dcm); // already done in finish() method
    }

    // show whats up
    Formatter f = new Formatter();
    f.format("Feature Collections found:%n");
    for (InvDatasetFeatureCollection fc : fcList) {
      CollectionManager dcm = fc.getDatasetCollectionManager();
      f.format("  %s == %s%n%s%n%n", fc, fc.getClass().getName(), dcm);
    }
    System.out.printf("%s%n", f.toString());
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  private static String user, pass;
  private static boolean sendTriggers;

  public static void main(String args[]) throws IOException, InterruptedException {
    ApplicationContext springContext = new FileSystemXmlApplicationContext("classpath:resources/application-config.xml");
    TdmRunner driver = (TdmRunner) springContext.getBean("testDriver");
    //RandomAccessFile.setDebugLeaks(true);


    for (int i = 0; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("-help")) {
        System.out.printf("usage: TdmRunner [-catalog <cat>] [-server <tdsServer>] [-cred <user:passwd>] [-showDirs] %n");
        System.out.printf("example: TdmRunner -catalog classpath:/resources/indexNomads.xml -server http://localhost:8080/ -cred user:password %n");
        System.exit(0);
      }
      if (args[i].equalsIgnoreCase("-showDirs"))
        driver.setShowOnly(true);
      if (args[i].equalsIgnoreCase("-server"))
        driver.setTdsServer(args[i + 1]);
      if (args[i].equalsIgnoreCase("-cred")) {
        String cred = args[i + 1];
        String[] split = cred.split(":");
        user = split[0];
        pass = split[1];
        sendTriggers = true;
      }
      if (args[i].equalsIgnoreCase("-catalog")) {
        Resource cat = new FileSystemResource(args[i + 1]);
        driver.setCatalog(cat);
      }
    }

    if (sendTriggers) {
      session = new HTTPSession(serverName);
      session.setCredentialsProvider(new CredentialsProvider() {
        public Credentials getCredentials(AuthScheme authScheme, String s, int i, boolean b) throws CredentialsNotAvailableException {
          //System.out.printf("getCredentials called %s %s%n", user, pass);
          return new UsernamePasswordCredentials(user, pass);
        }
      });
      session.setUserAgent("tdmRunner");
      HTTPSession.setGlobalUserAgent("TDM v4.3");
    }

    CollectionUpdater.INSTANCE.setTdm(true);

    driver.start();
  }


}

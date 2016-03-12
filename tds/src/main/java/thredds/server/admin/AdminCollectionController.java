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

package thredds.server.admin;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.coverity.security.Escape;
import com.google.common.escape.Escaper;
import com.google.common.eventbus.EventBus;
import com.google.common.net.UrlEscapers;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import thredds.core.DataRootManager;
import thredds.core.DatasetManager;
import thredds.featurecollection.CollectionUpdater;
import thredds.featurecollection.InvDatasetFeatureCollection;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.*;
import thredds.monitor.FmrcCacheMonitorImpl;
import thredds.server.catalog.FeatureCollectionRef;
import thredds.server.config.TdsContext;
import thredds.servlet.ServletUtil;
import thredds.util.ContentType;
import thredds.util.TdsPathUtils;
import ucar.nc2.constants.CDM;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.util.IO;
import ucar.unidata.util.StringUtil2;

/**
 * Allow external triggers for rereading Feature collections
 *
 * @author caron
 * @since May 4, 2010
 */
@Controller
@RequestMapping(value = {"/admin/collection"})
public class AdminCollectionController {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminCollectionController.class);

  private static final String PATH = "/admin/collection";
  private static final String COLLECTION = "collection";
  private static final String SHOW_COLLECTION = "showCollection";
  private static final String SHOW = "showStatus";
  private static final String SHOW_CSV = "showStatus.csv";
  private static final String DOWNLOAD = "download";
  private static final String DOWNLOAD_ALL = "downloadAll";
  private static final String TRIGGER = "trigger";

  @Autowired
  DebugCommands debugCommands;

  @Autowired
  DataRootManager dataRootManager;

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private DatasetManager datasetManager;

  @Autowired
  @Qualifier("fcTriggerEventBus")
  private EventBus eventBus;

  @Autowired
  CollectionUpdater collectionUpdater;

  @PostConstruct
  public void afterPropertiesSet() {
    Escaper urlParamEscaper = UrlEscapers.urlFormParameterEscaper();

    DebugCommands.Category debugHandler = debugCommands.findCategory("Collections");
    DebugCommands.Action act;

    act = new DebugCommands.Action("showCollection", "Show Collections") {
      public void doAction(DebugCommands.Event e) {
        // get sorted list of collections
        List<FeatureCollectionRef> fcList = dataRootManager.getFeatureCollections();
        Collections.sort(fcList, (o1, o2) -> o1.getCollectionName().compareTo(o2.getCollectionName()));

        for (FeatureCollectionRef fc : fcList) {
          String uriParam = Escape.uriParam(fc.getCollectionName());
          String url = tdsContext.getContextPath() + PATH + "/"+SHOW_COLLECTION + "?" + COLLECTION + "=" + uriParam;
          e.pw.printf("<p/><a href='%s'>%s</a> (%s)%n", url, fc.getCollectionName(), fc.getName());
          FeatureCollectionConfig config = fc.getConfig();
          if (config != null)
            e.pw.printf("%s%n", config.spec);
        }

        String url = tdsContext.getContextPath() + PATH + "/" + SHOW;
        e.pw.printf("<p/><a href='%s'>Show All Collection Status</a>%n", url);

        url = tdsContext.getContextPath() + PATH + "/" + SHOW_CSV;
        e.pw.printf("<p/><a href='%s'>Collection Status CSV</a>%n", url);

        url = tdsContext.getContextPath() + PATH + "/" + DOWNLOAD_ALL;
        e.pw.printf("<p/><a href='%s'>Download All top-level collection indices</a>%n", url);
      }
    };
    debugHandler.addAction(act);

    act = new DebugCommands.Action("sched", "Show FeatureCollection update scheduler") {
      public void doAction(DebugCommands.Event e) {
        org.quartz.Scheduler scheduler = collectionUpdater.getScheduler();
        if (scheduler == null) return;

        try {
          e.pw.println(scheduler.getMetaData());

          List<String> groups = scheduler.getJobGroupNames();
          List<String> triggers = scheduler.getTriggerGroupNames();

          // enumerate each job group
          for (String group : scheduler.getJobGroupNames()) {
            e.pw.println("Group " + group);

            // enumerate each job in group
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.<JobKey>groupEquals(group))) {
              e.pw.println("  Job " + jobKey.getName());
              e.pw.println("    " + scheduler.getJobDetail(jobKey));
            }

            // enumerate each trigger in group
            for (TriggerKey triggerKey : scheduler.getTriggerKeys(GroupMatcher.<TriggerKey>groupEquals(group))) {
              e.pw.println("  Trigger " + triggerKey.getName());
              e.pw.println("    " + scheduler.getTrigger(triggerKey));
            }
          }
        } catch (Exception e1) {
          e.pw.println("Error on scheduler " + e1.getMessage());
          log.error("Error on scheduler " + e1.getMessage());
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugCommands.Action("showFmrcCache", "Show FMRC Cache") {
      public void doAction(DebugCommands.Event e) {
        e.pw.println("<p>cache location = " + monitor.getCacheLocation() + "<p>");
        String statUrl = tdsContext.getContextPath() + FMRC_PATH + "/" + STATISTICS;
        e.pw.println("<p/> <a href='" + statUrl + "'>Show Cache Statistics</a>");
        for (String name : monitor.getCachedCollections()) {
          String ename =  urlParamEscaper.escape(name);
          String url = tdsContext.getContextPath() + FMRC_PATH + "?" + COLLECTION + "=" + ename;
          e.pw.println("<p/> <a href='" + url + "'>" + Escape.html(name) + "</a>");
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugCommands.Action("syncFmrcCache", "Flush FMRC Cache to disk") {
      public void doAction(DebugCommands.Event e) {
        monitor.sync();
        e.pw.println("<p>bdb cache location = " + monitor.getCacheLocation() + "<p> flushed to disk");
      }
    };
    debugHandler.addAction(act);
  }

  @RequestMapping(value = "/"+SHOW_COLLECTION, method = RequestMethod.GET)
  protected ResponseEntity<String> showCollection(@RequestParam String collection) throws Exception {
    Formatter out = new Formatter();

    FeatureCollectionRef want = dataRootManager.findFeatureCollection(collection);

    HttpStatus status = HttpStatus.OK;
    if (want == null) {
      status = HttpStatus.NOT_FOUND;
      out.format("NOT FOUND");

    } else {
      out.format("<h3>Collection %s</h3>%n%n", Escape.html(collection));
      showFeatureCollection(out, want);

      String uriParam = Escape.uriParam(want.getCollectionName());
      String url = tdsContext.getContextPath() + PATH + "/"+TRIGGER+"?" + COLLECTION + "=" + uriParam + "&" + TRIGGER + "=" + CollectionUpdateType.nocheck;
      out.format("<p/><a href='%s'>Send trigger to %s</a>%n", url, Escape.html(want.getCollectionName()));

      String url2 = tdsContext.getContextPath() + PATH + "/"+DOWNLOAD+"?" + COLLECTION + "=" + uriParam;
      out.format("<p/><a href='%s'>Download index file for %s</a>%n", url2, Escape.html(want.getCollectionName()));
    }

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_HTML);
    return new ResponseEntity<>(out.toString(), responseHeaders, status);
  }

  @RequestMapping(value = {"/"+SHOW})
  protected ResponseEntity<String> showCollectionStatus() throws Exception {
    Formatter out = new Formatter();

    // get sorted list of collections
    List<FeatureCollectionRef> fcList = dataRootManager.getFeatureCollections();
    Collections.sort(fcList, (o1, o2) -> o1.getCollectionName().compareTo(o2.getCollectionName()));

    for (FeatureCollectionRef fc : fcList) {
      String uriParam = Escape.uriParam(fc.getCollectionName());
      String url = tdsContext.getContextPath() + PATH + "?" + COLLECTION + "=" + uriParam;
      out.format("<p/><a href='%s'>%s</a> (%s)%n", url, fc.getCollectionName(), fc.getName());
      InvDatasetFeatureCollection fcd = datasetManager.openFeatureCollection(fc);

      out.format("<pre>%s</pre>%n", fcd.showStatusShort("txt"));
    }

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_HTML);
    return new ResponseEntity<>(out.toString(), responseHeaders, HttpStatus.OK);
  }

  @RequestMapping(value = {"/"+SHOW_CSV})
  protected ResponseEntity<String> showCollectionStatusCsv() throws Exception {
    Formatter out = new Formatter();

    // get sorted list of collections
    List<FeatureCollectionRef> fcList = dataRootManager.getFeatureCollections();
    Collections.sort(fcList, (o1, o2) -> {
      int compareType = o1.getConfig().type.toString().compareTo(o1.getConfig().type.toString());
      if (compareType != 0) return compareType;
      return o1.getCollectionName().compareTo(o2.getCollectionName());
    });

    out.format("%s, %s, %s, %s, %s, %s, %s, %s, %s%n", "collection", "ed", "type", "group", "nrecords", "ndups", "%", "nmiss", "%");
    for (FeatureCollectionRef fc : fcList) {
      if (fc.getConfig().type != FeatureCollectionType.GRIB1 && fc.getConfig().type != FeatureCollectionType.GRIB2) continue;
      InvDatasetFeatureCollection fcd = datasetManager.openFeatureCollection(fc);
      out.format("%s", fcd.showStatusShort("csv"));
    }

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_PLAIN);
    return new ResponseEntity<>(out.toString(), responseHeaders, HttpStatus.OK);
  }

  @RequestMapping(value = {"/trigger"})  // LOOK should require collection and trigger type params
  protected ResponseEntity<String> triggerFeatureCollection(HttpServletRequest req, HttpServletResponse res) throws Exception {
    Formatter out = new Formatter();

    CollectionUpdateType triggerType = null;
    String triggerTypeS = req.getParameter(TRIGGER);
    try {
      triggerType = CollectionUpdateType.valueOf(triggerTypeS);
    } catch (Throwable t) {
      // noop
    }

    if (triggerType == null) {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      out.format(" TRIGGER Type '%s' not legal%n", Escape.html(triggerTypeS));
      return null;
    }

    String collectName = StringUtil2.unescape(req.getParameter(COLLECTION)); // this is the collection name
    FeatureCollectionRef want = dataRootManager.findFeatureCollection(collectName);

    if (want == null) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      out.format("NOT FOUND");

    } else {
      out.format("<h3>Collection %s</h3>%n", Escape.html(collectName));

      if (!want.getConfig().isTrigggerOk()) {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        out.format(" TRIGGER NOT ENABLED%n");

      } else {
        eventBus.post(new CollectionUpdateEvent(triggerType, collectName, "trigger"));
        // CollectionUpdater.INSTANCE.triggerUpdate(collectName, triggerType);
        out.format(" TRIGGER SENT%n");
      }
    }

    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.setContentType(MediaType.TEXT_HTML);
    return new ResponseEntity<>(out.toString(), responseHeaders, HttpStatus.OK);
  }

  @RequestMapping(value = {"/"+DOWNLOAD_ALL})
  protected void downloadAll(HttpServletRequest req, HttpServletResponse res) throws IOException {
    File tempFile = File.createTempFile("CollectionIndex", ".zip");
    try (FileOutputStream fos = new FileOutputStream(tempFile.getPath())) {
      ZipOutputStream zout = new ZipOutputStream(fos);
      for (FeatureCollectionRef fc : dataRootManager.getFeatureCollections()) {
        File idxFile = GribCdmIndex.getTopIndexFileFromConfig(fc.getConfig());
        if (idxFile == null) continue;
        ZipEntry entry = new ZipEntry(idxFile.getName());
        zout.putNextEntry(entry);
        IO.copyFile(idxFile.getPath(), zout);
        zout.closeEntry();
      }
      zout.close();
      fos.close();
    }

    ServletUtil.returnFile(req, res, tempFile, ContentType.binary.toString());
    // tempFile.delete();
  }

  @RequestMapping(value = {"/"+DOWNLOAD})
  protected ResponseEntity<String> downloadIndex(HttpServletRequest req, HttpServletResponse res) throws Exception {
    String collectName = StringUtil2.unescape(req.getParameter(COLLECTION)); // this is the collection name
    FeatureCollectionRef want = dataRootManager.findFeatureCollection(collectName);

    if (want == null) {
      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.setContentType(MediaType.TEXT_PLAIN);
      return new ResponseEntity<>(Escape.html(collectName) + " NOT FOUND", responseHeaders, HttpStatus.NOT_FOUND);
    }

    File idxFile = GribCdmIndex.getTopIndexFileFromConfig(want.getConfig());
    if (idxFile == null) {
      HttpHeaders responseHeaders = new HttpHeaders();
      responseHeaders.setContentType(MediaType.TEXT_PLAIN);
      return new ResponseEntity<>(Escape.html(collectName) + " NOT FOUND", responseHeaders, HttpStatus.NOT_FOUND);
    }

    ServletUtil.returnFile(req, res, idxFile, ContentType.binary.toString());
    return null;
  }

  private void showFeatureCollection(Formatter out, FeatureCollectionRef fc) throws IOException {
    FeatureCollectionConfig config = fc.getConfig(); // LOOK
    if (config != null) {
      Formatter f = new Formatter();
      config.show(f);
      out.format("%n<pre>%s%n</pre>", f.toString());
    }

    InvDatasetFeatureCollection fcd = datasetManager.openFeatureCollection(fc);
    out.format("%n<pre>%n");
    fcd.showStatus(out);
    out.format("%n</pre>%n");
  }

  /////////////////////////////////////////////////////////////
  // old FmrcController - deprecated
  private static final String FMRC_PATH = "/admin/collection/showFmrc";
  private static final String STATISTICS = "cacheStatistics.txt";
  private static final String CMD = "cmd";
  private static final String FILE = "file";
  private final FmrcCacheMonitorImpl monitor = new FmrcCacheMonitorImpl();

  @RequestMapping(value = {"/showFmrc", "/showFmrc/*"})
  protected ModelAndView showFmrcCache(HttpServletRequest req, HttpServletResponse res) throws Exception {
    String path = TdsPathUtils.extractPath(req, "admin/");   // LOOK probably wrong

    if (path.endsWith(STATISTICS)) {
      res.setContentType(ContentType.text.getContentHeader());
      Formatter f = new Formatter();
      monitor.getCacheStatistics(f);
      String s = f.toString();
      PrintWriter pw = res.getWriter();
      pw.println(s);
      pw.flush();
      return null;
    }

    String collectName = StringUtil2.unescape(req.getParameter(COLLECTION)); // this is the collection name
    String fileName = req.getParameter(FILE);
    String cmd = req.getParameter(CMD);

    // show the file
    if (fileName != null) {
      String ufilename = java.net.URLDecoder.decode(fileName, CDM.UTF8);
      String contents = monitor.getCachedFile(collectName, ufilename);
      if (null == contents) {
        res.setContentType(ContentType.html.getContentHeader());
        PrintWriter pw = res.getWriter();
        pw.println("<p/> Cant find filename=" + Escape.html(fileName) + " in collection = " + Escape.html(collectName));
      } else {
        res.setContentType(ContentType.xml.getContentHeader());
        PrintWriter pw = res.getWriter();
        pw.println(contents);
      }

      return null;
    }

    // list the collection
    if (collectName != null) {
      String ecollectName = Escape.uriParam(collectName);
      String url = tdsContext.getContextPath() + FMRC_PATH + "?" + COLLECTION + "=" + ecollectName;
      res.setContentType(ContentType.html.getContentHeader());
      PrintWriter pw = res.getWriter();

      pw.println("Files for collection = " + Escape.html(collectName) + "");

      // allow delete
      String deleteUrl = tdsContext.getContextPath() + FMRC_PATH + "?" + COLLECTION + "=" + ecollectName + "&" + CMD + "=delete";
      pw.println("<a href='" + deleteUrl + "'> Delete Cache" + "</a>");

      pw.println("<ol>");
      for (String filename : monitor.getFilesInCollection(collectName)) {
        String efileName = java.net.URLEncoder.encode(filename, CDM.UTF8);
        pw.println("<li> <a href='" + url + "&" + FILE + "=" + efileName + "'>" + filename + "</a>");
      }
      pw.println("</ol>");
    }

    if (cmd != null && cmd.equals("delete")) {
      res.setContentType(ContentType.html.getContentHeader());
      PrintWriter pw = res.getWriter();

      try {
        monitor.deleteCollection(collectName);
        pw.println("<p/>deleted");

      } catch (Exception e) {
        pw.println("<pre>delete failed on collection = " + Escape.html(collectName));
        e.printStackTrace(pw);
        pw.println("</pre>");
      }
    }

    return null;
  }
}

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

package thredds.server.admin;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.coverity.security.Escape;
import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import thredds.catalog.InvDatasetFeatureCollection;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.*;
import thredds.monitor.FmrcCacheMonitorImpl;
import thredds.server.config.TdsContext;
import thredds.servlet.DataRootHandler;
import thredds.util.ContentType;
import thredds.util.TdsPathUtils;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.StringUtil2;

/**
 * Allow external triggers for rereading Feature collections
 *
 * @author caron
 * @since May 4, 2010
 */
@Controller
@RequestMapping(value={"/admin"})
public class CollectionController  {
  private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());

  private static final String PATH = "/admin/collection";
  private static final String COLLECTION = "collection";
  private static final String SHOW = "showStatus";
  private static final String TRIGGER = "trigger";

  @Autowired
  private TdsContext tdsContext;

  @PostConstruct
  public void afterPropertiesSet() {
    //this.tdsContext = _tdsContext;

    DebugController.Category debugHandler = DebugController.find("Collections");
    DebugController.Action act;

    act = new DebugController.Action("showCollection", "Show Collections") {
      public void doAction(DebugController.Event e) {
        // get sorted list of collections
        List<InvDatasetFeatureCollection> fcList = DataRootHandler.getInstance().getFeatureCollections();
        Collections.sort(fcList, new Comparator<InvDatasetFeatureCollection>() {
          public int compare(InvDatasetFeatureCollection o1, InvDatasetFeatureCollection o2) {
            return o1.getName().compareTo(o2.getName());
          }
        });

        for (InvDatasetFeatureCollection fc : fcList) {
          String uriParam = Escape.uriParam(fc.getCollectionName());
          String url = tdsContext.getContextPath() + PATH + "?" + COLLECTION + "=" + uriParam;
          e.pw.printf("<p/><a href='%s'>%s</a>%n", url, fc.getName());
          FeatureCollectionConfig config = fc.getConfig();
          if (config != null)
            e.pw.printf("%s%n",config.spec);
        }

        String url = tdsContext.getContextPath() + PATH + "/" + SHOW;
        e.pw.printf("<p/><a href='%s'>Show All Collection Status</a>%n", url);
      }
    };
    debugHandler.addAction(act);

    act = new DebugController.Action("sched", "Show scheduler") {
      public void doAction(DebugController.Event e) {
        org.quartz.Scheduler scheduler = CollectionUpdater.INSTANCE.getScheduler();
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

    act = new DebugController.Action("showFmrcCache", "Show FMRC Cache") {
      public void doAction(DebugController.Event e) {
        e.pw.println("<p>cache location = "+monitor.getCacheLocation()+"<p>");
        String statUrl = tdsContext.getContextPath() + FMRC_PATH + "/"+STATISTICS;
        e.pw.println("<p/> <a href='" + statUrl + "'>Show Cache Statistics</a>");
        for (String name : monitor.getCachedCollections()) {
          String ename = StringUtil2.quoteHtmlContent(name);
          String url = tdsContext.getContextPath() + FMRC_PATH + "?"+COLLECTION+"="+ename;
          e.pw.println("<p/> <a href='" + url + "'>" + name + "</a>");
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugController.Action("syncFmrcCache", "Flush FMRC Cache to disk") {
      public void doAction(DebugController.Event e) {
        monitor.sync();
        e.pw.println("<p>bdb cache location = "+monitor.getCacheLocation()+"<p> flushed to disk");
      }
    };
    debugHandler.addAction(act);

  }

  @RequestMapping(value={"/collection/showStatus"})
  protected ModelAndView handleCollectionStatus(HttpServletRequest req, HttpServletResponse res) throws Exception {
    res.setContentType(ContentType.html.getContentHeader());
    PrintWriter pw = res.getWriter();

   // get sorted list of collections
    List<InvDatasetFeatureCollection> fcList = DataRootHandler.getInstance().getFeatureCollections();
    Collections.sort(fcList, new Comparator<InvDatasetFeatureCollection>() {
      public int compare(InvDatasetFeatureCollection o1, InvDatasetFeatureCollection o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });

    for (InvDatasetFeatureCollection fc : fcList) {
      String uriParam = Escape.uriParam(fc.getCollectionName());
      String url = tdsContext.getContextPath() + PATH + "?" + COLLECTION + "=" + uriParam;
      pw.printf("<p/><a href='%s'>%s</a>%n", url, fc.getName());
      pw.printf("<pre>%s</pre>%n", fc.showStatusShort("txt"));
    }
    return null;
  }

  @RequestMapping(value={"/collection/showStatus.csv"})
  protected ModelAndView handleCollectionStatusCsv(HttpServletRequest req, HttpServletResponse res) throws Exception {
    res.setContentType(ContentType.csv.getContentHeader());
    PrintWriter pw = res.getWriter();

   // get sorted list of collections
    List<InvDatasetFeatureCollection> fcList = DataRootHandler.getInstance().getFeatureCollections();
    Collections.sort(fcList, new Comparator<InvDatasetFeatureCollection>() {
      public int compare(InvDatasetFeatureCollection o1, InvDatasetFeatureCollection o2) {
        int compareType = o1.getConfig().type.toString().compareTo(o1.getConfig().type.toString());
        if (compareType != 0) return compareType;
        return o1.getName().compareTo(o2.getName());
      }
    });

    pw.printf("%s, %s, %s, %s, %s, %s, %s, %s%n", "collection", "type", "group", "nrecords", "ndups", "%", "nmiss", "%");
    for (InvDatasetFeatureCollection fc : fcList) {
      if (fc.getConfig().type != FeatureCollectionType.GRIB1 && fc.getConfig().type != FeatureCollectionType.GRIB2) continue;
      pw.printf("%s", fc.showStatusShort("csv"));
    }
    return null;
  }

  @RequestMapping(value={"/collection", "/collection/trigger"})
  protected ModelAndView handleCollectionTriggers(HttpServletRequest req, HttpServletResponse res) throws Exception {
    //String path = req.getPathInfo();
    String path = req.getServletPath();
    if (path == null) path = "";
    
    if (path.startsWith("/admin") )
    	path = path.substring("/admin".length(), path.length());

    res.setContentType(ContentType.html.getContentHeader());
    PrintWriter pw = res.getWriter();

    // deal with a trigger
    CollectionUpdateType triggerType = null;
    if (path.equals("/"+COLLECTION+"/"+TRIGGER)) {
      String triggerTypeS = req.getParameter(TRIGGER);
      try {
        triggerType = CollectionUpdateType.valueOf(triggerTypeS);
      } catch (Throwable t) {
        ;  // noop
      }
      if (triggerType == null) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        pw.printf(" TRIGGER Type %s not legal%n", Escape.html(triggerTypeS));
        pw.flush();
        return null;
      }
    }

    String collectName = StringUtil2.unescape( req.getParameter(COLLECTION)); // this is the collection name
    InvDatasetFeatureCollection fc = DataRootHandler.getInstance().findFcByCollectionName(collectName);
    if (fc == null) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      pw.append("NOT FOUND");
      pw.flush();
      return null;
    }

    pw.printf("<h3>Collection %s</h3>%n", Escape.html(collectName));

    if (triggerType != null) {
      if (!fc.getConfig().isTrigggerOk()) {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        pw.printf(" TRIGGER NOT ENABLED%n");
        pw.flush();
        return null;
      }

      CollectionUpdater.INSTANCE.triggerUpdate(collectName, triggerType);
      pw.printf(" TRIGGER SENT%n");

    } else {
      showFeatureCollection(pw, fc);

      String uriParam = Escape.uriParam(fc.getCollectionName());
      String url = tdsContext.getContextPath() + PATH + "/trigger?" + COLLECTION + "=" + uriParam + "&" + TRIGGER + "=" + CollectionUpdateType.nocheck;
      pw.printf("<p/><a href='%s'>Send trigger to %s</a>%n", url, Escape.html(fc.getName()));
    }

    pw.flush();
    return null;
  }

  private void showFeatureCollection(PrintWriter pw, InvDatasetFeatureCollection fc) {
    FeatureCollectionConfig config = fc.getConfig(); // LOOK
    if (config != null) {
      Formatter f = new Formatter();
      config.show(f);
      pw.printf("%n<pre>%s%n</pre>", f.toString());
    }

    Formatter f = new Formatter();
    fc.showStatus(f);
    pw.printf("%n<pre>%s%n</pre>", f.toString());
  }

  /////////////////////////////////////////////////////////////
  // old FmrcController - deprecated
  private static final String FMRC_PATH = "/admin/showFmrc";
  private static final String STATISTICS = "cacheStatistics.txt";
  private static final String CMD = "cmd";
  private static final String FILE = "file";
  private final FmrcCacheMonitorImpl monitor = new FmrcCacheMonitorImpl();

  @RequestMapping(value={"/showFmrc", "/showFmrc/*"})
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

    String collectName = StringUtil2.unescape( req.getParameter(COLLECTION)); // this is the collection name
    String fileName = req.getParameter(FILE);
    String cmd = req.getParameter(CMD);

    // show the file
    if (fileName != null) {
      String ufilename = java.net.URLDecoder.decode(fileName, CDM.UTF8);
      String contents = monitor.getCachedFile(collectName, ufilename);
      if (null == contents) {
        res.setContentType(ContentType.html.getContentHeader());
        PrintWriter pw = res.getWriter();
        pw.println("<p/> Cant find filename="+Escape.html(fileName)+" in collection = "+ Escape.html(collectName));
      }  else {
        res.setContentType(ContentType.xml.getContentHeader());
        PrintWriter pw = res.getWriter();
        pw.println(contents);
      }

      return null;
    }

    // list the collection
    if (collectName != null) {
      String ecollectName = Escape.uriParam(collectName);
      String url = tdsContext.getContextPath() + FMRC_PATH + "?"+COLLECTION+"="+ecollectName;
      res.setContentType(ContentType.html.getContentHeader());
      PrintWriter pw = res.getWriter();

      pw.println("Files for collection = "+Escape.html(collectName)+"");

      // allow delete
      String deleteUrl = tdsContext.getContextPath() + FMRC_PATH + "?"+COLLECTION+"="+ecollectName+"&"+CMD+"=delete";
      pw.println("<a href='" + deleteUrl + "'> Delete" + "</a>");

      pw.println("<ol>");
      for (String filename : monitor.getFilesInCollection(collectName)) {
        String efileName = java.net.URLEncoder.encode(filename, CDM.UTF8);
        pw.println("<li> <a href='" + url + "&"+FILE+"="+efileName + "'>" + filename + "</a>");
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
        pw.println("<pre>delete failed on collection = "+Escape.html(collectName));
        e.printStackTrace(pw);
        pw.println("</pre>");
      }
    }

    return null;
  }


}

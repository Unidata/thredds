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

import java.io.IOException;
import java.io.PrintWriter;
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
import thredds.inventory.*;
import thredds.monitor.FmrcCacheMonitorImpl;
import thredds.server.config.TdsContext;
import thredds.servlet.DataRootHandler;
import thredds.util.ContentType;
import thredds.util.TdsPathUtils;
import ucar.nc2.time.CalendarDateFormatter;
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
  private static final String TRIGGER = "trigger";
  // private static final String NOCHECK = "nocheck";
  
  @Autowired
  private TdsContext tdsContext;

  @PostConstruct
  public void afterPropertiesSet(){
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
          String ename = StringUtil2.escape(fc.getName(), "");
          String url = tdsContext.getContextPath() + PATH + "?" + COLLECTION + "=" + ename;
          e.pw.printf("<p/><a href='%s'>%s</a>%n", url, fc.getName());
        }
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
        String statUrl = tdsContext.getContextPath() + PATH + "/"+STATISTICS;
        e.pw.println("<p/> <a href='" + statUrl + "'>Show Cache Statistics</a>");
        for (String name : monitor.getCachedCollections()) {
          String ename = StringUtil2.escape(name, "");
          String url = tdsContext.getContextPath() + PATH + "?"+COLLECTION+"="+ename;
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

  @RequestMapping(value={"/collection", "/collection/trigger"})
  protected ModelAndView handleCollectionTriggers(HttpServletRequest req, HttpServletResponse res) throws Exception {
    //String path = req.getPathInfo();
    String path = req.getServletPath();
    if (path == null) path = "";
    
    if(path.startsWith("/admin") )
    	path = path.substring("/admin".length(), path.length());

    res.setContentType(ContentType.html.getContentHeader());
    PrintWriter pw = res.getWriter();

    // find the collection
    CollectionUpdateType type = null;
    if (path.equals("/"+COLLECTION+"/"+TRIGGER)) {
      String triggerType = req.getParameter(TRIGGER);
      try {
        type = CollectionUpdateType.valueOf(triggerType);
      } catch (Throwable t) {
        ;  // noop
      }
      if (type == null) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        pw.printf(" TRIGGER Type %s not legal%n", Escape.html(triggerType));
        pw.flush();
        return null;
      }
    }

    String collectName = req.getParameter(COLLECTION);
    InvDatasetFeatureCollection fc = DataRootHandler.getInstance().findFcByCollectionName(collectName);
    if (fc == null) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      pw.append("NOT FOUND");
      pw.flush();
      return null;
    }

    pw.printf("<h3>Collection Name %s</h3>%n", Escape.html(collectName));

    if (type != null) {
      if (!fc.getConfig().isTrigggerOk()) {
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        pw.printf(" TRIGGER NOT ENABLED%n");
        pw.flush();
        return null;
      }

      CollectionUpdater.INSTANCE.triggerUpdate(collectName, type);
      pw.printf(" TRIGGER SENT%n");

    } else {
      MCollection dcm = fc.getDatasetCollectionManager();
      if (dcm != null)
        showFiles(pw, dcm);
      String ename = StringUtil2.escape(fc.getName(), "");
      String url = tdsContext.getContextPath() + PATH + "/trigger?" + COLLECTION + "=" + ename;
      pw.printf("<p/><a href='%s'>Trigger rescan for %s</a>%n", url, fc.getName());
    }

    pw.flush();
    return null;
  }

  private void showFiles(PrintWriter pw, MCollection dcm) {
    if (dcm instanceof CollectionManager) {
      CollectionManager cm = (CollectionManager) dcm;
      boolean unscanned = cm.getLastScanned() == 0;
      if (unscanned) {
        pw.printf("%n<pre>Not Yet Scanned%n");
        return;
      }

      pw.printf("%n<pre>Last Scanned %-20s%n", CalendarDateFormatter.toDateTimeString(new Date(cm.getLastScanned())));
    }

    pw.printf("%n%-100s %-20s %9.3s %s%n", "Path", "Last Modified", "MB", "Aux");
    try {
      for (MFile mfile : dcm.getFilesSorted())
        pw.printf("%-100s %-20s %9.3f %s%n", mfile.getPath(), CalendarDateFormatter.toDateTimeString(new Date(mfile.getLastModified())),
                (double) mfile.getLength() / (1000 * 1000), mfile.getAuxInfo());
    } catch (IOException e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    }
    pw.printf("</pre>%n");
  }

  /////////////////////////////////////////////////////////////
  // old FmrcController - deprecated
  private static final String FMRC_PATH = "/admin/fmrcCache";
  private static final String STATISTICS = "cacheStatistics.txt";
  private static final String CMD = "cmd";
  private static final String FILE = "file";
  private final FmrcCacheMonitorImpl monitor = new FmrcCacheMonitorImpl();

  @RequestMapping(value={"/fmrcCache", "/fmrcCache/*"})
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

    String collectName = req.getParameter(COLLECTION);
    String fileName = req.getParameter(FILE);
    String cmd = req.getParameter(CMD);

    // show the file
    if (fileName != null) {
      String contents = monitor.getCachedFile(collectName, fileName);
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
      String ecollectName = StringUtil2.escape(collectName, "");
      String url = tdsContext.getContextPath() + FMRC_PATH + "?"+COLLECTION+"="+ecollectName;
      res.setContentType(ContentType.html.getContentHeader());
      PrintWriter pw = res.getWriter();

      pw.println("Files for collection = "+Escape.html(collectName)+"");

      // allow delete
      String deleteUrl = tdsContext.getContextPath() + FMRC_PATH + "?"+COLLECTION+"="+ecollectName+"&"+CMD+"=delete";
      pw.println("<a href='" + deleteUrl + "'> Delete" + "</a>");

      pw.println("<ol>");
      for (String filename : monitor.getFilesInCollection(collectName)) {
        String efileName = StringUtil2.escape(filename, "");
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

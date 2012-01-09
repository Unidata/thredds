package thredds.server.root;

import org.quartz.JobKey;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import thredds.catalog.InvDatasetFeatureCollection;
import thredds.inventory.CollectionManager;
import thredds.inventory.CollectionUpdater;
import thredds.inventory.DatasetCollectionMFiles;
import thredds.inventory.MFile;
import thredds.server.config.TdsContext;
import thredds.servlet.DataRootHandler;
import thredds.servlet.DebugHandler;
import thredds.servlet.UsageLog;
import ucar.nc2.time.CalendarDateFormatter;
import ucar.unidata.util.StringUtil2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

/**
 * Allow external triggers for rereading Feature collections
 *
 * @author caron
 * @since May 4, 2010
 */
public class CollectionController extends AbstractController {
  private final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
  //private final org.slf4j.Logger logFc = org.slf4j.LoggerFactory.getLogger(DatasetCollectionMFiles.class);

  private static final String PATH = "/admin/collection";
  private static final String COLLECTION = "collection";
  private static final String TRIGGER = "trigger";
  private final TdsContext tdsContext;
  //private final FmrcCacheMonitorImpl monitor = new FmrcCacheMonitorImpl();

  CollectionController(TdsContext _tdsContext) {
    this.tdsContext = _tdsContext;

    DebugHandler debugHandler = DebugHandler.get("Collections");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showCollection", "Show Collections") {
      public void doAction(DebugHandler.Event e) {
        List<InvDatasetFeatureCollection> fcList = DataRootHandler.getInstance().getFeatureCollections();
        for (InvDatasetFeatureCollection fc : fcList) {
          String ename = StringUtil2.escape(fc.getName(), "");
          String url = tdsContext.getContextPath() + PATH + "?" + COLLECTION + "=" + ename;
          e.pw.printf("<p/><a href='%s'>%s</a>%n", url, fc.getName());
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("sched", "Show scheduler") {
      public void doAction(DebugHandler.Event e) {
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
  }

  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
    log.info("handleRequest: " + UsageLog.setupRequestContext(req));

    String path = req.getPathInfo();
    if (path == null) path = "";

    PrintWriter pw = res.getWriter();
    res.setContentType("text/html");

    // find the collection
    String collectName = req.getParameter(COLLECTION);
    String triggerS = req.getParameter(TRIGGER);
    boolean trigger = (triggerS != null) && triggerS.equalsIgnoreCase("true");
    boolean nocheck = (triggerS != null) && triggerS.equalsIgnoreCase("nocheck");
    InvDatasetFeatureCollection fc = DataRootHandler.getInstance().getFeatureCollection(collectName);
    if (fc == null) {
      log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, 0));
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      pw.append("NOT FOUND");
      pw.flush();
      return null;
    }

    CollectionManager dcm = fc.getDatasetCollectionManager();
    pw.printf("<h3>Collection Name %s</h3>%n", dcm.getCollectionName());

    if (trigger || nocheck) {
      // see if trigger is allowed
      if (!fc.getConfig().updateConfig.triggerOk && !fc.getConfig().tdmConfig.triggerOk) {
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, 0));
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        pw.printf(" TRIGGER NOT ENABLED%n");
        pw.flush();
        return null;
      }

      CollectionUpdater.INSTANCE.triggerUpdate(dcm.getCollectionName(), trigger ? "trigger" : "nocheck");
      pw.printf(" TRIGGER SENT%n");

    } else {
      showFiles(pw, dcm);
      String ename = StringUtil2.escape(fc.getName(), "");
      String url = tdsContext.getContextPath() + PATH + "?" + COLLECTION + "=" + ename + "&trigger=true";
      pw.printf("<p/><a href='%s'>Trigger rescan for %s</a>%n", url, fc.getName());
    }

    log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, 0));
    pw.flush();
    return null;
  }

  private void showFiles(PrintWriter pw, CollectionManager dcm) {
    boolean unscanned = dcm.getLastScanned() == 0;
    if (unscanned) {
      pw.printf("%n<pre>Not Yet Scanned%n");
      return;
    }

    pw.printf("%n<pre>Last Scanned %-20s%n", CalendarDateFormatter.toDateTimeString(new Date(dcm.getLastScanned())));
    pw.printf("%n%-100s %-20s %9.3s %s%n", "Path", "Last Modified", "MB", "Aux");
    for (MFile mfile : dcm.getFiles())
      pw.printf("%-100s %-20s %9.3f %s%n", mfile.getPath(), CalendarDateFormatter.toDateTimeString(new Date(mfile.getLastModified())),
              (double) mfile.getLength() / 1000 * 1000, mfile.getAuxInfo());
    pw.printf("</pre>%n");
  }

}

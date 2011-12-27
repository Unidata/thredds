package thredds.server.root;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import thredds.catalog.InvDatasetFeatureCollection;
import thredds.inventory.CollectionManager;
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
  }

  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
    log.info("handleRequest: " + UsageLog.setupRequestContext(req));

    String path = req.getPathInfo();
    if (path == null) path = "";

    PrintWriter pw = res.getWriter();
    res.setContentType("text/html");

    // find the collection
    String collectName = req.getParameter(COLLECTION);
    String trigger = req.getParameter(TRIGGER);
    boolean wantTrigger = (trigger != null) && trigger.equalsIgnoreCase("true");
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

    if (wantTrigger) {
      // see if trigger is allowed
      if (!fc.getConfig().updateConfig.triggerOk && !fc.getConfig().tdmConfig.triggerOk) {
        log.info(UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_FORBIDDEN, 0));
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        pw.printf(" TRIGGER NOT ENABLED%n");
        pw.flush();
        return null;
      }

      Formatter f = new Formatter();
      boolean scanReturn = fc.triggerRescan(f);
      String err = f.toString();
      if (err != null && err.length() > 0) {
        pw.printf(" RESCAN FAILED = %s%n", err);
      } else {
        pw.printf(" RESCAN RETURN = %s%n", scanReturn);
        showFiles(pw, dcm);
      }

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
    pw.printf("%n%-80s %-20s %7s %s%n", "Path", "Last Modified", "KB", "Aux");
    for (MFile mfile : dcm.getFiles())
      pw.printf("%-80s %-20s %7d %s%n", mfile.getPath(), CalendarDateFormatter.toDateTimeString(new Date(mfile.getLastModified())),
              mfile.getLength() / 1000, mfile.getAuxInfo());
    pw.printf("</pre>%n");
  }

}

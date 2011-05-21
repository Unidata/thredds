package thredds.server.root;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import thredds.catalog.InvDatasetFeatureCollection;
import thredds.inventory.DatasetCollectionManager;
import thredds.inventory.MFile;
import thredds.server.config.TdsContext;
import thredds.servlet.DataRootHandler;
import thredds.servlet.DebugHandler;
import thredds.servlet.UsageLog;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Date;
import java.util.List;

/**
 * Allow external triggers for rereading FMRC collections
 *
 * @author caron
 * @since May 4, 2010
 */
public class CollectionController extends AbstractController {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );
  private static final String PATH = "/admin/collection";
  private static final String COLLECTION = "collection";
  private static final String TRIGGER = "trigger";
  private final TdsContext tdsContext;

  CollectionController( TdsContext _tdsContext) {
    this.tdsContext = _tdsContext;

    DebugHandler debugHandler = DebugHandler.get("Collections");
    DebugHandler.Action act;

    act = new DebugHandler.Action("triggerRescan", "Show Collections") {
      public void doAction(DebugHandler.Event e) {
        List<InvDatasetFeatureCollection> fcList = DataRootHandler.getInstance().getFeatureCollections();
        for (InvDatasetFeatureCollection fc :fcList) {
          String ename = StringUtil.escape(fc.getName(), "");
          String url = tdsContext.getContextPath() + PATH + "?"+COLLECTION+"="+ename;
          e.pw.printf("<p/><a href='%s'>%s</a>%n", url, fc.getName());
        }
      }
    };
    debugHandler.addAction(act);
  }

  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
    log.info( "handleRequest: " + UsageLog.setupRequestContext( req ) );

    String path = req.getPathInfo();
    if (path == null) path = "";

    // find the collection
    String collectName = req.getParameter(COLLECTION);
    String trigger = req.getParameter(TRIGGER);
    boolean wantTrigger = (trigger != null) && trigger.equalsIgnoreCase("true");
    InvDatasetFeatureCollection fc = DataRootHandler.getInstance().getFeatureCollection(collectName);
    if (fc == null) {
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, 0) );
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      PrintWriter pw = res.getWriter();
      pw.append("NOT FOUND");
      pw.flush();
      return null;
    }

    if (wantTrigger) {
      // trigger the collection if allowed
      if (!fc.getConfig().updateConfig.triggerOk) {
        log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, 0) );
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        PrintWriter pw = res.getWriter();
        pw.append("NOT ALLOWED");
        pw.flush();
        return null;
      }

      fc.triggerRescan();

    } else {

      // show whats in the collection
      res.setContentType("text/html");
      DateFormatter df = new DateFormatter();
      PrintWriter pw = res.getWriter();
      DatasetCollectionManager dcm = fc.getDatasetCollectionManager();
      pw.printf("<h3>Collection Name %s</h3>%n", dcm.getCollectionName()+"");
      pw.printf("%n<pre>Last Scanned %-20s%n", df.toDateTimeStringISO(new Date(dcm.getLastScanned())));
      pw.printf("%n%-60s %-20s %7s %s%n", "Path", "Last Modified", "KB", "Aux");
      if (null != dcm.getFiles()) {
        for (MFile mfile : dcm.getFiles())
          pw.printf("%-60s %-20s %7d %s%n", mfile.getPath(), df.toDateTimeStringISO(new Date(mfile.getLastModified())),
                  mfile.getLength()/1000, mfile.getAuxInfo());
      }
      pw.printf("</pre>%n");
      pw.flush();
    }

    log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, 0) );
    return null;
  }

}

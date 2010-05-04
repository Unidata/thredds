package thredds.server.root;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import thredds.catalog.InvDatasetFeatureCollection;
import thredds.monitor.FmrcCacheMonitorImpl;
import thredds.server.config.TdsContext;
import thredds.servlet.DataRootHandler;
import thredds.servlet.DebugHandler;
import thredds.servlet.UsageLog;
import ucar.unidata.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.List;

/**
 * Allow external triggers for rereading FMRC collections
 *
 * @author caron
 * @since May 4, 2010
 */
public class CollectionTriggerController extends AbstractController {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );
  private static final String PATH = "/admin/trigger";
  private static final String COLLECTION = "collection";
  private final TdsContext tdsContext;
  private final FmrcCacheMonitorImpl monitor = new FmrcCacheMonitorImpl();

  CollectionTriggerController( TdsContext _tdsContext) {
    this.tdsContext = _tdsContext;

    DebugHandler debugHandler = DebugHandler.get("Collections");
    DebugHandler.Action act;

    act = new DebugHandler.Action("triggerRescan", "Show Collections") {
      public void doAction(DebugHandler.Event e) {
        List<InvDatasetFeatureCollection> fmrcList = DataRootHandler.getInstance().getFmrc();
        for (InvDatasetFeatureCollection fmrc :fmrcList) {
          String ename = StringUtil.escape(fmrc.getName(), "");
          String url = tdsContext.getContextPath() + PATH + "?"+COLLECTION+"="+ename;
          e.pw.println("<p/> <a href='" + url + "'>" + fmrc.getName() + "</a>");
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
    InvDatasetFeatureCollection fmrc = DataRootHandler.getInstance().getFmrc(collectName);
    if (fmrc == null) {
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_NOT_FOUND, 0) );
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      PrintWriter pw = res.getWriter();
      pw.append("NOT FOUND");
      pw.flush();
      return null;
    }

    // trigger the collection if allowed
    if (!fmrc.getConfig().updateConfig.triggerOk) {
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_FORBIDDEN, 0) );
      res.setStatus(HttpServletResponse.SC_FORBIDDEN);
      PrintWriter pw = res.getWriter();
      pw.append("NOT ALLOWED");
      pw.flush();
      return null;
    }

    // ok
    fmrc.triggerRescan();
    log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, 0) );
    PrintWriter pw = res.getWriter();
    pw.append("OK");
    pw.flush();
    return null;
  }

}

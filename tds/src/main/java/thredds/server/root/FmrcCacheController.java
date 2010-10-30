/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package thredds.server.root;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import thredds.monitor.FmrcCacheMonitorImpl;
import thredds.server.config.TdsContext;
import thredds.servlet.DebugHandler;
import thredds.servlet.UsageLog;
import ucar.unidata.util.StringUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.util.Formatter;

/**
 * Show Fmrc Collection cache
 *
 * @author caron
 * @since Apr 19, 2010
 */
public class FmrcCacheController extends AbstractController {
  private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger( getClass() );
  private static final String PATH = "/admin/fmrcCache";
  private static final String COLLECTION = "collection";
  private static final String STATISTICS = "cacheStatistics.txt";
  private static final String CMD = "cmd";
  private static final String FILE = "file";
  private final TdsContext tdsContext;
  private final FmrcCacheMonitorImpl monitor = new FmrcCacheMonitorImpl();

  FmrcCacheController( TdsContext _tdsContext) {
    this.tdsContext = _tdsContext;

    DebugHandler debugHandler = DebugHandler.get("Collections");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showFmrcCache", "Show FMRC Cache") {
      public void doAction(DebugHandler.Event e) {
        e.pw.println("<p>cache location = "+monitor.getCacheLocation()+"<p>");
        String statUrl = tdsContext.getContextPath() + PATH + "/"+STATISTICS;
        e.pw.println("<p/> <a href='" + statUrl + "'>Show Cache Statistics</a>");
        for (String name : monitor.getCachedCollections()) {
          String ename = StringUtil.escape(name, "");
          String url = tdsContext.getContextPath() + PATH + "?"+COLLECTION+"="+ename;
          e.pw.println("<p/> <a href='" + url + "'>" + name + "</a>");
        }
      }
    };
    debugHandler.addAction(act);
  }

  protected ModelAndView handleRequestInternal(HttpServletRequest req, HttpServletResponse res) throws Exception {
    log.info( "handleRequestInternal(): " + UsageLog.setupRequestContext( req ) );

    String path = req.getPathInfo();
    if (path == null) path = "";

    PrintWriter pw = res.getWriter();

    if (path.endsWith(STATISTICS)) {
      res.setContentType("text/plain");
      Formatter f = new Formatter();
      monitor.getCacheStatistics(f);
      String s = f.toString();
      pw.println(s);
      pw.flush();
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, s.length()));
      return null;
    }

    String collectName = req.getParameter(COLLECTION);
    String fileName = req.getParameter(FILE);
    String cmd = req.getParameter(CMD);

    // show the file
    if (fileName != null) {
      String contents = monitor.getCachedFile(collectName, fileName);
      if (null == contents)
        pw.println("<p/> Cant find filename="+fileName+" in collection = "+collectName);
      else {
        res.setContentType("application/xml");
        pw.println(contents);
      }
      
      log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, 0) );
      return null;
    }
    
    // list the collection
    if (collectName != null) {
      String ecollectName = StringUtil.escape(collectName, "");
      String url = tdsContext.getContextPath() + PATH + "?"+COLLECTION+"="+ecollectName;
      res.setContentType("text/html");
      pw.println("Files for collection = "+collectName+"");

      // allow delete
      String deleteUrl = tdsContext.getContextPath() + PATH + "?"+COLLECTION+"="+ecollectName+"&"+CMD+"=delete";
      pw.println("<a href='" + deleteUrl + "'> Delete" + "</a>");

      pw.println("<ol>");
      for (String filename : monitor.getFilesInCollection(collectName)) {
        String efileName = StringUtil.escape(filename, "");
        pw.println("<li> <a href='" + url + "&"+FILE+"="+efileName + "'>" + filename + "</a>");
      }
     pw.println("</ol>");
    }

    if (cmd != null && cmd.equals("delete")) {
      try {
        monitor.deleteCollection(collectName);
        pw.println("<p/>deleted");
      } catch (Exception e) {
        pw.println("<pre>delete failed on collection = "+collectName);
        e.printStackTrace(pw);
        pw.println("</pre>");
      }
    }

    log.info( UsageLog.closingMessageForRequestContext( HttpServletResponse.SC_OK, 0) );
    return null;
  }

}

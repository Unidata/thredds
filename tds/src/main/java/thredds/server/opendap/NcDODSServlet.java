// $Id: NcDODSServlet.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
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

package thredds.server.opendap;

import opendap.dap.parser.ParseException;
import opendap.dap.DAP2Exception;

import opendap.servlet.GuardedDataset;
import opendap.servlet.ReqState;

import ucar.nc2.NetcdfFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;

import java.net.URI;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import thredds.servlet.*;

/**
 * ************************************************************************
 * NetCDF opendap server.
 *
 * @author John Caron
 * @version $Id: NcDODSServlet.java 51 2006-07-12 17:13:13Z caron $
 */

public class NcDODSServlet extends opendap.servlet.AbstractServlet {
  static final String GDATASET = "guarded_dataset";

  private org.slf4j.Logger log;

  private boolean debugInit = false;

  private String serviceId, serviceTitle;
  private String latestServiceId, latestServiceTitle;
  private int maxNetcdfFilesCached = 100;

  private String odapVersionString;
  private String odapSpecVersionString = "opendap/3.7";
  private String odapImplVersionString = null;

  private URI baseURI = null;

  public void init() throws javax.servlet.ServletException {
    super.init();

    org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger("catalogInit");
    allowDeflate = false; // handled by Tomcat

    try {
      // init logging
      log = org.slf4j.LoggerFactory.getLogger( getClass());

      // Configure OPeNDAP spec and impl version numbers
      this.odapImplVersionString = ThreddsConfig.get( "odapImplVersion", null );
      this.odapVersionString = this.odapSpecVersionString
                               + ( this.odapImplVersionString != null
                                   ? " \r\n " + this.odapImplVersionString : "");

      debugInit |= Debug.isSet("ncdods/init");
      if (debugInit || Debug.isSet("ncdods/showServerInfo")) {
        ServletUtil.showServletInfo(this, System.out);
      }

      serviceId = getInitParameter("serviceId");
      if (serviceId == null) serviceId = "ncdods";
      serviceTitle = getInitParameter("serviceTitle");
      if (serviceTitle == null) serviceTitle = "netCDF-OpenDAP Server 3.0a";

      latestServiceId = getInitParameter("latestServiceId");
      if (latestServiceId == null) latestServiceId = "latest";
      latestServiceTitle = getInitParameter("latestServiceTitle");
      if (latestServiceTitle == null) latestServiceTitle = "netCDF-OpenDAP Server 3.0a";

      // set up the NetcdfFile cache
      String p = getInitParameter("maxNetcdfFilesCached");
      if (p != null) {
        try {
          maxNetcdfFilesCached = Integer.parseInt(p);
          if (debugInit) log.debug(" maxNetcdfFilesCached = " + maxNetcdfFilesCached);
        } catch (NumberFormatException e) {
          log.error(" maxNetcdfFilesCached bad number format in web.xml; use value " + maxNetcdfFilesCached);
        }
      }

      // debugging actions
      makeDebugActions();

      logServerStartup.info(" initialized");

    } catch (Throwable t) {
      logServerStartup.error("NcDODSServlet init", t);
      t.printStackTrace();
    }

  }

  /**
   * ***********************************************************************
   */

  // LOOK can we automate getting the version number ??
  public String getServerVersion() {
    return this.odapVersionString;
  }

  // Servlets that support HTTP GET requests and can quickly determine their last modification time should
  // override this method. This makes browser and proxy caches work more effectively, reducing the load on
  // server and network resources.
  protected long getLastModified(HttpServletRequest req) {
    String query = req.getQueryString();
    if (query != null) return -1;

    String path = req.getPathInfo();
    if (path == null) return -1;

    if (path.endsWith(".asc"))
      path = path.substring(0, path.length() - 4);
    else if (path.endsWith(".ascii"))
      path = path.substring(0, path.length() - 6);
    else if (path.endsWith(".das"))
      path = path.substring(0, path.length() - 4);
    else if (path.endsWith(".dds"))
      path = path.substring(0, path.length() - 4);
    else if (path.endsWith(".ddx"))
      path = path.substring(0, path.length() - 4);
    else if (path.endsWith(".dods"))
      path = path.substring(0, path.length() - 5);
    else if (path.endsWith(".html"))
      path = path.substring(0, path.length() - 5);
    else if (path.endsWith(".info"))
      path = path.substring(0, path.length() - 5);
    else if (path.endsWith(".opendap"))
      path = path.substring(0, path.length() - 5);
    else
      return -1;

    // if (null != DatasetHandler.findResourceControl( path)) return -1; // LOOK weird Firefox beahviour?

    File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(path);
    if ((file != null) && file.exists())
      return file.lastModified();

    return -1;
  }

  /**
   * ********************* debugging *******************
   */

  private void makeDebugActions() {
    DebugHandler debugHandler = DebugHandler.get("ncdodsServer");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showStatus", "Show ncdods status") {
      public void doAction(DebugHandler.Event e) {
        try {
          doGetStatus(e.req, e.res);
        }
        catch (Exception ioe) {
          log.error("ShowStatus", ioe);
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("help", "Show help page") {
      public void doAction(DebugHandler.Event e) {
        try {
          doGetHELP(e.req, e.res);
        }
        catch (Exception ioe) {
          log.error("ShowHelp", ioe);
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugHandler.Action("version", "Show server version") {
      public void doAction(DebugHandler.Event e) {
        e.pw.println("  version= " + getServerVersion());
      }
    };
    debugHandler.addAction(act);

  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
          throws IOException, ServletException {

    log.info( UsageLog.setupRequestContext(req));

    try {
      String path = req.getPathInfo();
      if (log.isDebugEnabled()) {
        log.debug("doGet path=" + path);
        if (Debug.isSet("showRequestDetail"))
          log.debug(ServletUtil.showRequestDetail(this, req));
      }

      if (path == null) {
        log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }

      if (baseURI == null) { // first time, set baseURI
        URI reqURI = ServletUtil.getRequestURI(req);
        baseURI = reqURI.resolve("/thredds/dodsC/");
        //rootCatalog.setBaseURI( baseURI);
        log.debug(" baseURI was set = " + baseURI);
      }

      if (path.endsWith(".close")) {
        closeSession(req, res);
        res.setContentLength(0);
        log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, 0));
        return;

      } else if (path.endsWith("latest.xml")) {
        DataRootHandler.getInstance().processReqForLatestDataset(this, req, res);
        return;

        // need to pass normal "html" on to super class
      } else if (path.endsWith("catalog.xml") || path.endsWith("catalog.html") || path.endsWith("/")) {
        if (DataRootHandler.getInstance().processReqForCatalog(req, res))
          return;
      }

      // default is to throw it to the superclass - this processes the .dds, .das etc
      super.doGet(req, res);
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, -1));

    } catch (FileNotFoundException e) {
      log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_NOT_FOUND, -1));
      res.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());

    } catch (IOException ioe) {
      ServletUtil.handleException(ioe, res);

    } catch (Throwable t) {
      t.printStackTrace();
      ServletUtil.handleException(t, res);
    }
  }

  /**
   * ******************** dataset directory **************************************************
   */
  public void doGetDIR(HttpServletRequest req, HttpServletResponse res, ReqState rs)
          throws IOException, ServletException {

    // rather dangerous here, since you can go into an infinite loop
    // so we're going to insist that there's  no suffix
    if ((rs.getRequestSuffix() == null) || (rs.getRequestSuffix().length() == 0)) {
      ServletUtil.forwardToCatalogServices(req, res);
      return;
    }

    badURL(req, res);
  }

  private void closeSession(HttpServletRequest req, HttpServletResponse res) {
    HttpSession session = req.getSession();
    session.invalidate();
  }

  /**
   * *********************** dataset caching ***********************************************
   */

  // any time the server needs access to the dataset, it gets a "GuardedDataset" which allows us to add caching
  // optionally, a session may be established, which allows us to reserve the dataset for that session.
  protected GuardedDataset getDataset(ReqState preq) throws DAP2Exception, IOException, ParseException {
    HttpServletRequest req = preq.getRequest();

    // see if the client wants sessions
    boolean acceptSession = false;
    String s = req.getHeader("X-Accept-Session");
    if (s != null && s.equalsIgnoreCase("true"))
      acceptSession = true;

    HttpSession session = null;
    if (acceptSession) {
      // see if theres already a session established
      session = req.getSession();
      if (!session.isNew()) {
        GuardedDatasetImpl gdataset = (GuardedDatasetImpl) session.getAttribute(GDATASET);
        if (null != gdataset) {
          if (log.isDebugEnabled()) log.debug(" found gdataset " + gdataset + " in session " + session.getId());
          return gdataset;
        }
      }
    }

    /* debug
    System.out.println("NcDODSServlet getDataset = "+preq.getRequestSuffix());
    String reqdetail = ServletUtil.showRequestDetail(this, req);
    System.out.println( reqdetail);
    ServletUtil.showSession( req, System.out);  */

    // canonicalize the path
    /* String spath = req.getServletPath();
    if (spath.length() > 0) {
      if (spath.startsWith("/"))
        spath = spath.substring(1);
      if (!spath.endsWith("/"))
        spath = spath + "/";
    } */

    NetcdfFile ncd;
    String reqPath = preq.getDataSet(); // was spath + preq.getDataSet();
    try {
      ncd = DatasetHandler.getNetcdfFile(req, preq.getResponse(), reqPath);
    } catch (FileNotFoundException fne) {
      throw new DAP2Exception(DAP2Exception.NO_SUCH_FILE, "Cant find " + reqPath);
    } catch (Throwable e) {
      log.error("Error ", e);
      throw new DAP2Exception(DAP2Exception.UNDEFINED_ERROR, "Server Error on dataset "+reqPath);
    }

    if (null == ncd) return null;
    GuardedDatasetImpl gdataset = new GuardedDatasetImpl(reqPath, ncd, acceptSession);

    if (acceptSession) {
      session.setAttribute(GDATASET, gdataset);
      session.setAttribute("dataset", ncd.getLocation());
      // session.setMaxInactiveInterval(30); // 30 second timeout !!
      if (log.isDebugEnabled()) log.debug(" added gdataset " + gdataset + " in session " + session.getId());
    } else {
      session = req.getSession();
      session.setAttribute("dataset", ncd.getLocation()); // see UsageValve
    }

    return gdataset;
  }

}

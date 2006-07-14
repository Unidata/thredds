// $Id: NcDODSServlet.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package dods.servers.netcdf;

import dods.dap.*;
import dods.dap.parser.ParseException;

import dods.servlet.GuardedDataset;
import dods.servlet.ReqState;

import thredds.servlet.*;

import java.io.*;
import java.net.URI;
import javax.servlet.*;
import javax.servlet.http.*;

import ucar.nc2.NetcdfFile;

/**
 * ************************************************************************
 * NetCDF DODS server.
 *
 * @author John Caron
 * @version $Id: NcDODSServlet.java 51 2006-07-12 17:13:13Z caron $
 */

public class NcDODSServlet extends dods.servlet.DODSServlet {
  static final String GDATASET = "guarded_dataset";

  private org.slf4j.Logger log;

  private boolean debugInit = false;

  private String serviceId, serviceTitle;
  private String latestServiceId, latestServiceTitle;
  private int maxNetcdfFilesCached = 100;

  //private String contentPath;
  //private String configFilename;

  //private InvCatalogImpl rootCatalog;
  private URI baseURI = null;

  public void init() throws ServletException {
    super.init();

    try {
      ServletUtil.initDebugging(this); // read debug flags
      ServletUtil.initLogging(this);
      //contentPath = ServletUtil.getContentPath(this) +"dodsC/";

      // init logging
      log = org.slf4j.LoggerFactory.getLogger(getClass());

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

      /* first time, create content directory
      String initialContentPath = ServletUtil.getInitialContentPath(this)+"dodsC/";;
      try {
        ServletUtil.copyDir(initialContentPath, contentPath);
        log.info("CatalogServlet copyDir "+initialContentPath+" to "+contentPath);
      } catch (IOException ioe) {
        log.error("CatalogServlet failed to copyDir "+initialContentPath+" to "+contentPath, ioe);
      } */

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
      //NcDataset.setCacheMax(maxNetcdfFilesCached);

      // set up roots
      //configFilename = contentPath + "catalog.xml";
      //readConfig();

      // debugging actions
      makeDebugActions();

      log.info(" initialized");

    } catch (Throwable t) {
      log.error("CatalogServlet init", t);
      t.printStackTrace();
    }

    // Create the HTML page once
    //catalogPage = doTransform( configURL, "http://www.unidata.ucar.edu/projects/THREDDS/xml/AggServerConfig.0.4.xsl");
  }

  /**
   * ***********************************************************************
   */

  // LOOK can we automate getting the version number ??
  public String getServerVersion() {
    return "dods/3.7";
  }

  // Servlets that support HTTP GET requests and can quickly determine their last modification time should
  // override this method. This makes browser and proxy caches work more effectively, reducing the load on
  // server and network resources.
  protected long getLastModified(HttpServletRequest req) {
    String path = req.getPathInfo();
    if (path == null) return -1;

    if (path.endsWith(".dds"))
      path = path.substring(0, path.length() - 4);
    else if (path.endsWith(".das"))
      path = path.substring(0, path.length() - 4);
    else if (path.endsWith(".dods"))
      path = path.substring(0, path.length() - 5);
    else if (path.endsWith(".html"))
      path = path.substring(0, path.length() - 5);
    else if (path.endsWith(".info"))
      path = path.substring(0, path.length() - 5);
    else
      return -1;

    String filePath = DataRootHandler.getInstance().translatePath(path);
    // @todo Should instead use ((CrawlableDatasetFile)catHandler.findRequestedDataset( path )).getFile();
    if (filePath == null) return -1;
    File file = new File(filePath);
    if (file.exists())
      return file.lastModified();

    return -1;
  }

  /**
   * ********************* debugging *******************
   */

  private void makeDebugActions() {
    DebugHandler debugHandler = new DebugHandler("ncdodsServer");
    DebugHandler.Action act;

    act = new DebugHandler.Action("showStatus", "Show ncdods status") {
      public void doAction(DebugHandler.Event e) {
        try {
          doGetStatus(e.req, e.res);
        }
        catch (Exception ioe) {
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

  /**
   * ************************************************************************
   * PUT requests: save a configuration file.
   * <p/>
   * Request must be of the form
   * config?server=serverName
   *
   * @param req The client's <code> HttpServletRequest</code> request object.
   * @param res The server's <code> HttpServletResponse</code> response object.
   *            <p/>
   *            public void doPut(HttpServletRequest req, HttpServletResponse res)
   *            throws ServletException, IOException {
   *            <p/>
   *            String path = ServletUtil.getRequestPath( req);
   *            if (Debug.isSet("showRequest"))
   *            log.debug("**NcDODSSubset doPut="+ServletUtil.getRequest(req));
   *            if (Debug.isSet("showRequestDetail"))
   *            log.debug( ServletUtil.showRequestDetail(this, req));
   *            <p/>
   *            if (path.equals("/dodsC/catalogConfig.xml")) {
   *            if (ServletUtil.saveFile( this, contentPath, "catalogConfig.xml", req, res)) {
   *            readCatalog();
   *            return; // ok
   *            }
   *            }
   *            <p/>
   *            res.sendError(HttpServletResponse.SC_NOT_FOUND);
   *            }
   */

  public void doGet(HttpServletRequest req, HttpServletResponse res)
          throws IOException, ServletException {

    ServletUtil.logServerAccessSetup(req);

    try {
      String path = req.getPathInfo();
      if (log.isDebugEnabled()) {
        log.debug("doGet path=" + path);
        if (Debug.isSet("showRequestDetail"))
          log.debug(ServletUtil.showRequestDetail(this, req));
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
        ServletUtil.logServerAccess(HttpServletResponse.SC_OK, 0);
        return;

      } else if (path.endsWith("latest.xml")) {
        DataRootHandler.getInstance().processReqForLatestDataset(this, req, res);
        return;

        // need to pass normal "html" on to super class
      } else if (path.endsWith("catalog.xml") || path.endsWith("catalog.html") || path.endsWith("/")) {
        if (DataRootHandler.getInstance().processReqForCatalog( req, res))
          return;
      }

      // default is to throw it to the superclass - this processes the .dds, .das etc
      super.doGet(req, res);
      ServletUtil.logServerAccess(HttpServletResponse.SC_OK, -1); // dunno the length

    } catch (FileNotFoundException e) {
      ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, -1);
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
  protected GuardedDataset getDataset(ReqState preq) throws DODSException, IOException, ParseException {
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
    String spath = req.getServletPath();
    if (spath.length() > 0) {
      if (spath.startsWith("/"))
        spath = spath.substring(1);
      if (!spath.endsWith("/"))
        spath = spath + "/";
    }
    String reqPath = preq.getDataSet(); // was spath + preq.getDataSet();

    NetcdfFile ncd = DatasetHandler.getNetcdfFile(reqPath);
    if (ncd == null) {
      throw new DODSException(DODSException.NO_SUCH_FILE, "Cant find " + reqPath);
    }
    GuardedDatasetImpl gdataset = new GuardedDatasetImpl(reqPath, ncd, acceptSession);

    if (acceptSession) {
      session.setAttribute(GDATASET, gdataset);
      // session.setMaxInactiveInterval(30); // 30 second timeout !!
      if (log.isDebugEnabled()) log.debug(" added gdataset " + gdataset + " in session " + session.getId());
    }

    return gdataset;
  }

}

/* String filePath = CatalogHandler.translatePath( reqPath);
 if (filePath == null)
   throw new DODSException(DODSException.NO_SUCH_FILE , "No data root for "+reqPath);
 org.jdom.Element ncmlElement = CatalogHandler.getNcML( reqPath);

 try {

   log.debug("NcDODSServlet try to acquire Nc = "+reqPath+" access = "+filePath);
   ds = new GuardedDatasetImpl(reqPath, filePath, ncmlElement);
   log.debug("   acquire is ok = "+filePath);

  } catch (FileNotFoundException e) {
    throw e; // normal

  } catch (Throwable t) {
    String errMsg = "NcDODSServlet ERROR opening NcDataset "+reqPath+" access = "+filePath+"\n"+t.getMessage();
    log.error(errMsg, t);
    throw new DODSException(DODSException.CANNOT_READ_FILE, errMsg);
 }

 return ds;
} */

/*  private void addAccess(InvDataset ds) {
 if (ds.hasAccess()) {
   InvAccessImpl access = (InvAccessImpl) ds.getAccess( ServiceType.DODS);
   if (access != null) {
     accessHash.put( access.getUnresolvedUrlName(), access);
     if (Debug.isSet("dods/dataURL"))
       log.debug("Dataset Cache dataURL= ("+access.getUrlPath()+")"+
          "("+access.getUnresolvedUrlName()+")");
   }
 }

 List datasets = ds.getDatasets();
 for (int i=0; i<datasets.size(); i++)
   addAccess( (InvDataset) datasets.get(i));
}

private void showAccessURLS(PrintStream pw, InvDataset ds) {

 if (ds.hasAccess()) {
   InvAccessImpl access = (InvAccessImpl) ds.getAccess( ServiceType.DODS);
   if (access != null)
     showAccess(pw, ds.getName(), access);
 }

 if (ds.hasNestedDatasets()) {
   pw.print("<li> <b>" +ds.getName() + ":</b> ");
   pw.println("<ul> ");
   List datasets = ds.getDatasets();
   for (int i=0; i<datasets.size(); i++) {
     InvDataset cc = (InvDataset) datasets.get(i);
     showAccessURLS( pw, cc);
   }
   pw.println("</ul><br>");
 }
}

private void showAccess(PrintStream pw, String name, InvAccessImpl access) {

 String urlName = access.getUnresolvedUrlName();

 pw.print("<li> <b>" +name + ":</b> ");
 pw.println(" <a href='" + urlName +"'>External URL</a> ");

     // show agg files
 List mdata = access.getDataset().getMetadata( MetadataType.AGGREGATION);
 if (mdata.size() > 0) { // its an agg dataset
   InvMetadata mdatum = (InvMetadata) mdata.get(0);
   java.util.List aggs = (java.util.List) mdatum.getContentObject();

   // run through aggregation elements
   for (int j=0; j<aggs.size(); j++) {
     Aggregation agg = (Aggregation) aggs.get(j);
     pw.print("Aggregation ("+agg.getAggregationType()+") Component File Access URLs:");
     pw.println("<ol> ");
     // run through each file
     java.util.List fileAccessList = agg.getFileAccess();
     for (int i=0; i<fileAccessList.size(); i++) {
       Aggregation.FileAccess fileAccess = (Aggregation.FileAccess) fileAccessList.get(i);
       pw.println("<li>  "+fileAccess.getStandardUrlName());
     }
     pw.println("</ol> ");
   }

 } else if (access.getService().getName().equals("this")) {
   String internalName = NcDataset.getInternalName( access);
   pw.println(" Internal URL = "+internalName);
 }


 pw.println();
} */

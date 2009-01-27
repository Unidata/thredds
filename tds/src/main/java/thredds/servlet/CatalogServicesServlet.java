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

package thredds.servlet;

import thredds.catalog.*;

import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;

import ucar.unidata.util.StringUtil;
import ucar.nc2.util.IO;

/**
 * Catalog Services.
 * These may have parameters "catalog", "dataset", and "cmd"
 * <p/>
 * Map to /catalog.html for catalog display service.
 * Map to /validate for catalog validate service.
 */
public class CatalogServicesServlet extends HttpServlet {
  protected static org.slf4j.Logger log;

  public void init() {
    log = org.slf4j.LoggerFactory.getLogger(getClass());
    UsageLog.setupNonRequestContext();
    log.info("--- initialized " + getClass().getName());
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {

    UsageLog.log.info( UsageLog.setupRequestContext(req));

    try {
      if (Debug.isSet("showRequest"))
        System.out.println("**CatalogServices req=" + ServletUtil.getRequest(req));
      if (Debug.isSet("showRequestDetail"))
        System.out.println("**CatalogServices req=" + ServletUtil.showRequestDetail(this, req));

      //// common code to all catalog services

      // get the requested catalog URI
      String htmlService = ServletUtil.getRequestBase(req); // this is the base of the request
      URI reqURI = new URI(htmlService); // current request as a URI
      String targetCatalog = req.getParameter("catalog");

      // Determine if requested catalog is local to webapp.
      boolean isLocalCat = false;

      URI catURI; // requested catalog as a URI
      InvCatalogImpl catalog = null;
      if ((targetCatalog == null) || (targetCatalog.length() == 0)) {
        isLocalCat = true;
        catURI = reqURI.resolve("catalog.xml"); // default catalog
        catalog = (InvCatalogImpl) DataRootHandler.getInstance().getCatalog("catalog.xml", catURI);
      } else {
        if (targetCatalog.endsWith("/")) {
          targetCatalog += "catalog.xml";
        } else if (targetCatalog.endsWith(".html")) { // be lenient in what you accept
          int len = targetCatalog.length();
          targetCatalog = targetCatalog.substring(0, len - 4) + "xml";
        }

        // Determine the catalog to handle whether target is local to webapp or not.
        URI targetUri = new URI(targetCatalog);
        catURI = reqURI.resolve(targetUri);
        // Check if target scheme is same as request URI.
        String scheme = catURI.getScheme();
        if (scheme != null && scheme.equalsIgnoreCase(reqURI.getScheme())) {
          // Check if target host is same as request URI.
          String host = catURI.getHost();
          if (host != null && host.equalsIgnoreCase(reqURI.getHost())) {
            // Check if target port is same as request URI.
            if (catURI.getPort() == reqURI.getPort()) {
              // Check if target path starts with the context path of the request being handled.
              String path = catURI.getPath();
              if (path != null && path.startsWith(req.getContextPath())) {
                isLocalCat = true;
                // Remove context path plus trailing slash.
                String catPath = path.substring(req.getContextPath().length() + 1);
                // Remove CatalogServlet path if path starts with it
                if (catPath.startsWith("catalog/"))
                  catPath = catPath.substring("catalog/".length());
                catalog = (InvCatalogImpl) DataRootHandler.getInstance().getCatalog(catPath, catURI);
              }
            }
          }
        }
      }

      // at this point, we really must insist that the catalog ends with an xml

      // For catalogs not local to this webapp, read catalog from external URL.
      if (catalog == null) {
        // parse the catalog
        InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);
        try {
          catalog = catFactory.readXML(catURI);
        } catch (Throwable t) {
          // assume its a malformed catalog, dont log the error message
          res.sendError(HttpServletResponse.SC_BAD_REQUEST, t.getMessage());
          return;
        }
      }

      boolean isHtmlReq = req.getServletPath().endsWith(".html");
      handleCatalogServiceRequest(catalog, catURI, isHtmlReq, isLocalCat, req, res);

    } catch (Throwable t) {
      log.error("doGet req= " + ServletUtil.getRequest(req) + " got Exception", t);
      ServletUtil.handleException(t, res);
    }
  }

  /**
   * Handle requests for the various catalog services:
   * <p/>
   * <ul>
   * <li> convert</li>
   * <li> show</li>
   * <li> subset</li>
   * <li> validate</li>
   * </ul>
   *
   * @param catalog request is for this catalog
   * @param catURI the URI of the catalog
   * @param isHtmlReq true if the request ends in "html"
   * @param isLocalCat true id the catalog is local
   * @param req the request
   * @param res the response
   * @throws IOException on read error
   */
  public static void handleCatalogServiceRequest(InvCatalogImpl catalog, URI catURI, boolean isHtmlReq, boolean isLocalCat,
                                                 HttpServletRequest req, HttpServletResponse res)
      throws IOException {

    // check for fatal errors
    StringBuilder validateMess = new StringBuilder();
    boolean debug = "true".equals(req.getParameter("debug"));
    catalog.check(validateMess, debug);
    boolean isFatal = catalog.hasFatalError();
    if (isFatal) {
      res.setHeader("Validate", "FAIL");
      sendValidationError(catURI.toString(), validateMess.toString(), res, HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    res.setHeader("Validate", "OK");

    // now figure out what they want
    String cmd = req.getParameter("cmd");
    String datasetID = req.getParameter("dataset");
    if (cmd == null) {
      cmd = (datasetID == null) ? "show" : "subset";
    }

    // check if they want to show as html
    if (cmd.equals("show")) {
      HtmlWriter.getInstance().writeCatalog(res, catalog, isLocalCat); // show catalog as HTML
      return;
    }

    // check if they want to see validation results
    if (cmd.equals("validate")) {
      sendMesssage(catURI.toString(), validateMess.toString(), res, HttpServletResponse.SC_OK);
      return;
    }

    // check if they want to convert
    if (cmd.equals("convert")) {
      doConvert(catalog, res);
      ServletUtil.logServerAccess(HttpServletResponse.SC_OK, -1);
      return;
    }

    // check if they want to subset
    if (cmd.equals("subset")) {

      // find the dataset
      if (datasetID == null) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Must have a dataset parameter");
        ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, -1);
        return;
      }
      InvDataset dataset = catalog.findDatasetByID(datasetID);
      if (dataset == null) {
        log.warn("Cant find dataset=" + datasetID + " in catalog=" + catURI);
        ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, -1);
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cant find dataset=" + datasetID);
        return;
      }
      /* if ( dataset.getResourceControl() != null )
      {
        if ( !req.isUserInRole( dataset.getResourceControl() ) )
        {
          ServletUtil.logServerAccess( HttpServletResponse.SC_UNAUTHORIZED, -1 );
          res.sendError( HttpServletResponse.SC_UNAUTHORIZED, "Need role=" + dataset.getResourceControl() );
          return;
        }
      } */

      // html or xml ?
      if (isHtmlReq) {
        showDataset(catURI.toString(), (InvDatasetImpl) dataset, req, res); // show dataset as HTML
      } else {
        catalog.subset(dataset); // subset the catalog

        // Return catalog as XML response.
        InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(false);
        String result = catFactory.writeXML(catalog);
        ServletUtil.logServerAccess(HttpServletResponse.SC_OK, result.length());

        res.setContentLength(result.length());
        res.setContentType("text/xml");
        res.getOutputStream().write(result.getBytes());
      }

      return;
    } // subset

    // dont know what this command is
    ServletUtil.logServerAccess(HttpServletResponse.SC_BAD_REQUEST, -1);
    res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown command=" + cmd);

  }

  private static void doConvert(InvCatalogImpl catalog, HttpServletResponse res) {
    catalog.setCatalogConverterToVersion1();

    try {
      OutputStream os = res.getOutputStream();
      res.setContentType("text/xml");
      res.setStatus(HttpServletResponse.SC_OK);
      catalog.writeXML(os);
      os.flush();
    } catch (java.io.IOException ioe) {
      ServletUtil.handleException(ioe, res);
    }
  }

  static private void sendMesssage(String catURL, String mess, HttpServletResponse res, int status) throws IOException {
    res.setStatus(status);
    res.setContentType("text/html");
    StringBuilder sb = new StringBuilder(10000);

    sb.append(HtmlWriter.getInstance().getHtmlDoctypeAndOpenTag());
    sb.append("<head>\r\n");
    sb.append("<title> Catalog Services</title>\r\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html\">\r\n");
    sb.append(HtmlWriter.getInstance().getTdsPageCssLink());
    sb.append("</head>\r\n");
    sb.append("<body>\r\n");
    sb.append(HtmlWriter.getInstance().getUserHead());

    sb.append("<h2> Catalog ").append(catURL).append(" :</h2>\r\n");

    sb.append("<b>\r\n");
    sb.append(StringUtil.quoteHtmlContent(mess));
    sb.append("</b>\r\n");

    sb.append("</body>\r\n");
    sb.append("</html>\r\n");

    PrintWriter pw = new PrintWriter(res.getOutputStream());
    pw.write(sb.toString());
    pw.flush();

    ServletUtil.logServerAccess(status, sb.length());
  }

  static private void sendValidationError(String catURL, String mess, HttpServletResponse res, int status) throws IOException {
    res.setStatus(status);
    res.setContentType("text/html");
    StringBuilder sb = new StringBuilder(10000);

    sb.append(HtmlWriter.getInstance().getHtmlDoctypeAndOpenTag());
    sb.append("<head>\r\n");
    sb.append("<title> Catalog Services</title>\r\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html\">\r\n");
    sb.append(HtmlWriter.getInstance().getTdsPageCssLink());
    sb.append("</head>\r\n");
    sb.append("<body>\r\n");
    sb.append(HtmlWriter.getInstance().getUserHead());

    sb.append("<h2> Catalog ").append(catURL).append(" has fatal errors:</h2>\r\n");

    sb.append("<b>\r\n");
    sb.append(StringUtil.quoteHtmlContent(mess));
    sb.append("</b>\r\n");

     // show catalog.xml
    sb.append("<hr><pre>\r\n");
    try {
      String catString = IO.readURLcontentsWithException( catURL);
      sb.append( StringUtil.quoteHtmlContent(catString)+"\r\n");
    } catch (Exception ee) {
      log.warn("Error reading URL= "+catURL);
      sb.append("Error reading URL= ");
      sb.append(catURL);
      sb.append("; err=");
      sb.append(ee.getMessage());
      sb.append("\r\n");
    }
    sb.append("</pre>\r\n");

    sb.append("</body>\r\n");
    sb.append("</html>\r\n");

    PrintWriter pw = new PrintWriter(res.getOutputStream());
    pw.write(sb.toString());
    pw.flush();

    ServletUtil.logServerAccess(status, sb.length());
  }

  static private void showDataset(String catURL, InvDatasetImpl dataset, HttpServletRequest req, HttpServletResponse res) throws IOException {
    res.setContentType("text/html");
    StringBuilder sb = new StringBuilder(10000);

    sb.append(HtmlWriter.getInstance().getHtmlDoctypeAndOpenTag());
    sb.append("<head>\r\n");
    sb.append("<title> Catalog Services</title>\r\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html\">\r\n");
    sb.append(HtmlWriter.getInstance().getTdsPageCssLink());
    sb.append("</head>\r\n");
    sb.append("<body>\r\n");
    sb.append(HtmlWriter.getInstance().getUserHead());

    sb.append("<h2> Catalog ").append(catURL).append("</h2>\r\n");

    InvDatasetImpl.writeHtmlDescription(sb, dataset, false, true, false, false);

    // optional access through Viewers
    ViewServlet.showViewers(sb, dataset, req);

    sb.append("</body>\r\n");
    sb.append("</html>\r\n");

    PrintWriter pw = new PrintWriter(res.getOutputStream());
    pw.write(sb.toString());
    pw.flush();

    ServletUtil.logServerAccess(HttpServletResponse.SC_OK, sb.length());
  }


}

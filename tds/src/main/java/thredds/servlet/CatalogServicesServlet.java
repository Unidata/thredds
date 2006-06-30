// $Id: CatalogServicesServlet.java,v 1.4 2006/05/31 21:46:17 caron Exp $
/*
 * Copyright 2002 Unidata Program Center/University Corporation for
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

package thredds.servlet;

import thredds.catalog.*;
import thredds.util.*;

import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;

import ucar.unidata.util.StringUtil;

/**
 * Catalog Services.
 * These may have parameters "catalog", "dataset", and "cmd"
 *
 * Map to /catalog.html for catalog display service.
 * Map to /validate for catalog validate service.
 *
 */
public class CatalogServicesServlet extends HttpServlet {
  protected static org.slf4j.Logger log;

  public void init() {
    ServletUtil.initLogging(this);
    log = org.slf4j.LoggerFactory.getLogger(getClass());
    ServletUtil.logServerSetup( this.getClass().getName() + ".init()" );
    log.info("--- initialized "+getClass().getName());
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
                             throws ServletException, IOException {

    ServletUtil.logServerAccessSetup( req );

    try {
      if (Debug.isSet("showRequest"))
        System.out.println("**CatalogServices req=" + ServletUtil.getRequest(req));
      if (Debug.isSet("showRequestDetail"))
        System.out.println( "**CatalogServices req=" + ServletUtil.showRequestDetail(this, req));

      //// common code to all catalog services

      // get the requested catalog URI
      String htmlService = ServletUtil.getRequestBase( req); // this is the base of the request
      URI reqURI = new URI( htmlService); // current request as a URI
      String requestedCatalog = req.getParameter("catalog");

      URI catURI; // requested catalog as a URI
      if ((requestedCatalog == null) || (requestedCatalog.length() == 0)) {
        catURI = reqURI.resolve( "catalog.xml"); // default catalog
      } else if (requestedCatalog.endsWith("/")) {
        catURI = reqURI.resolve( new URI( requestedCatalog+ "catalog.xml"));
      } else if (requestedCatalog.endsWith(".html")) { // be lenient in what you accept
        int len = requestedCatalog.length();
        requestedCatalog = requestedCatalog.substring(0, len-4) + "xml";
        catURI = reqURI.resolve( new URI( requestedCatalog));
      } else {
        catURI = reqURI.resolve( new URI( requestedCatalog)); // deals with possible reletive URL
      }

      // at this point, we really must insist that the catalog ends with an xml

     // LOOK need object cache to keep the catalogs !!
      // parse the catalog
      InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);
      InvCatalogImpl catalog;
      try {
        catalog = catFactory.readXML( catURI);
      } catch (Throwable t) {
        // assume its a malformed catalog, dont log the error message
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, t.getMessage());
        return;
      }

      boolean isHtmlReq = req.getServletPath().endsWith( ".html" );
      handleCatalogServiceRequest( catalog, catURI, isHtmlReq, req, res );

    } catch (Throwable t) {
      log.error("doGet req= "+ServletUtil.getRequest(req)+" got Exception", t);
      ServletUtil.handleException( t,  res);
    }
  }

  /**
   * Handle requests for the various catalog services:
   *
   * <ul>
   * <li> convert</li>
   * <li> show</li>
   * <li> subset</li>
   * <li> validate</li>
   * </ul>
   *
   * @param catalog
   * @param catURI
   * @param req
   * @param res
   * @throws IOException
   */
  public static void handleCatalogServiceRequest( InvCatalogImpl catalog, URI catURI, boolean isHtmlReq,
                                                  HttpServletRequest req, HttpServletResponse res )
          throws IOException
  {
    // check for fatal errors
    StringBuffer validateMess = new StringBuffer();
    boolean debug = "true".equals( req.getParameter( "debug" ) );
    catalog.check( validateMess, debug );
    boolean isFatal = catalog.hasFatalError();
    if ( isFatal )
    {
      res.setHeader( "Validate", "FAIL" );
      sendValidationError( catURI.toString(), validateMess.toString(), res, HttpServletResponse.SC_NOT_FOUND );
      return;
    }
    res.setHeader( "Validate", "OK" );

    // now figure out what they want
    String cmd = req.getParameter( "cmd" );
    String datasetID = req.getParameter( "dataset" );
    if ( cmd == null )
    {
      cmd = ( datasetID == null ) ? "show" : "subset";
    }

    // check if they want to show as html
    if ( cmd.equals( "show" ) )
    {
      HtmlWriter2.getInstance().writeCatalog( res, catalog, false ); // show catalog as HTML
      return;
    }

    // check if they want to see validation results
    if ( cmd.equals( "validate" ) )
    {
      sendMesssage( catURI.toString(), validateMess.toString(), res, HttpServletResponse.SC_OK );
      return;
    }

    // check if they want to convert
    if ( cmd.equals( "convert" ) )
    {
      doConvert( catalog, res );
      ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1 );
      return;
    }

    // check if they want to subset
    if ( cmd.equals( "subset" ) )
    {

      // find the dataset
      if ( datasetID == null )
      {
        res.sendError( HttpServletResponse.SC_BAD_REQUEST, "Must have a dataset parameter" );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1 );
        return;
      }
      InvDataset dataset = catalog.findDatasetByID( datasetID );
      if ( dataset == null )
      {
        log.warn( "Cant find dataset=" + datasetID + " in catalog=" + catURI );
        ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1 );
        res.sendError( HttpServletResponse.SC_BAD_REQUEST, "Cant find dataset=" + datasetID );
        return;
      }
      if ( dataset.getResourceControl() != null )
      {
        if ( !req.isUserInRole( dataset.getResourceControl() ) )
        {
          ServletUtil.logServerAccess( HttpServletResponse.SC_UNAUTHORIZED, -1 );
          res.sendError( HttpServletResponse.SC_UNAUTHORIZED, "Need role=" + dataset.getResourceControl() );
          return;
        }
      }

      // html or xml ?
      if ( isHtmlReq )
      {
        showDataset( catURI.toString(), (InvDatasetImpl) dataset, res ); // show dataset as HTML
      }
      else
      {
        catalog.subset( dataset ); // subset the catalog
        OutputStream os = res.getOutputStream();
        res.setContentType( "text/xml" );
        catalog.writeXML( os );
        ServletUtil.logServerAccess( HttpServletResponse.SC_OK, -1 );
      }

      return;
    } // subset

    // dont know what this command is
    ServletUtil.logServerAccess( HttpServletResponse.SC_BAD_REQUEST, -1 );
    res.sendError( HttpServletResponse.SC_BAD_REQUEST, "Unknown command=" + cmd );

  }

  private static void doConvert( InvCatalogImpl catalog, HttpServletResponse res) {
    catalog.setCatalogConverterToVersion1();

    try {
      OutputStream os = res.getOutputStream();
      res.setContentType("text/xml");
      catalog.writeXML(os);
      res.setStatus(HttpServletResponse.SC_OK);
      os.flush();
    } catch (java.io.IOException ioe) {
      ServletUtil.handleException(ioe, res);
    }
  }

  static private void sendMesssage(String catURL, String mess, HttpServletResponse res, int status) throws IOException {
    res.setStatus(status);
    res.setContentType("text/html");
    StringBuffer sb = new StringBuffer(10000);

    sb.append("<html>\r\n");
    sb.append("<head>\r\n");
    sb.append("<title> Catalog Services</title>\r\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html\">\r\n");
    sb.append(HtmlWriter2.getInstance().getUserCSS());
    sb.append("</head>\r\n");
    sb.append("<body>\r\n");
    sb.append(HtmlWriter2.getInstance().getUserHead());

    sb.append("<h2> Catalog " + catURL+" :</h2>\r\n");

    sb.append("<b>\r\n");
    sb.append( StringUtil.quoteHtmlContent(mess));
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
    StringBuffer sb = new StringBuffer(10000);

    sb.append("<html>\r\n");
    sb.append("<head>\r\n");
    sb.append("<title> Catalog Services</title>\r\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html\">\r\n");
    sb.append(HtmlWriter2.getInstance().getUserCSS());
    sb.append("</head>\r\n");
    sb.append("<body>\r\n");
    sb.append(HtmlWriter2.getInstance().getUserHead());

    sb.append("<h2> Catalog " + catURL+" has fatal errors:</h2>\r\n");

    sb.append("<b>\r\n");
    sb.append( StringUtil.quoteHtmlContent(mess));
    sb.append("</b>\r\n");

    // show catalog.xml
    sb.append("<hr><pre>\r\n");
    String catString = IO.readURLcontents( catURL);
    sb.append( StringUtil.quoteHtmlContent(catString)+"\r\n");
    sb.append("</pre>\r\n");

    sb.append("</body>\r\n");
    sb.append("</html>\r\n");

    PrintWriter pw = new PrintWriter(res.getOutputStream());
    pw.write(sb.toString());
    pw.flush();

    ServletUtil.logServerAccess(status, sb.length());
  }

  static private void showDataset(String catURL, InvDatasetImpl dataset, HttpServletResponse res) throws IOException {
    res.setContentType("text/html");
    StringBuffer sb = new StringBuffer(10000);

    sb.append("<html>\r\n");
    sb.append("<head>\r\n");
    sb.append("<title> Catalog Services</title>\r\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html\">\r\n");
    sb.append(HtmlWriter2.getInstance().getUserCSS());
    sb.append("</head>\r\n");
    sb.append("<body>\r\n");
    sb.append(HtmlWriter2.getInstance().getUserHead());

    sb.append("<h2> Catalog " + catURL+"</h2>\r\n");

    InvDatasetImpl.writeHtmlDescription( sb, dataset, false, true, false, false);

    // optional access through Viewers
    ViewServlet.showViewers( sb, dataset);

    sb.append("</body>\r\n");
    sb.append("</html>\r\n");

    PrintWriter pw = new PrintWriter(res.getOutputStream());
    pw.write(sb.toString());
    pw.flush();

    ServletUtil.logServerAccess(HttpServletResponse.SC_OK, sb.length());
  }


}

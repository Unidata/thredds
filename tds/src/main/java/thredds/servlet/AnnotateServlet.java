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
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

import ucar.nc2.util.IO;

/**
 * Servlet handles catalog annotation.
 * Not currently used.
 */
public class AnnotateServlet extends AbstractServlet {
  protected String getPath() {
    return "views/";
  }

  protected void makeDebugActions() {
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {

    log.info( UsageLog.setupRequestContext(req));

    try {
      if (Debug.isSet("showRequest"))
        log.debug("**CatalogAnnotate req=" + ServletUtil.getRequest(req));
      if (Debug.isSet("showRequestDetail"))
        log.debug(ServletUtil.showRequestDetail(this, req));

      // see if it has a catalog parameter
      String catURL = req.getParameter("catalog");
      boolean isDefaultCatalog = (catURL == null) || (catURL.length() == 0);
      if (isDefaultCatalog) {
        String reqBase = ServletUtil.getRequestBase(req); // this is the base of the request
        URI reqURI = new URI(reqBase);
        URI catURI = reqURI.resolve("catalog.xml");
        catURL = catURI.toString();
      }
      if (Debug.isSet("showCatalog"))
        log.debug("CatalogServices: catalog = " + catURL);

      String datasetID;
      // see if it has a dataset parameter
      datasetID = req.getParameter("dataset");
      if (datasetID == null) {
        log.error("CatalogAnnotate: must have dataset query parameter" + ServletUtil.getRequest(req));
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "must have dataset query parameter");
        return;
      } else {
        if (Debug.isSet("showCatalog"))
          log.debug("CatalogAnnotate: dataset = " + datasetID);
      }

      res.setStatus(HttpServletResponse.SC_OK);
      PrintWriter pw = new PrintWriter(res.getOutputStream());

      res.setContentType("text/html");
      pw.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n");
      pw.println("        \"http://www.w3.org/TR/html4/loose.dtd\">\n");
      pw.println("<html>\n");
      pw.println("<head>");
      pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
      pw.println("</head>");
      pw.println("<body bgcolor=\"#FFF0FF\">");
      pw.println("<img src='/thredds/thredds.jpg' >");
      pw.println("<h2> Catalog (" + catURL + ")</h2>");
      pw.println("<h3> Dataset (" + datasetID + ")</h3><ul>");

      List<Annotation> views = Annotation.findAnnotation(catURL, datasetID, "IDV");
      for (Annotation v : views) {
        String href = getPath() + v.jnlpFilename;
        pw.println("<li> <a href='" + href + "'>" + v.title + "</a> " + v.desc);
      }

      pw.println("</ul></body></html>");

      pw.flush();

    } catch (Throwable t) {
      log.error("doGet req= " + ServletUtil.getRequest(req) + " got Exception", t);
      ServletUtil.handleException(t, res);
    }
  }

  public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {

    log.info( UsageLog.setupRequestContext(req));

    try {
      if (Debug.isSet("showRequest"))
        log.debug("**CatalogAnnotate post=" + ServletUtil.getRequest(req));
      if (Debug.isSet("showRequestDetail"))
        log.debug(ServletUtil.showRequestDetail(this, req));

      // see if it has a catalog parameter
      String catURL = req.getParameter("catalog");
      boolean isDefaultCatalog = (catURL == null) || (catURL.length() == 0);
      if (isDefaultCatalog) {
        String reqBase = ServletUtil.getRequestBase(req); // this is the base of the request
        URI reqURI = new URI(reqBase);
        URI catURI = reqURI.resolve("catalog.xml");
        catURL = catURI.toString();
      }
      if (Debug.isSet("showCatalog"))
        log.debug("CatalogServices: catalog = " + catURL);

      String datasetID;
      // see if it has a dataset parameter
      datasetID = req.getParameter("dataset");
      if (datasetID == null) {
        log.error("CatalogAnnotate: must have dataset query parameter" + ServletUtil.getRequest(req));
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "must have dataset query parameter");
        return;
      } else {
        if (Debug.isSet("showCatalog"))
          log.debug("CatalogAnnotate: dataset = " + datasetID);
      }

      // parse the catalog
      InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory(true);
      InvCatalogImpl catalog;
      try {
        catalog = catFactory.readXML(catURL);
      } catch (Exception e) {
        ServletUtil.handleException(e, res);
        return;
      }

      PrintWriter pw = new PrintWriter(res.getOutputStream());

      String jnlpString = req.getParameter("jnlp");
      File dir = new File(contentPath);
      try {
        File jnlpFile = File.createTempFile("IDV", ".jnlp", dir);
        IO.writeToFile(jnlpString, jnlpFile);

        String title = req.getParameter("title");
        String desc = req.getParameter("description");
        Annotation.add(new Annotation(catURL, datasetID, title, desc, jnlpFile.getName(), "IDV"));
        res.setStatus(HttpServletResponse.SC_OK);
        pw.println("\nOK");

      } catch (IOException ioe) {
        ioe.printStackTrace();
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "ERROR= " + ioe);
      }

      pw.flush();

    } catch (Throwable t) {
      log.error("doPost req= " + ServletUtil.getRequest(req) + " got Exception", t);
      ServletUtil.handleException(t, res);
    }
  }

  /* private class View {
    String catalogURL;
    String datasetID;
    String title, desc;
    String jnlpFilename;

    View( String catalogURL, String datasetID, String title, String desc, String jnlpFilename) {
      this.catalogURL = catalogURL;
      this.datasetID = datasetID;
      this.title = title;
      this.desc = desc;
      this.title = title;
      this.jnlpFilename = jnlpFilename;

      System.out.println("View= "+this);
    }

    public String toString() {
      return "catalog= ("+catalogURL+") dataset= ("+datasetID+") title= "+title+" desc= "+desc+" jnlpFilename="+jnlpFilename;
    }
  } */

}

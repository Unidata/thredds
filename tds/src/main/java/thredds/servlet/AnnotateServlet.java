// $Id: AnnotateServlet.java 51 2006-07-12 17:13:13Z caron $
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

package thredds.servlet;

import thredds.catalog.*;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * Servlet handles catalog annotation.
 * Not currently used.
 *
 */
public class AnnotateServlet extends AbstractServlet {
  protected String getPath() { return "views/"; }
  protected void makeDebugActions() { }

  public void doGet(HttpServletRequest req, HttpServletResponse res)
                             throws ServletException, IOException {

    ServletUtil.logServerAccessSetup( req );

    try {
      if (Debug.isSet("showRequest"))
        log.debug("**CatalogAnnotate req=" + ServletUtil.getRequest(req));
      if (Debug.isSet("showRequestDetail"))
        log.debug( ServletUtil.showRequestDetail(this, req));

      // see if it has a catalog parameter
      String catURL = req.getParameter("catalog");
      boolean isDefaultCatalog = (catURL == null) || (catURL.length() == 0);
      if (isDefaultCatalog) {
        String reqBase = ServletUtil.getRequestBase( req); // this is the base of the request
        URI reqURI = new URI( reqBase);
        URI catURI = reqURI.resolve( "catalog.xml");
        catURL = catURI.toString();
      }
      if (Debug.isSet("showCatalog"))
        log.debug("CatalogServices: catalog = "+catURL);

      String datasetID = null;
      // see if it has a dataset parameter
      datasetID = req.getParameter("dataset");
      if (datasetID == null) {
        log.error("CatalogAnnotate: must have dataset query parameter" +ServletUtil.getRequest(req));
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "must have dataset query parameter");
        return;
      } else {
        if (Debug.isSet("showCatalog"))
          log.debug("CatalogAnnotate: dataset = "+datasetID);
      }

     PrintWriter pw = new PrintWriter(res.getOutputStream());

        res.setContentType("text/html");
        pw.println( "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" );
        pw.println( "        \"http://www.w3.org/TR/html4/loose.dtd\">\n" );
        pw.println( "<html>\n" );
        pw.println("<head>");
        pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
        pw.println("</head>");
        pw.println("<body bgcolor=\"#FFF0FF\">");
        pw.println("<img src='/thredds/thredds.jpg' >");
        pw.println("<h2> Catalog (" + catURL+")</h2>");
        pw.println("<h3> Dataset (" + datasetID+")</h3><ul>");

        List views = Annotation.findAnnotation(catURL, datasetID, "IDV");
        for (int i=0; i<views.size(); i++) {
          Annotation v = (Annotation) views.get(i);
          String href = getPath() + v.jnlpFilename;
          pw.println("<li> <a href='" + href + "'>" + v.title + "</a> "+ v.desc);
        }

        pw.println("</ul></body></html>");

      pw.flush();
      res.setStatus( HttpServletResponse.SC_OK);

    } catch (Throwable t) {
      log.error("doGet req= "+ServletUtil.getRequest(req)+" got Exception", t);
      ServletUtil.handleException( t,  res);
    }
  }

  public void doPost(HttpServletRequest req, HttpServletResponse res)
                             throws ServletException, IOException {

    ServletUtil.logServerAccessSetup( req );

    try {
      if (Debug.isSet("showRequest"))
        log.debug("**CatalogAnnotate post=" + ServletUtil.getRequest(req));
      if (Debug.isSet("showRequestDetail"))
        log.debug( ServletUtil.showRequestDetail(this, req));

      // see if it has a catalog parameter
      String catURL = req.getParameter("catalog");
      boolean isDefaultCatalog = (catURL == null) || (catURL.length() == 0);
      if (isDefaultCatalog) {
        String reqBase = ServletUtil.getRequestBase( req); // this is the base of the request
        URI reqURI = new URI( reqBase);
        URI catURI = reqURI.resolve( "catalog.xml");
        catURL = catURI.toString();
      }
      if (Debug.isSet("showCatalog"))
        log.debug("CatalogServices: catalog = "+catURL);

      String datasetID = null;
      // see if it has a dataset parameter
      datasetID = req.getParameter("dataset");
      if (datasetID == null) {
        log.error("CatalogAnnotate: must have dataset query parameter" +ServletUtil.getRequest(req));
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, "must have dataset query parameter");
        return;
      } else {
        if (Debug.isSet("showCatalog"))
          log.debug("CatalogAnnotate: dataset = "+datasetID);
      }

      // parse the catalog
      InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( true);
      InvCatalogImpl catalog = null;
      try {
        catalog = (InvCatalogImpl) catFactory.readXML(catURL);
      } catch (Exception e) {
        ServletUtil.handleException( e,  res);
        return;
      }

      PrintWriter pw = new PrintWriter(res.getOutputStream());

      String jnlpString = req.getParameter("jnlp");
      File dir = new File(contentPath);
      try {
        File jnlpFile = File.createTempFile("IDV", ".jnlp", dir);
        thredds.util.IO.writeToFile(jnlpString, jnlpFile);

        String title = req.getParameter("title");
        String desc = req.getParameter("description");
        Annotation.add( new Annotation(catURL, datasetID, title, desc, jnlpFile.getName(), "IDV"));
        pw.println("\nOK");
        res.setStatus( HttpServletResponse.SC_OK);

      } catch (IOException ioe) {
        ioe.printStackTrace();
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "ERROR= "+ioe);
      }

      pw.flush();

    } catch (Throwable t) {
      log.error("doPost req= "+ServletUtil.getRequest(req)+" got Exception", t);
      ServletUtil.handleException( t,  res);
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

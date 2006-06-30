// $Id: ViewServlet.java,v 1.3 2006/05/22 17:29:26 caron Exp $
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

import ucar.unidata.util.StringUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.ArrayList;

import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvAccess;
import thredds.catalog.ServiceType;
import thredds.catalog.DataType;

/**
 * Catalog Serving
 *
 * handles /view/*
 */
public class ViewServlet extends AbstractServlet {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ViewServlet.class);
  static private HashMap templates = new HashMap();
  static private ArrayList viewerList;
  static {
    viewerList = new ArrayList();
    registerViewer( new IDV());
    registerViewer( new Nj22ToolsUI());
  }

  static public void registerViewer(Viewer v) {
    viewerList.add( v);
  }

  static private String getTemplate( String path) {
    String template = (String) templates.get( path);
    if (template != null) return template;

    try {
      template = thredds.util.IO.readFile(path);
    } catch (IOException ioe) {
      return null;
    }

    templates.put( path, template);
    return template;
  }

  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    ServletUtil.logServerAccessSetup( req );

    String path = req.getPathInfo();
    int pos = path.lastIndexOf("/");
    String filename = "views/" + path.substring(pos);
    log.debug("**ViewManager req= "+path+" look for "+rootPath + filename);

    String template = getTemplate( rootPath + filename);
    if (template == null)
      template = getTemplate( contentPath + filename);
    if (template == null) {
      ServletUtil.logServerAccess( HttpServletResponse.SC_NOT_FOUND, 0 );
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    StringBuffer sbuff = new StringBuffer( template);

    Enumeration params = req.getParameterNames();
    while (params.hasMoreElements()) {
      String name = (String) params.nextElement();
      String values[] = req.getParameterValues(name);
      if (values != null) {
        String sname = "{"+name+"}";
        for (int i = 0; i < values.length; i++) {
          StringUtil.substitute( sbuff, sname, values[i]); // multiple ok
        }
      }
    }

    try {
      res.setContentType("application/x-java-jnlp-file");
      ServletUtil.returnString(sbuff.toString(), res);
      //System.out.println(" jnlp="+sbuff.toString());

    } catch (Throwable t) {
      log.error(" jnlp="+sbuff.toString(), t);
      ServletUtil.logServerAccess( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 0 );
      res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  static public void showViewers( StringBuffer sbuff, InvDatasetImpl dataset) {
    int count = 0;
    for (int i = 0; i < viewerList.size(); i++) {
      Viewer viewer = (Viewer) viewerList.get(i);
      if (viewer.isViewable( dataset)) count ++;
    }
    if (count == 0) return;

    sbuff.append("<h3>Viewers:</h3><ul>\r\n");
    for (int i = 0; i < viewerList.size(); i++) {
      Viewer viewer = (Viewer) viewerList.get(i);
      if (viewer.isViewable( dataset)) {
        sbuff.append("  <li> ");
        sbuff.append( viewer.getViewerLinkHtml( dataset));
        sbuff.append("\n");
      }
    }
    sbuff.append("</ul>\r\n");
  }

  /* static private void showViews( StringBuffer sbuff, InvDatasetImpl dataset, String viewer) {
    List list = View.getViews(); // findViews( dataset.getParentCatalog().getUriString(), dataset.getID(), viewer);
    if (list.size() == 0) return;
    //sbuff.append("<h4>Contributed Views:</h4>\n<ol>\n");
    sbuff.append("<ul>\n");
    for (int i=0; i<list.size(); i++) {
      View v = (View) list.get(i);
      v.writeHtml( sbuff);
    }
    sbuff.append("\n</ul><p>\n");
  } */

  // must end with "/"
  protected String getPath() { return "view/";  }
  protected void makeDebugActions() { }

  private static class Nj22ToolsUI implements Viewer {

    public  boolean isViewable( InvDatasetImpl ds) {
      String id = ds.getID();
      return ((id != null) && ds.hasAccess());
    }

    public String  getViewerLinkHtml( InvDatasetImpl ds) {
      // LOOK use getContextName instead of hardcodeing thredds
      return "<a href='/thredds/view/nj22UI.jnlp?" + ds.getSubsetUrl()+"'>NetCDF-Java Tools (webstart)</a>";
    }

  }

  private static class IDV implements Viewer {

    public boolean isViewable( InvDatasetImpl ds) {
      InvAccess access = ds.getAccess(ServiceType.DODS);
      if (access == null) access = ds.getAccess(ServiceType.OPENDAP);
      if (access == null) return false;

      DataType dt = ds.getDataType();
      if (dt != DataType.GRID) return false;
      return true;
    }

    public String getViewerLinkHtml( InvDatasetImpl ds) {
      InvAccess access = ds.getAccess(ServiceType.DODS);
      if (access == null) access = ds.getAccess(ServiceType.OPENDAP);

      // LOOK use getContextName instead of hardcodeing thredds
      return "<a href='/thredds/view/idv.jnlp?url="+access.getStandardUrlName()+"'>Integrated Data Viewer (IDV) (webstart)</a>";
    }

  }

}

// $Id$
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

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Format;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.net.URI;

import thredds.catalog.*;

/**
 * Most HTML writing goes through here.
 *
 * NOTE:
 * Need to be able to modify the following items:
 * <ul>
 * <li>Context path: "/thredds"</li>
 * <li>Servlet name: "THREDDS Data Server"</li>
 * <li>Documentation location: "/thredds/docs/"</li>
 * <li>Version information: ThreddsDefault.version</li>
 * <li>Catalog reference URL: "/thredds/catalogServices?catalog="</li>
 * </ul>
 *
 * @author stolen from tomcat 
 * @author john
 */
public class HtmlWriter {
  public static final String UNIDATA_CSS = "<link rel='stylesheet' href='/thredds/upc.css' type='text/css' />";

  public static final String UNIDATA_HEAD =
      "<table width=\"100%\">\n" +
      "    <tr>\n" +
      "        <td width=\"95\" height=\"95\" align=\"left\"><img src=\"/thredds/unidataLogo.gif\" width=\"95\" height=\"93\"> </td>\n" +
      "        <td width=\"701\" align=\"left\" valign=\"top\">\n" +
      "            <table width=\"303\">\n" +
      "                <tr>\n" +
      "                  <td width=\"295\" height=\"22\" align=\"left\" valign=\"top\"><h3><strong>Thredds Data Server</strong></h3></td>\n" +
      "                </tr>\n" +
      "            </table>\n" +
      "        </td>\n" +
      "    </tr>\n" +
      "</table>";

  private static final String TOMCAT_CSS =
      "H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} " +
      "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} " +
      "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} " +
      "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} " +
      "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} " +
      "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}" +
      "A {color : black;}" +
      "A.name {color : black;}" +
      "HR {color : #525D76;}";

  private static URLEncoder encoder = new URLEncoder("-_.*/");


  /**
   * Write a file directory.
   *
   * @param res write to this
   * @param dir directory
   * @param path the URL path reletive to the base
   * @throws IOException
   */
  public static void writeDirectory(HttpServletResponse res, File dir, String path) throws IOException {
    ucar.nc2.units.DateFormatter formatter = new ucar.nc2.units.DateFormatter();

    // error checking
    if (dir == null) {
      ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, 0);
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    if (!dir.exists() || !dir.isDirectory()) {
      ServletUtil.logServerAccess(HttpServletResponse.SC_NOT_FOUND, 0);
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    // if the directory doesnt end in a slash, the browser will remove the last component of the path,
    /* so we have to add it back on.
    String dirName = dir.getPath();
    dirName = StringUtil.replace(dirName,'\\', "/");
    boolean endsWithSlash = path.endsWith("/");
    if (!endsWithSlash) {
      int lastSlash = path.lastIndexOf("/");
      if (lastSlash > 0)
        path = path.substring(lastSlash + 1);
    } */

    StringBuffer sb = new StringBuffer();

    // Render the page header
    sb.append("<html>\r\n");
    sb.append("<head>\r\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
    sb.append("<title>");
    sb.append("Directory listing for " + path);
    sb.append("</title>\r\n");
    sb.append("<STYLE><!--");
    sb.append(TOMCAT_CSS);
    sb.append("--></STYLE>\r\n");
    sb.append("</head>\r\n");
    sb.append("<body>\r\n");
    sb.append("<h1>");
    sb.append("Directory listing for " + path);

    // Render the link to our parent (if required)
    String parentDirectory = path;
    if (parentDirectory.endsWith("/")) {
      parentDirectory =
          parentDirectory.substring(0, parentDirectory.length() - 1);
    }
    int slash = parentDirectory.lastIndexOf('/');
    if (slash >= 0) {
      String parent = parentDirectory.substring(0, slash);
      sb.append(" - <a href=\"");
      if (parent.equals(""))
        parent = "/";
      sb.append("../"); // sb.append(encode(parent));
      //if (!parent.endsWith("/"))
      //  sb.append("/");
      sb.append("\">");
      sb.append("<b>");
      sb.append("Up to " + parent);
      sb.append("</b>");
      sb.append("</a>");
    }

    sb.append("</h1>\r\n");
    sb.append("<HR size=\"1\" noshade=\"noshade\">");

    sb.append("<table width=\"100%\" cellspacing=\"0\"" +
        " cellpadding=\"5\" align=\"center\">\r\n");

    // Render the column headings
    sb.append("<tr>\r\n");
    sb.append("<td align=\"left\"><font size=\"+1\"><strong>");
    sb.append("Filename");
    sb.append("</strong></font></td>\r\n");
    sb.append("<td align=\"center\"><font size=\"+1\"><strong>");
    sb.append("Size");
    sb.append("</strong></font></td>\r\n");
    sb.append("<td align=\"right\"><font size=\"+1\"><strong>");
    sb.append("Last Modified");
    sb.append("</strong></font></td>\r\n");
    sb.append("</tr>");

    // Render the directory entries within this directory
    boolean shade = false;
    File[] children = dir.listFiles();
    List fileList = Arrays.asList(children);
    Collections.sort( fileList);
    for (int i = 0; i < fileList.size(); i++) {
      File child = (File) fileList.get(i);

      String childname = child.getName();
      if (childname.equalsIgnoreCase("WEB-INF") ||
          childname.equalsIgnoreCase("META-INF"))
        continue;

      if (child.isDirectory()) childname = childname + "/";
      //if (!endsWithSlash) childname = path + "/" + childname; // client removes last path if no slash

      sb.append("<tr");
      if (shade)
        sb.append(" bgcolor=\"#eeeeee\"");
      sb.append(">\r\n");
      shade = !shade;

      sb.append("<td align=\"left\">&nbsp;&nbsp;\r\n");
      sb.append("<a href=\"");
      //sb.append( encode(contextPath));
      // resourceName = encode(path + resourceName);
      sb.append(childname);
      sb.append("\"><tt>");
      sb.append(childname);
      sb.append("</tt></a></td>\r\n");

      sb.append("<td align=\"right\"><tt>");
      if (child.isDirectory())
        sb.append("&nbsp;");
      else
        sb.append(renderSize(child.length()));
      sb.append("</tt></td>\r\n");

      sb.append("<td align=\"right\"><tt>");
      sb.append(formatter.toDateTimeString(new Date(child.lastModified())));
      sb.append("</tt></td>\r\n");

      sb.append("</tr>\r\n");
    }

    // Render the page footer
    sb.append("</table>\r\n");
    sb.append("<HR size=\"1\" noshade=\"noshade\">");

    sb.append("<h3>").append(ThreddsDefaultServlet.getVersionStatic());
    sb.append(" <a href='/thredds/docs/'> Documentation</a></h3>\r\n");
    sb.append("</body>\r\n");
    sb.append("</html>\r\n");

    res.setContentLength(sb.length());
    res.setContentType("text/html; charset=iso-8859-1");

    // LOOK faster to use PrintStream instead of PrintWriter
    // Return an input stream to the underlying bytes
    // Prepare a writer
    OutputStreamWriter osWriter = null;
    try {
      osWriter = new OutputStreamWriter(res.getOutputStream(), "UTF8");
    } catch (Exception e) {
      // Should never happen
      osWriter = new OutputStreamWriter(res.getOutputStream());
    }
    PrintWriter writer = new PrintWriter(osWriter);

    writer.write(sb.toString());
    writer.flush();

    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, sb.length() );
  }

  private static String renderSize(long size) {

    long leftSide = size / 1024;
    long rightSide = (size % 1024) / 103;   // Makes 1 digit
    if ((leftSide == 0) && (rightSide == 0) && (size > 0))
      rightSide = 1;

    return ("" + leftSide + "." + rightSide + " kb");
  }

  private static String URLencode(String url) {
    return encoder.encode(url);
  }

  /**
   * Write a catalog in HTML, make it look like a file directory.
   *
   * @param res write to this
   * @param cat catalog to write
   * @throws IOException
   */
  public static void writeCatalog(HttpServletResponse res, InvCatalogImpl cat, String contextPath) throws IOException {
    String catAsString = getCatalog( cat, contextPath);

    res.setContentLength( catAsString.length());
    res.setContentType("text/html; charset=iso-8859-1");

    // Write it out
    OutputStreamWriter osWriter = null;
    try {
      osWriter = new OutputStreamWriter(res.getOutputStream(), "UTF8");
    } catch (Exception e) {
      // Should never happen
      osWriter = new OutputStreamWriter(res.getOutputStream());
    }
    PrintWriter writer = new PrintWriter(osWriter);
    writer.write( catAsString);
    writer.flush();

    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, catAsString.length() );
  }

  private static String getCatalog(InvCatalogImpl cat, String contextPath )
  {
    StringBuffer sb = new StringBuffer(10000);
    ucar.nc2.units.DateFormatter formatter = new ucar.nc2.units.DateFormatter();

    String catname = StringUtil.quoteHtmlContent( cat.getUriString());

    // Render the page header
    sb.append("<html>\r\n");
    sb.append("<head>\r\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
    sb.append("<title>");
    sb.append("Catalog " + catname);
    sb.append("</title>\r\n");
    sb.append("<STYLE><!--");
    sb.append(TOMCAT_CSS);
    sb.append("--></STYLE> ");
    sb.append("</head>\r\n");
    sb.append("<body>");
    sb.append("<h1>");
    sb.append("Catalog " + catname);
    sb.append("</h1>");
    sb.append("<HR size=\"1\" noshade=\"noshade\">");

    sb.append("<table width=\"100%\" cellspacing=\"0\"" +
        " cellpadding=\"5\" align=\"center\">\r\n");

    // Render the column headings
    sb.append("<tr>\r\n");
    sb.append("<td align=\"left\"><font size=\"+1\"><strong>");
    sb.append("Dataset");
    sb.append("</strong></font></td>\r\n");
    sb.append("<td align=\"center\"><font size=\"+1\"><strong>");
    sb.append("Size");
    sb.append("</strong></font></td>\r\n");
    sb.append("<td align=\"right\"><font size=\"+1\"><strong>");
    sb.append("Last Modified");
    sb.append("</strong></font></td>\r\n");
    sb.append("</tr>");

    // Recursively render the datasets
    boolean shade = false;
    shade = doDatasets(cat, cat.getDatasets(), contextPath, sb, shade, 0, formatter);

    // Render the page footer
    sb.append("</table>\r\n");

    sb.append("<HR size=\"1\" noshade=\"noshade\">");

    sb.append("<h3>").append(ThreddsDefaultServlet.getVersionStatic());
    sb.append(" <a href='" + contextPath + "/docs/'> Documentation</a></h3>\r\n");
    sb.append("</body>\r\n");
    sb.append("</html>\r\n");

    return( sb.toString() );
  }

  static private boolean doDatasets(InvCatalogImpl cat, List datasets, String contextPath, StringBuffer sb, boolean shade, int level,
          ucar.nc2.units.DateFormatter formatter) {
    URI catURI = cat.getBaseURI();
    String catHtml;
    if (catURI.isAbsolute()) {
      catHtml = contextPath + "/catalog.html?cmd=subset&catalog="+cat.getUriString()+"&";
    } else { // replace xml with html
      catHtml = cat.getUriString();
      int pos = catHtml.lastIndexOf('.');
      catHtml = catHtml.substring(0,pos)+".html?";
    }

    for (int i = 0; i < datasets.size(); i++) {
      InvDatasetImpl ds = (InvDatasetImpl) datasets.get(i);
      String name = StringUtil.quoteHtmlContent( ds.getName());

      sb.append("<tr");
      if (shade)
        sb.append(" bgcolor=\"#eeeeee\"");
      sb.append(">\r\n");
      shade = !shade;

      sb.append("<td align=\"left\">");
      for (int j=0;j<=level;j++)
        sb.append("&nbsp;&nbsp;");
      sb.append("\r\n");

      if (ds instanceof InvCatalogRef) {
        InvCatalogRef catref = (InvCatalogRef) ds;
        URI catrefURI = catref.getURI();
        String href = catrefURI.toString();
        if (catrefURI.isAbsolute()) {
          href = contextPath + "/catalogServices?catalog="+href;
        } else {
          int pos = href.lastIndexOf('.');
          href = href.substring(0,pos)+".html";
        }
        sb.append("<a href=\"");
        sb.append( StringUtil.quoteHtmlContent( href));
        sb.append("\"><tt>");
        sb.append(name);
        sb.append("/</tt></a></td>\r\n");
      } else if (ds.getID() != null) {
        sb.append("<a href=\"");
        // sb.append("catalog.html?cmd=subset&catalog=");
        sb.append( StringUtil.quoteHtmlContent( catHtml));
        sb.append("dataset=");
        sb.append( StringUtil.quoteHtmlContent( ds.getID()));
        sb.append("\"><tt>");
        sb.append(name);
        sb.append("</tt></a></td>\r\n");
      } else {
        sb.append("<tt>");
        sb.append(name);
        sb.append("</tt></td>\r\n");
      }

      sb.append("<td align=\"right\"><tt>");
      double size = ds.getDataSize();
      if ((size != 0.0) && !Double.isNaN(size))
        sb.append( Format.formatByteSize(size) );
      else
        sb.append("&nbsp;");
      sb.append("</tt></td>\r\n");

      sb.append("<td align=\"right\"><tt>");
      sb.append(formatter.toDateTimeString(new Date()));
      sb.append("</tt></td>\r\n");

      sb.append("</tr>\r\n");

      if (!(ds instanceof InvCatalogRef))
        shade = doDatasets(cat, ds.getDatasets(), contextPath, sb, shade, level+1, formatter);
    }

    return shade;
  }

  /**
   * Show CDM compliance (coordinate systems, etc) of a NetcdfDataset.
   *
   * @param res write to this
   * @param ds dataset to write
   * @throws IOException
   */
  public static void showCDM(HttpServletResponse res, NetcdfDataset ds) throws IOException {
    StringBuffer sb = new StringBuffer(10000);

    String name = StringUtil.quoteHtmlContent( ds.getLocation());

    // Render the page header
    sb.append("<html>\r\n");
    sb.append("<head>\r\n");
    sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
    sb.append("<title>");
    sb.append("Common Data Model");
    sb.append("</title>\r\n");
    sb.append("<STYLE><!--");
    sb.append(TOMCAT_CSS);
    sb.append("--></STYLE> ");
    sb.append("</head>\r\n");
    sb.append("<body>");
    sb.append("<h1>");
    sb.append("Dataset " + name);
    sb.append("</h1>");
    sb.append("<HR size=\"1\" noshade=\"noshade\">");

    sb.append("<table width=\"100%\" cellspacing=\"0\"" +
        " cellpadding=\"5\" align=\"center\">\r\n");

    //////// Axis
    sb.append("<tr>\r\n");
    sb.append("<td align=\"left\"><font size=\"+1\"><strong>");
    sb.append("Axis");
    sb.append("</strong></font></td>\r\n");
    sb.append("<td align=\"left\"><font size=\"+1\"><strong>");
    sb.append("Type");
    sb.append("</strong></font></td>\r\n");
    sb.append("<td align=\"left\"><font size=\"+1\"><strong>");
    sb.append("Units");
    sb.append("</strong></font></td>\r\n");
    sb.append("</tr>");

    // Show the coordinate axes
    boolean shade = false;
    List axes = ds.getCoordinateAxes();
    for (int i = 0; i < axes.size(); i++) {
      CoordinateAxis axis = (CoordinateAxis) axes.get(i);
      showAxis( axis, sb, shade);
      shade = !shade;
    }

    ///////////// Grid
    GridDataset gds = new ucar.nc2.dataset.grid.GridDataset( ds);

    // look for projections
    //List gridsets = gds.getGridSets();

    sb.append("<tr>\r\n");
    sb.append("<td align=\"left\"><font size=\"+1\"><strong>");
    sb.append("GeoGrid");
    sb.append("</strong></font></td>\r\n");
    sb.append("<td align=\"left\"><font size=\"+1\"><strong>");
    sb.append("Description");
    sb.append("</strong></font></td>\r\n");
    sb.append("<td align=\"left\"><font size=\"+1\"><strong>");
    sb.append("Units");
    sb.append("</strong></font></td>\r\n");
    sb.append("</tr>");

    // Show the grids
    shade = false;
    List grids = gds.getGrids();
    for (int i = 0; i < grids.size(); i++) {
      GridDatatype grid = (GridDatatype) grids.get(i);
      showGrid( grid, sb, shade);
      shade = !shade;
    }

    // Render the page footer
    sb.append("</table>\r\n");

    sb.append("<HR size=\"1\" noshade=\"noshade\">");

    sb.append("<h3>").append(ThreddsDefaultServlet.getVersionStatic());
    sb.append(" <a href='/thredds/docs/'> Documentation</a></h3>\r\n");
    sb.append("</body>\r\n");
    sb.append("</html>\r\n");

    res.setContentLength(sb.length());
    res.setContentType("text/html; charset=iso-8859-1");

    // Write it out
    OutputStreamWriter osWriter = null;
    try {
      osWriter = new OutputStreamWriter(res.getOutputStream(), "UTF8");
    } catch (Exception e) {
      // Should never happen
      osWriter = new OutputStreamWriter(res.getOutputStream());
    }
    PrintWriter writer = new PrintWriter(osWriter);
    writer.write(sb.toString());
    writer.flush();

    ServletUtil.logServerAccess( HttpServletResponse.SC_OK, sb.length() );
  }

  static private void showAxis(CoordinateAxis axis, StringBuffer sb, boolean shade) {

    sb.append("<tr");
    if (shade)
      sb.append(" bgcolor=\"#eeeeee\"");
    sb.append(">\r\n");
    shade = !shade;

    sb.append("<td align=\"left\">");
    sb.append("\r\n");

    StringBuffer sbuff = new StringBuffer();
    axis.getNameAndDimensions(sbuff, false, true);
    String name = StringUtil.quoteHtmlContent(sbuff.toString());
    sb.append("&nbsp;");
    sb.append(name);
    sb.append("</tt></a></td>\r\n");

    sb.append("<td align=\"left\"><tt>");
    AxisType type = axis.getAxisType();
    String stype = (type == null) ? "" : StringUtil.quoteHtmlContent(type.toString());
    sb.append(stype);
    sb.append("</tt></td>\r\n");

    sb.append("<td align=\"left\"><tt>");
    String units = axis.getUnitsString();
    String sunits = (units == null) ? "" : units;
    sb.append(sunits);
    sb.append("</tt></td>\r\n");

    sb.append("</tr>\r\n");
  }

  static private void showGrid(GridDatatype grid, StringBuffer sb, boolean shade) {

    sb.append("<tr");
    if (shade)
      sb.append(" bgcolor=\"#eeeeee\"");
    sb.append(">\r\n");
    shade = !shade;

    sb.append("<td align=\"left\">");
    sb.append("\r\n");

    VariableEnhanced ve = grid.getVariable();
    StringBuffer sbuff = new StringBuffer();
    ve.getNameAndDimensions(sbuff, false, true);
    String name = StringUtil.quoteHtmlContent(sbuff.toString());
    sb.append("&nbsp;");
    sb.append(name);
    sb.append("</tt></a></td>\r\n");

    sb.append("<td align=\"left\"><tt>");
    String desc = ve.getDescription();
    String sdesc = (desc == null) ? "" : StringUtil.quoteHtmlContent(desc);
    sb.append(sdesc);
    sb.append("</tt></td>\r\n");

    sb.append("<td align=\"left\"><tt>");
    String units = ve.getUnitsString();
    String sunits = (units == null) ? "" : units;
    sb.append(sunits);
    sb.append("</tt></td>\r\n");

    sb.append("</tr>\r\n");
  }

}
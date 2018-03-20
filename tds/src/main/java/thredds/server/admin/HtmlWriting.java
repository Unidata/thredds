/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import thredds.server.config.HtmlConfigBean;
import thredds.server.config.TdsContext;
import thredds.util.ContentType;
import ucar.nc2.time.CalendarDate;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provide methods to write HTML representations of a catalog, directory, or CDM dataset to an HTTP response.
 * LOOK get rid of
 * @author edavis
 */

@Component
public class HtmlWriting {
  // static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HtmlWriting.class);

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private HtmlConfigBean htmlConfig;


  public String getHtmlDoctypeAndOpenTag() {
    return "<!DOCTYPE html PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'\n" +
            "        'http://www.w3.org/TR/html4/loose.dtd'>\n" + "<html>\n";
  }

  public String getGoogleTrackingContent() {
    if (this.htmlConfig.getGoogleTrackingCode().isEmpty()) {
      return "";
    } else {
      // See https://developers.google.com/analytics/devguides/collection/analyticsjs/
      return new StringBuilder()
              .append("<!-- Google Analytics -->\n")
              .append("<script>\n")
              .append("(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){\n")
              .append("(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),\n")
              .append("m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)\n")
              .append("})(window,document,'script','https://www.google-analytics.com/analytics.js','ga');\n")
              .append('\n')
              .append("ga('create', '").append(this.htmlConfig.getGoogleTrackingCode()).append("', 'auto');\n")
              .append("ga('send', 'pageview');\n")
              .append("</script>\n")
              .append("<!-- End Google Analytics -->\n").toString();
    }
  }

  
  public String getTdsCatalogCssLink() {
    return new StringBuilder()
            .append("<link rel='stylesheet' href='")
            .append(this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getCatalogCssUrl()))
            .append("' type='text/css' >").toString();
  }

  public String getTdsPageCssLink() {
    return new StringBuilder()
            .append("<link rel='stylesheet' href='")
            .append(this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getPageCssUrl()))
            .append("' type='text/css' >").toString();
  }

  //  public static final String UNIDATA_HEAD
  public String getUserHead() {
    return new StringBuilder()
            .append("<table width='100%'><tr><td>\n")
            .append("  <img src='").append(this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getHostInstLogoUrl()))
            .append("'\n")
            .append("       alt='").append(this.htmlConfig.getHostInstLogoAlt()).append("'\n")
            .append("       align='left' valign='top'\n")
            .append("       hspace='10' vspace='2'>\n")
            .append("  <h3><strong>").append(this.tdsContext.getWebappDisplayName()).append("</strong></h3>\n")
            .append("</td></tr></table>\n")
            .toString();
  }

  public String getOldStyleHeader() {
    StringBuilder sb = new StringBuilder();
    appendOldStyleHeader(sb);
    return sb.toString();
  }

  public void appendOldStyleHeader(StringBuilder sb) {
    appendOldStyleHeader(sb,
            this.htmlConfig.getWebappName(), this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getWebappUrl()),
            this.htmlConfig.getInstallLogoAlt(), this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getInstallLogoUrl()),
            this.htmlConfig.getInstallName(), this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getInstallUrl()),
            this.htmlConfig.getHostInstName(), this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getHostInstUrl()));
  }

  public void appendOldStyleHeader(StringBuilder sb,
                                   String webappName, String webappUrl,
                                   String logoAlt, String logoUrl,
                                   String installName, String installUrl,
                                   String hostName, String hostUrl) {
    // Table setup.
    sb.append("<table width='100%'>\n")
            .append("<tr><td>\n");
    // Logo
    sb.append("<img src='").append(logoUrl)
            .append("' alt='").append(logoAlt)
            .append("' align='left' valign='top'")
            .append(" hspace='10' vspace='2'")
            .append(">\n");

    // Installation name.
    sb.append("<h3><strong>")
            .append("<a href='").append(installUrl).append("'>")
            .append(installName).append("</a>")
            .append("</strong>");
    if (false) sb.append(" at ").append(hostName);
    sb.append("</h3>\n");

    // Webapp Name.
    sb.append("<h3><strong>")
            .append("<a href='").append(webappUrl).append("'>")
            .append(webappName).append("</a>")
            .append("</strong></h3>\n");

    sb.append("</td></tr>\n")
            .append("</table>\n");
  }

  public void appendSimpleFooter(StringBuilder sb) {
    sb.append("<h3>");
    if (this.htmlConfig.getInstallName() != null) {
      String installUrl = this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getInstallUrl());
      if (installUrl != null)
        sb.append("<a href='").append(installUrl).append("'>");
      sb.append(this.htmlConfig.getInstallName());
      if (installUrl != null)
        sb.append("</a>");
    }
    if (this.htmlConfig.getHostInstName() != null) {
      sb.append(" at ");
      String hostInstUrl = this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getHostInstUrl());
      if (hostInstUrl != null)
        sb.append("<a href='").append(hostInstUrl).append("'>");
      sb.append(this.htmlConfig.getHostInstName());
      if (hostInstUrl != null)
      sb.append("</a>");
      sb.append(String.format(" see <a href='%s/serverInfo.html'> Info </a>", tdsContext.getContextPath()));
      sb.append("<br>\n");
    }

    sb.append( this.tdsContext.getWebappDisplayName() )
            .append( " [Version " ).append( this.tdsContext.getVersionInfo() );
    sb.append( "] <a href='" )
            .append( this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getWebappDocsUrl() ) )
            .append( "'> Documentation</a>" );
    sb.append( "</h3>\n" );
  }

  /**
   * Write a file directory.
   *
   * @param res  the HttpServletResponse on which to write the file directory response.
   * @param dir  directory
   * @param path the URL path reletive to the base
   * @return the number of characters (Unicode code units) in the response.
   * @throws java.io.IOException if an I/O exception occurs.
   */
  public int writeDirectory(HttpServletResponse res, File dir, String path) throws IOException {
    // error checking
    if (dir == null) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return 0;
    }

    if (!dir.exists() || !dir.isDirectory()) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return 0;
    }

    // Get directory as HTML
    String dirHtmlString = getDirectory(path, dir);

    thredds.servlet.ServletUtil.setResponseContentLength(res, dirHtmlString);
    res.setContentType(ContentType.html.getContentHeader());
    PrintWriter writer = res.getWriter();
    writer.write(dirHtmlString);
    writer.flush();

    return dirHtmlString.length();
  }

  private String getDirectory(String path, File dir) {
    StringBuilder sb = new StringBuilder();

    // Render the page header
    sb.append(getHtmlDoctypeAndOpenTag()); // "<html>\n" );
    sb.append("<head>\r\n");
    sb.append("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
    sb.append("<title>");
    sb.append("Directory listing for ").append(dir.getPath()); // debug mode - show the actual file path
    sb.append("</title>\r\n");
    sb.append(this.getTdsCatalogCssLink()).append("\n");
    sb.append(this.getGoogleTrackingContent());
    sb.append("</head>\r\n");
    sb.append("<body>\r\n");
    sb.append("<h1>");
    sb.append("Directory listing for ").append(dir.getPath());

    /* Render the link to our parent (if required)
    String parentDirectory = path;
    if (parentDirectory.endsWith("/")) {
      parentDirectory = parentDirectory.substring(0, parentDirectory.length() - 1);
    }
    int slash = parentDirectory.lastIndexOf('/');
    if (slash >= 0) {
      String parent = parentDirectory.substring(0, slash);
      sb.append(" - <a href='");
      if (parent.equals("")) {
        parent = "/";
      }
      sb.append("../"); // sb.append(encode(parent));
      //if (!parent.endsWith("/"))
      //  sb.append("/");
      sb.append("'>");
      sb.append("<b>");
      sb.append("Up to ").append(parent);
      sb.append("</b>");
      sb.append("</a>");
    } */

    sb.append("</h1>\r\n");
    sb.append("<HR size='1' noshade='noshade'>");

    sb.append("<table width='100%' cellspacing='0' cellpadding='5' align='center'>\r\n");

    // Render the column headings
    sb.append("<tr>\r\n");
    sb.append("<td align='left'><font size='+1'><strong>");
    sb.append("Filename");
    sb.append("</strong></font></td>\r\n");
    sb.append("<td align='center'><font size='+1'><strong>");
    sb.append("Size");
    sb.append("</strong></font></td>\r\n");
    sb.append("<td align='right'><font size='+1'><strong>");
    sb.append("Last Modified");
    sb.append("</strong></font></td>\r\n");
    sb.append("</tr>");

    // Render the directory entries within this directory
    boolean shade = false;
    File[] children = dir.listFiles();
    List<File> fileList = (children == null) ? new ArrayList<File>() : Arrays.asList(children);
    Collections.sort(fileList);
    for (File child : fileList) {

      String childname = child.getName();
      if (childname.equalsIgnoreCase("WEB-INF") || childname.equalsIgnoreCase("META-INF")) {
        continue;
      }

      if (child.isDirectory()) childname = childname + "/";
      //if (!endsWithSlash) childname = path + "/" + childname; // client removes last path if no slash

      sb.append("<tr");
      if (shade)
        sb.append(" bgcolor='#eeeeee'");
      sb.append(">\r\n");
      shade = !shade;

      sb.append("<td align='left'>&nbsp;&nbsp;\r\n");
      sb.append("<a href='");
      //sb.append( encode(contextPath));
      // resourceName = encode(path + resourceName);
      sb.append(childname);
      sb.append("'><tt>");
      sb.append(childname);
      sb.append("</tt></a></td>\r\n");

      sb.append("<td align='right'><tt>");
      if (child.isDirectory()) {
        sb.append("&nbsp;");
      } else {
        sb.append(renderSize(child.length()));
      }
      sb.append("</tt></td>\r\n");

      sb.append("<td align='right'><tt>");
      sb.append(CalendarDate.of(child.lastModified()).toString());
      sb.append("</tt></td>\r\n");

      sb.append("</tr>\r\n");
    }

    // Render the page footer
    sb.append("</table>\r\n");
    sb.append("<HR size='1' noshade='noshade'>");

    appendSimpleFooter(sb);

    sb.append("</body>\r\n");
    sb.append("</html>\r\n");

    return sb.toString();
  }

  private String renderSize(long size) {

    long leftSide = size / 1024;
    long rightSide = (size % 1024) / 103;   // Makes 1 digit
    if ((leftSide == 0) && (rightSide == 0) && (size > 0)) {
      rightSide = 1;
    }

    return ("" + leftSide + "." + rightSide + " kb");
  }

   /*
  private void appendWebappFooter( StringBuilder sb )
  {
    sb.append( "<h3>" )
            .append( this.tdsContext.getWebappDisplayName() )
            .append( " [Version " ).append( this.tdsContext.getVersionInfo() );
    sb.append( "] <a href='" )
            .append( this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getWebappDocsUrl() ) )
            .append( "'> Documentation</a>" );
    sb.append("</h3>\n");
  }

  //  private static final String TOMCAT_CSS
  private String getTomcatCSS() {
    return new StringBuilder("<STYLE type='text/css'><!--")
            .append("H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} ")
            .append("H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} ")
            .append("H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} ")
            .append("BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} ")
            .append("B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} ")
            .append("P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}")
            .append("A {color : black;}")
            .append("A.name {color : black;}")
            .append("HR {color : #525D76;}")
            .append("--></STYLE>\r\n")
            .toString();
  }

  */

  /*   public void appendTableHeader(StringBuilder stringBuilder,
                                boolean includeInstall,
                                boolean includeWebapp,
                                boolean includeLogos) {
    // Table setup.
    stringBuilder
            .append("<table width='100%'>\n");

    if (includeInstall) {
      stringBuilder.append("<tr><td>\n");
      appendInstallationInfo(stringBuilder, includeLogos);
      stringBuilder.append("</td><td>\n");
      appendHostInstInfo(stringBuilder, includeLogos);
      stringBuilder.append("</td></tr>\n");
    }

    if (includeWebapp) {
      stringBuilder
              .append("<tr><td>\n");
      appendWebappInfo(stringBuilder, includeLogos);
      stringBuilder.append("</td></tr>\n");
    }
    stringBuilder.append("</table><hr>\n");
  }

  private void appendWebappInfo(StringBuilder stringBuilder, boolean includeLogo) {
    // Include webapp info
    String webappUrl = this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getWebappUrl());
    String webappLogoUrl = this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getWebappLogoUrl());
    if (includeLogo && webappLogoUrl != null)
      stringBuilder
              .append("<img src='").append(webappLogoUrl)
              .append("' alt='").append(this.htmlConfig.getWebappLogoAlt())
              .append("'> ");
    stringBuilder
            .append("<a href='").append(webappUrl).append("'>")
            .append(this.tdsContext.getWebappDisplayName())
            .append("</a>");
  }

  private void appendHostInstInfo(StringBuilder stringBuilder, boolean includeLogo) {
    // Include host institution information
    if (this.htmlConfig.getHostInstName() != null) {
      String hostInstUrl = this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getHostInstUrl());
      String hostInstLogoUrl = this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getHostInstLogoUrl());
      if (includeLogo && hostInstLogoUrl != null)
        stringBuilder
                .append("<img src='").append(hostInstLogoUrl)
                .append("' alt='").append(this.htmlConfig.getHostInstLogoAlt())
                .append("'> ");
      if (hostInstUrl != null)
        stringBuilder.append("<a href='").append(hostInstUrl).append("'>");
      stringBuilder.append(this.htmlConfig.getHostInstName());
      if (hostInstUrl != null)
        stringBuilder.append("</a>");
    } else
      stringBuilder.append("Unknown Host Institution");
  }

  private void appendInstallationInfo(StringBuilder stringBuilder, boolean includeLogo) {
    // Include information on this intsallation.
    if (this.htmlConfig.getInstallName() != null) {
      String installUrl = this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getInstallUrl());
      String installLogoUrl = this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getInstallLogoUrl());
      if (includeLogo && installLogoUrl != null)
        stringBuilder
                .append("<img src='").append(installLogoUrl)
                .append("' alt='").append(this.htmlConfig.getInstallLogoAlt())
                .append("'> ");
      if (installUrl != null)
        stringBuilder.append("<a href='").append(installUrl).append("'>");
      stringBuilder.append(this.htmlConfig.getInstallName());
      if (installUrl != null)
        stringBuilder.append("</a>");
    } else {
      // This installation is not named.
      stringBuilder.append("Unnamed TDS Installation");
    }
  } */

  /*
    public String getXHtmlDoctypeAndOpenTag() {
    return "<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN'\n" +
            "        'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>\n" +
            "<html xmlns='http://www.w3.org/1999/xhtml' xml:lang='en' lang='en'>";
  }

  //  public static final String UNIDATA_CSS
  public String getUserCSS() {
    return new StringBuilder()
            .append("<link rel='stylesheet' href='")
            .append(this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getPageCssUrl()))
            .append("' type='text/css' >").toString();
  }


  private String getCollapsableJs() {
      return new StringBuilder()
          .append("<script src=\"https://code.jquery.com/jquery-1.11.2.min.js\"></script>\n")
          .append("<script type='text/javascript'>\n")
          .append("  $(document).ready(function() {\n")
          .append("    $('tr.header').on('click',function() {\n")
          .append("      $(this).nextUntil('tr.header').slideToggle(0);\n")
          .append("    });\n")
          .append("    $('tr.header').click();\n")
          .append("    $('tr').removeAttr(\"bgcolor\");\n")
          .append("    $('table#datasets').addClass(\"hoverTable\");\n")
          .append("  });\n")
	      .append("</script>\n").toString();
  }

  private String getCollapsableCss() {
    return new StringBuilder()
      .append("<style>\n")
      .append("  .hoverTable tr.header {cursor:pointer;}\n")
      .append("  .hoverTable{\n")
      .append("    width:100%;\n")
      .append("    cellspacing:0;\n")
      .append("    cellpadding:5;\n")
      .append("    align:center;\n")
      .append("  }\n")
      .append("  .hoverTable tr {\n")
      .append("    background: #ffffff;\n")
      .append("  }\n")
      .append("  .hoverTable tr:hover {\n")
      .append("    background-color: #eeeeee;\n")
      .append("  }\n")
      .append("</style>\n").toString();
  }

  public StringBuilder makeCollapsable(StringBuilder sb, int level) {
	    if (level > 0) {
	    String replaceThis = "<tr";
        String withThis = "<tr class=\"header\"";
        int strRepStart = sb.lastIndexOf(replaceThis);
        if (strRepStart != -1) {
          sb.replace(strRepStart, strRepStart + replaceThis.length(), withThis);
        }
	  }
      return sb;
  }
   */

  /*
   * Show CDM compliance (coordinate systems, etc) of a NetcdfDataset.
   *
   * @param ds dataset to write
   *
  public void showCDM(HttpServletResponse res, NetcdfDataset ds)
          throws IOException {
    String cdmAsString = getCDM(ds);

    res.setContentLength(cdmAsString.length());
    res.setContentType(ContentType.html.getContentHeader());
    PrintWriter writer = res.getWriter();

    writer.write(cdmAsString);
    writer.flush();
  }

  private String getCDM(NetcdfDataset ds) throws IOException {
    StringBuilder sb = new StringBuilder(10000);

    String name = Escape.html(ds.getLocation());

    // Render the page header
    sb.append(getHtmlDoctypeAndOpenTag()); // "<html>\n" );
    sb.append("<head>\r\n");
    sb.append("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
    sb.append("<title>");
    sb.append("Common Data Model");
    sb.append("</title>\r\n");
    sb.append(this.getTdsPageCssLink()).append("\n");
    sb.append(this.getGoogleTrackingContent());
    sb.append("</head>\r\n");
    sb.append("<body>");
    sb.append("<h1>");
    sb.append("Dataset ").append(name);
    sb.append("</h1>");
    sb.append("<HR size='1' noshade='noshade'>");

    sb.append("<table width='100%' cellspacing='0' cellpadding='5' align='center'>\r\n");

    //////// Axis
    sb.append("<tr>\r\n");
    sb.append("<td align='left'><font size='+1'><strong>");
    sb.append("Axis");
    sb.append("</strong></font></td>\r\n");
    sb.append("<td align='left'><font size='+1'><strong>");
    sb.append("Type");
    sb.append("</strong></font></td>\r\n");
    sb.append("<td align='left'><font size='+1'><strong>");
    sb.append("Units");
    sb.append("</strong></font></td>\r\n");
    sb.append("</tr>");

    // Show the coordinate axes
    boolean shade = false;
    for (CoordinateAxis axis : ds.getCoordinateAxes()) {
      showAxis(axis, sb, shade);
      shade = !shade;
    }

    ///////////// Grid
    GridDataset gds = new ucar.nc2.dt.grid.GridDataset(ds);

    // find projections
    //List gridsets = gds.getGridsets();

    sb.append("<tr>\r\n");
    sb.append("<td align='left'><font size='+1'><strong>");
    sb.append("GeoGrid");
    sb.append("</strong></font></td>\r\n");
    sb.append("<td align='left'><font size='+1'><strong>");
    sb.append("Description");
    sb.append("</strong></font></td>\r\n");
    sb.append("<td align='left'><font size='+1'><strong>");
    sb.append("Units");
    sb.append("</strong></font></td>\r\n");
    sb.append("</tr>");

    // Show the grids
    shade = false;
    for (GridDatatype grid : gds.getGrids()) {
      showGrid(grid, sb, shade);
      shade = !shade;
    }

    // Render the page footer
    sb.append("</table>\r\n");

    sb.append("<HR size='1' noshade='noshade'>");

    appendSimpleFooter(sb);

    sb.append("</body>\r\n");
    sb.append("</html>\r\n");

    return (sb.toString());
  }

  private void showAxis(CoordinateAxis axis, StringBuilder sb, boolean shade) {
    sb.append("<tr");
    if (shade) {
      sb.append(" bgcolor='#eeeeee'");
    }
    sb.append(">\r\n");
    shade = !shade;

    sb.append("<td align='left'>");
    sb.append("\r\n");

    StringBuilder sbuff = new StringBuilder();
    axis.getNameAndDimensions(sbuff);
    String name = Escape.html(sbuff.toString());
    sb.append("&nbsp;");
    sb.append(name);
    sb.append("</tt></a></td>\r\n");

    sb.append("<td align='left'><tt>");
    AxisType type = axis.getAxisType();
    String stype = (type == null) ? "" : Escape.html(type.toString());
    sb.append(stype);
    sb.append("</tt></td>\r\n");

    sb.append("<td align='left'><tt>");
    String units = axis.getUnitsString();
    String sunits = (units == null) ? "" : units;
    sb.append(sunits);
    sb.append("</tt></td>\r\n");

    sb.append("</tr>\r\n");
  }

  private void showGrid(GridDatatype grid, StringBuilder sb, boolean shade) {
    sb.append("<tr");
    if (shade) {
      sb.append(" bgcolor='#eeeeee'");
    }
    sb.append(">\r\n");
    shade = !shade;

    sb.append("<td align='left'>");
    sb.append("\r\n");

    VariableEnhanced ve = grid.getVariable();
    StringBuilder sbuff = new StringBuilder();
    ve.getNameAndDimensions(new Formatter(sbuff), false, true);
    String name = Escape.html(sbuff.toString());
    sb.append("&nbsp;");
    sb.append(name);
    sb.append("</tt></a></td>\r\n");

    sb.append("<td align='left'><tt>");
    String desc = ve.getDescription();
    String sdesc = (desc == null) ? "" : Escape.html(desc);
    sb.append(sdesc);
    sb.append("</tt></td>\r\n");

    sb.append("<td align='left'><tt>");
    String units = ve.getUnitsString();
    String sunits = (units == null) ? "" : units;
    sb.append(sunits);
    sb.append("</tt></td>\r\n");

    sb.append("</tr>\r\n");
  } */

}

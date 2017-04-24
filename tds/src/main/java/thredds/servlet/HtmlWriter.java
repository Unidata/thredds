/*
 * Copyright 1998-2014 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.servlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import thredds.catalog.*;
import thredds.server.config.HtmlConfig;
import thredds.server.config.TdsContext;
import thredds.server.viewer.dataservice.ViewerService;
import thredds.util.ContentType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.units.DateType;
import ucar.unidata.util.Format;
import ucar.unidata.util.StringUtil2;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static thredds.servlet.ServletUtil.setResponseContentLength;

// For MarkDown rendering
import java.io.BufferedReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Provide methods to write HTML representations of a catalog, directory, or CDM dataset to an HTTP response.
 * <p/>
 * HtmlWriter is implemented as a singleton. Before HtmlWriter can be used it
 * must be initialized with init(...). The singleton instance can then be
 * obtained with getInstance().
 *
 * @author edavis
 */

@Component
public class HtmlWriter {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HtmlWriter.class);

  private static HtmlWriter singleton;

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private HtmlConfig htmlConfig;

  @Autowired
  private ViewerService viewerService;

  private HtmlWriter() {
  }

  @PostConstruct
  public void afterPropertiesSet() {
    singleton = this;
  }

  public static HtmlWriter getInstance() {
    return singleton;
  }

  public String getHtmlDoctypeAndOpenTag() {
    return "<!DOCTYPE html PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'\n" +
            "        'http://www.w3.org/TR/html4/loose.dtd'>\n" + "<html>\n";
  }

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
  
  public String getGoogleTrackingContent() {
      if (this.htmlConfig.getGoogleTrackingCode().isEmpty()){
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
            .append("  <h3><strong>").append(this.tdsContext.getWebappName()).append("</strong></h3>\n")
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

  public void appendTableHeader(StringBuilder stringBuilder,
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
            .append(this.tdsContext.getWebappName())
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

    sb.append( this.tdsContext.getWebappName() )
            .append( " [Version " ).append( this.tdsContext.getVersionInfo() );
    sb.append( "] <a href='" )
            .append( this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getWebappDocsUrl() ) )
            .append( "'> Documentation</a>" );
    sb.append( "</h3>\n" );
  }

  private void appendWebappFooter( StringBuilder sb )
  {
    sb.append( "<h3>" )
            .append( this.tdsContext.getWebappName() )
            .append( " [Version " ).append( this.tdsContext.getVersionInfo() );
    sb.append( "] <a href='" )
            .append( this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getWebappDocsUrl() ) )
            .append( "'> Documentation</a>" );
    sb.append( "</h3>\n" );
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

  /**
   * Write a file directory.
   *
   * @param res  the HttpServletResponse on which to write the file directory response.
   * @param dir  directory
   * @param path the URL path reletive to the base
   * @return the number of characters (Unicode code units) in the response.
   * @throws java.io.IOException if an I/O exception occurs.
   */
  public int writeDirectory(HttpServletResponse res, File dir, String path)
          throws IOException {
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

    res.setContentType(ContentType.html.getContentHeader());
    thredds.servlet.ServletUtil.setResponseContentLength(res, dirHtmlString);
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
    sb.append("Directory listing for ").append(path);
    sb.append("</title>\r\n");
    sb.append(this.getTdsCatalogCssLink()).append("\n");
    sb.append(this.getGoogleTrackingContent());
    sb.append("</head>\r\n");
    sb.append("<body>\r\n");
    sb.append("<h1>");
    sb.append("Directory listing for ").append(path);

    // Render the link to our parent (if required)
    String parentDirectory = path;
    if (parentDirectory.endsWith("/")) {
      parentDirectory =
              parentDirectory.substring(0, parentDirectory.length() - 1);
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
    }

    sb.append("</h1>\r\n");
    sb.append("<HR size='1' noshade='noshade'>");

    sb.append("<table width='100%' cellspacing='0'" +
            " cellpadding='5' align='center'>\r\n");

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
      if (childname.equalsIgnoreCase("WEB-INF") ||
              childname.equalsIgnoreCase("META-INF")) {
        continue;
      }

      if (child.isDirectory()) childname = childname + "/";
      //if (!endsWithSlash) childname = path + "/" + childname; // client removes last path if no slash

      sb.append("<tr");
      if (shade) {
        sb.append(" bgcolor='#eeeeee'");
      }
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

  /**
   * Write an InvCatalogImpl to the HttpServletResponse, return the size in bytes of the catalog written to the response.
   *
   * @param req            the HttpServletRequest
   * @param res            the HttpServletResponse.
   * @param cat            the InvCatalogImpl to write to the HttpServletResponse.
   * @param isLocalCatalog indicates whether this catalog is local to this server.
   * @return the size in bytes of the catalog written to the HttpServletResponse.
   * @throws IOException if problems writing the response.
   */
  public int writeCatalog(HttpServletRequest req, HttpServletResponse res, InvCatalogImpl cat, boolean isLocalCatalog)
          throws IOException {
    String catHtmlAsString = convertCatalogToHtml(cat, isLocalCatalog);

    // Once this header is set, we know the encoding, and thus the actual
    // number of *bytes*, not characters, to encode
    res.setContentType(ContentType.html.getContentHeader());
    int len = setResponseContentLength(res, catHtmlAsString);

    if (!req.getMethod().equals("HEAD")) {
      PrintWriter writer = res.getWriter();
      writer.write(catHtmlAsString);
      writer.flush();
    }

    return len;
  }

  /**
   * Parse markdownSourcePath as markdown and render to output as html
   *
   * @param output Appendable append the output to
   * @param markdownSourcePath file path to markdown source
   * @return true if markdownSourcePath is to a parsable file
   */
  private boolean generateMarkdown(Path markdownSourcePath, Appendable output) {
    BufferedReader reader;
    try {
      reader = Files.newBufferedReader(markdownSourcePath, StandardCharsets.UTF_8);
    } catch (IOException e) {
      return false;
    } catch (java.lang.SecurityException e) {
      return false;
    }
    Parser parser = Parser.builder().build();
    Node node;
    try {
      node = parser.parseReader(reader);
    } catch (IOException e) {
      return false;
    }
    HtmlRenderer.builder().escapeHtml(true).build().render(node, output);
    return true;
  }


  /**
   * Try to find a markdown file for the catalog, render it to html and
   * append to output.  For crawable datasets it will try to look for
   * README.md in catalog directory.  If it is not found (or failed to
   * render) then it will try to find README.md in partent directories, and
   * stop if it is not found in the collection root directory.
   *
   * For static catalogs generated from xml files, it will try to render
   * catalog.md if present.
   *
   * @cat search for markdown file that relates to this catalog
   * @output append html to this stream
   * @returns true if markdown was appended
   */
  private boolean appendMarkdown(InvCatalogImpl cat, Appendable output) {
    Boolean hasAppended = false;
    String catalogSourcePath = cat.getCreateFrom();
    if (catalogSourcePath == null) {
      return false;
    }
    if (catalogSourcePath.startsWith("file:")) {
      catalogSourcePath = catalogSourcePath.substring(5);
    }
    if (catalogSourcePath.endsWith(".xml")) {
      // replace suffix xml (length 3) with suffix md
      int pos = catalogSourcePath.length() - 3;
      Path p = FileSystems.getDefault().getPath(catalogSourcePath.substring(0, pos).concat("md"));
      hasAppended = generateMarkdown(p, output);
    } else if (Files.isDirectory(FileSystems.getDefault().getPath(catalogSourcePath))) {
      Path dir = FileSystems.getDefault().getPath(catalogSourcePath);
      hasAppended = true; // assume the following will succeed (we'll return false if we fail)
      while(!generateMarkdown(dir.resolve("README.md"), output)) {
        // Tried special file .  (same directory) and was not able to
        // generate markdown (no file or invalid).  Do not traverese further:
        // /path/collection/./catalogLevel where ./ denotes collection start
        // catalog.
        Path name = dir.getFileName();
        if (name == null || ".".equals(name.toString())) {
          return false;
        }
        dir = dir.getParent();
        // Got to the root folder and still no success, so end the loop
        if (dir == null) {
          return false;
        }
      }
    }
    return hasAppended;
  }

  /**
   * Write a catalog in HTML, make it look like a file directory.
   *
   * @param cat catalog to write
   */
  String convertCatalogToHtml(InvCatalogImpl cat, boolean isLocalCatalog) {
    StringBuilder sb = new StringBuilder(10000);

    String catname = StringUtil2.quoteHtmlContent(cat.getUriString());
        
    // Render the page header
    sb.append(getHtmlDoctypeAndOpenTag()); // "<html>\n" );
    sb.append("<head>\r\n");
    sb.append("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
    sb.append("<title>");
    if (cat.isStatic())
      sb.append("TdsStaticCatalog ").append(catname); // for searching
    else
      sb.append("Catalog ").append(catname);
    sb.append("</title>\r\n");
    sb.append(this.getTdsCatalogCssLink()).append("\n");
    sb.append(this.getGoogleTrackingContent());
    sb.append("</head>\r\n");
    sb.append("<body>");
    sb.append("<h1 style=\"overflow:hidden;\">");

    // Logo
    //String logoUrl = this.htmlConfig.getInstallLogoUrl();
    String logoUrl = this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getInstallLogoUrl());
    if (logoUrl != null) {
      sb.append("<img src='").append(logoUrl);
      String logoAlt = this.htmlConfig.getInstallLogoAlt();
      if (logoAlt != null) sb.append("' alt='").append(logoAlt);
      sb.append("' align='left' valign='top'")
      //      .append(" hspace='10' vspace='2'")
            .append(">\n");
    }

    sb.append("Catalog ").append(catname);
    sb.append("</h1>");
    sb.append("<HR size='1' noshade='noshade'>");

    // Try to render README.md in tds.content.path
    if (generateMarkdown(FileSystems.getDefault().getPath(tdsContext.getContentDirectory().toString(), "README.md"), sb)) {
      sb.append("<HR size='1' noshade='noshade'>");
    }

    // Try to render catalog.md or README.md from collection directory
    if (appendMarkdown(cat, sb)) {
      sb.append("<HR size='1' noshade='noshade'>");
    }

    sb.append("<table width='100%' cellspacing='0' cellpadding='5' align='center'>\r\n");

    // Render the column headings
    sb.append("<tr>\r\n");
    sb.append("<th align='left'><font size='+1'>");
    sb.append("Dataset");
    sb.append("</font></th>\r\n");
    sb.append("<th align='center'><font size='+1'>");
    sb.append("Size");
    sb.append("</font></th>\r\n");
    sb.append("<th align='right'><font size='+1'>");
    sb.append("Last Modified");
    sb.append("</font></th>\r\n");
    sb.append("</tr>");

    // Recursively render the datasets
    doDatasets(cat, cat.getDatasets(), sb, false, 0, isLocalCatalog);

    // Render the page footer
    sb.append("</table>\r\n");

    sb.append("<HR size='1' noshade='noshade'>");

    appendSimpleFooter(sb);

    sb.append("</body>\r\n");
    sb.append("</html>\r\n");

    return sb.toString();
  }

  private boolean doDatasets(InvCatalogImpl cat, List<InvDataset> datasets, StringBuilder sb, boolean shade, int level, boolean isLocalCatalog) {
    //URI catURI = cat.getBaseURI();
    String catHtml;
    if (!isLocalCatalog) {
        // Setup HREF url to link to HTML dataset page (more below).
        catHtml = this.tdsContext.getContextPath() + "/remoteCatalogService?command=subset&catalog=" + cat.getUriString() + "&";
        // Can't be "/catalogServices?..." because subset decides on xml or html by trailing ".html" on URL path
    } else { // replace xml with html
      URI catURI = cat.getBaseURI();
      // Get the catalog name - we want a relative URL
      catHtml = catURI.getPath();
      if (catHtml == null) catHtml = cat.getUriString();  // if URI is a file
      int pos = catHtml.lastIndexOf("/");
      if (pos != -1) catHtml = catHtml.substring(pos + 1);

      // change the ending to "catalog.html?"
      pos = catHtml.lastIndexOf('.');
      if (pos < 0)
        catHtml = catHtml + "catalog.html?";
      else
        catHtml = catHtml.substring(0, pos) + ".html?";
    }

    for (InvDataset dataset : datasets) {
      InvDatasetImpl ds = (InvDatasetImpl) dataset;
      String name = StringUtil2.quoteHtmlContent(ds.getName());

      sb.append("<tr");
      if (shade) {
        sb.append(" bgcolor='#eeeeee'");
      }
      sb.append(">\r\n");
      shade = !shade;

      sb.append("<td align='left'>");
      for (int j = 0; j <= level; j++) {
        sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
      }
      sb.append("\r\n");

      if (ds instanceof InvCatalogRef) {
        InvCatalogRef catref = (InvCatalogRef) ds;
        String href = catref.getXlinkHref();
        if (!isLocalCatalog) {
          URI hrefUri = cat.getBaseURI().resolve(href);
          href = hrefUri.toString();
        }
        try {
          URI uri = new URI(href);
          if (uri.isAbsolute()) {
            // read default as set in threddsConfig.xml
            boolean defaultUseRemoteCatalogService = this.htmlConfig.getUseRemoteCatalogService();

            // check to see if catalogRef contains tag that overrides default
            Boolean dsUseRemoteCatalogSerivce = ((InvCatalogRef) ds).useRemoteCatalogService();

            // by default, use the option found in threddsConfig.xml
            boolean useRemoteCatalogService = defaultUseRemoteCatalogService;

            // if the dataset does not have the useRemoteDataset option set, opt for the default behavior
            if (dsUseRemoteCatalogSerivce == null) dsUseRemoteCatalogSerivce = defaultUseRemoteCatalogService;

            // if the default is not the same as what is defined in the catalog, go with the catalog option
            // as the user has explicitly overridden the default
            if (defaultUseRemoteCatalogService != dsUseRemoteCatalogSerivce) {
             useRemoteCatalogService = dsUseRemoteCatalogSerivce;
            }

            // now, do the right thing with using the remoteCatalogService, or not
            if (useRemoteCatalogService) {
              href = this.tdsContext.getContextPath() + "/remoteCatalogService?catalog=" + href;
            } else {
              int pos = href.lastIndexOf('.');
              href = href.substring(0, pos) + ".html";
            }
          } else {
            int pos = href.lastIndexOf('.');
            href = href.substring(0, pos) + ".html";
          }

        } catch (URISyntaxException e) {
          log.error(href, e);
        }

        sb.append("<img src='").append(htmlConfig.prepareUrlStringForHtml(htmlConfig.getFolderIconUrl()))
                .append("' alt='").append(htmlConfig.getFolderIconAlt()).append("'> &nbsp;");
        sb.append("<a href='");
        sb.append(StringUtil2.quoteHtmlContent(href));
        sb.append("'><tt>");
        sb.append(name);
        sb.append("/</tt></a></td>\r\n");
      } else // Not an InvCatalogRef
      {
        if (ds.hasNestedDatasets())
          sb.append("<img src='").append(htmlConfig.prepareUrlStringForHtml(htmlConfig.getFolderIconUrl()))
                  .append("' alt='").append(htmlConfig.getFolderIconAlt()).append("'> &nbsp;");

        // Check if dataset has single resolver service.
        if (ds.getAccess().size() == 1 &&
                (ds.getAccess().get(0)).getService().getServiceType().equals(ServiceType.RESOLVER)) {
          InvAccess access = ds.getAccess().get(0);
          String accessUrlName = access.getUnresolvedUrlName();
          int pos = accessUrlName.lastIndexOf(".xml");

          if (accessUrlName.equalsIgnoreCase("latest.xml") && !isLocalCatalog) {
            String catBaseUriPath = "";
            String catBaseUri = cat.getBaseURI().toString();
            pos = catBaseUri.lastIndexOf("catalog.xml");
            if (pos != -1) {
              catBaseUriPath = catBaseUri.substring(0,pos);
            }
            accessUrlName = this.tdsContext.getContextPath() + "/remoteCatalogService?catalog=" + catBaseUriPath + accessUrlName;
          } else if (pos != -1) {
            accessUrlName = accessUrlName.substring(0, pos) + ".html";
          }

          sb.append("<a href='");
          sb.append(StringUtil2.quoteHtmlContent(accessUrlName));
          sb.append("'><tt>");
          String tmpName = name;
          if (tmpName.endsWith(".xml")) {
            tmpName = tmpName.substring(0, tmpName.lastIndexOf('.'));
          }
          sb.append(tmpName);
          sb.append("</tt></a></td>\r\n");
        }
        // Dataset with an ID.
        else if (ds.getID() != null) {
          // Write link to HTML dataset page.
          sb.append("<a href='");
          sb.append(StringUtil2.quoteHtmlContent(catHtml));
          sb.append("dataset=");
          sb.append(StringUtil2.replace(ds.getID(), '+', "%2B"));
          sb.append("'><tt>");
          sb.append(name);
          sb.append("</tt></a></td>\r\n");
        }
        // Dataset without an ID.
        else {
          sb.append("<tt>");
          sb.append(name);
          sb.append("</tt></td>\r\n");
        }
      }

      sb.append("<td align='right'><tt>");
      double size = ds.getDataSize();
      if ((size != 0.0) && !Double.isNaN(size)) {
        sb.append(Format.formatByteSize(size));
      } else {
        sb.append("&nbsp;");
      }
      sb.append("</tt></td>\r\n");

      sb.append("<td align='right'><tt>");

      // Get last modified time.
      DateType lastModDateType = ds.getLastModifiedDate();
      if (lastModDateType == null) {
          sb.append("--");// "Unknown");
      } else {
        sb.append(lastModDateType.toDateTimeString());
      }

      sb.append("</tt></td>\r\n");

      sb.append("</tr>\r\n");

      if (!(ds instanceof InvCatalogRef)) {
        shade = doDatasets(cat, ds.getDatasets(), sb, shade, level + 1, isLocalCatalog);
      }
    }

    return shade;
  }

  private String convertDatasetToHtml(String catURL, InvDatasetImpl dataset,
                                      HttpServletRequest request,
                                      boolean isLocalCatalog) {
    StringBuilder sb = new StringBuilder(10000);

    sb.append(this.getHtmlDoctypeAndOpenTag());
    sb.append("<head>\r\n");
    sb.append("<title> Catalog Services</title>\r\n");
    sb.append("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>\r\n");
    sb.append(this.getTdsPageCssLink()).append("\n");
    sb.append(this.getGoogleTrackingContent());
    sb.append("</head>\r\n");
    sb.append("<body>\r\n");
    this.appendOldStyleHeader(sb);
    //this.appendTableHeader( sb, true, true, true );

    sb.append("<h2> Catalog ").append(catURL).append("</h2>\r\n");

    InvDatasetImpl.writeHtmlDescription(sb, dataset, false, true, false, false, !isLocalCatalog);

    // optional access through Viewers
    if (isLocalCatalog)
      viewerService.showViewers(sb, dataset, request);
    
    sb.append( "</body>\r\n" );
    sb.append( "</html>\r\n" );

    return sb.toString();
  }

  public int showDataset(String catURL, InvDatasetImpl dataset,
                         HttpServletRequest request,
                         HttpServletResponse response,
                         boolean isLocalCatalog)
          throws IOException {
    String datasetAsHtml = this.convertDatasetToHtml(catURL, dataset, request, isLocalCatalog);

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(ContentType.html.getContentHeader());
    if (!request.getMethod().equals("HEAD")) {
      PrintWriter pw = response.getWriter();
      pw.write(datasetAsHtml);
      pw.flush();
    }

    return datasetAsHtml.length();
  }

  /**
   * Show CDM compliance (coordinate systems, etc) of a NetcdfDataset.
   *
   * @param ds dataset to write
   */
  public void showCDM(HttpServletResponse res, NetcdfDataset ds)
          throws IOException {
    String cdmAsString = getCDM(ds);

    res.setContentType(ContentType.html.getContentHeader());
    thredds.servlet.ServletUtil.setResponseContentLength(res, cdmAsString);
    PrintWriter writer = res.getWriter();

    writer.write(cdmAsString);
    writer.flush();
  }

  private String getCDM(NetcdfDataset ds) throws IOException {
    StringBuilder sb = new StringBuilder(10000);

    String name = StringUtil2.quoteHtmlContent(ds.getLocation());

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

    sb.append("<table width='100%' cellspacing='0'" +
            " cellpadding='5' align='center'>\r\n");

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

    // look for projections
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
    String name = StringUtil2.quoteHtmlContent(sbuff.toString());
    sb.append("&nbsp;");
    sb.append(name);
    sb.append("</tt></a></td>\r\n");

    sb.append("<td align='left'><tt>");
    AxisType type = axis.getAxisType();
    String stype = (type == null) ? "" : StringUtil2.quoteHtmlContent(type.toString());
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
    String name = StringUtil2.quoteHtmlContent(sbuff.toString());
    sb.append("&nbsp;");
    sb.append(name);
    sb.append("</tt></a></td>\r\n");

    sb.append("<td align='left'><tt>");
    String desc = ve.getDescription();
    String sdesc = (desc == null) ? "" : StringUtil2.quoteHtmlContent(desc);
    sb.append(sdesc);
    sb.append("</tt></td>\r\n");

    sb.append("<td align='left'><tt>");
    String units = ve.getUnitsString();
    String sunits = (units == null) ? "" : units;
    sb.append(sunits);
    sb.append("</tt></td>\r\n");

    sb.append("</tr>\r\n");
  }

}

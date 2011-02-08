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
import thredds.server.config.TdsContext;
import thredds.server.config.HtmlConfig;
import ucar.nc2.units.DateType;

import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;
import ucar.unidata.util.Format;
import ucar.unidata.util.StringUtil;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.*;

/**
 * Provide methods to write HTML representations of a catalog, directory, or CDM dataset to an HTTP response.
 * <p/>
 * HtmlWriter is implemented as a singleton. Before HtmlWriter can be used it
 * must be initialized with init(...). The singleton instance can then be
 * obtained with getInstance().
 *
 * @author edavis
 */
public class HtmlWriter {
  static private org.slf4j.Logger log =
      org.slf4j.LoggerFactory.getLogger(HtmlWriter.class);

  private static HtmlWriter singleton;

  private TdsContext tdsContext;
  private HtmlConfig htmlConfig;

  private ucar.nc2.units.DateFormatter formatter = new ucar.nc2.units.DateFormatter();

  private HtmlWriter() {}

  public void setTdsContext( TdsContext tdsContext ) {
    this.tdsContext = tdsContext;
  }

  public void setHtmlConfig( HtmlConfig htmlConfig ) {
    this.htmlConfig = htmlConfig;
  }

  public void setSingleton( HtmlWriter self) {
    this.singleton = self;
  }

  public static HtmlWriter getInstance() {
    return singleton;
  }

  public String getHtmlDoctypeAndOpenTag() {
    return new StringBuilder()
        .append("<!DOCTYPE html PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'\n")
        .append("        'http://www.w3.org/TR/html4/loose.dtd'>\n")
        .append("<html>\n")
        .toString();
  }

  public String getXHtmlDoctypeAndOpenTag() {
    return new StringBuilder()
        // .append( "<?xml version='1.0' encoding='utf-8'?>")
        .append("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN'\n")
        .append("        'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>\n")
        .append("<html xmlns='http://www.w3.org/1999/xhtml' xml:lang='en' lang='en'>")
        .toString();
  }

  //  public static final String UNIDATA_CSS
  public String getUserCSS() {
    return new StringBuilder()
        .append("<link rel='stylesheet' href='")
        .append( this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getPageCssUrl() ) )
        .append("' type='text/css' >").toString();
  }

  public String getTdsCatalogCssLink() {
    return new StringBuilder()
        .append("<link rel='stylesheet' href='")
        .append( this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getCatalogCssUrl() ) )
        .append("' type='text/css' >").toString();
  }

  public String getTdsPageCssLink() {
    return new StringBuilder()
        .append("<link rel='stylesheet' href='")
        .append( this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getPageCssUrl() ))
        .append("' type='text/css' >").toString();
  }

  //  public static final String UNIDATA_HEAD
  public String getUserHead() {
    return new StringBuilder()
        .append("<table width='100%'><tr><td>\n")
        .append("  <img src='").append( this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getHostInstLogoUrl() ))
            .append("'\n")
        .append("       alt='").append( this.htmlConfig.getHostInstLogoAlt()).append("'\n")
        .append("       align='left' valign='top'\n")
        .append("       hspace='10' vspace='2'>\n")
        .append("  <h3><strong>").append( this.tdsContext.getWebappName()).append("</strong></h3>\n")
        .append("</td></tr></table>\n")
        .toString();
  }

  public String getOldStyleHeader()
  {
    StringBuilder sb = new StringBuilder();
    appendOldStyleHeader( sb );
    return sb.toString();
  }

  public void appendOldStyleHeader( StringBuilder sb )
  {
    appendOldStyleHeader( sb,
                          this.htmlConfig.getWebappName(), this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getWebappUrl() ),
                          this.htmlConfig.getInstallLogoAlt(), this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getInstallLogoUrl() ),
                          this.htmlConfig.getInstallName(), this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getInstallUrl() ),
                          this.htmlConfig.getHostInstName(), this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getHostInstUrl() ) );
  }

  public void appendOldStyleHeader( StringBuilder sb,
                                    String webappName, String webappUrl,
                                    String logoAlt, String logoUrl,
                                    String installName, String installUrl,
                                    String hostName, String hostUrl)
  {
    // Table setup.
    sb.append( "<table width='100%'>\n" )
            .append( "<tr><td>\n" );
    // Logo
    sb.append( "<img src='" ).append( logoUrl )
            .append( "' alt='" ).append( logoAlt )
            .append( "' align='left' valign='top'" )
            .append( " hspace='10' vspace='2'" )
            .append( ">\n" );

    // Installation name.
    sb.append( "<h3><strong>" )
            .append( "<a href='" ).append( installUrl ).append( "'>" )
            .append( installName ).append( "</a>" )
            .append( "</strong>");
    if ( false ) sb.append( " at ").append( hostName);
    sb.append( "</h3>\n" );

    // Webapp Name.
    sb.append( "<h3><strong>" )
            .append( "<a href='" ).append( webappUrl ).append( "'>" )
            .append( webappName ).append( "</a>" )
            .append( "</strong></h3>\n" );

    sb.append( "</td></tr>\n" )
            .append( "</table>\n" );
  }

  public void appendTableHeader( StringBuilder stringBuilder,
                                 boolean includeInstall,
                                 boolean includeWebapp,
                                 boolean includeLogos)
  {
    // Table setup.
    stringBuilder
        .append( "<table width='100%'>\n");

    if ( includeInstall )
    {
      stringBuilder.append( "<tr><td>\n");
      appendInstallationInfo( stringBuilder, includeLogos );
      stringBuilder.append( "</td><td>\n");
      appendHostInstInfo( stringBuilder, includeLogos );
      stringBuilder.append( "</td></tr>\n" );
    }

    if ( includeWebapp )
    {
      stringBuilder
              .append( "<tr><td>\n" );
      appendWebappInfo( stringBuilder, includeLogos );
      stringBuilder.append( "</td></tr>\n" );
    }
    stringBuilder.append( "</table><hr>\n");
  }

  private void appendWebappInfo( StringBuilder stringBuilder, boolean includeLogo )
  {
    // Include webapp info
    String webappUrl = this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getWebappUrl() );
    String webappLogoUrl = this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getWebappLogoUrl() );
    if ( includeLogo && webappLogoUrl != null )
      stringBuilder
              .append( "<img src='" ).append( webappLogoUrl )
              .append( "' alt='" ).append( this.htmlConfig.getWebappLogoAlt() )
              .append( "'> " );
    stringBuilder
            .append( "<a href='" ).append( webappUrl ).append( "'>" )
            .append( this.tdsContext.getWebappName() )
            .append( "</a>");
  }

  private void appendHostInstInfo( StringBuilder stringBuilder, boolean includeLogo )
  {
    // Include host institution information
    if ( this.htmlConfig.getHostInstName() != null )
    {
      String hostInstUrl = this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getHostInstUrl() );
      String hostInstLogoUrl = this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getHostInstLogoUrl() );
      if ( includeLogo && hostInstLogoUrl != null )
        stringBuilder
                .append( "<img src='" ).append( hostInstLogoUrl )
                .append( "' alt='" ).append( this.htmlConfig.getHostInstLogoAlt() )
                .append( "'> " );
      if ( hostInstUrl != null )
        stringBuilder.append( "<a href='" ).append( hostInstUrl ).append( "'>" );
      stringBuilder.append( this.htmlConfig.getHostInstName() );
      if ( hostInstUrl != null )
        stringBuilder.append( "</a>" );
    }
    else
      stringBuilder.append( "Unknown Host Institution");
  }

  private void appendInstallationInfo( StringBuilder stringBuilder, boolean includeLogo )
  {
    // Include information on this intsallation.
    if ( this.htmlConfig.getInstallName() != null )
    {
      String installUrl = this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getInstallUrl() );
      String installLogoUrl = this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getInstallLogoUrl() );
      if ( includeLogo && installLogoUrl != null )
        stringBuilder
                .append( "<img src='" ).append( installLogoUrl )
                .append( "' alt='" ).append( this.htmlConfig.getInstallLogoAlt() )
                .append( "'> " );
      if ( installUrl != null )
        stringBuilder.append( "<a href='" ).append( installUrl ).append( "'>" );
      stringBuilder.append( this.htmlConfig.getInstallName() );
      if ( installUrl != null )
        stringBuilder.append( "</a>" );
    }
    else
    {
      // This installation is not named.
      stringBuilder.append("Unnamed TDS Installation");
    }
  }

  private void appendSimpleFooter( StringBuilder sb )
  {
    sb.append( "<h3>" );
    if ( this.htmlConfig.getInstallName() != null )
    {
      String installUrl = this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getInstallUrl() );
      if ( installUrl != null )
        sb.append( "<a href='" ).append( installUrl ).append( "'>" );
      sb.append( this.htmlConfig.getInstallName() );
      if ( installUrl != null )
        sb.append( "</a>" );
    }
    if ( this.htmlConfig.getHostInstName() != null )
    {
      sb.append( " at " );
      String hostInstUrl = this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getHostInstUrl() );
      if ( hostInstUrl != null )
        sb.append( "<a href='" ).append( hostInstUrl ).append( "'>" );
      sb.append( this.htmlConfig.getHostInstName() );
      if ( hostInstUrl != null )
        sb.append( "</a>" );
      sb.append( "<br>\n" );
    }
    sb.append( this.tdsContext.getWebappName() )
            .append( " [Version " ).append( this.tdsContext.getWebappVersion() );
    if ( this.tdsContext.getWebappVersionBuildDate() != null )
      sb.append( " - " ).append( this.tdsContext.getWebappVersionBuildDate() );
    sb.append( "] <a href='" )
            .append( this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getWebappDocsUrl() ) )
            .append( "'> Documentation</a>" );
    sb.append( "</h3>\n" );
  }

  private void appendWebappFooter( StringBuilder sb )
  {
    sb.append( "<h3>" )
            .append( this.tdsContext.getWebappName() )
            .append( " [Version " ).append( this.tdsContext.getWebappVersion() );
    if ( this.tdsContext.getWebappVersionBuildDate() != null )
      sb.append( " - " ).append( this.tdsContext.getWebappVersionBuildDate() );
    sb.append( "] <a href='" )
            .append( this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getWebappDocsUrl() ) )
            .append( "'> Documentation</a>" );
    sb.append( "</h3>\n" );
  }

  //  private static final String TOMCAT_CSS
  private String getTomcatCSS() {
    return new StringBuilder( "<STYLE type='text/css'><!--" )
        .append("H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} ")
        .append("H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} ")
        .append("H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} ")
        .append("BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} ")
        .append("B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} ")
        .append("P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}")
        .append("A {color : black;}")
        .append("A.name {color : black;}")
        .append("HR {color : #525D76;}")
        .append( "--></STYLE>\r\n" )
    .toString();
  }

  /**
   * Write a file directory.
   *
   * @param res the HttpServletResponse on which to write the file directory response.
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

    res.setContentLength(dirHtmlString.length());
    res.setContentType("text/html; charset=UTF-8");
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
    sb.append(this.getTdsCatalogCssLink()).append( "\n");
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
    List<File> fileList = Arrays.asList(children);
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
      sb.append(formatter.toDateTimeString(new Date(child.lastModified())));
      sb.append("</tt></td>\r\n");

      sb.append("</tr>\r\n");
    }

    // Render the page footer
    sb.append("</table>\r\n");
    sb.append("<HR size='1' noshade='noshade'>");

    appendSimpleFooter( sb );

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
   * Write an InvCatalogImpl to the HttpServletResponse, return the size in bytes of the catalog writtn to the response.
   *
   * @param req the HttpServletRequest
   * @param res the HttpServletResponse.
   * @param cat the InvCatalogImpl to write to the HttpServletResponse.
   * @param isLocalCatalog indicates whether this catalog is local to this server.
   * @return the size in bytes of the catalog written to the HttpServletResponse.
   * @throws IOException if problems writing the response.
   */
  public int writeCatalog(HttpServletRequest req, HttpServletResponse res, InvCatalogImpl cat, boolean isLocalCatalog)
      throws IOException {
    String catHtmlAsString = convertCatalogToHtml(cat, isLocalCatalog);

    res.setContentLength(catHtmlAsString.length());
    res.setContentType("text/html; charset=UTF-8");
    if ( ! req.getMethod().equals( "HEAD" ) )
    {
      PrintWriter writer = res.getWriter();
      writer.write(catHtmlAsString);
      writer.flush();
    }

    return catHtmlAsString.length();
  }

  /**
   * Write a catalog in HTML, make it look like a file directory.
   *
   * @param cat catalog to write
   */
  String convertCatalogToHtml(InvCatalogImpl cat, boolean isLocalCatalog) {
    StringBuilder sb = new StringBuilder(10000);

    String catname = StringUtil.quoteHtmlContent(cat.getUriString());

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
    sb.append("</head>\r\n");
    sb.append("<body>");
    sb.append("<h1>");
    sb.append("Catalog ").append(catname);
    sb.append("</h1>");
    sb.append("<HR size='1' noshade='noshade'>");

    sb.append("<table width='100%' cellspacing='0'" +
        " cellpadding='5' align='center'>\r\n");

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
    boolean shade = false;
    shade = doDatasets(cat, cat.getDatasets(), sb, shade, 0, isLocalCatalog);

    // Render the page footer
    sb.append("</table>\r\n");

    sb.append("<HR size='1' noshade='noshade'>");

    appendSimpleFooter( sb );

    sb.append("</body>\r\n");
    sb.append("</html>\r\n");

    return (sb.toString());
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
      int pos = catHtml.lastIndexOf("/");
      if ( pos != -1) catHtml = catHtml.substring( pos + 1 );

      // change the ending to "catalog.html?"
      pos = catHtml.lastIndexOf('.');
      if (pos < 0)
        catHtml = catHtml + "catalog.html?";
      else
        catHtml = catHtml.substring(0, pos) + ".html?";
    }

    for (InvDataset dataset : datasets) {
      InvDatasetImpl ds = (InvDatasetImpl) dataset;
      String name = StringUtil.quoteHtmlContent(ds.getName());

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
            href = this.tdsContext.getContextPath() + "/remoteCatalogService?catalog=" + href;
          } else {
            int pos = href.lastIndexOf('.');
            href = href.substring(0, pos) + ".html";
          }

        } catch (URISyntaxException e) {
          log.error(href, e);
        }

        sb.append("<img src='").append( htmlConfig.prepareUrlStringForHtml( htmlConfig.getFolderIconUrl() ))
            .append("' alt='").append( htmlConfig.getFolderIconAlt()).append("'> &nbsp;");
        sb.append("<a href='");
        sb.append(StringUtil.quoteHtmlContent(href));
        sb.append("'><tt>");
        sb.append(name);
        sb.append("/</tt></a></td>\r\n");
      } else // Not an InvCatalogRef
      {
        if (ds.hasNestedDatasets())
            sb.append( "<img src='" ).append( htmlConfig.prepareUrlStringForHtml( htmlConfig.getFolderIconUrl() ) )
                    .append( "' alt='" ).append( htmlConfig.getFolderIconAlt() ).append( "'> &nbsp;" );

          // Check if dataset has single resolver service.
        if (ds.getAccess().size() == 1 &&
            (ds.getAccess().get(0)).getService().getServiceType().equals(ServiceType.RESOLVER)) {
          InvAccess access = ds.getAccess().get(0);
          String accessUrlName = access.getUnresolvedUrlName();
          int pos = accessUrlName.lastIndexOf(".xml");
          if (pos != -1)
            accessUrlName = accessUrlName.substring(0, pos) + ".html";
          sb.append("<a href='");
          sb.append(StringUtil.quoteHtmlContent(accessUrlName));
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
          sb.append(StringUtil.quoteHtmlContent(catHtml));
          sb.append("dataset=");
          sb.append( StringUtil.replace( ds.getID(), '+', "%2B" ) );
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
        if (!ds.hasAccess())
          sb.append("--");// "");
        else
          sb.append("--");// "Unknown");
      } else {
        if (lastModDateType.isPresent())
          sb.append(formatter.toDateTimeString(new Date()));
        if (lastModDateType.getDate() != null)
          sb.append(formatter.toDateTimeString(lastModDateType.getDate()));
      }

      sb.append("</tt></td>\r\n");

      sb.append("</tr>\r\n");

      if (!(ds instanceof InvCatalogRef)) {
        shade = doDatasets(cat, ds.getDatasets(), sb, shade, level + 1, isLocalCatalog);
      }
    }

    return shade;
  }

  private String convertDatasetToHtml( String catURL, InvDatasetImpl dataset,
                                       HttpServletRequest request,
                                       boolean isLocalCatalog)
  {
    StringBuilder sb = new StringBuilder( 10000 );

    sb.append( this.getHtmlDoctypeAndOpenTag() );
    sb.append( "<head>\r\n" );
    sb.append( "<title> Catalog Services</title>\r\n" );
    sb.append( "<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>\r\n" );
    sb.append( this.getTdsPageCssLink() );
    sb.append( "</head>\r\n" );
    sb.append( "<body>\r\n" );
    this.appendOldStyleHeader( sb );
    //this.appendTableHeader( sb, true, true, true );

    sb.append( "<h2> Catalog " ).append( catURL ).append( "</h2>\r\n" );

    InvDatasetImpl.writeHtmlDescription( sb, dataset, false, true, false, false, ! isLocalCatalog );

    // optional access through Viewers
    if ( isLocalCatalog )
      ViewServlet.showViewers( sb, dataset, request );

    sb.append( "</body>\r\n" );
    sb.append( "</html>\r\n" );

    return sb.toString();
  }

  public int showDataset( String catURL, InvDatasetImpl dataset,
                           HttpServletRequest request,
                           HttpServletResponse response,
                           boolean isLocalCatalog )
          throws IOException
  {
    String datasetAsHtml = this.convertDatasetToHtml( catURL, dataset, request, isLocalCatalog );

    response.setStatus( HttpServletResponse.SC_OK );
    response.setContentType( "text/html; charset=UTF-8" );
    if ( ! request.getMethod().equals( "HEAD" ) )
    {
      PrintWriter pw = response.getWriter();
      pw.write( datasetAsHtml );
      pw.flush();
    }

    return datasetAsHtml.length();
  }

  /**
   * Show CDM compliance (ccordinate systems, etc) of a NetcdfDataset.
   *
   * @param ds dataset to write
   */
  public void showCDM(HttpServletResponse res, NetcdfDataset ds)
      throws IOException {
    String cdmAsString = getCDM(ds);

    res.setContentLength(cdmAsString.length());
    res.setContentType("text/html; charset=UTF-8");
    PrintWriter writer = res.getWriter();

    writer.write(cdmAsString);
    writer.flush();

    log.info( UsageLog.closingMessageForRequestContext(HttpServletResponse.SC_OK, cdmAsString.length()));

  }

  private String getCDM(NetcdfDataset ds) throws IOException {
    StringBuilder sb = new StringBuilder(10000);

    String name = StringUtil.quoteHtmlContent(ds.getLocation());

    // Render the page header
    sb.append(getHtmlDoctypeAndOpenTag()); // "<html>\n" );
    sb.append("<head>\r\n");
    sb.append("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
    sb.append("<title>");
    sb.append("Common Data Model");
    sb.append("</title>\r\n");
    sb.append(this.getTdsPageCssLink()).append( "\n");
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

    appendSimpleFooter( sb );

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
    String name = StringUtil.quoteHtmlContent(sbuff.toString());
    sb.append("&nbsp;");
    sb.append(name);
    sb.append("</tt></a></td>\r\n");

    sb.append("<td align='left'><tt>");
    AxisType type = axis.getAxisType();
    String stype = (type == null) ? "" : StringUtil.quoteHtmlContent(type.toString());
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
    String name = StringUtil.quoteHtmlContent(sbuff.toString());
    sb.append("&nbsp;");
    sb.append(name);
    sb.append("</tt></a></td>\r\n");

    sb.append("<td align='left'><tt>");
    String desc = ve.getDescription();
    String sdesc = (desc == null) ? "" : StringUtil.quoteHtmlContent(desc);
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

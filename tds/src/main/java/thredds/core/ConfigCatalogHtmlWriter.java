/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.core;

import com.coverity.security.Escape;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import thredds.client.catalog.*;
import thredds.client.catalog.tools.DatasetHtmlWriter;
import thredds.server.catalog.CatalogScan;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.FeatureCollectionRef;
import thredds.server.config.HtmlConfigBean;
import thredds.server.config.TdsContext;
import thredds.server.viewer.ViewerService;
import thredds.servlet.ServletUtil;
import thredds.util.ContentType;
import ucar.nc2.units.DateType;
import ucar.unidata.util.Format;
import ucar.unidata.util.StringUtil2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Formatter;
import java.util.List;

/**
 * Write HTML representations of a Catalog or Dataset.
 * Used by both client and server.
 *
 * @author caron
 * @since 1/19/2015
 */
@Component
public class ConfigCatalogHtmlWriter {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConfigCatalogHtmlWriter.class);

  static public String getHtmlDoctypeAndOpenTag() {
    return "<!DOCTYPE html PUBLIC '-//W3C//DTD HTML 4.01 Transitional//EN'\n" +
            "        'http://www.w3.org/TR/html4/loose.dtd'>\n" + "<html>\n";
  }

  static public String getXHtmlDoctypeAndOpenTag() {
    return "<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN'\n" +
            "        'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>\n" +
            "<html xmlns='http://www.w3.org/1999/xhtml' xml:lang='en' lang='en'>";
  }

  @Autowired
  private ViewerService viewerService;

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private HtmlConfigBean htmlConfig;


  /**
   * Write an Catalog to the HttpServletResponse, return the size in bytes of the catalog written to the response.
   *
   * @param req            the HttpServletRequest
   * @param res            the HttpServletResponse.
   * @param cat            the InvCatalogImpl to write to the HttpServletResponse.
   * @param isLocalCatalog indicates whether this catalog is local to this server.
   * @return the size in bytes of the catalog written to the HttpServletResponse.
   * @throws IOException if problems writing the response.
   */
  public int writeCatalog(HttpServletRequest req, HttpServletResponse res, Catalog cat, boolean isLocalCatalog) throws IOException {
    String catHtmlAsString = convertCatalogToHtml(cat, isLocalCatalog);

    // Once this header is set, we know the encoding, and thus the actual
    // number of *bytes*, not characters, to encode
    res.setContentType(ContentType.html.getContentHeader());
    int len = ServletUtil.setResponseContentLength(res, catHtmlAsString);

    if (!req.getMethod().equals("HEAD")) {
      PrintWriter writer = res.getWriter();
      writer.write(catHtmlAsString);
      writer.flush();
    }

    return len;
  }

  /**
   * Write a catalog in HTML, make it look like a file directory.
   *
   * @param cat catalog to write
   */
  String convertCatalogToHtml(Catalog cat, boolean isLocalCatalog) {
    StringBuilder sb = new StringBuilder(10000);

    String uri = cat.getUriString();
    if (uri == null) uri = cat.getName();
    if (uri == null) uri = "unknown";
    String catname = Escape.html(uri);

    // Render the page header
    sb.append(getHtmlDoctypeAndOpenTag()); // "<html>\n" );
    sb.append("<head>\r\n");
    sb.append("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>");
    sb.append("<title>");
    //if (cat.isStatic())
    //  sb.append("TdsStaticCatalog ").append(catname); // for searching
    //else
    sb.append("Catalog ").append(catname);
    sb.append("</title>\r\n");
    sb.append(getTdsCatalogCssLink()).append("\n");
    sb.append(this.getGoogleTrackingContent());
    sb.append("</head>\r\n");
    sb.append("<body>");
    sb.append("<h1>");

    // Logo
    //String logoUrl = this.htmlConfig.getInstallLogoUrl();
    String logoUrl = htmlConfig.prepareUrlStringForHtml(htmlConfig.getInstallLogoUrl());
    if (logoUrl != null) {
      sb.append("<img src='").append(logoUrl);
      String logoAlt = htmlConfig.getInstallLogoAlt();
      if (logoAlt != null) sb.append("' alt='").append(logoAlt);
      sb.append("' align='left' valign='top'").append(">\n");
    }

    sb.append(" Catalog ").append(catname);
    sb.append("</h1>");
    sb.append("<HR size='1' noshade='noshade'>");

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
    doDatasets(cat, cat.getDatasetsLocal(), sb, false, 0, isLocalCatalog);

    // Render the page footer
    sb.append("</table>\r\n");
    sb.append("<HR size='1' noshade='noshade'>");
    appendSimpleFooter(sb);
    sb.append("</body>\r\n");
    sb.append("</html>\r\n");

    return (sb.toString());
  }

  private boolean doDatasets(Catalog cat, List<Dataset> datasets, StringBuilder sb, boolean shade, int level, boolean isLocalCatalog) {
    //URI catURI = cat.getBaseURI();
    String catHtml;
    if (!isLocalCatalog) {
      // Setup HREF url to link to HTML dataset page (more below).
      catHtml = tdsContext.getContextPath() + "/remoteCatalogService?command=subset&catalog=" + cat.getUriString() + "&";
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

    for (Dataset ds : datasets) {
      String name = ds.getName();
      if (name == null) name = ""; // eg catrefs
      else name = Escape.html(ds.getName());

      sb.append("<tr");
      if (shade) sb.append(" bgcolor='#eeeeee'");
      sb.append(">\r\n");
      shade = !shade;

      sb.append("<td align='left'>");
      for (int j = 0; j <= level; j++) sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
      sb.append("\r\n");

      if (ds instanceof CatalogRef) {
        CatalogRef catref = (CatalogRef) ds;
        String href = catref.getXlinkHref();
        if (!isLocalCatalog) {
          URI hrefUri = cat.getBaseURI().resolve(href);
          href = hrefUri.toString();
        }
        try {
          URI uri = new URI(href);
          if (uri.isAbsolute()) {
            boolean defaultUseRemoteCatalogService = htmlConfig.getUseRemoteCatalogService(); // read default as set in threddsConfig.xml
            Boolean dsUseRemoteCatalogSerivce = ((CatalogRef) ds).useRemoteCatalogService();  // check to see if catalogRef contains tag that overrides default
            boolean useRemoteCatalogService = defaultUseRemoteCatalogService; // by default, use the option found in threddsConfig.xml
            if (dsUseRemoteCatalogSerivce == null)
              dsUseRemoteCatalogSerivce = defaultUseRemoteCatalogService; // if the dataset does not have the useRemoteDataset option set, opt for the default behavior

            // if the default is not the same as what is defined in the catalog, go with the catalog option
            // as the user has explicitly overridden the default
            if (defaultUseRemoteCatalogService != dsUseRemoteCatalogSerivce) {
              useRemoteCatalogService = dsUseRemoteCatalogSerivce;
            }

            // now, do the right thing with using the remoteCatalogService, or not
            if (useRemoteCatalogService) {
              href = tdsContext.getContextPath() + "/remoteCatalogService?catalog=" + href;
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
        String folderIcon;
        if (ds instanceof CatalogScan || ds.hasProperty("CatalogScan"))
          folderIcon = "cat_folder.png";
        else if (ds instanceof DatasetScan || ds.hasProperty("DatasetScan"))
          folderIcon = "scan_folder.png";
        else if (ds instanceof FeatureCollectionRef)
          folderIcon = "fc_folder.png";
        else
          folderIcon = "folder.png";
          // folderIcon = htmlConfig.getFolderIconUrl();

        sb.append("<img src='").append(htmlConfig.prepareUrlStringForHtml(folderIcon))
                .append("' alt='").append(htmlConfig.getFolderIconAlt()).append("'> &nbsp;");
        sb.append("<a href='");
        sb.append(Escape.html(href));
        sb.append("'><tt>");
        sb.append(name);
        sb.append("</tt></a></td>\r\n");

      } else { // Not a CatalogRef
        if (ds.hasNestedDatasets())
          sb.append("<img src='").append(htmlConfig.prepareUrlStringForHtml("folder.png"))
                  .append("' alt='").append(htmlConfig.getFolderIconAlt()).append("'> &nbsp;");

        // Check if dataset has single resolver service.
        if (ds.getAccess().size() == 1 && ServiceType.Resolver == ds.getAccess().get(0).getService().getType()) {
          Access access = ds.getAccess().get(0);
          String accessUrlName = access.getUnresolvedUrlName();
          int pos = accessUrlName.lastIndexOf(".xml");

          if (accessUrlName.equalsIgnoreCase("latest.xml") && !isLocalCatalog) {
            String catBaseUriPath = "";
            String catBaseUri = cat.getBaseURI().toString();
            pos = catBaseUri.lastIndexOf("catalog.xml");
            if (pos != -1) {
              catBaseUriPath = catBaseUri.substring(0, pos);
            }
            accessUrlName = tdsContext.getContextPath() + "/remoteCatalogService?catalog=" + catBaseUriPath + accessUrlName;
          } else if (pos != -1) {
            accessUrlName = accessUrlName.substring(0, pos) + ".html";
          }

          sb.append("<a href='");
          sb.append(Escape.html(accessUrlName));
          sb.append("'><tt>");
          String tmpName = name;
          if (tmpName.endsWith(".xml")) {
            tmpName = tmpName.substring(0, tmpName.lastIndexOf('.'));
          }
          sb.append(tmpName);
          sb.append("</tt></a></td>\r\n");

        } else if (ds.findProperty(Dataset.NotAThreddsDataset) != null) { // Dataset can only be file served
          // Write link to HTML dataset page.
          sb.append("<a href='");
          sb.append(Escape.html(makeFileServerUrl(ds)));
          sb.append("'><tt>");
          sb.append(name);
          sb.append("</tt></a></td>\r\n");

        } else if (ds.getID() != null) { // Dataset with an ID.
          // Write link to HTML dataset page.
          sb.append("<a href='");
          sb.append(Escape.html(catHtml));
          sb.append("dataset=");
          sb.append(StringUtil2.replace(ds.getID(), '+', "%2B"));
          sb.append("'><tt>");
          sb.append(name);
          sb.append("</tt></a></td>\r\n");

        } else { // Dataset without an ID.
          sb.append("<tt>");
          sb.append(name);
          sb.append("</tt></td>\r\n");
        }
      }

      sb.append("<td align='right'><tt>");
      double size = ds.getDataSize();
      if ((size > 0) && !Double.isNaN(size)) {
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

      if (!(ds instanceof CatalogRef)) {
        shade = doDatasets(cat, ds.getDatasetsLocal(), sb, shade, level + 1, isLocalCatalog);
      }
    }

    return shade;
  }

  private String makeFileServerUrl(Dataset ds) {
    Access acc = ds.getAccess(ServiceType.HTTPServer);
    assert acc != null;
    return acc.getStandardUrlName();
  }

  private String convertDatasetToHtml(String catURL, Dataset dataset,
                                      HttpServletRequest request,
                                      boolean isLocalCatalog) {
    Formatter out = new Formatter();

    out.format("%s<head>%n", getHtmlDoctypeAndOpenTag());
    out.format("<title>Catalog Services</title>%n");
    out.format("<meta http-equiv='Content-Type' content='text/html; charset=UTF-8'>%n");
    out.format("%s%n", getTdsPageCssLink());
    out.format(getGoogleTrackingContent());  // String already has EOL.
    out.format("</head>%n");
    out.format("<body>%n");

    StringBuilder sb = new StringBuilder();
    appendOldStyleHeader(sb);
    out.format("%s%n", sb);

    out.format("<h2> Catalog %s</h2>%n", catURL);

    DatasetHtmlWriter writer = new DatasetHtmlWriter();
    // (Formatter out, Dataset ds, boolean complete, boolean isServer, boolean datasetEvents, boolean catrefEvents, boolean resolveRelativeUrls)
    writer.writeHtmlDescription(out, dataset, false, true, false, false, !isLocalCatalog);

    // optional access through Viewers
    if (isLocalCatalog)
      viewerService.showViewers(out, dataset, request);

    out.format("</body>%n");
    out.format("</html>%n");

    return out.toString();
  }

  public int showDataset(String catURL, Dataset dataset,
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


    //  public static final String UNIDATA_CSS
  public String getUserCSS() {
    return new StringBuilder()
            .append("<link rel='stylesheet' href='")
            .append(this.htmlConfig.prepareUrlStringForHtml(this.htmlConfig.getPageCssUrl()))
            .append("' type='text/css' >").toString();
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
      sb.append(" see <a href='/thredds/info/serverInfo.html'> Info </a>");
      sb.append("<br>\n");
    }

    sb.append( this.tdsContext.getWebappDisplayName() )
            .append( " [Version " ).append( this.tdsContext.getVersionInfo() );
    sb.append( "] <a href='" )
            .append( this.htmlConfig.prepareUrlStringForHtml( this.htmlConfig.getWebappDocsUrl() ) )
            .append( "'> Documentation</a>" );
    sb.append( "</h3>\n" );
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
}

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
package thredds.server.config;

import thredds.servlet.ThreddsConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class HtmlConfig
{
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( HtmlConfig.class );

  private String webappName;
  private String webappVersion;
  private String webappVersionBrief;
  private String webappVersionBuildDate;
  private String webappContextPath;

  private String webappUrl;
  private String webappDocsUrl;
  private String webappLogoUrl;
  private String webappLogoAlt;

  private String pageCssUrl;
  private String catalogCssUrl;

  private String folderIconUrl;
  private String folderIconAlt;
  private String datasetIconUrl;
  private String datasetIconAlt;

  private String installName;
  private String installUrl;
  private String installLogoUrl;
  private String installLogoAlt;

  private String hostInstName;
  private String hostInstUrl;
  private String hostInstLogoUrl;
  private String hostInstLogoAlt;

  public HtmlConfig()
  {}

  public void init( String webappName,
                    String webappVersion, String webappVersionBrief,
                    String webappVersionBuildDate,
                    String webappContextPath )
  {
    this.webappName = webappName;
    this.webappVersion = webappVersion;
    this.webappVersionBrief = webappVersionBrief;
    this.webappVersionBuildDate = webappVersionBuildDate;
    this.webappContextPath = webappContextPath;
  }

  public String getWebappName() {
    return this.webappName;
  }

  public String getWebappVersion() {
    return webappVersion;
  }

  public String getWebappVersionBrief() {
    return webappVersionBrief;
  }

  public String getWebappVersionBuildDate() {
    return webappVersionBuildDate;
  }

  public String getWebappContextPath() {
    return webappContextPath;
  }

  /**
   * Return the URL to the main web page for the webapp.
   *
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the main web page for the webapp.
   */
  public String getWebappUrl() {
    return webappUrl;
  }

  public void setWebappUrl( String webappUrl) {
    this.webappUrl = webappUrl;
  }

  /**
   * Return the URL to the webapp documentation page.
   *
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the webapp documentation page.
   */
  public String getWebappDocsUrl() {
    return webappDocsUrl;
  }

  public void setWebappDocsUrl( String webappDocsUrl ) {
    this.webappDocsUrl = webappDocsUrl;
  }

  /**
   * Return the Url to the logo file for the webapp.
   *
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the path to the logo file for the webapp.
   */
  public String getWebappLogoUrl() {
    return webappLogoUrl;
  }

  public void setWebappLogoUrl( String webappLogoUrl ) {
    this.webappLogoUrl = webappLogoUrl;
  }

  /**
   * Return the alternate text for the webapp logo.
   *
   * @return the alternate text for the webapp logo.
   */
  public String getWebappLogoAlt() {
    return webappLogoAlt;
  }

  public void setWebappLogoAlt( String webappLogoAlt ) {
    this.webappLogoAlt = webappLogoAlt;
  }

  /**
   * Return the URL to the CSS file used for all non-catalog HTML pages.
   *
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the Url to the CSS file used for all non-catalog HTML pages.
   */
  public String getPageCssUrl() {
    return pageCssUrl;
  }

  public void setPageCssUrl( String pageCssUrl ) {
    this.pageCssUrl = pageCssUrl;
  }

  /**
   * Return the URL to the CSS file used for catalog HTML pages.
   *
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the CSS file used for catalog HTML pages.
   */
  public String getCatalogCssUrl() {
    return catalogCssUrl;
  }

  public void setCatalogCssUrl( String catalogCssUrl ) {
    this.catalogCssUrl = catalogCssUrl;
  }

  /**
   * Return the URL to the icon document used for folders in HTML catalog views.
   *
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the icon document used for folders in HTML catalog views.
   */
  public String getFolderIconUrl() {
    return folderIconUrl;
  }

  public void setFolderIconUrl( String folderIconUrl ) {
    this.folderIconUrl = folderIconUrl;
  }

  public String getFolderIconAlt() {
    return folderIconAlt;
  }

  public void setFolderIconAlt( String folderIconAlt ) {
    this.folderIconAlt = folderIconAlt;
  }

  /**
   * Return the URL to the icon document used for datasets in HTML catalog views.
   *
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the icon document used for datasets in HTML catalog views.
   */
  public String getDatasetIconUrl() {
    return datasetIconUrl;
  }

  public void setDatasetIconUrl( String datasetIconUrl ) {
    this.datasetIconUrl = datasetIconUrl;
  }

  public String getDatasetIconAlt() {
    return datasetIconAlt;
  }

  public void setDatasetIconAlt( String datasetIconAlt ) {
    this.datasetIconAlt = datasetIconAlt;
  }

  /**
   * Return the name of this TDS installation.
   *
   * @return the name of this TDS installation.
   */
  public String getInstallName() {
    return installName;
  }

  public void setInstallName( String installName ) {
    this.installName = installName;
  }

  /**
   * Return the URL to the top level of this TDS installation.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the top level of this installation.
   */
  public String getInstallUrl() {
    return installUrl;
  }

  public void setInstallUrl( String installUrl ) {
    this.installUrl = installUrl;
  }

  /**
   * Return the path to the logo file for this TDS installation.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the path to the logo file for this installation of the webapp.
   */
  public String getInstallLogoUrl() {
    return installLogoUrl;
  }

  public void setInstallLogoUrl( String installLogoUrl ) {
    this.installLogoUrl = installLogoUrl;
  }

  /**
   * Return the alternate text for the logo for this TDS installation.
   *
   * @return the alternate text for the logo for this installation.
   */
  public String getInstallLogoAlt() {
    return installLogoAlt;
  }

  public void setInstallLogoAlt( String installLogoAlt ) {
    this.installLogoAlt = installLogoAlt;
  }

  /**
   * Return the name of the institution hosting this TDS installation.
   *
   * @return the name of the institution hosting this TDS installation.
   */
  public String getHostInstName() {
    return hostInstName;
  }

  public void setHostInstName( String hostInstName ) {
    this.hostInstName = hostInstName;
  }

  /**
   * Return the URL to a web page for the institution hosting this installation.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to a web page for the institution hosting this installation.
   */
  public String getHostInstUrl() {
    return hostInstUrl;
  }

  public void setHostInstUrl( String hostInstUrl ) {
    this.hostInstUrl = hostInstUrl;
  }

  /**
   * Return the path to the logo file for the institution hosting this installation.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the path to the logo file for the institution hosting this installation.
   */
  public String getHostInstLogoUrl() {
    return hostInstLogoUrl;
  }

  public void setHostInstLogoUrl( String hostInstLogoUrl ) {
    this.hostInstLogoUrl = hostInstLogoUrl;
  }

  /**
   * Return the alternate text for the logo for the institution hosting this installation.
   *
   * @return the alternate text for the logo for the institution hosting this installation.
   */
  public String getHostInstLogoAlt() {
    return hostInstLogoAlt;
  }

  public void setHostInstLogoAlt( String hostInstLogoAlt ) {
    this.hostInstLogoAlt = hostInstLogoAlt;
  }

  /**
   * Return a URL ready to use in a generated HTML page from a URL that
   * is either absolute or relative to the webapp context path. That is,
   * if relative, it is relative to "http://server:port/thredds/".
   *
   * <p>For simplicity, all relative URLs are converted to URLs that are
   * absolute paths. For instance, "catalog.xml" becomes "/thredds/catalog.xml".
   *
   * @param url the URL to prepare for use in HTML.
   * @return a URL ready to use in a generated HTML page.
   */
  public String prepareUrlStringForHtml( String url )
  {
    if ( url == null )
      return null;
    URI uri = null;
    try
    {
      uri = new URI( url );
    }
    catch ( URISyntaxException e )
    {
      throw new IllegalArgumentException( "Given a bad URL [" + url + "].", e );
    }
    if ( uri.isAbsolute() )
      return uri.toString();
    if ( url.startsWith( "/" ) )
      return url;
    return this.getWebappContextPath() + "/" + url;
  }

  public void addHtmlConfigInfoToModel( Map<String, Object> model )
  {
    model.put( "catalogCssUrl", this.getCatalogCssUrl());
    model.put( "standardCssUrl", this.getPageCssUrl());
    model.put( "datasetIconAlt", this.getDatasetIconAlt());
    model.put( "datasetIconUrl", this.getDatasetIconUrl());
    model.put( "folderIconAlt", this.getFolderIconAlt());
    model.put( "folderIconUrl", this.getFolderIconUrl());

    model.put( "hostInstName", this.getHostInstName() );
    model.put( "hostInstUrl", this.prepareUrlStringForHtml( this.getHostInstUrl() ) );
    model.put( "hostInstLogoUrl", this.prepareUrlStringForHtml( this.getHostInstLogoUrl() ) );
    model.put( "hostInstLogoAlt", this.getHostInstLogoAlt() );

    model.put( "installationName", this.getInstallName() );
    model.put( "installationUrl", this.prepareUrlStringForHtml( this.getInstallUrl() ) );
    model.put( "installationLogoUrl", this.prepareUrlStringForHtml( this.getInstallLogoUrl() ) );
    model.put( "installationLogoAlt", this.getInstallLogoAlt() );

    model.put( "webappName", this.getWebappName() );
    model.put( "webappVersion", this.getWebappVersion() );
    model.put( "webappVersionBuildDate", this.getWebappVersionBuildDate() );
    model.put( "webappUrl", this.prepareUrlStringForHtml( this.getWebappUrl() ) );
    model.put( "webappDocsUrl", this.prepareUrlStringForHtml( this.getWebappDocsUrl() ) );
    model.put( "webappLogoUrl", this.prepareUrlStringForHtml( this.getWebappLogoUrl() ) );
    model.put( "webappLogoAlt", this.getWebappLogoAlt() );
  }
}

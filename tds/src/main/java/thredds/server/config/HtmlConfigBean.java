/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.server.config;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

@Component
public class HtmlConfigBean {

  private String webappName;
  private String webappVersion;
  private String webappVersionBuildDate;
  private String webappContextPath;

  private String webappUrl;
  private String webappDocsUrl;
  private String webappLogoUrl;
  private String webappLogoAlt;

  private String pageCssUrl;
  private String catalogCssUrl;
  private String datasetCssUrl;
  private String openDapCssUrl;
  private String googleTrackingCode;

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

  private boolean useRemoteCatalogService;

  private boolean generateDatasetJsonLD;

  public HtmlConfigBean() {
  }

  public void init(String webappName,
                   String webappVersion,
                   String webappVersionBuildDate,
                   String webappContextPath) {
    this.webappName = webappName;
    this.webappVersion = webappVersion;
    this.webappVersionBuildDate = webappVersionBuildDate;
    this.webappContextPath = webappContextPath;
  }

  public String getWebappName() {
    return this.webappName;
  }

  public String getWebappVersion() {
    return webappVersion;
  }

  public String getWebappVersionBuildDate() {
    return webappVersionBuildDate;
  }

  public String getWebappContextPath() {
    return webappContextPath;
  }

  /**
   * Return the URL to the main web page for the webapp.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the main web page for the webapp.
   */
  public String getWebappUrl() {
    return webappUrl;
  }

  public void setWebappUrl(String webappUrl) {
    this.webappUrl = webappUrl;
  }

  /**
   * Return the URL to the webapp documentation page.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the webapp documentation page.
   */
  public String getWebappDocsUrl() {
    return webappDocsUrl;
  }

  public void setWebappDocsUrl(String webappDocsUrl) {
    this.webappDocsUrl = webappDocsUrl;
  }

  /**
   * Return the Url to the logo file for the webapp.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the path to the logo file for the webapp.
   */
  public String getWebappLogoUrl() {
    return webappLogoUrl;
  }

  public void setWebappLogoUrl(String webappLogoUrl) {
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

  public void setWebappLogoAlt(String webappLogoAlt) {
    this.webappLogoAlt = webappLogoAlt;
  }

  /**
   * Return the URL to the CSS file used for all non-catalog HTML pages.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the Url to the CSS file used for all non-catalog HTML pages.
   */
  public String getPageCssUrl() {
    return pageCssUrl;
  }

  public void setPageCssUrl(String pageCssUrl) {
    this.pageCssUrl = pageCssUrl;
  }

  /**
   * Return the URL to the CSS file used for the OPeNDAP access pages.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the CSS file used for the OPeNDAP access pages.
   */
  public String getOpenDapCssUrl() {
    return openDapCssUrl;
  }

  public void setOpenDapCssUrl(String openDapCssUrl) {
    this.openDapCssUrl = openDapCssUrl;
  }

  /**
   * Return the URL to the CSS file used for catalog HTML pages.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the CSS file used for catalog HTML pages.
   */
  public String getCatalogCssUrl() {
    return catalogCssUrl;
  }

  public void setCatalogCssUrl(String catalogCssUrl) {
    this.catalogCssUrl = catalogCssUrl;
  }

  /**
   * Return the URL to the CSS file used for catalog HTML pages.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the CSS file used for catalog HTML pages.
   */
  public String getDatasetCssUrl() {
    return datasetCssUrl;
  }

  public void setDatasetCssUrl(String datasetCssUrl) {
    this.datasetCssUrl = datasetCssUrl;
  }

  /**
   * Return the google tracking code for google analytics.
   *
   * @return the google tracking code for google analytics.
   */
  public String getGoogleTrackingCode() {
    return googleTrackingCode;
  }

  public void setGoogleTrackingCode(String googleTrackingCode) {
    this.googleTrackingCode = googleTrackingCode;
  }


  /**
   * Return the URL to the icon document used for folders in HTML catalog views.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the icon document used for folders in HTML catalog views.
   */
  public String getFolderIconUrl() {
    return folderIconUrl;
  }

  public void setFolderIconUrl(String folderIconUrl) {
    this.folderIconUrl = folderIconUrl;
  }

  public String getFolderIconAlt() {
    return folderIconAlt;
  }

  public void setFolderIconAlt(String folderIconAlt) {
    this.folderIconAlt = folderIconAlt;
  }

  /**
   * Return the URL to the icon document used for datasets in HTML catalog views.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the icon document used for datasets in HTML catalog views.
   */
  public String getDatasetIconUrl() {
    return datasetIconUrl;
  }

  public void setDatasetIconUrl(String datasetIconUrl) {
    this.datasetIconUrl = datasetIconUrl;
  }

  public String getDatasetIconAlt() {
    return datasetIconAlt;
  }

  public void setDatasetIconAlt(String datasetIconAlt) {
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

  public void setInstallName(String installName) {
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

  public void setInstallUrl(String installUrl) {
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

  public void setInstallLogoUrl(String installLogoUrl) {
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

  public void setInstallLogoAlt(String installLogoAlt) {
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

  public void setHostInstName(String hostInstName) {
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

  public void setHostInstUrl(String hostInstUrl) {
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

  public void setHostInstLogoUrl(String hostInstLogoUrl) {
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

  public void setHostInstLogoAlt(String hostInstLogoAlt) {
    this.hostInstLogoAlt = hostInstLogoAlt;
  }

  /**
   * Return the config option that sets the default of whether or not to use remoteCatalogService
   * for html representation of catalogRef's in client catalogs.
   *
   * @return true: use remoteCatalogService, false: assume catalogRef's point to a TDS, so simply link
   * the html url of the remote catalog.
   */
  public Boolean getUseRemoteCatalogService() {
    return useRemoteCatalogService;
  }

  public void setUseRemoteCatalogService(Boolean remoteCatalogService) {
    this.useRemoteCatalogService = remoteCatalogService;
  }

  /**
   * Return the config option that determines whether or not to DataSet JSON-LD elements are
   * generated in the head of the html for direct datasets.
   *
   * @return true: generate json-ld, false: do not generate json-ld.
   */
  public Boolean getGenerateDatasetJsonLD() { return generateDatasetJsonLD; }

  public void setGenerateDatasetJsonLD(Boolean generateDatasetJsonLD) {
    this.generateDatasetJsonLD = generateDatasetJsonLD;
  }

  /**
   * Return a URL ready to use in a generated HTML page from a URL that
   * is either absolute or relative to the webapp context path. That is,
   * if relative, it is relative to "http://server:port/thredds/".
   * <p/>
   * <p>For simplicity, all relative URLs are converted to URLs that are
   * absolute paths. For instance, "catalog.xml" becomes "/thredds/catalog.xml".
   *
   * @param url the URL to prepare for use in HTML.
   * @return a URL ready to use in a generated HTML page.
   */
  public String prepareUrlStringForHtml(String url) {
    if (url == null)
      return null;
    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Given a bad URL [" + url + "].", e);
    }
    if (uri.isAbsolute())
      return uri.toString();
    if (url.startsWith("/"))
      return url;
    return this.getWebappContextPath() + "/" + url;
  }

  public void addHtmlConfigInfoToModel(Map<String, Object> model) {
    model.put("catalogCssUrl", this.getCatalogCssUrl());
    model.put("standardCssUrl", this.getPageCssUrl());
    model.put("openDapCssUrl", this.getOpenDapCssUrl());
    model.put("googleTrackingCode", this.getGoogleTrackingCode());
    model.put("datasetIconAlt", this.getDatasetIconAlt());
    model.put("datasetIconUrl", this.getDatasetIconUrl());
    model.put("folderIconAlt", this.getFolderIconAlt());
    model.put("folderIconUrl", this.getFolderIconUrl());
    model.put("generateDatasetJsonLD", this.getGenerateDatasetJsonLD());

    model.put("hostInstName", this.getHostInstName());
    model.put("hostInstUrl", this.prepareUrlStringForHtml(this.getHostInstUrl()));
    model.put("hostInstLogoUrl", this.prepareUrlStringForHtml(this.getHostInstLogoUrl()));
    model.put("hostInstLogoAlt", this.getHostInstLogoAlt());

    model.put("installationName", this.getInstallName());
    model.put("installationUrl", this.prepareUrlStringForHtml(this.getInstallUrl()));
    model.put("installationLogoUrl", this.prepareUrlStringForHtml(this.getInstallLogoUrl()));
    model.put("installationLogoAlt", this.getInstallLogoAlt());

    model.put("useRemoteCatalogService", this.getUseRemoteCatalogService());

    model.put("webappName", this.getWebappName());
    model.put("webappVersion", this.getWebappVersion());
    model.put("webappVersionBuildDate", this.getWebappVersionBuildDate());
    model.put("webappUrl", this.prepareUrlStringForHtml(this.getWebappUrl()));
    model.put("webappDocsUrl", this.prepareUrlStringForHtml(this.getWebappDocsUrl()));
    model.put("webappLogoUrl", this.prepareUrlStringForHtml(this.getWebappLogoUrl()));
    model.put("webappLogoAlt", this.getWebappLogoAlt());
  }
}

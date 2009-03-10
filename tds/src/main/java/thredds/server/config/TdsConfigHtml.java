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
public class TdsConfigHtml
{
//  private static org.slf4j.Logger log =
//          org.slf4j.LoggerFactory.getLogger( TdsConfigHtml.class );

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

    initFromConfig();
  }

  private void initFromConfig()
  {
    this.pageCssUrl    = ThreddsConfig.get( "htmlSetup.standardCssUrl", "" );
    this.catalogCssUrl = ThreddsConfig.get( "htmlSetup.catalogCssUrl", "");

    this.folderIconUrl  = ThreddsConfig.get( "htmlSetup.folderIconUrl", "");
    this.folderIconAlt  = ThreddsConfig.get( "htmlSetup.folderIconAlt", "");
    this.datasetIconUrl = ThreddsConfig.get( "htmlSetup.datasetIconUrl", "");
    this.datasetIconAlt = ThreddsConfig.get( "htmlSetup.datasetIconAlt", "");

    this.installName    = ThreddsConfig.get( "htmlSetup.installName", "");
    this.installLogoUrl = ThreddsConfig.get( "htmlSetup.installLogoUrl", "");
    this.installLogoAlt = ThreddsConfig.get( "htmlSetup.installLogoAlt", "");

    this.hostInstName    = ThreddsConfig.get( "htmlSetup.hostInstName", "" );
    this.hostInstUrl     = ThreddsConfig.get( "htmlSetup.hostInstUrl", "" );
    this.hostInstLogoUrl = ThreddsConfig.get( "htmlSetup.hostInstLogoUrl", "" );
    this.hostInstLogoAlt = ThreddsConfig.get( "htmlSetup.hostInstLogoAlt", "" );
  }

  /**
   * Return the name of this webapp. The name is that given by display-name in web.xml.
   *
   * @return the name of this webapp.
   */
  public String getWebappName()
  {
    return this.webappName;
  }

  public void setWebappName( String webappName)
  {
    this.webappName = webappName;
  }

  public String getWebappVersion()
  {
    return webappVersion;
  }

  public void setWebappVersion( String webappVersion )
  {
    this.webappVersion = webappVersion;
  }

  public String getWebappVersionBrief()
  {
    return webappVersionBrief;
  }

  public void setWebappVersionBrief( String webappVersionBrief )
  {
    this.webappVersionBrief = webappVersionBrief;
  }

  public String getWebappVersionBuildDate()
  {
    return webappVersionBuildDate;
  }

  public void setWebappVersionBuildDate( String webappVersionBuildDate )
  {
    this.webappVersionBuildDate = webappVersionBuildDate;
  }

  public String getWebappContextPath()
  {
    return webappContextPath;
  }

  public void setWebappContextPath( String webappContextPath )
  {
    this.webappContextPath = webappContextPath;
  }

  /**
   * Return the URL to the main web page for the webapp.
   *
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the main web page for the webapp.
   */
  public String getWebappUrl()
  {
    return webappUrl;
  }

  public void setWebappUrl( String webappUrl)
  {
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
  public String getWebappDocsUrl()
  {
    return webappDocsUrl;
  }

  public void setWebappDocsUrl( String webappDocsUrl )
  {
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
  public String getWebappLogoUrl()
  {
    return webappLogoUrl;
  }

  public void setWebappLogoUrl( String webappLogoUrl )
  {
    this.webappLogoUrl = webappLogoUrl;
  }

  /**
   * Return the alternate text for the webapp logo.
   *
   * @return the alternate text for the webapp logo.
   */
  public String getWebappLogoAlt()
  {
    return webappLogoAlt;
  }

  public void setWebappLogoAlt( String webappLogoAlt )
  {
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
  public String getPageCssUrl()
  {
    return pageCssUrl;
  }

  /**
   * Return the URL to the CSS file used for catalog HTML pages.
   *
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the CSS file used for catalog HTML pages.
   */
  public String getCatalogCssUrl()
  {
    return catalogCssUrl;
  }

  /**
   * Return the URL to the icon document used for folders in HTML catalog views.
   *
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the icon document used for folders in HTML catalog views.
   */
  public String getFolderIconUrl()
  {
    return folderIconUrl;
  }

  public String getFolderIconAlt()
  {
    return folderIconAlt;
  }

  /**
   * Return the URL to the icon document used for datasets in HTML catalog views.
   *
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the icon document used for datasets in HTML catalog views.
   */
  public String getDatasetIconUrl()
  {
    return datasetIconUrl;
  }

  public String getDatasetIconAlt()
  {
    return datasetIconAlt;
  }

  /**
   * Return the name of this TDS installation.
   *
   * @return the name of this TDS installation.
   */
  public String getInstallName()
  {
    return installName;
  }

  /**
   * Return the URL to the top level of this TDS installation.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to the top level of this installation.
   */
  public String getInstallUrl()
  {
    return installUrl;
  }

  public void setInstallUrl( String installUrl )
  {
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
  public String getInstallLogoUrl()
  {
    return installLogoUrl;
  }

  /**
   * Return the alternate text for the logo for this TDS installation.
   *
   * @return the alternate text for the logo for this installation.
   */
  public String getInstallLogoAlt()
  {
    return installLogoAlt;
  }

  /**
   * Return the name of the institution hosting this TDS installation.
   *
   * @return the name of the institution hosting this TDS installation.
   */
  public String getHostInstName()
  {
    return hostInstName;
  }

  /**
   * Return the URL to a web page for the institution hosting this installation.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the URL to a web page for the institution hosting this installation.
   */
  public String getHostInstUrl()
  {
    return hostInstUrl;
  }

  /**
   * Return the path to the logo file for the institution hosting this installation.
   * <p/>
   * <p>Note: A relative URL is considered relative to the webapp context path.
   * That is, it is relative to "http://server:port/thredds/".
   *
   * @return the path to the logo file for the institution hosting this installation.
   */
  public String getHostInstLogoUrl()
  {
    return hostInstLogoUrl;
  }

  /**
   * Return the alternate text for the logo for the institution hosting this installation.
   *
   * @return the alternate text for the logo for the institution hosting this installation.
   */
  public String getHostInstLogoAlt()
  {
    return hostInstLogoAlt;
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

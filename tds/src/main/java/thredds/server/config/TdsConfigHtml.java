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

/**
 * _more_
 *
 * @author edavis
 * @since 4.0
 */
public class TdsConfigHtml
{
  private static org.slf4j.Logger log =
          org.slf4j.LoggerFactory.getLogger( TdsConfigHtml.class );

  private String webappContextPath;
  private String webappName;
  private String webappVersion;
  private String pageCssPath;
  private String catalogCssPath;
  private String webappUrl;
  private String webappLogoPath;
  private String webappLogoAlt;
  private String siteUrl;
  private String siteLogoPath;
  private String siteLogoAlt;
  private String webappDocsPath;          
  private String folderIconPath;
  private String folderIconAlt;
  private String datasetIconPath;
  private String datasetIconAlt;

  public void init( TdsContext tdsContext )
  {
    this.webappContextPath = tdsContext.getContextPath();
    this.webappName = tdsContext.getWebappName();
    this.webappVersion = tdsContext.getWebappVersionFull();
  }

  /**
   * Return the webapp context path.
   *
   * @return the webapp context path.
   */
  public String getWebappContextPath()
  {
    return webappContextPath;
  }

  /**
   * Return the name of the webapp.
   * @return the name of the webapp.
   */
  public String getWebappName()
  {
    return webappName;
  }

  /**
   * Return the version of the webapp.
   * @return the version of the webapp.
   */
  public String getWebappVersion()
  {
    return webappVersion;
  }

  /**
   * Return the path to the CSS file used for most HTML pages.
   *
   * If the path does not starts with "/", it is relative
   * to the context path.
   *
   * @return the path to the CSS file used for most HTML pages.
   */
  public String getPageCssPath()
  {
    return pageCssPath;
  }

  public void setPageCssPath( String pageCssPath )
  {
    this.pageCssPath = pageCssPath;
  }

  /**
   * Return the path to the CSS file used for catalog HTML pages.
   *
   * If the path does not starts with "/", it is relative
   * to the context path.
   *
   * @return the path to the CSS file used for catalog HTML pages.
   */
  public String getCatalogCssPath()
  {
    return catalogCssPath;
  }

  public void setCatalogCssPath( String catalogCssPath )
  {
    this.catalogCssPath = catalogCssPath;
  }

  /**
   * Return a URL to the main web page for the webapp.
   *
   * @return a URL to the main web page for the webapp.
   */
  public String getWebappUrl()
  {
    return webappUrl;
  }

  public void setWebappUrl( String webappUrl )
  {
    this.webappUrl = webappUrl;
  }

  /**
   * Return the path to the logo file for the webapp.
   *
   * If the path does not starts with "/", it is relative
   * to the context path.
   *
   * @return the path to the logo file for the webapp.
   */
  public String getWebappLogoPath()
  {
    return webappLogoPath;
  }

  public void setWebappLogoPath( String webappLogoPath )
  {
    this.webappLogoPath = webappLogoPath;
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
   * Return the URL to the top level of this site.
   *
   * @return the URL to the top level of this site.
   */
  public String getSiteUrl()
  {
    return siteUrl;
  }

  public void setSiteUrl( String siteUrl )
  {
    this.siteUrl = siteUrl;
  }

  /**
   * Return the path to the logo file for the site running the webapp.
   *
   * If the path does not starts with "/", it is relative
   * to the context path.
   *
   * @return the path to the logo file for the site running the webapp.
   */
  public String getSiteLogoPath()
  {
    return siteLogoPath;
  }

  public void setSiteLogoPath( String siteLogoPath )
  {
    this.siteLogoPath = siteLogoPath;
  }

  /**
   * Return the alternate text for the site logo.
   *
   * @return the alternate text for the site logo.
   */
  public String getSiteLogoAlt()
  {
    return siteLogoAlt;
  }

  public void setSiteLogoAlt( String siteLogoAlt )
  {
    this.siteLogoAlt = siteLogoAlt;
  }

  /**
   * Return the path to the webapp documents.
   *
   * If the path is not absolute, it is relative to the context path.
   *
   * @return the path to the webapp documents.
   */
  public String getWebappDocsPath()
  {
    return webappDocsPath;
  }

  public void setWebappDocsPath( String webappDocsPath )
  {
    this.webappDocsPath = webappDocsPath;
  }

  public String getFolderIconPath()
  {
    return folderIconPath;
  }

  public void setFolderIconPath( String folderIconPath )
  {
    this.folderIconPath = folderIconPath;
  }

  public String getFolderIconAlt()
  {
    return folderIconAlt;
  }

  public void setFolderIconAlt( String folderIconAlt )
  {
    this.folderIconAlt = folderIconAlt;
  }

  public String getDatasetIconPath()
  {
    return datasetIconPath;
  }

  public void setDatasetIconPath( String datasetIconPath )
  {
    this.datasetIconPath = datasetIconPath;
  }

  public String getDatasetIconAlt()
  {
    return datasetIconAlt;
  }

  public void setDatasetIconAlt( String datasetIconAlt )
  {
    this.datasetIconAlt = datasetIconAlt;
  }
}

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
  private String webappLogoPath;
  private String webappLogoAlt;
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

  public void setWebappContextPath( String webappContextPath )
  {
    this.webappContextPath = webappContextPath;
  }

  /**
   * Return the name of the webapp.
   * @return the name of the webapp.
   */
  public String getWebappName()
  {
    return webappName;
  }

  public void setWebappName( String webappName )
  {
    this.webappName = webappName;
  }

  /**
   * Return the version of the webapp.
   * @return the version of the webapp.
   */
  public String getWebappVersion()
  {
    return webappVersion;
  }

  public void setWebappVersion( String webappVersion )
  {
    this.webappVersion = webappVersion;
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

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

package thredds.server.config;

/**
 * Centralize the mapping of threddsConfig.xml configuration settings to the data objects used by
 * the various servlets. Supports earlier versions (some deprecated) of threddsConfig.xml config
 * settings.
 *
 * LOOK not used ??
 *
 * @author edavis
 * @since 4.1
 */
class TdsConfigMapper
{
  // ToDo Consider moving to Spring-like configuration.
  // ToDo Not yet using ThreddsConfig.get<type>() methods.

  TdsConfigMapper() {}

  private TdsServerInfo tdsServerInfo;
  private HtmlConfig htmlConfig;
  private WmsConfig wmsConfig;
  private CorsConfig corsConfig;
  private TdsUpdateConfig tdsUpdateConfig;
  
  void setTdsServerInfo( TdsServerInfo tdsServerInfo ) {
    this.tdsServerInfo = tdsServerInfo;
  }

  void setHtmlConfig( HtmlConfig htmlConfig ) {
    this.htmlConfig = htmlConfig;
  }

  void setWmsConfig( WmsConfig wmsConfig ) {
    this.wmsConfig = wmsConfig;
  }

  void setCorsConfig( CorsConfig corsConfig ) {
      this.corsConfig = corsConfig;
  }
  
  void setTdsUpdateConfig(TdsUpdateConfig tdsUpdateConfig) {
      this.tdsUpdateConfig = tdsUpdateConfig;
  }

  enum ServerInfoMappings
  {
    SERVER_NAME( "serverInformation.name", "htmlSetup.installName", "Initial TDS Installation" ),
    SERVER_LOGO_URL( "serverInformation.logoUrl", "htmlSetup.installLogoUrl", "threddsIcon.gif" ),
    SERVER_LOGO_ALT_TEXT( "serverInformation.logoAltText", "htmlSetup.installLogoAlt", "Initial TDS Installation" ),
    SERVER_ABSTRACT( "serverInformation.abstract", null, "" ),
    SERVER_KEYWORDS( "serverInformation.keywords", null, "" ),
    SERVER_CONTACT_NAME( "serverInformation.contact.name", null, "" ),
    SERVER_CONTACT_ORGANIZATION( "serverInformation.contact.organization", null, "" ),
    SERVER_CONTACT_EMAIL( "serverInformation.contact.email", null, "" ),
    SERVER_CONTACT_PHONE( "serverInformation.contact.phone", null, "" ),
    SERVER_HOST_INSTITUTION_NAME( "serverInformation.hostInstitution.name", "htmlSetup.hostInstName", "" ),
    SERVER_HOST_INSTITUTION_WEBSITE( "serverInformation.hostInstitution.webSite", "htmlSetup.hostInstUrl", "" ),
    SERVER_HOST_INSTITUTION_LOGO_URL( "serverInformation.hostInstitution.logoUrl", "htmlSetup.hostInstLogoUrl", "" ),
    SERVER_HOST_INSTITUTION_LOGO_ALT_TEXT( "serverInformation.hostInstitution.logoAltText", "htmlSetup.hostInstLogoAlt", "" );

    private String key;
    private String alternateKey;
    private String defaultValue;

    ServerInfoMappings( String key, String alternateKey, String defaultValue )
    {
      if ( key == null || defaultValue == null )
        throw new IllegalArgumentException( "The key and default value may not be null." );

      this.key = key;
      this.alternateKey = alternateKey;
      this.defaultValue = defaultValue;
    }

    String getValueFromThreddsConfig()
    {
      return TdsConfigMapper.getValueFromThreddsConfig( this.key, this.alternateKey, this.defaultValue);
    }
  }

  enum HtmlConfigMappings
  {
    HTML_STANDARD_CSS_URL( "htmlSetup.standardCssUrl", null,""),
    HTML_CATALOG_CSS_URL( "htmlSetup.catalogCssUrl", null,""),
    GOOGLE_TRACKING_CODE( "htmlSetup.googleTrackingCode", null,""),

    HTML_FOLDER_ICON_URL( "htmlSetup.folderIconUrl", null,"folder.gif"),
    HTML_FOLDER_ICON_ALT( "htmlSetup.folderIconAlt", null,"Folder"),
    HTML_DATASET_ICON_URL( "htmlSetup.datasetIconUrl", null,""),
    HTML_DATASET_ICON_ALT( "htmlSetup.datasetIconAlt", null,""),
    HTML_USE_REMOTE_CAT_SERVICE( "htmlSetup.useRemoteCatalogService", null, "true");

    private String key;
    private String alternateKey;
    private String defaultValue;

    HtmlConfigMappings( String key, String alternateKey, String defaultValue )
    {
      if ( key == null || defaultValue == null )
        throw new IllegalArgumentException( "The key and default value may not be null." );

      this.key = key;
      this.alternateKey = alternateKey;
      this.defaultValue = defaultValue;
    }

    String getValueFromThreddsConfig() {
      return TdsConfigMapper.getValueFromThreddsConfig( this.key, this.alternateKey, this.defaultValue);
    }
  }

  enum WmsConfigMappings
  {
    WMS_ALLOW( "WMS.allow", null, "false"),
    WMS_ALLOW_REMOTE( "WMS.allowRemote", null, "false"),
    WMS_PALETTE_LOCATION_DIR( "WMS.paletteLocationDir", null, null),
    WMS_MAXIMUM_IMAGE_WIDTH( "WMS.maxImageWidth", null, "2048"),
    WMS_MAXIMUM_IMAGE_HEIGHT( "WMS.maxImageHeight", null, "2048");

    private String key;
    private String alternateKey;
    private String defaultValue;

    WmsConfigMappings( String key, String alternateKey, String defaultValue )
    {
      if ( key == null )
        throw new IllegalArgumentException( "The key may not be null." );

      this.key = key;
      this.alternateKey = alternateKey;
      this.defaultValue = defaultValue;
    }

    String getDefaultValue() {
      return this.defaultValue;
    }

    String getValueFromThreddsConfig() {
      return TdsConfigMapper.getValueFromThreddsConfig( this.key, this.alternateKey, this.defaultValue);
    }
  }

  enum CorsConfigMappings
  {
    CORS_ENABLED("CORS.enabled", null, "false"),
    CORS_MAXIMUM_AGE("CORS.maxAge", null, "1728000"),
    CORS_ALLOWED_METHODS("CORS.allowedMethods", null, "GET"),
    CORS_ALLOWED_HEADERS("CORS.allowedHeaders", null, "Authorization"),
    CORS_ALLOWED_ORIGIN("CORS.allowedOrigin", null, "*");

    private String key;
    private String alternateKey;
    private String defaultValue;

    CorsConfigMappings( String key, String alternateKey,
                       String defaultValue )
    {
        if ( key == null )
            throw new IllegalArgumentException( "The key may not be null." );

        this.key = key;
        this.alternateKey = alternateKey;
        this.defaultValue = defaultValue;
    }

    String getDefaultValue() {
        return this.defaultValue;
    }

    String getValueFromThreddsConfig() {
        return TdsConfigMapper.getValueFromThreddsConfig( this.key, this.alternateKey, this.defaultValue);
    }
  }

  enum TdsUpdateConfigMappings {
    TDSUPDAATE_LOGVERSIONINFO("TdsUpdateConfig.logVersionInfo", null, "true");

    private String key;
    private String alternateKey;
    private String defaultValue;
  
    TdsUpdateConfigMappings( String key, String alternateKey, String defaultValue ) {
      if ( key == null ) {
       throw new IllegalArgumentException( "The key may not be null." );
      }
        
      this.key = key;
      this.alternateKey = alternateKey;
      this.defaultValue = defaultValue;

    } 
        
    String getDefaultValue() {
      return this.defaultValue;
    }
        
    String getValueFromThreddsConfig() {
      return TdsConfigMapper.getValueFromThreddsConfig( this.key, this.alternateKey, this.defaultValue);
    }
  }

    
  private static String getValueFromThreddsConfig( String key, String alternateKey, String defaultValue)
  {
    String value = ThreddsConfig.get( key, null );
    if ( value == null && alternateKey != null )
      value = ThreddsConfig.get( alternateKey, null );
    if ( value == null )
      value = defaultValue;
    return value;

  }

  void init( TdsContext tdsContext ) {
    setupServerInfo();
    setupHtmlConfig( tdsContext );
    setupWmsConfig();
    setupCorsConfig();
    setupTdsUpdateConfig();
  }

  private void setupServerInfo()
  {
    this.tdsServerInfo.setName( ServerInfoMappings.SERVER_NAME.getValueFromThreddsConfig() );
    this.tdsServerInfo.setLogoUrl( ServerInfoMappings.SERVER_LOGO_URL.getValueFromThreddsConfig());
    this.tdsServerInfo.setLogoAltText( ServerInfoMappings.SERVER_LOGO_ALT_TEXT.getValueFromThreddsConfig());
    this.tdsServerInfo.setSummary( ServerInfoMappings.SERVER_ABSTRACT.getValueFromThreddsConfig());
    this.tdsServerInfo.setKeywords( ServerInfoMappings.SERVER_KEYWORDS.getValueFromThreddsConfig());

    this.tdsServerInfo.setContactName( ServerInfoMappings.SERVER_CONTACT_NAME.getValueFromThreddsConfig());
    this.tdsServerInfo.setContactOrganization( ServerInfoMappings.SERVER_CONTACT_ORGANIZATION.getValueFromThreddsConfig());
    this.tdsServerInfo.setContactEmail( ServerInfoMappings.SERVER_CONTACT_EMAIL.getValueFromThreddsConfig());
    this.tdsServerInfo.setContactPhone( ServerInfoMappings.SERVER_CONTACT_PHONE.getValueFromThreddsConfig());

    this.tdsServerInfo.setHostInstitutionName( ServerInfoMappings.SERVER_HOST_INSTITUTION_NAME.getValueFromThreddsConfig() );
    this.tdsServerInfo.setHostInstitutionWebSite( ServerInfoMappings.SERVER_HOST_INSTITUTION_WEBSITE.getValueFromThreddsConfig() );
    this.tdsServerInfo.setHostInstitutionLogoUrl( ServerInfoMappings.SERVER_HOST_INSTITUTION_LOGO_URL.getValueFromThreddsConfig() );
    this.tdsServerInfo.setHostInstitutionLogoAltText( ServerInfoMappings.SERVER_HOST_INSTITUTION_LOGO_ALT_TEXT.getValueFromThreddsConfig() );
  }

  private void setupHtmlConfig( TdsContext tdsContext )
  {
    this.htmlConfig.init( tdsContext.getWebappName(), tdsContext.getWebappVersion(),
                          tdsContext.getWebappVersionBuildDate(), tdsContext.getContextPath() );

    this.htmlConfig.setInstallName( this.tdsServerInfo.getName() );
    this.htmlConfig.setInstallLogoUrl( this.tdsServerInfo.getLogoUrl() );
    this.htmlConfig.setInstallLogoAlt( this.tdsServerInfo.getLogoAltText() );

    this.htmlConfig.setHostInstName( this.tdsServerInfo.getHostInstitutionName() );
    this.htmlConfig.setHostInstUrl( this.tdsServerInfo.getHostInstitutionWebSite() );
    this.htmlConfig.setHostInstLogoUrl( this.tdsServerInfo.getHostInstitutionLogoUrl() );
    this.htmlConfig.setHostInstLogoAlt( this.tdsServerInfo.getHostInstitutionLogoAltText() );

    this.htmlConfig.setPageCssUrl( HtmlConfigMappings.HTML_STANDARD_CSS_URL.getValueFromThreddsConfig() );
    this.htmlConfig.setCatalogCssUrl( HtmlConfigMappings.HTML_CATALOG_CSS_URL.getValueFromThreddsConfig() );
    this.htmlConfig.setGoogleTrackingCode( HtmlConfigMappings.GOOGLE_TRACKING_CODE.getValueFromThreddsConfig() );
    
    this.htmlConfig.setFolderIconUrl( HtmlConfigMappings.HTML_FOLDER_ICON_URL.getValueFromThreddsConfig() );
    this.htmlConfig.setFolderIconAlt( HtmlConfigMappings.HTML_FOLDER_ICON_ALT.getValueFromThreddsConfig() );
    this.htmlConfig.setDatasetIconUrl( HtmlConfigMappings.HTML_DATASET_ICON_URL.getValueFromThreddsConfig() );
    this.htmlConfig.setDatasetIconAlt( HtmlConfigMappings.HTML_DATASET_ICON_ALT.getValueFromThreddsConfig() );

    this.htmlConfig.setUseRemoteCatalogService( Boolean.parseBoolean(HtmlConfigMappings.HTML_USE_REMOTE_CAT_SERVICE.getValueFromThreddsConfig()));
  }

  private void setupWmsConfig()
  {
    this.wmsConfig.setAllow( Boolean.parseBoolean( WmsConfigMappings.WMS_ALLOW.getValueFromThreddsConfig()));
    this.wmsConfig.setAllowRemote( Boolean.parseBoolean( WmsConfigMappings.WMS_ALLOW_REMOTE.getValueFromThreddsConfig()));
    this.wmsConfig.setPaletteLocationDir( WmsConfigMappings.WMS_PALETTE_LOCATION_DIR.getValueFromThreddsConfig() );

    try {
      this.wmsConfig.setMaxImageWidth( Integer.parseInt( WmsConfigMappings.WMS_MAXIMUM_IMAGE_WIDTH.getValueFromThreddsConfig() ) );
    }
    catch ( NumberFormatException e ) {
      // If the given maxImageWidth value is not a number, try the default value.
      this.wmsConfig.setMaxImageWidth( Integer.parseInt( WmsConfigMappings.WMS_MAXIMUM_IMAGE_WIDTH.getDefaultValue() ) );
    }
    try {
      this.wmsConfig.setMaxImageHeight( Integer.parseInt( WmsConfigMappings.WMS_MAXIMUM_IMAGE_HEIGHT.getValueFromThreddsConfig() ) );
    }
    catch ( NumberFormatException e ) {
      // If the given maxImageHeight value is not a number, try the default value.
      this.wmsConfig.setMaxImageHeight( Integer.parseInt( WmsConfigMappings.WMS_MAXIMUM_IMAGE_HEIGHT.getDefaultValue() ) );
    }
  }
  
  private void setupCorsConfig()
    {
    this.corsConfig.setEnabled( Boolean.parseBoolean( CorsConfigMappings.CORS_ENABLED.getValueFromThreddsConfig() ) );
    try {
        this.corsConfig.setMaxAge( Integer.parseInt(CorsConfigMappings.CORS_MAXIMUM_AGE.getValueFromThreddsConfig() ) );
    } catch ( NumberFormatException e ) {
        this.corsConfig.setMaxAge( Integer.parseInt(CorsConfigMappings.CORS_MAXIMUM_AGE.getDefaultValue() ) );
    }
    this.corsConfig.setAllowedHeaders( CorsConfigMappings.CORS_ALLOWED_HEADERS.getValueFromThreddsConfig() );
    this.corsConfig.setAllowedMethods( CorsConfigMappings.CORS_ALLOWED_METHODS.getValueFromThreddsConfig() );
    this.corsConfig.setAllowedOrigin( CorsConfigMappings.CORS_ALLOWED_ORIGIN.getValueFromThreddsConfig() );
  }
  
  private void setupTdsUpdateConfig() {
      this.tdsUpdateConfig.setLogVersionInfo(Boolean.parseBoolean(TdsUpdateConfigMappings.TDSUPDAATE_LOGVERSIONINFO.getValueFromThreddsConfig()));
  }
}

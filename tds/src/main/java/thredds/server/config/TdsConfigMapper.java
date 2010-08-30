package thredds.server.config;

import thredds.servlet.ThreddsConfig;

/**
 * Centralize the mapping of threddsConfig.xml configuration settings to the data objects used by
 * the various servlets. Supports earlier versions (some deprecated) of threddsConfig.xml config
 * settings. 
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

  void setTdsServerInfo( TdsServerInfo tdsServerInfo ) {
    this.tdsServerInfo = tdsServerInfo;
  }

  void setHtmlConfig( HtmlConfig htmlConfig ) {
    this.htmlConfig = htmlConfig;
  }

  void setWmsConfig( WmsConfig wmsConfig ) {
    this.wmsConfig = wmsConfig;
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

    HTML_FOLDER_ICON_URL( "htmlSetup.folderIconUrl", null,""),
    HTML_FOLDER_ICON_ALT( "htmlSetup.folderIconAlt", null,""),
    HTML_DATASET_ICON_URL( "htmlSetup.datasetIconUrl", null,""),
    HTML_DATASET_ICON_ALT( "htmlSetup.datasetIconAlt", null,"");

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
    this.htmlConfig.init( tdsContext.getWebappName(), tdsContext.getWebappVersion(), tdsContext.getWebappVersionBrief(),
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

    this.htmlConfig.setFolderIconUrl( HtmlConfigMappings.HTML_FOLDER_ICON_URL.getValueFromThreddsConfig() );
    this.htmlConfig.setFolderIconAlt( HtmlConfigMappings.HTML_FOLDER_ICON_ALT.getValueFromThreddsConfig() );
    this.htmlConfig.setDatasetIconUrl( HtmlConfigMappings.HTML_DATASET_ICON_URL.getValueFromThreddsConfig() );
    this.htmlConfig.setDatasetIconAlt( HtmlConfigMappings.HTML_DATASET_ICON_ALT.getValueFromThreddsConfig() );
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
}

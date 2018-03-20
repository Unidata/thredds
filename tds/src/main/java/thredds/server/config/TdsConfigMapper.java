/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Centralize the mapping of threddsConfig.xml configuration settings to the data objects used by
 * the various servlets. Supports earlier versions (some deprecated) of threddsConfig.xml config settings.
 * <p/>
 * called from TdsInit
 *
 * @author edavis
 * @since 4.1
 */
@Component
class TdsConfigMapper {
  // ToDo Not yet using ThreddsConfig.get<type>() methods.

  @Autowired
  private TdsServerInfoBean tdsServerInfo;
  @Autowired
  private HtmlConfigBean htmlConfig;
  @Autowired
  private WmsConfigBean wmsConfig;
  @Autowired
  private CorsConfigBean corsConfig;
  @Autowired
  private TdsUpdateConfigBean tdsUpdateConfig;

  // static so can be called from static enum classes
  private static String getValueFromThreddsConfig(String key, String alternateKey, String defaultValue) {
    String value = ThreddsConfig.get(key, null);
    if (value == null && alternateKey != null)
      value = ThreddsConfig.get(alternateKey, null);
    if (value == null)
      value = defaultValue;
    return value;
  }

  enum ServerInfoMappings {
    SERVER_NAME("serverInformation.name", "htmlSetup.installName", "Initial TDS Installation"),
    SERVER_LOGO_URL("serverInformation.logoUrl", "htmlSetup.installLogoUrl", "threddsIcon.png"),
    SERVER_LOGO_ALT_TEXT("serverInformation.logoAltText", "htmlSetup.installLogoAlt", "Initial TDS Installation"),
    SERVER_ABSTRACT("serverInformation.abstract", null, "Scientific Data"),
    SERVER_KEYWORDS("serverInformation.keywords", null, "meteorology, atmosphere, climate, ocean, earth science"),
    SERVER_CONTACT_NAME("serverInformation.contact.name", null, ""),
    SERVER_CONTACT_ORGANIZATION("serverInformation.contact.organization", null, ""),
    SERVER_CONTACT_EMAIL("serverInformation.contact.email", null, ""),
    SERVER_CONTACT_PHONE("serverInformation.contact.phone", null, ""),
    SERVER_HOST_INSTITUTION_NAME("serverInformation.hostInstitution.name", "htmlSetup.hostInstName", ""),
    SERVER_HOST_INSTITUTION_WEBSITE("serverInformation.hostInstitution.webSite", "htmlSetup.hostInstUrl", ""),
    SERVER_HOST_INSTITUTION_LOGO_URL("serverInformation.hostInstitution.logoUrl", "htmlSetup.hostInstLogoUrl", ""),
    SERVER_HOST_INSTITUTION_LOGO_ALT_TEXT("serverInformation.hostInstitution.logoAltText", "htmlSetup.hostInstLogoAlt", "");

    private String key;
    private String alternateKey;  // deprecated
    private String defaultValue;

    ServerInfoMappings(String key, String alternateKey, String defaultValue) {
      if (key == null || defaultValue == null)
        throw new IllegalArgumentException("The key and default value may not be null.");

      this.key = key;
      this.alternateKey = alternateKey;
      this.defaultValue = defaultValue;
    }

    String getValueFromThreddsConfig() {
      return TdsConfigMapper.getValueFromThreddsConfig(this.key, this.alternateKey, this.defaultValue);
    }

    static void load(TdsServerInfoBean info) {
      info.setName(SERVER_NAME.getValueFromThreddsConfig());
      info.setLogoUrl(SERVER_LOGO_URL.getValueFromThreddsConfig());
      info.setLogoAltText(SERVER_LOGO_ALT_TEXT.getValueFromThreddsConfig());
      info.setSummary(SERVER_ABSTRACT.getValueFromThreddsConfig());
      info.setKeywords(SERVER_KEYWORDS.getValueFromThreddsConfig());

      info.setContactName(SERVER_CONTACT_NAME.getValueFromThreddsConfig());
      info.setContactOrganization(SERVER_CONTACT_ORGANIZATION.getValueFromThreddsConfig());
      info.setContactEmail(SERVER_CONTACT_EMAIL.getValueFromThreddsConfig());
      info.setContactPhone(SERVER_CONTACT_PHONE.getValueFromThreddsConfig());

      info.setHostInstitutionName(SERVER_HOST_INSTITUTION_NAME.getValueFromThreddsConfig());
      info.setHostInstitutionWebSite(SERVER_HOST_INSTITUTION_WEBSITE.getValueFromThreddsConfig());
      info.setHostInstitutionLogoUrl(SERVER_HOST_INSTITUTION_LOGO_URL.getValueFromThreddsConfig());
      info.setHostInstitutionLogoAltText(SERVER_HOST_INSTITUTION_LOGO_ALT_TEXT.getValueFromThreddsConfig());
    }
  }


  enum HtmlConfigMappings {
    HTML_STANDARD_CSS_URL("htmlSetup.standardCssUrl", null, "tds.css"),
    HTML_CATALOG_CSS_URL("htmlSetup.catalogCssUrl", null, "tdsCat.css"),
    GOOGLE_TRACKING_CODE("htmlSetup.googleTrackingCode", null, ""),

    HTML_FOLDER_ICON_URL("htmlSetup.folderIconUrl", null, "folder.gif"),
    HTML_FOLDER_ICON_ALT("htmlSetup.folderIconAlt", null, "Folder"),
    HTML_DATASET_ICON_URL("htmlSetup.datasetIconUrl", null, ""),
    HTML_DATASET_ICON_ALT("htmlSetup.datasetIconAlt", null, ""),
    HTML_USE_REMOTE_CAT_SERVICE("htmlSetup.useRemoteCatalogService", null, "true");

    private String key;
    private String alternateKey;
    private String defaultValue;

    HtmlConfigMappings(String key, String alternateKey, String defaultValue) {
      if (key == null || defaultValue == null)
        throw new IllegalArgumentException("The key and default value may not be null.");

      this.key = key;
      this.alternateKey = alternateKey;
      this.defaultValue = defaultValue;
    }

    String getValueFromThreddsConfig() {
      return TdsConfigMapper.getValueFromThreddsConfig(this.key, this.alternateKey, this.defaultValue);
    }

    static void load(HtmlConfigBean htmlConfig, TdsContext tdsContext, TdsServerInfoBean serverInfo) {
      htmlConfig.init(tdsContext.getWebappDisplayName(), tdsContext.getWebappVersion(), tdsContext.getTdsVersionBuildDate(), tdsContext.getContextPath());

      htmlConfig.setInstallName(serverInfo.getName());
      htmlConfig.setInstallLogoUrl(serverInfo.getLogoUrl());
      htmlConfig.setInstallLogoAlt(serverInfo.getLogoAltText());

      htmlConfig.setHostInstName(serverInfo.getHostInstitutionName());
      htmlConfig.setHostInstUrl(serverInfo.getHostInstitutionWebSite());
      htmlConfig.setHostInstLogoUrl(serverInfo.getHostInstitutionLogoUrl());
      htmlConfig.setHostInstLogoAlt(serverInfo.getHostInstitutionLogoAltText());

      htmlConfig.setPageCssUrl(HTML_STANDARD_CSS_URL.getValueFromThreddsConfig());
      htmlConfig.setCatalogCssUrl(HTML_CATALOG_CSS_URL.getValueFromThreddsConfig());
      htmlConfig.setGoogleTrackingCode(GOOGLE_TRACKING_CODE.getValueFromThreddsConfig());

      htmlConfig.setFolderIconUrl(HTML_FOLDER_ICON_URL.getValueFromThreddsConfig());
      htmlConfig.setFolderIconAlt(HTML_FOLDER_ICON_ALT.getValueFromThreddsConfig());
      htmlConfig.setDatasetIconUrl(HTML_DATASET_ICON_URL.getValueFromThreddsConfig());
      htmlConfig.setDatasetIconAlt(HTML_DATASET_ICON_ALT.getValueFromThreddsConfig());

      htmlConfig.setUseRemoteCatalogService(Boolean.parseBoolean(HTML_USE_REMOTE_CAT_SERVICE.getValueFromThreddsConfig()));
    }
  }

  enum WmsConfigMappings {
    WMS_ALLOW("WMS.allow", null, "false"),
    WMS_ALLOW_REMOTE("WMS.allowRemote", null, "false"),
    WMS_PALETTE_LOCATION_DIR("WMS.paletteLocationDir", null, null),
    WMS_MAXIMUM_IMAGE_WIDTH("WMS.maxImageWidth", null, "2048"),
    WMS_MAXIMUM_IMAGE_HEIGHT("WMS.maxImageHeight", null, "2048");

    private String key;
    private String alternateKey;
    private String defaultValue;

    WmsConfigMappings(String key, String alternateKey, String defaultValue) {
      if (key == null)
        throw new IllegalArgumentException("The key may not be null.");

      this.key = key;
      this.alternateKey = alternateKey;
      this.defaultValue = defaultValue;
    }

    String getDefaultValue() {
      return this.defaultValue;
    }

    String getValueFromThreddsConfig() {
      return TdsConfigMapper.getValueFromThreddsConfig(this.key, this.alternateKey, this.defaultValue);
    }

    static void load(WmsConfigBean wmsConfig) {
      wmsConfig.setAllow(Boolean.parseBoolean(WMS_ALLOW.getValueFromThreddsConfig()));
      wmsConfig.setAllowRemote(Boolean.parseBoolean(WMS_ALLOW_REMOTE.getValueFromThreddsConfig()));
      wmsConfig.setPaletteLocationDir(WMS_PALETTE_LOCATION_DIR.getValueFromThreddsConfig());

      try {
        wmsConfig.setMaxImageWidth(Integer.parseInt(WMS_MAXIMUM_IMAGE_WIDTH.getValueFromThreddsConfig()));
      } catch (NumberFormatException e) {
        // If the given maxImageWidth value is not a number, try the default value.
        wmsConfig.setMaxImageWidth(Integer.parseInt(WMS_MAXIMUM_IMAGE_WIDTH.getDefaultValue()));
      }
      try {
        wmsConfig.setMaxImageHeight(Integer.parseInt(WMS_MAXIMUM_IMAGE_HEIGHT.getValueFromThreddsConfig()));
      } catch (NumberFormatException e) {
        // If the given maxImageHeight value is not a number, try the default value.
        wmsConfig.setMaxImageHeight(Integer.parseInt(WMS_MAXIMUM_IMAGE_HEIGHT.getDefaultValue()));
      }
    }
  }

  enum CorsConfigMappings {
    CORS_ENABLED("CORS.enabled", null, "false"),
    CORS_MAXIMUM_AGE("CORS.maxAge", null, "1728000"),
    CORS_ALLOWED_METHODS("CORS.allowedMethods", null, "GET"),
    CORS_ALLOWED_HEADERS("CORS.allowedHeaders", null, "Authorization"),
    CORS_ALLOWED_ORIGIN("CORS.allowedOrigin", null, "*");

    private String key;
    private String alternateKey;
    private String defaultValue;

    CorsConfigMappings(String key, String alternateKey,
                       String defaultValue) {
      if (key == null)
        throw new IllegalArgumentException("The key may not be null.");

      this.key = key;
      this.alternateKey = alternateKey;
      this.defaultValue = defaultValue;
    }

    String getDefaultValue() {
      return this.defaultValue;
    }

    String getValueFromThreddsConfig() {
      return TdsConfigMapper.getValueFromThreddsConfig(this.key, this.alternateKey, this.defaultValue);
    }

    static void load(CorsConfigBean corsConfig) {
       corsConfig.setEnabled(Boolean.parseBoolean(CORS_ENABLED.getValueFromThreddsConfig()));
       try {
         corsConfig.setMaxAge(Integer.parseInt(CORS_MAXIMUM_AGE.getValueFromThreddsConfig()));
       } catch (NumberFormatException e) {
         corsConfig.setMaxAge(Integer.parseInt(CORS_MAXIMUM_AGE.getDefaultValue()));
       }
       corsConfig.setAllowedHeaders(CORS_ALLOWED_HEADERS.getValueFromThreddsConfig());
       corsConfig.setAllowedMethods(CORS_ALLOWED_METHODS.getValueFromThreddsConfig());
       corsConfig.setAllowedOrigin(CORS_ALLOWED_ORIGIN.getValueFromThreddsConfig());
     }
  }

  enum TdsUpdateConfigMappings {
    TDSUPDAATE_LOGVERSIONINFO("TdsUpdateConfig.logVersionInfo", null, "true");

    private String key;
    private String alternateKey;
    private String defaultValue;

    TdsUpdateConfigMappings(String key, String alternateKey, String defaultValue) {
      if (key == null) {
        throw new IllegalArgumentException("The key may not be null.");
      }

      this.key = key;
      this.alternateKey = alternateKey;
      this.defaultValue = defaultValue;
    }

    String getDefaultValue() {
      return this.defaultValue;
    }

    String getValueFromThreddsConfig() {
      return TdsConfigMapper.getValueFromThreddsConfig(this.key, this.alternateKey, this.defaultValue);
    }

    static void load(TdsUpdateConfigBean tdsUpdateConfig) {
      tdsUpdateConfig.setLogVersionInfo(Boolean.parseBoolean(TDSUPDAATE_LOGVERSIONINFO.getValueFromThreddsConfig()));
    }
  }

  /////////////////////////////////////////////////////////////////////

  TdsConfigMapper() {
  }

  void init(TdsContext tdsContext) {
    ServerInfoMappings.load(tdsServerInfo);
    HtmlConfigMappings.load(htmlConfig, tdsContext, tdsServerInfo);
    WmsConfigMappings.load(wmsConfig);
    CorsConfigMappings.load(corsConfig);
    TdsUpdateConfigMappings.load(tdsUpdateConfig);
  }

}

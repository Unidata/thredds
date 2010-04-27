package thredds.server.config;

import thredds.servlet.ThreddsConfig;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class TdsConfigurator
{
  private TdsConfigurator() {}

  private void initFromConfig( HtmlConfig htmlConfig)
  {
    htmlConfig.setPageCssUrl( ThreddsConfig.get( "htmlSetup.standardCssUrl", "" ));
    htmlConfig.setCatalogCssUrl( ThreddsConfig.get( "htmlSetup.catalogCssUrl", "" ) );

    htmlConfig.setFolderIconUrl( ThreddsConfig.get( "htmlSetup.folderIconUrl", "" ) );
    htmlConfig.setFolderIconAlt( ThreddsConfig.get( "htmlSetup.folderIconAlt", "" ) );
    htmlConfig.setDatasetIconUrl( ThreddsConfig.get( "htmlSetup.datasetIconUrl", "" ) );
    htmlConfig.setDatasetIconAlt( ThreddsConfig.get( "htmlSetup.datasetIconAlt", "" ) );

    // Get server information (prefered tag, deprecated tag, default value).
    htmlConfig.setInstallName( ThreddsConfig.get( "serverInformation.name", null ) );
    if ( htmlConfig.getInstallName() == null ) htmlConfig.setInstallName( ThreddsConfig.get( "htmlSetup.installName", "" ) );
    htmlConfig.setInstallLogoUrl( ThreddsConfig.get( " serverInformation.logoUrl", null ) );
    if ( htmlConfig.getInstallLogoUrl() == null ) htmlConfig.setInstallLogoUrl( ThreddsConfig.get( "htmlSetup.installLogoUrl", "" ) );
    htmlConfig.setInstallLogoAlt( ThreddsConfig.get( "serverInformation.logoAltText", null ) );
    if ( htmlConfig.getInstallLogoAlt() == null ) htmlConfig.setInstallLogoAlt( ThreddsConfig.get( "htmlSetup.installLogoAlt", "" ) );

    // Get host institution information (prefered tag, deprecated tag, default value).
    htmlConfig.setHostInstName( ThreddsConfig.get( "serverInformation.hostInstitution.name", null ) );
    htmlConfig.setHostInstUrl( ThreddsConfig.get( "serverInformation.hostInstitution.webSite", null ) );
    htmlConfig.setHostInstLogoUrl( ThreddsConfig.get( "serverInformation.hostInstitution.logoUrl", null ) );
    htmlConfig.setHostInstLogoAlt( ThreddsConfig.get( "serverInformation.hostInstitution.logoAltText", null ) );

    if ( htmlConfig.getHostInstName() == null ) htmlConfig.setHostInstName( ThreddsConfig.get( "htmlSetup.hostInstName", "" ) );
    if ( htmlConfig.getHostInstUrl() == null ) htmlConfig.setHostInstUrl( ThreddsConfig.get( "htmlSetup.hostInstUrl", "" ) );
    if ( htmlConfig.getHostInstLogoUrl() == null ) htmlConfig.setHostInstLogoUrl( ThreddsConfig.get( "htmlSetup.hostInstLogoUrl", "" ) );
    if ( htmlConfig.getHostInstLogoAlt() == null ) htmlConfig.setHostInstLogoAlt( ThreddsConfig.get( "htmlSetup.hostInstLogoAlt", "" ) );
  }


}

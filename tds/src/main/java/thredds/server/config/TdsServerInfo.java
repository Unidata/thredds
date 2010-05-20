package thredds.server.config;

import thredds.servlet.ThreddsConfig;

import java.util.Hashtable;
import java.util.Map;

/**
 * _more_
 *
 * @author edavis
 * @since 4.1
 */
public class TdsServerInfo
{
  private String name;
  private String logoUrl;
  private String logoAltText;
  private String summary;
  private String keywords;
  private String contactName;
  private String contactOrganization;
  private String contactEmail;
  private String contactPhone;
  private String hostInstitutionName;
  private String hostInstitutionWebSite;
  private String hostInstitutionLogoUrl;
  private String hostInstitutionLogoAltText;

  public String getName() {
    return name;
  }

  public void setName( String name ) {
    this.name = name;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public void setLogoUrl( String logoUrl ) {
    this.logoUrl = logoUrl;
  }

  public String getLogoAltText() {
    return logoAltText;
  }

  public void setLogoAltText( String logoAltText ) {
    this.logoAltText = logoAltText;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary( String summary ) {
    this.summary = summary;
  }

  public String getKeywords() {
    return keywords;
  }

  public void setKeywords( String keywords ) {
    this.keywords = keywords;
  }

  public String getContactName() {
    return contactName;
  }

  public void setContactName( String contactName ) {
    this.contactName = contactName;
  }

  public String getContactOrganization() {
    return contactOrganization;
  }

  public void setContactOrganization( String contactOrganization ) {
    this.contactOrganization = contactOrganization;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail( String contactEmail ) {
    this.contactEmail = contactEmail;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public void setContactPhone( String contactPhone ) {
    this.contactPhone = contactPhone;
  }

  public String getHostInstitutionName() {
    return hostInstitutionName;
  }

  public void setHostInstitutionName( String hostInstitutionName ) {
    this.hostInstitutionName = hostInstitutionName;
  }

  public String getHostInstitutionWebSite() {
    return hostInstitutionWebSite;
  }

  public void setHostInstitutionWebSite( String hostInstitutionWebSite ) {
    this.hostInstitutionWebSite = hostInstitutionWebSite;
  }

  public String getHostInstitutionLogoUrl() {
    return hostInstitutionLogoUrl;
  }

  public void setHostInstitutionLogoUrl( String hostInstitutionLogoUrl ) {
    this.hostInstitutionLogoUrl = hostInstitutionLogoUrl;
  }

  public String getHostInstitutionLogoAltText() {
    return hostInstitutionLogoAltText;
  }

  public void setHostInstitutionLogoAltText( String hostInstitutionLogoAltText ) {
    this.hostInstitutionLogoAltText = hostInstitutionLogoAltText;
  }
}

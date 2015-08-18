/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package thredds.server.config;

import org.springframework.stereotype.Component;

/**
 * Server info model
 *
 * @author edavis
 * @since 4.1
 */
@Component
public class TdsServerInfoBean {
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

  public void setName(String name) {
    this.name = name;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public void setLogoUrl(String logoUrl) {
    this.logoUrl = logoUrl;
  }

  public String getLogoAltText() {
    return logoAltText;
  }

  public void setLogoAltText(String logoAltText) {
    this.logoAltText = logoAltText;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getKeywords() {
    return keywords;
  }

  public void setKeywords(String keywords) {
    this.keywords = keywords;
  }

  public String getContactName() {
    return contactName;
  }

  public void setContactName(String contactName) {
    this.contactName = contactName;
  }

  public String getContactOrganization() {
    return contactOrganization;
  }

  public void setContactOrganization(String contactOrganization) {
    this.contactOrganization = contactOrganization;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public void setContactPhone(String contactPhone) {
    this.contactPhone = contactPhone;
  }

  public String getHostInstitutionName() {
    return hostInstitutionName;
  }

  public void setHostInstitutionName(String hostInstitutionName) {
    this.hostInstitutionName = hostInstitutionName;
  }

  public String getHostInstitutionWebSite() {
    return hostInstitutionWebSite;
  }

  public void setHostInstitutionWebSite(String hostInstitutionWebSite) {
    this.hostInstitutionWebSite = hostInstitutionWebSite;
  }

  public String getHostInstitutionLogoUrl() {
    return hostInstitutionLogoUrl;
  }

  public void setHostInstitutionLogoUrl(String hostInstitutionLogoUrl) {
    this.hostInstitutionLogoUrl = hostInstitutionLogoUrl;
  }

  public String getHostInstitutionLogoAltText() {
    return hostInstitutionLogoAltText;
  }

  public void setHostInstitutionLogoAltText(String hostInstitutionLogoAltText) {
    this.hostInstitutionLogoAltText = hostInstitutionLogoAltText;
  }
}

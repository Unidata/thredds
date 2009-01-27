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
package thredds.catalog;

import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.nc2.constants.FeatureType;

/**
 * Proxy an InvDatasetImpl to another InvDatasetImpl.
 */
public class InvDatasetImplProxy extends thredds.catalog.InvDatasetImpl{
  private thredds.catalog.InvDatasetImpl proxy;
  private String aliasName;

  public InvDatasetImplProxy ( String aliasName, thredds.catalog.InvDatasetImpl proxy) {
    super(proxy.getParent(), proxy.getName());
    this.aliasName = aliasName;
    this.proxy = proxy;
  }

  public String getAliasName() { return aliasName; }

  public void addAccess(thredds.catalog.InvAccess p0) {
    proxy.addAccess( p0);
  }

  public void addDataset(thredds.catalog.InvDatasetImpl p0) {
    proxy.addDataset( p0);
  }

  public void addDocumentation(thredds.catalog.InvDocumentation p0) {
    proxy.addDocumentation( p0);
  }

  public void addProperty(thredds.catalog.InvProperty p0) {
    proxy.addProperty( p0);
  }

  public void addService(thredds.catalog.InvService p0) {
    proxy.addService( p0);
  }

   boolean check(java.lang.StringBuilder p0, boolean p1) {
    return proxy.check( p0, p1);
  }

  public java.lang.String dump() {
    return proxy.dump();
  }

   java.lang.String dump(int p0) {
    return proxy.dump( p0);
  }

  public boolean equals(java.lang.Object p0) {
    return proxy.equals( p0);
  }

  public thredds.catalog.InvDatasetImpl findDatasetByName(java.lang.String p0) {
    return proxy.findDatasetByName( p0);
  }

  public java.lang.String findProperty(java.lang.String p0) {
    return proxy.findProperty( p0);
  }

  public thredds.catalog.InvService findService(java.lang.String p0) {
    return proxy.findService( p0);
  }

  public boolean finish() {
    return true;
  }

  public thredds.catalog.InvAccess getAccess(thredds.catalog.ServiceType p0) {
    return proxy.getAccess( p0);
  }

  public java.util.List<InvAccess> getAccess() {
    return proxy.getAccess();
  }

  public java.util.List<InvAccess> getAccessLocal() {
    return proxy.getAccessLocal();
  }

  public java.lang.String getAlias() {
    return proxy.getAlias();
  }

  public java.lang.String getAuthority() {
    return proxy.getAuthority();
  }

  public thredds.catalog.CollectionType getCollectionType() {
    return proxy.getCollectionType();
  }

  public java.util.List<ThreddsMetadata.Contributor> getContributors() {
    return proxy.getContributors();
  }

  public java.util.List<ThreddsMetadata.Source> getCreators() {
    return proxy.getCreators();
  }

  public thredds.catalog.DataFormatType getDataFormatType() {
    return proxy.getDataFormatType();
  }

  public FeatureType getDataType() {
    return proxy.getDataType();
  }

  public java.util.List<InvDataset> getDatasets() {
    return proxy.getDatasets();
  }

  public java.util.List<DateType> getDates() {
    return proxy.getDates();
  }

  public java.util.List<InvDocumentation> getDocumentation() {
    return proxy.getDocumentation();
  }

  public java.lang.String getDocumentation(java.lang.String p0) {
    return proxy.getDocumentation( p0);
  }

  public java.lang.String getFullName() {
    return proxy.getFullName();
  }

  public thredds.catalog.ThreddsMetadata.GeospatialCoverage getGeospatialCoverage() {
    return proxy.getGeospatialCoverage();
  }

  public java.lang.String getID() {
    return proxy.getID();
  }

  public java.util.List<ThreddsMetadata.Vocab> getKeywords() {
    return proxy.getKeywords();
  }

  public thredds.catalog.ThreddsMetadata getLocalMetadata() {
    return proxy.getLocalMetadata();
  }

  protected boolean getMark() {
    return proxy.getMark();
  }

  public java.util.List<InvMetadata> getMetadata(thredds.catalog.MetadataType p0) {
    return proxy.getMetadata( p0);
  }

  public java.util.List<InvMetadata> getMetadata() {
    return proxy.getMetadata();
  }

  public java.lang.String getName() {
    return proxy.getName();
  }

  public thredds.catalog.InvDataset getParent() {
    return proxy.getParent();
  }

  public thredds.catalog.InvCatalog getParentCatalog() {
    return proxy.getParentCatalog();
  }

  public java.util.List<ThreddsMetadata.Vocab> getProjects() {
    return proxy.getProjects();
  }

  public java.util.List<InvProperty> getProperties() {
    return proxy.getProperties();
  }

  public java.util.List<ThreddsMetadata.Source> getPublishers() {
    return proxy.getPublishers();
  }

  public thredds.catalog.InvService getServiceDefault() {
    return proxy.getServiceDefault();
  }

  public java.util.List<InvService> getServicesLocal() {
    return proxy.getServicesLocal();
  }

  public DateRange getTimeCoverage() {
    return proxy.getTimeCoverage();
  }

  public java.lang.String getUniqueID() {
    return proxy.getUniqueID();
  }

  public java.lang.String getUrlPath() {
    return proxy.getUrlPath();
  }

  public java.lang.Object getUserProperty(java.lang.Object p0) {
    return proxy.getUserProperty( p0);
  }

  public java.util.List<ThreddsMetadata.Variables> getVariables() {
    return proxy.getVariables();
  }

  public boolean hasAccess() {
    return proxy.hasAccess();
  }

  public boolean hasNestedDatasets() {
    return proxy.hasNestedDatasets();
  }

  public int hashCode() {
    return proxy.hashCode();
  }

  public boolean isHarvest() {
    return proxy.isHarvest();
  }

  public boolean removeDataset(thredds.catalog.InvDatasetImpl p0) {
    return proxy.removeDataset( p0);
  }

  public void removeService(thredds.catalog.InvService p0) {
    proxy.removeService( p0);
  }

  public void setAlias(java.lang.String p0) {
    proxy.setAlias( p0);
  }

  public void setAuthority(java.lang.String p0) {
    proxy.setAuthority( p0);
  }

  public void setCatalog(thredds.catalog.InvCatalog p0) {
    proxy.setCatalog( p0);
  }

  public void setCollectionType(thredds.catalog.CollectionType p0) {
    proxy.setCollectionType( p0);
  }

  public void setContributors(java.util.List<ThreddsMetadata.Contributor> p0) {
    proxy.setContributors( p0);
  }

  public void setGeospatialCoverage(thredds.catalog.ThreddsMetadata.GeospatialCoverage p0) {
    proxy.setGeospatialCoverage( p0);
  }

  public void setHarvest(boolean p0) {
    proxy.setHarvest( p0);
  }

  public void setID(java.lang.String p0) {
    proxy.setID( p0);
  }

  public void setKeywords(java.util.List<ThreddsMetadata.Vocab> p0) {
    proxy.setKeywords( p0);
  }

  public void setLocalMetadata(thredds.catalog.ThreddsMetadata p0) {
    proxy.setLocalMetadata( p0);
  }

  protected void setMark(boolean p0) {
    proxy.setMark( p0);
  }

  public void setName(java.lang.String p0) {
    proxy.setName( p0);
  }

  public void setParent(thredds.catalog.InvDatasetImpl p0) {
    proxy.setParent( p0);
  }

  public void setProjects(java.util.List<ThreddsMetadata.Vocab> p0) {
    proxy.setProjects( p0);
  }

  public void setPublishers(java.util.List<ThreddsMetadata.Source> p0) {
    proxy.setPublishers( p0);
  }

  public void setServicesLocal(java.util.List<InvService> p0) {
    proxy.setServicesLocal( p0);
  }

  public void setTimeCoverage(DateRange p0) {
    proxy.setTimeCoverage( p0);
  }

  public void setUrlPath(java.lang.String p0) {
    proxy.setUrlPath( p0);
  }

  public void setUserProperty(java.lang.Object p0, java.lang.Object p1) {
    proxy.setUserProperty( p0, p1);
  }

  public java.lang.String toString() {
    return proxy.toString();
  }

}

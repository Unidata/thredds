/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
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
package thredds.client.catalog;

import net.jcip.annotations.Immutable;
import ucar.nc2.constants.FeatureType;

import java.util.ArrayList;
import java.util.List;

/**
 * <xsd:complexType name="DatasetType">
   <xsd:sequence>
     <xsd:group ref="threddsMetadataGroup" minOccurs="0" maxOccurs="unbounded" />

     <xsd:element ref="access" minOccurs="0" maxOccurs="unbounded"/>
     <xsd:element ref="ncml:netcdf" minOccurs="0"/>
     <xsd:element ref="dataset" minOccurs="0" maxOccurs="unbounded"/>
   </xsd:sequence>

   <xsd:attribute name="name" type="xsd:string" use="required"/>
   <xsd:attribute name="alias" type="xsd:token"/>
   <xsd:attribute name="authority" type="xsd:string"/> <!-- deprecated : use element -->
   <xsd:attribute name="collectionType" type="collectionTypes"/>
   <xsd:attribute name="dataType" type="dataTypes"/> <!-- deprecated : use element -->
   <xsd:attribute name="harvest" type="xsd:boolean"/>
   <xsd:attribute name="ID" type="xsd:token"/>
   <xsd:attribute name="resourceControl" type="xsd:string"/>

   <xsd:attribute name="serviceName" type="xsd:string" /> <!-- deprecated : use element -->
   <xsd:attribute name="urlPath" type="xsd:token" />
 </xsd:complexType>
 *
 * @author caron
 * @since 1/7/2015
 */
@Immutable
public class Dataset extends DatasetNode {                 // (11)
  private final String COLLECTIONTYPE = "collectionType";
  private final String HARVEST = "harvest";
  private final String ID = "id";
  private final String URLPATH = "urlPath";
  private final String METADATA = "metadata";
  private final String ACCESS = "access";

  public Dataset(DatasetNode parent, String name, String collectionType, Boolean harvest, String id, String urlPath, List<Metadata> metadata, List<Access> access, List<Dataset> datasets) {
    super(parent, name, datasets);
    if (collectionType != null) flds.put(COLLECTIONTYPE, collectionType);
    if (harvest != null) flds.put(HARVEST, harvest);
    if (id != null) flds.put(ID, id);
    if (urlPath != null) flds.put(URLPATH, urlPath);
    if (metadata != null) flds.put(METADATA, metadata);
    if (access != null) flds.put(ACCESS, access);
  }

  public String getCollectionType() {
    return (String) flds.get(COLLECTIONTYPE);
  }

  public boolean isHarvest() {
    return (Boolean) flds.get(HARVEST);
  }

  public String getId() {
    return (String) flds.get(ID);
  }

  public String getUrlPath() {
    return (String) flds.get(URLPATH);
  }

  public List<Metadata> getMetadata() {
    List<Metadata> metadata = (List<Metadata>) flds.get(METADATA);
    return metadata == null ? new ArrayList<Metadata>(0) : metadata;
  }

  public List<Metadata> getMetadata(Metadata.Type want) {
    List<Metadata> result = new ArrayList<>();
    for (Metadata m : getMetadata())
      if (m.getType() == want) result.add(m);
    return result;
  }

  public List<Access> getAccess() {
    List<Access> access = (List<Access>) flds.get(ACCESS);
    return access == null ? new ArrayList<Access>(0) : access;
  }

  public Access getAccess(ServiceType type) {
    for (Access acc : getAccess())
      if (acc.getService().getType() == type) return acc;
    return null;
  }

  public boolean hasAccess() {
    List<Access> access = getAccess();
    return !access.isEmpty();
  }

  /**
   * Get access element that matches the given access standard URL.
   * Match on a.getStandardUrlName().
   *
   * @param accessURL find theis access URL string
   * @return InvAccess or null if no match.
   */
  public Access findAccess(String accessURL) {
    for (Access a : getAccess()) {
      if (accessURL.equals(a.getStandardUrlName()))
        return a;
    }
    return null;
  }

  public List<Property> getProperties() {
    return new ArrayList<>(0);
  }

  public FeatureType getFeatureType() {
    return null;
  }

  public boolean isDatasetScan() {
    return false;
  }
}

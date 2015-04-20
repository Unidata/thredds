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
import thredds.client.catalog.builder.AccessBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;

import java.util.*;

/**
 * A Client Catalog Dataset
 * @author caron
 * @since 1/7/2015
 */
@Immutable
public class Dataset extends DatasetNode implements ThreddsMetadataContainer {
  public static final String Access = "Access";                 // Access or list of Access
  public static final String Alias = "Alias";
  public static final String Authority = "Authority";
  public static final String CollectionType = "CollectionType";
  public static final String Contributors = "Contributors";     // Contributor or List of Contributor
  public static final String Creators = "Creators";             // String or List of String
  public static final String DataFormatType = "DataFormatType"; // String
  public static final String Datasets = "Datasets";
  public static final String DatasetHash = "DatasetHash";
  public static final String DatasetRoots = "DatasetRoots";
  public static final String DataSize = "DataSize";
  public static final String Dates = "Dates";
  public static final String Documentation = "Documentation";
  public static final String Expires = "Expires";
  public static final String FeatureType = "FeatureType";       // String
  public static final String GeospatialCoverage = "GeospatialCoverage";
  public static final String Harvest = "Harvest";
  public static final String Id = "Id";
  public static final String Keywords = "Keywords";
  public static final String MetadataOther = "MetadataOther";
  public static final String Ncml = "Ncml";
  public static final String Projects = "Projects";
  public static final String Properties = "Properties";
  public static final String Publishers = "Publishers";
  public static final String RestrictAccess = "RestrictAccess";
  public static final String ServiceName = "ServiceName";
  public static final String Services = "Services";
  public static final String ThreddsMetadataInheritable = "ThreddsMetadataInheritable";
  public static final String TimeCoverage = "TimeCoverage";             // DateRange
  public static final String VariableGroups = "VariableGroups";
  // public static final String VariableMapLink = "VariableMapLink";    // String
  public static final String VariableMapLinkURI = "VariableMapLinkURI";    // ThreddsMetadata.UriResolved
  public static final String Version = "Version";
  public static final String UrlPath = "UrlPath";
  public static final String UseRemoteCatalogService = "UseRemoteCatalogService";

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  public Dataset(DatasetNode parent, String name, Map<String, Object> flds, List<AccessBuilder> accessBuilders, List<DatasetBuilder> datasetBuilders) {
    super(parent, name, flds, datasetBuilders);

    if (accessBuilders != null && accessBuilders.size() > 0) {
      List<Access> access = new ArrayList<>(accessBuilders.size());
      for (AccessBuilder acc : accessBuilders)
        access.add ( acc.makeAccess(this));
      flds.put(Access, Collections.unmodifiableList(access));
    }
  }

  /**
   * Construct an Dataset which refers to a urlPath.
   * This is used to create a standalone Dataset, outside of an Catalog.
   * An "anonymous" Service is created and attached to the Dataset.
   *
   * @param urlPath  : construct URL from this path
   * @param featureType : feature type
   * @param dataFormatS : data format
   * @param serviceType    : ServiceType
   */
  static public Dataset makeStandalone(String urlPath, String featureType, String dataFormatS, String serviceType) {
    DatasetBuilder builder = new DatasetBuilder(null);
    builder.put(FeatureType, featureType);
    Service service = new Service("anon", "", serviceType, null, null, null, null);
    AccessBuilder access = new AccessBuilder(builder, urlPath, service, dataFormatS, 0);
    builder.addAccess(access);
    return builder.makeDataset(null);
  }

  /////////////////////////////////////////////////////

  public List<Access> getAccess() {
    List<Access> result = new ArrayList<>();
    String urlPath = getUrlPath();
    String serviceDefault = getServiceNameDefault();
    String dataFormat = getDataFormatName();
    long dataSize = getDataSize();

    Catalog cat = getParentCatalog();
    if (cat != null) {
      Service s = cat.findService(serviceDefault);

      // add access element if urlPath and service is specified
      if ((urlPath != null) && (s != null)) {
        Access a = new Access(this, urlPath, s, dataFormat, dataSize);
        addAllAccess(a, result);
      }
    }

    // add local access elements
    List<Access> access = (List<Access>) getLocalFieldAsList(Access);
    if (access != null) {
      for (Access a : access) {
        addAllAccess(a, result);
      }
    }

    return result;
  }

  private void addAllAccess(Access a, List<Access> result) {
    if (a.getService().getType() == ServiceType.Compound) {
      for (Service nested : a.getService().getNestedServices()) {
        Access nestedAccess = new Access(this, a.getUrlPath(), nested, a.getDataFormatName(), a.getDataSize());
        addAllAccess(nestedAccess, result); // i guess it could recurse
      }
    } else {
      result.add(a);
    }
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

  /**
   * Get URL to this dataset. Dataset must have an ID.
   * Form is catalogURL#DatasetID
   *
   * @return URL to this dataset.
   */
  public String getCatalogUrl() {
    Catalog parent = getParentCatalog();
    if (parent == null) return null;
    String baseUri = parent.getUriString();
    if (baseUri == null) return null;
    return baseUri + "#" + getId();
  }

   /////////////////////////////////////////////////////
  // non-inheritable metadata
  public String getCollectionType() {
    return (String) flds.get(CollectionType);
  }
  public boolean isDatasetScan() {
    return false;
  }
  public boolean isHarvest() {
    Boolean result = (Boolean) flds.get(Harvest);
    return (result != null) && result;
  }
  public String getId() {
    return (String) flds.get(Id);
  }
  public String getID() {
    return getId();
  }
  public String getUrlPath() {
    return (String) flds.get(UrlPath);
  }

  public org.jdom2.Element getNcmlElement() {
    return (org.jdom2.Element) getLocalField(Dataset.Ncml);
  }

  /////////////////////////////////////////////////////
  // inheritable metadata

  @Override
  public Object getLocalField(String fldName) {
    return flds.get(fldName);
  }

  Object getInheritedOnlyField(String fldName) {
    ThreddsMetadata tmi = (ThreddsMetadata) flds.get(ThreddsMetadataInheritable);
    if (tmi != null) {
      Object value = tmi.getLocalField(fldName);
      if (value != null) return value;
    }
    Dataset parent = getParentDataset();
    return (parent == null) ? null : parent.getInheritedOnlyField( fldName);
  }

  Object getInheritedField(String fldName) {
    Object value = flds.get(fldName);
    if (value != null) return value;
    return getInheritedOnlyField(fldName);
  }

  public String getAuthority() {
    return (String) getInheritedField(Authority);
  }

  public String getDataFormatName() {
    return (String) getInheritedField(DataFormatType);
  }

  public ucar.nc2.constants.DataFormatType getDataFormatType() {
    String name = getDataFormatName();
    if (name == null) return null;
    try {
      return ucar.nc2.constants.DataFormatType.getType(name);
    } catch (Exception e) {
      return null;
    }
  }

  public long getDataSize() {
    Long size = (Long) getInheritedField(DataSize);
    return (size == null) ? -1 : size;
  }

  public boolean hasDataSize() {
    Long size = (Long) getInheritedField(DataSize);
    return (size != null) && size > 0;
  }

  public ucar.nc2.constants.FeatureType getFeatureType() {
    String name = getFeatureTypeName();
    try {
      return ucar.nc2.constants.FeatureType.getType(name);
    } catch (Exception e) {
      return null;
    }
  }

  public String getFeatureTypeName() {
    return (String) getInheritedField(FeatureType);
  }

 public ThreddsMetadata.GeospatialCoverage getGeospatialCoverage() {
    return (ThreddsMetadata.GeospatialCoverage) getInheritedField(GeospatialCoverage);
  }

  public String getServiceNameDefault() {
    return (String) getInheritedField(ServiceName);
  }

  public Service getServiceDefault() {
    Catalog cat = getParentCatalog();
    if (cat == null) return null;
    return cat.findService( getServiceNameDefault());
  }

  public String getRestrictAccess() {
    return (String) getInheritedField(RestrictAccess);
  }

  public DateRange getTimeCoverage() {
    return (DateRange) getInheritedField(TimeCoverage);
  }

  public ThreddsMetadata.UriResolved getVariableMapLink() {
    return (ThreddsMetadata.UriResolved) getInheritedField(VariableMapLinkURI);
  }

  ///////////////////////////////////////////

  List getInheritedFieldAsList(String fldName) {
    List result = new ArrayList();
    Object value = flds.get(fldName); // first look for local
    if (value != null) {
      if (value instanceof List) result.addAll((List) value);
      else result.add(value);
    }

    getAllFromInherited(fldName, result);   // then look for inherited
    return result;
  }

  void getAllFromInherited(String fldName, List result) {
    ThreddsMetadata tmi = (ThreddsMetadata) flds.get(ThreddsMetadataInheritable);
    if (tmi != null) {
      Object value = tmi.getLocalField(fldName);
      if (value != null) {
        if (value instanceof List) result.addAll((List) value);
        else result.add(value);
      }
    }
    Dataset parent = getParentDataset();
    if (parent != null)
      parent.getAllFromInherited(fldName, result);
  }


  public List<ThreddsMetadata.Source> getCreators() {
    return (List<ThreddsMetadata.Source>) getInheritedFieldAsList(Dataset.Creators);
  }

  public List<ThreddsMetadata.Contributor> getContributors() {
    return (List<ThreddsMetadata.Contributor>) getInheritedFieldAsList(Dataset.Contributors);
  }

  public List<DateType> getDates() {
    return (List<DateType>) getInheritedFieldAsList(Dates);
  }   // prob only one type

  public List<Documentation> getDocumentation() {
    return (List<Documentation>) getInheritedFieldAsList(Documentation);
  }

  public List<ThreddsMetadata.Vocab> getKeywords() {
    return (List<ThreddsMetadata.Vocab>) getInheritedFieldAsList(Keywords);
  }

  public List<ThreddsMetadata.MetadataOther> getMetadataOther() {
    return (List<ThreddsMetadata.MetadataOther>) getInheritedFieldAsList(MetadataOther);
  }

  public java.util.List<ThreddsMetadata.MetadataOther> getMetadata(String want) {
    List<ThreddsMetadata.MetadataOther> result = new ArrayList<>();
    for (ThreddsMetadata.MetadataOther m : getMetadataOther()) {
      if (m.getType() != null && m.getType().equalsIgnoreCase(want))
        result.add(m);
    }
    return result;
  }

  public List<ThreddsMetadata.Vocab> getProjects() {
    return (List<ThreddsMetadata.Vocab>) getInheritedFieldAsList(Projects);
  }

  public List<Property> getProperties() {
    List<Property> result = getInheritedFieldAsList(Properties);
    return Property.removeDups(result);
  }

  public String findProperty(String name) {
    Property result = null;
    for (Property p : getProperties()) {
      if (p.getName().equals(name))
        result = p;
    }
    return (result == null) ? null : result.getValue();
  }

  public List<ThreddsMetadata.Source> getPublishers() {
    return (List<ThreddsMetadata.Source>) getInheritedFieldAsList(Dataset.Publishers);
  }

  public List<ThreddsMetadata.VariableGroup> getVariables() {
    return (List<ThreddsMetadata.VariableGroup>) getInheritedFieldAsList(Dataset.VariableGroups);
  }

  public String getDocumentation(String type) {
    for (Documentation doc : getDocumentation()) {
      String dtype = doc.getType();
      if ((dtype != null) && dtype.equalsIgnoreCase(type))
        return doc.getInlineContent();
    }
    return null;
  }

  /**
   * @return specific type of documentation = history
   */
  public String getHistory() {
    return getDocumentation("history");
  }

  /**
   * @return specific type of documentation = processing_level
   */
  public String getProcessing() {
    return getDocumentation("processing_level");
  }

  /**
   * @return specific type of documentation = rights
   */
  public String getRights() {
    return getDocumentation("rights");
  }

  /**
   * @return specific type of documentation = summary
   */
  public String getSummary() {
    return getDocumentation("summary");
  }

  public DateType getLastModifiedDate() {
    for (DateType dateType : getDates()) {
      if ((dateType.getType() != null) && dateType.getType().equals("modified")) {
        return dateType;
      }
    }
    return null;
  }

}

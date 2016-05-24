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

import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;

import java.util.ArrayList;
import java.util.List;

/**
 * Public interface to a thredds dataset, basic abstraction for data.
 *
 * @author john caron
 */

public abstract class InvDataset {

  // not inherited

  protected InvCatalog catalog; // null unless top level dataset.
  protected InvDataset parent;  // null if top level dataset.
  protected String name;
  protected String id;
  protected List<InvDataset> datasets = new ArrayList<InvDataset>(); // local
  protected boolean harvest;
  protected CollectionType collectionType;

  // not inherited but need local copy
  protected List<InvAccess> access = new ArrayList<InvAccess>(); // expanded if compound service
  protected List<InvService> services = new ArrayList<InvService>(); // expanded if compound service

  // inherited
  protected String authorityName;
  protected FeatureType dataType;
  protected InvService defaultService;
  protected DataFormatType dataFormatType;
  protected String restrictAccess;

  protected List<InvDocumentation> docs;
  protected List<InvMetadata> metadata;
  protected List<InvProperty> properties;

  protected List<ThreddsMetadata.Source> creators;
  protected List<ThreddsMetadata.Contributor> contributors;
  protected List<DateType> dates;
  protected List<ThreddsMetadata.Vocab> keywords;
  protected List<ThreddsMetadata.Vocab> projects;
  protected List<ThreddsMetadata.Source> publishers;
  protected List<ThreddsMetadata.Variables> variables;
  public ThreddsMetadata.GeospatialCoverage gc;
  public DateRange tc;
  protected String variableMapLink;

  // for subclassing
  protected InvDataset(InvDataset parent, String name) {
    this.parent = parent;
    this.name = name;
  }

  /**
   * Get the "human readable" name of the dataset.
   *
   * @return "human readable" name of the dataset.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the full, heirarchical name of the dataset, which has all parent collection names.
   *
   * @return full, heirarchical name of the dataset, which has all parent collection names.
   */
  public String getFullName() {
    return (parent == null)
        ? name
        : (parent.getFullName() == null || parent.getFullName().length() == 0)
        ? name
        : parent.getFullName() + "/" + name;
  }

  /**
   * Get collectionType
   *
   * @return collectionType, or null
   */
  public CollectionType getCollectionType() {
    return collectionType;
  }

  /**
   * @return if harvest is true
   */
  public boolean isHarvest() {
    return harvest;
  }

  /**
   * Get the id of the dataset, or null.
   *
   * @return the id of the dataset, or null if none.
   */
  public String getID() {
    return id;
  }

  /**
   * If this dataset has an authority and an ID, then the concatenation of them is the
   * globally unique ID.
   *
   * @return globally unique ID, or null if missing authority or ID.
   */
  public String getUniqueID() {
    String authority = getAuthority();
    if ((authority != null) && (getID() != null))
      return authority + ":" + getID();
    else if (getID() != null)
      return getID();
    else
      return null;
  }

  /**
   * Get authority for this Dataset, may be null.
   *
   * @return authority for this Dataset, or null.
   */
  public String getAuthority() {
    return authorityName;
  }

  /**
   * Get the DataType (which may be inherited from parent), or null .
   *
   * @return the DataType or null
   */
  public FeatureType getDataType() {
    return dataType;
  }

  /**
   * Get the DataFormatType (which may be inherited from parent), or null .
   *
   * @return the DataFormatType or null .
   */
  public thredds.catalog.DataFormatType getDataFormatType() {
    return dataFormatType;
  }

  /**
   * If this dataset has access elements.
   *
   * @return true if has access elements.
   */
  public boolean hasAccess() {
    return !access.isEmpty();
  }

  /**
   * Get all access elements for this dataset.
   * This list will expand any compound services.
   *
   * @return List of InvAccess objects. List may not be null, may be empty.
   */
  public java.util.List<InvAccess> getAccess() {
    return access;
  }

  /**
   * Get access element of the specified service type for this dataset.
   * If more than one, get the first one.
   *
   * @param type find this ServiceType
   * @return InvAccess or null if there is not one.
   */
  public InvAccess getAccess(thredds.catalog.ServiceType type) {
    for (InvAccess a : getAccess()) {
      InvService s = a.getService();
      if (s.getServiceType() == type)
        return a;
    }
    return null;
  }

  /**
   * Get access element that matches the given access standard URL.
   * Match on a.getStandardUrlName().
   *
   * @param accessURL find theis access URL string
   * @return InvAccess or null if no match.
   */
  public InvAccess findAccess(String accessURL) {
    for (InvAccess a : getAccess()) {
      if (accessURL.equals(a.getStandardUrlName()))
        return a;
    }
    return null;
  }

  /**
   * Return the query fragment referencing this dataset, ie "catalog=catalog.xml&dataset=datasetID"
   *
   * @return the query fragment for this dataset
   */
  public String getSubsetUrl() {
    if (getID() == null) return null;
    return "catalog=" + getParentCatalog().baseURI.toString() + "&amp;dataset=" + getID();
  }

  /**
   * If this dataset has nested datasets.
   *
   * @return true if has nested datasets.
   */
  public boolean hasNestedDatasets() {
    return !getDatasets().isEmpty();
  }

  /**
   * Get a list of all the nested datasets.
   *
   * @return list of objects of type InvDataset. May be empty, not null.
   */
  public java.util.List<InvDataset> getDatasets() {
    return datasets;
  }

  /**
   * Find an immediate child dataset by its name.
   *
   * @param name match on this name
   * @return dataset if found or null if not exist.
   */
  public InvDatasetImpl findDatasetByName(String name) {
    for (InvDataset ds : getDatasets()) {
      if (ds.getName().equals(name))
        return (InvDatasetImpl) ds;
    }
    return null;
  }

  /**
   * Get parent dataset.
   *
   * @return parent dataset. If top dataset, return null.
   */
  public InvDataset getParent() {
    return parent;
  }

  /**
   * Get containing catalog.
   *
   * @return containing catalog.
   */
  public InvCatalog getParentCatalog() {
    if (catalog != null) return catalog;
    return (parent != null) ? parent.getParentCatalog() : null;
  }

  /**
   * Get URL to this dataset. Dataset must have an ID.
   * Form is catalogURL#DatasetID
   *
   * @return URL to this dataset.
   */
  public String getCatalogUrl() {
    return getParentCatalog().getUriString() + "#" + getID();
  }

  /**
   * Get list of documentation elements for this dataset.
   *
   * @return list of InvDocumentation objects. May be empty, not null.
   */
  public java.util.List<InvDocumentation> getDocumentation() {
    return docs;
  }

  /**
   * Get all properties for this dataset. These may have been specified
   * in the dataset or an enclosing parent element.
   *
   * @return List of type InvProperty. May be empty, not null.
   */
  public java.util.List<InvProperty> getProperties() {
    return properties;
  }

  /**
   * Finds the <b>first</b> property declared in the dataset with the given name. If a property other than the first
   * is desired, it can be obtained via {@link #getProperties}.
   * <p>
   * The property may have been specified in the dataset or in an enclosing parent element.
   *
   * @param name  the name of the property.
   * @return  the string value of the <b>first</b> property with the given name or
   *          {@code null} if no such property exists.
   */
  // TODO: Change the name of this method to something like "findFirstProperty".
  // This is an API change, so will have to be done in a major release.
  public String findProperty(String name) {
    InvProperty result = null;
    for (InvProperty p : getProperties()) {
      if (p.getName().equals(name))
        result = p;
    }
    return (result == null) ? null : result.getValue();
  }

  /**
   * Get the metadata elements for this InvDataset.
   *
   * @return List of InvMetadata objects. List may be empty but not null.
   */
  public java.util.List<InvMetadata> getMetadata() {
    return metadata;
  }

  /**
   * Get the metadata elements of the specified type.
   *
   * @param want find this metadata type
   * @return List of InvMetadata objects. List may be empty but not null.
   */
  public java.util.List<InvMetadata> getMetadata(thredds.catalog.MetadataType want) {
    List<InvMetadata> result = new ArrayList<InvMetadata>();
    for (InvMetadata m : getMetadata()) {
      MetadataType mtype = MetadataType.getType(m.getMetadataType());
      if (mtype == want)
        result.add(m);
    }
    return result;
  }

  /**
   * Find the named service declared in this dataset or one of its parents.
   *
   * @param name match this name
   * @return first service that matches the given name, or null if none found.
   */
  public InvService findService(String name) {
    if (name == null) return null;

    // search local (but expanded) services
    for (InvService p : services) {
      if (p.getName().equals(name))
        return p;
    }

    // not found, look in parent
    if (parent != null)
      return parent.findService(name);
    return (catalog == null) ? null : catalog.findService(name);
  }

  /**
   * Find the default service for this dataset and its children.
   * If not declared in this dataset, search in parents.
   * This is the default for any nested datasets or access elements.
   *
   * @return default service, or null
   */
  public InvService getServiceDefault() {
    return defaultService;
  }

  /**
   * Return the resource control value which indicates that only users with
   * proper permission can access this resource.
   * <p/>
   * ??? Not sure if the value indicates anything or just set or not set.
   *
   * @return the resource control value for this dataset (inherited from ancestor datasets).
   */
  public String getRestrictAccess() {
    if (restrictAccess != null) return restrictAccess;
    // not found, look in parent
    if (parent != null)
      return parent.getRestrictAccess();

    return null;
  }

  // 1.0 metadata
  /**
   * get any Creator metadata
   *
   * @return List of type ThreddsMetadata.Source, may be empty
   */
  public List<ThreddsMetadata.Source> getCreators() {
    return creators;
  }

  /**
   * get Contributor metadata
   *
   * @return List of type ThreddsMetadata.Contributor, may be empty
   */
  public List<ThreddsMetadata.Contributor> getContributors() {
    return contributors;
  }

  /**
   * get any DateType metadata
   *
   * @return List of type DateType, may be empty
   */
  public List<DateType> getDates() {
    return dates;
  }

  /**
   * get any Keyword metadata
   *
   * @return List of type ThreddsMetadata.Vocab, may be empty
   */
  public List<ThreddsMetadata.Vocab> getKeywords() {
    return keywords;
  }

  /**
   * get Projects metadata
   *
   * @return List of type ThreddsMetadata.Vocab, may be empty
   */
  public List<ThreddsMetadata.Vocab> getProjects() {
    return projects;
  }

  /**
   * get Publisher metadata
   *
   * @return List of type ThreddsMetadata.Source, may be empty
   */
  public List<ThreddsMetadata.Source> getPublishers() {
    return publishers;
  }

  /**
   * get specific type of documentation = history
   *
   * @return contents of the "history" documentation, or null
   */
  public String getHistory() {
    return getDocumentation("history");
  }

  /**
   * get specific type of documentation = processing_level
   *
   * @return contents of the "processing_level" documentation, or null
   */
  public String getProcessing() {
    return getDocumentation("processing_level");
  }

  /**
   * get specific type of documentation = rights
   *
   * @return contents of the "rights" documentation, or null
   */
  public String getRights() {
    return getDocumentation("rights");
  }

  /**
   * get specific type of documentation = summary
   *
   * @return contents of the "summary" documentation, or null
   */
  public String getSummary() {
    return getDocumentation("summary");
  }

  /**
   * get Variable metadata
   *
   * @return List of type ThreddsMetadata.Variables, may be empty
   */
  public List<ThreddsMetadata.Variables> getVariables() {
    return variables;
  }

  public String getVariableMapLink() {
    return variableMapLink;
  }

  /**
   * get Variables from the specified vocabulary
   *
   * @param vocab look for this vocabulary
   * @return Variables from the specified vocabulary, may be null
   */
  public ThreddsMetadata.Variables getVariables(String vocab) {
    ThreddsMetadata.Variables result = new ThreddsMetadata.Variables(vocab, null, null, null, null);
    if (variables == null) return result;

    for (ThreddsMetadata.Variables vs : variables) {
      if (vs.getVocabulary().equals(vocab))
        result.getVariableList().addAll(vs.getVariableList());
    }
    return result;
  }

  /**
   * get geospatial coverage
   *
   * @return geospatial coverage, or null if none
   */
  public ThreddsMetadata.GeospatialCoverage getGeospatialCoverage() {
    return gc;
  }

  /**
   * get time coverage as CalendarDateRange
   *
   * @return time coverage, or null if none
   */
  public CalendarDateRange getCalendarDateCoverage() {
    return CalendarDateRange.of(tc);
  }

  /**
   * get time coverage
   *
   * @return time coverage, or null if none
   */
  public DateRange getTimeCoverage() {
    return tc;
  }

  /**
   * get specific type of documentation
   *
   * @param type find this type of documentation
   * @return contents of documentation of specified type
   */
  public String getDocumentation(String type) {
    if (getDocumentation() == null)
      return null;
    for (InvDocumentation doc : getDocumentation()) {
      String dtype = doc.getType();
      if ((dtype != null) && dtype.equalsIgnoreCase(type)) return doc.getInlineContent();
    }
    return null;
  }

}

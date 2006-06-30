// $Id: InvDataset.java,v 1.19 2006/05/19 19:23:03 edavis Exp $
/*
 * Copyright 2002 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package thredds.catalog;

import thredds.datatype.DateRange;

import java.util.*;

/**
 * Public interface to a thredds dataset, basic abstraction for data.
 *
 * @author john caron
 * @version $Revision: 1.19 $ $Date: 2006/05/19 19:23:03 $
 */

public abstract class InvDataset {

  // not inherited
  protected InvCatalog catalog; // null unless top level dataset.
  protected InvDataset parent;  // null if top level dataset.
  protected String name;
  protected String id;
  protected ArrayList datasets = new ArrayList(); // local
  protected boolean harvest;
  protected CollectionType collectionType;

  // not inherited but need local copy
  protected ArrayList access = new ArrayList(); // expanded if compound service
  protected ArrayList services = new ArrayList(); // expanded if compound service

  // inherited
  protected String authorityName;
  protected DataType dataType;
  protected InvService defaultService;
  protected DataFormatType dataFormatType;
  protected String resourceControl;

  protected ArrayList docs;
  protected ArrayList metadata;
  protected ArrayList properties;

  protected ArrayList creators;
  protected ArrayList contributors;
  protected ArrayList dates;
  protected ArrayList keywords;
  protected ArrayList projects;
  protected ArrayList publishers;
  protected ArrayList variables;
  public ThreddsMetadata.GeospatialCoverage gc;
  public DateRange tc;

  // for subclassing
  protected InvDataset( InvDataset parent, String name) {
    this.parent = parent;
    this.name = name;
  }

  /**
   * Get the "human readable" name of the dataset.
   */
  public String getName() { return name; }

  /**
   * Get the full, heirarchical name of the dataset, which has all parent collection names.
   */
  public String getFullName() {
    return (parent == null)
        ? name
        : (parent.getFullName() == null || parent.getFullName().equals(""))
        ? name
        : parent.getFullName() + "/" + name;
  }

  /** Get collectionType */
  public CollectionType getCollectionType() { return collectionType; }

  /** Get harvest */
  public boolean isHarvest() { return harvest; }

  /**
   * Get the id of the dataset, or null.
   */
  public String getID() { return id; }

  /**
   * If this dataset has an authority and an ID, then the concatenation of them is the
   * globally unique ID.
   * @return globally unique ID, or null if missing authority or ID.
   */
  public String getUniqueID() {
    String authority = getAuthority();
    if ((authority != null) && (getID() != null))
      return authority + ":"+getID();
    else if (getID() != null)
      return getID();
    else
      return null;
  }

  /** Get authority for this Dataset, may be null. */
  public String getAuthority() {return authorityName; }

  /**
   * Get the DataType (which may be inherited from parent), or null .
   */
  public thredds.catalog.DataType getDataType(){
    return dataType;
  }

  /**
   * Get the DataFormatType (which may be inherited from parent), or null .
   */
  public thredds.catalog.DataFormatType getDataFormatType(){
    return dataFormatType;
  }

  /** If this dataset has access elements.
   * @return true if has access elements.
   */
  public boolean hasAccess() { return !access.isEmpty(); }

  /**
   * Get all access elements for this dataset.
   * This list will expand any compound services.
   * @return List of InvAccess objects. List may not be null, may be empty.
   */
  public java.util.List getAccess() { return access; }

  /**
   * Get access element of the specified service type for this dataset.
   * If more than one, get the first one.
   * @return InvAccess or null if there is not one.
   */
  public InvAccess getAccess( thredds.catalog.ServiceType type) {
    java.util.List alist = getAccess();
    for (int i=0; i<alist.size(); i++) {
      InvAccess m = (InvAccess) alist.get(i);
      InvService s = m.getService();
      if (s.getServiceType() == type)
        return m;
    }
    return null;
  }

  /**
   * Get access element that matches the given access standard URL.
   * @return InvAccess or null if no match.
   */
  public InvAccess findAccess( String accessURL) {
    java.util.List alist = getAccess();
    for (int i=0; i<alist.size(); i++) {
      InvAccess a = (InvAccess) alist.get(i);
      if (accessURL.equals(  a.getStandardUrlName()))
        return a;
    }
    return null;
  }

  /** Return the query fragment referencing this dataset, ie "catalog=catalog.xml&dataset=datasetID" */
  public String getSubsetUrl() {
    if (getID() == null) return null;
    return "catalog="+getParentCatalog().baseURI.toString()+"&dataset="+getID();
  }

  /** If this dataset has nested datasets.
   * @return true if has nested datasets.
   */
  public boolean hasNestedDatasets() { return !getDatasets().isEmpty(); }

  /** Get a list of all the nested datasets.
   *  @return list of objects of type InvDataset. May be empty, not null.
   */
  public java.util.List getDatasets() { return datasets; }

  /**
   * Find an immediate child dataset by its name.
   * @return dataset if found or null if not exist.
   */
  public InvDatasetImpl findDatasetByName(String name) {
    java.util.List dlist = getDatasets();
    for (int i=0; i<dlist.size(); i++) {
      InvDatasetImpl ds = (InvDatasetImpl) dlist.get(i);
      if (ds.getName().equals(name))
        return ds;
    }
    return null;
  }

  /** Get parent dataset.
   *  @return parent dataset. If top dataset, return null.
   */
  public InvDataset getParent() { return parent; }

  /** Get containing catalog.
   *  @return containing catalog.
   */
  public InvCatalog getParentCatalog() {
    if (catalog != null) return catalog;
    return (parent != null) ? parent.getParentCatalog() : null;
  }

  /** Get URL to this dataset. Dataset must have an ID.
   *  Form is catalogURL#DatasetID
   *  @return URL to this dataset.
   */
  public String getCatalogUrl() {
    return getParentCatalog().getUriString()+"#"+getID();
  }

  /** Get list of documentation elements for this dataset.
   *  @return list of InvDocumentation objects. May be empty, not null.
   */
  public java.util.List getDocumentation() { return docs; }

  /**
   * Get all properties for this dataset. These may have been specified
   * in the dataset or an enclosing parent element.
   * @return List of type InvProperty. May be empty, not null.
   */
  public java.util.List getProperties() { return properties; }

  /**
   * Find named property. This may have been specified
   * in the dataset or an enclosing parent element.
   * @return string value of property or null if not exist.
   */
  public String findProperty(String name) {
    InvProperty result = null;
    java.util.List plist = getProperties();
    for (int i=0; i<plist.size(); i++) {
      InvProperty p = (InvProperty) plist.get(i);
      if (p.getName().equals( name))
        result = p;
    }
    return (result == null) ? null : result.getValue();
  }

  /**
   * Get the metadata elements for this InvDataset.
   * @return List of InvMetadata objects. List may be empty but not null.
   */
  public java.util.List getMetadata() { return metadata; }

  /**
   * Get the metadata elements of the specified type.
   * @return List of InvMetadata objects. List may be empty but not null.
   */
  public java.util.List getMetadata( thredds.catalog.MetadataType want) {
    ArrayList result = new ArrayList();
    java.util.List mlist = getMetadata();
    for (int i=0; i<mlist.size(); i++) {
      InvMetadata m = (InvMetadata) mlist.get(i);
      MetadataType mtype = MetadataType.getType( m.getMetadataType());
      if (mtype == want)
        result.add(m);
    }
    return result;
  }

  /**
   * Find the named service declared in this dataset or one of its parents.
   * @return first service that matches the given name, or null if none found.
   */
  public InvService findService(String name) {
    if (name == null)  return null;

    // search local (but expanded) services
    for (int i=0; i<services.size(); i++) {
      InvService p = (InvService) services.get(i);
      if (p.getName().equals( name))
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
   */
  public InvService getServiceDefault() {
    return defaultService;
  }

  /**
   * Return the resource control value which indicates that only users with
   * proper permission can access this resource.
   *
   * ??? Not sure if the value indicates anything or just set or not set.
   *
   * @return the resource control value for this dataset (inherited from ancestor datasets).
   */
  public String getResourceControl() {
    if (resourceControl != null) return resourceControl;
    // not found, look in parent
    if (parent != null)
      return parent.getResourceControl();
    
    return null;
  }

  // 1.0 metadata
  /** get List of type ThreddsMetadata.Source */
  public List getCreators() { return creators; }
  /** get List of type ThreddsMetadata.Contributors */
  public List getContributors() { return contributors; }
  /** get List of type DateType */
  public List getDates() { return dates; }
  /** get List of type ThreddsMetadata.Vocab */
  public List getKeywords() { return keywords; }
  /** get List of type ThreddsMetadata.Vocab */
  public List getProjects() { return projects; }
  /** get List of type ThreddsMetadata.Source */
  public List getPublishers() { return publishers; }
    /** get specific type of documentation = history */
  public String getHistory() { return getDocumentation("history"); }
  /** get specific type of documentation = processing_level */
  public String getProcessing() { return getDocumentation("processing_level"); }
  /** get specific type of documentation = rights */
  public String getRights() { return getDocumentation("rights"); }
  /** get specific type of documentation = summary */
  public String getSummary() { return getDocumentation("summary"); }
  /** get List of type ThreddsMetadata.Variables */
  public List getVariables() { return variables; }

  /** get Variables from the specified vocabulary */
  public ThreddsMetadata.Variables getVariables(String vocab) {
    ThreddsMetadata.Variables result = new ThreddsMetadata.Variables(vocab, null, null, null, null);
    if (variables == null) return result;

    for (int i=0; i<variables.size(); i++) {
      ThreddsMetadata.Variables vs = (ThreddsMetadata.Variables) variables.get(i);
      if (vs.getVocabulary().equals(vocab))
        result.getVariableList().addAll( vs.getVariableList());
    }
    return result;
  }

  /** get geospatial coverage, or null if none */
  public ThreddsMetadata.GeospatialCoverage getGeospatialCoverage() { return gc; }

  /** get time coverage, or null if none */
  public DateRange getTimeCoverage( ) { return tc; }

  /** get specific type of documentation */
  public String getDocumentation(String type) {
    java.util.List docs = getDocumentation();
    for (int i=0; i<docs.size(); i++) {
      InvDocumentation doc = (InvDocumentation) docs.get(i);
      String dtype = doc.getType();
      if ((dtype != null) && dtype.equalsIgnoreCase(type)) return doc.getInlineContent();
    }
    return null;
  }

}
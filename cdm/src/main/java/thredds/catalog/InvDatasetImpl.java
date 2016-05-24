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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDuration;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.unidata.util.Format;
import ucar.unidata.util.StringUtil2;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Concrete implementation of a thredds Dataset, for reading and writing from XML.
 *
 * @author john caron
 * @see InvDataset
 */

/*
  Notes on inheritence implementation.
  Version 1.0
    "Local metadata" is the metadata contained in the dataset node itself.
    Inheritable metadata must be placed in an InvMetadata object with inherit = true;

    "Inherited metadata" is not local, but comes from an ancestor node.
    "Public metadata" is all the dataset's metadata, the local and the inherited.
    When finish() is called, the public metadata is constructed.

    If metadata cannot be inherited, it can be safely stored in the InvDataset objec. Otherwise, it must be kept in
    local or inheritable ThreddsMetadata.
*/
public class InvDatasetImpl extends InvDataset {
  static private final Logger logger = LoggerFactory.getLogger(InvDatasetImpl.class);

  private String urlPath;
  private String alias;
  private double size = 0.0;

  private List<InvAccess> accessLocal = new ArrayList<>();
  private List<InvService> servicesLocal = new ArrayList<>();
  protected ThreddsMetadata tm = new ThreddsMetadata(false); // all local metadata kept here. This may include inheritable InvMetadata
  protected ThreddsMetadata tmi = new ThreddsMetadata(true); // local inheritable metadata (canonicalization)
  protected org.jdom2.Element ncmlElement;

  // validation
  protected StringBuilder log = new StringBuilder();
  // filter
  protected boolean mark = false;

  /**
   * Constructor from Catalog XML info. You must call finish() before this object
   * is ready to be used. We had to do it this way so that nested service elements could be added
   * through addService(), before we define on the default service.
   *
   * @param parent      : parent dataset
   * @param name        : display name of dataset
   * @param dataType    : DataType name (may be null)
   * @param serviceName : default service (may be null)
   * @param urlPath     : URL = server.getURLbase() + urlPath
   */
  public InvDatasetImpl(InvDatasetImpl parent, String name, FeatureType dataType, String serviceName, String urlPath) {
    super(parent, name);

    tm.setDataType(dataType);
    tm.setServiceName(serviceName);
    this.urlPath = urlPath;
  }

  /**
   * Finish constructing after all elements have been added.
   * This does the inheritence thing
   * This can be called again if new elements are added.
   *
   * @return true if successful.
   */
  public boolean finish() {
    boolean ok = true;
    java.util.Iterator iter;
    logger.debug("Now finish " + getName() + " id= " + getID());

    authorityName = null;
    dataType = null;
    dataFormatType = null;
    defaultService = null;

    gc = null;
    tc = null;
    docs = new ArrayList<>();
    metadata = new ArrayList<>();
    properties = new ArrayList<>();

    creators = new ArrayList<>();
    contributors = new ArrayList<>();
    dates = new ArrayList<>();
    keywords = new ArrayList<>();
    projects = new ArrayList<>();
    publishers = new ArrayList<>();
    variables = new ArrayList<>();

    canonicalize(); // canonicalize thredds metadata
    transfer2PublicMetadata(tm, true); // add local metadata
    transfer2PublicMetadata(tmi, true); // add local inherited metadata
    transferInheritable2PublicMetadata((InvDatasetImpl) getParent()); // add inheritable metadata from parents

    // build the expanded access list
    access = new ArrayList<>();

    // add access element if urlPath is specified
    if ((urlPath != null) && (getServiceDefault() != null)) {
      InvAccessImpl a = new InvAccessImpl(this, urlPath, getServiceDefault());
      a.setSize(size);
      a.finish();
      addExpandedAccess(a);
    }

    // add local access elements
    iter = accessLocal.iterator();
    while (iter.hasNext()) {
      InvAccessImpl a = (InvAccessImpl) iter.next();
      a.finish();
      addExpandedAccess(a);
    }

    // recurse into child datasets.
    if (!(this instanceof InvCatalogRef)) {
      for (InvDataset invDataset : this.getDatasets()) {
        InvDatasetImpl curDs = (InvDatasetImpl) invDataset;
        ok &= curDs.finish();
      }
    }

    return ok;
  }

  /**
   * Look for InvMetadata elements in the parent that need to be added to the public metadata of this dataset.
   * Recurse up through all ancestors.
   *
   * @param parent transfer from here
   */
  private void transferInheritable2PublicMetadata(InvDatasetImpl parent) {
    if (parent == null) return;
    logger.debug(" inheritFromParent= " + parent.getID());

    transfer2PublicMetadata(parent.getLocalMetadataInheritable(), true);
    //transfer2PublicMetadata(parent.getCat6Metadata(), true);

    /* look through local metadata, find inherited InvMetadata elements
    ThreddsMetadata tmd = parent.getLocalMetadata();
    Iterator iter = tmd.getMetadata().iterator();
    while (iter.hasNext()) {
      InvMetadata meta = (InvMetadata) iter.next();
      if (meta.isInherited()) {
        if (!meta.isThreddsMetadata()) {
          metadata.add(meta);
        } else {
          if (debugInherit) System.out.println("  inheritMetadata Element " + tmd.isInherited() + " " + meta.isInherited());
          meta.finish(); // make sure XLink is read in.
          transfer2PublicMetadata(meta.getThreddsMetadata(), false);
        }
      }
    } */

    // recurse
    transferInheritable2PublicMetadata((InvDatasetImpl) parent.getParent());
  }

  /**
   * take all elements from tmd and add to the public metadata of this dataset.
   * for InvMetadata elements, only add if inheritAll || InvMetadata.isInherited().
   *
   * @param tmd        tahe metadata from here
   * @param inheritAll true if all should be inherited, else only those which are specifically mareked
   */
  private void transfer2PublicMetadata(ThreddsMetadata tmd, boolean inheritAll) {
    if (tmd == null) return;

    logger.debug("  transferMetadata " + tmd);

    if (authorityName == null)
      authorityName = tmd.getAuthority();
    if (dataType == null || (dataType == FeatureType.ANY) || (dataType == FeatureType.NONE))
      dataType = tmd.getDataType();
    if (dataFormatType == null || dataFormatType == DataFormatType.NONE)
      dataFormatType = tmd.getDataFormatType();

    if (defaultService == null)
      defaultService = findService(tmd.getServiceName());

    if (gc == null) {
      ThreddsMetadata.GeospatialCoverage tgc = tmd.getGeospatialCoverage();
      if ((tgc != null) && !tgc.isEmpty())
        gc = tgc;
    }

    if (tc == null) {
      DateRange ttc = tmd.getTimeCoverage();
      if (ttc != null) {
        tc = ttc;
      }
    }

    if (tc == null)
      tc = tmd.getTimeCoverage();

    for (InvProperty item : tmd.getProperties()) {
      logger.debug("  add Property " + item + " to " + getID());
      properties.add(item);
    }

    creators.addAll(tmd.getCreators());
    contributors.addAll(tmd.getContributors());
    dates.addAll(tmd.getDates());
    docs.addAll(tmd.getDocumentation());
    keywords.addAll(tmd.getKeywords());
    projects.addAll(tmd.getProjects());
    publishers.addAll(tmd.getPublishers());
    variables.addAll(tmd.getVariables());
    if (variableMapLink == null)
      variableMapLink = tmd.variableMapLink;

    for (InvMetadata meta : tmd.getMetadata()) {
      if (meta.isInherited() || inheritAll) {
        if (!meta.isThreddsMetadata()) {
          metadata.add(meta);
        } else {
          logger.debug("  add metadata Element " + tmd.isInherited() + " " + meta);
          meta.finish(); // make sure XLink is read in.
          transfer2PublicMetadata(meta.getThreddsMetadata(), inheritAll);
          metadata.add(meta);
        }
      }
    }

  }


  /**
   * Transfer all inheritable metadata from fromDs to the local metadata of this dataset.
   * Called by InvDatasetScan to transfer inheritable metaddata to the nested catalogRef
   *
   * @param fromDs transfer from here
   */
  public void transferMetadata(InvDatasetImpl fromDs, boolean copyInheritedMetadataFromParents) {
    if (fromDs == null) return;
    logger.debug(" transferMetadata= " + fromDs.getName());

    if (this != fromDs)
      getLocalMetadata().add(fromDs.getLocalMetadata(), false);

    transferInheritableMetadata(fromDs, getLocalMetadataInheritable(), copyInheritedMetadataFromParents);

    setResourceControl(fromDs.getRestrictAccess());
  }

  /**
   * transfer inherited metadata, consolidating it into target
   *
   * @param fromDs transfer from here, plus its parents
   * @param target transfer to here
   */
  private void transferInheritableMetadata(InvDatasetImpl fromDs, ThreddsMetadata target,
                                           boolean copyInheritedMetadataFromParents) {
    if (fromDs == null) return;
    logger.debug(" transferInheritedMetadata= " + fromDs.getName());

    target.add(fromDs.getLocalMetadataInheritable(), true);

    /* look through local metadata, find inherited InvMetadata elements
    ThreddsMetadata tmd = fromDs.getLocalMetadata();
    Iterator iter = tmd.getMetadata().iterator();
    while (iter.hasNext()) {
      InvMetadata meta = (InvMetadata) iter.next();
      if (meta.isInherited()) {
        if (!meta.isThreddsMetadata()) {
          tmc.addMetadata( meta);
        } else {
          logger.debug("  transferInheritedMetadata "+meta.hashCode()+" = "+meta);
          meta.finish(); // LOOK ?? make sure XLink is read in.
          tmc.add( meta.getThreddsMetadata(), true);
        }
      }
    }   */

    // now do the same for the parents
    if (copyInheritedMetadataFromParents)
      transferInheritableMetadata((InvDatasetImpl) fromDs.getParent(), target, true);
  }


  private void addExpandedAccess(InvAccessImpl a) {
    InvService service = a.getService();
    if (null == service) {
      a.check(log, false); // illegal; get error message
      return;
    }

    if (service.getServiceType() == ServiceType.COMPOUND) {
      // if its a compound service, expand it
      for (InvService nestedService : service.getServices()) {
        InvAccessImpl nestedAccess = new InvAccessImpl(this, a.getUrlPath(), nestedService);
        addExpandedAccess(nestedAccess); // i guess it could recurse
      }
    } else {
      access.add(a);
    }
  }

  /**
   * Put metadata into canonical form.
   * All non-inherited thredds metadata put into single metadata element, pointed to by getLocalMetadata().
   * All inherited thredds metadata put into single metadata element, pointed to by getLocalMetadataInherited().
   * This is needed to do reliable editing.
   */
  protected void canonicalize() {
    List<InvMetadata> whatsLeft = new ArrayList<>();
    List<InvMetadata> original = new ArrayList<>(tm.metadata); // get copy of metadata
    tm.metadata = new ArrayList<>();

    // transfer all non-inherited thredds metadata to tm
    // transfer all inherited thredds metadata to tmi
    for (InvMetadata m : original) {
      if (m.isThreddsMetadata() && !m.isInherited() && !m.hasXlink()) {
        ThreddsMetadata nested = m.getThreddsMetadata();
        tm.add(nested, false);

      } else if (m.isThreddsMetadata() && m.isInherited() && !m.hasXlink()) {
        ThreddsMetadata nested = m.getThreddsMetadata();
        tmi.add(nested, true);

      } else {
        whatsLeft.add(m);
      }
    }

    // non ThreddsMetadata goes into tm
    tm.metadata.addAll(whatsLeft);
  }

  /**
   * Construct an InvDatasetImpl which refers to a urlPath.
   * This is used to create a standalone InvDatasetImpl, outside of an InvCatalog.
   * An "anonymous" InvServerImpl is created and attached to the InvDataset.
   *
   * @param urlPath  : construct URL from this path
   * @param dataType : data type
   * @param stype    : ServiceType
   */
  public InvDatasetImpl(String urlPath, FeatureType dataType, ServiceType stype) {
    super(null, "local file");
    tm.setDataType(dataType);
    tm.setServiceName("anon");
    this.urlPath = urlPath;

    // create anonymous service
    addService(new InvService(tm.getServiceName(), stype.toString(), "", "", null));

    finish();
  }

  public InvDatasetImpl(InvDataset parent, String name) {
    super(parent, name);
  }

  /**
   * copy constructor
   *
   * @param from copy from here
   */
  public InvDatasetImpl(InvDatasetImpl from) {
    super(from.getParent(), from.getName());

    // steal everything
    this.tm = new ThreddsMetadata(from.getLocalMetadata());
    this.tmi = new ThreddsMetadata(from.getLocalMetadataInheritable());
    this.accessLocal = new ArrayList<>(from.getAccessLocal());
    this.servicesLocal = new ArrayList<>(from.getServicesLocal());

    this.harvest = from.harvest;
    this.collectionType = from.collectionType;
  }

  ////////////////////////////////////////////////////////
  // get/set local properties

  /**
   * @return alias for this Dataset, if there is one
   */
  public String getAlias() {
    return alias;
  }

  /**
   * Set alias for this Dataset
   *
   * @param alias ID of another Dataset
   */
  public void setAlias(String alias) {
    this.alias = alias;
    hashCode = 0;
  }

  /**
   * Set the containing catalog; use only for top level dataset.
   *
   * @param catalog the containing catalog for the top level dataset.
   */
  public void setCatalog(InvCatalog catalog) {
    this.catalog = catalog;
    hashCode = 0;
  }

  /**
   * Get real parent dataset, no proxies
   *
   * @return parent dataset. If top dataset, return null.
   */
  public InvDataset getParentReal() {
    return parent;
  }

  /**
   * Get urlPath for this Dataset
   *
   * @return urlPath for this Dataset
   */
  public String getUrlPath() {
    return urlPath;
  }

  /**
   * Set the urlPath for this InvDatasetImpl
   *
   * @param urlPath the urlPath for this InvDatasetImpl
   */
  public void setUrlPath(String urlPath) {
    this.urlPath = urlPath;
    hashCode = 0;
  }

  /**
   * Set authorityName for this Dataset
   *
   * @param authorityName for this Dataset
   */
  public void setAuthority(String authorityName) {
    tm.setAuthority(authorityName);
    hashCode = 0;
  }

  ////////////////////////////////////////////////////////
  // setters for public properties (in InvDataset)

  /**
   * Set collectionType
   *
   * @param collectionType the collection type
   */
  public void setCollectionType(CollectionType collectionType) {
    this.collectionType = collectionType;
    hashCode = 0;
  }

  /**
   * Set harvest
   *
   * @param harvest true if this dataset should be harvested for Digital Libraries
   */
  public void setHarvest(boolean harvest) {
    this.harvest = harvest;
    hashCode = 0;
  }

  /**
   * Set the ID for this Dataset
   *
   * @param id unique ID
   */
  public void setID(String id) {
    this.id = id;
    hashCode = 0;
  }

  /**
   * Set name of this Dataset.
   *
   * @param name of the dataset
   */
  public void setName(String name) {
    this.name = name;
    hashCode = 0;
  }

  /**
   * Set the parent dataset.
   *
   * @param parent parent dataset
   */
  public void setParent(InvDatasetImpl parent) {
    this.parent = parent;
    hashCode = 0;
  }

  ////////////////////////////////////////////////////////
  // setters for public properties - these go into the local metadata object
  // these are not inherited, only InvMetadata objects are inheritable.

  // LOOK these are probably wrong

  public void setGeospatialCoverage(ThreddsMetadata.GeospatialCoverage gc) {
    tm.setGeospatialCoverage(gc);
    hashCode = 0;
  }

  public void setTimeCoverage(CalendarDateRange tc) {
    tm.setTimeCoverage(tc);
    hashCode = 0;
  }

  public void setTimeCoverage(DateRange tc) {
    tm.setTimeCoverage(tc);
    hashCode = 0;
  }

  public void setDataFormatType(DataFormatType dataFormatType) {
    tm.setDataFormatType(dataFormatType);
    hashCode = 0;
  }

  public void setDataType(FeatureType dataType) {
    tm.setDataType(dataType);
    hashCode = 0;
  }

  public double getDataSize() {
    return tm.getDataSize();
  }

  public void setDataSize(double dataSize) {
    tm.setDataSize(dataSize);
    hashCode = 0;
  }

  public DateType getLastModifiedDate() {
    // Look for a last modified date.
    for (DateType dateType : tm.getDates()) {
      if ((dateType.getType() != null) && dateType.getType().equals("modified")) {
        return dateType;
      }
    }
    return null;
  }

  public void setLastModifiedDate(DateType lastModDate) {
    if (lastModDate == null)
      throw new IllegalArgumentException("Last modified date can't be null.");
    if (lastModDate.getType() == null || !lastModDate.getType().equals("modified")) {
      throw new IllegalArgumentException("Date type must be \"modified\" (was \"" + lastModDate.getType() + "\").");
    }

    // Check for existing last modified date and remove if one exists.
    DateType curLastModDateType = this.getLastModifiedDate();
    if (curLastModDateType != null) {
      tm.getDates().remove(curLastModDateType);
    }

    // Set the last modified date with the given DateType.
    tm.addDate(lastModDate);
    hashCode = 0;
  }

  public void setLastModifiedDate(Date lastModDate) {
    if (lastModDate == null)
      throw new IllegalArgumentException("Last modified date can't be null.");

    // Set the last modified date with the given Date.
    DateType lastModDateType = new DateType(false, lastModDate);
    lastModDateType.setType("modified");
    setLastModifiedDate(lastModDateType);
  }

  public void setServiceName(String serviceName) {
    tm.setServiceName(serviceName);
    hashCode = 0;
  }

  // LOOK these are wrong
  public void setContributors(List<ThreddsMetadata.Contributor> a) {
    List<ThreddsMetadata.Contributor> dest = tm.getContributors();
    for (ThreddsMetadata.Contributor item : a) {
      if (!dest.contains(item))
        dest.add(item);
    }
    hashCode = 0;
  }

  public void setKeywords(List<ThreddsMetadata.Vocab> a) {
    List<ThreddsMetadata.Vocab> dest = tm.getKeywords();
    for (ThreddsMetadata.Vocab item : a) {
      if (!dest.contains(item))
        dest.add(item);
    }
    hashCode = 0;
  }

  public void setProjects(List<ThreddsMetadata.Vocab> a) {
    List<ThreddsMetadata.Vocab> dest = tm.getProjects();
    for (ThreddsMetadata.Vocab item : a) {
      if (!dest.contains(item))
        dest.add(item);
    }
    hashCode = 0;
  }

  public void setPublishers(List<ThreddsMetadata.Source> a) {
    List<ThreddsMetadata.Source> dest = tm.getPublishers();
    for (ThreddsMetadata.Source item : a) {
      if (!dest.contains(item))
        dest.add(item);
    }
    hashCode = 0;
  }

  public void setResourceControl(String restrictAccess) {
    this.restrictAccess = restrictAccess;
  }

  //////////////////////////////////////////////////////////////////////////////
  // add/remove/get/find local elements

  /**
   * Add InvAccess element to this dataset.
   *
   * @param a add dthis
   */
  public void addAccess(InvAccess a) {
    accessLocal.add(a);
    hashCode = 0;
  }

  /**
   * Add a list of InvAccess elements to this dataset.
   *
   * @param a add all of these
   */
  public void addAccess(List<InvAccess> a) {
    accessLocal.addAll(a);
    hashCode = 0;
  }

  /**
   * @return the local access (non-expanded) elements.
   */
  public java.util.List<InvAccess> getAccessLocal() {
    return accessLocal;
  }

  /**
   * @return the ncml element if it exists, else return null.
   */
  public org.jdom2.Element getNcmlElement() {
    return ncmlElement;
  }

  public void setNcmlElement(org.jdom2.Element ncmlElement) {
    this.ncmlElement = ncmlElement;
  }

  /**
   * Add a nested dataset.
   *
   * @param ds add this
   */
  public void addDataset(InvDatasetImpl ds) {
    if (ds == null) return;
    ds.setParent(this);
    datasets.add(ds);
    hashCode = 0;
  }

  /**
   * Add a nested dataset at the location indicated by index.
   *
   * @param index add at this position
   * @param ds    add this
   */
  public void addDataset(int index, InvDatasetImpl ds) {
    if (ds == null) return;
    ds.setParent(this);
    datasets.add(index, ds);
    hashCode = 0;
  }

  /**
   * Remove the given dataset element from this dataset if it is in the dataset.
   *
   * @param ds - the dataset element to be removed
   * @return true if this dataset contained the given dataset element.
   */
  public boolean removeDataset(InvDatasetImpl ds) {
    if (this.datasets.remove(ds)) {
      ds.setParent(null);
      InvCatalogImpl cat = (InvCatalogImpl) getParentCatalog();
      if (cat != null)
        cat.removeDatasetByID(ds);
      return (true);
    }
    return (false);
  }

  /**
   * Replace the given dataset if it is a nesetd dataset.
   *
   * @param remove - the dataset element to be removed
   * @param add    - the dataset element to be added
   * @return true on success
   */
  public boolean replaceDataset(InvDatasetImpl remove, InvDatasetImpl add) {
    for (int i = 0; i < datasets.size(); i++) {
      InvDataset dataset = datasets.get(i);
      if (dataset.equals(remove)) {
        datasets.set(i, add);
        InvCatalogImpl cat = (InvCatalogImpl) getParentCatalog();
        if (cat != null) {
          cat.removeDatasetByID(remove);
          cat.addDatasetByID(add);
        }
        return true;
      }
    }
    return false;
  }

  /**
   * Add documentation element to this dataset.
   *
   * @param doc add this
   */
  public void addDocumentation(InvDocumentation doc) {
    tm.addDocumentation(doc);
    hashCode = 0;
  }

  /**
   * Add a property to this dataset
   *
   * @param p add this
   */
  public void addProperty(InvProperty p) {
    tm.addProperty(p);
    hashCode = 0;
  }

  /**
   * Add a service to this dataset.
   *
   * @param service add this service to the dataset
   * @deprecated add services only to catalog
   */
  public void addService(InvService service) {
    // System.out.println("--add dataset service= "+service.getName());
    servicesLocal.add(service);
    services.add(service);
    // add nested servers
    for (InvService nested : service.getServices()) {
      services.add(nested);
      // System.out.println("--add expanded service= "+nested.getName());
    }
    hashCode = 0;
  }


  /**
   * Remove a service from this dataset.
   *
   * @param service remove this
   * @deprecated put services in catalog
   */
  public void removeService(InvService service) {
    servicesLocal.remove(service);
    services.remove(service);
    // remove nested servers
    for (InvService nested : service.getServices()) {
      services.remove(nested);
    }
  }

  /**
   * Get services attached specifically to this dataset.
   *
   * @return List of type InvService. May be empty, but not null.
   */
  public java.util.List<InvService> getServicesLocal() {
    return servicesLocal;
  }

  /**
   * Set the list of services attached specifically to this dataset.
   * Discard any previous servies.
   *
   * @param s list of services.
   */
  public void setServicesLocal(java.util.List<InvService> s) {
    this.services = new ArrayList<>();
    this.servicesLocal = new ArrayList<>();

    for (InvService elem : s) {
      addService(elem);
    }
    hashCode = 0;
  }

  /**
   * Get the metadata stored in this dataset element.
   * Inherited metadata only in an InvMetadata object.
   *
   * @return the metadata stored in this dataset element.
   */
  public ThreddsMetadata getLocalMetadata() {
    return tm;
  }

  public void setLocalMetadata(ThreddsMetadata tm) {
    // look this is wrong.
    // need to copy fields into it !!
    // possible only the one that are different from default !!!
    // like stored defaults !! ha ha !
    this.tm = tm;
    hashCode = 0;
  }

  /**
   * local metadata that should be inherited by this dataset's children.
   *
   * @return local metadata that should be inherited by this dataset's children.
   */
  public ThreddsMetadata getLocalMetadataInheritable() {
    return tmi;
  }


  /*
   * local metadata that should be inherited by this dataset's children.
   *
  public ThreddsMetadata getCat6Metadata() {
    return tmi6;
  } */


  /**
   * Remove the given InvMetadata from the set of metadata local to this dataset.
   *
   * @param metadata remove this
   * @return true if an InvMetadata is removed, false otherwise.
   */
  public boolean removeLocalMetadata(InvMetadata metadata) {
    InvDatasetImpl parentDataset = ((InvDatasetImpl) metadata.getParentDataset());
    List localMdata = parentDataset.getLocalMetadata().getMetadata();
    if (localMdata.contains(metadata)) {
      if (localMdata.remove(metadata)) {
        hashCode = 0; // Need to recalculate the hash code.
        return (true);
      }
    }
    return (false);
  }

  public String getServiceName() {
    if (defaultService != null)
      return defaultService.getName();
    return null;
  }

  /*
   * get Documentation that are xlinks
   *
  public List getDocumentationLinks() {
    ArrayList result = new ArrayList();
    java.util.List docs = getDocumentation();
    for (int i = 0; i < docs.size(); i++) {
      InvDocumentation doc = (InvDocumentation) docs.get(i);
      if (doc.hasXlink())
        result.add(doc);
    }
    return result;
  } */

  /**
   * Filtering
   *
   * @return true if this is "marked"
   */
  protected boolean getMark() {
    return mark;
  }

  protected void setMark(boolean mark) {
    this.mark = mark;
  }

  /**
   * Look up the User property having the given key
   *
   * @param key property key
   * @return User property having the given key, or null
   */
  public Object getUserProperty(Object key) {
    if (userMap == null) return null;
    return userMap.get(key);
  }

  public void setUserProperty(Object key, Object value) {
    if (userMap == null) userMap = new HashMap<>();
    userMap.put(key, value);
  }

  private HashMap<Object, Object> userMap = null;

  public String toString() {
    return getName();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @param buff          a
   * @param ds            a
   * @param complete      a
   * @param isServer      a
   * @param datasetEvents a
   * @param catrefEvents  a
   * @deprecated Instead use {@link #writeHtmlDescription(StringBuilder buff, InvDatasetImpl ds, boolean complete, boolean isServer, boolean datasetEvents, boolean catrefEvents, boolean resolveRelativeUrls)}
   */
  static public void writeHtmlDescription(StringBuilder buff, InvDatasetImpl ds,
                                          boolean complete, boolean isServer,
                                          boolean datasetEvents,
                                          boolean catrefEvents) {
    writeHtmlDescription(buff, ds, complete, isServer, datasetEvents, catrefEvents, true);
  }

  /**
   * Write an Html representation of the given dataset.
   * <p> With datasetEvents, catrefEvents = true, this is used to construct an HTML page on the client
   * (eg using HtmlPage); the client then detects URL clicks and processes.
   * <p> With datasetEvents, catrefEvents = false, this is used to construct an HTML page on the server.
   * (eg using HtmlPage); the client then detects URL clicks and processes.
   *
   * @param buff          put HTML here.
   * @param ds            the dataset.
   * @param complete      if true, add HTML header and ender so its a complete, valid HTML page.
   * @param isServer      if true, then we are in the thredds data server, so do the following: <ul>
   *                      <li> append "html" to DODS Access URLs
   *                      </ul>
   * @param datasetEvents if true, prepend "dataset:" to any dataset access URLS
   * @param catrefEvents  if true, prepend "catref:" to any catref URLS
   */

  static public void writeHtmlDescription(StringBuilder buff, InvDatasetImpl ds,
                                          boolean complete, boolean isServer,
                                          boolean datasetEvents, boolean catrefEvents,
                                          boolean resolveRelativeUrls) {

    if (ds == null) return;

    if (complete) {
      buff.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n")
              .append("        \"http://www.w3.org/TR/html4/loose.dtd\">\n")
              .append("<html>\n");
      buff.append("<head>");
      buff.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
      buff.append("</head>");
      buff.append("<body>\n");
    }

    buff.append("<h2>Dataset: ").append(ds.getFullName()).append("</h2>\n<ul>\n");
    if ((ds.getDataFormatType() != null) && (ds.getDataFormatType() != DataFormatType.NONE))
      buff.append(" <li><em>Data format: </em>").append(StringUtil2.quoteHtmlContent(ds.getDataFormatType().toString())).append("</li>\n");

    if ((ds.getDataSize() != 0.0) && !Double.isNaN(ds.getDataSize()))
      buff.append(" <li><em>Data size: </em>").append(Format.formatByteSize(ds.getDataSize())).append("</li>\n");

    if ((ds.getDataType() != null) && (ds.getDataType() != FeatureType.ANY) && (ds.getDataType() != FeatureType.NONE))
      buff.append(" <li><em>Data type: </em>").append(StringUtil2.quoteHtmlContent(ds.getDataType().toString())).append("</li>\n");

    if ((ds.getCollectionType() != null) && (ds.getCollectionType() != CollectionType.NONE))
      buff.append(" <li><em>Collection type: </em>").append(StringUtil2.quoteHtmlContent(ds.getCollectionType().toString())).append("</li>\n");

    if (ds.isHarvest())
      buff.append(" <li><em>Harvest: </em>").append(ds.isHarvest()).append("</li>\n");

    if (ds.getAuthority() != null)
      buff.append(" <li><em>Naming Authority: </em>").append(StringUtil2.quoteHtmlContent(ds.getAuthority())).append("</li>\n");

    if (ds.getID() != null)
      buff.append(" <li><em>ID: </em>").append(StringUtil2.quoteHtmlContent(ds.getID())).append("</li>\n");

    if (ds.getRestrictAccess() != null)
      buff.append(" <li><em>RestrictAccess: </em>").append(StringUtil2.quoteHtmlContent(ds.getRestrictAccess())).append("</li>\n");

    if (ds instanceof InvCatalogRef) {
      InvCatalogRef catref = (InvCatalogRef) ds;
      String href = resolveRelativeUrls || catrefEvents
              ? resolve(ds, catref.getXlinkHref())
              : catref.getXlinkHref();
      if (catrefEvents) href = "catref:" + href;
      buff.append(" <li><em>CatalogRef: </em>").append(makeHref(href, null)).append("</li>\n");
    }

    buff.append("</ul>\n");

    java.util.List<InvDocumentation> docs = ds.getDocumentation();
    if (docs.size() > 0) {
      buff.append("<h3>Documentation:</h3>\n<ul>\n");
      for (InvDocumentation doc : docs) {
        String type = (doc.getType() == null) ? "" : "<strong>" + StringUtil2.quoteHtmlContent(doc.getType()) + ":</strong> ";
        String inline = doc.getInlineContent();
        if ((inline != null) && (inline.length() > 0))
          buff.append(" <li>").append(type).append(StringUtil2.quoteHtmlContent(inline)).append("</li>\n");
        if (doc.hasXlink()) {
          // buff.append(" <li>" + type + makeHrefResolve(ds, url.toString(), doc.getXlinkTitle()) + "</a>\n");
          buff.append(" <li>").append(type).append(makeHref(doc.getXlinkHref(), doc.getXlinkTitle())).append("</li>\n");
        }
      }
      buff.append("</ul>\n");
    }

    java.util.List<InvAccess> access = ds.getAccess();
    if (access.size() > 0) {
      buff.append("<h3>Access:</h3>\n<ol>\n");
      for (InvAccess a : access) {
        InvService s = a.getService();
        String urlString = resolveRelativeUrls || datasetEvents
                ? a.getStandardUrlName()
                : a.getUnresolvedUrlName();
        String fullUrlString = urlString;
        if (datasetEvents) fullUrlString = "dataset:" + fullUrlString;
        if (isServer) {
          ServiceType stype = s.getServiceType();
          if ((stype == ServiceType.OPENDAP) || (stype == ServiceType.DODS))
            fullUrlString = fullUrlString + ".html";
          else if (stype == ServiceType.DAP4)
            fullUrlString = fullUrlString + ".dmr.xml";
          else if (stype == ServiceType.WCS)
            fullUrlString = fullUrlString + "?service=WCS&version=1.0.0&request=GetCapabilities";
          else if (stype == ServiceType.WMS)
            fullUrlString = fullUrlString + "?service=WMS&version=1.3.0&request=GetCapabilities";
          //NGDC update 8/18/2011
          else if (stype == ServiceType.NCML || stype == ServiceType.UDDC || stype == ServiceType.ISO) {
            String catalogUrl = ds.getCatalogUrl();
        	  String datasetId = ds.id;
            if ( catalogUrl.indexOf('#') > 0)
              catalogUrl = catalogUrl.substring( 0, catalogUrl.lastIndexOf('#'));
            try {
              catalogUrl = URLEncoder.encode( catalogUrl, "UTF-8" );
              datasetId = URLEncoder.encode( datasetId,"UTF-8");
            } catch ( UnsupportedEncodingException e) {
              e.printStackTrace();
            }
            fullUrlString = fullUrlString + "?catalog=" + catalogUrl +  "&dataset=" + datasetId;
          }
          else if (stype == ServiceType.NetcdfSubset)
            fullUrlString = fullUrlString + "/dataset.html";
          else if ((stype == ServiceType.CdmRemote) || (stype == ServiceType.CdmrFeature))
            fullUrlString = fullUrlString + "?req=form";
        }
        buff.append(" <li> <b>").append(StringUtil2.quoteHtmlContent(s.getServiceType().toString()));
        buff.append(":</b> ").append(makeHref(fullUrlString, urlString)).append("</li>\n");
      }
      buff.append("</ol>\n");
    }

    java.util.List<ThreddsMetadata.Contributor> contributors = ds.getContributors();
    if (contributors.size() > 0) {
      buff.append("<h3>Contributors:</h3>\n<ul>\n");
      for (ThreddsMetadata.Contributor t : contributors) {
        String role = (t.getRole() == null) ? "" : "<strong> (" + StringUtil2.quoteHtmlContent(t.getRole()) + ")</strong> ";
        buff.append(" <li>").append(StringUtil2.quoteHtmlContent(t.getName())).append(role).append("</li>\n");
      }
      buff.append("</ul>\n");
    }

    java.util.List<ThreddsMetadata.Vocab> keywords = ds.getKeywords();
    if (keywords.size() > 0) {
      buff.append("<h3>Keywords:</h3>\n<ul>\n");
      for (ThreddsMetadata.Vocab t : keywords) {
        String vocab = (t.getVocabulary() == null) ? "" : " <strong>(" + StringUtil2.quoteHtmlContent(t.getVocabulary()) + ")</strong> ";
        buff.append(" <li>").append(StringUtil2.quoteHtmlContent(t.getText())).append(vocab).append("</li>\n");
      }
      buff.append("</ul>\n");
    }

    java.util.List<DateType> dates = ds.getDates();
    if (dates.size() > 0) {
      buff.append("<h3>Dates:</h3>\n<ul>\n");
      for (DateType d : dates) {
        String type = (d.getType() == null) ? "" : " <strong>(" + StringUtil2.quoteHtmlContent(d.getType()) + ")</strong> ";
        buff.append(" <li>").append(StringUtil2.quoteHtmlContent(d.getText())).append(type).append("</li>\n");
      }
      buff.append("</ul>\n");
    }

    java.util.List<ThreddsMetadata.Vocab> projects = ds.getProjects();
    if (projects.size() > 0) {
      buff.append("<h3>Projects:</h3>\n<ul>\n");
      for (ThreddsMetadata.Vocab t : projects) {
        String vocab = (t.getVocabulary() == null) ? "" : " <strong>(" + StringUtil2.quoteHtmlContent(t.getVocabulary()) + ")</strong> ";
        buff.append(" <li>").append(StringUtil2.quoteHtmlContent(t.getText())).append(vocab).append("</li>\n");
      }
      buff.append("</ul>\n");
    }

    java.util.List<ThreddsMetadata.Source> creators = ds.getCreators();
    if (creators.size() > 0) {
      buff.append("<h3>Creators:</h3>\n<ul>\n");
      for (ThreddsMetadata.Source t : creators) {
        buff.append(" <li><strong>").append(StringUtil2.quoteHtmlContent(t.getName())).append("</strong><ul>\n");
        buff.append(" <li><em>email: </em>").append(StringUtil2.quoteHtmlContent(t.getEmail())).append("</li>\n");
        if (t.getUrl() != null) {
          String newUrl = resolveRelativeUrls
                  ? makeHrefResolve(ds, t.getUrl(), null)
                  : makeHref(t.getUrl(), null);
          buff.append(" <li> <em>").append(newUrl).append("</em></li>\n");
        }
        buff.append(" </ul></li>\n");
      }
      buff.append("</ul>\n");
    }

    java.util.List<ThreddsMetadata.Source> publishers = ds.getPublishers();
    if (publishers.size() > 0) {
      buff.append("<h3>Publishers:</h3>\n<ul>\n");
      for (ThreddsMetadata.Source t : publishers) {
        buff.append(" <li><strong>").append(StringUtil2.quoteHtmlContent(t.getName())).append("</strong><ul>\n");
        buff.append(" <li><em>email: </em>").append(StringUtil2.quoteHtmlContent(t.getEmail())).append("\n");
        if (t.getUrl() != null) {
          String urlLink = resolveRelativeUrls
                  ? makeHrefResolve(ds, t.getUrl(), null)
                  : makeHref(t.getUrl(), null);
          buff.append(" <li> <em>").append(urlLink).append("</em>\n");
        }
        buff.append(" </ul>\n");
      }
      buff.append("</ul>\n");
    }

    /*
    4.2:
    <h3>Variables:</h3>
    <ul>
    <li><em>Vocabulary</em> [DIF]:
    <ul>
     <li><strong>Reflectivity</strong> =  <i></i> = EARTH SCIENCE &gt; Spectral/Engineering &gt; Radar &gt; Radar Reflectivity (db)
     <li><strong>Velocity</strong> =  <i></i> = EARTH SCIENCE &gt; Spectral/Engineering &gt; Radar &gt; Doppler Velocity (m/s)
     <li><strong>SpectrumWidth</strong> =  <i></i> = EARTH SCIENCE &gt; Spectral/Engineering &gt; Radar &gt; Doppler Spectrum Width (m/s)
     </ul>
     </ul>
    </ul>

    4.3:
    <h3>Variables:</h3>
    <ul>
    <li><em>Vocabulary</em> [CF-1.0]:
    <ul>
     <li><strong>d3d (meters) </strong> =  <i>3D Depth at Nodes
    <p>        </i> = depth_at_nodes
     <li><strong>depth (meters) </strong> =  <i>Bathymetry</i> = depth
     <li><strong>eta (m) </strong> =  <i></i> =
     <li><strong>temp (Celsius) </strong> =  <i>Temperature
    <p>        </i> = sea_water_temperature
     <li><strong>u (m/s) </strong> =  <i>Eastward Water
    <p>          Velocity
    <p>        </i> = eastward_sea_water_velocity
     <li><strong>v (m/s) </strong> =  <i>Northward Water
    <p>          Velocity
    <p>        </i> = northward_sea_water_velocity
    </ul>
    </ul>
     */

    java.util.List<ThreddsMetadata.Variables> vars = ds.getVariables();
    if (vars.size() > 0) {
      buff.append("<h3>Variables:</h3>\n<ul>\n");
      for (ThreddsMetadata.Variables t : vars) {

        buff.append("<li><em>Vocabulary</em> [");
        if (t.getVocabUri() != null) {
          URI uri = t.getVocabUri();
          String vocabLink = resolveRelativeUrls
                  ? makeHrefResolve(ds, uri.toString(), t.getVocabulary())
                  : makeHref(uri.toString(), t.getVocabulary());
          buff.append(vocabLink);
        } else {
          buff.append(StringUtil2.quoteHtmlContent(t.getVocabulary()));
        }
        buff.append("]:\n<ul>\n");

        java.util.List<ThreddsMetadata.Variable> vlist = t.getVariableList();
        if (vlist.size() > 0) {
          for (ThreddsMetadata.Variable v : vlist) {
            String units = (v.getUnits() == null || v.getUnits().length() == 0) ? "" : " (" + v.getUnits() + ") ";
            buff.append(" <li><strong>").append(StringUtil2.quoteHtmlContent(v.getName() + units)).append("</strong> = ");
            String desc = (v.getDescription() == null) ? "" : " <i>" + StringUtil2.quoteHtmlContent(v.getDescription()) + "</i> = ";
            buff.append(desc);
            if (v.getVocabularyName() != null)
              buff.append(StringUtil2.quoteHtmlContent(v.getVocabularyName()));
            buff.append("\n");
          }
        }
        buff.append("</ul>\n");
      }
      buff.append("</ul>\n");
    }
    if (ds.getVariableMapLink() != null) {
      buff.append("<h3>Variables:</h3>\n");
      buff.append("<ul><li>"+makeHref(ds.getVariableMapLink(), "VariableMap")+"</li></ul>\n");
    }

    ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
    if ((gc != null) && !gc.isEmpty()) {
      buff.append("<h3>GeospatialCoverage:</h3>\n<ul>\n");
      if (gc.isGlobal())
        buff.append(" <li><em> Global </em>\n");

      buff.append(" <li><em> Longitude: </em> ").append(rangeString(gc.getEastWestRange())).append("</li>\n");
      buff.append(" <li><em> Latitude: </em> ").append(rangeString(gc.getNorthSouthRange())).append("</li>\n");
      if (gc.getUpDownRange() != null) {
        buff.append(" <li><em> Altitude: </em> ").append(rangeString(gc.getUpDownRange())).append(" (positive is <strong>").append(StringUtil2.quoteHtmlContent(gc.getZPositive())).append(")</strong></li>\n");
      }

      java.util.List<ThreddsMetadata.Vocab> nlist = gc.getNames();
      if ((nlist != null) && (nlist.size() > 0)) {
        buff.append(" <li><em>  Names: </em> <ul>\n");
        for (ThreddsMetadata.Vocab elem : nlist) {
          buff.append(" <li>").append(StringUtil2.quoteHtmlContent(elem.getText())).append("\n");
        }
        buff.append(" </ul>\n");
      }
      buff.append(" </ul>\n");
    }

    CalendarDateRange tc = ds.getCalendarDateCoverage();
    if (tc != null) {
      buff.append("<h3>TimeCoverage:</h3>\n<ul>\n");
      CalendarDate start = tc.getStart();
      if (start != null)
        buff.append(" <li><em>  Start: </em> ").append(start.toString()).append("\n");
      CalendarDate end = tc.getEnd();
      if (end != null) {
        buff.append(" <li><em>  End: </em> ").append(end.toString()).append("\n");
      }
      CalendarDuration duration = tc.getDuration();
      if (duration != null)
        buff.append(" <li><em>  Duration: </em> ").append(StringUtil2.quoteHtmlContent(duration.toString())).append("\n");
      CalendarDuration resolution = tc.getResolution();
      if (resolution != null) {
        buff.append(" <li><em>  Resolution: </em> ").append(StringUtil2.quoteHtmlContent(resolution.toString())).append("\n");
      }
      buff.append(" </ul>\n");
    }

    java.util.List<InvMetadata> metadata = ds.getMetadata();
    boolean gotSomeMetadata = false;
    for (InvMetadata m : metadata) {
      if (m.hasXlink()) gotSomeMetadata = true;
    }

    if (gotSomeMetadata) {
      buff.append("<h3>Metadata:</h3>\n<ul>\n");
      for (InvMetadata m : metadata) {
        String type = (m.getMetadataType() == null) ? "" : m.getMetadataType();
        if (m.hasXlink()) {
          String title = (m.getXlinkTitle() == null) ? "Type " + type : m.getXlinkTitle();
          String mdLink = resolveRelativeUrls
                  ? makeHrefResolve(ds, m.getXlinkHref(), title)
                  : makeHref(m.getXlinkHref(), title);
          buff.append(" <li> ").append(mdLink).append("\n");
        } //else {
        //buff.append(" <li> <pre>"+m.getMetadataType()+" "+m.getContentObject()+"</pre>\n");
        //}
      }
      buff.append("</ul>\n");
    }

    java.util.List<InvProperty> propsOrg = ds.getProperties();
    java.util.List<InvProperty> props = new ArrayList<>(ds.getProperties().size());
    for (InvProperty p : propsOrg)  {
      if (!p.getName().startsWith("viewer"))  // eliminate the viewer properties from the html view
        props.add(p);
    }
    if (props.size() > 0) {
      buff.append("<h3>Properties:</h3>\n<ul>\n");
      for (InvProperty p : props) {
        if (p.getName().equals("attachments")) // LOOK whats this ?
        {
          String attachLink = resolveRelativeUrls
                  ? makeHrefResolve(ds, p.getValue(), p.getName())
                  : makeHref(p.getValue(), p.getName());
          buff.append(" <li>").append(attachLink).append("\n");
        } else {
          buff.append(" <li>").append(StringUtil2.quoteHtmlContent(p.getName() + " = \"" + p.getValue())).append("\"\n");
        }
      }
      buff.append("</ul>\n");
    }

    if (complete) buff.append("</body></html>");
  }

  static private String rangeString(ThreddsMetadata.Range r) {
    if (r == null) return "";
    String units = (r.getUnits() == null) ? "" : " " + r.getUnits();
    String resolution = r.hasResolution() ? " Resolution=" + r.getResolution() : "";
    return StringUtil2.quoteHtmlContent(r.getStart() + " to " + (r.getStart() + r.getSize()) + resolution + units);
  }

  /**
   * resolve reletive URLS against the catalog URL.
   *
   * @param ds   use ds parent catalog, if it exists
   * @param href URL to resolve
   * @return resolved URL
   */
  static public String resolve(InvDataset ds, String href) {
    InvCatalog cat = ds.getParentCatalog();
    if (cat != null) {
      try {
        java.net.URI uri = cat.resolveUri(href);
        href = uri.toString();
      } catch (java.net.URISyntaxException e) {
        logger.warn("InvDatasetImpl.writeHtml: error parsing URL= " + href);
      }
    }
    return href;
  }

  static private String makeHref(String href, String title) {
    if (title == null) title = href;
    return "<a href='" + StringUtil2.quoteHtmlContent(href) + "'>" + StringUtil2.quoteHtmlContent(title) + "</a>";
  }

  static private String makeHrefResolve(InvDatasetImpl ds, String href, String title) {
    if (title == null) title = href;
    href = resolve(ds, href);
    return makeHref(href, title);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @return debugging info
   */
  public String dump() {
    return dump(0);
  }

  String dump(int n) {
    StringBuilder buff = new StringBuilder(100);

    buff.append(indent(n));
    buff.append("Dataset name:<").append(getName());
    if (dataType != null) {
      buff.append("> dataType:<").append(dataType);
    }
    if (urlPath != null)
      buff.append("> urlPath:<").append(urlPath);
    if (defaultService != null)
      buff.append("> defaultService <").append(defaultService);
    buff.append("> uID:<").append(getUniqueID());
    buff.append(">\n");

    List<InvService> svcs = getServicesLocal();
    if (svcs.size() > 0) {
      String indent = indent(n + 2);
      buff.append(indent);
      buff.append("Services:\n");
      for (InvService s : svcs) {
        buff.append(s.dump(n + 4));
      }
    }

    if (access.size() > 0) {
      String indent = indent(n + 2);
      buff.append(indent);
      if (access.size() == 1) {
        buff.append("Access: ").append(access.get(0)).append("\n");
      } else if (access.size() > 1) {
        buff.append("Access:\n");
        for (InvAccess a : access) {
          buff.append(indent(n + 4)).append(a).append("\n");
        }
      }
    }

    buff.append(indent(n)).append("Thredds Metadata\n");
    buff.append(tm.dump(n + 4)).append("\n");
    buff.append(indent(n)).append("Thredds Metadata Inherited\n");
    buff.append(tmi.dump(n + 4)).append("\n");
    //buff.append(indent(n)).append("Thredds Metadata Cat6\n");
    //buff.append(tmi6.dump(n + 4)).append("\n");

    if (datasets.size() > 0) {
      String indent = indent(n + 2);
      buff.append(indent);
      buff.append("Datasets:\n");
      for (InvDataset ds : datasets) {
        InvDatasetImpl dsi = (InvDatasetImpl) ds;
        buff.append(dsi.dump(n + 4));
      }
    }
    return buff.toString();
  }

  static String indent(int n) {
    StringBuilder blanks = new StringBuilder(n);
    for (int i = 0; i < n; i++)
      blanks.append(" ");
    return blanks.toString();
  }

  boolean check(StringBuilder out, boolean show) {
    boolean isValid = true;

    if (log.length() > 0) {
      out.append(log);
    }

    /* check that the serviceName is valid
    if (serviceName != null) {
      if (null == findService(serviceName)) {
        out.append("**Dataset ("+getFullName()+"): has unknown service named ("+serviceName+")\n");
        isValid = false;
      }
    } */

    for (InvAccess acces : access) {
      InvAccessImpl a = (InvAccessImpl) acces;
      isValid &= a.check(out, show);
    }

    for (InvDataset dataset : datasets) {
      InvDatasetImpl ds = (InvDatasetImpl) dataset;
      isValid &= ds.check(out, show);
    }

    for (InvMetadata m : getMetadata()) {
      m.check(out);
    }

    for (InvService s : getServicesLocal()) {
      isValid &= s.check(out);
    }

    if (hasAccess() && (getDataType() == null)) {
      out.append("**Warning: Dataset (").append(getFullName()).append("): is selectable but no data type declared in it or in a parent element\n");
    }

    if (!hasAccess() && !hasNestedDatasets()) {
      out.append("**Warning: Dataset (").append(getFullName()).append("): is not selectable and does not have nested datasets\n");
    }

    if (show) System.out.println("  dataset " + name + " valid = " + isValid);

    return isValid;
  }

  /**
   * InvDatasetImpl elements with same values are equal.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InvDatasetImpl)) return false;
    return o.hashCode() == this.hashCode();
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      result = 37 * result + getName().hashCode();
      result = 37 * result + getServicesLocal().hashCode();
      result = 37 * result + getDatasets().hashCode();
      result = 37 * result + getAccessLocal().hashCode();
      result = 37 * result + (isHarvest() ? 1 : 0);

      if (null != getCollectionType())
        result = 37 * result + getCollectionType().hashCode();

      result = 37 * result + getDocumentation().hashCode();
      result = 37 * result + getProperties().hashCode();
      result = 37 * result + getMetadata().hashCode();

      result = 37 * result + getCreators().hashCode();
      result = 37 * result + getContributors().hashCode();
      result = 37 * result + getDates().hashCode();
      result = 37 * result + getKeywords().hashCode();
      result = 37 * result + getProjects().hashCode();
      result = 37 * result + getPublishers().hashCode();
      result = 37 * result + getVariables().hashCode();

      if (null != getID())
        result = 37 * result + getID().hashCode();
      if (null != getAlias())
        result = 37 * result + getAlias().hashCode();
      if (null != getAuthority())
        result = 37 * result + getAuthority().hashCode();
      if (null != getDataType())
        result = 37 * result + getDataType().hashCode();
      if (null != getDataFormatType())
        result = 37 * result + getDataFormatType().hashCode();
      if (null != getServiceDefault())
        result = 37 * result + getServiceDefault().hashCode();
      if (null != getUrlPath())
        result = 37 * result + getUrlPath().hashCode();

      if (null != getGeospatialCoverage())
        result = 37 * result + getGeospatialCoverage().hashCode();

      if (null != getCalendarDateCoverage())
        result = 37 * result + getCalendarDateCoverage().hashCode(); // */

      hashCode = result;
    }
    return hashCode;
  }

  private volatile int hashCode = 0; // Bloch, item 8 - lazily initialize hash value

}

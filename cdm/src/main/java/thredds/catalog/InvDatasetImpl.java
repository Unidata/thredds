// $Id: InvDatasetImpl.java 48 2006-07-12 16:15:40Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

import thredds.datatype.*;

import java.util.*;
import java.net.URI;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Format;

/**
 * Concrete implementation of a thredds Dataset, for reading and writing from XML.
 *
 * @author john caron
 * @version $Revision: 48 $ $Date: 2006-07-12 16:15:40Z $
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

  private String urlPath;
  private String alias;
  private double size = 0.0;

  private ArrayList accessLocal = new ArrayList();
  private ArrayList servicesLocal = new ArrayList();
  protected ThreddsMetadata tm = new ThreddsMetadata(false); // all local metadata kept here. This may include
                                                             // inheritable InvMetadata
  protected ThreddsMetadata tmi = new ThreddsMetadata(true); // local inheritable metadata (canonicalization)
  protected ThreddsMetadata tmi6 = new ThreddsMetadata(true); // local catalog 0.6 inheritable metadata
  protected org.jdom.Element ncmlElement;

  // validation
  protected StringBuffer log = new StringBuffer();
  // filter
  protected boolean mark = false;

  // debug
  private boolean debugInherit = false, debugInherit2 = false;

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
  public InvDatasetImpl(InvDatasetImpl parent, String name, DataType dataType, String serviceName, String urlPath) {
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
    if (debugInherit) System.out.println("Now finish " + getName() + " id= " + getID());

    authorityName = null;
    dataType = null;
    dataFormatType = null;
    defaultService = null;

    gc = null;
    tc = null;
    docs = new ArrayList();
    metadata = new ArrayList();
    properties = new ArrayList();

    creators = new ArrayList();
    contributors = new ArrayList();
    dates = new ArrayList();
    keywords = new ArrayList();
    projects = new ArrayList();
    publishers = new ArrayList();
    variables = new ArrayList();

    canonicalize(); // canonicalize thredds metadata
    transfer2PublicMetadata(tm, true); // add local metadata
    transfer2PublicMetadata(tmi, true); // add local inherited metadata
    transfer2PublicMetadata(tmi6, true); // add local inherited metadata (cat 6 only)
    transferInheritable2PublicMetadata((InvDatasetImpl) getParent()); // add inheritable metadata from parents

    // build the expanded access list
    access = new ArrayList();

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
      java.util.Iterator dsIter = this.getDatasets().iterator();
      while (dsIter.hasNext()) {
        InvDatasetImpl curDs = (InvDatasetImpl) dsIter.next();
        ok &= curDs.finish();
      }
    }

    return ok;
  }

  /**
   * Look for InvMetadata elements in the parent that need to be added to the public metadata of this dataset.
   * Recurse up through all ancestors.
   *
   * @param parent
   */
  private void transferInheritable2PublicMetadata(InvDatasetImpl parent) {
    if (parent == null) return;
    if (debugInherit) System.out.println(" inheritFromParent= " + parent.getID());

    transfer2PublicMetadata(parent.getLocalMetadataInheritable(), true);
    transfer2PublicMetadata(parent.getCat6Metadata(), true);

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
   * take all elements from tmg and add to the public metadata of this dataset.
   * for InvMetadata elements, only add if inheritAll || InvMetadata.isInherited().
   *
   * @param tmd
   * @param inheritAll
   */
  private void transfer2PublicMetadata(ThreddsMetadata tmd, boolean inheritAll) {
    if (tmd == null) return;

    if (debugInherit) System.out.println("  transferMetadata " + tmd);

    if (authorityName == null)
      authorityName = tmd.getAuthority();
    if (dataType == null || dataType == DataType.NONE)
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
        // System.out.println(" tc assigned = "+ttc);
        tc = ttc;
      }
    }

    if (tc == null)
      tc = tmd.getTimeCoverage();

    Iterator iter = tmd.getProperties().iterator();
    while (iter.hasNext()) {
      Object item = iter.next();
      if (!properties.contains(item)) { // dont add properties with same name
        if (debugInherit) System.out.println("  add Property " + item + " to " + getID());
        properties.add(item);
      }
    }

    creators.addAll(tmd.getCreators());
    contributors.addAll(tmd.getContributors());
    dates.addAll(tmd.getDates());
    docs.addAll(tmd.getDocumentation());
    keywords.addAll(tmd.getKeywords());
    projects.addAll(tmd.getProjects());
    publishers.addAll(tmd.getPublishers());
    variables.addAll(tmd.getVariables());

    iter = tmd.getMetadata().iterator();
    while (iter.hasNext()) {
      InvMetadata meta = (InvMetadata) iter.next();
      if (meta.isInherited() || inheritAll) {
        if (!meta.isThreddsMetadata()) {
          metadata.add(meta);
        } else {
          if (debugInherit) System.out.println("  add metadata Element " + tmd.isInherited() + " " + meta);
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
  public void transferMetadata(InvDatasetImpl fromDs) {
    if (debugInherit2) System.out.println(" transferMetadata= " + fromDs.getName());

    if ( this != fromDs )
      getLocalMetadata().add( fromDs.getLocalMetadata(), false);
    transferInheritableMetadata(fromDs, getLocalMetadataInheritable());

    setResourceControl( fromDs.getResourceControl());
  }

  /** transfer inherited metadata, consolidating it into target */
  private void transferInheritableMetadata(InvDatasetImpl fromDs, ThreddsMetadata target) {
    if (fromDs == null) return;
    if (debugInherit2) System.out.println(" transferInheritedMetadata= " + fromDs.getName());

    target.add( fromDs.getLocalMetadataInheritable(), true);

    /* look through local metadata, find inherited InvMetadata elements
    ThreddsMetadata tmd = fromDs.getLocalMetadata();
    Iterator iter = tmd.getMetadata().iterator();
    while (iter.hasNext()) {
      InvMetadata meta = (InvMetadata) iter.next();
      if (meta.isInherited()) {
        if (!meta.isThreddsMetadata()) {
          tmc.addMetadata( meta);
        } else {
          if (debugInherit2) System.out.println("  transferInheritedMetadata "+meta.hashCode()+" = "+meta);
          meta.finish(); // LOOK ?? make sure XLink is read in.
          tmc.add( meta.getThreddsMetadata(), true);
        }
      }
    }   */

    // now do the same for the parents
    transferInheritableMetadata((InvDatasetImpl) fromDs.getParent(), target);
  }


  private void addExpandedAccess(InvAccessImpl a) {
    InvService service = a.getService();
    if (null == service) {
      a.check(log, false); // illegal; get error message
      return;
    }

    if (service.getServiceType() == ServiceType.COMPOUND) {
      // if its a compound service, expand it
      java.util.List serviceList = service.getServices();
      for (int i = 0; i < serviceList.size(); i++) {
        InvService nestedService = (InvService) serviceList.get(i);
        InvAccessImpl nestedAccess = new InvAccessImpl(this, a.getUrlPath(), nestedService);
        addExpandedAccess(nestedAccess); // i guess it could recurse
      }
    } else {
      access.add(a);
    }
  }

  /**
   * Put metadata into canonical form.
   * All non-inherited thredds metadata put into dataset.
   * All inherited thredds metaddata put into single metadata element, pointed to by getLocalMetadataInherited.
   * This is needed to do reliable editing.
   */
  protected void canonicalize() {

    // transfer all non-inherited thredds metadata to tm
    Iterator iter = tm.metadata.iterator();
    while(iter.hasNext()) {
      InvMetadata m = (InvMetadata) iter.next();
      if (m.isThreddsMetadata() && !m.isInherited() && !m.hasXlink()) {
        ThreddsMetadata nested = m.getThreddsMetadata();
        tm.add( nested, false);
        iter.remove();
      }
    }

    // transfer all inherited thredds metadata to tmi
    iter = tm.metadata.iterator();
    while(iter.hasNext()) {
      InvMetadata m = (InvMetadata) iter.next();
      if (m.isThreddsMetadata() && m.isInherited() && !m.hasXlink()) {
        ThreddsMetadata nested = m.getThreddsMetadata();
        tmi.add( nested, true);
        iter.remove();
      }
    }
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
  public InvDatasetImpl(String urlPath, DataType dataType, ServiceType stype) {
    super(null, "local file");
    tm.setDataType(dataType);
    tm.setServiceName("anon");
    this.urlPath = urlPath;

    // create anonomous service
    addService(new InvService(tm.getServiceName(), stype.toString(), "", "", null));

    finish();
  }

  public InvDatasetImpl(InvDataset parent, String name) {
    super(parent, name);
  }

  /** copy constructor */
  public InvDatasetImpl(InvDatasetImpl from) {
    super(from.getParent(), from.getName());

    // steal everything
    this.tm = from.getLocalMetadata();
    this.tmi = from.getLocalMetadataInheritable();
    this.accessLocal = new ArrayList( from.getAccessLocal());
    this.servicesLocal = new ArrayList( from.getServicesLocal());
    
    this.harvest = from.harvest;
    this.collectionType = from.collectionType;
  }

  ////////////////////////////////////////////////////////
  // get/set local properties

  /**
   * Get alias for this Dataset, if it exists
   */
  public String getAlias() { return alias; }

  /**
   * Set alias for this Dataset
   */
  public void setAlias(String alias) {
    this.alias = alias;
    hashCode = 0;
  }

  /**
   * Set the containing catalog; use only for top level dataset.
   */
  public void setCatalog(InvCatalog catalog) {
    this.catalog = catalog;
    hashCode = 0;
  }

  /** Get real parent dataset, no proxies
   *  @return parent dataset. If top dataset, return null.
   */
  public InvDataset getParentReal() { return parent; }

  /**
   * Get urlPath for this Dataset
   */
  public String getUrlPath() { return urlPath; }

  /**
   * Set the urlPath for this InvDatasetImpl
   */
  public void setUrlPath(String urlPath) {
    this.urlPath = urlPath;
    hashCode = 0;
  }

  /**
   * Set authorityName for this Dataset
   */
  public void setAuthority(String authorityName) {
    tm.setAuthority(authorityName);
    hashCode = 0;
  }

  ////////////////////////////////////////////////////////
  // setters for public properties (in InvDataset)

  /**
   * Set collectionType
   */
  public void setCollectionType(CollectionType collectionType) {
    this.collectionType = collectionType;
    hashCode = 0;
  }

  /**
   * Set harvest
   */
  public void setHarvest(boolean harvest) {
    this.harvest = harvest;
    hashCode = 0;
  }

  /**
   * Set the ID for this Dataset
   */
  public void setID(String id) {
    this.id = id;
    hashCode = 0;
  }

  /**
   * Set name of this Dataset.
   */
  public void setName(String name) {
    this.name = name;
    hashCode = 0;
  }

  /**
   * Set the parent dataset.
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

  public void setTimeCoverage(DateRange tc) {
    tm.setTimeCoverage(tc);
    hashCode = 0;
  }

  public void setDataType(DataType dataType) {
    tm.setDataType(dataType);
    hashCode = 0;
  }

  public double getDataSize( ) {
    return tm.getDataSize();
  }

  public void setDataSize( double dataSize ) {
    tm.setDataSize( dataSize );
    hashCode = 0;
  }

  public DateType getLastModifiedDate()
  {
    // Look for a last modified date.
    for ( Iterator it = tm.getDates().iterator(); it.hasNext(); )
    {
      DateType curDateType = (DateType) it.next();

      if ( curDateType.getType() != null && curDateType.getType().equals( "modified"))
      {
        return curDateType;
      }
    }
    return null;
  }

  public void setLastModifiedDate( DateType lastModDate )
  {
    if ( lastModDate == null )
      throw new IllegalArgumentException( "Last modified date can't be null.");
    if ( lastModDate.getType() == null || ! lastModDate.getType().equals( "modified" ) )
    {
      throw new IllegalArgumentException( "Date type must be \"modified\" (was \"" + lastModDate.getType() + "\").");
    }

    // Check for existing last modified date and remove if one exists.
    DateType curLastModDateType = this.getLastModifiedDate();
    if ( curLastModDateType != null )
    {
      tm.getDates().remove( curLastModDateType );
    }

    // Set the last modified date with the given DateType.
    tm.addDate( lastModDate );
    hashCode = 0;
  }

  public void setLastModifiedDate( Date lastModDate )
  {
    if ( lastModDate == null )
      throw new IllegalArgumentException( "Last modified date can't be null." );

    // Set the last modified date with the given Date.
    DateType lastModDateType = new DateType( false, lastModDate );
    lastModDateType.setType( "modified" );
    setLastModifiedDate( lastModDateType );
  }

  public void setServiceName(String serviceName) {
    tm.setServiceName(serviceName);
    hashCode = 0;
  }

  // LOOK these are wrong
  public void setContributors(ArrayList a) {
    List dest = tm.getContributors();
    for (Iterator iter = a.iterator(); iter.hasNext();) {
      ThreddsMetadata.Contributor item = (ThreddsMetadata.Contributor) iter.next();
      if (!dest.contains(item))
        dest.add(item);
    }
    hashCode = 0;
  }

  public void setKeywords(ArrayList a) {
    List dest = tm.getKeywords();
    for (Iterator iter = a.iterator(); iter.hasNext();) {
      ThreddsMetadata.Vocab item = (ThreddsMetadata.Vocab) iter.next();
      if (!dest.contains(item))
        dest.add(item);
    }
    hashCode = 0;
  }

  public void setProjects(ArrayList a) {
    List dest = tm.getProjects();
    for (Iterator iter = a.iterator(); iter.hasNext();) {
      ThreddsMetadata.Vocab item = (ThreddsMetadata.Vocab) iter.next();
      if (!dest.contains(item))
        dest.add(item);
    }
    hashCode = 0;
  }

  public void setPublishers(ArrayList a) {
    List dest = tm.getPublishers();
    for (Iterator iter = a.iterator(); iter.hasNext();) {
      ThreddsMetadata.Source item = (ThreddsMetadata.Source) iter.next();
      if (!dest.contains(item))
        dest.add(item);
    }
    hashCode = 0;
  }

  public void setResourceControl( String resourceControl) {
    this.resourceControl = resourceControl;
  }

  //////////////////////////////////////////////////////////////////////////////
  // add/remove/get/find local elements

  /**
   * Add InvAccess element to this dataset.
   */
  public void addAccess(InvAccess a) {
    accessLocal.add(a);
    hashCode = 0;
  }

  /**
   * Add a list of InvAccess elements to this dataset.
   */
  public void addAccess(List a) {
    accessLocal.addAll(a);
    hashCode = 0;
  }

  /**
   * Get the non-expanded access elements.
   */
  public java.util.List getAccessLocal() { return accessLocal; }

  /**
   * Get ncml element if it exists, else return null.
   */
  public org.jdom.Element getNcmlElement( ) { return ncmlElement; }
  public void setNcmlElement( org.jdom.Element ncmlElement) { this.ncmlElement = ncmlElement; }

  /**
   * Add a nested dataset.
   */
  public void addDataset(InvDatasetImpl ds) {
    if (ds == null) return;
    ds.setParent(this);
    datasets.add(ds);
    hashCode = 0;
  }

  /**
   * Add a nested dataset at the location indicated by index.
   */
  public void addDataset( int index, InvDatasetImpl ds) {
    if (ds == null) return;
    ds.setParent(this);
    datasets.add( index, ds);
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
        cat.removeDatasetByID( ds);
      return (true);
    }
    return (false);
  }

  /**
   * Replace the given dataset if it is a nesetd dataset.
   *
   * @param remove - the dataset element to be removed
   * @param add - the dataset element to be added
   * @return true on success
   */
  public boolean replaceDataset(InvDatasetImpl remove, InvDatasetImpl add) {
    for (int i = 0; i < datasets.size(); i++) {
      InvDataset dataset = (InvDataset) datasets.get(i);
       if (dataset.equals(remove)) {
         datasets.set(i, add);
         InvCatalogImpl cat = (InvCatalogImpl) getParentCatalog();
         if (cat != null) {
           cat.removeDatasetByID( remove);
           cat.addDatasetByID( add);
         }
         return true;
       }
    }
    return false;
  }

  /**
   * Add documentation element to this dataset.
   */
  public void addDocumentation(InvDocumentation doc) {
    tm.addDocumentation(doc);
    hashCode = 0;
  }

  /**
   * Add a property to this dataset
   */
  public void addProperty(InvProperty p) {
    tm.addProperty(p);
    hashCode = 0;
  }

  /**
   * Add a service to this dataset.
   *
   * @deprecated put services in catalog
   */
  public void addService(InvService service) {
    // System.out.println("--add dataset service= "+service.getName());
    servicesLocal.add(service);
    services.add(service);
    // add nested servers
    java.util.List serviceList = service.getServices();
    for (int k = 0; k < serviceList.size(); k++) {
      InvService nested = (InvService) serviceList.get(k);
      services.add(nested);
      // System.out.println("--add expanded service= "+nested.getName());
    }
    hashCode = 0;
  }

  /**
   * Remove a service from this dataset.
   *
   * @deprecated put services in catalog
   */
  public void removeService(InvService service) {
    servicesLocal.remove(service);
    services.remove(service);
    // remove nested servers
    java.util.List serviceList = service.getServices();
    for (int k = 0; k < serviceList.size(); k++) {
      InvService nested = (InvService) serviceList.get(k);
      services.remove(nested);
    }
  }

  /**
   * Get services attached specifically to this dataset.
   *
   * @return List of type InvService. May be empty, but not null.
   */
  public java.util.List getServicesLocal() { return servicesLocal; }

  /**
   * Set the list of services attached specifically to this dataset.
   * Discard any previous servies.
   *
   * @param s list of services.
   */
  public void setServicesLocal(java.util.ArrayList s) {
    this.services = new ArrayList();
    this.servicesLocal = new ArrayList();

    for (int i = 0; i < s.size(); i++) {
      InvService elem = (InvService) s.get(i);
      addService(elem);
    }
    hashCode = 0;
  }

  /** Get the metadata stored in this dataset element.
   * Inherited metadata only in an InvMetadata object. */
  public ThreddsMetadata getLocalMetadata() { return tm; }

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
   */
  public ThreddsMetadata getLocalMetadataInheritable() { return tmi; }


  /**
   * local metadata that should be inherited by this dataset's children.
   */
  public ThreddsMetadata getCat6Metadata() { return tmi6; }


  /**
   * Remove the given InvMetadata from the set of metadata local to this dataset.
   *
   * @param metadata
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

    /** get Documentation that are xlinks */
  public List getDocumentationLinks() {
    ArrayList result = new ArrayList();
    java.util.List docs = getDocumentation();
    for (int i=0; i<docs.size(); i++) {
      InvDocumentation doc = (InvDocumentation) docs.get(i);
      if (doc.hasXlink())
        result.add(doc);
    }
    return result;
  }

  /**
   * Filtering
   */
  protected boolean getMark() { return mark; }

  protected void setMark(boolean mark) { this.mark = mark; }

  /**
   * User properties
   */
  public Object getUserProperty(Object key) {
    if (userMap == null) return null;
    return userMap.get(key);
  }

  public void setUserProperty(Object key, Object value) {
    if (userMap == null) userMap = new HashMap();
    userMap.put(key, value);
  }

  private HashMap userMap = null;

  public String toString() { return getName(); }

  ////////////////////////////////////////////////////////////////////////////////////////////////

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
   * <li> append "html" to DODS Access URLs
   * </ul>
   * @param datasetEvents if true, prepend "dataset:" to any dataset access URLS
   * @param catrefEvents  if true, prepend "catref:" to any catref URLS
   */

  static public void writeHtmlDescription(StringBuffer buff, InvDatasetImpl ds,
      boolean complete, boolean isServer, boolean datasetEvents, boolean catrefEvents) {

    if (ds == null) return;

    if (complete) {
      buff.append("<html>");
      buff.append("<head>");
      buff.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">");
      buff.append("</head>");
      buff.append("<body>\n");
    }

    buff.append("<h2>Dataset: " + ds.getFullName() + "</h2>\n<ul>\n");
    if ((ds.getDataFormatType() != null) && (ds.getDataFormatType() != DataFormatType.NONE))
      buff.append(" <li><em>Data format: </em>" + StringUtil.quoteHtmlContent( ds.getDataFormatType().toString()) + "\n");

    if ((ds.getDataSize() != 0.0) && !Double.isNaN( ds.getDataSize()))
      buff.append(" <li><em>Data size: </em>" + Format.formatByteSize(ds.getDataSize()) + "\n");

    if ((ds.getDataType() != null) && (ds.getDataType() != DataType.NONE))
      buff.append(" <li><em>Data type: </em>" + StringUtil.quoteHtmlContent( ds.getDataType().toString()) + "\n");

    if ((ds.getCollectionType() != null) && (ds.getCollectionType() != CollectionType.NONE))
      buff.append(" <li><em>Collection type: </em>" + StringUtil.quoteHtmlContent( ds.getCollectionType().toString()) + "\n");

    if (ds.isHarvest())
      buff.append(" <li><em>Harvest: </em>" + ds.isHarvest() + "\n");

    if (ds.getAuthority() != null)
      buff.append(" <li><em>Naming Authority: </em>" + StringUtil.quoteHtmlContent( ds.getAuthority()) + "\n");

    if (ds.getID() != null)
      buff.append(" <li><em>ID: </em>" + StringUtil.quoteHtmlContent( ds.getID()) + "\n");

    if (ds.getResourceControl() != null)
      buff.append(" <li><em>ResourceControl: </em>" + StringUtil.quoteHtmlContent( ds.getResourceControl()) + "\n");

    if (ds instanceof InvCatalogRef) {
      InvCatalogRef catref = (InvCatalogRef) ds;
      String href = resolve(ds, catref.getXlinkHref());
      if (catrefEvents) href = "catref:" + href;
      buff.append(" <li><em>CatalogRef: </em>" + makeHref(href, null) + "\n");
    }

    buff.append("</ul>\n");

    java.util.List docs = ds.getDocumentation();
    if (docs.size() > 0) {
      buff.append("<h3>Documentation:</h3>\n<ul>\n");
      for (int i = 0; i < docs.size(); i++) {
        InvDocumentation doc = (InvDocumentation) docs.get(i);
        String type = (doc.getType() == null) ? "" : "<strong>" + StringUtil.quoteHtmlContent( doc.getType()) + ":</strong> ";
        String inline = doc.getInlineContent();
        if ((inline != null) && (inline.length() > 0))
          buff.append(" <li>" + type + StringUtil.quoteHtmlContent( inline) + "\n");
        if (doc.hasXlink()) {
          // buff.append(" <li>" + type + makeHrefResolve(ds, uri.toString(), doc.getXlinkTitle()) + "</a>\n");
          buff.append(" <li>" + type + makeHref(doc.getXlinkHref(), doc.getXlinkTitle()) + "</a>\n");
        }
      }
      buff.append("</ul>");
    }

    java.util.List access = ds.getAccess();
    if (access.size() > 0) {
      buff.append("<h3>Access:</h3>\n<ol>\n");
      for (int i = 0; i < access.size(); i++) {
        InvAccess a = (InvAccess) access.get(i);
        InvService s = a.getService();
        String urlString = a.getStandardUrlName();
        if (datasetEvents) urlString = "dataset:" + urlString;
        if (isServer) {
          ServiceType stype = s.getServiceType();
          if ((stype == ServiceType.OPENDAP) || (stype == ServiceType.DODS))
            urlString = urlString + ".html";
          else if (stype == ServiceType.WCS)
            urlString = urlString + "?request=GetCapabilities&version=1.0.0&service=WCS";
          else if (stype == ServiceType.NetcdfServer)
            urlString = urlString + "?showForm";
        }
        buff.append(" <li> <b>" + StringUtil.quoteHtmlContent( s.getServiceType().toString()));
        buff.append(":</b> " + makeHref(urlString, a.getStandardUrlName()) + "\n");
      }
      buff.append("</ol>\n");
    }

    java.util.List list = ds.getContributors();
    if (list.size() > 0) {
      buff.append("<h3>Contributors:</h3>\n<ul>\n");
      for (int i = 0; i < list.size(); i++) {
        ThreddsMetadata.Contributor t = (ThreddsMetadata.Contributor) list.get(i);
        String role = (t.getRole() == null) ? "" : "<strong> (" + StringUtil.quoteHtmlContent( t.getRole()) + ")</strong> ";
        buff.append(" <li>" + StringUtil.quoteHtmlContent( t.getName()) + role + "\n");
      }
      buff.append("</ul>");
    }

    list = ds.getKeywords();
    if (list.size() > 0) {
      buff.append("<h3>Keywords:</h3>\n<ul>\n");
      for (int i = 0; i < list.size(); i++) {
        ThreddsMetadata.Vocab t = (ThreddsMetadata.Vocab) list.get(i);
        String vocab = (t.getVocabulary() == null) ? "" : " <strong>(" + StringUtil.quoteHtmlContent( t.getVocabulary()) + ")</strong> ";
        buff.append(" <li>" + StringUtil.quoteHtmlContent( t.getText()) + vocab + "\n");
      }
      buff.append("</ul>");
    }

    list = ds.getDates();
    if (list.size() > 0) {
      buff.append("<h3>Dates:</h3>\n<ul>\n");
      for (int i = 0; i < list.size(); i++) {
        DateType d = (DateType) list.get(i);
        String type = (d.getType() == null) ? "" : " <strong>(" + StringUtil.quoteHtmlContent( d.getType()) + ")</strong> ";
        buff.append(" <li>" + StringUtil.quoteHtmlContent( d.getText()) + type + "\n");
      }
      buff.append("</ul>");
    }

    list = ds.getProjects();
    if (list.size() > 0) {
      buff.append("<h3>Projects:</h3>\n<ul>\n");
      for (int i = 0; i < list.size(); i++) {
        ThreddsMetadata.Vocab t = (ThreddsMetadata.Vocab) list.get(i);
        String vocab = (t.getVocabulary() == null) ? "" : " <strong>(" + StringUtil.quoteHtmlContent( t.getVocabulary()) + ")</strong> ";
        buff.append(" <li>" + StringUtil.quoteHtmlContent( t.getText()) + vocab + "\n");
      }
      buff.append("</ul>");
    }

    list = ds.getCreators();
    if (list.size() > 0) {
      buff.append("<h3>Creators:</h3>\n<ul>\n");
      for (int i = 0; i < list.size(); i++) {
        ThreddsMetadata.Source t = (ThreddsMetadata.Source) list.get(i);
        buff.append(" <li><strong>" + StringUtil.quoteHtmlContent( t.getName()) + "</strong><ul>\n");
        buff.append(" <li><em>email: </em>" + StringUtil.quoteHtmlContent( t.getEmail()) + "\n");
        if (t.getUrl() != null) {
          buff.append(" <li> <em>" + makeHrefResolve(ds, t.getUrl(), null) + "</em>\n");
        }
        buff.append(" </ul>\n");
      }
      buff.append("</ul>");
    }

    list = ds.getPublishers();
    if (list.size() > 0) {
      buff.append("<h3>Publishers:</h3>\n<ul>\n");
      for (int i = 0; i < list.size(); i++) {
        ThreddsMetadata.Source t = (ThreddsMetadata.Source) list.get(i);
        buff.append(" <li><strong>" + StringUtil.quoteHtmlContent( t.getName()) + "</strong><ul>\n");
        buff.append(" <li><em>email: </em>" + StringUtil.quoteHtmlContent( t.getEmail()) + "\n");
        if (t.getUrl() != null) {
          buff.append(" <li> <em>" + makeHrefResolve(ds, t.getUrl(), null) + "</em>\n");
        }
        buff.append(" </ul>\n");
      }
      buff.append("</ul>");
    }

    list = ds.getVariables();
    if (list.size() > 0) {
      buff.append("<h3>Variables:</h3>\n<ul>\n");
      for (int i = 0; i < list.size(); i++) {
        ThreddsMetadata.Variables t = (ThreddsMetadata.Variables) list.get(i);

        if (t.getVocabUri() != null) {
          URI uri = t.getVocabUri();
          buff.append(" <li>" + makeHrefResolve(ds, uri.toString(), t.getVocabulary()) + "</a>");
        } else {
          buff.append(" <li>" + StringUtil.quoteHtmlContent( t.getVocabulary()));
        }
        buff.append(" <em>vocabulary:</em> <ul>\n");

        java.util.List vlist = t.getVariableList();
        if (vlist.size() > 0) {
          for (int j = 0; j < vlist.size(); j++) {
            ThreddsMetadata.Variable v = (ThreddsMetadata.Variable) vlist.get(j);
            buff.append(" <li><strong>" + StringUtil.quoteHtmlContent( v.getName()) + "</strong> = ");
            String desc = (v.getDescription() == null) ? "" : " <i>" +StringUtil.quoteHtmlContent( v.getDescription())+"</i> = ";
            buff.append(desc);
            String units = (v.getUnits() == null || v.getUnits().length() == 0) ? "" : " (" + v.getUnits() + ") ";
            buff.append(StringUtil.quoteHtmlContent( v.getVocabularyName() + units) + "\n");
          }
          buff.append(" </ul>\n");
        }
        buff.append("</ul>");
      }
      buff.append("</ul>");
    }

    ThreddsMetadata.GeospatialCoverage gc = ds.getGeospatialCoverage();
    if ((gc != null) && !gc.isEmpty()) {
      buff.append("<h3>GeospatialCoverage:</h3>\n<ul>\n");
      if (gc.isGlobal())
        buff.append(" <li><em> Global </em></ul>\n");
      else {
        buff.append(" <li><em> Longitude: </em> " + rangeString(gc.getEastWestRange()) + "\n");
        buff.append(" <li><em> Latitude: </em> " + rangeString(gc.getNorthSouthRange()) + "\n");
        if (gc.getUpDownRange() != null)
          buff.append(" <li><em> Altitude: </em> " + rangeString(gc.getUpDownRange()) +
              " (positive is <strong>" + StringUtil.quoteHtmlContent( gc.getZPositive()) + ")</strong>\n");

        java.util.List nlist = gc.getNames();
        if ((nlist != null) && (nlist.size() > 0)) {
          buff.append(" <li><em>  Names: </em> <ul>\n");
          for (int i = 0; i < nlist.size(); i++) {
            ThreddsMetadata.Vocab elem = (ThreddsMetadata.Vocab) nlist.get(i);
            buff.append(" <li>" + StringUtil.quoteHtmlContent( elem.getText()) + "\n");
          }
          buff.append(" </ul>\n");
        }
        buff.append(" </ul>\n");
      }
    }

    DateRange tc = ds.getTimeCoverage();
    if (tc != null) {
      buff.append("<h3>TimeCoverage:</h3>\n<ul>\n");
      DateType start = tc.getStart();
      if ((start != null) && !start.isBlank())
        buff.append(" <li><em>  Start: </em> " + start.toDateTimeString() + "\n");
      DateType end = tc.getEnd();
      if ((end != null) && !end.isBlank())
        buff.append(" <li><em>  End: </em> " + end.toDateTimeString() + "\n");
      TimeDuration duration = tc.getDuration();
      if ((duration != null) && !duration.isBlank())
        buff.append(" <li><em>  Duration: </em> " + StringUtil.quoteHtmlContent( duration.toString()) + "\n");
      TimeDuration resolution = tc.getResolution();
      if (tc.useResolution() && (resolution != null) && !resolution.isBlank())
        buff.append(" <li><em>  Resolution: </em> " + StringUtil.quoteHtmlContent( resolution.toString()) + "\n");
      buff.append(" </ul>\n");
    }

    java.util.List metadata = ds.getMetadata();
    boolean gotSomeMetadata = false;
    for (int i = 0; i < metadata.size(); i++) {
      InvMetadata m = (InvMetadata) metadata.get(i);
      if (m.hasXlink()) gotSomeMetadata = true;
    }

    if (gotSomeMetadata) {
      buff.append("<h3>Metadata:</h3>\n<ul>\n");
      for (int i = 0; i < metadata.size(); i++) {
        InvMetadata m = (InvMetadata) metadata.get(i);
        String type = (m.getMetadataType() == null) ? "" : m.getMetadataType();
        if (m.hasXlink()) {
          String title = (m.getXlinkTitle() == null) ? "Type " + type : m.getXlinkTitle();
          buff.append(" <li> " + makeHrefResolve(ds, m.getXlinkHref(), title) + "\n");
        } //else {
        //buff.append(" <li> <pre>"+m.getMetadataType()+" "+m.getContentObject()+"</pre>\n");
        //}
      }
      buff.append("</ul>");
    }

    java.util.List props = ds.getProperties();
    if (props.size() > 0) {
      buff.append("<h3>Properties:</h3>\n<ul>\n");
      for (int i = 0; i < props.size(); i++) {
        InvProperty p = (InvProperty) props.get(i);
        if (p.getName().equals("attachments")) // LOOK whats this ?
          buff.append(" <li>" + makeHrefResolve(ds, p.getValue(), p.getName()) + "\n");
        else
          buff.append(" <li>" + StringUtil.quoteHtmlContent( p.getName() + " = \"" + p.getValue()) + "\"\n");
      }
      buff.append("</ul>");
    }

    if (complete) buff.append("</body></html>");
  }

  static private String rangeString(ThreddsMetadata.Range r) {
    if (r == null) return "";
    String units = (r.getUnits() == null) ? "" : " " + r.getUnits();
    String resolution = r.hasResolution() ? " Resolution=" + r.getResolution() : "";
    return StringUtil.quoteHtmlContent( r.getStart() + " to " + (r.getStart() + r.getSize()) + resolution + units);
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
        System.err.println("InvDatasetImpl.writeHtml: error parsing URL= " + href);
      }
    }
    return href;
  }

  static private String makeHref(String href, String title) {
    if (title == null) title = href;
    return "<a href='" + StringUtil.quoteHtmlContent( href) + "'>" + StringUtil.quoteHtmlContent( title) + "</a>";
  }

  static private String makeHrefResolve(InvDatasetImpl ds, String href, String title) {
    if (title == null) title = href;
    href = resolve(ds, href);
    return makeHref(href, title);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * debugging info
   */
  public String dump() { return dump(0); }

  String dump(int n) {
    StringBuffer buff = new StringBuffer(100);

    buff.append(indent(n));
    buff.append("Dataset name:<" + getName());
    if (dataType != null)
      buff.append("> dataType:<" + dataType);
    if (urlPath != null)
      buff.append("> urlPath:<" + urlPath);
    if (defaultService != null)
      buff.append("> defaultService <" + defaultService);
    buff.append("> uID:<" + getUniqueID());
    buff.append(">\n");

    List svcs = getServicesLocal();
    if (svcs.size() > 0) {
      String indent = indent(n + 2);
      buff.append(indent);
      buff.append("Services:\n");
      for (int i = 0; i < svcs.size(); i++) {
        InvService s = (InvService) svcs.get(i);
        buff.append(s.dump(n + 4));
      }
    }

    if (access.size() > 0) {
      String indent = indent(n + 2);
      buff.append(indent);
      if (access.size() == 1) {
        buff.append("Access: " + access.get(0) + "\n");
      } else if (access.size() > 1) {
        buff.append("Access:\n");
        for (int i = 0; i < access.size(); i++) {
          InvAccess a = (InvAccessImpl) access.get(i);
          buff.append(indent(n + 4) + a + "\n");
        }
      }
    }

    buff.append(indent(n)+"Thredds Metadata\n");
    buff.append(tm.dump(n+4) + "\n");
    buff.append(indent(n)+"Thredds Metadata Inherited\n");
    buff.append(tmi.dump(n+4) + "\n");
    buff.append(indent(n)+"Thredds Metadata Cat6\n");
    buff.append(tmi6.dump(n+4) + "\n");

    if (datasets.size() > 0) {
      String indent = indent(n + 2);
      buff.append(indent);
      buff.append("Datasets:\n");
      for (int i = 0; i < datasets.size(); i++) {
        InvDatasetImpl ds = (InvDatasetImpl) datasets.get(i);
        buff.append(ds.dump(n + 4));
      }
    }
    return buff.toString();
  }

  static String indent(int n) {
    StringBuffer blanks = new StringBuffer(n);
    for (int i = 0; i < n; i++)
      blanks.append(" ");
    return blanks.toString();
  }

  boolean check(StringBuffer out, boolean show) {
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

    for (int i = 0; i < access.size(); i++) {
      InvAccessImpl a = (InvAccessImpl) access.get(i);
      isValid &= a.check(out, show);
    }

    for (int i = 0; i < datasets.size(); i++) {
      InvDatasetImpl ds = (InvDatasetImpl) datasets.get(i);
      isValid &= ds.check(out, show);
    }

    List mdata = getMetadata();
    for (int i = 0; i < mdata.size(); i++) {
      InvMetadata m = (InvMetadata) mdata.get(i);
      m.check(out);
    }

    List services = getServicesLocal();
    for (int i = 0; i < services.size(); i++) {
      InvService s = (InvService) services.get(i);
      isValid &= s.check(out);
    }

    if (hasAccess() && (getDataType() == null)) {
      out.append("**Warning: Dataset (" + getFullName() + "): is selectable but no data type declared in it or in a parent element\n");
    }

    if (!hasAccess() && !hasNestedDatasets()) {
      out.append("**Warning: Dataset (" + getFullName() + "): is not selectable and does not have nested datasets\n");
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

      if (null != getTimeCoverage())
        result = 37 * result + getTimeCoverage().hashCode(); // */

      hashCode = result;
    }
    return hashCode;
  }

  private volatile int hashCode = 0; // Bloch, item 8 - lazily initialize hash value


  /**
   * test
   */
  public static void main(String[] args) {
    InvDatasetImpl topDs = new InvDatasetImpl(null, "topDs", DataType.getType("Grid"), "myService", "myUrlPath/");
    InvService myS = new InvService("myService", ServiceType.DODS.toString(),
        "http://motherlode.ucar.edu/cgi-bin/dods/nph-dods", "", null);
    topDs.addService(myS);
    topDs.getLocalMetadata().setServiceName("myService");
    InvDatasetImpl childDs = new InvDatasetImpl(null, "childDs", null, null, "myUrlPath/");
    topDs.addDataset(childDs);
    InvService ts = childDs.findService("myService");

    System.out.println("InvDatasetImpl.main(): " + childDs.getAccess(ServiceType.DODS).toString());
  }

}

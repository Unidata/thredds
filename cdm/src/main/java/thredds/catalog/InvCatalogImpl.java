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

import ucar.nc2.units.DateType;

import java.util.*;
import java.net.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.event.EventListenerList;

/**
 * Concrete implementation of a Thredds catalog object. Use this when you are constructing or modifying.
 *
 * @author john caron
 * @see InvCatalog
 */

public class InvCatalogImpl extends InvCatalog {
  private String createFrom;
  // private String dtdID; // keep track of how read, so can write XML
  private List<DataRootConfig> roots = new ArrayList<DataRootConfig>();

  // validation
  private StringBuilder log = new StringBuilder();
  private boolean hasError = false;

  /**
   * Munge this catalog so the given dataset is the top catalog.
   *
   * @param ds make this top; must be existing dataset in this catalog.
   */
  public void subset(InvDataset ds) {
    InvDatasetImpl dataset = (InvDatasetImpl) ds;

    // Make all inherited metadata local.
    dataset.transferMetadata(dataset);

    topDataset = dataset;
    datasets.clear(); // throw away the rest
    datasets.add(topDataset);

    // parent lookups need to be local
    //InvService service = dataset.getServiceDefault();
    //if (service != null) LOOK
    //  dataset.serviceName = service.getName();
    dataset.dataType = dataset.getDataType();

    // all properties need to be local
    // LOOK dataset.setPropertiesLocal( new ArrayList(dataset.getProperties()));

    // next part requires this before it
    dataset.setCatalog(this);
    dataset.parent = null;

    // any referenced services need to be local
    List<InvService> services = new ArrayList<InvService>(dataset.getServicesLocal());
    findServices(services, dataset);
    dataset.setServicesLocal(services);

    finish();
  }

//  /**
//   * Return a catalog with a shallow copy of the given dataset as the top dataset.
//   *
//   * @param dataset the dataset to copy into a new catalog
//   * @return A new catalog with a shallow copy of the given dataset as the top dataset.
//   */
//  public InvCatalogImpl subsetShallowCopy( InvDatasetImpl dataset)
//  {
//    InvCatalogImpl newCatalog = new InvCatalogImpl( null, "1.0.2", null);
//    InvDatasetImpl newTopDataset = new InvDatasetImpl( null, dataset.getName());
//    newTopDataset.transferMetadata( dataset );
//    if (dataset.getAccess() != null && ! dataset.getAccess().isEmpty())
//    {
//      newTopDataset.addAccess( dataset.getAccess() );
//    }
//    dataset.getDatasets();
//    dataset.getID();
//    dataset.getServiceName();
//    dataset.getServicesLocal();
//    dataset.getUrlPath();
//
//    // ToDo LOOK Probably fine for throw away catalog that is only generated so that XML can be generated.
//    dataset.finish();
//
//    newCatalog.addDataset( newTopDataset );
//
//    return newCatalog;
//  }

  void findServices(List<InvService> result, InvDataset ds) {
    if (ds instanceof InvCatalogRef) return;

    // look for access elements with unresolved services
    for (InvAccess a : ds.getAccess()) {
      InvService s = a.getService();
      InvDataset d = a.getDataset();
      if (null == d.findService(s.getName()) && !(result.contains(s)))
        result.add(s);
    }

    // recurse into nested datasets
    for (InvDataset nested : ds.getDatasets()) {
      findServices(result, nested);
    }
  }

  /**
   * Munge this catalog to remove any dataset that doesnt pass through the filter.
   *
   * @param filter remove datasets that dont pass this filter.
   */
  public void filter(DatasetFilter filter) {
    mark(filter, topDataset);
    delete(topDataset);
    this.filter = filter;
  }

  // got to keep it for catalogRef's
  private DatasetFilter filter = null;

  protected DatasetFilter getDatasetFilter() {
    return filter;
  }

  // see if this ds should be filtered out.
  // if so, setMark and return true.
  // if any nested dataset are kept, then keep it.
  // unread CatalogRefs are always kept.
  private boolean mark(DatasetFilter filter, InvDatasetImpl ds) {
    if (ds instanceof InvCatalogRef) {
      InvCatalogRef catRef = (InvCatalogRef) ds;
      if (!catRef.isRead()) return false;
    }

    // recurse into nested datasets first
    boolean allMarked = true;
    for (InvDataset nested : ds.getDatasets()) {
      allMarked &= mark(filter, (InvDatasetImpl) nested);
    }
    if (!allMarked) return false;

    if (filter.accept(ds) >= 0)
      return false;

    // mark for deletion
    ds.setMark(true);
    if (debugFilter) System.out.println(" mark " + ds.getName());
    return true;
  }

  private boolean debugFilter = false;

  // remove marked datasets
  private void delete(InvDatasetImpl ds) {
    if (ds instanceof InvCatalogRef) {
      InvCatalogRef catRef = (InvCatalogRef) ds;
      if (!catRef.isRead()) return;
    }

    Iterator iter = ds.getDatasets().iterator();
    while (iter.hasNext()) {
      InvDatasetImpl nested = (InvDatasetImpl) iter.next();
      if (nested.getMark()) {
        iter.remove();
        if (debugFilter) System.out.println(" remove " + nested.getName());
      } else
        delete(nested);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////
  // construction and internals

  /**
   * Construct an InvCatalog.
   * You must call finish() after all objects are added.
   *
   * @param name    : catalog name.
   * @param version : catalog version.
   * @param baseURI : catalog base URI (external).
   */
  public InvCatalogImpl(String name, String version, URI baseURI) {
    this(name, version, null, baseURI);
  }

  /**
   * Construct an InvCatalog.
   * You must call finish() after all objects are added.
   *
   * @param name    : catalog name.
   * @param version : catalog version.
   * @param expires : date/time catalog expires.
   * @param baseURI : catalog base URI  (external).
   */
  public InvCatalogImpl(String name, String version, DateType expires, URI baseURI) {
    this.name = name;
    this.version = version;
    this.expires = expires;
    this.baseURI = baseURI;
  }

  /**
   * Finish constructing after all elements have been added or modified.
   * This routine will do any needed internal consistency work.
   * Its ok to call multiple times.
   *
   * @return true if successful.
   */
  public boolean finish() {

    // make topDataset if needed
    //if (topDataset == null) {

    if (datasets.size() == 1) { // already only one; make it top
      topDataset = (InvDatasetImpl) datasets.get(0);

    } else { // create one
      topDataset = new InvDatasetImpl(null, name == null ? "Top Dataset" : name);
      for (InvDataset dataset : datasets)
        topDataset.addDataset((InvDatasetImpl) dataset);
      topDataset.setServicesLocal(services);
    }
    //}
    topDataset.setCatalog(this);

    // build dataset hash table
    dsHash = new HashMap<String, InvDataset>();
    addDatasetIds(topDataset);

    // recurse through the datasets and finish them
    return topDataset.finish();
  }

  private void addDatasetIds(InvDatasetImpl ds) {
    addDatasetByID(ds);

    if (ds instanceof InvCatalogRef) return;
    //if (ds instanceof InvDatasetFmrc) return;

    // recurse into nested
    for (InvDataset invDataset : ds.getDatasets()) {
      InvDatasetImpl nested = (InvDatasetImpl) invDataset;
      addDatasetIds(nested);
    }
  }

  /**
   * Add Dataset to internal hash.
   *
   * @param ds : add this dataset if ds.getID() != null
   * @see InvCatalog#findDatasetByID
   */
  public void addDatasetByID(InvDatasetImpl ds) {
    if (ds.getID() != null)
      dsHash.put(ds.getID(), ds);
  }

  /**
   * Find the dataset in this catalog by its ID. If found, remove it.
   *
   * @param ds Remove this dataset from the hash
   */
  public void removeDatasetByID(InvDatasetImpl ds) {
    if (ds.getID() != null)
      dsHash.remove(ds.getID());
  }

  /**
   * Add Dataset (1.0)
   *
   * @param ds add this dataset
   */
  public void addDataset(InvDatasetImpl ds) {
    if (ds != null)
      datasets.add(ds);
  }

  /**
   * Remove the given dataset from this catalog if it is a direct child of this catalog.
   *
   * @param ds remove this dataset
   * @return true if found and removed
   */
  public boolean removeDataset(InvDatasetImpl ds) {
    if (this.datasets.remove(ds)) {
      ds.setParent(null);
      removeDatasetByID(ds);
      return true;
    }
    return false;
  }

  /**
   * Replace the given dataset if it is a nested dataset.
   *
   * @param remove - the dataset element to be removed
   * @param add    - the dataset element to be added
   * @return true on success
   */
  public boolean replaceDataset(InvDatasetImpl remove, InvDatasetImpl add) {
    if (topDataset.equals(remove)) {
      topDataset = add;
      topDataset.setCatalog(this);
    }
    for (int i = 0; i < datasets.size(); i++) {
      InvDataset dataset = datasets.get(i);
      if (dataset.equals(remove)) {
        datasets.set(i, add);
        removeDatasetByID(remove);
        addDatasetByID(add);
        return true;
      }
    }
    return false;
  }

  /**
   * Add Property (1.0)
   *
   * @param p add this property
   */
  public void addProperty(InvProperty p) {
    properties.add(p);
  }

  /**
   * Add Service (1.0)
   *
   * @param s add this service
   */
  public void addService(InvService s) {
    if (s == null)
      throw new IllegalArgumentException("Service to add was null.");

    // While adding a service, there are three possible results:
    if (s.getName() != null) {
      Object obj = serviceHash.get(s.getName());
      if (obj == null) {
        // 1) No service with matching name entry was found, add given service;
        serviceHash.put(s.getName(), s);
        services.add(s);
        return;

      } else {
        // A service with matching name was found.
        if (s.equals(obj)) {
          // 2) matching name entry, objects are equal so OK;
          return;
        } else {
          // 3) matching name entry, objects are not equal so ???
          // @todo throw an exception???
          // Currently just dropping given service
          log.append("Multiple Services with the same name\n");
          return;
        }
      }
    }
  }

  /**
   * Add top-level InvDataset to this catalog.
   *
   * @deprecated Use addDataset() instead; datamodel now allows multiple top level datasets.
   */
  public void setDataset(InvDatasetImpl ds) {
    topDataset = ds;
    addDataset(ds);
  }

  /**
   * String describing how the catalog was created, for debugging.
   *
   * @return how the catalog was created, for debugging
   */
  public String getCreateFrom() {
    return createFrom;
  }

  /**
   * Set how the catalog was created, for debugging.
   * @param createFrom how the catalog was created, for debugging
   */
  public void setCreateFrom(String createFrom) {
    this.createFrom = createFrom;
  }

  /**
   * Set the catalog base URI.
   * Its used to resolve reletive URLS.
   * @param baseURI set to this
   */
  public void setBaseURI(URI baseURI) {
    this.baseURI = baseURI;
  }

  /**
   * @return the catalog base URI.
   */
  public URI getBaseURI() {
    return baseURI;
  }

  /*
   * @return DTD string
   *
  public String getDTDid() {
    return dtdID;
  }

  /*
   * set DTD
   *
  public void setDTDid(String dtdID) {
    this.dtdID = dtdID;
  } */

  /**
   * Set the expires date after which the catalog is no longer valid.
   *
   * @param expiresDate a {@link DateType} representing the date after which the catlog is no longer valid.
   */
  public void setExpires(DateType expiresDate) {
    this.expires = expiresDate;
  }

  /**
   * Check if there is a fatal error and catalog should not be used.
   *
   * @return true if catalog not useable.
   */
  public boolean hasFatalError() {
    return hasError;
  }

  /**
   * Append an error message to the message log. Call check() to get the log when
   * everything is done.
   *
   * @param message   append this message to log
   * @param isInvalid true if this is a fatal error.
   */
  public void appendErrorMessage(String message, boolean isInvalid) {
    log.append(message);
    hasError = hasError | isInvalid;
  }

  /**
   * Check internal data structures.
   *
   * @param out  : print errors here
   * @param show : print messages for each object (debug)
   * @return true if no fatal consistency errors.
   */
  public boolean check(StringBuilder out, boolean show) {
    boolean isValid = !hasError;
    out.append("----Catalog Validation version 1.0.01\n");

    if (log.length() > 0)
      out.append(log);

    if (show) System.out.println(" catalog valid = " + isValid);

    //if (topDataset != null)
    //  isValid &= topDataset.check( out, show);

    for (InvDataset ds : datasets) {
      InvDatasetImpl dsi = (InvDatasetImpl) ds;
      dsi.check(out, show); // cant make it invalid !!
    }

    return isValid;
  }

  public String getLog() {
    return log.toString();
  }

  /**
   * Debugging: dump entire data structure.
   *
   * @return String representation.
   */
  public String dump() {
    StringBuilder buff = new StringBuilder(1000);
    buff.setLength(0);

    buff.append("Catalog <").append(getName())
        .append("> <").append(getVersion())
        .append("> <").append(getCreateFrom()).append(">\n");
    buff.append(topDataset.dump(2));

    return buff.toString();
  }

  /**
   * Add a PropertyChangeEvent Listener. THIS IS EXPERIMENTAL DO NOT RELY ON.
   * Throws a PropertyChangeEvent:
   * <ul><li>propertyName = "InvCatalogRefInit", getNewValue() = InvCatalogRef that was just initialized
   * </ul>
   * @param l the listener
   */
  public void addPropertyChangeListener(PropertyChangeListener l) {
    if (listenerList == null) listenerList = new EventListenerList();
    listenerList.add(PropertyChangeListener.class, l);
  }

  /**
   * Remove a PropertyChangeEvent Listener.
   * @param l the listener
   */
  public void removePropertyChangeListener(PropertyChangeListener l) {
    listenerList.remove(PropertyChangeListener.class, l);
  }

  private EventListenerList listenerList = null;

  // PropertyChangeEvent(Object source, String propertyName, Object oldValue, Object newValue)
  void firePropertyChangeEvent(PropertyChangeEvent event) {
    // System.out.println("firePropertyChangeEvent "+event);
    if (listenerList == null) return;

    // Process the listeners last to first
    Object[] listeners = listenerList.getListenerList();
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == PropertyChangeListener.class) {
        ((PropertyChangeListener) listeners[i + 1]).propertyChange(event);
      }
    }
  }

  /**
   * This finds the topmost catalog, even when its a InvCatalogRef.
   * Used to throw a PropertyChange event on the top catalog.
   *
   * @return top catalog
   */
  InvCatalogImpl getTopCatalog() {
    return (top == null) ? this : top;
  }

  void setTopCatalog(InvCatalogImpl top) {
    this.top = top;
  }

  private InvCatalogImpl top = null;

  // this is how catalogRefs read their catalogs
  InvCatalogFactory getCatalogFactory() {
    return factory;
  }

  void setCatalogFactory(InvCatalogFactory factory) {
    this.factory = factory;
  }

  private InvCatalogFactory factory = null;

  // track converter
  InvCatalogConvertIF getCatalogConverter() {
    return converter;
  }

  void setCatalogConverter(InvCatalogConvertIF converter) {
    this.converter = converter;
  }

  private InvCatalogConvertIF converter = null;

  /**
   * Set the connverter to 1.0, typically to write a 0.6 out to a 1.0
   */
  public void setCatalogConverterToVersion1() {
    setCatalogConverter(factory.getCatalogConverter(XMLEntityResolver.CATALOG_NAMESPACE_10));
  }

  /**
   * Get dataset roots.
   *
   * @return List of InvProperty. May be empty, may not be null.
   */
  public java.util.List<DataRootConfig> getDatasetRoots() {
    return roots;
  }


  /**
   * Add Dataset Root, key = path,  value = location.
   * @param root add a dataset root
   */
  public void addDatasetRoot(DataRootConfig root) {
    roots.add(root);
  }

  /**
   * Write the catalog as an XML document to the specified stream.
   *
   * @param os write to this OutputStream
   * @throws java.io.IOException on an error.
   */
  public void writeXML(java.io.OutputStream os) throws java.io.IOException {
    InvCatalogConvertIF fac = getCatalogConverter();
    fac.writeXML(this, os);
  }

  /**
   * Write the catalog as an XML document to the specified stream.
   *
   * @param os  write to this OutputStream
   * @param raw if true, write original (server) version, else write client version
   * @throws java.io.IOException on an error.
   */
  public void writeXML(java.io.OutputStream os, boolean raw) throws java.io.IOException {
    InvCatalogConvertIF fac = getCatalogConverter();
    fac.writeXML(this, os, raw);
  }

  /**
   * InvCatalogImpl elements with same values are equal.
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InvCatalogImpl)) return false;
    return o.hashCode() == this.hashCode();
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  public int hashCode() {
    if (hashCode == 0) {
      int result = 17;
      if (null != getName())
        result = 37 * result + getName().hashCode();
      result = 37 * result + getServices().hashCode();
      result = 37 * result + getDatasets().hashCode();
      hashCode = result;
    }
    return hashCode;
  }

  private volatile int hashCode = 0; // Bloch, item 8

}
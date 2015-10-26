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

import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.units.DateType;

import java.net.URI;

import ucar.nc2.constants.FeatureType;

/**
 * A reference to a InvCatalog. The referenced catalog is not read until getDatasets() is called.
 * A client will see the referenced catalog as a nested dataset.
 * <p/>
 * <p/>
 * The client can also do asynchronous reading, if the InvCatalogFactory supports it, and if readAsynch() is used.
 * <p/>
 * <pre>
 * Parent relationship:
 *   ds -> catRef -- catalog
 *                      ^ top  -> ds ...
 * <p/>
 *  ParentView relationship:
 *   ds -> catRef -> top  -> ds ...
 *           (or) -> ds                 if UseProxy
 * <p/>
 * </pre>
 *
 * @author john caron
 * @see InvDatasetImpl for API, thredds.catalog.ui.CatalogTreeView as example to read asynchronously
 */

public class InvCatalogRef extends InvDatasetImpl {
  private String href;
  private Boolean useRemoteCatalogService;

  private InvDatasetImpl proxy = null; // top dataset of referenced catalog
  private URI uri = null;
  private String errMessage = null;

  private boolean init = false, useProxy = false;
  private boolean debug = false, debugProxy = false, debugAsynch = false;

  /**
   * Constructor.
   *
   * @param parent : parent dataset
   * @param name  : display name of collection
   * @param href   : URL to another catalog
   */
  public InvCatalogRef(InvDatasetImpl parent, String name, String href) {
    super(parent, name);
    setXlinkHref(href);
  }

  /**
   * Constructor.
   *
   * @param parent : parent dataset
   * @param name  : display name of collection
   * @param href   : URL to another catalog
   * @param useRemoteCatalogService : force catalogRef to go through the remoteCatalogService
   */
  public InvCatalogRef(InvDatasetImpl parent, String name, String href, Boolean useRemoteCatalogService) {
    this(parent, name, href);
    this.useRemoteCatalogService = useRemoteCatalogService;
  }

  public Boolean useRemoteCatalogService() { return useRemoteCatalogService; }

  /**
   * @return Xlink Href, as a String, unresolved
   */
  public String getXlinkHref() {
    return href;
  }

  public void setXlinkHref(String href) {
    this.href = href.trim();
    this.uri = null;
  }

  /**
   * @return Xlink reference as a URI, resolved
   */
  public URI getURI() {
    if (uri != null) return uri;

    // may be relative
    try {
      return getParentCatalog().resolveUri(href);
    }
    catch (java.net.URISyntaxException e) {
      synchronized (this) {
        errMessage = "URISyntaxException on url  " + href + " = " + e.getMessage();
      }
      return null;
    }
  }

  /**
   * Get a list of all the nested datasets.
   * @return Datasets. This triggers a read of the referenced catalog the first time its called.
   */
  @Override
  public java.util.List<InvDataset> getDatasets() {
    read();
    return useProxy ? proxy.getDatasets() : super.getDatasets();
  }

  /**
   * @return true the referenced catalog has been read
   */
  public boolean isRead() {
    return init;
  }

  /**
   * This triggers a read of the referenced catalog the first time its called.
   * @return top dataset of referenced catalog.
   */
  public InvDatasetImpl getProxyDataset() {
    read();
    return proxy;
  }

  /**
   * Release resources - undo the read of the catalog. This is needed when crawling large catalogs.
   * For modest catalogs that you will repeatedly examine, do not use this method.
   */
  public void release() {
    datasets = new java.util.ArrayList<>();
    proxy = null;
    useProxy = false;
    init = false;
  }


  //////////////////////////////////////////////
  @Override
  public boolean finish() {
    return super.finish();
  }

  private synchronized void read() {
    if (init)
      return;

    URI uriResolved = getURI();
    if (uriResolved == null) {
      // this is to display an error message
      proxy = new InvDatasetImpl(null, "HREF ERROR");
      proxy.addProperty(new InvProperty("HREF ERROR", errMessage));
      datasets.add(proxy);
      init = true;
      return;
    }

    // open and read the referenced catalog XML
    try {
      if (debug)
        System.out.println(" InvCatalogRef read " + getFullName() + "  hrefResolved = " + uriResolved);

      InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(true);
      // InvCatalogFactory factory = ((InvCatalogImpl) getParentCatalog()).getCatalogFactory();
      InvCatalogImpl cat = factory.readXML(uriResolved.toString());
      finishCatalog(cat);

    } catch (Exception e) {
      // this is to display an error message
      proxy = new InvDatasetImpl(null, "HREF ERROR");
      if (debug)
        System.out.println("HREF ERROR =\n  " + href + " err= " + e.getMessage());
      proxy.addProperty(new InvProperty("HREF ERROR", href));
      datasets.add(proxy);
      init = true;
    }
  }

  private void finishCatalog(InvCatalogImpl cat) {
    InvCatalogImpl parentCatalog;
    if (cat.hasFatalError()) {
      // this is to display an error message
      proxy = new InvDatasetImpl(null, "ERROR OPENING");
      StringBuilder out = new StringBuilder();
      cat.check(out);
      if (debug) System.out.println("PARSE ERROR =\n  " + out.toString());
      proxy.addProperty(new InvProperty("ERROR OPENING", out.toString()));
      proxy.finish();

    } else {
      // check for filter
      parentCatalog = (InvCatalogImpl) getParentCatalog();
      DatasetFilter filter = parentCatalog.getDatasetFilter();
      if (filter != null)
        cat.filter(filter);

      proxy = cat.getDataset();
      if (proxy.getMark()) {
        proxy.setName(proxy.getName() + " (EMPTY)");
        proxy.addProperty(new InvProperty("isEmpty", "true"));
        proxy.finish();
      }

      String name = getName().trim();
      String proxyName = proxy.getName().trim();

      useProxy = proxyName.equals(name) && !(proxy instanceof InvCatalogRef);
      if (debugProxy) System.out.println("catRefname=" + name + "=topName=" + proxyName + "=" + useProxy);
      //if (useProxy) {
      //for (int i=0; i<docs.size(); i++)
      //  proxy.addDocumentation( (InvDocumentation) docs.get(i));
      //proxy.finish();

      /*List proxyData = proxy.getDatasets();
     for (int i=0; i<proxyData.size(); i++) {
       InvDatasetImpl ds = (InvDatasetImpl) proxyData.get(i);
       ds.setViewParent( this); // for treeview
     } */
      //}
    }

    // make proxy the top dataset
    datasets.add(proxy);

    //proxy.setViewParent( this); // for treeview

    /* all this rigamorole to send an event when reference is read.
    if (parentCatalog != null) {
      // get topmost catalog
      InvCatalogImpl top = parentCatalog.getTopCatalog();
      cat.setTopCatalog(top);

      // may not be on the awt event thread, so need to do invokeLater
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          InvCatalogImpl top = ((InvCatalogImpl)getParentCatalog()).getTopCatalog();
          top.firePropertyChangeEvent(
              new java.beans.PropertyChangeEvent(top, "InvCatalogRefInit", null, this));
        }
      });
    } */

    init = true;
  }

  /**
   * Read the referenced catalog asynchronously, if the catalog factory supports it.
   * If it doesnt, this method will work equivilently to read(), which is called the first time
   * getDatasets() is called. If the catalog is already read in, the callback will be called
   * immediately, before this method exits.
   *
   * @param factory : use this catalog factory
   * @param caller  when catalog is read
   * @see CatalogSetCallback
   */
  public synchronized void readAsynch(InvCatalogFactory factory, CatalogSetCallback caller) {
    if (init) {
      caller.setCatalog((InvCatalogImpl) getParentCatalog());
      return;
    }

    // may be reletive
    String hrefResolved;
    try {
      java.net.URI uri = getParentCatalog().resolveUri(href);
      hrefResolved = uri.toString();
    }
    catch (java.net.URISyntaxException e) {
      // this is to display an error message
      proxy = new InvDatasetImpl(null, "HREF ERROR");
      if (debug)
        System.out.println("HREF ERROR =\n  " + href + " err= " + e.getMessage());
      proxy.addProperty(new InvProperty("HREF ERROR", href));
      datasets.add(proxy);
      return;
    }

    // open and read the referenced catalog XML asynchronously
    // setCatalog will be called when ready
    try {
      if (debug) System.out.println(" InvCatalogRef readXMLasynch " + getFullName() +
          "  hrefResolved = " + hrefResolved);
      factory.readXMLasynch(hrefResolved, new Callback(caller));
    } catch (Exception e) {
      // this is to display an error message
      proxy = new InvDatasetImpl(null, "HREF ERROR");
      if (debug)
        System.out.println("HREF ERROR =\n  " + href + " err= " + e.getMessage());
      proxy.addProperty(new InvProperty("HREF ERROR", href));
      datasets.add(proxy);
    }
  }

  private class Callback implements CatalogSetCallback {
    CatalogSetCallback caller;

    Callback(CatalogSetCallback caller) {
      this.caller = caller;
    }

    public void setCatalog(InvCatalogImpl cat) {
      if (debugAsynch)
        System.out.println(" setCatalog was called");
      finishCatalog(cat);
      caller.setCatalog(cat);
    }

    public void failed() {
      if (debugAsynch)
        System.out.println(" setCatalog failed");
      caller.failed();
    }
  }

  boolean check(StringBuilder out, boolean show) {
    return isRead() ? proxy.check(out, show) : super.check(out, show);
  }

  /* public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof InvCatalogRef)) return false;
    InvCatalogRef invCatalogRef = (InvCatalogRef) o;

    if (href != null ? !href.equals(invCatalogRef.href) : invCatalogRef.href != null) return false;
    // Add the name comparison from super.
    if (name != null ? !name.equals(invCatalogRef.name) : invCatalogRef.name != null) return false;

    return true;
  }

  public int hashCode() {
    //int result = super.hashCode();
    int result = 17;
    result = 29 * result + (href != null ? href.hashCode() : 0);
    result = 29 * result + (name != null ? name.hashCode() : 0);
    return result;
  }  */

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    // if (!super.equals(o)) return false;

    InvCatalogRef that = (InvCatalogRef) o;

    if (href != null ? !href.equals(that.href) : that.href != null) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + (href != null ? href.hashCode() : 0);
    result = 29 * result + (name != null ? name.hashCode() : 0);
    return result;
  }

  //// proxy
  @Override
  public thredds.catalog.InvDatasetImpl findDatasetByName(java.lang.String p0) {
    return !useProxy ? super.findDatasetByName(p0) : proxy.findDatasetByName(p0);
  }

  @Override
  public java.lang.String findProperty(java.lang.String p0) {
    return !useProxy ? super.findProperty(p0) : proxy.findProperty(p0);
  }

  @Override
  public thredds.catalog.InvService findService(java.lang.String p0) {
    return !useProxy ? super.findService(p0) : proxy.findService(p0);
  }

  @Override
  public thredds.catalog.InvAccess getAccess(thredds.catalog.ServiceType p0) {
    return !useProxy ? super.getAccess(p0) : proxy.getAccess(p0);
  }

  @Override
  public java.util.List<InvAccess> getAccess() {
    return !useProxy ? super.getAccess() : proxy.getAccess();
  }

  @Override
  public java.lang.String getAlias() {
    return !useProxy ? super.getAlias() : proxy.getAlias();
  }

  @Override
  public String getAuthority() {
    return useProxy ? proxy.getAuthority() : super.getAuthority();
  }

  @Override
  public thredds.catalog.CollectionType getCollectionType() {
    return !useProxy ? super.getCollectionType() : proxy.getCollectionType();
  }

  @Override
  public java.util.List<ThreddsMetadata.Contributor> getContributors() {
    return !useProxy ? super.getContributors() : proxy.getContributors();
  }

  @Override
  public java.util.List<ThreddsMetadata.Source> getCreators() {
    return !useProxy ? super.getCreators() : proxy.getCreators();
  }

  @Override
  public thredds.catalog.DataFormatType getDataFormatType() {
    return !useProxy ? super.getDataFormatType() : proxy.getDataFormatType();
  }

  @Override
  public FeatureType getDataType() {
    return !useProxy ? super.getDataType() : proxy.getDataType();
  }

  @Override
  public java.util.List<DateType> getDates() {
    return !useProxy ? super.getDates() : proxy.getDates();
  }

  @Override
  public java.util.List<InvDocumentation> getDocumentation() {
    return !useProxy ? super.getDocumentation() : proxy.getDocumentation();
  }

  @Override
  public java.lang.String getDocumentation(java.lang.String p0) {
    return !useProxy ? super.getDocumentation(p0) : proxy.getDocumentation(p0);
  }

  @Override
  public java.lang.String getFullName() {
    return !useProxy ? super.getFullName() : proxy.getFullName();
  }

  @Override
  public thredds.catalog.ThreddsMetadata.GeospatialCoverage getGeospatialCoverage() {
    return !useProxy ? super.getGeospatialCoverage() : proxy.getGeospatialCoverage();
  }

  @Override
  public java.lang.String getID() {
    return !useProxy ? super.getID() : proxy.getID();
  }

  @Override
  public java.util.List<ThreddsMetadata.Vocab> getKeywords() {
    return !useProxy ? super.getKeywords() : proxy.getKeywords();
  }

  @Override
  protected boolean getMark() {
    return !useProxy ? super.getMark() : proxy.getMark();
  }

  @Override
  public java.util.List<InvMetadata> getMetadata(thredds.catalog.MetadataType p0) {
    return !useProxy ? super.getMetadata(p0) : proxy.getMetadata(p0);
  }

  @Override
  public java.util.List<InvMetadata> getMetadata() {
    return !useProxy ? super.getMetadata() : proxy.getMetadata();
  }

  @Override
  public java.lang.String getName() {
    if (!useProxy) return super.getName();
    return (proxy == null) ? "N/A" : proxy.getName();
  }

  @Override
  public thredds.catalog.InvDataset getParent() {
    return !useProxy ? super.getParent() : proxy.getParent();
  }

  /* LOOK
  public thredds.catalog.InvCatalog getParentCatalog() {
    return !useProxy ? super.getParentCatalog() : proxy.getParentCatalog();
  } */

  @Override
  public java.util.List<ThreddsMetadata.Vocab> getProjects() {
    return !useProxy ? super.getProjects() : proxy.getProjects();
  }

  @Override
  public java.util.List<InvProperty> getProperties() {
    return !useProxy ? super.getProperties() : proxy.getProperties();
  }

  @Override
  public java.util.List<ThreddsMetadata.Source> getPublishers() {
    return !useProxy ? super.getPublishers() : proxy.getPublishers();
  }

  @Override
  public thredds.catalog.InvService getServiceDefault() {
    return !useProxy ? super.getServiceDefault() : proxy.getServiceDefault();
  }

  @Override
  public CalendarDateRange getCalendarDateCoverage() {
    return !useProxy ? super.getCalendarDateCoverage() : proxy.getCalendarDateCoverage();
  }

  @Override
  public java.lang.String getUniqueID() {
    return !useProxy ? super.getUniqueID() : proxy.getUniqueID();
  }

  @Override
  public java.lang.String getUrlPath() {
    return !useProxy ? super.getUrlPath() : proxy.getUrlPath();
  }

  @Override
  public java.lang.Object getUserProperty(java.lang.Object p0) {
    return !useProxy ? super.getUserProperty(p0) : proxy.getUserProperty(p0);
  }

  @Override
  public java.util.List<ThreddsMetadata.Variables> getVariables() {
    return !useProxy ? super.getVariables() : proxy.getVariables();
  }

  @Override
  public String getVariableMapLink() {
    return !useProxy ? super.getVariableMapLink() : proxy.getVariableMapLink();
  }

  @Override
  public boolean hasAccess() {
    return !useProxy ? super.hasAccess() : proxy.hasAccess();
  }

  @Override
  public boolean hasNestedDatasets() {
    return useProxy ? proxy.hasNestedDatasets() : true;
  }

  @Override
  public boolean isHarvest() {
    return !useProxy ? super.isHarvest() : proxy.isHarvest();
  }

}

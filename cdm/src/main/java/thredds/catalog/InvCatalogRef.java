// $Id: InvCatalogRef.java 48 2006-07-12 16:15:40Z caron $
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

import thredds.datatype.DateRange;

import java.net.URI;

/**
 * A reference to a InvCatalog. The referenced catalog is not read until getDatasets() is called.
 * A client will see the referenced catalog as a nested dataset.
 *
 * <p>
 * The client can also do asynchronous reading, if the InvCatalogFactory supports it,
 *  and if readAsynch() is used.
 *
 * <pre>
 * Parent relationship:
 *   ds -> catRef -- catalog
 *                      ^ top  -> ds ...
 *
 *  ParentView relationship:
 *   ds -> catRef -> top  -> ds ...
 *           (or) -> ds                 if UseProxy
 *
 * </pre>
 * @see InvDatasetImpl for API, thredds.catalog.ui.CatalogTreeView as example to read asynchronously
 *
 * @author john caron
 * @version $Revision: 48 $ $Date: 2006-07-12 16:15:40Z $
 */

public class InvCatalogRef extends InvDatasetImpl {
  private String href;
  private InvDatasetImpl proxy = null; // top dataset of referenced catalog
  private URI uri = null;
  private String errMessage = null;

  private boolean init = false, useProxy = false;
  private boolean debug = false, debugProxy = false, debugAsynch = false;

  /**
   * Constructor.
   * @param parent : parent dataset
   * @param title : display name of collection
   * @param href : URL to another catalog
   */
  public InvCatalogRef( InvDatasetImpl parent, String title, String href) {
    super( parent, title);
    this.href = href.trim();
  }

    /** get Xlink Href, as a String, unresolved */
  public String getXlinkHref() { return href; }

    /** get Xlink reference as a URI, resolved */
  public URI getURI() {
    if (uri != null) return uri;

    // may be reletive
    try {
      return getParentCatalog().resolveUri(href);
    }
    catch (java.net.URISyntaxException e) {
      errMessage = "URISyntaxException on url  " + href + " = " + e.getMessage();
      return null;
    }
  }

    /** Get Datasets. This triggers a read of the referenced catalog the first time its called.*/
  public java.util.List getDatasets() {
    read();
    return useProxy ? proxy.getDatasets() : super.getDatasets();
  }

  /** If the referenced catalog has been read */
  public boolean isRead() { return init; }

  /** Return top dataset of referenced catalog.
   *  This triggers a read of the referenced catalog the first time its called.
   **/
  public InvDatasetImpl getProxyDataset() {
    read();
    return proxy;
  }

  /** Release resources - undo the read of the catalog. This is needed when crawling large catalogs.
   * For modest catalogs that you will repeatedly examine, do not use this method. */
  public void release() {
    datasets = new java.util.ArrayList();
    proxy = null;
    init = false;
  }


  //////////////////////////////////////////////
  public boolean finish() {
    return super.finish( );
  }

  private synchronized void read() {
    if (init)
      return;

    URI uriResolved = getURI();
    if (uriResolved == null) {
      // this is to display an error message
      proxy = new InvDatasetImpl(null, "HREF ERROR");
      if (debug)
        System.out.println(errMessage);
      proxy.addProperty(new InvProperty("HREF ERROR", errMessage));
      datasets.add(proxy);
      init = true;
    }

    // open and read the referenced catalog XML
    try {
      if (debug)
        System.out.println(" InvCatalogRef read " + getFullName()+"  hrefResolved = " + uriResolved);

      InvCatalogFactory factory = ((InvCatalogImpl)getParentCatalog()).getCatalogFactory();
      InvCatalogImpl cat = factory.readXML(uriResolved.toString());
      finishCatalog( cat);

    } catch (Exception e) {
      // this is to display an error message
      proxy = new InvDatasetImpl(null, "HREF ERROR");
      if (debug)
        System.out.println("HREF ERROR =\n  " + href + " err= " + e.getMessage());
      proxy.addProperty(new InvProperty("HREF ERROR", href));
      datasets.add(proxy);
      init = true;
      return;
    }
  }

  private void finishCatalog( InvCatalogImpl cat) {
    InvCatalogImpl parentCatalog = null;
    if (cat.hasFatalError()) {
      // this is to display an error message
      proxy = new InvDatasetImpl( null, "ERROR OPENING");
      StringBuffer out = new StringBuffer();
      cat.check( out);
      if (debug) System.out.println( "PARSE ERROR =\n  "+out.toString());
      proxy.addProperty( new InvProperty( "ERROR OPENING", out.toString()));
      proxy.finish();

    } else {
      // check for filter
      parentCatalog = (InvCatalogImpl) getParentCatalog();
      DatasetFilter filter = parentCatalog.getDatasetFilter();
      if (filter != null)
        cat.filter( filter);

      proxy = (InvDatasetImpl) cat.getDataset();
      if (proxy.getMark()) {
        proxy.setName(proxy.getName() + " (EMPTY)");
        proxy.addProperty( new InvProperty("isEmpty", "true"));
        proxy.finish();
      }

      String name = getName().trim();
      String proxyName = proxy.getName().trim();

      useProxy = proxyName.equals( name) && !(proxy instanceof InvCatalogRef);
      if (debugProxy) System.out.println("catRefname="+name+"=topName="+proxyName+"="+useProxy);
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
    datasets.add( proxy);
    
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
   *  getDatasets() is called. If the catalog is already read in, the callback will be called
   *  immediately, before this method exits.
   * @param factory : use this catalog factory
   * @param caller when catalog is read
   * @see CatalogSetCallback
   */
  public synchronized void readAsynch( InvCatalogFactory factory, CatalogSetCallback caller) {
    if (init) {
      caller.setCatalog( (InvCatalogImpl) getParentCatalog());
      return;
    }

    // may be reletive
    String hrefResolved = null;
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
      factory.readXMLasynch(hrefResolved, new Callback( caller));
    } catch (Exception e) {
      // this is to display an error message
      proxy = new InvDatasetImpl(null, "HREF ERROR");
      if (debug)
        System.out.println("HREF ERROR =\n  " + href + " err= " + e.getMessage());
      proxy.addProperty(new InvProperty("HREF ERROR", href));
      datasets.add(proxy);
      return;
    }
  }

  private class Callback implements CatalogSetCallback {
    CatalogSetCallback caller;
    Callback( CatalogSetCallback caller) {
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

  boolean check(StringBuffer out, boolean show) {
    return isRead() ? proxy.check(out, show) : super.check(out, show);
  }

  public boolean equals( Object o )
  {
    if ( this == o ) return true;
    if ( !( o instanceof InvCatalogRef ) ) return false;
    // Comment out the super comparison as it is the problem.
    //if ( !super.equals( o ) ) return false;

    final InvCatalogRef invCatalogRef = (InvCatalogRef) o;

    if ( href != null ? !href.equals( invCatalogRef.href ) : invCatalogRef.href != null ) return false;
    // Add the name comparison from super.
    if ( name != null ? !name.equals( invCatalogRef.name ) : invCatalogRef.name != null ) return false;

    return true;
  }

  public int hashCode()
  {
    //int result = super.hashCode();
    int result = 17;
    result = 29 * result + ( href != null ? href.hashCode() : 0 );
    result = 29 * result + ( name != null ? name.hashCode() : 0 );
    return result;
  }

  //// proxy
  public thredds.catalog.InvDatasetImpl findDatasetByName(java.lang.String p0) {
    return !useProxy ? super.findDatasetByName(p0) : proxy.findDatasetByName( p0);
  }

  public java.lang.String findProperty(java.lang.String p0) {
    return !useProxy ? super.findProperty(p0) : proxy.findProperty( p0);
  }

  public thredds.catalog.InvService findService(java.lang.String p0) {
    return !useProxy ? super.findService(p0) : proxy.findService( p0);
  }

  public thredds.catalog.InvAccess getAccess(thredds.catalog.ServiceType p0) {
    return !useProxy ? super.getAccess(p0) : proxy.getAccess( p0);
  }

  public java.util.List getAccess() {
    return !useProxy ? super.getAccess() : proxy.getAccess();
  }

  public java.lang.String getAlias() {
    return !useProxy ? super.getAlias() : proxy.getAlias();
  }

  public String getAuthority() {
    return useProxy ? proxy.getAuthority() : super.getAuthority();
  }

  public thredds.catalog.CollectionType getCollectionType() {
    return!useProxy ? super.getCollectionType() :  proxy.getCollectionType();
  }

  public java.util.List getContributors() {
    return !useProxy ? super.getContributors() : proxy.getContributors();
  }

  public java.util.List getCreators() {
    return !useProxy ? super.getCreators() : proxy.getCreators();
  }

  public thredds.catalog.DataFormatType getDataFormatType() {
    return !useProxy ? super.getDataFormatType() : proxy.getDataFormatType();
  }

  public thredds.catalog.DataType getDataType() {
    return !useProxy ? super.getDataType() : proxy.getDataType();
  }

  public java.util.List getDates() {
    return !useProxy ? super.getDates() : proxy.getDates();
  }

  public java.util.List getDocumentation() {
    return !useProxy ? super.getDocumentation() : proxy.getDocumentation();
  }

  public java.lang.String getDocumentation(java.lang.String p0) {
    return !useProxy ? super.getDocumentation(p0) : proxy.getDocumentation( p0);
  }

  public java.lang.String getFullName() {
    return !useProxy ? super.getFullName() : proxy.getFullName();
  }

  public thredds.catalog.ThreddsMetadata.GeospatialCoverage getGeospatialCoverage() {
    return !useProxy ? super.getGeospatialCoverage() : proxy.getGeospatialCoverage();
  }

  public java.lang.String getID() {
    return !useProxy ? super.getID() : proxy.getID();
  }

  public java.util.List getKeywords() {
    return !useProxy ? super.getKeywords() : proxy.getKeywords();
  }

  protected boolean getMark() {
    return !useProxy ? super.getMark() : proxy.getMark();
  }

  public java.util.List getMetadata(thredds.catalog.MetadataType p0) {
    return !useProxy ? super.getMetadata(p0) : proxy.getMetadata( p0);
  }

  public java.util.List getMetadata() {
    return !useProxy ? super.getMetadata() : proxy.getMetadata();
  }

  public java.lang.String getName() {
    return !useProxy ? super.getName() : proxy.getName();
  }

  public thredds.catalog.InvDataset getParent() {
    return !useProxy ? super.getParent() : proxy.getParent();
  }

  /* LOOK
  public thredds.catalog.InvCatalog getParentCatalog() {
    return !useProxy ? super.getParentCatalog() : proxy.getParentCatalog();
  } */

  public java.util.List getProjects() {
    return !useProxy ? super.getProjects() : proxy.getProjects();
  }

  public java.util.List getProperties() {
    return !useProxy ? super.getProperties() : proxy.getProperties();
  }

  public java.util.List getPublishers() {
    return !useProxy ? super.getPublishers() : proxy.getPublishers();
  }

  public thredds.catalog.InvService getServiceDefault() {
    return !useProxy ? super.getServiceDefault() : proxy.getServiceDefault();
  }

  public DateRange getTimeCoverage() {
    return !useProxy ? super.getTimeCoverage() : proxy.getTimeCoverage();
  }

  public java.lang.String getUniqueID() {
    return !useProxy ? super.getUniqueID() : proxy.getUniqueID();
  }

  public java.lang.String getUrlPath() {
    return !useProxy ? super.getUrlPath() : proxy.getUrlPath();
  }

  public java.lang.Object getUserProperty(java.lang.Object p0) {
    return !useProxy ? super.getUserProperty(p0) : proxy.getUserProperty( p0);
  }

  public java.util.List getVariables() {
    return  !useProxy ? super.getVariables() :  proxy.getVariables();
  }

  public boolean hasAccess() {
    return !useProxy ? super.hasAccess() : proxy.hasAccess();
  }

  public boolean hasNestedDatasets() {
    return useProxy ? proxy.hasNestedDatasets() : true;
  }

  public boolean isHarvest() {
    return !useProxy ? super.isHarvest() : proxy.isHarvest();
  }

}

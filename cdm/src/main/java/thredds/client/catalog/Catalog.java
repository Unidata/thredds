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

import org.jdom2.Namespace;
import thredds.client.catalog.builder.AccessBuilder;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.CatalogRefBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.URLnaming;

import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Client Catalog
 *
 * @author caron
 * @since 1/7/2015
 */
@Immutable
public class Catalog extends DatasetNode {
  static public final String CATALOG_NAMESPACE_10 = "http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0";
  static public final Namespace defNS = Namespace.getNamespace(CATALOG_NAMESPACE_10);
  static public final String NJ22_NAMESPACE = "http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2";
  static public final Namespace ncmlNS = Namespace.getNamespace("ncml", NJ22_NAMESPACE);
  static public final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";
  static public final Namespace xlinkNS = Namespace.getNamespace("xlink", XLINK_NAMESPACE);
  static public final Namespace xsiNS = Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");

  // all of these are catalog only
  // public static final String CatalogScan = "CatalogScan";       // List<CatalogScan>
  public static final String DatasetHash = "DatasetHash";       // Map<String,Dataset>
  public static final String DatasetRoots = "DatasetRoots";     // List<DatasetRootConfig>
  public static final String Expires = "Expires";               // CalendarDate
  public static final String Services = "Services";             // List<Service>  (LOOK what about using Map ?)
  public static final String Version = "Version";               // String

  //////////////////////////////////////////////////////////////////////////////////////////
  private final URI baseURI;   // LOOK its possible we never want to use this. perhaps "location" instead ??

  public Catalog(URI baseURI, String name, Map<String, Object> flds, List<DatasetBuilder> datasets) {
    super(null, name, flds, datasets);
    this.baseURI = baseURI;

    Map<String, Dataset> datasetMap = new HashMap<>();
    addDatasetsToHash(getDatasetsLocal(), datasetMap);
    if (!datasetMap.isEmpty())
      flds.put(Catalog.DatasetHash, datasetMap);
  }

  private void addDatasetsToHash(List<Dataset> datasets, Map<String, Dataset> datasetMap) {
    if (datasets == null) return;
    for (Dataset ds : datasets) {
      String id = ds.getIdOrPath();
      if (id != null) datasetMap.put(id, ds);
      if (ds instanceof CatalogRef) continue; // dont recurse into catrefs
      addDatasetsToHash(ds.getDatasetsLocal(), datasetMap);
    }
  }

  public URI getBaseURI() {
    return baseURI;
  }

  public CalendarDate getExpires() {
    return (CalendarDate) flds.get(Catalog.Expires);
  }

  public String getVersion() {
    return (String) flds.get(Catalog.Version);
  }

  public List<Service> getServices() {
    List<Service> services = (List<Service>) flds.get(Catalog.Services);
    return services == null ? new ArrayList<>(0) : services;
  }

  public boolean hasService(String name) {
    for (Service s : getServices())
      if (s.getName().equalsIgnoreCase(name)) return true;
    return false;
  }

  public Service findService(String serviceName)  {
    if (serviceName == null) return null;
    List<Service> services = (List<Service>) flds.get(Catalog.Services);
    return findService(services, serviceName);
  }

  public Service findService(ServiceType type)  {
    if (type == null) return null;
    List<Service> services = (List<Service>) flds.get(Catalog.Services);
    return findService(services, type);
  }

  private Service findService(List<Service> services, String want)  {
    if (services == null) return null;
    for (Service s : services) {
      if (s.getName().equals(want)) return s;
    }
    for (Service s : services) {
      Service result = findService(s.getNestedServices(), want);
      if (result != null) return result;
    }
    return null;
  }

  private Service findService(List<Service> services, ServiceType type)  {
    if (services == null) return null;
    for (Service s : services) {
      if (s.getType() == type) return s;
    }
    for (Service s : services) {
      Service result = findService(s.getNestedServices(), type);
      if (result != null) return result;
    }
    return null;
  }

  public List<Property> getProperties() {
    List<Property> properties = (List<Property>) flds.get(Dataset.Properties);
    return properties == null ? new ArrayList<>(0) : properties;
  }

  public Dataset findDatasetByID( String id) {
    Map<String, Dataset> datasetMap = (Map<String, Dataset>) flds.get(Catalog.DatasetHash);
    return datasetMap == null ? null : datasetMap.get(id);
  }

  // get all datasets contained directly in this catalog
  public Iterable<Dataset> getAllDatasets() {
    List<Dataset> all = new ArrayList<>();
    addAll(this, all);
    return all;
  }

  private void addAll(DatasetNode node, List<Dataset> all) {
    all.addAll(node.getDatasetsLocal());
    for (DatasetNode nested : node.getDatasetsLocal())
      addAll(nested, all);
  }

  /**
   * Resolve reletive URIs, using the catalog's base URI. If the uriString is not reletive, then
   * no resolution is done. This also allows baseURI to be a file: scheme.
   *
   * @param uriString any url, reletive or absolute
   * @return resolved url string, or null on error
   * @throws java.net.URISyntaxException if uriString violates RFC 2396
   * @see java.net.URI#resolve
   */
  public URI resolveUri(String uriString) throws URISyntaxException {
    if (baseURI == null) return new URI(uriString);
    String resolved = URLnaming.resolve(baseURI.toString(), uriString);
    return new URI(resolved);
  }

  // look is this different than URLnaming ??
  public static URI resolveUri(URI baseURI, String uriString) throws URISyntaxException {
    URI want = new URI(uriString);
    if ((baseURI == null) || want.isAbsolute())
      return want;

    // gotta deal with file ourself
    String scheme = baseURI.getScheme();
    if ((scheme != null) && scheme.equals("file")) {
      String baseString = baseURI.toString();
      if ((uriString.length() > 0) && (uriString.charAt(0) == '#'))
        return new URI(baseString + uriString);
      int pos = baseString.lastIndexOf('/');
      if (pos > 0) {
        String r = baseString.substring(0, pos + 1) + uriString;
        return new URI(r);
      }
    }

    //otherwise let the URI class resolve it
    return baseURI.resolve(want);
  }

  public String getUriString() {
    URI baseURI = getBaseURI();
    return baseURI == null ? null : baseURI.toString();
  }

  //////////////////////////////////////////////////////////////////////////////////
  // from DeepCopyUtils, for subsetting

  public Catalog subsetCatalogOnDataset( Dataset dataset) {
    if ( dataset == null ) throw new IllegalArgumentException( "Dataset may not be null." );
    if ( dataset.getParentCatalog() != this ) throw new IllegalArgumentException( "Catalog must contain the dataset." );

    CatalogBuilder builder = new CatalogBuilder();

    URI docBaseUri = formDocBaseUriForSubsetCatalog( dataset );
    builder.setBaseURI(docBaseUri);
    builder.setName( dataset.getName());

    List<Service> neededServices = new ArrayList<>();
    DatasetBuilder topDs = copyDataset( null, dataset, neededServices, true );  // LOOK, cant set catalog as datasetNode parent

    for (Service s : neededServices)
      builder.addService(s);

    builder.addDataset( topDs );

    return builder.makeCatalog();
  }

  private DatasetBuilder copyDataset( DatasetBuilder parent, Dataset dataset, List<Service> neededServices, boolean copyInherited ) {

    neededServices.add(dataset.getServiceDefault());

    DatasetBuilder result;

    if ( dataset instanceof CatalogRef ) {
      CatalogRef catRef = (CatalogRef) dataset;
      CatalogRefBuilder catBuilder = new CatalogRefBuilder( parent);
      catBuilder.setHref( catRef.getXlinkHref());
      catBuilder.setTitle( catRef.getName());
      result = catBuilder;

    } else {
      result =  new DatasetBuilder(parent);

      List<Access> access = dataset.getLocalFieldAsList(Dataset.Access);  // dont expand
      for ( Access curAccess : access) {
        result.addAccess( copyAccess( result, curAccess, neededServices ));
      }

      List<Dataset> datasets = dataset.getLocalFieldAsList(Dataset.Datasets);  // dont expand
      for ( Dataset currDs : datasets) {
        result.addDataset( copyDataset( result, currDs, neededServices, copyInherited ));
      }
    }

    result.setName( dataset.getName() );
    result.transferMetadata(dataset, copyInherited);  // make a copy of all local metadata
    return result;
  }

  private AccessBuilder copyAccess( DatasetBuilder parent, Access access, List<Service> neededServices ) {
    neededServices.add(access.getService());  // LOOK may get dups
    return new AccessBuilder( parent, access.getUrlPath(), access.getService(), access.getDataFormatName(), access.getDataSize() );
  }

  private URI formDocBaseUriForSubsetCatalog( Dataset dataset ) {
    String catDocBaseUri = getUriString();
    String subsetDocBaseUriString = catDocBaseUri + "/" + ( dataset.getID() != null ? dataset.getID() : dataset.getName() );
    try {
      return new URI( subsetDocBaseUriString);
    } catch ( URISyntaxException e ) {   // This shouldn't happen. But just in case ...
      throw new IllegalStateException( "Bad document Base URI for new catalog [" + catDocBaseUri + "/" + (dataset.getID() != null ? dataset.getID() : dataset.getName()) + "].", e );
    }
  }



}

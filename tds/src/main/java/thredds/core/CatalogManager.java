/* Copyright */
package thredds.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import thredds.client.catalog.Catalog;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.Service;
import thredds.client.catalog.builder.CatalogBuilder;
import thredds.client.catalog.builder.DatasetBuilder;
import thredds.featurecollection.FeatureCollectionCache;
import thredds.featurecollection.InvDatasetFeatureCollection;
import thredds.server.catalog.CatalogScan;
import thredds.server.catalog.ConfigCatalog;
import thredds.server.catalog.ConfigCatalogCache;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.builder.ConfigCatalogBuilder;
import thredds.server.config.TdsContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides an API to find a catalog from its path. Handles static and dynamic catalogs.
 *
 * @author caron
 * @since 6/7/2015
 */
@Component("CatalogManager")
public class CatalogManager {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CatalogManager.class);

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private ConfigCatalogCache ccc;

  @Autowired
  private FeatureCollectionCache featureCollectionCache;

  @Autowired
  private DataRootManager dataRootManager;

  @Autowired
  private AllowedServices globalServices;

  ///////////////////////////////////////////////////////////

  /**
   * If a catalog exists and is allowed (not filtered out) for the given path, return
   * the catalog as an Catalog. Otherwise, return null.
   * <p/>
   * The validity of the returned catalog is not guaranteed. Use Catalog.check() to
   * check that the catalog is valid.
   *
   * @param path    the path for the requested catalog.
   * @param baseURI the base URI for the catalog, used to resolve relative URLs.
   * @return the requested Catalog, or null if catalog does not exist or is not allowed.
   */
  public Catalog getCatalog(String path, URI baseURI) throws IOException {
    if (path == null)
      return null;

    String workPath = path;
    if (workPath.startsWith("/"))
      workPath = workPath.substring(1);

    // Check if its a dataRoot.
    Object dyno = makeDynamicCatalog(workPath, baseURI);
    if (dyno != null) {
      if (dyno instanceof CatalogBuilder) {
        CatalogBuilder catBuilder = (CatalogBuilder) dyno;
        addGlobalServices(catBuilder);
        return catBuilder.makeCatalog();
      } else {
        ConfigCatalog configCatalog = (ConfigCatalog) dyno;
        return addGlobalServices(configCatalog);
      }
    }

    // check cache and read if needed
    ConfigCatalog configCatalog = ccc.get(workPath);
    if (configCatalog == null) return null;
    return addGlobalServices(configCatalog);
  }

  // barfola on the return type
  private Object makeDynamicCatalog(String path, URI baseURI) throws IOException {
  // private CatalogBuilder makeDynamicCatalog(String path, URI baseURI) throws IOException {

    // Make sure this is a dynamic catalog request.
    //if (!path.endsWith("catalog.xml") && !path.endsWith("/latest.xml"))
    //  return null;

    // strip off the filename
    int pos = path.lastIndexOf("/");
    String workPath = (pos >= 0) ? path.substring(0, pos) : path;
    String filename = (pos > 0) ? path.substring(pos+1) : path;

    // now look through the data roots for a maximal match
    DataRootManager.DataRootMatch match = dataRootManager.findDataRootMatch(workPath);
    if (match == null)
      return null;

    // Feature Collection
    if (match.dataRoot.getFeatureCollection() != null) {
      InvDatasetFeatureCollection fc = featureCollectionCache.get(match.dataRoot.getFeatureCollection());

      boolean isLatest = path.endsWith("/latest.xml");
      if (isLatest)
        return fc.makeLatest(match.remaining, path, baseURI);
      else
        return fc.makeCatalog(match.remaining, path, baseURI);
    }

    // if (path.endsWith("/latest.xml")) return null; // latest is not handled here  LOOK are you sure ??

    // DatasetScan
    DatasetScan dscan = match.dataRoot.getDatasetScan();
    if (dscan != null) {
      if (log.isDebugEnabled()) log.debug("makeDynamicCatalog(): Calling DatasetScan.makeCatalogForDirectory( " + baseURI + ", " + path + ").");
      CatalogBuilder cat = dscan.makeCatalogForDirectory(workPath, baseURI);

      if (null == cat)
        log.error("makeDynamicCatalog(): DatasetScan.makeCatalogForDirectory failed = " + workPath);

      return cat;
    }

    // CatalogScan
    CatalogScan catScan = match.dataRoot.getCatalogScan();
    if (catScan != null) {
      if (!filename.equalsIgnoreCase(CatalogScan.CATSCAN)) { // its an actual catalog
        return catScan.getCatalog(tdsContext.getContentDirectory(), match.remaining, filename, ccc);
      }

      if (log.isDebugEnabled()) log.debug("makeDynamicCatalog(): Calling CatalogScan.makeCatalogForDirectory( " + baseURI + ", " + path + ").");
      CatalogBuilder cat = catScan.makeCatalogFromDirectory(tdsContext.getContentDirectory(), match.remaining, baseURI);

      if (null == cat)
        log.error("makeDynamicCatalog(): CatalogScan.makeCatalogForDirectory failed = " + workPath);

      return cat;
    }

    log.warn("makeDynamicCatalog() failed for =" + workPath + " request path= " + path);
    return null;
  }

  /////////////////////////////////////////////////////
  // rigamorole to modify invariant catalogs; we may need to add global services

  private Catalog addGlobalServices(ConfigCatalog cat) {
    Set<String> serviceNames = new HashSet<>();
    for (Dataset ds : cat.getDatasets())
      findServices(ds, serviceNames);
    if (serviceNames.isEmpty()) return cat;

    List<Service> services = new ArrayList<>(cat.getServices());
    for (String name : serviceNames) {
      if (cat.hasService(name)) continue;
      Service s = globalServices.findService(name);
      if (s != null) services.add(s);
    }
    if (services.isEmpty()) return cat;

    return ConfigCatalogBuilder.makeCatalogWithServices(cat, services);
  }

  private void findServices(Dataset ds, Set<String> serviceNames) {
    String sname = (String) ds.get(Dataset.ServiceName);
    if (sname != null)
      serviceNames.add(sname);
    for (Dataset nested : ds.getDatasets())
      findServices(nested, serviceNames);
  }

  //////////////////////////
  private void addGlobalServices(CatalogBuilder cat) {
    Set<String> serviceNames = new HashSet<>();
    for (DatasetBuilder ds : cat.getDatasets())
      findServices(ds, serviceNames);
    if (serviceNames.isEmpty()) return;

    Set<Service> services = new HashSet<>();
    for (String name : serviceNames) {
      if (cat.hasService(name)) continue;
      Service s = globalServices.findService(name);
      if (s != null) services.add(s);
    }
    if (services.isEmpty()) return;

    for (Service s : services)
      cat.addService(s);
  }

  private void findServices(DatasetBuilder ds, Set<String> serviceNames) {
    String sname = (String) ds.get(Dataset.ServiceName);
    if (sname != null)
      serviceNames.add(sname);
    for (DatasetBuilder nested : ds.getDatasets())
      findServices(nested, serviceNames);
  }

}

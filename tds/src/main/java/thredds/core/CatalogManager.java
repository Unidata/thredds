/* Copyright */
package thredds.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import thredds.client.catalog.Catalog;
import thredds.featurecollection.FeatureCollectionCache;
import thredds.featurecollection.InvDatasetFeatureCollection;
import thredds.server.catalog.CatalogScan;
import thredds.server.catalog.DatasetScan;

import java.io.IOException;
import java.net.URI;

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
  private ConfigCatalogCache ccc;

  @Autowired
  private FeatureCollectionCache featureCollectionCache;

  @Autowired
  private DataRootManager dataRootManager;

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

    // check cache for quick hit
    Catalog catalog = ccc.getIfPresent(workPath);
    if (catalog != null) return catalog;

    // Check if its a dataRoot.
    catalog = makeDynamicCatalog(workPath, baseURI);
    if (catalog != null) return catalog;

    // check cache and read if needed
    catalog = ccc.get(workPath);

    /* its a static catalog that needs to be read
    if (reread) {
      File catFile = this.tdsContext.getConfigFileSource().getFile(workPath);
      if (catFile != null) {
        String catalogFullPath = catFile.getPath();
        logCatalogInit.info("**********\nReading catalog {} at {}\n", catalogFullPath, CalendarDate.present());

        InvCatalogFactory factory = getCatalogFactory(true);
        Catalog reReadCat = readCatalog(factory, workPath, catalogFullPath);

        if (reReadCat != null) {
          catalog = reReadCat;
          if (staticCache) { // a static catalog has been updated
            synchronized (this) {
              reReadCat.setStatic(true);
              staticCatalogHash.put(workPath, reReadCat);
            }
          }
        }

      } else {
        logCatalogInit.error(ERROR + "Static catalog does not exist that we expected = " + workPath);
      }
    }  */


    // Check for proxy dataset resolver catalog.
    //if (catalog == null && this.isProxyDatasetResolver(workPath))
    //  catalog = (Catalog) this.getProxyDatasetResolverCatalog(workPath, baseURI);

    return catalog;
  }

  private Catalog makeDynamicCatalog(String path, URI baseURI) throws IOException {

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

    // look for the feature Collection
    if (match.dataRoot.getFeatureCollection() != null) {
      InvDatasetFeatureCollection fc = featureCollectionCache.get(match.dataRoot.getFeatureCollection());

      boolean isLatest = path.endsWith("/latest.xml");
      if (isLatest)
        return fc.makeLatest(match.remaining, path, baseURI);
      else
        return fc.makeCatalog(match.remaining, path, baseURI);
    }

    // if (path.endsWith("/latest.xml")) return null; // latest is not handled here  LOOK are you sure ??

    DatasetScan dscan = match.dataRoot.getDatasetScan();
    if (dscan != null) {
      if (log.isDebugEnabled()) log.debug("makeDynamicCatalog(): Calling DatasetScan.makeCatalogForDirectory( " + baseURI + ", " + path + ").");
      Catalog cat = dscan.makeCatalogForDirectory(workPath, baseURI);

      if (null == cat)
        log.error("makeDynamicCatalog(): DatasetScan.makeCatalogForDirectory failed = " + workPath);

      return cat;
    }

    CatalogScan catScan = match.dataRoot.getCatalogScan();
    if (catScan != null) {
      if (log.isDebugEnabled()) log.debug("makeDynamicCatalog(): Calling CatalogScan.makeCatalogForDirectory( " + baseURI + ", " + path + ").");
      Catalog cat = catScan.makeCatalog(match.remaining, filename, baseURI, ccc);

      if (null == cat)
        log.error("makeDynamicCatalog(): CatalogScan.makeCatalogForDirectory failed = " + workPath);

      return cat;
    }

    log.warn("makeDynamicCatalog(): No FeatureCollection or DatasetScan for =" + workPath + " request path= " + path);
    return null;
  }
}

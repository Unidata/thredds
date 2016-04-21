/*
 * Copyright 1998-2015 John Caron and University Corporation for Atmospheric Research/Unidata
 *
 *  Portions of this software were developed by the Unidata Program at the
 *  University Corporation for Atmospheric Research.
 *
 *  Access and use of this software shall impose the following obligations
 *  and understandings on the user. The user is granted the right, without
 *  any fee or cost, to use, copy, modify, alter, enhance and distribute
 *  this software, and any derivative works thereof, and its supporting
 *  documentation for any purpose whatsoever, provided that this entire
 *  notice appears in all copies of the software, derivative works and
 *  supporting documentation.  Further, UCAR requests that the user credit
 *  UCAR/Unidata in any publications that result from the use of this
 *  software or in any product that includes this software. The names UCAR
 *  and/or Unidata, however, may not be used in any advertising or publicity
 *  to endorse or promote any products or commercial entity unless specific
 *  written permission is obtained from UCAR/Unidata. The user also
 *  understands that UCAR/Unidata is not obligated to provide the user with
 *  any support, consulting, training or assistance of any kind with regard
 *  to the use, operation and performance of this software nor to provide
 *  the user with any updates, revisions, new versions or "bug fixes."
 *
 *  THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *  INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *  FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *  NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *  WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
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
import thredds.server.config.TdsContext;

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
   * <p>
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

    // Check if its a CatalogBuilder or ConfigCatalog
    Object dyno = makeDynamicCatalog(workPath, baseURI);
    if (dyno != null) {
      CatalogBuilder catBuilder;
      if (dyno instanceof CatalogBuilder) {
        catBuilder = (CatalogBuilder) dyno;
      } else {
        ConfigCatalog configCatalog = (ConfigCatalog) dyno;
        catBuilder = configCatalog.makeCatalogBuilder();     // turn it back into mutable object
      }
      addGlobalServices(catBuilder);
      return catBuilder.makeCatalog();
    }

    // check cache and read if needed
    ConfigCatalog configCatalog = ccc.get(workPath);
    if (configCatalog == null) return null;
    CatalogBuilder catBuilder = configCatalog.makeCatalogBuilder();
    addGlobalServices(catBuilder);
    return catBuilder.makeCatalog();
  }

  // barfola on the return type
  private Object makeDynamicCatalog(String path, URI baseURI) throws IOException {
    boolean isLatest = path.endsWith("/latest.xml");

    // strip off the filename
    int pos = path.lastIndexOf("/");
    String workPath = (pos >= 0) ? path.substring(0, pos) : path;
    String filename = (pos > 0) ? path.substring(pos + 1) : path;

    // now look through the data roots for a maximal match
    DataRootManager.DataRootMatch match = dataRootManager.findDataRootMatch(workPath);
    if (match == null)
      return null;

    // Feature Collection
    if (match.dataRoot.getFeatureCollection() != null) {
      InvDatasetFeatureCollection fc = featureCollectionCache.get(match.dataRoot.getFeatureCollection());

      if (isLatest)
        return fc.makeLatest(match.remaining, path, baseURI);
      else
        return fc.makeCatalog(match.remaining, path, baseURI);
    }

    // DatasetScan
    DatasetScan dscan = match.dataRoot.getDatasetScan();
    if (dscan != null) {
      if (log.isDebugEnabled()) log.debug("makeDynamicCatalog(): Calling DatasetScan.makeCatalogForDirectory( " + baseURI + ", " + path + ").");
      CatalogBuilder cat;

      if (isLatest)
        cat = dscan.makeCatalogForLatest(workPath, baseURI);
      else
        cat = dscan.makeCatalogForDirectory(workPath, baseURI);

      if (null == cat)
        log.error("makeDynamicCatalog(): DatasetScan.makeCatalogForDirectory failed = " + workPath);

      return cat;
    }

    // CatalogScan
    CatalogScan catScan = match.dataRoot.getCatalogScan();
    if (catScan != null) {
      if (!filename.equalsIgnoreCase(CatalogScan.CATSCAN)) { // its an actual catalog
        return catScan.getCatalog(tdsContext.getThreddsDirectory(), match.remaining, filename, ccc);
      }

      if (log.isDebugEnabled()) log.debug("makeDynamicCatalog(): Calling CatalogScan.makeCatalogForDirectory( " + baseURI + ", " + path + ").");
      CatalogBuilder cat = catScan.makeCatalogFromDirectory(tdsContext.getThreddsDirectory(), match.remaining, baseURI);

      if (null == cat)
        log.error("makeDynamicCatalog(): CatalogScan.makeCatalogForDirectory failed = " + workPath);

      return cat;
    }

    log.warn("makeDynamicCatalog() failed for =" + workPath + " request path= " + path);
    return null;
  }

  /////////////////////////////////////////////////////
  // rigamorole to modify invariant catalogs; we may need to add global services

  private void addGlobalServices(CatalogBuilder cat) {

    // look for datasets that want to use global services
    Set<String> allServiceNames = new HashSet<>();
    findServices(cat.getDatasets(), allServiceNames);  // all services used
    if (!allServiceNames.isEmpty()) {
      List<Service> servicesMissing = new ArrayList<>();   // all services missing
      for (String name : allServiceNames) {
        if (cat.hasServiceInDataset(name)) continue;
        Service s = globalServices.findGlobalService(name);
        if (s != null) servicesMissing.add(s);
      }
      servicesMissing.forEach(cat::addService);
    }

    // look for datasets that want to use standard services
    for (DatasetBuilder node : cat.getDatasets()) {
      String sname = (String) node.getFldOrInherited(Dataset.ServiceName);
      String urlPath = (String) node.get(Dataset.UrlPath);
      String ftypeS = (String) node.getFldOrInherited(Dataset.FeatureType);
      if (sname == null && urlPath != null && ftypeS != null) {
        Service s = globalServices.getStandardServices(ftypeS);
        if (s != null) {
          node.put(Dataset.ServiceName, s.getName());
          cat.addService(s);
        }
      }
    }
  }

  private void findServices(Iterable<DatasetBuilder> datasets, Set<String> serviceNames) {
    for (DatasetBuilder ds : datasets) {
      String sname = (String) ds.getFldOrInherited(Dataset.ServiceName);
      if (sname != null)
        serviceNames.add(sname);
      findServices(ds.getDatasets(), serviceNames); // recurse
    }
  }
}



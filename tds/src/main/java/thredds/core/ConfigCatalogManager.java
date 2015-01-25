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

package thredds.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import thredds.client.catalog.*;
import thredds.server.catalog.ConfigCatalog;
import thredds.server.catalog.DatasetRootConfig;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.FeatureCollection;
import thredds.server.catalog.builder.ConfigCatalogBuilder;
import thredds.server.config.TdsContext;
import thredds.servlet.PathMatcher;
import thredds.servlet.ThreddsConfig;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Read in the Config Catalogs
 *
 * @author caron
 * @since 1/23/2015
 */
@Component("ConfigCatalogManager")
public class ConfigCatalogManager {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ConfigCatalogManager.class);
  static private org.slf4j.Logger logCatalogInit = org.slf4j.LoggerFactory.getLogger(ConfigCatalogManager.class.getName() + ".catalogInit");
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");
  static private final String ERROR = "*** ERROR ";

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private PathMatcher<DataRoot> pathMatcher; // collection of DataRoot objects

  private HashMap<String, ConfigCatalog> staticCatalogHash; // Hash of static catalogs, key = path
  private Set<String> staticCatalogNames;                   // Hash of static catalogs, key = path
  private HashSet<String> idHash = new HashSet<>();         // Hash of ids, to look for duplicates
  private boolean cacheStaticCatalogs;
  private AllowedServices allowedServices;

  public ConfigCatalog getStaticCatalog(String path) {
    return staticCatalogHash.get(path);
  }

  public List<String> getStaticCatalogPaths() {
    List<String> result = new ArrayList<>();
    for (String s : staticCatalogNames)
      result.add(s);
    return result;
  }

  public boolean isStaticCatalogNotInCache(String path) {
    return !cacheStaticCatalogs && staticCatalogNames.contains(path);
  }

  void initCatalogs() {
    ArrayList<String> catList = new ArrayList<>();
    catList.add("catalog.xml"); // always first
    catList.addAll(ThreddsConfig.getCatalogRoots()); // add any others listed in ThreddsConfig

    logCatalogInit.info("initCatalogs(): initializing " + catList.size() + " root catalogs.");
    this.initCatalogs(catList);
  }

  void initCatalogs(List<String> configCatalogRoots) {
    allowedServices = new AllowedServices();

    cacheStaticCatalogs = ThreddsConfig.getBoolean("Catalog.cache", true);  // user can turn off static catalog caching
    startupLog.info("DataRootHandler: staticCache= " + cacheStaticCatalogs);

    this.staticCatalogNames = new HashSet<>();
    this.staticCatalogHash = new HashMap<>();

    for (String path : configCatalogRoots) {
      try {
        path = StringUtils.cleanPath(path);
        logCatalogInit.info("\n**************************************\nCatalog init " + path + "\n[" + CalendarDate.present() + "]");
        initCatalog(path, true, true);
      } catch (Throwable e) {
        logCatalogInit.error(ERROR + "initializing catalog " + path + "; " + e.getMessage(), e);
      }
    }
  }

  /**
   * Reads a catalog, finds datasetRoot, datasetScan, datasetFmrc, NcML and restricted access datasets
   * <p/>
   *
   * @param path    file path of catalog, reletive to contentPath, ie catalog fullpath = contentPath + path.
   * @param recurse if true, look for catRefs in this catalog
   * @param cache   if true, always cache
   * @throws java.io.IOException if reading catalog fails
   */
  private void initCatalog(String path, boolean recurse, boolean cache) throws IOException {
    path = StringUtils.cleanPath(path);
    File f = this.tdsContext.getConfigFileSource().getFile(path);
    if (f == null) {
      logCatalogInit.error(ERROR + "initCatalog(): Catalog [" + path + "] does not exist in config directory.");
      return;
    }
    System.out.printf("initCatalog %s%n", f.getPath());

    // make sure we dont already have it
    if (staticCatalogNames.contains(path)) {
      logCatalogInit.error(ERROR + "initCatalog(): Catalog [" + path + "] already seen, possible loop (skip).");
      return;
    }
    staticCatalogNames.add(path);
    if (logCatalogInit.isDebugEnabled()) logCatalogInit.debug("initCatalog {} -> {}", path, f.getAbsolutePath());

    // read it
    ConfigCatalog cat = readCatalog(path, f.getPath());
    if (cat == null) {
      logCatalogInit.error(ERROR + "initCatalog(): failed to read catalog <" + f.getPath() + ">.");
      return;
    }

    // look for datasetRoots
    for (DatasetRootConfig p : cat.getDatasetRoots()) {
      addRoot(p, true);
    }

    List<String> disallowedServices = allowedServices.getDisallowedServices(cat);
    if (!disallowedServices.isEmpty()) {
      logCatalogInit.error(ERROR + "initCatalog(): declared services: " + disallowedServices.toString() + " in catalog: " + f.getPath() + " are disallowed in threddsConfig file");
    }

    // get the directory path, reletive to the contentPath
    int pos = path.lastIndexOf("/");
    String dirPath = (pos > 0) ? path.substring(0, pos + 1) : "";

    // look for datasetScans and NcML elements and Fmrc and featureCollections
    boolean needsCache = initSpecialDatasets(cat.getDatasets());

    // optionally add catalog to cache
    if (cacheStaticCatalogs || cache || needsCache) {
      staticCatalogHash.put(path, cat);
      if (logCatalogInit.isDebugEnabled()) logCatalogInit.debug("  add static catalog to hash=" + path);
    }

    if (recurse) {
      initFollowCatrefs(dirPath, cat.getDatasets());
    }
  }

  /**
   * Does the actual work of reading a catalog.
   *
   * @param path            reletive path starting from content root
   * @param catalogFullPath absolute location on disk
   * @return the Catalog, or null if failure
   */
  private ConfigCatalog readCatalog(String path, String catalogFullPath)  {
    URI uri;
    try {
      uri = new URI("file:" + StringUtil2.escape(catalogFullPath, "/:-_.")); // LOOK needed ?
    } catch (URISyntaxException e) {
      logCatalogInit.error(ERROR + "readCatalog(): URISyntaxException=" + e.getMessage());
      return null;
    }

    ConfigCatalogBuilder builder = new ConfigCatalogBuilder();
    try {
      // read the catalog
      logCatalogInit.info("\n-------readCatalog(): full path=" + catalogFullPath + "; path=" + path);
      ConfigCatalog cat = (ConfigCatalog) builder.buildFromURI(uri);
      if (builder.hasFatalError()) {
        logCatalogInit.error(ERROR + "   invalid catalog -- " + builder.getErrorMessage());
        return null;
      }

      if (builder.getErrorMessage().length() > 0)
        logCatalogInit.debug(builder.getErrorMessage());

      return cat;

    } catch (Throwable t) {
      logCatalogInit.error(ERROR + "  Exception on catalog=" + catalogFullPath + " " + t.getMessage() + "\n log=" + builder.getErrorMessage(), t);
      return null;
    }

  }

  /**
   * Finds datasetScan, datasetFmrc, NcML and restricted access datasets.
   * Look for duplicate Ids (give message). Dont follow catRefs.
   *
   * @param dsList the list of Dataset
   * @return true if the containing catalog should be cached
   */
  private boolean initSpecialDatasets(List<Dataset> dsList) {
    boolean needsCache = false;

    Iterator<Dataset> iter = dsList.iterator();
    while (iter.hasNext()) {
      Dataset dataset = iter.next();

      // look for duplicate ids
      String id = dataset.getID();
      if (id != null) {
        if (idHash.contains(id)) {
          logCatalogInit.error(ERROR + "Duplicate id on  '" + dataset.getName() + "' id= '" + id + "'");
        } else {
          idHash.add(id);
        }
      }

      if (dataset instanceof DatasetScan) {
        DatasetScan ds = (DatasetScan) dataset;
        Service service = ds.getServiceDefault();
        if (service == null) {
          logCatalogInit.error(ERROR + "DatasetScan " + ds.getName() + " has no default Service - skipping");
          continue;
        }
        if (!addRoot(ds))
          iter.remove();

      } else if (dataset instanceof FeatureCollection) {
        FeatureCollection fc = (FeatureCollection) dataset;
        addRoot(fc);
        needsCache = true;

        // not a DatasetScan or DatasetFmrc or FeatureCollection
      } else if (dataset.getNcmlElement() != null) {
        DatasetHandler.putNcmlDataset(dataset.getUrlPath(), dataset);
      }

      if (!(dataset instanceof CatalogRef)) {
        // recurse
        initSpecialDatasets(dataset.getDatasets());
      }
    }

    return needsCache;
  }

  private void initFollowCatrefs(String dirPath, List<Dataset> datasets) throws IOException {
    for (Dataset Dataset : datasets) {

      if ((Dataset instanceof CatalogRef) && !(Dataset instanceof DatasetScan)
              && !(Dataset instanceof FeatureCollection)) {
        CatalogRef catref = (CatalogRef) Dataset;
        String href = catref.getXlinkHref();
        if (logCatalogInit.isDebugEnabled()) logCatalogInit.debug("  catref.getXlinkHref=" + href);

        // Check that catRef is relative
        if (!href.startsWith("http:")) {
          // Clean up relative URLs that start with "./"
          if (href.startsWith("./")) {
            href = href.substring(2);
          }

          String path;
          String contextPathPlus = this.tdsContext.getContextPath() + "/";
          if (href.startsWith(contextPathPlus)) {
            path = href.substring(contextPathPlus.length()); // absolute starting from content root
          } else if (href.startsWith("/")) {
            // Drop the catRef because it points to a non-TDS served catalog.
            logCatalogInit.error(ERROR + "Skipping catalogRef <xlink:href=" + href + ">. Reference is relative to the server outside the context path [" + contextPathPlus + "]. " +
                    "Parent catalog info: Name=\"" + catref.getParentCatalog().getName() + "\"; Base URI=\"" + catref.getParentCatalog().getUriString() + "\"; dirPath=\"" + dirPath + "\".");
            continue;
          } else {
            path = dirPath + href;  // reletive starting from current directory
          }

          initCatalog(path, true, false);
        }

      } else if (!(Dataset instanceof DatasetScan) && !(Dataset instanceof FeatureCollection)) {
        // recurse through nested datasets
        initFollowCatrefs(dirPath, Dataset.getDatasets());
      }
    }
  }

  private boolean addRoot(DatasetScan dscan) {
    // check for duplicates
    String path = dscan.getPath();

    if (path == null) {
      logCatalogInit.error(ERROR + dscan.getName() + " missing a path attribute.");
      return false;
    }

    DataRoot droot = pathMatcher.get(path);
    if (droot != null) {
      if (!droot.getDirLocation().equals(dscan.getScanLocation())) {
        logCatalogInit.error(ERROR + "DatasetScan already have dataRoot =<" + path + ">  mapped to directory= <" + droot.getDirLocation() + ">" +
                " wanted to map to fmrc=<" + dscan.getScanLocation() + "> in catalog " + dscan.getParentCatalog().getUriString());
      }

      return false;
    }

    // add it
    droot = new DataRoot(dscan);
    pathMatcher.put(path, droot);

    logCatalogInit.debug(" added rootPath=<" + path + ">  for directory= <" + dscan.getScanLocation() + ">");
    return true;
  }

  public List<FeatureCollection> getFeatureCollections() {
    List<FeatureCollection> result = new ArrayList<>();
    Iterator iter = pathMatcher.iterator();
    while (iter.hasNext()) {
      DataRoot droot = (DataRoot) iter.next();
      if (droot.getFeatureCollection() != null)
        result.add(droot.getFeatureCollection());
    }
    return result;
  }

  public FeatureCollection findFcByCollectionName(String collectionName) {
    Iterator iter = pathMatcher.iterator();
    while (iter.hasNext()) {
      DataRoot droot = (DataRoot) iter.next();
      if ((droot.getFeatureCollection() != null) && droot.getFeatureCollection().getCollectionName().equals(collectionName))
        return droot.getFeatureCollection();
    }
    return null;
  }

  private boolean addRoot(FeatureCollection fc) {
    // check for duplicates
    String path = fc.getPath();

    if (path == null) {
      logCatalogInit.error(ERROR + fc.getName() + " missing a path attribute.");
      return false;
    }

    DataRoot droot = pathMatcher.get(path);
    if (droot != null) {
      logCatalogInit.error(ERROR + "FeatureCollection already have dataRoot =<" + path + ">  mapped to directory= <" + droot.getDirLocation() + ">" +
              " wanted to use by FeatureCollection Dataset =<" + fc.getName() + ">");
      return false;
    }

    // add it
    droot = new DataRoot(fc);

    if (droot.getDirLocation() != null) {
      File file = new File(droot.getDirLocation());
      if (!file.exists()) {
        logCatalogInit.error(ERROR + "FeatureCollection = '" + fc.getName() + "' directory= <" + droot.getDirLocation() + "> does not exist\n");
        return false;
      }
    }

    pathMatcher.put(path, droot);
    logCatalogInit.debug(" added rootPath=<" + path + ">  for feature collection= <" + fc.getName() + ">");
    return true;
  }

  private boolean addRoot(String path, String dirLocation, boolean wantErr) {
    // check for duplicates
    DataRoot droot = pathMatcher.get(path);
    if (droot != null) {
      if (wantErr)
        logCatalogInit.error(ERROR + "already have dataRoot =<" + path + ">  mapped to directory= <" + droot.getDirLocation() + ">" +
                " wanted to map to <" + dirLocation + ">");

      return false;
    }

    File file = new File(dirLocation);
    if (!file.exists()) {
      logCatalogInit.error(ERROR + "Data Root =" + path + " directory= <" + dirLocation + "> does not exist");
      return false;
    }

    // add it
    droot = new DataRoot(path, dirLocation);
    pathMatcher.put(path, droot);

    logCatalogInit.debug(" added rootPath=<" + path + ">  for directory= <" + dirLocation + ">");
    return true;
  }

  private boolean addRoot(DatasetRootConfig config, boolean wantErr) {
    String path = config.getPath();
    String location = config.getLocation();

    // check for duplicates
    DataRoot droot = pathMatcher.get(path);
    if (droot != null) {
      if (wantErr)
        logCatalogInit.error(ERROR + "DataRootConfig already have dataRoot =<" + path + ">  mapped to directory= <" + droot.getDirLocation() + ">" +
                " wanted to map to <" + location + ">");

      return false;
    }

    location = ConfigCatalog.translateAlias(location);
    File file = new File(location);
    if (!file.exists()) {
      logCatalogInit.error(ERROR + "DataRootConfig path =" + path + " directory= <" + location + "> does not exist");
      return false;
    }

    // add it
    droot = new DataRoot(path, location);
    pathMatcher.put(path, droot);

    logCatalogInit.debug(" added rootPath=<" + path + ">  for directory= <" + location + ">");
    return true;
  }

}

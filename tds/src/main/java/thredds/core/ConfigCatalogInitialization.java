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
import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.Service;
import thredds.server.admin.DebugCommands;
import thredds.server.catalog.*;
import thredds.server.catalog.builder.ConfigCatalogBuilder;
import thredds.server.config.TdsContext;
import thredds.server.config.ThreddsConfig;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Reads in the Config catalogs on startup.
 * Manges any changes while running.
 *
 * @author caron
 * @since 3/21/2015
 */
@Component("ConfigCatalogInitialization")
public class ConfigCatalogInitialization {
  static private org.slf4j.Logger logCatalogInit = org.slf4j.LoggerFactory.getLogger(ConfigCatalogInitialization.class.getName()+".catalogInit");
  static private final String ERROR = "*** ERROR: ";

  @Autowired
  private TdsContext tdsContext;  // used for  getContentDirectory, contextPath

  @Autowired
  private DataRootPathMatcher<DataRoot> dataRootPathMatcher;

  @Autowired
  private DatasetTracker datasetTracker;

  @Autowired
  private AllowedServices allowedServices;

  @Autowired
  private DebugCommands debugCommands;

  private List<String> rootCatalogKeys;    // needed ??
  private File rootFile;
  private String contextPath;
  private DatasetTracker.Callback callback;

  public ConfigCatalogInitialization() {
  }

  // used from outside of tomcat/spring
  public ConfigCatalogInitialization(String rootPath, String rootCatalog, DataRootPathMatcher<DataRoot> matcher, DatasetTracker datasetTracker,
                                     AllowedServices allowedServices, DatasetTracker.Callback callback) throws IOException {
    this.rootFile = new File(rootPath);
    this.contextPath = "/thredds";
    this.dataRootPathMatcher = matcher;
    this.datasetTracker = datasetTracker;
    this.allowedServices = allowedServices;
    this.callback = callback;

    Set<String> pathHash = new HashSet<>();       // Hash of ids, to look for duplicates
    Set<String> idHash = new HashSet<>();         // Hash of ids, to look for duplicates

    initCatalog(rootCatalog, pathHash, idHash);
  }

  public void init() {
    this.rootFile = this.tdsContext.getContentDirectory();
    this.contextPath = tdsContext.getContextPath();

    rootCatalogKeys = new ArrayList<>();
    rootCatalogKeys.add("catalog.xml"); // always first
    rootCatalogKeys.addAll(ThreddsConfig.getCatalogRoots()); // add any others listed in ThreddsConfig

    logCatalogInit.info("ConfigCatalogManage: initializing " + rootCatalogKeys.size() + " root catalogs.");

    Set<String> pathHash = new HashSet<>();       // Hash of ids, to look for duplicates
    Set<String> idHash = new HashSet<>();         // Hash of ids, to look for duplicates

    for (String path : rootCatalogKeys) {
      try {
        path = StringUtils.cleanPath(path);
        logCatalogInit.info("\n**************************************\nCatalog init " + path + "[" + CalendarDate.present() + "]");
        initCatalog(path, pathHash, idHash);
      } catch (Throwable e) {
        logCatalogInit.error(ERROR + "initializing catalog " + path + "; " + e.getMessage(), e);
      }
    }

    makeDebugActions();
  }

  private void initCatalog(String path, Set<String> pathHash, Set<String> idHash) throws IOException {
    path = StringUtils.cleanPath(path);
    // File f = this.tdsContext.getCatalogRootDirSource().getFile(path);
    File f = new File(this.rootFile, path);
    if (!f.exists()) {
      logCatalogInit.error(ERROR + "initCatalog(): Catalog [" + path + "] does not exist in config directory.");
      return;
    }
    // System.out.printf("initCatalog %s%n", f.getPath());

    // make sure we dont already have it
    if (pathHash.contains(path)) {
      logCatalogInit.error(ERROR + "initCatalog(): Catalog [" + path + "] already seen, possible loop (skip).");
      return;
    }
    pathHash.add(path);
    // if (logCatalogInit.isDebugEnabled()) logCatalogInit.debug("initCatalog {} -> {}", path, f.getAbsolutePath());

    // read it
    ConfigCatalog cat = readCatalog(path, f.getPath());
    if (cat == null) {
      logCatalogInit.error(ERROR + "initCatalog(): failed to read catalog <" + f.getPath() + ">.");
      return;
    }
    //ccc.put(path, cat);  // LOOK really ??
    if (callback != null) callback.hasCatalogRef(cat);

    // look for datasetRoots
    for (DatasetRootConfig p : cat.getDatasetRoots()) {
      addRoot(p, true);
    }

    if (callback == null) {
      List<String> disallowedServices = allowedServices.getDisallowedServices(cat.getServices());
      if (!disallowedServices.isEmpty()) {
        allowedServices.getDisallowedServices(cat.getServices());
        logCatalogInit.error(ERROR + "initCatalog(): declared services: " + Arrays.toString(disallowedServices.toArray()) + " in catalog: " + f.getPath() + " are disallowed in threddsConfig file");
      }
    }

    // get the directory path, reletive to the contentPath
    int pos = path.lastIndexOf("/");
    String dirPath = (pos > 0) ? path.substring(0, pos + 1) : "";

    // look for dataRoots in datasetScans and featureCollections
    extractDataRoots(cat.getDatasets(), idHash);

    // recurse
    processDatasets(dirPath, cat.getDatasets(), pathHash, idHash);
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
      // uri = new URI("file:" + StringUtil2.escape(catalogFullPath, "/:-_.")); // LOOK needed ?
      uri = new URI(this.contextPath + "/catalog/" + path);
    } catch (URISyntaxException e) {
      logCatalogInit.error(ERROR + "readCatalog(): URISyntaxException=" + e.getMessage());
      return null;
    }

    ConfigCatalogBuilder builder = new ConfigCatalogBuilder();
    try {
      // read the catalog
      logCatalogInit.info("\n-------readCatalog(): full path=" + catalogFullPath + "; path=" + path+ "; uri=" + uri);
      ConfigCatalog cat = (ConfigCatalog) builder.buildFromLocation(catalogFullPath, uri);
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

  private void processDatasets(String dirPath, List<Dataset> datasets, Set<String> pathHash, Set<String> idHash) throws IOException {
    for (Dataset ds : datasets) {
      datasetTracker.trackDataset(ds, callback);

      if ((ds instanceof DatasetScan) || (ds instanceof FeatureCollectionRef)) continue;

      if (ds instanceof CatalogRef) { // follow catalog refs
        CatalogRef catref = (CatalogRef) ds;
        String href = catref.getXlinkHref();
        // if (logCatalogInit.isDebugEnabled()) logCatalogInit.debug("  catref.getXlinkHref=" + href);

        // Check that catRef is relative
        if (!href.startsWith("http:")) {
          // Clean up relative URLs that start with "./"
          if (href.startsWith("./")) {
            href = href.substring(2);
          }

          String path;
          String contextPathPlus = this.contextPath + "/";
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

          initCatalog(path, pathHash, idHash);
        }

      } else {
        // recurse through nested datasets
        processDatasets(dirPath, ds.getDatasets(), pathHash, idHash);
      }
    }
  }

  /**
   * Finds datasetScan, datasetFmrc, NcML and restricted access datasets.
   * Look for duplicate Ids (give message). Dont follow catRefs.
   *
   * @param dsList the list of Dataset
   */
  private void extractDataRoots(List<Dataset> dsList, Set<String> idHash) {

    for (Dataset dataset : dsList) {
      // look for duplicate ids
      String id = dataset.getID();
      if (id != null) {
        if (idHash.contains(id)) {
          logCatalogInit.error(ERROR + "Duplicate id on  '" + dataset.getName() + "' id= '" + id + "'");
        } else {
          idHash.add(id);
        }
      }

      if (dataset.getRestrictAccess() != null) {
        if (callback != null) callback.hasRestriction(dataset);
      }

      if (dataset instanceof DatasetScan) {
        DatasetScan ds = (DatasetScan) dataset;
        Service service = ds.getServiceDefault();
        if (service == null) {
          logCatalogInit.error(ERROR + "DatasetScan " + ds.getName() + " has no default Service - skipping");  // LOOK needed?
          continue;
        }
        addRoot(ds);

      } else if (dataset instanceof FeatureCollectionRef) {
        FeatureCollectionRef fc = (FeatureCollectionRef) dataset;
        addRoot(fc);

        // not a DatasetScan or DatasetFmrc or FeatureCollection
      } else if (dataset.getNcmlElement() != null) {
        if (callback != null) callback.hasNcml(dataset);
      }

      if (!(dataset instanceof CatalogRef)) {
        // recurse
        extractDataRoots(dataset.getDatasets(), idHash);
      }
    }

  }

  private boolean addRoot(DatasetScan dscan) {
    // check for duplicates
    String path = dscan.getPath();

    if (path == null) {
      logCatalogInit.error(ERROR + "DatasetScan '" + dscan.getName() + "' missing the path attribute.");
      return false;
    }

    DataRoot droot = dataRootPathMatcher.get(path);
    if (droot != null) {
      if (!droot.getDirLocation().equals(dscan.getScanLocation())) {
        logCatalogInit.error(ERROR + "DatasetScan already have dataRoot =<" + path + ">  mapped to directory= <" + droot.getDirLocation() + ">" +
                " wanted to map to =<" + dscan.getScanLocation() + "> in catalog " + dscan.getParentCatalog().getUriString());
      }
      return false;
    }

    // add it
    droot = new DataRoot(dscan);
    dataRootPathMatcher.put(path, droot);
    if (callback != null) callback.hasDataRoot(droot);
    logCatalogInit.debug(" added rootPath=<" + path + ">  for directory= <" + dscan.getScanLocation() + ">");
    return true;
  }

  private boolean addRoot(FeatureCollectionRef fc) {
    // check for duplicates
    String path = fc.getPath();

    if (path == null) {
      logCatalogInit.error(ERROR + "FeatureCollection '"+ fc.getName() + "' missing the path attribute.");
      return false;
    }

    DataRoot droot = dataRootPathMatcher.get(path);
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

    dataRootPathMatcher.put(path, droot);
    if (callback != null) callback.hasDataRoot(droot);
    logCatalogInit.debug(" added rootPath=<" + path + ">  for feature collection= <" + fc.getName() + ">");
    return true;
  }

  private boolean addRoot(DatasetRootConfig config, boolean wantErr) {
    String path = config.getPath();
    String location = config.getLocation();

    // check for duplicates
    DataRoot droot = dataRootPathMatcher.get(path);
    if (droot != null) {
      if (wantErr)
        logCatalogInit.error(ERROR + "DatasetRootConfig already have dataRoot =<" + path + ">  mapped to directory= <" + droot.getDirLocation() + ">" +
                " wanted to map to <" + location + ">");

      return false;
    }

    location = DataRootAlias.translateAlias(location);
    File file = new File(location);
    if (!file.exists() && (callback == null)) {
      logCatalogInit.error(ERROR + "DatasetRootConfig path ='" + path + "' directory= <" + location + "> does not exist");
      return false;
    }

    // add it
    droot = new DataRoot(path, location);
    dataRootPathMatcher.put(path, droot);
    if (callback != null) callback.hasDataRoot(droot);
    logCatalogInit.debug(" added rootPath=<" + path + ">  for directory= <" + location + ">");
    return true;
  }

  /////////////////////////////////////////////////////

  public void makeDebugActions() {
    DebugCommands.Category debugHandler = debugCommands.findCategory("catalogs");
    DebugCommands.Action act;

    act = new DebugCommands.Action("showStatic", "Show root catalogs") {
      public void doAction(DebugCommands.Event e) {
        StringBuilder sbuff = new StringBuilder();
        synchronized (ConfigCatalogInitialization.this) {
          for (String catPath : rootCatalogKeys) {
            sbuff.append(" catalog= ").append(catPath).append("\n");
            //String filename = StringUtil2.unescape(cat.getUriString());
            //sbuff.append(" from= ").append(filename).append("\n");
          }
        }
        e.pw.println(StringUtil2.quoteHtmlContent("\n" + sbuff.toString()));
      }
    };
    debugHandler.addAction(act);

  }
}

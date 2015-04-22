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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.util.StringUtils;
import thredds.client.catalog.*;
import thredds.featurecollection.FeatureCollectionCache;
import thredds.featurecollection.InvDatasetFeatureCollection;
import thredds.server.admin.DebugCommands;
import thredds.server.catalog.*;
import thredds.server.config.TdsContext;

import thredds.util.*;
import thredds.util.filesource.FileSource;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * The DataRootHandler manages all the "data roots" for a TDS
 * and provides mappings from URLs to catalog and datasets
 * <p/>
 * <p>The "data roots" are read in from one or more trees of config catalogs
 * and are defined by the datasetScan and datasetRoot and featureColelction elements in the config catalogs.
 * <p/>
 *
 * @author caron
 * @since 1/23/2015
 */
@Component("DataRootManager")
public class DataRootManager implements InitializingBean {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataRootManager.class);
  static private org.slf4j.Logger logCatalogInit = org.slf4j.LoggerFactory.getLogger(DataRootManager.class.getName() + ".catalogInit");
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

  static public final boolean debug = false;

  static public DataRootManager getInstance() {
    return new DataRootManager(); // Used for testing only
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private DataRootPathMatcher<DataRoot> dataRootPathMatcher;

  @Autowired
  private ConfigCatalogManager ccManager;

  @Autowired
  private ConfigCatalogCache ccc;

  @Autowired
  FeatureCollectionCache featureCollectionCache;
  
  @Autowired
  DebugCommands debugCommands;

  private DataRootManager() {
  }

  //Set method must be called so annotation at method level rather than property level
  @Resource(name = "dataRootLocationAliasExpanders")
  public void setDataRootLocationAliasExpanders(Map<String, String> aliases) {
    for (Map.Entry<String, String> entry : aliases.entrySet())
      DataRootAlias.addAlias("${" + entry.getKey() + "}", entry.getValue());
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    FileSource fileSource = tdsContext.getPublicDocFileSource();
    if (fileSource != null) {
      File file = fileSource.getFile("");
      if (file != null)
        DataRootAlias.addAlias("content", StringUtils.cleanPath(file.getPath())); // LOOK
    }

    makeDebugActions();
    startupLog.info("DataRootManager:" + DataRootAlias.size() +" aliases set ");
  }

  /* public boolean registerConfigListener(ConfigListener cl) {
    if (cl == null) return false;
    if (configListeners.contains(cl)) return false;
    return configListeners.add(cl);
  }

  public boolean unregisterConfigListener(ConfigListener cl) {
    if (cl == null) return false;
    return configListeners.remove(cl);
  }  */

  /**
   * Reinitialize lists of static catalogs, data roots, dataset Ids.
   *
  public synchronized void reinit() {
    // Notify listeners of start of initialization.
    isReinit = true;
    for (ConfigListener cl : configListeners)
      cl.configStart();

    logCatalogInit.info("\n**************************************\n**************************************\nStarting TDS config catalog reinitialization\n["
            + CalendarDate.present() + "]");

    // cleanup 
    thredds.inventory.bdb.MetadataManager.closeAll();

    // Empty all config catalog information.
    pathMatcher = new PathMatcher();
    //idHash = new HashSet<>();

    DatasetHandler.reinit(); // NcML datasets

    isReinit = false;

    logCatalogInit.info("\n**************************************\n**************************************\nDone with TDS config catalog reinitialization\n["
            + CalendarDate.present() + "]");
  }

  volatile boolean isReinit = false;  */

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  static public class DataRootMatch {
    public String rootPath;     // this is the matching part of the URL
    public String remaining;   // this is the part of the URL that didnt match
    public String dirLocation;   // this is the directory that should be substituted for the rootPath
    public DataRoot dataRoot;  // this is the directory that should be substituted for the rootPath
  }

  /**
   * Find the location match for a dataRoot.
   * Aliasing has been done.
   *
   * @param path the dataRoot path name
   * @return best DataRoot location or null if no match.
   */
  public String findDataRootLocation(String path) {
    if ((path.length() > 0) && (path.charAt(0) == '/'))
      path = path.substring(1);

    DataRoot dataRoot = dataRootPathMatcher.findLongestMatch(path);
    return (dataRoot == null) ? null : dataRoot.getDirLocation();
  }

  /**
   * Extract the DataRoot from the request.
   * Use this when you need to manipulate the path based on the part that matches a DataRoot.
   *
   * @param req the request
   * @return the DataRootMatch, or null if not found
   */
  private DataRootMatch findDataRootMatch(HttpServletRequest req) {
    String spath = TdsPathUtils.extractPath(req, null);
    return findDataRootMatch(spath);
  }

  public DataRootMatch findDataRootMatch(String spath) {
    if (spath == null)
      return null;
    if (spath.startsWith("/"))
      spath = spath.substring(1);
    DataRoot dataRoot = dataRootPathMatcher.findLongestMatch(spath);
    if (dataRoot == null)
      return null;

    DataRootMatch match = new DataRootMatch();
    match.rootPath = dataRoot.getPath();
    match.remaining = spath.substring(match.rootPath.length());
    if (match.remaining.startsWith("/"))
      match.remaining = match.remaining.substring(1);
    match.dirLocation = dataRoot.getDirLocation();
    match.dataRoot = dataRoot;
    return match;
  }

  private DataRoot findDataRoot(String spath) {
    if (spath == null)
      return null;
    if (spath.startsWith("/"))
      spath = spath.substring(1);
    return dataRootPathMatcher.findLongestMatch(spath);
  }

  /**
   * Return the the location to which the given path maps.
   * Null is returned if the dataset does not exist, the
   * matching DatasetScan or DataRoot filters out the requested MFile, the MFile does not represent a File
   * (i.e., it is not a CrawlableDatasetFile), or an I/O error occurs
   *
   * @param reqPath the request path.
   * @return the location of the file on disk, or null
   * @throws IllegalStateException if the request is not for a descendant of (or the same as) the matching DatasetRoot collection location.
   */
  public String getLocationFromRequestPath(String reqPath) {
    DataRoot reqDataRoot = findDataRoot(reqPath);
    if (reqDataRoot == null)
      return null;

    return reqDataRoot.getFileLocationFromRequestPath(reqPath);
  }

  /*
   * Return the the location to which the given path maps.
   * Null is returned if 1) there is no dataRoot match, 2) the dataset does not exist, or
   * 3) the matching DatasetScan filters it out the requested MFile
   *
   * @param reqPath the request path.
   * @return the location of the file on disk, or null
   * @throws IllegalStateException if the request is not for a descendant of (or the same as) the matching DatasetRoot collection location.
   *
  public String getLocationFromRequestPath(String reqPath) {
    DataRootMatch match = findDataRootMatch(reqPath);
    if (match == null) return null;

    String fullPath = match.dirLocation + match.remaining;
    if (match.dataRoot.getFeatureCollection() != null) {
      return match.dirLocation + match.remaining;
    }

    match.dataRoot.getFileLocationFromRequestPath(reqPath);
    return fullPath;
  }

  static public String getNetcdfFilePath(HttpServletRequest req, String reqPath) throws IOException {
    if (log.isDebugEnabled()) log.debug("DatasetHandler wants " + reqPath);
    if (debugResourceControl) System.out.println("getNetcdfFile = " + ServletUtil.getRequest(req));

    if (reqPath == null)
      return null;

    if (reqPath.startsWith("/"))
      reqPath = reqPath.substring(1);

    // look for a match
    DataRootHandler.DataRootMatch match = DataRootHandler.getInstance().findDataRootMatch(reqPath);

    String fullpath = null;
    if (match != null)
      fullpath = match.dirLocation + match.remaining;
    else {
      File file = DataRootHandler.getInstance().getCrawlableDatasetAsFile(reqPath);
      if (file != null)
        fullpath = file.getAbsolutePath();
    }
    return fullpath;
  }

  }   */

  ///////////////////////////////////////////////////////////

  /**
   * LOOK not sure why in this class, maybe better in ??
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
    String workPath = path;

    // Make sure this is a dynamic catalog request.
    if (!path.endsWith("/catalog.xml") && !path.endsWith("/latest.xml"))
      return null;

    // strip off the filename
    int pos = workPath.lastIndexOf("/");
    if (pos >= 0)
      workPath = workPath.substring(0, pos);

    // now look through the data roots for a maximal match
    DataRootMatch match = findDataRootMatch(workPath);
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

    /* Check that path is allowed, ie not filtered out LOOK
    try {
      if (getCrawlableDataset(workPath) == null)
        return null;
    } catch (IOException e) {
      log.error("makeDynamicCatalog(): I/O error on request <" + path + ">: " + e.getMessage(), e);
      return null;
    }

    // at this point, its gotta be a DatasetScan, not a DatasetRoot
    if (match.dataRoot.getDatasetScan() == null) {
      log.warn("makeDynamicCatalog(): No DatasetScan for =" + workPath + " request path= " + path);
      return null;
    } */

    if (path.endsWith("/latest.xml")) return null; // latest is not handled here  LOOK are you sure ??

    DatasetScan dscan = match.dataRoot.getDatasetScan();
    if (log.isDebugEnabled())
      log.debug("makeDynamicCatalog(): Calling makeCatalogForDirectory( " + baseURI + ", " + path + ").");
    Catalog cat = dscan.makeCatalogForDirectory(workPath, baseURI);

    if (null == cat) {
      log.error("makeDynamicCatalog(): makeCatalogForDirectory failed = " + workPath);
    }

    return cat;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  // debugging only !!

  public void showRoots(Formatter f) {
    for (Map.Entry<String, DataRoot> entry : dataRootPathMatcher.getValues()) {
      f.format(" %s%n", entry.getValue());
    }
  }

  public List<FeatureCollectionRef> getFeatureCollections() {
    List<FeatureCollectionRef> result = new ArrayList<>();
    for (Map.Entry<String, DataRoot> entry : dataRootPathMatcher.getValues()) {
      DataRoot droot = entry.getValue();
      if (droot.getFeatureCollection() != null)
        result.add(droot.getFeatureCollection());
    }
    return result;
  }

  public void makeDebugActions() {
    DebugCommands.Category debugHandler = debugCommands.findCategory("catalogs");
    DebugCommands.Action act;

    act = new DebugCommands.Action("showStatic", "Show root catalogs") {
      public void doAction(DebugCommands.Event e) {
        StringBuilder sbuff = new StringBuilder();
        synchronized (DataRootManager.this) {
          List<String> list = ccManager.getRootCatalogKeys();
          for (String catPath : list) {
            sbuff.append(" catalog= ").append(catPath).append("\n");
            //String filename = StringUtil2.unescape(cat.getUriString());
            //sbuff.append(" from= ").append(filename).append("\n");
          }
        }
        e.pw.println(StringUtil2.quoteHtmlContent("\n" + sbuff.toString()));
      }
    };
    debugHandler.addAction(act);

    act = new DebugCommands.Action("showDataRootPaths", "Show data roots paths") {
      public void doAction(DebugCommands.Event e) {
        synchronized (DataRootManager.this) {
          for (String drPath : dataRootPathMatcher.getKeys()) {
            e.pw.println(" <b>" + drPath + "</b>");
          }
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugCommands.Action("showDataRoots", "Show data roots") {
      public void doAction(DebugCommands.Event e) {
        synchronized (DataRootManager.this) {
          for (Map.Entry<String, DataRoot> entry : dataRootPathMatcher.getValues()) {     // LOOK sort
            DataRoot ds = entry.getValue();
            e.pw.print(" <b>" + ds.getPath() + "</b>");
            String url = DataRootManager.this.tdsContext.getContextPath() + "/admin/dataDir/" + ds.getPath() + "/";
            String type = (ds.getDatasetScan() == null) ? "root" : "scan";
            e.pw.println(" for " + type + " directory= <a href='" + url + "'>" + ds.getDirLocation() + "</a> ");
          }
        }
      }
    };
    debugHandler.addAction(act);


    /* act = new DebugCommands.Action("reinit", "Reinitialize") {
      public void doAction(DebugCommands.Event e) {
        try {
          singleton.reinit();
          e.pw.println("reinit ok");

        } catch (Exception e1) {
          e.pw.println("Error on reinit " + e1.getMessage());
          log.error("Error on reinit " + e1.getMessage());
        }
      }
    };
    debugHandler.addAction(act); */

  }

}



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
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import thredds.client.catalog.*;
import thredds.featurecollection.InvDatasetFeatureCollection;
import thredds.filesystem.MFileOS7;
import thredds.inventory.MFile;
import thredds.server.admin.DebugController;
import thredds.server.catalog.*;
import thredds.server.config.TdsContext;

import thredds.util.*;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * The DataRootHandler manages all the "data roots" for a given web application
 * and provides mappings from URLs to catalog and datasets
 * <p/>
 * <p>The "data roots" are read in from one or more trees of config catalogs
 * and are defined by the datasetScan and datasetRoot elements in the config catalogs.
 * <p/>
 * <p> Uses the singleton design pattern.
 *
 * @author caron
 * @since 1/23/2015
 */
@Component("DataRootManager")
@DependsOn("CdmInit")
public class DataRootManager {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DataRootManager.class);
  static private org.slf4j.Logger logCatalogInit = org.slf4j.LoggerFactory.getLogger(DataRootManager.class.getName() + ".catalogInit");
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

  // dont need to Guard/synchronize singleton, since creation and publication is only done by a servlet init() and therefore only in one thread (per ClassLoader).
  //Spring bean so --> there will be one per context (by default is a singleton in the Spring realm) 
  static private final String ERROR = "*** ERROR ";
  static public final boolean debug = false;

  static public DataRootManager getInstance() {
    return new DataRootManager(); // LOOK wrong
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private PathMatcher pathMatcher;

  @Autowired
  private ConfigCatalogManager ccManager;

  @Autowired
  private ConfigCatalogCache ccc;

  private List<ConfigListener> configListeners = new ArrayList<>();

  private DataRootManager() {

  }

  //Set method must be called so annotation at method level rather than property level
  @Resource(name = "dataRootLocationAliasExpanders")
  public void setDataRootLocationAliasExpanders(Map<String, String> aliases) {
    for (Map.Entry<String, String> entry : aliases.entrySet())
      ConfigCatalog.addAlias(entry.getKey(), entry.getValue());
  }

  //////////////////////////////////////////////

  /* public void init() {
  public void afterPropertiesSet() {

    //Registering first the AccessConfigListener
    registerConfigListener(new RestrictedAccessConfigListener());

    // Initialize any given DataRootLocationAliasExpanders that are TdsConfiguredPathAliasReplacement
    FileSource fileSource = tdsContext.getPublicDocFileSource();
    if (fileSource != null) {
      File file = fileSource.getFile("");
      if (file != null)
        ConfigCatalog.addAlias("content", StringUtils.cleanPath(file.getPath())); // LOOK
    }

    this.makeDebugActions();
    DatasetHandler.makeDebugActions();

    //Set the instance
    DataRootManager.setInstance(this);
  }  */

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

    DataRoot dataRoot = (DataRoot) pathMatcher.match(path);
    return (dataRoot == null) ? null : dataRoot.getDirLocation();
  }

  /**
   * Find the longest match for this path.
   *
   * @param fullpath the complete path name
   * @return best DataRoot or null if no match.
   */
  private DataRoot findDataRoot(String fullpath) {
    if ((fullpath.length() > 0) && (fullpath.charAt(0) == '/'))
      fullpath = fullpath.substring(1);

    return (DataRoot) pathMatcher.match(fullpath);
  }

  /**
   * Extract the DataRoot from the request.
   * Use this when you need to manipulate the path based on the part that matches a DataRoot.
   *
   * @param req the request
   * @return the DataRootMatch, or null if not found
   */
  //  see essupport SEG-622383
  public DataRootMatch findDataRootMatch(HttpServletRequest req) {
    String spath = TdsPathUtils.extractPath(req, null);

    if (spath.length() > 0) {
      if (spath.startsWith("/"))
        spath = spath.substring(1);
    }

    return findDataRootMatch(spath);
  }

  public DataRootMatch findDataRootMatch(String spath) {
    if (spath.startsWith("/"))
      spath = spath.substring(1);
    DataRoot dataRoot = findDataRoot(spath);
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

  /**
   * Return true if the given path matches a dataRoot, otherwise return false.
   * A succesful match means that the request is either for a dynamic catalog
   * or a dataset.
   *
   * @param path the request path, ie req.getServletPath() + req.getPathInfo()
   * @return true if the given path matches a dataRoot, otherwise false.
   */
  public boolean hasDataRootMatch(String path) {
    if (path.length() > 0)
      if (path.startsWith("/"))
        path = path.substring(1);

    DataRoot dataRoot = findDataRoot(path);
    if (dataRoot == null) {
      if (log.isDebugEnabled()) log.debug("hasDataRootMatch(): no DatasetScan for " + path);
      return false;
    }
    return true;
  }

  /**
   * Return the the MFile to which the given path maps.
   * Null is returned if the dataset does not exist, the
   * matching DatasetScan or DataRoot filters out the requested MFile, the MFile does not represent a File
   * (i.e., it is not a CrawlableDatasetFile), or an I/O error occurs
   *
   * @param reqPath the request path.
   * @return the location of the file on disk, or null
   * @throws IllegalStateException if the request is not for a descendant of (or the same as) the matching DatasetRoot collection location.
   */
  public String getLocationFromRequestPath(String reqPath) {
    if (reqPath.length() > 0) {
      if (reqPath.startsWith("/"))
        reqPath = reqPath.substring(1);
    }

    DataRoot reqDataRoot = findDataRoot(reqPath);
    if (reqDataRoot == null)
      return null;

    return reqDataRoot.getFileLocationFromRequestPath(reqPath);
  }

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

    // Check for static catalog.
    boolean reread = false;
    Catalog catalog = ccc.get(workPath);
    if (catalog != null) {  // see if its stale
      CalendarDate expiresDateType = catalog.getExpires();
      if ((expiresDateType != null) && expiresDateType.getMillis() < System.currentTimeMillis())
        reread = true;     // LOOK reread ??
    }

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


    // Check for dynamic catalog.
    if (catalog == null)
      catalog = makeDynamicCatalog(workPath, baseURI);

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
      boolean isLatest = path.endsWith("/latest.xml");
      if (isLatest)
        return match.dataRoot.getFeatureCollection().makeLatest(match.remaining, path, baseURI);
      else
        return match.dataRoot.getFeatureCollection().makeCatalog(match.remaining, path, baseURI);
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

    if (path.endsWith("/latest.xml")) return null; // latest is not handled here

    DatasetScan dscan = match.dataRoot.getDatasetScan();
    if (log.isDebugEnabled())
      log.debug("makeDynamicCatalog(): Calling makeCatalogForDirectory( " + baseURI + ", " + path + ").");
    Catalog cat = dscan.makeCatalogForDirectory(path, baseURI);

    if (null == cat) {
      log.error("makeDynamicCatalog(): makeCatalogForDirectory failed = " + workPath);
    }

    return cat;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  // debugging only !!

  public void showRoots(Formatter f) {
    Iterator iter = pathMatcher.iterator();
    while (iter.hasNext()) {
      DataRoot ds = (DataRoot) iter.next();
      f.format(" %s%n", ds.toString2());
    }
  }

  public List<InvDatasetFeatureCollection> getFeatureCollections() {
    return null;
  }


  public void makeDebugActions() {
    DebugController.Category debugHandler = DebugController.find("catalogs");
    DebugController.Action act;
    /* act = new DebugHandler.Action("showError", "Show catalog error logs") {
     public void doAction(DebugHandler.Event e) {
       synchronized ( DataRootHandler.this )
       {
         try {
          File fileOut = new File(contentPath + "logs/catalogError.log");
          FileOutputStream fos = new FileOutputStream(fileOut);
          //String contents = catalogErrorLog.toString();
          thredds.util.IO.writeContents(contents, fos);
          fos.close();
        } catch (IOException ioe) {
          log.error("DataRootHandler.writeCatalogErrorLog error ", ioe);
        }

         e.pw.println( StringUtil.quoteHtmlContent( "\n"+catalogErrorLog.toString()));
       }
     }
   };
   debugHandler.addAction( act);  */

    act = new DebugController.Action("showStatic", "Show root catalogs") {
      public void doAction(DebugController.Event e) {
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

    act = new DebugController.Action("showRoots", "Show data roots") {
      public void doAction(DebugController.Event e) {
        synchronized (DataRootManager.this) {
          Iterator iter = pathMatcher.iterator();
          while (iter.hasNext()) {
            DataRoot ds = (DataRoot) iter.next();
            e.pw.print(" <b>" + ds.getPath() + "</b>");
            String url = DataRootManager.this.tdsContext.getContextPath() + "/admin/dataDir/" + ds.getPath() + "/";
            String type = (ds.getDatasetScan() == null) ? "root" : "scan";
            e.pw.println(" for " + type + " directory= <a href='" + url + "'>" + ds.getDirLocation() + "</a> ");
          }
        }
      }
    };
    debugHandler.addAction(act);

    act = new DebugController.Action("getRoots", "Check data roots") {
      public void doAction(DebugController.Event e) {
        synchronized (DataRootManager.this) {
          e.pw.print("<pre>\n");
          Iterator iter = pathMatcher.iterator();
          boolean ok = true;
          while (iter.hasNext()) {
            DataRoot ds = (DataRoot) iter.next();
            if ((ds.getDirLocation() == null)) continue;

            try {
              File f = new File(ds.getDirLocation());
              if (!f.exists()) {
                e.pw.print("MISSING on dir = " + ds.getDirLocation() + " for path = " + ds.getPath() + "\n");
                ok = false;
              }
            } catch (Throwable t) {
              e.pw.print("ERROR on dir = " + ds.getDirLocation() + " for path = " + ds.getPath() + "\n");
              e.pw.print(t.getMessage() + "\n");
              ok = false;
            }
          }
          if (ok)
            e.pw.print("ALL OK\n");
          e.pw.print("</pre>\n");
        }
      }
    };
    debugHandler.addAction(act);

    /* act = new DebugController.Action("reinit", "Reinitialize") {
      public void doAction(DebugController.Event e) {
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

  /**
   * To receive notice of TDS configuration events, implement this interface
   * and use the DataRootHandler.registerConfigListener() method to register
   * an instance with a DataRootHandler instance.
   * <p/>
   * Configuration events include start and end of configuration, inclusion
   * of a catalog in configuration, and finding a dataset in configuration.
   * <p/>
   * Concurrency issues:<br>
   * 1) As this is a servlet framework, requests that configuration be reinitialized
   * may occur concurrently with requests for the information the listener is
   * keeping. Be careful not to allow access to the information during
   * configuration.<br>
   * 2) The longer the configuration process, the more time there is to have
   * a concurrency issue come up. So, try to keep responses to these events
   * fairly light weight. Or build new configuration information and synchronize
   * and switch only when the already built config information is being switched
   * with the existing config information.
   */
  public interface ConfigListener {
    /**
     * Recieve notification that configuration has started.
     */
    public void configStart();

    /**
     * Recieve notification that configuration has completed.
     */
    public void configEnd();

    /**
     * Recieve notification on the inclusion of a configuration catalog.
     *
     * @param catalog the catalog being included in configuration.
     */
    public void configCatalog(Catalog catalog);

    /**
     * Recieve notification that configuration has found a dataset.
     *
     * @param dataset the dataset found during configuration.
     */
    public void configDataset(Dataset dataset);
  }

}



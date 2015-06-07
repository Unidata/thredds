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
 * and provides mappings from URLs to catalog and datasets.
 * <p/>
 * <p>The "data roots" are read in from one or more trees of config catalogs
 * and are defined by the datasetScan and datasetRoot and featureCollection elements in the config catalogs.
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

  static public final boolean debug = true;

  static public DataRootManager getInstance() {
    return new DataRootManager(); // Used for testing only
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Autowired
  private TdsContext tdsContext;

  @Autowired
  private DataRootPathMatcher<DataRoot> dataRootPathMatcher;

  @Autowired
  private DebugCommands debugCommands;

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
    FileSource fileSource = tdsContext.getPublicContentDirSource(); // content -> {tomcat}/content/thredds/public
    if (fileSource != null) {
      File file = fileSource.getFile("");
      if (file != null)
        DataRootAlias.addAlias("content", StringUtils.cleanPath(file.getPath())); // LOOK
    }

    makeDebugActions();
    startupLog.info("DataRootManager:" + DataRootAlias.size() +" aliases set ");
  }

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



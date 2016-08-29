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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.util.StringUtils;
import thredds.server.admin.DebugCommands;
import thredds.server.catalog.*;
import thredds.server.catalog.tracker.DataRootExt;
import thredds.server.config.TdsContext;

import thredds.util.filesource.FileSource;
import ucar.nc2.util.AliasTranslator;

import javax.annotation.Resource;
import java.io.File;
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
@Component
public class DataRootManager implements InitializingBean {
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");
  static private final Logger logger = LoggerFactory.getLogger(DataRootManager.class);

  static public final boolean debug = true;

  static public DataRootManager getInstance() {
    return new DataRootManager(); // Used for testing only
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  @Autowired
  private TdsContext tdsContext;

  // injected by catalogInitializer, when catalogs are reread, so cant be spring managed
  private DataRootPathMatcher dataRootPathMatcher;

  @Autowired
  private DebugCommands debugCommands;

  private DataRootManager() {
  }

  //Set method must be called so annotation at method level rather than property level
  @Resource(name = "dataRootLocationAliasExpanders")
  public void setDataRootLocationAliasExpanders(Map<String, String> aliases) {
    for (Map.Entry<String, String> entry : aliases.entrySet())
      AliasTranslator.addAlias("${" + entry.getKey() + "}", entry.getValue());
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    FileSource fileSource = tdsContext.getPublicContentDirSource(); // content -> {tomcat}/content/thredds/public
    if (fileSource != null) {
      File file = fileSource.getFile("");
      if (file != null)
        AliasTranslator.addAlias("content", StringUtils.cleanPath(file.getPath())); // LOOK
    }

    File  uploaddir = tdsContext.getUploadDir();
    if(uploaddir != null) {
      AliasTranslator.addAlias("${tds.upload.dir}",StringUtils.cleanPath(uploaddir.getAbsolutePath()));
    }

    makeDebugActions();
    startupLog.info("DataRootManager:" + AliasTranslator.size() +" aliases set ");
  }

  public synchronized void setDataRootPathMatcher(DataRootPathMatcher dataRootPathMatcher) {
    this.dataRootPathMatcher = dataRootPathMatcher;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  static public class DataRootMatch {
    public String rootPath;     // this is the matching part of the URL
    public String remaining;   // this is the part of the URL that didnt match
    public String dirLocation;   // this is the directory that should be substituted for the rootPath
    public DataRoot dataRoot;  // this is the directory that should be substituted for the rootPath
  }

  /*
   * Find the location match for a dataRoot.
   * Aliasing has been done.
   *
   * @param path the dataRoot path name
   * @return best DataRoot location or null if no match.
   *
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
   *
  private DataRootMatch findDataRootMatch(HttpServletRequest req) {
    String spath = TdsPathUtils.extractPath(req, null);
    return findDataRootMatch(spath);
  } */

  public DataRootMatch findDataRootMatch(String spath) {
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

  private synchronized DataRoot findDataRoot(String spath) {
    if (spath == null)
      return null;
    if (spath.startsWith("/"))
      spath = spath.substring(1);

    // LOOK could it be safe to swap dataRootPathMatcher without synchronizing?
    return dataRootPathMatcher.findDataRoot(spath);
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

  public synchronized void showRoots(Formatter f) {
    List<Map.Entry<String, DataRootExt>> list = new ArrayList<>(dataRootPathMatcher.getValues());
    Collections.sort(list, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));    // java 8 lambda, baby

    for (Map.Entry<String, DataRootExt> entry : list) {
      f.format(" %s%n", entry.getValue());
    }
  }

  public synchronized List<FeatureCollectionRef> getFeatureCollections() {
    List<FeatureCollectionRef> result = new ArrayList<>();
    for (Map.Entry<String, DataRootExt> entry : dataRootPathMatcher.getValues()) {
      DataRootExt drootExt = entry.getValue();
      if (drootExt.getType() == DataRoot.Type.featureCollection) {
        DataRoot dataRoot = dataRootPathMatcher.convert2DataRoot(drootExt);
        if (dataRoot == null) {
          logger.error("Cant find dataRoot {} in DataRootPathMatcher", drootExt);
          continue;
        }
        result.add(dataRoot.getFeatureCollection());
      }
    }
    return result;
  }

  public synchronized FeatureCollectionRef findFeatureCollection(String collectionName) {
    for (Map.Entry<String, DataRootExt> entry : dataRootPathMatcher.getValues()) {
      DataRootExt drootExt = entry.getValue();
      if (drootExt.getType() == DataRoot.Type.featureCollection && drootExt.getName().equals(collectionName)) {
        DataRoot dataRoot = dataRootPathMatcher.convert2DataRoot(drootExt);
        return dataRoot.getFeatureCollection();
      }
    }
    return null;
  }

  public void makeDebugActions() {
    DebugCommands.Category debugHandler = debugCommands.findCategory("Catalogs");
    DebugCommands.Action act;

    act = new DebugCommands.Action("showDataRoots", "Show data roots") {
      public void doAction(DebugCommands.Event e) {
        synchronized (DataRootManager.this) {
          List<Map.Entry<String, DataRootExt>> list = new ArrayList<>(dataRootPathMatcher.getValues());
          Collections.sort(list, (o1, o2) -> o1.getKey().compareTo(o2.getKey()));    // java 8 lambda, baby

          for (Map.Entry<String, DataRootExt> entry : list) {
            DataRootExt ds = entry.getValue();
            e.pw.printf(" <b>%s</b>", ds.getPath());
            String url = DataRootManager.this.tdsContext.getContextPath() + "/admin/dir/dataDir/" + ds.getPath() + "/";
            e.pw.printf(" for %s directory= <a href='%s'>%s</a>", ds.getType(), url, ds.getDirLocation());
            if (ds.getRestrict() != null)
              e.pw.printf(" (restrict ='%s')", ds.getRestrict());
            e.pw.printf("%n");
          }
        }
      }
    };
    debugHandler.addAction(act);

  }

}



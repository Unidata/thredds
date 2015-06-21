/* Copyright */
package thredds.server.catalog;

import org.springframework.beans.factory.annotation.Autowired;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.server.catalog.tracker.DataRootExt;
import thredds.server.catalog.tracker.DatasetTracker;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Find the dataRoot path from the request, by getting the longest match.
 * Use a TreeSet for minimum in-memory use.
 * Keep the objects in a seperate map that could be off-heap and persistent.
 *
 * @author caron
 * @since 4/1/2015
 */
public class DataRootPathMatcher {
  static private final Logger logger = LoggerFactory.getLogger(DataRootPathMatcher.class);
  static private org.slf4j.Logger logCatalogInit = org.slf4j.LoggerFactory.getLogger(DataRootPathMatcher.class.getName()+".initCatalog");
  static private final boolean debug = false;
  static private final String ERROR = "*** ERROR: ";
  static private boolean skipTestDataDir = true;

  private static class PathComparator implements Comparator<String> {
    public int compare(String s1, String s2) {
      int compare = s2.compareTo( s1); // reverse sort
      if (debug) System.out.println(" compare "+s1+" to "+s2+" = "+compare);
      return compare;
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  @Autowired
  private DatasetTracker tracker;

  @Autowired
  private ConfigCatalogCache ccc;

  private final TreeSet<String> treeSet;    // this should be in-memory for speed
  private final Map<String, DataRootExt> map;         // this could be turned into an off-heap cache if needed, with persistence.

  public DataRootPathMatcher() {
    treeSet = new TreeSet<>( new PathComparator());
    map = new HashMap<>();
  }

  /**
   * Add a dataRootExt to in-memory tree.
   * @return true if not already exist
   */
  public boolean put(DataRootExt dateRootExt) {
    map.put(dateRootExt.getPath(), dateRootExt);
    return treeSet.add(dateRootExt.getPath());
  }

  /**
   * See if this object already exists in the collection
   * @param path find object that has this key
   * @return true if already contains the key
   */
  public boolean contains(String  path) {
    return treeSet.contains(path);
  }

  public DataRootExt get(String  path) {
    return map.get(path);
  }

  /**
   * Get an iterator over the dataRoot paths, in sorted order.
   * @return iterator
   */
  public Iterable<String> getKeys() {
    return treeSet;
  }

  /**
   * Get an iterator over the dataRoot keys and values
   * @return iterator
   */
  public Set<Map.Entry<String, DataRootExt>> getValues() {
    return map.entrySet();
  }

  /**
   * Find the longest path match.
   * @param reqPath find object with longest match where reqPath.startsWith( key)
   * @return the value whose key is the longest that matches path, or null if none
   */
  public String findLongestPathMatch( String reqPath) {
    SortedSet<String> tail = treeSet.tailSet( reqPath);
    if (tail.isEmpty()) return null;
    String after = tail.first();
    if (reqPath.startsWith( after)) // common case
      return tail.first();

    // have to check more, until no common starting chars
    for (String key : tail) {
      if (reqPath.startsWith(key))
        return key;

      // terminate when there's no match at all.
      if (StringUtil2.match(reqPath, key) == 0)
        break;
    }

    return null;
  }

  /**
   * Find the longest DataRoot match.
   * @param reqPath find object with longest match where reqPath.startsWith( key)
   * @return the value whose key is the longest that matches path, or null if none
   */
  public DataRoot findDataRoot( String reqPath) {
    String path =  findLongestPathMatch(reqPath);
    if (path == null) return null;
    DataRootExt dataRootExt = map.get(path);
    if (dataRootExt == null) {
      logger.error("DataRootPath Matcher inconsistent state");
      return null;
    }
    return findDataRoot(dataRootExt);
  }

  public DataRoot findDataRoot( DataRootExt dataRootExt) {
    DataRoot dataRoot = dataRootExt.getDataRoot();
    if (dataRoot != null) return dataRoot;

    // otherwise must read it in
    dataRoot = readDataRoot(dataRootExt);
    dataRootExt.setDataRoot(dataRoot);
    return dataRoot;
  }

  public DataRoot readDataRoot( DataRootExt dataRootExt) {

    try {
      ConfigCatalog cat = ccc.get(dataRootExt.getCatLocation());
    } catch (IOException e) {
      e.printStackTrace();
    }
    // LOOK whats next ?
    return null;
  }

  public void readDataRoots() {
    for (DataRootExt dre : tracker.getDataRoots()) {
      put(dre);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // building up the data roots after reading catalogs

  public boolean addRoot(DatasetScan dscan, String catalogRelPath) {
    String path = dscan.getPath();

    if (path == null) {
      logCatalogInit.error(ERROR + "DatasetScan '" + dscan.getName() + "' missing the path attribute.");
      return false;
    }

    DataRootExt dre = get(path); // check for duplicates
    if (dre != null) {
        logCatalogInit.error(ERROR + "DatasetScan trying to add duplicate dataRoot =<" + path + ">  already mapped to directory= <" + dre.getDirLocation() + ">" +
                " wanted to map to =<" + dscan.getScanLocation() + "> in catalog " + catalogRelPath);
      return false;
    }

    // check for existence
    File file = new File(dscan.getScanLocation());
    if (!skipTestDataDir && !file.exists()) {
      logCatalogInit.error(ERROR + "DatasetScan path ='" + path + "' directory= <" + dscan.getScanLocation() + "> does not exist");
      return false;
    }

    // add it
    putRoot(new DataRoot(dscan), catalogRelPath);
    logCatalogInit.debug(" added rootPath=<" + path + ">  for directory= <" + dscan.getScanLocation() + ">");
    return true;
  }

  public boolean addRoot(FeatureCollectionRef fc, String catalogRelPath) {
    String path = fc.getPath();

    if (path == null) {
      logCatalogInit.error(ERROR + "DatasetScan '" + fc.getName() + "' missing the path attribute.");
      return false;
    }

    DataRootExt dre = get(path); // check for duplicates
    if (dre != null) {
        logCatalogInit.error(ERROR + "FeatureCollection trying to add duplicate dataRoot =<" + path + ">  already mapped to directory= <" + dre.getDirLocation() + ">" +
                " wanted to map to =<" + fc.getTopDirectoryLocation() + "> in catalog " + catalogRelPath);
      return false;
    }

    // check for existence
    File file = new File(fc.getTopDirectoryLocation());
    if (!skipTestDataDir && !file.exists()) {
      logCatalogInit.error(ERROR + "FeatureCollection path ='" + path + "' directory= <" + fc.getTopDirectoryLocation() + "> does not exist");
      return false;
    }

    // add it
    putRoot(new DataRoot(fc), catalogRelPath);
    logCatalogInit.debug(" added rootPath=<" + path + ">  for feature collection= <" + fc.getName() + ">");
    return true;
  }

  public boolean addRoot(DatasetRootConfig config, String catalogRelPath) {
    String path = config.getPath();

    if (path == null) {
      logCatalogInit.error(ERROR + "DatasetRoot '" + config + "' missing the path attribute.");
      return false;
    }

    DataRootExt dre = get(path); // check for duplicates
    if (dre != null) {
        logCatalogInit.error(ERROR + "DatasetRoot trying to add duplicate dataRoot =<" + path + ">  already mapped to directory= <" + dre.getDirLocation() + ">" +
                " wanted to map to =<" + config.getLocation() + "> in catalog " + catalogRelPath);
      return false;
    }

    // translate and check for existance
    String location = AliasTranslator.translateAlias(config.getLocation());
    File file = new File(location);
    if (!skipTestDataDir && !file.exists()) {
      logCatalogInit.error(ERROR + "DatasetRootConfig path ='" + path + "' directory= <" + location + "> does not exist");
      return false;
    }

    // add it
    putRoot(new DataRoot(config.getPath(), location), catalogRelPath);
    logCatalogInit.debug(" added rootPath=<" + path + ">  for DatasetRootConfig location= <" + location + ">");
    return true;
  }

  public boolean addRoot(CatalogScan catScan, String catalogRelPath) {
    String path = catScan.getPath();

    if (path == null) {
      logCatalogInit.error(ERROR + "CatalogScan '" + catScan + "' missing the path attribute.");
      return false;
    }

    DataRootExt dre = get(path); // check for duplicates
    if (dre != null) {
        logCatalogInit.error(ERROR + "CatalogScan trying to add duplicate dataRoot =<" + path + ">  already mapped to directory= <" + dre.getDirLocation() + ">" +
                " wanted to map to =<" + catScan.getLocation() + "> in catalog " + catalogRelPath);
      return false;
    }

    // translate and check for existence
    String location = AliasTranslator.translateAlias(catScan.getLocation());
    File file = new File(location);
    if (!skipTestDataDir && !file.exists()) {
      logCatalogInit.error(ERROR + "CatalogScan path ='" + path + "' directory= <" + location + "> does not exist");
      return false;
    }

    // add it
    putRoot(new DataRoot(catScan), catalogRelPath);
    logCatalogInit.debug(" added rootPath=<" + path + ">  for CatalogScan location= <" + location + ">");
    return true;
  }

  private void putRoot(DataRoot droot,  String catalogRelPath) {
    DataRootExt drootExt = new DataRootExt(droot, catalogRelPath);
    put(drootExt);
    tracker.trackDataRoot(drootExt);
  }

}

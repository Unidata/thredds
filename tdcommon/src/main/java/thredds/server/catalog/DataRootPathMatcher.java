/* Copyright */
package thredds.server.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.CatalogRef;
import thredds.client.catalog.Dataset;
import thredds.client.catalog.Service;
import thredds.server.catalog.tracker.DataRootExt;
import thredds.server.catalog.tracker.DataRootTracker;
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
  static private org.slf4j.Logger logCatalogInit = org.slf4j.LoggerFactory.getLogger("catalogInit");
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

  private ConfigCatalogCache ccc;
  private DataRootTracker tracker;

  private final TreeSet<String> treeSet = new TreeSet<>( new PathComparator());    // this should be in-memory for speed
  private final Map<String, DataRootExt> map = new HashMap<>();         // this could be turned into an off-heap cache if needed, with persistence.

  public DataRootPathMatcher(ConfigCatalogCache ccc, DataRootTracker tracker) {
    this.ccc = ccc;
    this.tracker = tracker;

    for (DataRootExt dre : tracker.getDataRoots()) {
      put(dre);
    }
  }

  /**
   * Add a dataRootExt to in-memory tree.
   * @return true if not already exist
   */
  private boolean put(DataRootExt dateRootExt) {
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
   * Get an iterator over the dataRoot keys and values. debug
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
    SortedSet<String> tail = treeSet.tailSet(reqPath);
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
      logger.error("DataRootPathMatcher found path {} but not in map", path);
      return null;
    }
    return findDataRoot(dataRootExt);
  }

  public DataRoot findDataRoot( DataRootExt dataRootExt) {
    DataRoot dataRoot = dataRootExt.getDataRoot();
    if (dataRoot != null) return dataRoot;

    // otherwise must read the catalog that its in
    dataRoot = readDataRootFromCatalog(dataRootExt);
    dataRootExt.setDataRoot(dataRoot);
    return dataRoot;
  }

  private DataRoot readDataRootFromCatalog( DataRootExt dataRootExt) {
    try {
      ConfigCatalog cat = ccc.get(dataRootExt.getCatLocation());
      extractDataRoots(dataRootExt.getCatLocation(), cat.getDatasets(), false);  // will create a new DataRootExt and replace this one in the map
      DataRootExt dataRootExtNew = map.get(dataRootExt.getPath());
      if (null == dataRootExtNew) {
        logger.error("Reading catalog " + dataRootExt.getCatLocation() + " failed to find dataRoot path=" + dataRootExt.getPath());
        return null;
      }
      return dataRootExtNew.getDataRoot();

    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // building up the data roots after reading catalogs

  /**
   * Finds datasetScan, datasetFmrc
   * Look for duplicate Ids (give message). Dont follow catRefs.
   *
   * @param dsList the list of Dataset
   */
  public void extractDataRoots(String catalogRelPath, List<Dataset> dsList, boolean checkDups) {

    for (Dataset dataset : dsList) {
      if (dataset instanceof DatasetScan) {
        DatasetScan ds = (DatasetScan) dataset;
        Service service = ds.getServiceDefault();
        if (service == null) {
          logCatalogInit.error(ERROR + "DatasetScan " + ds.getName() + " has no default Service - skipping");  // LOOK needed?
          continue;
        }
        addRoot(ds, catalogRelPath, checkDups);

      } else if (dataset instanceof FeatureCollectionRef) {
        FeatureCollectionRef fc = (FeatureCollectionRef) dataset;
        addRoot(fc, catalogRelPath, checkDups);

      } else if (dataset instanceof CatalogScan) {
        CatalogScan catScan = (CatalogScan) dataset;
        addRoot(catScan, catalogRelPath, checkDups);
      }

      if (!(dataset instanceof CatalogRef)) {
        // recurse
        extractDataRoots(catalogRelPath, dataset.getDatasets(), checkDups);
      }
    }
  }

  private boolean addRoot(DatasetScan dscan, String catalogRelPath, boolean checkDups) {
    String path = dscan.getPath();

    if (path == null) {
      logCatalogInit.error(ERROR + "DatasetScan '" + dscan.getName() + "' missing the path attribute.");
      return false;
    }

    if (checkDups) {
      DataRootExt dre = get(path); // check for duplicates
      if (dre != null) {
        logCatalogInit.error(ERROR + "DatasetScan trying to add duplicate dataRoot =<" + path + ">  already mapped to directory= <" + dre.getDirLocation() + ">" +
                " wanted to map to =<" + dscan.getScanLocation() + "> in catalog " + catalogRelPath);
        return false;
      }
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

  private boolean addRoot(FeatureCollectionRef fc, String catalogRelPath, boolean checkDups) {
    String path = fc.getPath();

    if (path == null) {
      logCatalogInit.error(ERROR + "DatasetScan '" + fc.getName() + "' missing the path attribute.");
      return false;
    }

    if (checkDups) {
      DataRootExt dre = get(path); // check for duplicates
      if (dre != null) {
        logCatalogInit.error(ERROR + "FeatureCollection trying to add duplicate dataRoot =<" + path + ">  already mapped to directory= <" + dre.getDirLocation() + ">" +
                " wanted to map to =<" + fc.getTopDirectoryLocation() + "> in catalog " + catalogRelPath);
        return false;
      }
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

  private boolean addRoot(CatalogScan catScan, String catalogRelPath, boolean checkDups) {
    String path = catScan.getPath();

    if (path == null) {
      logCatalogInit.error(ERROR + "CatalogScan '" + catScan + "' missing the path attribute.");
      return false;
    }

    if (checkDups) {
      DataRootExt dre = get(path); // check for duplicates
      if (dre != null) {
        logCatalogInit.error(ERROR + "CatalogScan trying to add duplicate dataRoot =<" + path + ">  already mapped to directory= <" + dre.getDirLocation() + ">" +
                " wanted to map to =<" + catScan.getLocation() + "> in catalog " + catalogRelPath);
        return false;
      }
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

  private void putRoot(DataRoot droot, String catalogRelPath) {
    DataRootExt drootExt = new DataRootExt(droot, catalogRelPath);
    put(drootExt);
    tracker.trackDataRoot(drootExt);
  }

}

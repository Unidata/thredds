/* Copyright */
package thredds.server.catalog.tracker;

import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.Dataset;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.FeatureCollectionRef;

import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

/**
 * DatasetTracker using in memory hash table
 *
 * @author caron
 * @since 6/6/2015
 */
public class DatasetTrackerInMem implements DatasetTracker {
  static private final Logger logger = LoggerFactory.getLogger(DatasetTracker.class);
  //static final boolean debugResourceControl = false;

  // resource control
  private HashMap<String, String> resourceControlHash = new HashMap<>(); // path, restrictAccess string for datasets
  // private volatile PathMatcher<String> resourceControlMatcher = new PathMatcher<>(); // path, restrictAccess string for datasetScan
  private boolean hasResourceControl = false;

  // InvDataset (not DatasetScan, DatasetFmrc) that have an NcML element in it. key is the request Path
  private Map<String, String> ncmlDatasetHash = new HashMap<>();
  //private int count;

  public boolean init(String dirPath, long maxDatasets) {
    return true;
  }

  public boolean trackDataset(Dataset dataset, Callback callback) {
    if (callback != null) callback.hasDataset(dataset);

    if (dataset.getRestrictAccess() != null) {
      if (callback != null) callback.hasRestriction(dataset);
      else putResourceControl(dataset);
    }

    // dont track ncml for DatasetScan or FeatureCollectionRef
    if (dataset instanceof DatasetScan) return false;
    if (dataset instanceof FeatureCollectionRef) return false;

    if (dataset.getNcmlElement() != null) {
      if (callback != null) callback.hasNcml(dataset);
      // want the string representation
      Element ncmlElem = dataset.getNcmlElement();
      XMLOutputter xmlOut = new XMLOutputter();
      String ncml = xmlOut.outputString(ncmlElem);
      System.out.printf("%s%n", ncml);
      ncmlDatasetHash.put(dataset.getUrlPath(), ncml);
    }
    return true;
  }

  /**
   * This tracks Dataset elements that have resource control attributes
   *
   * @param ds the dataset
   */
  void putResourceControl(Dataset ds) {
    if (logger.isDebugEnabled()) logger.debug("putResourceControl " + ds.getRestrictAccess() + " for " + ds.getName());
    resourceControlHash.put(ds.getUrlPath(), ds.getRestrictAccess());

    // resourceControl is inherited, but no guarentee that children paths are related, unless its a
    //   DatasetScan or InvDatasetFmrc. So we keep track of all datasets that have a ResourceControl, including children
    // DatasetScan and InvDatasetFmrc must use a PathMatcher, others can use exact match (hash)

    /* if (ds instanceof DatasetScan) {
      DatasetScan scan = (DatasetScan) ds;
      if (debugResourceControl)
        System.out.println("putResourceControl " + ds.getRestrictAccess() + " for datasetScan " + scan.getPath());
      resourceControlMatcher.put(scan.getPath(), ds.getRestrictAccess());

    } else { // dataset
      if (debugResourceControl)
        System.out.println("putResourceControl " + ds.getRestrictAccess() + " for dataset " + ds.getUrlPath());

      // LOOK: seems like you only need to add if InvAccess.InvService.isReletive
      // LOOK: seems like we should use resourceControlMatcher to make sure we match .dods, etc
      for (Access access : ds.getAccess()) {
        if (access.getService().isRelativeBase())
          resourceControlHash.put(access.getUrlPath(), ds.getRestrictAccess());
      }
    }  */

    hasResourceControl = true;
  }

  public String findResourceControl(String path) {
    if (!hasResourceControl) return null;

    if (path.startsWith("/"))
      path = path.substring(1);

    return resourceControlHash.get(path);
    //if (null == rc)
    //  rc = resourceControlMatcher.match(path);
    // return rc;
  }

  @Override
  public String findNcml(String path) {
    return ncmlDatasetHash.get(path);
  }

  @Override
  public boolean exists() {
    return false;
  }

  @Override
  public boolean reinit() {
    resourceControlHash = new HashMap<>(); // path, restrictAccess string for datasets
    ncmlDatasetHash = new HashMap<>();
    return true;
  }

  @Override
  public void close() {
  }

  @Override
  public void save() {
  }

  @Override
  public void showDB(Formatter f) {
    f.format("DatasetTrackerInMem%n");
    for (Map.Entry<String, String> entry : ncmlDatasetHash.entrySet()) {
      f.format(" %s == %s%n", entry.getKey(), entry.getValue());
    }
  }
}

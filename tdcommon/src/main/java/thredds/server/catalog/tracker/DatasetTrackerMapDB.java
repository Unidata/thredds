/* Copyright */
package thredds.server.catalog.tracker;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.client.catalog.Access;
import thredds.client.catalog.Dataset;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.FeatureCollectionRef;

import java.io.Externalizable;
import java.io.File;

/**
 * org.mapdb.DB implementation of DatasetTracker
 *
 * @author caron
 * @since 6/7/2015
 */
public class DatasetTrackerMapDB implements DatasetTracker {
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

  private HTreeMap<String, Externalizable> map;

  public boolean init(String pathname, long maxDatasets) {
    File file = new File(pathname+"/mapdb1.dat");

    try {
      DB db = DBMaker.newFileDB(file)
              .transactionDisable()
              .mmapFileEnableIfSupported()
              .closeOnJvmShutdown()
              .make();

      map = db.getHashMap("datasets");
      return true;

    } catch (Throwable e) {
      startupLog.error("DatasetTrackerMapDB failed on '"+ file.getAbsolutePath()+ "', delete catalog cache and reload ", e);
      if (file.exists()) {
        boolean wasDeleted = file.delete();
        if (!wasDeleted) {
          startupLog.error("DatasetTrackerMapDB not able to delete {} ", file.getAbsolutePath());
          throw e;
        }
      }

      // try again
      DB db = DBMaker.newFileDB(file)
              .transactionDisable()
              .mmapFileEnableIfSupported()
              .closeOnJvmShutdown()
              .make();

      map = db.getHashMap("datasets");
      return true;
    }
  }

  @Override
  public boolean trackDataset(Dataset dataset, Callback callback) {
    if (callback != null) {
      callback.hasDataset(dataset);
      if (dataset.getRestrictAccess() != null) {
        callback.hasRestriction(dataset);
      }
      if (dataset.getNcmlElement() != null) {
        callback.hasNcml(dataset);
      }
    }

    boolean hasRestrict = dataset.getRestrictAccess() != null;
    boolean hasNcml = (dataset.getNcmlElement() == null) && !(dataset instanceof DatasetScan) && (dataset instanceof FeatureCollectionRef);
    if (!hasRestrict && !hasNcml) return false;

    String path = null;
    for (Access access : dataset.getAccess()) {

      String accessPath = access.getUrlPath();
      if (accessPath == null)
        System.out.println("HEY");
      if (path == null) path = accessPath;
      else if (!path.equals(access.getUrlPath())) {
        System.out.printf(" %s%n %s%n%n", path, accessPath);
      }
    }
    if (path == null)
      return false;

    CatalogExt dsext = new CatalogExt(dataset, hasNcml);
    map.put(path, dsext);
    return true;
  }

  @Override
  public String findResourceControl(String path) {
    CatalogExt dext = (CatalogExt) map.get(path);
    if (dext == null) return null;
    return dext.getRestrictAccess();
  }

  @Override
  public String findNcml(String path) {
    CatalogExt dext = (CatalogExt) map.get(path);
    if (dext == null) return null;
    return dext.getNcml();
  }

  @Override
  public CatalogExt findCatalogExt(String path) {
    return (CatalogExt) map.get(path);
  }

  public void close() {
    map.close();
  }
}

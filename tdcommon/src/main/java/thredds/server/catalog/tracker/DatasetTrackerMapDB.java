/* Copyright */
package thredds.server.catalog.tracker;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import thredds.client.catalog.Access;
import thredds.client.catalog.Dataset;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.FeatureCollectionRef;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;

/**
 * org.mapdb.DB implementation of DatasetTracker
 *
 * @author caron
 * @since 6/7/2015
 */
public class DatasetTrackerMapDB implements DatasetTracker {
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

  private String pathname;
  private File dbFile;
  private DB db;
  private HTreeMap<String, Externalizable> datasetMap;
  private boolean alreadyExists;

  CatalogTracker catTracker;
  DataRootTracker dataRootTracker;

  public boolean init(String pathname, long maxDatasets) {
    this.pathname = pathname;
    dbFile = new File(pathname+"/mapdb.dat");

    try {
      open();
      startupLog.info("DatasetTrackerMapDB opened success on '"+ dbFile.getAbsolutePath()+ "'");
      return true;

    } catch (Throwable e) {
      startupLog.error("DatasetTrackerMapDB failed on '"+ dbFile.getAbsolutePath()+ "', delete catalog cache and reload ", e);
      return reinit();
    }
  }

  public void close() throws IOException {
    if (catTracker != null) {
      catTracker.save();
      catTracker = null;
    }

    if (dataRootTracker != null) {
      dataRootTracker.save();
      dataRootTracker = null;
    }

    if (db != null) {
     datasetMap.close();
      db.close();
      startupLog.info("DatasetTrackerMapDB closed");
      db = null;
    }
  }

  @Override
  public boolean exists() {
    return alreadyExists;
  }

  @Override
  public boolean reinit() {
    if (dbFile.exists()) {
      boolean wasDeleted = dbFile.delete();
      if (!wasDeleted) {
        startupLog.error("DatasetTrackerMapDB not able to delete {} ", dbFile.getAbsolutePath());
        return false;
      }
    }

    try {
      open();
      alreadyExists = false;
      return true;

    } catch (Throwable e) {
      startupLog.error("DatasetTrackerMapDB failed on '"+ dbFile.getAbsolutePath()+ "', delete catalog cache and reload ", e);
      return false;
    }
  }

  private void open() {
    db = DBMaker.newFileDB(dbFile)
            .transactionDisable()
            .mmapFileEnableIfSupported()
            .closeOnJvmShutdown()
            .make();

    alreadyExists = db.exists("datasets");  // LOOK ??
    datasetMap = db.getHashMap("datasets");

    catTracker = new CatalogTracker(pathname+"/catTracker.dat");
    dataRootTracker = new DataRootTracker(pathname+"/datarootTracker.dat");
  }

  ////////////////////////////////////////////////////////////////
  // datasets

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

    try {
      DatasetExt dsext = new DatasetExt(0, dataset, hasNcml);
      datasetMap.put(path, dsext);
      return true;
    } catch (Throwable t) {
      startupLog.error("MapDB afailed to put DatasetExt", t);
      //        t.printStackTrace();
      return false;
    }
  }

  @Override
  public String findResourceControl(String path) {
    DatasetExt dext = (DatasetExt) datasetMap.get(path);
    if (dext == null) return null;
    return dext.getRestrictAccess();
  }

  @Override
  public String findNcml(String path) {
    DatasetExt dext = (DatasetExt) datasetMap.get(path);
    if (dext == null) return null;
    return dext.getNcml();
  }

  ////////////////////////////////////////////////////////////////
  // dataroots

  @Override
  public boolean trackDataRoot(DataRootExt dsext) {
    return dataRootTracker.trackDataRoot(dsext);
  }

  @Override
  public Iterable<? extends DataRootExt> getDataRoots() {
    return dataRootTracker.getDataRoots();
  }

  ////////////////////////////////////////////////////////////////
  // catalogs

  @Override
  public boolean trackCatalog(CatalogExt catext) {
    return catTracker.trackCatalog(catext);
  }

  @Override
  public boolean removeCatalog(String relPath) {
    return catTracker.removeCatalog(relPath);
  }

  @Override
  public Iterable<? extends CatalogExt> getCatalogs() {
    return catTracker.getCatalogs();
  }
}

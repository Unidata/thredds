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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * org.mapdb.DB implementation of DatasetTracker
 *
 * @author caron
 * @since 6/7/2015
 */
public class DatasetTrackerMapDB implements DatasetTracker {
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");

  private File dbFile;
  private DB db;
  private Set<Externalizable> catalogSet;
  private Set<Externalizable> datarootSet;
  private HTreeMap<String, Externalizable> datasetMap;
  private boolean alreadyExists;

  public boolean init(String pathname, long maxDatasets) {
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

  public void close() {
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
    DB db = DBMaker.newFileDB(dbFile)
            .transactionDisable()
            .mmapFileEnableIfSupported()
            .closeOnJvmShutdown()
            .make();

    alreadyExists = db.exists("datasets");  // LOOK ??
    datasetMap = db.getHashMap("datasets");
    datarootSet = db.getHashSet("dataroots");
    catalogSet = db.getHashSet("catalogs");
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
      DatasetExt dsext = new DatasetExt(dataset, hasNcml);
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
    return datarootSet.add(dsext);
  }

  @Override
  public Iterable<? extends DataRootExt> getDataRoots() {
    List<DataRootExt> result = new ArrayList<>();
    for (Externalizable ext : datarootSet)
      result.add((DataRootExt) ext);
    return result;
    //return (Iterable<? extends DataRootExt>) datarootSet;
  }

  ////////////////////////////////////////////////////////////////
  // catalogs

  @Override
  public boolean trackCatalog(CatalogExt catext) {
    return catalogSet.add(catext);
  }

  @Override
  public Iterable<? extends CatalogExt> getCatalogs() {
    List<CatalogExt> result = new ArrayList<>();
    for (Externalizable ext : datarootSet)
    result.add((CatalogExt) ext);
    return result;
    //return (Iterable<? extends CatalogExt>) catalogSet;
  }
}

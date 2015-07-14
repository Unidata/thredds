package thredds.server.catalog.tracker;

import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import thredds.client.catalog.Access;
import thredds.client.catalog.Dataset;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.FeatureCollectionRef;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;

/**
 * Description
 *
 * @author John
 * @since 6/8/2015
 */
public class DatasetTrackerChronicle implements DatasetTracker {
  static private org.slf4j.Logger catalogInitLog = org.slf4j.LoggerFactory.getLogger("catalogInit");
  static private final String datasetName = "/chronicle.datasets.dat";

  // delete old databases
  public static void cleanupBefore(String pathname, long trackerNumber) {
    for (long tnum = trackerNumber - 1; tnum > 0; tnum--) {
      File oldDatabaseFile = new File(pathname + datasetName + "." + tnum);
      if (!oldDatabaseFile.exists()) break;
      if (oldDatabaseFile.delete()) {
        catalogInitLog.info("DatasetTrackerChronicle deleted {} ", oldDatabaseFile.getAbsolutePath());
      } else {
        catalogInitLog.error("DatasetTrackerChronicle not able to delete {} ", oldDatabaseFile.getAbsolutePath());
      }
    }
  }

  private boolean alreadyExists;
  private boolean changed;
  private String pathname;
  private File dbFile;
  private long maxDatasets;
  private ChronicleMap<String, Externalizable> datasetMap;

  public DatasetTrackerChronicle(String pathname, long maxDatasets, long number) {
    this.pathname = pathname;
    dbFile = new File(pathname + datasetName + "." + number);
    alreadyExists = dbFile.exists();
    this.maxDatasets = maxDatasets;

    try {
      open();
      catalogInitLog.info("DatasetTrackerChronicle opened success on '" + dbFile.getAbsolutePath() + "'");

    } catch (Throwable e) {
      catalogInitLog.error("DatasetTrackerChronicle failed on '" + dbFile.getAbsolutePath() + "', delete catalog cache and reload ", e);
      reinit();
    }
  }

  public void save() throws IOException {
    /* LOOK just a guess
    if (changed) {
      datasetMap.close();
      ChronicleMapBuilder<String, Externalizable> builder = ChronicleMapBuilder.of(String.class, Externalizable.class)
              .averageValueSize(200).entries(maxDatasets);
      datasetMap = builder.createPersistedTo(dbFile);
    } */
  }

  public void close() throws IOException {
    if (datasetMap != null) {
      datasetMap.close();
      System.out.printf("datasetMap.close() was called%n");
      datasetMap = null;
    }
  }

  public boolean exists() {
    return alreadyExists;
  }

  public boolean reinit() {
    if (dbFile.exists()) {
      boolean wasDeleted = dbFile.delete();
      if (!wasDeleted) {
        catalogInitLog.error("DatasetTrackerChronicle not able to delete {} ", dbFile.getAbsolutePath());
        return false;
      }
    }

    try {
      open();
      alreadyExists = false;
      return true;

    } catch (Throwable e) {
      catalogInitLog.error("DatasetTrackerChronicle failed on '" + dbFile.getAbsolutePath() + "', delete catalog cache and reload ", e);
      return false;
    }
  }

  private void open() throws IOException {
    ChronicleMapBuilder<String, Externalizable> builder = ChronicleMapBuilder.of(String.class, Externalizable.class)
            .averageValueSize(200).entries(maxDatasets);
    datasetMap = builder.createPersistedTo(dbFile);
  }

  public boolean trackDataset(Dataset dataset, DatasetTracker.Callback callback) {
    if (callback != null) {
      callback.hasDataset(dataset);
      boolean track = false;
       if (dataset.getRestrictAccess() != null) {
         callback.hasRestriction(dataset);
         track = true;
       }
      if (dataset.getNcmlElement()!= null) {
        callback.hasNcml(dataset);
        track = true;
      }
      if (track) callback.hasTrackedDataset(dataset);
    }

    boolean hasRestrict = dataset.getRestrictAccess() != null;
    boolean hasNcml = (dataset.getNcmlElement() != null) && !(dataset instanceof DatasetScan) && !(dataset instanceof FeatureCollectionRef);
    if (!hasRestrict && !hasNcml) return false;

    String path = null;
    for (Access access : dataset.getAccess()) {

      String accessPath = access.getUrlPath();
      if (accessPath == null)
        System.out.println("HEY");
      if (path == null) path = accessPath;
      else if (!path.equals(access.getUrlPath())) {
        System.out.printf(" paths differ: %s%n %s%n%n", path, accessPath);
      }
    }
    if (path == null)
      return false;

    changed = true;
    DatasetExt dsext = new DatasetExt(0, dataset, hasNcml);
    datasetMap.put(path, dsext);
    return true;
  }

  public String findResourceControl(String path) {
    DatasetExt dext = (DatasetExt) datasetMap.get(path);
    if (dext == null) return null;
    return dext.getRestrictAccess();
  }

  public String findNcml(String path) {
    DatasetExt dext = (DatasetExt) datasetMap.get(path);
    if (dext == null) return null;
    return dext.getNcml();
  }

}

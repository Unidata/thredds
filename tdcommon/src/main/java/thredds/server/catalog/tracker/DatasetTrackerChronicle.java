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
  static private org.slf4j.Logger startupLog = org.slf4j.LoggerFactory.getLogger("serverStartup");
  static private final String datasetName = "/chronicle.datasets.dat";

  private boolean alreadyExists;
  private boolean changed;
  private String pathname;
  private File dbFile;
  private long maxDatasets;
  private ChronicleMap<String, Externalizable> datasetMap;

  private CatalogTracker catalogTracker;
  private DataRootTracker dataRootTracker;

  public boolean init(String pathname, long maxDatasets) throws IOException {
    this.pathname = pathname;
    dbFile = new File(pathname+datasetName);
    alreadyExists = dbFile.exists();
    this.maxDatasets = maxDatasets;

    try {
      open();
      startupLog.info("DatasetTrackerChronicle opened success on '" + dbFile.getAbsolutePath() + "'");
      return true;

    } catch (Throwable e) {
      startupLog.error("DatasetTrackerChronicle failed on '"+ dbFile.getAbsolutePath()+ "', delete catalog cache and reload ", e);
      return reinit();
    }
  }

  @Override
  public void save() throws IOException {
    try {
      if (catalogTracker != null)
        catalogTracker.save();

      if (dataRootTracker != null)
        dataRootTracker.save();

    } catch (IOException ioe) {
      startupLog.error("Saving tracker info", ioe);
    }

    // LOOK just a guess
    if (changed) {
      datasetMap.close();
      ChronicleMapBuilder<String, Externalizable> builder = ChronicleMapBuilder.of(String.class, Externalizable.class)
              .averageValueSize(200).entries(maxDatasets);
      datasetMap = builder.createPersistedTo(dbFile);
    }
  }

  @Override
  public void close() throws IOException {
    if (catalogTracker != null) {
      catalogTracker.save();
      catalogTracker = null;
    }

    if (dataRootTracker != null) {
      dataRootTracker.save();
      dataRootTracker = null;
    }

    if (datasetMap != null) {
      datasetMap.close();
      datasetMap = null;
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
        startupLog.error("DatasetTrackerChronicle not able to delete {} ", dbFile.getAbsolutePath());
        return false;
      }
    }
    catalogTracker.reinit();
    dataRootTracker.reinit();

    try {
      open();
      alreadyExists = false;
      return true;

    } catch (Throwable e) {
      startupLog.error("DatasetTrackerChronicle failed on '"+ dbFile.getAbsolutePath()+ "', delete catalog cache and reload ", e);
      return false;
    }
  }

  private void open() throws IOException {
    ChronicleMapBuilder<String, Externalizable> builder = ChronicleMapBuilder.of(String.class, Externalizable.class)
             .averageValueSize(200).entries(maxDatasets);
    datasetMap = builder.createPersistedTo(dbFile);

    catalogTracker = new CatalogTracker(pathname);
    dataRootTracker = new DataRootTracker(pathname);
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
    return catalogTracker.trackCatalog(catext);
  }

  @Override
  public boolean removeCatalog(String relPath) {
    return catalogTracker.removeCatalog(relPath);
  }

  @Override
  public Iterable<? extends CatalogExt> getCatalogs() {
    return catalogTracker.getCatalogs();
  }

}

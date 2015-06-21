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

  ChronicleMap<String, Externalizable> map;

  public boolean init(String pathname, long maxDatasets) throws IOException {
    File file = new File(pathname+"/chronicle.dat");

    ChronicleMapBuilder<String, Externalizable> builder = ChronicleMapBuilder.of(String.class, Externalizable.class)
             .averageValueSize(200).entries(maxDatasets);

    map = builder.createPersistedTo(file);
    return true;
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

    DatasetExt dsext = new DatasetExt(dataset, hasNcml);
    map.put(path, dsext);
    return true;
  }

  @Override
  public String findResourceControl(String path) {
    DatasetExt dext = (DatasetExt) map.get(path);
    if (dext == null) return null;
    return dext.getRestrictAccess();
  }

  @Override
  public String findNcml(String path) {
    DatasetExt dext = (DatasetExt) map.get(path);
    if (dext == null) return null;
    return dext.getNcml();
  }

  @Override
  public boolean exists() {
    return false;
  }

  @Override
  public boolean reinit() {
    return false;
  }

  @Override
  public DatasetExt findDatasetExt(String path) {
    return null;
  }

  @Override
  public boolean trackDataRoot(DataRootExt ds) {
    return false;
  }

  @Override
  public DatasetExt findDataRootExt(String path) {
    return (DatasetExt) map.get(path);
  }

  @Override
  public Iterable<DataRootExt> getDataRoots() {
    return null;
  }

  @Override
  public boolean trackCatalog(CatalogExt ds) {
    return false;
  }

  @Override
  public Iterable<CatalogExt> getCatalogs() {
    return null;
  }

  public void close() {
    map.close();
  }
}

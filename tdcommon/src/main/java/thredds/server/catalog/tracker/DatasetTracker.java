/* Copyright */
package thredds.server.catalog.tracker;

import thredds.client.catalog.Dataset;
import thredds.server.catalog.ConfigCatalog;
import thredds.server.catalog.DataRoot;

import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 6/6/2015
 */
public interface DatasetTracker extends AutoCloseable {
  boolean init(String dirPath, long maxDatasets) throws IOException;
  boolean exists(); // detect if database exists
  boolean reinit(); // throw out all and start again


  boolean trackDataset(Dataset ds, Callback callback);

  String findResourceControl(String path);
  String findNcml(String path);
  DatasetExt findDatasetExt(String path);

  boolean trackDataRoot(DataRootExt ds);
  DatasetExt findDataRootExt(String path);  // LOOK Needed ?
  Iterable<DataRootExt> getDataRoots();

  boolean trackCatalog(CatalogExt ds);
  Iterable<CatalogExt> getCatalogs();

  void close();

  interface Callback {
    void hasDataRoot(DataRoot dataRoot);
    void hasDataset(Dataset dd);
    void hasNcml(Dataset dd);
    void hasRestriction(Dataset dd);
    void hasCatalogRef(ConfigCatalog dd);
    void finish();
  }
}

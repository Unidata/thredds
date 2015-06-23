/* Copyright */
package thredds.server.catalog.tracker;

import thredds.client.catalog.Dataset;
import thredds.server.catalog.ConfigCatalog;
import thredds.server.catalog.DataRoot;

import java.io.Externalizable;
import java.io.IOException;

/**
 * Describe
 *
 * @author caron
 * @since 6/6/2015
 */
public interface DatasetTracker extends AutoCloseable {
  // return true on success
  boolean init(String dirPath, long maxDatasets) throws IOException;

  void save();
  void close() throws IOException;
  boolean exists(); // detect if database exists
  boolean reinit(); // throw out all and start again

  // datasets
  boolean trackDataset(Dataset ds, Callback callback);
  String findResourceControl(String path);
  String findNcml(String path);
  //DatasetExt findDatasetExt(String path);  // LOOK Needed ?

  // data root
  boolean trackDataRoot(DataRootExt ds);
  Iterable<? extends DataRootExt> getDataRoots();
  //DatasetExt findDataRootExt(String path);  // LOOK Needed ?

  // catalogs
  boolean trackCatalog(CatalogExt ds);
  boolean removeCatalog(String relPath);
  Iterable<? extends CatalogExt> getCatalogs();

  interface Callback {
    void hasDataRoot(DataRoot dataRoot);
    void hasDataset(Dataset dd);
    void hasNcml(Dataset dd);
    void hasRestriction(Dataset dd);
    void hasCatalogRef(ConfigCatalog dd);
    void finish();
  }
}

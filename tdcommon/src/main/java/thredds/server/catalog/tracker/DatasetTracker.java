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
  boolean trackDataset(Dataset ds, Callback callback);
  String findResourceControl(String path);
  String findNcml(String path);
  CatalogExt findCatalogExt(String path);
  void close();

  interface Callback {
    void hasDataRoot(DataRoot dataRoot);
    void hasDataset(Dataset dd);
    void hasNcml(Dataset dd);
    void hasRestriction(Dataset dd);
    void hasCatalogRef(ConfigCatalog dd);
  }
}

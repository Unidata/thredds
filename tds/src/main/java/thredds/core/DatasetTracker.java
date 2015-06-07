/* Copyright */
package thredds.core;

import thredds.client.catalog.Dataset;
import thredds.server.catalog.ConfigCatalog;

/**
 * Describe
 *
 * @author caron
 * @since 6/6/2015
 */
public interface DatasetTracker {
  void trackDataset(Dataset ds, Callback callback);
  String findResourceControl(String path);
  String findNcml(String path);


  interface Callback {
    void hasDataRoot(DataRoot dataRoot);
    void hasDataset(Dataset dd);
    void hasNcml(Dataset dd);
    void hasRestriction(Dataset dd);
    void hasCatalogRef(ConfigCatalog dd);
  }
}

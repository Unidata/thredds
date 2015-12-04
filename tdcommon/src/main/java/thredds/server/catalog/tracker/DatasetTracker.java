/* Copyright */
package thredds.server.catalog.tracker;

import java.io.Closeable;
import java.io.IOException;
import java.util.Formatter;

import thredds.client.catalog.Dataset;
import thredds.server.catalog.ConfigCatalog;

/**
 * DatasetTracker interface
 *
 * @author caron
 * @since 6/6/2015
 */
public interface DatasetTracker extends Closeable {
  /* return true on success
  boolean init(String dirPath, long maxDatasets) throws IOException;  */

  void save() throws IOException;
  void close() throws IOException;
  boolean exists(); // detect if database exists
  boolean reinit(); // throw out all and start again

  // datasets
  boolean trackDataset(long catId, Dataset ds, Callback callback);
  String findResourceControl(String path);
  String findNcml(String path);

  // debug
  void showDB(Formatter f);

  interface Callback {
    void hasDataRoot(DataRootExt dataRoot);
    void hasDataset(Dataset dd);
    void hasTrackedDataset(Dataset dd);
    void hasNcml(Dataset dd);
    void hasRestriction(Dataset dd);
    void hasCatalogRef(ConfigCatalog dd);
    void finish();
  }
}

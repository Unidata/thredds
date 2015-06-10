package thredds.server.catalog.tracker;

import thredds.client.catalog.Dataset;

/**
 * Description
 *
 * @author John
 * @since 6/8/2015
 */
public class DatasetTrackerNoop implements DatasetTracker {
  @Override
  public boolean trackDataset(Dataset ds, Callback callback) {
    if (callback != null) callback.hasDataset(ds);

     if (ds.getRestrictAccess() != null) {
       if (callback != null) callback.hasRestriction(ds);
     }

    if (ds.getNcmlElement()!= null) {
      if (callback != null) callback.hasNcml(ds);
    }

    return false;
  }

  @Override
  public String findResourceControl(String path) {
    return null;
  }

  @Override
  public String findNcml(String path) {
    return null;
  }

  @Override
  public void close() {
  }

  @Override
  public CatalogExt findCatalogExt(String path) {
    return null;
  }
}

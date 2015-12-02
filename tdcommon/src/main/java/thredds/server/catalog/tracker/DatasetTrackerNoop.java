/* Copyright */
package thredds.server.catalog.tracker;

import thredds.client.catalog.Dataset;

import java.util.Formatter;

/**
 * Description
 *
 * @author John
 * @since 6/8/2015
 */
public class DatasetTrackerNoop implements DatasetTracker {

  @Override
  public boolean trackDataset(long catId, Dataset ds, Callback callback) {
    if (callback == null) return false;

    callback.hasDataset(ds);
    boolean track = false;
     if (ds.getRestrictAccess() != null) {
       callback.hasRestriction(ds);
       track = true;
     }
    if (ds.getNcmlElement()!= null) {
      callback.hasNcml(ds);
      track = true;
    }
    if (track) callback.hasTrackedDataset(ds);

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
  public void save() {
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public boolean reinit() {
    return true;
  }

  @Override
  public void showDB(Formatter f) {
  }

}

/* Copyright */
package thredds.server.catalog.tracker;

import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import thredds.client.catalog.Dataset;
import thredds.server.catalog.DatasetScan;
import thredds.server.catalog.FeatureCollectionRef;

import java.io.Externalizable;
import java.io.File;

/**
 * Describe
 *
 * @author caron
 * @since 6/7/2015
 */
public class DatasetTrackerMapDB implements DatasetTracker {

  final HTreeMap<String, Externalizable> map;

  public DatasetTrackerMapDB() {
    String tmp = System.getProperty("java.io.tmpdir");
    String pathname = "C:/temp/mapDBtest/cats.dat";

    File file = new File(pathname);

    DB db = DBMaker.newFileDB(file)
            .mmapFileEnableIfSupported()
            .closeOnJvmShutdown()
            .make();

    map = db.getHashMap("datasets");
  }

  @Override
  public void trackDataset(Dataset dataset, Callback callback) {
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
    if (!hasRestrict && !hasNcml) return;

    String ncml = null;
    if (hasNcml) {
      // want the string representation
      Element ncmlElem = dataset.getNcmlElement();
      XMLOutputter xmlOut = new XMLOutputter();
      ncml = xmlOut.outputString(ncmlElem);
    }

    CatalogExt dsext = new CatalogExt(dataset, ncml);
    map.put(dataset.getUrlPath(), dsext);
  }

  @Override
  public String findResourceControl(String path) {
    CatalogExt dext = (CatalogExt) map.get(path);
    if (dext == null) return null;
    return null; // dext.getResourceControl();
  }

  @Override
  public String findNcml(String path) {
    CatalogExt dext = (CatalogExt) map.get(path);
    if (dext == null) return null;
    return null; // dext.getNcml();
  }
}

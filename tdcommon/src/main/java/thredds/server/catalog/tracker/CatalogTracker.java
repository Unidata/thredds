package thredds.server.catalog.tracker;

import java.io.*;
import java.util.*;

/**
 * Track the list of catalogs
 *
 * @author John
 * @since 6/22/2015
 */
public class CatalogTracker {
  private static final String dbname = "/catTracker.dat";
  String filepath;
  Set<String> catalogs;
  boolean changed;

  public CatalogTracker(String pathname) {
    this.filepath = pathname + dbname;
    File file = new File(filepath);
    if (!file.exists() || readCatalogs() <= 0) {
      changed = true;
      catalogs = new HashSet<>();
    }
  }

  void reinit() {
    File file = new File(filepath);
    if (file.exists()) {
       boolean wasDeleted = file.delete();
       if (!wasDeleted) {
         throw new IllegalStateException("DatasetTrackerMapDB not able to delete "+ filepath);
       }
     }
    changed = true;
    catalogs = new HashSet<>();
  }

  // catalogs
  boolean trackCatalog(CatalogExt cat) {
    changed = true;
    return catalogs.add(cat.getCatRelLocation());
  }

  boolean removeCatalog(String relPath) {
    changed = true;
    return catalogs.remove(relPath);
  }

  Iterable<? extends CatalogExt> getCatalogs() {  // LOOK random order, should we sort?
    List<CatalogExt> result = new ArrayList<>();
    for (String relPath : catalogs) {
      CatalogExt ext = new CatalogExt(0, relPath);
      result.add(ext);
    }
    Collections.sort(result, (o1, o2) -> o1.getCatRelLocation().compareTo(o2.getCatRelLocation()));    // java 8 lambda, baby
    return result;
  }

  int readCatalogs() {
    catalogs = new HashSet<>();
    int count = 0;
    try (DataInputStream in = new DataInputStream(new FileInputStream(filepath))) {
      while (in.available() > 0) {
        CatalogExt ext = new CatalogExt();
        ext.readExternal(in);
        catalogs.add(ext.getCatRelLocation());
        count++;
      }

    } catch (IOException e) {
      e.printStackTrace();
      return 0;
    }
    return count;
  }

  void save() throws IOException {
    if (!changed) return;
    try (DataOutputStream out = new DataOutputStream(new FileOutputStream(filepath))) {
      for (String relPath : catalogs) {
        CatalogExt ext = new CatalogExt(0, relPath);
        ext.writeExternal(out);
      }
    }
    changed = false;
  }
}

package thredds.server.catalog.tracker;

import java.io.*;
import java.util.*;

/**
 * Track the list of catalogs.
 * Used to check if any have changed, without having to read the catalog tree.
 *
 * @author John
 * @since 6/22/2015
 */
public class CatalogTracker {
  private static final String dbname = "/catTracker.dat";
  private String filepath;
  private Set<CatalogExt> catalogs;
  private boolean changed;

  public CatalogTracker(String pathname, boolean startOver) {
    this.filepath = pathname + dbname;
    File file = new File(filepath);
    if (startOver) reinit();
    if (!file.exists() || startOver || readCatalogs() <= 0) {
      changed = true;
      catalogs = new HashSet<>();
    }
  }

  private void reinit() {
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
  public boolean trackCatalog(CatalogExt cat) {
    changed = true;
    return catalogs.add(cat);
  }

  public boolean removeCatalog(String catPath) {
    changed = true;
    return catalogs.remove( new CatalogExt(0, catPath, false));
  }

  // return sorted catalogs
  public Iterable<? extends CatalogExt> getCatalogs() {
    List<CatalogExt> result = new ArrayList<>();
    for (CatalogExt ext : catalogs)
      result.add(ext);
    Collections.sort(result, (o1, o2) -> o1.getCatRelLocation().compareTo(o2.getCatRelLocation()));    // java 8 lambda, baby
    return result;
  }

  private int readCatalogs() {
    catalogs = new HashSet<>();
    int count = 0;
    try (DataInputStream in = new DataInputStream(new FileInputStream(filepath))) {
      while (in.available() > 0) {
        CatalogExt ext = new CatalogExt();
        ext.readExternal(in);
        catalogs.add(ext);
        count++;
      }

    } catch (IOException e) {
      e.printStackTrace();
      return 0;
    }
    return count;
  }

  public void save() throws IOException {
    if (!changed) return;
    try (DataOutputStream out = new DataOutputStream(new FileOutputStream(filepath))) {
      for (CatalogExt ext : catalogs) {
        ext.writeExternal(out);
      }
    }
    changed = false;
  }
}

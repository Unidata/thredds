package thredds.server.catalog.tracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  static private final Logger logger = LoggerFactory.getLogger(CatalogTracker.class);

  private static final String dbname = "/catTracker.dat";
  private final String filepath;
  private final int expectedSize;
  private long nextCatId;
  private Map<String, CatalogExt> catalogs;
  private boolean changed;

  public CatalogTracker(String pathname, boolean startOver, int expectedSize, long nextCatId) {
    this.filepath = pathname + dbname;
    this.expectedSize = expectedSize > 0 ? expectedSize : 100;
    this.nextCatId = nextCatId;

    File file = new File(filepath);
    if (startOver) reinit();
    if (!file.exists() || startOver || readCatalogs() <= 0) {
      changed = true;
      catalogs = new HashMap<>(2*expectedSize);
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
    catalogs = new HashMap<>(2*expectedSize);
  }

  public long put(CatalogExt cat) {
    changed = true;
    if (cat.setCatId(nextCatId))
      nextCatId++;
    catalogs.put(cat.getCatRelLocation(), cat);
    return cat.getCatId();
  }

  public CatalogExt get(String path) {
    return catalogs.get(path);
  }

  public CatalogExt removeCatalog(String catPath) {
    changed = true;
    return catalogs.remove( catPath);
  }

    // return sorted catalogs
  public Iterable<? extends CatalogExt> getCatalogs() {
    if (catalogs == null) readCatalogs();

    List<CatalogExt> result = new ArrayList<>();
    for (CatalogExt ext : catalogs.values())
      result.add(ext);
    Collections.sort(result, (o1, o2) -> o1.getCatRelLocation().compareTo(o2.getCatRelLocation()));    // java 8 lambda, baby
    return result;
  }

  private int readCatalogs() {
    catalogs = new HashMap<>();
    int count = 0;
    try (DataInputStream in = new DataInputStream(new FileInputStream(filepath))) {
      while (in.available() > 0) {
        CatalogExt ext = new CatalogExt();
        ext.readExternal(in);
        catalogs.put(ext.getCatRelLocation(), ext);
        count++;
      }

    } catch (IOException e) {
      logger.error("read "+filepath, e);
      return 0;
    }
    return count;
  }

  public void save() throws IOException {
    if (!changed) return;
    try (DataOutputStream out = new DataOutputStream(new FileOutputStream(filepath))) {
      for (CatalogExt ext : catalogs.values()) {
        ext.writeExternal(out);
      }
    }
    changed = false;
  }

  public int size() {
    return catalogs.size();
  }

  public long getNextCatId() {
    return nextCatId;
  }
}

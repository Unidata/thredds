package thredds.server.catalog.tracker;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Description
 *
 * @author John
 * @since 6/22/2015
 */
public class CatalogTracker {
  String filepath;
  Set<String> catalogs;

  public CatalogTracker(String filepath) {
    this.filepath = filepath;
    File file = new File(filepath);
    if (!file.exists() || readCatalogs() <= 0)
      catalogs = new HashSet<>();
  }

  // catalogs
  boolean trackCatalog(CatalogExt cat) {
    return catalogs.add(cat.getCatRelLocation());
  }

  boolean removeCatalog(String relPath) {
    return catalogs.remove(relPath);
  }

  Iterable<? extends CatalogExt> getCatalogs() {
    List<CatalogExt> result = new ArrayList<>();
    for (String relPath : catalogs) {
      CatalogExt ext = new CatalogExt(0, relPath);
      result.add(ext);
    }
    return result;
  }

  int readCatalogs() {
    catalogs = new HashSet<>();
    int count = 0;
    try (ObjectInput in = new ObjectInputStream(new FileInputStream(filepath))) {
      while (in.available() > 0) {
        CatalogExt ext = new CatalogExt();
        ext.readExternal(in);
        catalogs.add(ext.getCatRelLocation());
        count++;
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return 0;

    } catch (IOException e) {
      e.printStackTrace();
      return 0;

    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return 0;
    }

    return count;
  }

  void save() throws IOException {
    try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream(filepath))) {
      for (String relPath : catalogs) {
        CatalogExt ext = new CatalogExt(0, relPath);
        ext.writeExternal(out);
      }
    }
  }
}

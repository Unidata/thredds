package thredds.server.catalog.tracker;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Description
 *
 * @author John
 * @since 6/22/2015
 */
public class DataRootTracker {
  private static final String dbname = "/datarootTracker.dat";
  String filepath;
  Set<DataRootExt> dataRoots;
  boolean changed;

  public DataRootTracker(String pathname) {
    this.filepath = pathname +dbname ;
    File file = new File(filepath);
    if (!file.exists() || readDataRoots() <= 0) {
      dataRoots = new HashSet<>();
      changed = true;
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
    dataRoots = new HashSet<>();
    changed = true;
  }

  boolean trackDataRoot(DataRootExt ds) {
    changed = true;
    return dataRoots.add(ds);
  }

  boolean removeDataRoot(DataRootExt ds) {
    changed = true;
    return dataRoots.remove(ds);
  }

  Iterable<? extends DataRootExt> getDataRoots() {
    return dataRoots;
  }

  int readDataRoots() {
    dataRoots = new HashSet<>();
    int count = 0;
    try (DataInputStream in = new DataInputStream(new FileInputStream(filepath))) {
      while (in.available() > 0) {
        DataRootExt ext = new DataRootExt();
        ext.readExternal(in);
        dataRoots.add(ext);
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
      for (DataRootExt ext : dataRoots) {
        ext.writeExternal(out);
      }
    }
    changed = false;
  }
}

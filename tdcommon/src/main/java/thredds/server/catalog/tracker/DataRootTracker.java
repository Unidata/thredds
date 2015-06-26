package thredds.server.catalog.tracker;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Track the list of dataRoots.
 * Used to check if any have changed, without having to read the catalog tree.
 *
 * @author John
 * @since 6/22/2015
 */
public class DataRootTracker {
  private static final String dbname = "/datarootTracker.dat";
  String filepath;
  Set<DataRootExt> dataRoots;
  boolean changed;

  public DataRootTracker(String pathname, boolean startOver) {
    this.filepath = pathname +dbname ;
    File file = new File(filepath);
    if (startOver) reinit();
    if (!file.exists() || startOver || readDataRoots() <= 0) {
      dataRoots = new HashSet<>();
      changed = true;
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
    dataRoots = new HashSet<>();
    changed = true;
  }

  public boolean trackDataRoot(DataRootExt ds) {
    changed = true;
    return dataRoots.add(ds);
  }

  boolean removeDataRoot(DataRootExt ds) {
    changed = true;
    return dataRoots.remove(ds);
  }

  public Iterable<? extends DataRootExt> getDataRoots() {
    return dataRoots;
  }

  private int readDataRoots() {
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

  public void save() throws IOException {
    if (!changed) return;
    try (DataOutputStream out = new DataOutputStream(new FileOutputStream(filepath))) {
      for (DataRootExt ext : dataRoots) {
        ext.writeExternal(out);
      }
    }
    changed = false;
  }
}

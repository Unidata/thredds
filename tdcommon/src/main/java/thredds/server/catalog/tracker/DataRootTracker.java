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
  String filepath;
  Set<DataRootExt> dataRoots;

  public DataRootTracker(String filepath) {
    this.filepath = filepath;
    File file = new File(filepath);
    if (!file.exists() || readDataRoots() <= 0)
      dataRoots = new HashSet<>();
  }

  boolean trackDataRoot(DataRootExt ds) {
    return dataRoots.add(ds);
  }

  boolean removeDataRoot(DataRootExt ds) {
    return dataRoots.remove(ds);
  }

  Iterable<? extends DataRootExt> getDataRoots() {
    return dataRoots;
  }

  int readDataRoots() {
    dataRoots = new HashSet<>();
    int count = 0;
    try (ObjectInput in = new ObjectInputStream(new FileInputStream(filepath))) {
      while (in.available() > 0) {
        DataRootExt ext = new DataRootExt();
        ext.readExternal(in);
        dataRoots.add(ext);
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
      for (DataRootExt ext : dataRoots) {
        ext.writeExternal(out);
      }
    }
  }
}

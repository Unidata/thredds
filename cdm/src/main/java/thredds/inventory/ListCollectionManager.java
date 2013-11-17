package thredds.inventory;

import thredds.filesystem.MFileOS;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * CollectionManager that is initialized by specific list of MFiles.
 *
 * @author caron
 * @since 11/13/13
 */
public class ListCollectionManager extends CollectionManagerAbstract {
  private List<MFile> mfiles = new ArrayList<>();

  public ListCollectionManager(String collectionName, String collectionSpec, String olderThan, Formatter errlog) {
    super(collectionName, null);

    if (collectionSpec.startsWith(MFileCollectionManager.LIST))
      collectionSpec = collectionSpec.substring(MFileCollectionManager.LIST.length());

    String[] files = collectionSpec.split(";");
    for (String s : files) {
      String filename = s.trim();
      if (filename.length() == 0) continue;
      Path p = Paths.get(filename);
      if (Files.exists(p))
        mfiles.add(new MFileOS(filename));
    }
  }

  @Override
  public String getRoot() {
    return null;
  }

  @Override
  public long getLastScanned() {
    return System.currentTimeMillis();
  }

  @Override
  public long getLastChanged() {
    return 0;
  }

  @Override
  public boolean isScanNeeded() {
    return false;
  }

  @Override
  public boolean scanIfNeeded() throws IOException {
    return false;
  }

  @Override
  public boolean scan(boolean sendEvent) throws IOException {
    return false;
  }

  @Override
  public Iterable<MFile> getFiles() {
    return mfiles == null ? new ArrayList<MFile>() : mfiles;
  }

}

package thredds.inventory;

import org.slf4j.Logger;
import thredds.filesystem.MFileOS;
import ucar.nc2.util.CloseableIterator;
import ucar.unidata.util.StringUtil2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * CollectionManager that is initialized by specific list of MFiles.
 * Sorted by name. no date extractor.
 *
 * @author caron
 * @since 11/13/13
 */
public class CollectionList extends CollectionAbstract {
  protected List<MFile> mfiles = new ArrayList<>();

  public CollectionList(String collectionName, String list, Logger logger) {
    super(collectionName, logger);

    if (list.startsWith(MFileCollectionManager.LIST))
      list = list.substring(MFileCollectionManager.LIST.length());

    String[] files = list.split(";");
    for (String s : files) {
      String filename = s.trim();
      if (filename.length() == 0) continue;
      Path p = Paths.get(filename);
      if (Files.exists(p))
        mfiles.add(new MFileOS(filename));
    }

    Collections.sort(mfiles);
  }

  protected CollectionList(String collectionName, Logger logger) {
    super(collectionName, logger);
  }

  @Override
  public Iterable<MFile> getFilesSorted() {
    return mfiles;
  }

  @Override
  public CloseableIterator<MFile> getFileIterator() throws IOException {
    return new MFileIterator(mfiles.iterator());
  }

  @Override
  public void close() {
    // noop
  }

}

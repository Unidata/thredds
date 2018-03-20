/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import org.slf4j.Logger;
import thredds.filesystem.MFileOS;
import ucar.nc2.util.CloseableIterator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * MCollection that is initialized by specific list of MFiles.
 * Sorted by name. no date extractor.
 *
 * @author caron
 * @since 11/13/13
 */
public class CollectionList extends CollectionAbstract {
  protected List<MFile> mfiles = new ArrayList<>();

  public CollectionList(String collectionName, String list, Logger logger) {
    super(collectionName, logger);

    if (list.startsWith(CollectionAbstract.LIST))
      list = list.substring(CollectionAbstract.LIST.length());

    long lastModified = 0;
    String[] files = list.split(";");
    for (String s : files) {
      String filename = s.trim();
      if (filename.length() == 0) continue;
      Path p = Paths.get(filename);
      if (Files.exists(p)) {
        MFileOS mfile = new MFileOS(filename);
        mfiles.add(new MFileOS(filename));
        lastModified = Math.max(lastModified, mfile.getLastModified());
      }
    }

    Collections.sort(mfiles);
    this.lastModified = lastModified;
    this.root = System.getProperty("user.dir");
  }

  public CollectionList(String collectionName, String root, List<MFile> mfiles, Logger logger) {
    super(collectionName, logger);
    setRoot(root);
    this.mfiles = mfiles;
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
    return new MFileIterator(mfiles.iterator(), null);
  }

  @Override
  public void close() {  }

}
